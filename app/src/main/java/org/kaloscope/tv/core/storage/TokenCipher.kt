package org.kaloscope.tv.core.storage

/**
 * Protects session tokens with device-bound cryptographic keys.
 */
interface TokenCipher {
    fun encrypt(value: String): String

    fun decrypt(value: String): String?
}
