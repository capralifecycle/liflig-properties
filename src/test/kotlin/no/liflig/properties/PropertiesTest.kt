package no.liflig.properties

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.Properties
import kotlin.test.assertEquals

class PropertiesTest {

    @Nested
    inner class ForString {
        @Test
        fun `stringNotNull with an existing property`() {
            assertEquals("bar", mapOf("foo" to "bar").toProperties().stringNotNull("foo"))
        }

        @Test
        fun `stringNotNull can give a empty string`() {
            assertEquals("", mapOf("foo" to "").toProperties().stringNotNull("foo"))
        }

        @Test
        fun `stringNotNull throws when the property does not exist`() {
            val exception = assertThrows<IllegalArgumentException> {
                Properties().stringNotNull("foo")
            }
            assertEquals("Property 'foo' not found", exception.message)
        }

        @Test
        fun `stringNotEmpty with an existing property`() {
            assertEquals("bar", mapOf("foo" to "bar").toProperties().stringNotEmpty("foo"))
        }

        @Test
        fun `stringNotEmpty throws on empty string`() {
            val exception = assertThrows<IllegalArgumentException> {
                mapOf("foo" to "").toProperties().stringNotEmpty("foo")
            }
            assertEquals("Property 'foo' contains an empty value", exception.message)
        }

        @Test
        fun `stringNotEmpty throws when the property does not exist`() {
            val exception = assertThrows<IllegalArgumentException> {
                Properties().stringNotEmpty("foo")
            }
            assertEquals("Property 'foo' not found", exception.message)
        }
    }

    @Nested
    inner class ForInt {
        @Test
        fun `int for a valid value`() {
            assertEquals(123, mapOf("foo" to "123").toProperties().int("foo"))
        }

        @Test
        fun `int gives null on missing property`() {
            assertEquals(null, Properties().int("foo"))
        }

        @Test
        fun `int throws on empty value`() {
            val exception = assertThrows<IllegalArgumentException> {
                mapOf("foo" to "").toProperties().int("foo")
            }
            assertEquals("Property 'foo' contains an invalid integer: ''", exception.message)
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "bar",
                "123.0",
                "123.3",
            ]
        )
        fun `int throws on invalid value`(input: String) {
            val exception = assertThrows<IllegalArgumentException> {
                mapOf("foo" to input).toProperties().int("foo")
            }
            assertEquals("Property 'foo' contains an invalid integer: '$input'", exception.message)
        }

        @Test
        fun `intRequired throws when the property does not exist`() {
            val exception = assertThrows<IllegalArgumentException> {
                Properties().intRequired("foo")
            }
            assertEquals("Property 'foo' not found", exception.message)
        }
    }

    @Nested
    inner class ForLong {
        @Test
        fun `long for a valid value`() {
            assertEquals(123, mapOf("foo" to "123").toProperties().long("foo"))
        }

        @Test
        fun `long gives null on missing property`() {
            assertEquals(null, Properties().long("foo"))
        }

        @Test
        fun `long throws on empty value`() {
            val exception = assertThrows<IllegalArgumentException> {
                mapOf("foo" to "").toProperties().long("foo")
            }
            assertEquals("Property 'foo' contains an invalid long: ''", exception.message)
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "bar",
                "123.0",
                "123.3",
            ]
        )
        fun `long throws on invalid value`(input: String) {
            val exception = assertThrows<IllegalArgumentException> {
                mapOf("foo" to input).toProperties().long("foo")
            }
            assertEquals("Property 'foo' contains an invalid long: '$input'", exception.message)
        }

        @Test
        fun `longRequired throws when the property does not exist`() {
            val exception = assertThrows<IllegalArgumentException> {
                Properties().longRequired("foo")
            }
            assertEquals("Property 'foo' not found", exception.message)
        }
    }

    @Nested
    inner class ForBoolean {
        @ParameterizedTest
        @CsvSource(
            value = [
                "true,true",
                "true,TRue",
                "false,false",
                "false,falSE",
                "false,1",
                "false,yes",
                "false,0",
            ]
        )
        fun `boolean always resolves to true or false on value`(expected: Boolean, input: String) {
            assertEquals(expected, mapOf("foo" to input).toProperties().boolean("foo"))
        }

        @Test
        fun `boolean gives null on missing property`() {
            assertEquals(null, Properties().boolean("foo"))
        }

        @Test
        fun `boolean gives false on empty value`() {
            assertEquals(false, mapOf("foo" to "").toProperties().boolean("foo"))
        }

        @Test
        fun `booleanRequired throws when the property does not exist`() {
            val exception = assertThrows<IllegalArgumentException> {
                Properties().booleanRequired("foo")
            }
            assertEquals("Property 'foo' not found", exception.message)
        }
    }
}
