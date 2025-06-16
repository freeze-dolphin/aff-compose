package com.tairitsu.compose.arcaea.serializer

import com.tairitsu.compose.arcaea.ArcNote
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ArcTapListSerializer : KSerializer<ArcNote.ArcTapList> {

    val serializer = ListSerializer(Long.serializer())

    override val descriptor: SerialDescriptor
        get() = serializer.descriptor

    override fun deserialize(decoder: Decoder): ArcNote.ArcTapList {
        return ArcNote.ArcTapList(serializer.deserialize(decoder).toMutableList())
    }

    override fun serialize(encoder: Encoder, value: ArcNote.ArcTapList) {
        serializer.serialize(encoder, value.data)
    }

}
