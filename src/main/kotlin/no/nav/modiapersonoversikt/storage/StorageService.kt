package no.nav.modiapersonoversikt.storage

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.CreateBucketRequest
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.features.BadRequestException
import no.nav.modiapersonoversikt.Metrics.Companion.timed
import no.nav.modiapersonoversikt.ObjectMapperProvider.Companion.objectMapper
import no.nav.modiapersonoversikt.model.VeilederObjekt
import no.nav.modiapersonoversikt.model.Veiledere
import org.slf4j.LoggerFactory

private const val VEILEDERREMOTESTORE_BUCKET_NAME = "veilarbremotestore-bucket"
private const val VEILEDERREMOTESTORE_KEY_NAME = "veilarbremotestore"
private val log = LoggerFactory.getLogger("veilarbremotestore.StorageService")

class StorageService(private val s3: AmazonS3) : StorageProvider {
    init {
        lagS3BucketsHvisNodvendig(VEILEDERREMOTESTORE_BUCKET_NAME)
    }

    override fun hentVeildere(): Veiledere =
        timed("hent_VeilederObjekt") {
            try {
                val remoteStore = s3.getObject(VEILEDERREMOTESTORE_BUCKET_NAME, VEILEDERREMOTESTORE_KEY_NAME)
                objectMapper.readValue(remoteStore.objectContent)
            } catch (e: Exception) {
                emptyMap()
            }
        }

    override fun hentVeilederObjekt(veilederId: String): VeilederObjekt {
        val res = hentVeildere()[veilederId];

        if (res != null) {
            return res
        }

        return VeilederObjekt(emptyMap(), "-1")
    }

    override fun oppdaterVeilederObjekt(veileder: VeilederObjekt): VeilederObjekt {
        val oppdatertVeilederStorage = veileder.id
                ?.let {
                    hentVeildere().plus(it to veileder)
                }
                ?: throw BadRequestException("id må være definert")

        lagreVeiledere(oppdatertVeilederStorage)

        return oppdatertVeilederStorage[veileder.id] ?: error("Fant ikke veileder med id: ${ veileder.id}")
    }


    override fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt): VeilederObjekt {
        if (!hentVeildere().containsKey(veilederObjekt.id)) {
            val nyeVeildere = hentVeildere().plus(veilederObjekt.id to veilederObjekt)
            lagreVeiledere(nyeVeildere)
        } else {
            throw BadRequestException("Finnes allerede data på veilederen")
        }
        return veilederObjekt
    }

    override fun slettVeilederObjekt(id: String) {
        val nyeVeildere = hentVeildere().minus(id)
        lagreVeiledere(nyeVeildere)
    }

    private fun lagreVeiledere(veileder: Veiledere) {
        timed("lagre_veiledere") {
            s3.putObject(VEILEDERREMOTESTORE_BUCKET_NAME, VEILEDERREMOTESTORE_KEY_NAME, objectMapper.writeValueAsString(veileder))
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
}
