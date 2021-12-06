package no.liflig.properties

import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

private object PropertiesLoader

private val logger = LoggerFactory.getLogger(PropertiesLoader::class.java)

/**
 * Load properties from file(s) and AWS Parameter Store.
 *
 * Loads properties from the given files in order. Values in later files overwrite previous values.
 *
 * - application.properties (from classpath)
 * - overrides.properties (from working directory)
 * - application-test.properties (from classpath)
 * - overrides-test.properties (from working directory)
 *
 * If SSM_PREFIX environment variable is given, properties from AWS Parameter Store
 * will be loaded before overrides.properties.
 *
 * All sources are optional.
 *
 * @throws PropertyLoadingException when it fails to load a property, for example
 * when a secret ([SecretLoadingException]) or SSM parameter ([ParameterLoadingException]) is invalid.
 * This exception has multiple subclasses, for finer grained catching.
 */
@Throws(PropertyLoadingException::class)
@Suppress("unused" /* This is the entry point for library consumers. */)
fun loadProperties() = loadPropertiesInternal()

// For testing
@Throws(PropertyLoadingException::class)
internal fun loadPropertiesInternal(
    applicationProperties: String = "application.properties",
    applicationTestProperties: String = "application-test.properties",
    overridesProperties: String = "overrides.properties",
    overridesTestProperties: String = "overrides-test.properties",
    griidPropertiesFetcher: GriidPropertiesFetcher = GriidPropertiesFetcher(),
    getenv: (String) -> String? = System::getenv
) =
    Properties()
        .apply {
            putAll(fromClasspath(applicationProperties))
            putAll(fromParameterStore(griidPropertiesFetcher, getenv))
            putAll(fromFile(File(overridesProperties)))
            putAll(fromClasspath(applicationTestProperties))
            putAll(fromFile(File(overridesTestProperties)))
        }
        .also { logger.info("Loaded ${it.size} properties in total") }

@Throws(PropertyLoadingException::class)
private fun fromParameterStore(
    griidPropertiesFetcher: GriidPropertiesFetcher,
    getenv: (String) -> String?
): Properties =
    Properties().apply {
        val ssmPrefixEnvName = "SSM_PREFIX"
        when (val ssmPrefix = getenv(ssmPrefixEnvName)) {
            null -> logger.info(
                "Environment variable [$ssmPrefixEnvName] not found - no properties loaded from AWS Parameter Store"
            )
            else -> {
                putAll(griidPropertiesFetcher.forPrefix(ssmPrefix))
                logger.info("Loaded $size properties from AWS Parameter Store using prefix [$ssmPrefix]. Keys: $keys")
            }
        }
    }

/**
 * Load properties from a file in classpath if it exists or else
 * return an empty properties list.
 */
private fun fromClasspath(filename: String): Properties =
    Properties().apply {
        when (val resource = PropertiesLoader.javaClass.classLoader.getResourceAsStream(filename)) {
            null -> logger.info("File [$filename] not found on classpath - no properties loaded")
            else -> {
                resource.reader().use(::load)
                logger.info("Loaded $size properties from [$filename] on classpath. Keys: $keys")
            }
        }
    }

/**
 * Load properties from a file from working directory if it exists or else
 * return an empty properties list.
 */
private fun fromFile(file: File): Properties =
    Properties().apply {
        if (file.exists()) {
            file.reader().use(::load)
            logger.info("Loaded $size properties from [${file.path}] in working directory. Keys: $keys")
        } else {
            logger.info("File [${file.path}] not found in working directory - no properties loaded")
        }
    }
