package com.tairitsu.compose.json_serializer

import com.tairitsu.compose.Position
import com.tairitsu.compose.Position.Companion.pos
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializer for [Position]
 */
object PositionSerializer : KSerializer<Position> {

    val serializer = ListSerializer(Double.serializer())

    override fun deserialize(decoder: Decoder): Position =
        serializer.deserialize(decoder).let {
            it[0] pos it[1]
        }


    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Position) {
        serializer.serialize(encoder, value.toList())
    }
}