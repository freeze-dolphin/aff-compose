package com.tairitsu.compose.parser

import com.tairitsu.compose.*
import com.tairitsu.compose.Position.Companion.pos

open class ArcaeaChartSerializer : ChartSerializer {

    companion object {
        val Instance by lazy { ArcaeaChartSerializer() }
        fun serialize(chart: Chart): List<String> = Instance.serialize(chart)
    }

    open val headerSerializer: (Chart.Configuration) -> MutableList<String> = { chartConfig ->
        mutableListOf<String>().apply {
            add("AudioOffset:${chartConfig.audioOffset}")

            chartConfig.extra.forEach { (name, value) ->
                add("$name:$value")
            }

            add("-")
        }
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

    open val timingGroupSpecialEffectSeparator: CharSequence = "_"

    open val timingGroupSpecialEffectFilter: (TimingGroup.SpecialEffect) -> Boolean =
        { it.type in TimingGroup.SpecialEffectType.getVanillaSpecialEffectTypes() } // only vanilla fx are allowed

    override fun serializeTimingGroup(timingGroup: TimingGroup, ctx: SerializationContext): List<String> =
        mutableListOf<String>().apply tg@{
            val isMainTimingGroup = timingGroup.name == "main"

            if (!isMainTimingGroup)
                add(
                    "timinggroup(" +
                            timingGroup
                                .getSpecialEffects()
                                .filter(timingGroupSpecialEffectFilter)
                                .joinToString(timingGroupSpecialEffectSeparator) { serializeTimingGroupSpecialEffect(it) } +
                            "){"
                )

            mutableListOf<TimedObject>().apply obj@{
                addAll(timingGroup.getTimings())
                addAll(timingGroup.getNotes())
                addAll(timingGroup.getScenecontrols())
                addAll(timingGroup.getCameras())
                sortedWith(TimedObject.Sorter).forEach { timedObject ->
                    serializeTimedObject(timedObject, SerializationContext(ctx.chart, timingGroup)).forEach {
                        this@tg.add(
                            " ".repeat(if (isMainTimingGroup) 0 else 2)
                                    + it
                        )
                    }
                }
            }

            if (!isMainTimingGroup) add("};")
        }

    override fun serializeTimedObject(timedObject: TimedObject, ctx: SerializationContext): List<String> = listOf(
        when (timedObject) {
            is Timing -> serializeTiming(timedObject, ctx)

            is NormalNote -> serializeNormalNote(timedObject, ctx)
            is HoldNote -> serializeHoldNote(timedObject, ctx)
            is ArcNote -> serializeArcNote(timedObject, ctx)

            is Scenecontrol -> serializeScenecontrol(timedObject, ctx)
            is Camera -> serializeCamera(timedObject, ctx)

            else -> error("Unsupported TimedObject: $timedObject at time ${timedObject.time}")
        }
    ).filter {
        it.isNotEmpty() // exclude empty lines of dropped notes
    }

    open fun serializeTiming(timedObject: Timing, ctx: SerializationContext): String = timedObject.run {
        "timing(${offset},${bpm.toAffFormat()},${beats.toAffFormat()});"
    }

    open fun serializeNormalNote(timedObject: NormalNote, ctx: SerializationContext): String = timedObject.run {
        "(${time},${serializeColumn()});"
    }

    open fun serializeHoldNote(timedObject: HoldNote, ctx: SerializationContext): String = timedObject.run {
        "hold(${time},${endTime},${serializeColumn()});"
    }

    open fun serializeArcNote(timedObject: ArcNote, ctx: SerializationContext): String = timedObject.run {
        val serializedArcResolution = serializeArcResolution(this, arcResolution, ctx)
        if (serializedArcResolution == Double.NaN.toString()) {
            return ""
        }

        "arc(${time}," +
                "${endTime}," +
                "${startPosition.x.toAffFormat()}," +
                "${endPosition.x.toAffFormat()}," +
                "${easeType.value}," +
                "${startPosition.y.toAffFormat()}," +
                "${endPosition.y.toAffFormat()}," +
                "${color.value}," +
                "${serializeHitSound(hitSound, ctx)}," +
                arcType.value +
                serializedArcResolution +
                ")" +
                serializeArcTaps(this, ctx) +
                ";"
    }

    open fun serializeScenecontrol(timedObject: Scenecontrol, ctx: SerializationContext): String = timedObject.run {
        if (type !in ScenecontrolType.getVanillaScenecontrolTypes()) {
            return ""
        }

        "scenecontrol(${time}," +
                "${type.value}," +
                params.joinToString(",") +
                ");"
    }

    open fun serializeCamera(timedObject: Camera, ctx: SerializationContext): String = timedObject.run {
        "camera(${timedObject.time}," +
                "${timedObject.xOff.toAffFormat()}," +
                "${timedObject.yOff.toAffFormat()}," +
                "${timedObject.zOff.toAffFormat()}," +
                "${timedObject.xozAng.toAffFormat()}," +
                "${timedObject.yozAng.toAffFormat()}," +
                "${timedObject.xoyAng.toAffFormat()}," +
                "${timedObject.ease.value}," +
                "${timedObject.duration}" +
                ");"
    }

    open fun serializeTimingGroupSpecialEffect(specialEffect: TimingGroup.SpecialEffect): String =
        specialEffect.type.value + (specialEffect.param ?: "")

    open fun serializeHitSound(hitSound: String, ctx: SerializationContext): String = hitSound

    open fun serializeArcResolution(arcNote: ArcNote, arcResolution: Double, ctx: SerializationContext): String =
        if (arcResolution > 1.0)
            ",${arcResolution.toAffFormat()}"
        else ""

    open fun serializeArcTaps(arcNote: ArcNote, ctx: SerializationContext): String {
        if (arcNote.arcTapList.isEmpty()) return "" // no arctap, just return

        val serialized = mutableListOf<String>()

        arcNote.arcTapList.forEach { arctap ->
            if (arctap.length != null) { // handle var-len arctap
                val centerPos = arcNote.getPositionAt(arctap.time) // calculate absolute position for this arctap

                val tgName = "__internal_vlArcTapConv_${ctx.timingGroup.name}"
                val timingGroup = ctx.chart.postTiming.getOrPut(tgName) { ctx.timingGroup.duplicate(tgName) }

                val radius = arctap.length / 2

                timingGroup.apply {
                    addArcNote(
                        ArcNote(
                            arctap.time,
                            arctap.time + 1,
                            centerPos.x - radius pos centerPos.y,
                            ArcNote.EaseType.S,
                            centerPos.x + radius pos centerPos.y,
                            ArcNote.Color.GRAY,
                            ArcNote.NoteType.ARC,
                            hitSound = arcNote.hitSound,
                        )
                    )
                }
            } else {
                serialized.add("arctap(${arctap.time})")
            }
        }

        if (serialized.isEmpty()) return ""

        return "[" + serialized.joinToString(",") + "]"
    }
}