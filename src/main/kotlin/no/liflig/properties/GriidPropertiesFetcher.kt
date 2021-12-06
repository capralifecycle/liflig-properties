package no.liflig.properties

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Properties

class GriidPropertiesFetcher {
    private val serializer = JsonElement.serializer()
    private val json = Json {}

    @Throws(PropertyLoadingException::class)
    fun forPrefix(ssmPrefix: String): Properties =
        Properties().apply {
            putAll(parametersByPath(ssmPrefix))
            putAll(secretsByPath(ssmPrefix))
        }

    @Throws(ParameterLoadingException::class, SecretLoadingException::class)
    private fun parametersByPath(path: String): Map<String, String> =
        AwsClientHelper.getParametersByPath("$path/config/")
            .mapKeys { (key, _) -> key.removePrefix("$path/config/") }

    @Throws(ParameterLoadingException::class, SecretLoadingException::class)
    private fun secretsByPath(path: String): Map<String, String> =
        AwsClientHelper.getParametersByPath("$path/secrets/")
            .flatMap { (parameterKey, parameterValue) ->
                val baseKey = parameterKey.removePrefix("$path/secrets/")
                val jsonSecret = AwsClientHelper.getSecret(parameterValue)

                renameKeyAndSerializeValue(jsonSecret, baseKey)
            }
            .toMap()

    internal fun renameKeyAndSerializeValue(jsonSecret: String, baseKey: String): List<Pair<String, String>> =
        when (val jsonElement = json.decodeFromString(serializer, jsonSecret)) {
            is JsonPrimitive -> listOf(Pair(baseKey, jsonElement.toString()))
            is JsonObject -> serializeJsonObject(jsonElement, baseKey)
            else -> throw IllegalStateException("Secret $baseKey is neither JsonPrimitive nor JsonObject")
        }

    private fun serializeJsonObject(jsonElement: JsonObject, baseKey: String): List<Pair<String, String>> =
        jsonElement.entries
            .map { (secretKey, secretValue) ->
                when (secretValue) {
                    is JsonPrimitive -> "$baseKey.$secretKey" to secretValue.content
                    else -> throw IllegalStateException("Invalid value in secret")
                }
            }
            .toList()
}
