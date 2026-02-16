package no.utgdev.juuffs

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

const val exampleJSON = """{"query":"*[_type=='featuretoggle'] {\n  ...\n}","result":[{"_createdAt":"2025-07-04T20:10:44Z","_id":"f73a7ea1-5f2f-45c0-9b3f-563950085422","_originalId":"drafts.f73a7ea1-5f2f-45c0-9b3f-563950085422","_rev":"42fd0870-3ec1-4c88-abf1-47b551ef42f9","_type":"featuretoggle","_updatedAt":"2025-07-04T20:22:52Z","description":"Skrur på egenregistrering","name":"TH-1234 egenregistrering","variants":[{"_key":"a91646c9436d","_type":"variant","constraints":[{"_key":"c5efeb8b159c","_type":"constraint","ctxKey":"env","operator":"EQUALS","type":"comparison","value":"stage"}],"description":"Variant for stage","evaluation":{"_type":"evaluation","isEnabled":true,"type":"fixed"},"name":"Stage"},{"_key":"6722f9173f0e","_type":"variant","constraints":[{"_key":"f321608c0787","_type":"constraint","ctxKey":"env","operator":"EQUALS","type":"comparison","value":"beta"},{"_key":"4baa1426de9d","_type":"constraint","isEnabled":true,"type":"default"}],"evaluation":{"_type":"evaluation","percentage":50,"type":"gradual"},"name":"Beta"},{"_key":"518bd6c7be04","_type":"variant","constraints":[{"_key":"b855f43f4869","_type":"constraint","ctxKey":"env","operator":"EQUALS","type":"comparison","value":"prod"},{"_key":"27e10659e3d8","_type":"constraint","ctxKey":"username","operator":"IN_LIST","type":"comparison","value":"utgdev,sanned"}],"evaluation":{"_type":"evaluation","isEnabled":true,"type":"fixed"},"name":"Prod"}]}],"syncTags":["s1:oT8kxg"],"ms":5}"""
const val exampleJSO2 = """{"query":"*\n[_type=='featuretoggle']\n[name == name]\n{\n  ...\n}","result":[{"_createdAt":"2025-07-04T20:10:44Z","_id":"drafts.f73a7ea1-5f2f-45c0-9b3f-563950085422","_rev":"94be1483-5e1b-4106-b865-d146ee6c0724","_type":"featuretoggle","_updatedAt":"2025-07-05T12:12:03Z","name":"TH-1234 egenregistrering","variants":[{"_key":"a91646c9436d","_type":"variant","constraints":[{"_key":"c5efeb8b159c","_type":"constraint","ctxKey":"env","operator":"EQUALS","type":"comparison","value":"stage"}],"evaluation":{"_type":"evaluation","isEnabled":true,"type":"fixed"},"name":"Stage"},{"_key":"6722f9173f0e","_type":"variant","constraints":[{"_key":"f321608c0787","_type":"constraint","ctxKey":"env","operator":"EQUALS","type":"comparison","value":"beta"},{"_key":"4baa1426de9d","_type":"constraint","isEnabled":true,"type":"default"}],"evaluation":{"_type":"evaluation","percentage":50,"type":"gradual"},"name":"Beta"},{"_key":"518bd6c7be04","_type":"variant","constraints":[{"_key":"b855f43f4869","_type":"constraint","ctxKey":"env","operator":"EQUALS","type":"comparison","value":"prod"},{"_key":"27e10659e3d8","_type":"constraint","ctxKey":"username","operator":"IN_LIST","type":"comparison","value":"utgdev,sanned"}],"evaluation":{"_type":"evaluation","isEnabled":true,"percentage":75,"type":"gradual"},"name":"Prod"}]}],"syncTags":["s1:oT8kxg"],"ms":3}"""

fun main() {
    val json = Json {
        ignoreUnknownKeys = true
    }
    val result = json.decodeFromString<SanityResponse<FeatureToggles.Toggle>>(exampleJSO2)
    println(result)

    val stageEnabled = result.result.first().simulate(50,
        FeatureToggles.Context.of(
            "env" to "stage",
        )
    )

    val betaEnabled = result.result.first().simulate(50,
        FeatureToggles.Context.of(
            "env" to "beta",
        )
    )

    val prodEnabled = result.result.first().simulate(50,
        FeatureToggles.Context.of(
            "env" to "prod",
            "username" to "utgdev2"
        )
    )

    println("ENABLED: $stageEnabled $betaEnabled $prodEnabled")
}

fun FeatureToggles.Toggle.simulate(count: Int, ctx:  FeatureToggles.Context): String {
    var enabled = 0
    repeat(count) {
        if (this.evaluate(ctx)) enabled++
    }

    return "$enabled/$count"
}

@Serializable
data class SanityResponse<T>(
    val query: String,
    val result: List<T>,
)