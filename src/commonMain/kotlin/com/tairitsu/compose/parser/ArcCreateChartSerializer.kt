package com.tairitsu.compose.parser

import com.tairitsu.compose.ArcNote
import com.tairitsu.compose.Chart
import com.tairitsu.compose.SerializationContext
import com.tairitsu.compose.TimingGroup

class ArcCreateChartSerializer : ArcaeaChartSerializer() {

    companion object {
        val Instance by lazy { ArcCreateChartSerializer() }
        fun serialize(chart: Chart): List<String> = Instance.serialize(chart)
    }

    override val timingGroupSpecialEffectSeparator = ","

    override fun serializeTimingGroupSpecialEffect(specialEffect: TimingGroup.SpecialEffect): String =
        specialEffect.type.value + (specialEffect.param?.let { "=$it" } ?: "")

    override fun serializeArcResolution(arcNote: ArcNote, arcResolution: Double, ctx: SerializationContext): String {
        if (arcResolution != 1.0) {
            val parentGroupId = if (ctx.timingGroup.name != "main") {
                ctx.chart.subTiming.values.indexOf(ctx.timingGroup)
            } else "main"

            val tgName = "${parentGroupId}_arcResolution${arcResolution}"
            val timingGroup = ctx.chart.postTiming.getOrPut(tgName) { ctx.timingGroup.duplicate(tgName) }

            timingGroup.addSpecialEffect(
                TimingGroup.SpecialEffect(
                    TimingGroup.SpecialEffectType.fromValue("arcresolution"),
                    arcResolution.toString()
                )
            )

            timingGroup.addArcNote(arcNote.copy(arcResolution = 1.0))
        }

        return ""
    }

    override fun serializeArcTaps(arcNote: ArcNote, ctx: SerializationContext): String {
        if (arcNote.arcTapList.isEmpty()) return ""

        return "[" + arcNote.arcTapList.joinToString(",") {
            "arctap(" + it.time.toString() + (if (it.length != null) ",${it.length}" else "") + ")"
        } + "]"
    }
}