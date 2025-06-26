package no.utgdev.utils.no.utgdev.juuffs

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureTogglesTest {

    @Test
    fun `variant evaluates to true when constraints satisfied`() {
        val toggle = FeatureToggles.toggle("test") {
            variant("v1") {
                constraint { "role" equals "admin" }
                evaluation { default(true) }
            }
        }

        val ctx = FeatureToggles.Context(mapOf("role" to "admin"))
        assertTrue(toggle.evaluate(ctx))
    }

    @Test
    fun `variant evaluates to false when constraints not satisfied`() {
        val toggle = FeatureToggles.toggle("test") {
            variant("v1") {
                constraint { "role" equals "admin" }
                evaluation { default(true) }
            }
        }

        val ctx = FeatureToggles.Context(mapOf("role" to "user"))
        assertFalse(toggle.evaluate(ctx))
    }

    @Test
    fun `listContains operator works`() {
        val toggle = FeatureToggles.toggle("test") {
            variant("v1") {
                constraint { "roles" listContains "dev" }
                evaluation { default(true) }
            }
        }

        val ctx = FeatureToggles.Context(mapOf("roles" to "dev,admin"))
        assertTrue(toggle.evaluate(ctx))
    }

    @Test
    fun `gradual evaluation should be disabled if value is higher`() {
        val rnd = StaticRandom(50)
        val toggle = FeatureToggles.toggle("gradual") {
            variant("v1") {
                evaluation {
                    gradual(10, rnd)
                }
            }
        }

        assertFalse(toggle.evaluate(FeatureToggles.Context.Empty))
    }

    @Test
    fun `gradual evaluation should be enabled if value is lower`() {
        val rnd = StaticRandom(50)
        val toggle = FeatureToggles.toggle("gradual") {
            variant("v1") {
                evaluation {
                    gradual(70, rnd)
                }
            }
        }

        assertTrue(toggle.evaluate(FeatureToggles.Context.Empty))
    }

    class StaticRandom(val value: Int) : Random() {
        override fun nextBits(bitCount: Int): Int {
            return value * 2
        }
    }
}