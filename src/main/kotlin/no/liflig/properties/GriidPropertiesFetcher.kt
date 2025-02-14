package no.liflig.properties

import java.util.Properties
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class GriidPropertiesFetcher {

  @Throws(PropertyLoadingException::class)
  fun forPrefix(ssmPrefix: String): Properties =
      Properties().apply {
        putAll(parametersByPath(ssmPrefix))
        putAll(secretsByPath(ssmPrefix))
      }

  @Throws(ParameterLoadingException::class, SecretLoadingException::class)
  private fun parametersByPath(path: String): Map<String, String> =
      AwsClientHelper.getParametersByPath("$path/config/").mapKeys { (key, _) ->
        key.removePrefix("$path/config/")
      }

  @Throws(ParameterLoadingException::class, SecretLoadingException::class)
  private fun secretsByPath(path: String): Map<String, String> =
      AwsClientHelper.getParametersByPath("$path/secrets/")
          .flatMap { (parameterKey, parameterValue) ->
            val baseKey = parameterKey.removePrefix("$path/secrets/")
            val secret = AwsClientHelper.getSecret(parameterValue)

            renameKeyAndSerializeValue(secret, baseKey)
          }
          .toMap()

  internal fun renameKeyAndSerializeValue(
      secret: String,
      baseKey: String,
  ): List<Pair<String, String>> {
    val jsonElement =
        try {
          Json.decodeFromString<JsonElement>(secret)
        } catch (_: SerializationException) {
          // secret was not json, return raw string
          return listOf(baseKey to secret)
        }

    return when (jsonElement) {
      is JsonPrimitive -> listOf(Pair(baseKey, jsonElement.toString()))
      is JsonObject -> serializeJsonObject(jsonElement, baseKey)
      else -> throw IllegalStateException("Secret $baseKey is neither JsonPrimitive nor JsonObject")
    }
  }

  private fun serializeJsonObject(
      jsonElement: JsonObject,
      baseKey: String,
  ): List<Pair<String, String>> =
      jsonElement.entries
          .map { (secretKey, secretValue) ->
            when (secretValue) {
              is JsonPrimitive -> "$baseKey.$secretKey" to secretValue.content
              else -> throw IllegalStateException("Invalid value in secret")
            }
          }
          .toList()
}
