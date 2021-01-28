package util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.MessageDigest

fun String.sha512(): String {
    val digest = MessageDigest.getInstance("SHA-512").digest(toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

fun getLogger(forClass: Class<*>): Logger =
    LoggerFactory.getLogger(forClass)
