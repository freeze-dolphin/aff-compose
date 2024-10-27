package com.tairitsu.compose.arcaea.serializer

import com.tairitsu.compose.arcaea.Difficulty.RatingClass
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [RatingClass]
 */
object DifficultyRatingClassSerializer : KSerializer<RatingClass> {
    override fun deserialize(decoder: Decoder): RatingClass = when (decoder.decodeInt()) {
        0 -> RatingClass.PAST
        1 -> RatingClass.PRESENT
        2 -> RatingClass.FUTURE
        3 -> RatingClass.BEYOND
        4 -> RatingClass.ETERNAL
        else -> throw IllegalArgumentException("Invalid rating class")
    }

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Difficulty.RatingClass", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: RatingClass) {
        encoder.encodeInt(value.rating)
    }
}