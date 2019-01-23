package certServer

import java.net.InetAddress
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CertApp(
    // Amount of time to keep the certificate in memory
    private val ttl: TimeToLive = TimeToLive(3, ChronoUnit.MONTHS),
    // Our own name to use for managing our own certificate
    val self: String = InetAddress.getLocalHost().hostName
) {

    // When refreshing, we update the status of the certificate
    // so that any other requests for the same certificate are
    // paused until we've got a new id
    private fun createOrRefreshCertificate(domain: String, map: ConcurrentHashMap<String, MockCert>): String {
        map[domain] = MockCert(status = Status.FETCHING)
        Thread.sleep(10000)
        map[domain] =
                synchronized(map) { MockCert(UUID.randomUUID().toString(), LocalDateTime.now(), Status.CERTIFIED) }
        return "${map.getValue(domain).id}-$domain"
    }

    fun getCertificate(domain: String, map: ConcurrentHashMap<String, MockCert>): String =
        when {
            // First handle cases where we don't have any record of the requested certificate
            !map.containsKey(domain) -> synchronized(map) { createOrRefreshCertificate(domain, map) }

            // Next we make sure we don't make multiple requests for a certificate we're waiting for
            map.getValue(domain).status == Status.FETCHING -> {
                when {
                    map.getValue(domain).status == Status.CERTIFIED -> "${map.getValue(domain).id}-$domain"
                    else -> synchronized(map) { getCertificate(domain, map) }
                }
            }

            // Then we'll be sure that our certificate is always up to date
            map.getValue(self).creationDate!! <= LocalDateTime.now().minus(
                ttl.value,
                ttl.unit
            ) -> synchronized(map) { createOrRefreshCertificate(self, map) }

            // Then we auto-refresh any of the other certificates
            map.getValue(domain).creationDate!! <= LocalDateTime.now().minus(
                ttl.value,
                ttl.unit
            ) -> synchronized(map) { createOrRefreshCertificate(domain, map) }

            // If we get to this point we're clear to serve the certificate from memory
            else -> "${map.getValue(domain).id}-$domain"
        }
}
