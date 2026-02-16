package no.utgdev.juuffs

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SanityClient(val config: Config) {
    data class Config(
        val projectId: String,
        val dataset: String,
        val token: String?,
        val apiVersion: String = "v2025-07-10",
    )
    val parser = Json {
        ignoreUnknownKeys = true
    }
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(parser)
        }
    }
    val sseClient = HttpClient(CIO) {
        install(SSE) {
            reconnectionTime = 3.seconds
            maxReconnectionAttempts = 5
        }
    }
    val url = "https://${config.projectId}.api.sanity.io/${config.apiVersion}/data/query/${config.dataset}"
    val sseUrl = "https://${config.projectId}.api.sanity.io/${config.apiVersion}/data/listen/${config.dataset}"

    suspend inline fun <reified T> fetch(
        groq: String,
        parameters: Map<String, Any> = emptyMap(),
        perspective: Perspective? = null,
    ): SanityResponse<T> {
        return httpClient.get(url) {
            perspective?.also {
                parameter("perspective", it.value)
            }
            parameter("query", groq)
            for (entry in parameters.toJsonObject()) {
                parameter("\$${entry.key}", parser.encodeToString(entry.value))
            }
            config.token?.also {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }.body()
    }

    sealed class SanityEvent {
        data class Mutation(
            val eventId: String,
            val documentId: String,
            val transition: String,
            val mutations: JsonArray?
        ) : SanityEvent()
    }
    sealed class DocumentChange<T> {
        data class Update<T>(val doc: T) : DocumentChange<T>()
        data class Deletion<T>(val documentId: String) : DocumentChange<T>()
    }

    class ListenConfig(
        val processingChunkSize: Int,
        val processingMaxDelay: Duration,
    ) {
        companion object {
            val Default = ListenConfig(5, 1.seconds)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalAtomicApi::class)
    inline fun <reified T> listen(
        groq: String,
        parameters: Map<String, Any> = emptyMap(),
        perspective: Perspective? = null,
        listenConfig: ListenConfig = ListenConfig.Default
    ): Flow<DocumentChange<T>> = callbackFlow {
        println("\uD83D\uDD04Connecting to SSE...")
        sseClient.sse(urlString = sseUrl, request = {
            config.token?.also {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
            parameter("query", groq)
            for (entry in parameters.toJsonObject()) {
                parameter("\$${entry.key}", parser.encodeToString(entry.value))
            }
        }) {
            incoming
                .filter { event -> event.event != null }
                .mapNotNull { event ->
                    println("ℹ️Got event '${event.event}': ${event.data}")
                    when (event.event) {
                        "welcome" -> {
                            null
                        }

                        "disconnect" -> {
                            cancel("Disconnect request: ${event.data}")
                            null
                        }

                        "channelError" -> {
                            cancel("ChannelError: ${event.data}")
                            null
                        }

                        "mutation" -> {
                            val eventData = requireNotNull(event.data) { "MutationData not found" }
                            val data = parser.parseToJsonElement(eventData).jsonObject
                            SanityEvent.Mutation(
                                eventId = requireNotNull(data["eventId"]?.jsonPrimitive?.content),
                                documentId = requireNotNull(data["documentId"]?.jsonPrimitive?.content),
                                transition = requireNotNull(data["transition"]?.jsonPrimitive?.content),
                                mutations = data["mutations"]?.jsonArray
                            )
                        }

                        else -> error("❌Unrecognized event: ${event.event}")
                    }
                }
                .filter { mutation ->
                    val res = when (perspective) {
                        null -> true
                        Perspective.RAW -> true
                        Perspective.DRAFTS -> mutation.documentId.startsWith("drafts.")
                        Perspective.PUBLISHED -> !mutation.documentId.startsWith("drafts.")
                    }

                    println("ℹ️Filtering for perspective: $perspective -> $res")
                    res
                }
                .chunkedWithTimeout(listenConfig.processingChunkSize, listenConfig.processingMaxDelay)
                .map { mutations ->
                    println("ℹ️Got mutations: ${mutations.size}")
                    val idsToFetch = mutations.filter { it.transition != "disappear" }.map { it.documentId }
                    val deletions: Flow<DocumentChange<T>> = mutations
                        .filter { it.transition == "disappear" }
                        .map { DocumentChange.Deletion<T>(it.documentId) }
                        .asFlow()
                    val groq = """*[_id in ${'$'}ids]"""

                    val updates: Flow<DocumentChange<T>> = runCatching {
                        val result = fetch<T>(groq, mapOf("ids" to idsToFetch), Perspective.RAW).result
                        println("ℹ️Fetched documents: ${result.size}")
                        result.map { DocumentChange.Update(it) }.asFlow()
                    }.getOrElse {
                        println(it)
                        emptyFlow()
                    }
                    merge(deletions, updates)
                }
                .flattenConcat()
                .collect { trySend(it) }
        }
    }

    enum class Perspective(val value: String) {
        RAW("raw"),
        DRAFTS("drafts"),
        PUBLISHED("published"),
    }

    @Serializable
    data class SanityResponse<T>(
        val query: String,
        val result: List<T>,
    )
}

fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is String -> JsonPrimitive(this)
    is Int -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Double -> JsonPrimitive(this)
    is List<*> -> buildJsonArray {
        for (v in this@toJsonElement) {
            add(v.toJsonElement())
        }
    }

    is Map<*, *> -> buildJsonObject {
        for ((k, v) in this@toJsonElement) {
            put(k.toString(), v.toJsonElement())
        }
    }

    else -> error("Unsupported type: $this")
}

fun Map<String, Any>.toJsonObject(): JsonObject = buildJsonObject {
    for ((k, v) in this@toJsonObject) {
        put(k, v.toJsonElement())
    }
}

fun <T> Flow<T>.chunkedWithTimeout(
    chunkSize: Int,
    timeout: Duration
): Flow<List<T>> = channelFlow {
    var buffer = mutableListOf<T>()
    var job: Job? = null

    suspend fun flush(reason: String) {
        println("ℹ️Flusing buffer: $reason")
        send(buffer)
        buffer = mutableListOf()
    }

    collect { value ->
        buffer.add(value)
        if (buffer.size >= chunkSize) {
            job?.cancel()
            flush("buffer size")
        } else if (buffer.size == 1) {
            // Start timer to flush when first element is added
            job = launch {
                delay(timeout)
                flush("after delay $timeout")
            }
        }
    }

    job?.cancel()
    flush("end of flow")
}