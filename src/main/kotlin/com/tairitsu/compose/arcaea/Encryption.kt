package com.tairitsu.compose.arcaea

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private fun decrypt(algorithm: String, cipherText: String, key: SecretKeySpec, iv: IvParameterSpec): String {
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.DECRYPT_MODE, key, iv)
    val plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText))
    return String(plainText)
}

private fun encrypt(algorithm: String, inputText: String, key: SecretKeySpec, iv: IvParameterSpec): String {
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.ENCRYPT_MODE, key, iv)
    val cipherText = cipher.doFinal(inputText.toByteArray())
    return Base64.getEncoder().encodeToString(cipherText)
}

object Encryption {
    fun Chart.encryptText(algorithm: String, key: SecretKeySpec, iv: IvParameterSpec): String {
        return encrypt(algorithm, this.serialize(), key, iv)
    }

    fun decryptTextChart(encryptedText: String, algorithm: String, key: SecretKeySpec, iv: IvParameterSpec): Chart {
        return Chart.fromAff(decrypt(algorithm, encryptedText, key, iv))
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val cbor = Cbor {}

    @OptIn(ExperimentalSerializationApi::class)
    fun Chart.serializeToCbor(encryptActions: ByteArray.() -> Unit): ByteArray {
        val bytes = cbor.encodeToByteArray(this)
        encryptActions.invoke(bytes)
        return bytes
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun deserializeFromCbor(bytes: ByteArray, decryptActions: ByteArray.() -> Unit): Chart {
        decryptActions.invoke(bytes)
        return cbor.decodeFromByteArray<Chart>(bytes)
    }

}