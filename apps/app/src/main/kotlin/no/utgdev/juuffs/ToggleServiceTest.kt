package no.utgdev.juuffs

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking {
    val config = Config()
    val toggleService = ToggleService(config)

    println(toggleService.getToggles())

    repeat(5) {
        delay(2.seconds)
        println("$it -> ${toggleService.getToggles()}")
    }

    toggleService.stopListener()
}