package org.kaloscope.tv.core.storage

interface TokenCipher {
    fun encrypt(value: String): String

    fun decrypt(value: String): String?
}
