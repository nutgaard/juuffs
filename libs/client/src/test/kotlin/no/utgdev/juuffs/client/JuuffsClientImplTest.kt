package no.utgdev.juuffs.client

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class JuuffsClientImplTest {
    @Test
    fun `should create some tests`() {
        assertEquals("hei", "hei")
    }

    @Test
    @Ignore
    fun `base case`() = runBlocking {
        val client: JuuffsClient = JuuffsClientImpl(
            baseUrl = "http://localhost:8080",
            cacheTTL = 1.seconds,
        )

        async {
            repeat(10) {
                val isEnabled = client.isEnabled("TH-2000-dsl", mapOf(
                    "my_key" to "some-value",
                    "roles" to "heimdall-admin, heimdall-dev, heimdall-teamlead",
                ))
                println("isEnabled: $isEnabled")
                delay(3.seconds)
            }
        }

        delay(20.seconds)
    }
}
