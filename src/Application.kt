package certServer

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.application.log
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.request.path
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.time.LocalDateTime
import java.time.temporal.TemporalUnit
import java.util.concurrent.ConcurrentHashMap

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("UNUSED_PARAMETER") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    // default call logging for Ktor
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }

    // CORS is open to assist in local development
    install(CORS) {
        anyHost()
    }

    val app = CertApp()

    // The structure we'll be using to cache certificates.
    // After starting up the server we set up our own certificate first
    val certMap = ConcurrentHashMap<String, MockCert>().apply {
        runBlocking {
            app.getCertificate(app.self, this@apply)
        }
    }

    routing {
        get("/") {
            call.respondText {
                """
                Hi there! This service is used to get mock certificates for domains that you provide.

                In order to use this service, you'll need to use the "/cert/{domain}" endpoint.

                For example, "/cert/https://thisisanexample.com" is how you would use this service to get the certificate for thisisanexample.com

            """.trimIndent()
            }
        }

        get("/cert/{domain?}") {
            val domain = call.parameters["domain"]

            // If someone hits this route without providing
            // a domain, our default response is to send them
            // back a string asking for one
            domain?.let {
                log.info("Starting request...")
                call.respondText(app.getCertificate(domain, certMap))
                log.info("Request finished.")
            } ?: call.respondText { "Sorry, would you please include a domain in your request?" }
        }
    }
}

// Responsible for holding the mock certificate and metadata
data class MockCert(val id: String? = null, val creationDate: LocalDateTime? = null, val status: Status)

// We use this to avoid having our status be a string that could be typo'ed
enum class Status {
    FETCHING,
    CERTIFIED
}

data class TimeToLive(val value: Long, val unit: TemporalUnit)