package no.liflig.properties

import java.util.Properties

/**
 * Returns the string value of the property given by [key]. It might be an empty string.
 * @throws IllegalArgumentException if the property does not exist.
 */
@JvmName("getStringNotNull")
fun Properties.stringNotNull(key: String): String = getProperty(key).require(key)

/**
 * Returns the string value of the property given by [key] with length at least 1.
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
 * Returns the integer value of the property given by [key].
 * @throws IllegalArgumentException if the value is not a valid representation of a number.
 */
@JvmName("getInt")
fun Properties.int(key: String): Int? =
    getProperty(key)?.let {
        try {
            it.toInt()
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Property '$key' contains an invalid integer: '$it'", e)
        }
    }

/**
 * Returns the integer value of the property given by [key].
 * @throws IllegalArgumentException if the property does not exist or the value is not a valid representation of a number.
 */
@JvmName("getIntRequired")
fun Properties.intRequired(key: String): Int = int(key).require(key)

/**
 * Returns the long value of the property given by [key].
 * @throws IllegalArgumentException if the value is not a valid representation of a number.
 */
@JvmName("getLong")
fun Properties.long(key: String): Long? = getProperty(key)?.let {
    try {
        it.toLong()
    } catch (e: NumberFormatException) {
        throw IllegalArgumentException("Property '$key' contains an invalid long: '$it'", e)
    }
}

/**
 * Returns the long value of the property given by [key].
 * @throws IllegalArgumentException if the property does not exist or the value is not a valid representation of a number.
 */
@JvmName("getLongRequired")
fun Properties.longRequired(key: String): Long = long(key).require(key)

/**
 * Returns `true` if the content of the property given by [key] is equal to the word "true", ignoring case, and `false` otherwise.
 */
@JvmName("getBoolean")
fun Properties.boolean(key: String): Boolean? = getProperty(key)?.toBoolean()

/**
 * Returns `true` if the content of the property given by [key] is equal to the word "true", ignoring case, and `false` otherwise.
 * @throws IllegalArgumentException if the property does not exist.
 */
@JvmName("getBooleanRequired")
fun Properties.booleanRequired(key: String) = boolean(key).require(key)

private fun <T> T?.require(propertyKey: String): T {
    if (this == null) {
        throw IllegalArgumentException("Property '$propertyKey' not found")
    }
    return this
}
