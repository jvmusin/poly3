package util

import java.security.MessageDigest
import java.util.Base64

fun String.sha512(): String {
    val digest = MessageDigest.getInstance("SHA-512").digest(toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

fun String.encodeBase64(): String = Base64.getEncoder().encodeToString(toByteArray())
fun String.decodeBase64(): String = Base64.getDecoder().decode(this).decodeToString()
