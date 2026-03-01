package com.tairitsu.compose.parser

import com.tairitsu.compose.*
import com.tairitsu.compose.Position.Companion.pos

/**
 * Simple implementation of chart parser for ArcCreate
 * macro function `fragment(...);` is not supported
 */
open class SimpleArcCreateChartParser : ArcaeaChartParser() {

    companion object {
        val Instance by lazy { SimpleArcCreateChartParser() }
        fun parse(content: String): Chart = Instance.parse(content)
    }

    override val arcTapNoteParser: (UniversalChartVisitor.Event, MutableList<ArcTapNote>) -> Unit = { evt, arcTapList ->
        arcTapList.run {
            when (evt.name) {
                "arctap" -> {
                    require(evt.values.size in 1..2)
                    require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                    if (evt.values.size == 2) {
                        require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                        // arctap width in ArcCreate is doubled
                        arctap(evt.values[0].num.toLong(), evt.values[1].num.toDouble() / 2)
                    } else {
                        val time = evt.values[0].num
                        arctap(time)
                    }
                }
            }
        }
    }

    @Suppress("DuplicatedCode")
    override val arcNoteParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size in 10..11)
                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[2].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[3].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[4].type == UniversalChartVisitor.ValueType.STRING)
                require(evt.values[5].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[6].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[7].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[8].type == UniversalChartVisitor.ValueType.STRING)
                require(evt.values[9].type == UniversalChartVisitor.ValueType.STRING)
                if (evt.values.size == 11) require(evt.values[10].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                val time = evt.values[0].num
                val endTime = evt.values[1].num
                val startPosition = evt.values[2].num pos evt.values[5].num
                val easeType = ArcNote.EaseType.fromValue(evt.values[4].str)
                val endPosition = evt.values[3].num pos evt.values[6].num
                val color = evt.values[7].num.toInt()
                val hitSound = evt.values[8].str
                val noteType = ArcNote.NoteType.fromValue(evt.values[9].str)
                val arcResolution = (if (evt.values.size == 11) evt.values[10].num else 1.0).toDouble()

                arc(
                    time,
                    endTime,
                    startPosition.x,
                    endPosition.x,
                    easeType,
                    startPosition.y,
                    endPosition.y,
                    color,
                    hitSound,
                    noteType,
                    arcResolution
                ) {
                    evt.subEvents.forEach { subEvt ->
                        arcTapNoteParser(subEvt, this)
                    }
                }
            }
        }
    }

    class ArcResolutionNoteFilter(private val arcResolution: Double) : NoteFilter() {
        override fun filterArcNote(note: ArcNote): Note {
            return note.copy(arcResolution = this.arcResolution)
        }
    }

    override val timingGroupParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            require(evt.values.all { it.type == UniversalChartVisitor.ValueType.KEY_VALUE || it.type == UniversalChartVisitor.ValueType.STRING })

            val fx = evt.values.map {
                when (it.type) {
                    UniversalChartVisitor.ValueType.STRING -> TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.fromValue(it.str))
                    UniversalChartVisitor.ValueType.KEY_VALUE -> {
                        val (key, rawValue) = it.kv
                        val value = when (rawValue.type) {
                            UniversalChartVisitor.ValueType.STRING -> rawValue.str
                            UniversalChartVisitor.ValueType.ALGEBRAIC -> rawValue.num.toString()
                            UniversalChartVisitor.ValueType.KEY_VALUE -> error("Recursive key-value pair not supported")
                        }

                        TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.fromValue(key), value)
                    }

                    else -> error("Unexpected value `${it.raw}` with type ${it.type}")
                }
            }

            timingGroup(fxFilter = globalEffectFilter) {
                fx.forEach { addSpecialEffect(it) }

                globalNoteFilter?.let { addNoteFilter(it) }
                globalEventFilter?.let { addEventFilter(it) }

                fx.firstOrNull { it.type.value == "arcresolution" }?.let {
                    val arcResolution = it.param!!.toDouble()

                    addNoteFilter(ArcResolutionNoteFilter(arcResolution))
                }

                val nCtx = ParseContext(this@run, this)

                evt.segment?.events?.forEach { bEvt ->
                    getEventParser()(bEvt, nCtx)
                }
            }
        }
    }

}