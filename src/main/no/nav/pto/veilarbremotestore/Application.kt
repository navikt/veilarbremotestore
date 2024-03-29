package no.nav.pto.veilarbremotestore

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import no.nav.pto.veilarbremotestore.storage.StorageService
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

private val log = LoggerFactory.getLogger("veilarbremotestore.Application")

data class ApplicationState(var running: Boolean = true, var initialized: Boolean = false)

fun main() {
    val configuration = Configuration()
    val credentials = BasicAWSCredentials(configuration.s3AccessKey, configuration.s3SecretKey)
    val s3 = AmazonS3ClientBuilder.standard()
            .withEndpointConfiguration(AwsClientBuilder.EndpointConfiguration(configuration.s3Url, configuration.s3Region))
            .enablePathStyleAccess()
            .withCredentials(AWSStaticCredentialsProvider(credentials)).build()

    val applicationState = ApplicationState()
    val applicationServer = createHttpServer(
            applicationState = applicationState,
            provider = StorageService(s3, configuration.namespace),
            configuration = configuration
    )

    Runtime.getRuntime().addShutdownHook(Thread {
        log.info("Shutdown hook called, shutting down gracefully")
        applicationState.initialized = false
        applicationServer.stop(5000, 5000)
    })

    applicationServer.start(wait = true)
}
