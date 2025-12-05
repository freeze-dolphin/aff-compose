@file:OptIn(ExperimentalSerializationApi::class)

package com.tairitsu.compose

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.experimental.xor

private val cbor = Cbor {
    encodeDefaults = true
}

fun Chart.serializeToCbor(encryptActions: ByteArray.() -> Unit): ByteArray {
    val bytes = cbor.encodeToByteArray(this)
    encryptActions.invoke(bytes)
    return bytes
}

fun deserializeFromCbor(bytes: ByteArray, decryptActions: ByteArray.() -> Unit): Chart {
    decryptActions.invoke(bytes)
    return cbor.decodeFromByteArray<Chart>(bytes)
}

fun Chart.serializeToCborWithKey(keyArray: ByteArray): ByteArray = serializeToCbor {
    this.zip(keyArray).map { (bit, kBit) ->
        bit.xor(kBit)
    }.toByteArray()
}

fun deserializeFromCborWithKey(bytes: ByteArray, keyArray: ByteArray): Chart = deserializeFromCbor(bytes) {
    this.zip(keyArray).map { (bit, kBit) ->
        bit.xor(kBit)
    }.toByteArray()
}
