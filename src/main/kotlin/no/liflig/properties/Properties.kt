package no.liflig.properties

import java.util.Properties

/**
 * Returns the optional string value of the property given by [key] or null if the property does not
 * exist.
 */
fun Properties.string(key: String): String? = getProperty(key)

/**
 * Returns the string value of the property given by [key]. It might be an empty string.
 *
 * @throws IllegalArgumentException if the property does not exist.
 */
@JvmName("getStringNotNull")
fun Properties.stringNotNull(key: String): String = getProperty(key).require(key)

/**
 * Returns the string value of the property given by [key] with length at least 1.
 *
 * @throws IllegalArgumentException if the property does not exist or the value is empty.
 */
@JvmName("getStringNotEmpty")
fun Properties.stringNotEmpty(key: String): String =
    getProperty(key).require(key).let {
      if (it == "") {
        throw IllegalArgumentException("Property '$key' contains an empty value")
      } else {
        it
      }
    }

/**
 * Returns the integer value of the property given by [key] or null if property does not exist or if
 * string value is blank.
 *
 * @throws IllegalArgumentException if the value is not a valid representation of an integer.
 */
@JvmName("getInt")
fun Properties.int(key: String): Int? =
    getProperty(key)
        ?.takeIf { it.isNotBlank() }
        ?.let {
          try {
            it.toInt()
          } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Property '$key' contains an invalid integer: '$it'", e)
          }
        }

/**
 * Returns the integer value of the property given by [key].
 *
 * @throws IllegalArgumentException if the property does not exist, if value is empty or if or the
 *   value is not a valid representation of an integer.
 */
@JvmName("getIntRequired") fun Properties.intRequired(key: String): Int = int(key).require(key)

/**
 * Returns the long value of the property given by [key] or null if property does not exist or if
 * value is blank.
 *
 * @throws IllegalArgumentException if the value is not a valid representation of a Long.
 */
@JvmName("getLong")
fun Properties.long(key: String): Long? =
    getProperty(key)
        ?.takeIf { it.isNotBlank() }
        ?.let {
          try {
            it.toLong()
          } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Property '$key' contains an invalid long: '$it'", e)
          }
        }

/**
 * Returns the long value of the property given by [key].
 *
 * @throws IllegalArgumentException if the property does not exist, if value is empty or if the
 *   value is not a valid representation of a Long.
 */
@JvmName("getLongRequired") fun Properties.longRequired(key: String): Long = long(key).require(key)

/**
 * Returns `true` if the content of the property given by [key] is equal to the word "true"
 * (ignoring case) or `false` otherwise. Returns null if property is missing or value is blank.
 */
@JvmName("getBoolean")
fun Properties.boolean(key: String): Boolean? =
    getProperty(key)?.takeIf { it.isNotBlank() }?.toBoolean()

/**
 * Returns `true` if the content of the property given by [key] is equal to the word "true"
 * (ignoring case) or `false` otherwise.
 *
 * @throws IllegalArgumentException if the property is missing or value is blank.
 */
@JvmName("getBooleanRequired")
fun Properties.booleanRequired(key: String) = boolean(key).require(key)

private fun <T> T?.require(propertyKey: String): T {
  if (this == null) {
    throw IllegalArgumentException("Property '$propertyKey' is either not found or value is empty")
  }
  return this
}
