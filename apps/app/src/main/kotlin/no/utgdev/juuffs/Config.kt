package no.utgdev.juuffs

import no.utgdev.juuffs.FeatureToggles.toggle
import java.io.File
import java.io.FileReader
import java.util.Properties

val featureToggles = listOf(
    toggle(name = "TH-2000-dsl") {
        variant(name = "enabled for devs") {
            constraint {
                "my_key" equals "some-value2"
                "my_key" notEquals "some-other-value"
                "roles" listContains "heimdall-dev"
            }
            evaluation {
                default(true)
            }
        }
    }
)

class Config {
    val sanityConfig = SanityClient.Config(
        projectId = "jy1r6rh3",
        dataset = "production",
        token = EnvUtils.getRequiredConfig("sanity_token")
    )
}


object EnvUtils {
    fun getConfig(name: String, defaultValues: Map<String, String?> = emptyMap()): String? {
        return System.getProperty(
            name,
            System.getenv(name) ?: defaultValues[name]
        )
    }

    fun getRequiredConfig(name: String, defaultValues: Map<String, String?> = emptyMap()): String = requireNotNull(getConfig(name, defaultValues)) {
        "Could not find property for environment variable $name"
    }

    fun readEnv() {
        FileReader(File(".env")).use {
            val properties = Properties()
            properties.putAll(System.getProperties())
            properties.load(it)

            System.setProperties(properties)
        }
    }
}
