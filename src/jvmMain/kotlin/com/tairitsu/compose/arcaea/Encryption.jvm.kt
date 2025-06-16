package com.tairitsu.compose.arcaea

import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

actual fun Chart.encryptText(algorithm: String, key: ByteArray): String {
    val skey = SecretKeySpec(key, algorithm)
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.ENCRYPT_MODE, skey)
    val cipherText = cipher.doFinal(this.serializeForArcaea().toByteArray())
    return Base64.getEncoder().encodeToString(cipherText)
}

actual fun decryptTextChart(encryptedText: String, algorithm: String, key: ByteArray): Chart {
    val skey = SecretKeySpec(key, algorithm)
    val cipher = Cipher.getInstance(algorithm)
    cipher.init(Cipher.DECRYPT_MODE, skey)
    val plainText = cipher.doFinal(Base64.getDecoder().decode(encryptedText))
    return Chart.fromAff(String(plainText))
}