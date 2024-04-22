package com.tairitsu.compose.arcaea.serializer

import com.tairitsu.compose.arcaea.ArcNote
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [ArcNote.CurveType]
 */
object ArcNoteCurveTypeSerializer : KSerializer<ArcNote.CurveType> {
    override fun deserialize(decoder: Decoder): ArcNote.CurveType {
        return decoder.decodeString().let {
            ArcNote.CurveType(it)
        }
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("ArcNote.CurveType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ArcNote.CurveType) {
        encoder.encodeString(value.value)
    }
}