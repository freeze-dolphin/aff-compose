package com.tairitsu.compose.parser

import com.tairitsu.compose.ArcNote
import com.tairitsu.compose.Chart
import com.tairitsu.compose.SerializationContext
import com.tairitsu.compose.TimingGroup

class ArcCreateChartSerializer : ArcaeaChartSerializer() {

    companion object {
        val Instance by lazy { ArcCreateChartSerializer() }
    }

    override fun serialize(chart: Chart): List<String> = mutableListOf<String>().apply {
        addAll(headerSerializer(chart.configuration))
        addAll(serializeTimingGroup(chart.mainTiming, SerializationContext(chart, chart.mainTiming)))
        chart.subTiming.forEach { (_, timingGroup) ->
            addAll(serializeTimingGroup(timingGroup, SerializationContext(chart, timingGroup)))
        }
        chart.postTiming.forEach { (_, timingGroup) ->
            addAll(serializeTimingGroup(timingGroup, SerializationContext(chart, timingGroup)))
        }
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
            val timingGroup = ctx.chart.postTiming.getOrPut(tgName) { TimingGroup(tgName) }

            // newly generated tg
            if (timingGroup.getSpecialEffects().isEmpty()) {
                // duplicate all timings
                ctx.timingGroup.getTimings().forEach { timingGroup.addTiming(it) }

                // duplicate all fx
                ctx.timingGroup.getSpecialEffects().forEach { timingGroup.addSpecialEffect(it) }

                // add arcresolution fx
                timingGroup.addSpecialEffect(
                    TimingGroup.SpecialEffect(
                        TimingGroup.SpecialEffectType.fromValue("arcresolution"),
                        arcResolution.toString()
                    )
                )
            }

            timingGroup.addArcNote(arcNote.copy(arcResolution = 1.0))
        }

        return ""
    }


}