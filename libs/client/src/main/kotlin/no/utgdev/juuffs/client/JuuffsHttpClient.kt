package no.utgdev.juuffs.client

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.callid.CallId
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.http.appendPathSegments
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import no.utgdev.juuffs.FeatureToggles

interface JuuffsHttpClient {
    suspend fun fetch(): List<FeatureToggles.Toggle>
}

class JuuffsHttpClientImpl(private val baseUrl: String) : JuuffsHttpClient {
    private val httpClient = HttpClient(CIO) {
        engine {
            maxConnectionsCount = 1
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 1000
        }
        install(CallId) {

        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    override suspend fun fetch(): List<FeatureToggles.Toggle> {
        return httpClient.get(baseUrl) {
            url {
                appendPathSegments("toggles")
            }
        }.body()
    }
}