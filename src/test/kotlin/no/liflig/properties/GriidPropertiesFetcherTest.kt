package no.liflig.properties

import kotlin.test.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class GriidPropertiesFetcherTest {
  @Test
  fun `renameKeyAndSerializeValue serializes values correctly`() {
    val expected = listOf(Pair("app.db.password", "eeShee0haiv9"))
    val jsonSecret = """{"password": "eeShee0haiv9"}"""
    val baseKey = "app.db"
    val ret = GriidPropertiesFetcher().renameKeyAndSerializeValue(jsonSecret, baseKey)
    assertEquals(expected, ret)
  }

  @Test
  fun `renameKeyAndSerializeValue with invalid secret fails`() {
    val jsonSecret = """{"password": [1, 2]}"""
    val baseKey = "app.db"
    assertThrows(IllegalStateException::class.java) {
      GriidPropertiesFetcher().renameKeyAndSerializeValue(jsonSecret, baseKey)
    }
  }

  @Test
  fun `renameKeyAndSerializeValue with array fails`() {
    val jsonSecret = """["value1","value2"]"""
    val baseKey = "app.db"
    assertThrows(IllegalStateException::class.java) {
      GriidPropertiesFetcher().renameKeyAndSerializeValue(jsonSecret, baseKey)
    }
  }

  @Test
  fun `renameKeyAndSerializeValue serializes arbitrary strings correctly`() {
    val key = "app.someSecret"
    val password = "passordWithSpecialCha]r}acters\"{:[]}"

    val expected = listOf(Pair(key, password))
    val actual = GriidPropertiesFetcher().renameKeyAndSerializeValue(password, key)
    assertEquals(expected, actual)
  }
}
