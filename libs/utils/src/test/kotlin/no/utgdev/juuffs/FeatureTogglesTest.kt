package no.utgdev.utils.no.utgdev.juuffs

import org.junit.jupiter.api.Test
import kotlin.random.Random
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureTogglesTest {

    private class FixedRandom(private val value: Int) : Random() {
        override fun nextBits(bitCount: Int): Int = value
        override fun nextInt(from: Int, until: Int): Int = value
    }

    @Test
    fun `equals operator succeeds when values match`() {
        val ctx = FeatureToggles.Context(mapOf("user" to "123"))
        val constraint = FeatureToggles.Contraint.Comparison(
            "user",
            FeatureToggles.ContraintOperator.EQUALS,
            "123"
        )

        assertTrue(constraint.evaluate(ctx))
    }

    @Test
    fun `equals operator fails when values do not match`() {
        val ctx = FeatureToggles.Context(mapOf("user" to "456"))
        val constraint = FeatureToggles.Contraint.Comparison(
            "user",
            FeatureToggles.ContraintOperator.EQUALS,
            "123"
        )

        assertFalse(constraint.evaluate(ctx))
    }

    @Test
    fun `list contains operator succeeds when value exists`() {
        val ctx = FeatureToggles.Context(mapOf("groups" to "a, b, c"))
        val constraint = FeatureToggles.Contraint.Comparison(
            "groups",
            FeatureToggles.ContraintOperator.LIST_CONTAINS,
            "b"
        )

        assertTrue(constraint.evaluate(ctx))
    }

    @Test
    fun `list contains operator fails when value missing`() {
        val ctx = FeatureToggles.Context(mapOf("groups" to "a, b, c"))
        val constraint = FeatureToggles.Contraint.Comparison(
            "groups",
            FeatureToggles.ContraintOperator.LIST_CONTAINS,
            "d"
        )

        assertFalse(constraint.evaluate(ctx))
    }

    @Test
    fun `gradual evaluation returns true when random below percentage`() {
        val evaluation = FeatureToggles.Evaluation.GradualEvaluation(50, FixedRandom(10))
        val enabled = evaluation.isEnabled(FeatureToggles.Context.Empty)

        assertTrue(enabled)
    }

    @Test
    fun `gradual evaluation returns false when random above percentage`() {
        val evaluation = FeatureToggles.Evaluation.GradualEvaluation(50, FixedRandom(90))
        val enabled = evaluation.isEnabled(FeatureToggles.Context.Empty)

        assertFalse(enabled)
    }
}
