package no.nav.veilarbremotestore.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.features.BadRequestException
import no.nav.veilarbremotestore.Metrics.Companion.timed
import no.nav.veilarbremotestore.ObjectMapperProvider.Companion.objectMapper
import no.nav.veilarbremotestore.model.VeilederObjekt
import org.slf4j.LoggerFactory

private const val VEILEDERREMOTESTORE_VEILEDER_BUCKET_NAME = "veilarbremotestore-veileder-bucket"
private const val VEILEDERREMOTESTORE_RESSURS_BUCKET_NAME = "veilarbremotestore-ressurs-bucket"
private val log = LoggerFactory.getLogger("veilarbremotestore.StorageService")

class StorageService(private val s3: AmazonS3) : StorageProvider {

    init {
        lagS3BucketsHvisNodvendig(VEILEDERREMOTESTORE_RESSURS_BUCKET_NAME)
    }

    override fun hentVeilederObjekt(veilederId: String): VeilederObjekt? {
        val res = timed("hent_VeilederObjekt") {
            try {
                val remoteStore = s3.getObject(VEILEDERREMOTESTORE_VEILEDER_BUCKET_NAME, veilederId)
                objectMapper.readValue<VeilederObjekt>(remoteStore.objectContent)
            } catch (e: Exception) {
                null
            }
        }

        return res
    }

    override fun oppdaterVeilederFelt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt =
            hentVeilederObjekt(veilederId)
                    ?.filterTo(veilederObjekt.toMutableMap()) { it.key !in veilederObjekt }
                    ?.also { lagreVeiledere(it, veilederId) }
                    ?: throw BadRequestException("Fant ikke veileder med id: $veilederId")


    override fun slettVeilederFelter(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt {
        val original = hentVeilederObjekt(veilederId) ?: throw BadRequestException("Fant ikke veileder med id: veilederId")
        val tmp = original.filter { it.key !in veilederObjekt.keys }
        lagreVeiledere(tmp, veilederId)
        return tmp
    }

    override fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt {
        if (hentVeilederObjekt(veilederId) != null) {
            lagreVeiledere(veilederObjekt, veilederId)
        } else {
            throw BadRequestException("Fant ikke veileder med id: ${veilederId}")
        }
        return veilederObjekt
    }

    override fun hentRessurser(): List<String> {
        return try {
            s3.listObjectsV2(VEILEDERREMOTESTORE_RESSURS_BUCKET_NAME).objectSummaries.map { it.key };
        } catch (e: Exception) {
            emptyList();
        }
    }


    override fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt {
        if (hentVeilederObjekt(veilederId) == null) {
            lagreVeiledere(veilederObjekt, veilederId)
        } else {
            throw BadRequestException("Finnes allerede data på veilederen")
        }
        return veilederObjekt
    }

    override fun slettVeilederObjekt(veilederId: String) {
        s3.deleteObject(VEILEDERREMOTESTORE_VEILEDER_BUCKET_NAME, veilederId)
    }

    override fun slettGamleRessurser () {
        val ressurser = hentRessurser();
        getAlleVeilederId().map{ veilederId ->
                hentVeilederObjekt(veilederId)
                        ?.filter { ressurser.contains(it.key) }
                        ?.also { println(it) }
                        ?.mapNotNull { it.key to hentRessurs(it.key)?.let { it1 -> slettGamleEndringer(it.value, it1) } }?.toMap()
                        ?.also { oppdaterVeilederFelt(it as VeilederObjekt, veilederId) }
            }
        }

    override fun leggTilRessurs(ressursNavn: String, vararg ressursVerdi: String) {
        s3.putObject(VEILEDERREMOTESTORE_RESSURS_BUCKET_NAME, ressursNavn, objectMapper.writeValueAsString(ressursVerdi))
    }

    override fun oppdaterRessurs(ressursNavn: String, vararg ressursVerdi: String) {
        s3.putObject(VEILEDERREMOTESTORE_RESSURS_BUCKET_NAME, ressursNavn, objectMapper.writeValueAsString(hentRessurs(ressursNavn)?.plus(ressursVerdi)))
    }

    private fun lagreVeiledere(veilederObjekt: VeilederObjekt, veilederId: String) {
        timed("lagre_veiledere") {
            s3.putObject(VEILEDERREMOTESTORE_VEILEDER_BUCKET_NAME, veilederId, objectMapper.writeValueAsString(veilederObjekt))
        }
    }

    private fun lagS3BucketsHvisNodvendig(vararg buckets: String) {
        timed("lag_buckets_hvis_nodvendig") {
            val s3BucketNames = s3.listBuckets().map { it.name }
            val missingBuckets = buckets.filter { !s3BucketNames.contains(it) }

            println("Wanted Buckets: ${buckets.joinToString(", ")}")
            println("Found Buckets: ${s3BucketNames.joinToString(", ")}")
            println("Missing Buckets: ${missingBuckets.joinToString(", ")}")

            missingBuckets
                    .forEach {
                        s3.createBucket(CreateBucketRequest(it).withCannedAcl(CannedAccessControlList.Private))
                    }
        }
    }

    override fun getAlleVeilederId(): List<String> {
        return try {
            s3.listObjectsV2(VEILEDERREMOTESTORE_VEILEDER_BUCKET_NAME).objectSummaries.map{ it.key };
        } catch (e: Exception) {
            emptyList();
        }
    }

    override fun hentRessurs(ressursNavn: String): List<String> {
        return try {
            val res = s3.getObject(VEILEDERREMOTESTORE_RESSURS_BUCKET_NAME, ressursNavn)
            objectMapper.readValue(res.objectContent);
        } catch (e: Exception) {
            emptyList();
        }
    }

    private fun slettGamleEndringer (a: String, listeMedMuligeEndringer: List<String>): String {
        return a.split(",")
                .map { it.trim() }
                .filter{listeMedMuligeEndringer.contains(it)}
                .joinToString {","}
    }
}
