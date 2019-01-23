package certServer

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.util.UUID.randomUUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApplicationTest {

    // A more sustainable way to test this particular case would be to not
    // copy/paste the exact error message because that could change and then
    // you'd have to change it in both the production code and the test code.
    @Test
    fun `Shows instructions when root endpoint is pinged`() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals(
                    """
                Hi there! This service is used to get mock certificates for domains that you provide.

                In order to use this service, you'll need to use the "/cert/{domain}" endpoint.

                For example, "/cert/https://thisisanexample.com" is how you would use this service to get the certificate for thisisanexample.com

            """.trimIndent(), response.content
                )
            }

        }
    }


    @Test
    fun `Asks for a domain when cert route isn't provided with one`() {
        withTestApplication({ module(testing = true) }) {
            handleRequest(HttpMethod.Get, "/cert/").apply {
                assertEquals("Sorry, would you please include a domain in your request?", response.content)
            }
        }
    }

    @Test
    fun `Returns cached data for unexpired domains`() {
        withTestApplication({ module(testing = true) }) {
            val cachedId: String? = handleRequest(HttpMethod.Get, "/cert/test").response.content

            handleRequest(HttpMethod.Get, "/cert/test").apply {
                assertEquals(cachedId, response.content)
            }
        }
    }

    @Test
    fun `Only makes one request for a new ID when multiple requests are made for a new domain`() {
        withTestApplication({ module(testing = true) }) {
            suspend fun parallelRequests() {

                // Start two requests asynchronously.
                val req1 = async { handleRequest(HttpMethod.Get, "/cert/test") }
                val req2 = async { handleRequest(HttpMethod.Get, "/cert/test") }

                // Get the request contents without blocking threads, but
                // suspending the function until both requests are done.
                val res1 = req1.await()
                val res2 = req2.await()

                assertEquals(res1.response.content, res2.response.content)
            }
            runBlocking { parallelRequests() }
        }
    }

    @Test
    fun `createOrRefreshCertificate refreshes an expired certificate`() {
        withTestApplication({ module(testing = true) }) {
            val app = CertApp()
            val expiredCert = MockCert(randomUUID().toString(), LocalDateTime.now().minusMonths(4), Status.CERTIFIED)
            val testMap = ConcurrentHashMap<String, MockCert>().apply {
                runBlocking {
                    app.getCertificate(app.self, this@apply)
                }
                put("expired", expiredCert)
            }

            assertNotEquals(expiredCert.id, app.getCertificate("expired", testMap))
        }
    }

}

