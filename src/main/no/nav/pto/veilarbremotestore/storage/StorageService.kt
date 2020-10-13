package no.nav.pto.veilarbremotestore.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.features.BadRequestException
import no.nav.pto.veilarbremotestore.Metrics.Companion.timed
import no.nav.pto.veilarbremotestore.ObjectMapperProvider.Companion.objectMapper
import no.nav.pto.veilarbremotestore.model.VeilederObjekt
import java.security.MessageDigest

private const val VEILEDERREMOTESTORE_BUCKET_NAME = "veilarbremotestore-bucket"

class StorageService(private val s3: AmazonS3) : StorageProvider {
    init {
        lagS3BucketsHvisNodvendig(VEILEDERREMOTESTORE_BUCKET_NAME)
    }

    override fun hentVeilederObjekt(veilederId: String): VeilederObjekt? {
        val res = timed("hent_VeilederObjekt") {
            try {
                val hashedVeilederId = hashVeilederId(veilederId);
                val remoteStore = s3.getObject(VEILEDERREMOTESTORE_BUCKET_NAME, hashedVeilederId)
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
                    ?:leggTilVeilederObjekt(veilederObjekt, veilederId)


    override fun slettVeilederFelter(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt {
        val original = hentVeilederObjekt(veilederId) ?: throw BadRequestException("Fant ikke veileder med id: $veilederId")
        val tmp = original.filter { it.key !in veilederObjekt.keys }
        lagreVeiledere(tmp, veilederId)
        return tmp
    }

    override fun oppdaterVeilederObjekt(veilederObjekt: VeilederObjekt, veilederId: String): VeilederObjekt {
       if (hentVeilederObjekt(veilederId) != null) {
            lagreVeiledere(veilederObjekt, veilederId)
        } else {
            throw BadRequestException("Fant ikke veileder med id: $veilederId")
        }
        return veilederObjekt
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
        s3.deleteObject(VEILEDERREMOTESTORE_BUCKET_NAME, hashVeilederId(veilederId))
    }

    private fun lagreVeiledere(veileder: VeilederObjekt, veilederId: String) {
        timed("lagre_veiledere") {
            s3.putObject(VEILEDERREMOTESTORE_BUCKET_NAME, hashVeilederId(veilederId), objectMapper.writeValueAsString(veileder))
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

    private fun hashVeilederId (veilederId: String):  String {
        return MessageDigest.getInstance("SHA-256")
                .digest(veilederId.toByteArray())
                .fold("", { str, it -> str + "%02x".format(it) })
    }
}