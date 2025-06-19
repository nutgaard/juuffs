package no.utgdev.juuffs.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.utgdev.utils.no.utgdev.juuffs.FeatureToggles
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface JuuffsClient {
    fun isEnabled(featureToggle: String, context: FeatureToggles.Context = FeatureToggles.Context.Empty): Boolean
    fun isEnabled(featureToggle: String, context: Map<String, String>): Boolean = isEnabled(
        featureToggle,
        FeatureToggles.Context(context)
    )
    fun stop()
}


class JuuffsClientImpl(
    val contextProvider: () -> FeatureToggles.Context,
    val httpClient: JuuffsHttpClient,
    val cacheTTL: Duration,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : JuuffsClient {
    private var isActive: Boolean = true
    private var job: Job
    private var cache: Map<String, FeatureToggles.Toggle> = emptyMap()
    private val logger = LoggerFactory.getLogger(JuuffsClientImpl::class.java)

    init {
        runBlocking {
            updateCache()
        }
        job = scope.launch {
            while (isActive) {
                runCatching {
                    updateCache()
                }.onFailure {
                    logger.error("Could not fetch featuretoggles", it)
                }
                delay(cacheTTL)
            }
        }
    }

    constructor(
        baseUrl: String,
        baseContext: FeatureToggles.Context = FeatureToggles.Context.Empty,
        cacheTTL: Duration = 10.seconds,
    ) : this(
        contextProvider = { baseContext },
        httpClient = JuuffsHttpClientImpl(baseUrl),
        cacheTTL = cacheTTL,
    )
    constructor(
        httpClient: JuuffsHttpClient,
        baseContext: FeatureToggles.Context = FeatureToggles.Context.Empty,
        cacheTTL: Duration = 10.seconds,
    ) : this(
        contextProvider = { baseContext },
        httpClient = httpClient,
        cacheTTL = cacheTTL,
    )

    override fun isEnabled(
        featureToggle: String,
        context: FeatureToggles.Context
    ): Boolean {
        val fullContext = contextProvider().merge(context)
        val toggle = cache[featureToggle] ?: return false
        return toggle.evaluate(fullContext)
    }

    override fun stop() {
        this.isActive = false
        this.job.cancel()
    }

    private suspend fun updateCache() {
        cache = httpClient.fetch().associateBy { it.name }
        logger.info("Updated cache: ${cache.size}")
    }
}