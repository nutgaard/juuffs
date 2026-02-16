package no.utgdev.juuffs

import no.utgdev.juuffs.FeatureToggles.toggle

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
        token = "skYFHDALwzWde6Cmc6lKNKqZqMIvlM6fLsBttuwaPrUijjkH3xEVUlI5gyP7MwvsW8pmEQ50UiEdL7t1skvU4p7SErvBltZzIh9X40aTGk8WvTd8RsL7DuIZ9isFs3a4haqEz1Tg0azPeCEb6qtv3yweYVB4EJrif7Y6luqV0DUt7eqccf2L"
    )
}