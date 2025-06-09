package no.liflig.properties

import java.io.File
import java.lang.System.getenv
import java.util.Properties
import no.liflig.logging.getLogger

private object PropertiesLoader

private val log = getLogger()

/**
 * Load properties from file(s) and AWS Parameter Store.
 *
 * Loads properties from the given files in order. Values in later files overwrite previous values.
 * - application.properties (from classpath)
 * - overrides.properties (from working directory)
 * - application-test.properties (from classpath)
 * - overrides-test.properties (from working directory)
 *
 * If `SSM_PREFIX` environment variable is given, properties from AWS Parameter Store will be loaded
 * before `overrides.properties`.
 *
 * if [environmentPrefix] is non-null, then environment variables are loaded as properties before
 * properties from AWS Parameter Store. Note that passing secrets as environment variables has
 * security implications since the environment is easily accessible, and passing secrets via files
 * or reading them directly from a secrets manager is preferred.
 *
 * All sources are optional.
 *
 * @param environmentPrefix if non-null, environment variables are filtered according to the prefix
 *   and parsed as properties, converting the names from `PROPERTY_NAME` to `property.name`. These
 *   properties are loaded after `application.properties` and before AWS Parameter Store. If the
 *   prefix is non-empty, the prefix will be stripped from the resulting properties, so
 *   `PREFIX_PROPERTY_NAME` will result in the property `property.name`.
 * @throws PropertyLoadingException when it fails to load a property, for example when a secret
 *   ([SecretLoadingException]) or SSM parameter ([ParameterLoadingException]) is invalid. This
 *   exception has multiple subclasses, for finer grained catching.
 */
@Throws(PropertyLoadingException::class)
@Suppress("unused") // This is the entry point for library consumers.
@JvmOverloads
fun loadProperties(environmentPrefix: String? = null) =
    loadPropertiesInternal(environmentPrefix = environmentPrefix)

// For testing
@Throws(PropertyLoadingException::class)
internal fun loadPropertiesInternal(
    applicationProperties: String = "application.properties",
    applicationTestProperties: String = "application-test.properties",
    overridesProperties: String = "overrides.properties",
    overridesTestProperties: String = "overrides-test.properties",
    griidPropertiesFetcher: GriidPropertiesFetcher = GriidPropertiesFetcher(),
    environmentPrefix: String? = null,
    getenv: (String) -> String? = System::getenv,
    getFullEnv: () -> Map<String, String> = System::getenv,
) =
    Properties()
        .apply {
          putAll(fromClasspath(applicationProperties))
          putAll(fromEnvironment(environmentPrefix, getFullEnv))
          putAll(fromParameterStore(griidPropertiesFetcher, getenv))
          putAll(fromFile(File(overridesProperties)))
          putAll(fromClasspath(applicationTestProperties))
          putAll(fromFile(File(overridesTestProperties)))
        }
        .also { properties -> log.info { "Loaded ${properties.size} properties in total" } }

@Throws(PropertyLoadingException::class)
private fun fromParameterStore(
    griidPropertiesFetcher: GriidPropertiesFetcher,
    getenv: (String) -> String?,
): Properties =
    Properties().apply {
      val ssmPrefixEnvName = "SSM_PREFIX"
      when (val ssmPrefix = getenv(ssmPrefixEnvName)) {
        null ->
            log.info {
              "Environment variable [$ssmPrefixEnvName] not found - no properties loaded from AWS Parameter Store"
            }
        else -> {
          putAll(griidPropertiesFetcher.forPrefix(ssmPrefix))
          log.info {
            "Loaded $size properties from AWS Parameter Store using prefix [$ssmPrefix]. Keys: $keys"
          }
        }
      }
    }

/**
 * Load properties from a file in classpath if it exists or else return an empty properties list.
 */
private fun fromClasspath(filename: String): Properties =
    Properties().apply {
      when (val resource = PropertiesLoader.javaClass.classLoader.getResourceAsStream(filename)) {
        null -> log.info { "File [$filename] not found on classpath - no properties loaded" }
        else -> {
          resource.reader().use(::load)
          log.info { "Loaded $size properties from [$filename] on classpath. Keys: $keys" }
        }
      }
    }

/**
 * Load properties from a file from working directory if it exists or else return an empty
 * properties list.
 */
private fun fromFile(file: File): Properties =
    Properties().apply {
      if (file.exists()) {
        file.reader().use(::load)
        log.info { "Loaded $size properties from [${file.path}] in working directory. Keys: $keys" }
      } else {
        log.info { "File [${file.path}] not found in working directory - no properties loaded" }
      }
    }

private fun fromEnvironment(
    environmentPrefix: String?,
    getFullEnv: () -> Map<String, String>
): Properties {
  if (environmentPrefix != null) {
    val props =
        getFullEnv()
            .filterKeys { key -> key.startsWith(environmentPrefix) }
            .entries
            .associate { entry ->
              val key =
                  entry.key
                      .removePrefix(environmentPrefix)
                      .removePrefix("_")
                      .replace("_", ".")
                      .lowercase()
              key to entry.value
            }
            .toProperties()
    log.info {
      "Loaded ${props.size} properties from environment using prefix [$environmentPrefix]. Keys: ${props.keys}"
    }
    return props
  } else {
    log.info { "Environment variable prefix not set - no properties loaded" }
    return Properties()
  }
}
