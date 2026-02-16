package no.utgdev.juuffs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import no.utgdev.juuffs.SanityClient.DocumentChange

fun main() = runBlocking {
    val config = Config()
    val client = SanityClient(config.sanityConfig)
    val flowOfUpdates: Flow<DocumentChange<FeatureToggles.Toggle>> = client.listen<FeatureToggles.Toggle>(
        groq = "*[_type=='featuretoggle']"
    )
    flowOfUpdates.collect { println("GOT UPDATE: $it") }
}