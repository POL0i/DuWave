package com.example.beatpulse.utils

import java.security.MessageDigest

actual fun sha256Hash(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
