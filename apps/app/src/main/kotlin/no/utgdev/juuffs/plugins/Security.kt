package no.utgdev.juuffs.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.*
import io.ktor.server.auth.*

object Security {
    class Config(
        var mock: Boolean = false,
        val providers: MutableList<AuthProvider> = mutableListOf(),
    )
    class AuthProvider(
        val name: String,
    )

    val Plugin = createApplicationPlugin("Security", ::Config) {
        val config = pluginConfig
        require(config.providers.isNotEmpty()) { "At least on authprovider must be provided. Remove this if not." }
        with(application) {
            install(Authentication) {
                if (config.mock) setupMock(config.providers)
                else setupAuth(config.providers)
            }
        }
    }

    private fun AuthenticationConfig.setupMock(providers: MutableList<AuthProvider>) {
        val name = "mock person"
        val token = JWT.create()
            .withSubject(name)
            .sign(Algorithm.none())
        val principal = TokenPrincipal(token)

        for (authProvider in providers) {
            provider(authProvider.name) {
                authenticate {
                    it.principal(principal)
                }
            }
        }
    }

    private fun AuthenticationConfig.setupAuth(providers: MutableList<AuthProvider>) {
        // TODO setup authenticators
    }

    class TokenPrincipal(
        val token: String,
        val payload: Payload
    ) {
        constructor(token: String): this(token, JWT.decode(token))

        val subject: String? = payload.subject
    }
}