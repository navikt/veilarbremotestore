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

private const val VEILEDERREMOTESTORE_BUCKET_NAME = "veilarbremotestore-bucket"
private val log = LoggerFactory.getLogger("veilarbremotestore.StorageService")

class StorageService(private val s3: AmazonS3) : StorageProvider {
    init {
        lagS3BucketsHvisNodvendig(VEILEDERREMOTESTORE_BUCKET_NAME)
    }




    override fun hentVeilederObjekt(veilederId: String): VeilederObjekt? {
        val res = timed("hent_VeilederObjekt") {
            try {
                val remoteStore = s3.getObject(VEILEDERREMOTESTORE_BUCKET_NAME, veilederId)
                objectMapper.readValue<VeilederObjekt>(remoteStore.objectContent)
            } catch (e: Exception) {
                 null
            }
        }

        return res
    }

    override fun oppdaterVeilederObjekt(veileder: VeilederObjekt, id: String): VeilederObjekt {
        if (hentVeilederObjekt(id) != null ) {
            lagreVeiledere(veileder, id)
        } else {
            throw BadRequestException("Fant ikke veileder med id: ${id}")
        }
        return veileder
    }


    override fun leggTilVeilederObjekt(veilederObjekt: VeilederObjekt, id: String): VeilederObjekt {
        if (hentVeilederObjekt(id) == null) {
            lagreVeiledere(veilederObjekt, id)
        } else {
            throw BadRequestException("Finnes allerede data på veilederen")
        }
        return veilederObjekt
    }

    override fun slettVeilederObjekt(id: String) {
        s3.deleteBucket(id);
    }

    private fun lagreVeiledere(veileder: VeilederObjekt, id: String) {
        timed("lagre_veiledere") {
            s3.putObject(VEILEDERREMOTESTORE_BUCKET_NAME, id, objectMapper.writeValueAsString(veileder))
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
