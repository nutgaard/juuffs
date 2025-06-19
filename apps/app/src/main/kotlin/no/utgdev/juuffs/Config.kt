package no.utgdev.juuffs

import no.utgdev.utils.no.utgdev.juuffs.FeatureToggles.toggle

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