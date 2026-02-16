package no.utgdev.juuffs

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respondText
import no.utgdev.juuffs.plugins.Logging
import no.utgdev.juuffs.plugins.Monitoring
import no.utgdev.juuffs.plugins.Security
import org.slf4j.LoggerFactory

fun startApplication(useMock: Boolean = false) {
    val logger = LoggerFactory.getLogger("main")
    val server = embeddedServer(Netty, port = 9090) {
        install(Logging.Plugin)
        install(Monitoring.Plugin)
        install(Security.Plugin) {
            mock = useMock
            providers += Security.AuthProvider("azuread")
        }
        install(DefaultHeaders)
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                logger.error("Unhandled exception", cause)
                call.respondText(text = "500: $cause", status = HttpStatusCode.InternalServerError)
            }
        }
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Patch)
            allowMethod(HttpMethod.Delete)
            allowHeader(HttpHeaders.Authorization)
            allowCredentials = true
            anyHost()
        }

        juuffsModule()
    }

    server.start(wait = true)
}

fun main() {
    startApplication(useMock = false)
}

