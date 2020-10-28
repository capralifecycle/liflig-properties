import no.liflig.properties.GriidPropertiesFetcher
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class GriidPropertiesFetcherTest {
    @Test
    fun `renameKeyAndSerializeValue serializes values correctly`() {
        val expected = listOf(Pair("app.db.password", "eeShee0haiv9"))
        val jsonSecret =
            """{"password": "eeShee0haiv9"}"""
        val baseKey = "app.db"
        val ret = GriidPropertiesFetcher().renameKeyAndSerializeValue(jsonSecret, baseKey)
        assertEquals(expected, ret)
    }

    @Test
    fun `renameKeyAndSerializeValue with invalid secret fails`() {
        val jsonSecret =
            """{"password": [1, 2]}"""
        val baseKey = "app.db"
        assertThrows(IllegalStateException::class.java) {
            GriidPropertiesFetcher().renameKeyAndSerializeValue(jsonSecret, baseKey)
        }
    }
}
