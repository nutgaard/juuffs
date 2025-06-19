package no.utgdev.juuffs.plugins

import dev.hayden.KHealth
import io.ktor.server.application.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.MeterBinder
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

object Monitoring {
    class Config {
        var contextPath: String = ""
        var metricName: String = "ktor.http.server.requests"
        var meterBinders: List<MeterBinder> = listOf(
            ClassLoaderMetrics(),
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics(),
            JvmThreadMetrics(),
            FileDescriptorMetrics(),
        )
        var distributionStatisticConfig = DistributionStatisticConfig.Builder()
            .percentiles(0.5, 0.9, 0.95, 0.99)
            .build()

    }

    val Registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val Plugin = createApplicationPlugin("Monitoring", ::Config) {
        val config = pluginConfig
        with(application) {
            install(MicrometerMetrics) {
                registry = Registry
                metricName = config.metricName
                meterBinders = config.meterBinders
                distributionStatisticConfig = config.distributionStatisticConfig
            }

            install(KHealth) {
                healthCheckPath = "${config.contextPath}/internal/isAlive"
                readyCheckPath = "${config.contextPath}/internal/isReady"
                healthChecks {  }
            }

            routing {
                route(config.contextPath) {
                    get("internal/metrics") {
                        call.respondText(Registry.scrape())
                    }
                }
            }
        }
    }
}