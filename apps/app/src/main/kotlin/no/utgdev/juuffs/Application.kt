package no.utgdev.juuffs

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.juuffsModule() {
    routing {
        get("toggles") {
            call.respond(featureToggles)
        }
    }
}