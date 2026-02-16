package no.utgdev.juuffs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.utgdev.juuffs.FeatureToggles.ContraintOperator.IN_LIST
import kotlin.random.Random

object FeatureToggles {
    @Serializable
    data class Toggle(
        val name: String,
        val variants: List<Variant> = emptyList()
    ) {
        fun evaluate(ctx: Context): Boolean {
            return variants.any { it.evaluate(ctx) }
        }
    }

    @Serializable
    data class Variant(
        val name: String,
        val constraints: List<Contraint> = emptyList(),
        val evaluation: Evaluation
    ) {
        fun evaluate(ctx: Context): Boolean {
            val satisfiesContraints = constraints.all { it.evaluate(ctx) }
            return if (satisfiesContraints) evaluation.isEnabled(ctx) else false
        }
    }

    data class Context(
        val data: Map<String, String>,
    ) {
        fun merge(other: Context): Context = Context(this.data + other.data)

        companion object {
            val Empty = Context(emptyMap())
            fun of(vararg entries: Pair<String, String>) = Context(entries.toMap())
        }
    }

    @Serializable
    sealed interface Contraint {
        fun evaluate(ctx: Context): Boolean

        @Serializable
        @SerialName("default")
        data class Default(val isEnabled: Boolean = true) : Contraint {
            override fun evaluate(ctx: Context): Boolean = isEnabled
        }

        @Serializable
        @SerialName("comparison")
        data class Comparison(val ctxKey: String, val operator: ContraintOperator, @SerialName("value") val constraintValue: String) : Contraint {
            override fun evaluate(ctx: Context): Boolean {
                val ctxValue = ctx.data[ctxKey]
                return operator.evaluate(ctxValue, constraintValue)
            }
        }
    }

    enum class ContraintOperator {
        EQUALS, NOT_EQUALS, IN_STRING, NOT_IN_STRING, IN_LIST, NOT_IN_LIST;

        fun evaluate(ctxValue: String?, contraintvalue: String): Boolean {
            if (ctxValue == null) return false

            return when (this) {
                EQUALS -> ctxValue == contraintvalue
                NOT_EQUALS -> ctxValue != contraintvalue
                IN_STRING -> contraintvalue.contains(ctxValue)
                NOT_IN_STRING -> !contraintvalue.contains(ctxValue)
                IN_LIST -> contraintvalue.split(",").map{ it.trim() }.contains(ctxValue)
                NOT_IN_LIST -> !contraintvalue.split(",").map{ it.trim() }.contains(ctxValue)
            }
        }
    }

    @Serializable
    sealed interface Evaluation {
        fun isEnabled(ctx: Context): Boolean

        @Serializable
        @SerialName("fixed")
        data class FixedEvaluation(val isEnabled: Boolean): Evaluation {
            override fun isEnabled(ctx: Context): Boolean = isEnabled
        }

        @Serializable
        @SerialName("gradual")
        data class GradualEvaluation(
            val percentage: Int,
            @Transient val random: Random = Random.Default,
        ) : Evaluation {
            override fun isEnabled(ctx: Context): Boolean = random.nextInt(0, 100) < percentage
        }
    }

    @DslMarker
    annotation class FeatureToggleDsl

    @FeatureToggleDsl
    class ToggleBuilder(val name: String) {
        val variants = mutableListOf<Variant>()
        fun variant(name: String, fn: VariantBuilder.() -> Unit) {
            variants += VariantBuilder(name).apply(fn).build()
        }

        fun build() = Toggle(
            name = name,
            variants = variants,
        )
    }

    @FeatureToggleDsl
    class VariantBuilder(val name: String) {
        val constraints = mutableListOf<Contraint>()
        var evaluation: Evaluation = Evaluation.FixedEvaluation(false)

        fun constraint(fn: ContraintBuilder.() -> Unit) {
            constraints += ContraintBuilder().apply(fn).build()
        }

        fun evaluation(fn: EvaluationBuilder.() -> Unit) {
            evaluation = EvaluationBuilder().apply(fn).build()
        }

        fun build() = Variant(
            name = name,
            constraints = constraints,
            evaluation = evaluation,
        )
    }

    @FeatureToggleDsl
    class ContraintBuilder {
        val contraints = mutableListOf<Contraint>(
            Contraint.Default(true),
        )
        infix fun String.equals(other: String) {
            contraints += Contraint.Comparison(this, ContraintOperator.EQUALS, other)
        }
        infix fun String.notEquals(other: String) {
            contraints += Contraint.Comparison(this, ContraintOperator.NOT_EQUALS, other)
        }

        infix fun String.listContains(other: String) {
            contraints += Contraint.Comparison(this, IN_LIST, other)
        }

        fun build() : List<Contraint> = contraints
    }

    @FeatureToggleDsl
    class EvaluationBuilder {
        var evaluation: Evaluation = Evaluation.FixedEvaluation(isEnabled = false)

        fun default(isEnabled: Boolean) {
            evaluation = Evaluation.FixedEvaluation(isEnabled = isEnabled)
        }

        fun gradual(percentage: Int, random: Random = Random.Default) {
            evaluation = Evaluation.GradualEvaluation(percentage, random)
        }

        fun build(): Evaluation = evaluation
    }


    fun toggle(name: String, fn: ToggleBuilder.() -> Unit): Toggle {
        return ToggleBuilder(name).apply(fn).build()
    }
}