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
 *
 * If SSM_PREFIX environment variable is given, properties from AWS Parameter Store
 * will be loaded before overrides.properties.
 *
 * All sources are optional.
 */
fun loadProperties() = loadPropertiesInternal()

// For testing
internal fun loadPropertiesInternal(
    applicationProperties: String = "application.properties",
    applicationTestProperties: String = "application-test.properties",
    overridesProperties: String = "overrides.properties",
    griidPropertiesFetcher: GriidPropertiesFetcher = GriidPropertiesFetcher(),
    getenv: (String) -> String? = System::getenv
) =
    Properties().apply {
        putAll(fromClasspath(applicationProperties))
        putAll(fromParameterStore(griidPropertiesFetcher, getenv))
        putAll(fromFile(File(overridesProperties)))
        putAll(fromClasspath(applicationTestProperties))
    }

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
                logger.info("Loaded $size properties from AWS Parameter Store using prefix [$ssmPrefix]")
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
                logger.info("Loaded $size properties from [$filename] on classpath")
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
            logger.info("Loaded $size properties from [${file.path}] in working directory")
        } else {
            logger.info("File [${file.path}] not found in working directory - no properties loaded")
        }
    }
