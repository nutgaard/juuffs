package no.utgdev.juuffs.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.calllogging.*
import java.util.*

object Logging {
    class Config {
        var callIdMdc: String = "call-id"
    }

    val Plugin = createApplicationPlugin("Logging", ::Config) {
        val config = pluginConfig
        with(application) {
            install(CallLogging) {
                callIdMdc(config.callIdMdc)
                disableDefaultColors()
            }
            install(CallId) {
                header(HttpHeaders.XCorrelationId)
                header(HttpHeaders.XRequestId)
                generate { UUID.randomUUID().toString() }
            }
        }
    }
}