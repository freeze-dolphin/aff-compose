package com.tairitsu.compose.arcaea.serializer

import com.tairitsu.compose.arcaea.ArcNote
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [ArcNote.Color]
 */
object ArcNoteColorSerializer : KSerializer<ArcNote.Color> {
    override fun deserialize(decoder: Decoder): ArcNote.Color {
        return decoder.decodeInt().let {
            ArcNote.Color(it)
        }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ArcNote.Color", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: ArcNote.Color) {
        encoder.encodeInt(value.value)
    }
}