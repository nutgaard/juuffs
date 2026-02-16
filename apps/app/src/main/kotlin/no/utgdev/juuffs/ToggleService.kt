package no.utgdev.juuffs

import kotlinx.coroutines.*

class ToggleService(val config: Config) {
    private val sanityClient = SanityClient(config.sanityConfig)
    private val featuretoggles: MutableMap<String, FeatureToggles.Toggle> = mutableMapOf()
    private var listenerJob: Job? = null

    init {
        runBlocking {
            sanityClient.fetch<FeatureToggles.Toggle>(
                groq = "*[_type=='featuretoggle']",
                perspective = SanityClient.Perspective.RAW
            )
                .result
                .forEach {
                    println("toggle updated ${it.name}")
                    featuretoggles[it.name] = it
                }
        }
        startListener()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun startListener() {
        if (listenerJob != null) return

        listenerJob = GlobalScope.launch(Dispatchers.IO) {
            sanityClient.listen<FeatureToggles.Toggle>("*[_type=='featuretoggle']")
                .collect {
                    when (it) {
                        is SanityClient.DocumentChange.Deletion<*> -> {
                            featuretoggles.remove(it.documentId)
                        }
                        is SanityClient.DocumentChange.Update<*> -> {
                            val doc = it.doc as FeatureToggles.Toggle
                            featuretoggles[doc.name] = doc
                        }
                    }
                    println("toggle updated ${it}")
                }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun stopListener() {
        listenerJob?.cancel()
        listenerJob = null
    }

    fun getToggle(name: String): FeatureToggles.Toggle? = featuretoggles[name]
    fun getToggles(): List<FeatureToggles.Toggle> = featuretoggles.values.toList()
}