package util

import java.security.MessageDigest
import java.util.Base64

/** Calculates `SHA512` hash of `this` [String]. */
fun String.sha512(): String {
    val digest = MessageDigest.getInstance("SHA-512").digest(toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

/** Encodes `this` [String] to [Base64] string representation. */
fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())

/** Decodes `this` [Base64] string representation to an ordinary [String]. */
fun String.decodeBase64(): String = Base64.getDecoder().decode(this).decodeToString()
