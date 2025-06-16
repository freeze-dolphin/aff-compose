package com.tairitsu.compose.arcaea.serializer

import com.tairitsu.compose.arcaea.Camera
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [Camera.CameraEaseType]
 */
object CameraEaseTypeSerializer : KSerializer<Camera.CameraEaseType> {
    override fun deserialize(decoder: Decoder): Camera.CameraEaseType {
        return decoder.decodeString().let {
            Camera.CameraEaseType(it)
        }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Camera.CameraEaseType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Camera.CameraEaseType) {
        encoder.encodeString(value.value)
    }
}