package no.liflig.properties

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

// The properties files in this project is named so that by default no files
// matches the conventions (meaning no properties gets loaded by default).
// We override file names in tests to simulate environments matching a specific set of files.
class PropertiesLoaderTest {
  @Test
  fun `should load properties for normal runtime with SSM params`() {
    val awsPath = "/construct/current"
    val mockProperties = mapOf("hacker.name" to "Henrik").toProperties()

    val griidPropertiesFetcher =
        mockk<GriidPropertiesFetcher> { every { forPrefix(awsPath) } returns mockProperties }

    val properties =
        loadPropertiesInternal(
            applicationProperties = "testdata/application.properties",
            griidPropertiesFetcher = griidPropertiesFetcher,
            getenv = { awsPath },
        )

    assertEquals("Henrik", properties.getProperty("hacker.name"))
  }

  @Test
  fun `loads invalid files`() {
    // Java properties loading does not perform any validation,
    // leading to some strange/unexpected results if properties being
    // loaded is not following the expected format, instead of failing.
    val properties =
        loadPropertiesInternal(
            applicationProperties = "testdata/invalid.properties",
        )
    assertEquals("Smith", properties.getProperty("hacker.nameAgent"))
  }

  @Test
  fun `all sources are optional`() {
    val properties = loadPropertiesInternal()
    assertEquals(0, properties.size)
  }

  @Test
  fun `an overrides file have precedence over the default application properties`() {
    val properties =
        loadPropertiesInternal(
            applicationProperties = "testdata/application.properties",
            overridesProperties = "test-assets/overrides.properties",
        )
    assertEquals("Morpheus", properties.getProperty("hacker.name"))
  }

  @Test
  fun `test properties have precedence over other properties`() {
    val awsPath = "/construct/current"
    val griidPropertiesFetcher =
        mockk<GriidPropertiesFetcher> {
          every { forPrefix(awsPath) } returns mapOf("hacker.name" to "Henrik").toProperties()
        }

    val properties =
        loadPropertiesInternal(
            applicationProperties = "testdata/application.properties",
            applicationTestProperties = "testdata/application-test.properties",
            overridesProperties = "test-assets/overrides.properties",
            griidPropertiesFetcher = griidPropertiesFetcher,
            getenv = { awsPath },
        )
    assertEquals("Trinity", properties.getProperty("hacker.name"))
  }

  @Test
  fun `test overrides properties have precedence over all other properties`() {
    val awsPath = "/construct/current"
    val griidPropertiesFetcher =
        mockk<GriidPropertiesFetcher> {
          every { forPrefix(awsPath) } returns mapOf("hacker.name" to "Henrik").toProperties()
        }

    val properties =
        loadPropertiesInternal(
            applicationProperties = "testdata/application.properties",
            applicationTestProperties = "testdata/application-test.properties",
            overridesProperties = "test-assets/overrides.properties",
            overridesTestProperties = "test-assets/overrides-test.properties",
            griidPropertiesFetcher = griidPropertiesFetcher,
            getenv = { awsPath },
        )
    assertEquals("Dozer", properties.getProperty("hacker.name"))
  }

  @Test
  fun `should load environment variables when app prefix is set`() {
    val properties =
        loadPropertiesInternal(
            environmentPrefix = "APP",
            getFullEnv = {
              mapOf(
                  "APP_FOO_BAR" to "fb",
                  "APP_ZIG_ZAG" to "zz",
              )
            },
        )
    assertEquals(2, properties.size)
    assertEquals("fb", properties.getProperty("foo.bar"))
    assertEquals("zz", properties.getProperty("zig.zag"))
  }

  @Test
  fun `should load environment variables when app prefix is empty`() {
    val properties =
        loadPropertiesInternal(
            environmentPrefix = "",
            getFullEnv = {
              mapOf(
                  "SOME_OTHER_VAR" to "val",
                  "APP_FOO_BAR" to "fb",
                  "APP_ZIG_ZAG" to "zz",
              )
            },
        )
    assertEquals(3, properties.size)
    assertEquals("fb", properties.getProperty("app.foo.bar"))
    assertEquals("zz", properties.getProperty("app.zig.zag"))
  }

  @Test
  fun `parameter store should override environment variables`() {
    val awsPath = "/construct/current"
    val griidPropertiesFetcher =
        mockk<GriidPropertiesFetcher> {
          every { forPrefix(awsPath) } returns mapOf("foo.bar" to "fromssm").toProperties()
        }
    val properties =
        loadPropertiesInternal(
            griidPropertiesFetcher = griidPropertiesFetcher,
            environmentPrefix = "APP",
            getenv = { awsPath },
            getFullEnv = {
              mapOf(
                  "APP_FOO_BAR" to "fb",
                  "APP_ZIG_ZAG" to "zz",
              )
            },
        )
    assertEquals(2, properties.size)
    assertEquals("fromssm", properties.getProperty("foo.bar"))
    assertEquals("zz", properties.getProperty("zig.zag"))
  }
}
