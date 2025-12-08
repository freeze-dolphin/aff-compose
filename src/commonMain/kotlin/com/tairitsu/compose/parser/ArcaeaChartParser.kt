package com.tairitsu.compose.parser

import com.tairitsu.compose.*
import com.tairitsu.compose.Position.Companion.pos
import com.tairitsu.compose.arcaea.antlr.UniversalAffChartLexer
import com.tairitsu.compose.arcaea.antlr.UniversalAffChartParser
import org.antlr.v4.kotlinruntime.CharStreams
import org.antlr.v4.kotlinruntime.CommonTokenStream
import kotlin.math.absoluteValue

@Suppress("DuplicatedCode")
open class ArcaeaChartParser : ChartParser {

    companion object {
        val Instance by lazy { ArcaeaChartParser() }
        fun parse(content: String): Chart = Instance.parse(content)
    }

    open val normalNoteParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size == 2)
                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                val time = evt.values[0].algebraicValue!!
                val column = evt.values[1].algebraicValue!!
                if (column.toDouble().isInteger()) {
                    normalNote(time, column.toInt())
                } else {
                    normalNoteFloat(time, column.toDouble())
                }
            }
        }
    }

    open val holdNoteParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size == 3)
                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[2].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                val time = evt.values[0].num
                val endTime = evt.values[1].num
                val column = evt.values[2].num
                if (column.toDouble().isInteger()) {
                    holdNote(time, endTime, column.toInt())
                } else {
                    holdNoteFloat(time, endTime, column.toDouble())
                }
            }
        }
    }

    open val arcTapNoteParser: (UniversalChartVisitor.Event, MutableList<ArcTapNote>) -> Unit = { evt, arcTapList ->
        arcTapList.run {
            when (evt.name) {
                "arctap" -> {
                    require(evt.values.size == 1)
                    require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                    val time = evt.values[0].num
                    arctap(time)
                }
            }
        }
    }

    open val arcNoteParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size in 10..11)
                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // time
                require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // endTime
                require(evt.values[2].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // x1
                require(evt.values[3].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // x2
                require(evt.values[4].type == UniversalChartVisitor.ValueType.STRING) // easeType
                require(evt.values[5].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // y1
                require(evt.values[6].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // y2
                require(evt.values[7].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // color
                require(evt.values[8].type == UniversalChartVisitor.ValueType.STRING) // hitsound
                require(evt.values[9].type == UniversalChartVisitor.ValueType.STRING) // arcType
                if (evt.values.size == 11) require(evt.values[10].type == UniversalChartVisitor.ValueType.ALGEBRAIC) // arcResolution

                val time = evt.values[0].num
                val endTime = evt.values[1].num
                val startPosition = evt.values[2].num pos evt.values[5].num
                val easeType = ArcNote.EaseType.fromValue(evt.values[4].str)
                val endPosition = evt.values[3].num pos evt.values[6].num
                val color = evt.values[7].num.toInt()
                val hitSound = evt.values[8].str
                val noteType = ArcNote.NoteType.fromValue(evt.values[9].str)
                val arcResolution = (if (evt.values.size == 11) evt.values[10].num else 1.0).toDouble()

                if (time == endTime &&
                    startPosition.y == endPosition.y &&
                    color == 3 &&
                    noteType == ArcNote.NoteType.ARC &&
                    evt.subEvents.isEmpty()
                ) {
                    val centerPos = (startPosition.x + endPosition.x) / 2 pos startPosition.y
                    val length = (startPosition.x - endPosition.x).absoluteValue * 2

                    trace(
                        time,
                        time.toLong() + 1,
                        centerPos, s, centerPos,
                        gray,
                        hitSound
                    ) {
                        arctap(time, length)
                    }
                } else {
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
    }

    open val timingParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size == 3)
                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[2].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                val time = evt.values[0].num
                val bpm = evt.values[1].num
                val beats = evt.values[2].num
                timing(time, bpm, beats)
            }
        }
    }

    open val scenecontrolParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size >= 2)

                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[1].type == UniversalChartVisitor.ValueType.STRING)

                val time = evt.values[0].num
                val type = ScenecontrolType.fromValue(evt.values[1].str)

                if (evt.values.size == 2) {
                    scenecontrol(time, type)
                } else {
                    val params = evt.values.subList(2, evt.values.size)
                    require(params.all { it.type == UniversalChartVisitor.ValueType.STRING || it.type == UniversalChartVisitor.ValueType.ALGEBRAIC })

                    val typedParams = (params.map {
                        when (it.type) {
                            UniversalChartVisitor.ValueType.STRING -> it.str
                            UniversalChartVisitor.ValueType.ALGEBRAIC -> it.num
                            else -> error("Unexcepted scenecontrol parameter: ${it.raw}, with type: ${it.type}")
                        }
                    }.toTypedArray())

                    scenecontrol(time, type, *typedParams)
                }
            }
        }
    }

    open val cameraParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) {
                require(evt.values.size == 9)
                require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[2].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[3].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[4].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[5].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[6].type == UniversalChartVisitor.ValueType.ALGEBRAIC)
                require(evt.values[7].type == UniversalChartVisitor.ValueType.STRING)
                require(evt.values[8].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                val time = evt.values[0].num
                val xOff = evt.values[1].num
                val yOff = evt.values[2].num
                val zOff = evt.values[3].num
                val xozAng = evt.values[4].num
                val yozAng = evt.values[5].num
                val xoyAng = evt.values[6].num
                val ease = Camera.EaseType.fromValue(evt.values[7].str)
                val duration = evt.values[8].num
                camera(time, xOff, yOff, zOff, xozAng, yozAng, xoyAng, ease, duration)
            }
        }
    }

    open val timingGroupParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            require(evt.values.size in 0..1) // all special effects are connected using '_' in Arcaea
            require(evt.values.isEmpty() || evt.values[0].type == UniversalChartVisitor.ValueType.STRING)

            val fxMatcher = Regex("([a-zA-Z]+)(-?(0|([1-9][0-9]*))(\\.\\d+)?)")
            val fx = if (evt.values.isEmpty())
                emptyList()
            else
                evt.values[0].str.split('_').map { rawFx ->
                    if (rawFx.all { it.isLetter() }) {
                        TimingGroup.SpecialEffect(TimingGroup.SpecialEffectType.fromValue(rawFx))
                    } else if (rawFx.matches(fxMatcher)) {
                        val match = fxMatcher.findAll(rawFx)

                        require(match.count() == 1)
                        match.first().groupValues.let { groups ->
                            TimingGroup.SpecialEffect(
                                TimingGroup.SpecialEffectType.fromValue(groups[1]),
                                groups[2]
                            )
                        }
                    } else {
                        error("Unable to parse `${rawFx}` into a SpecialEffect")
                    }
                }

            timingGroup(fxFilter = globalEffectFilter) {
                fx.forEach { addSpecialEffect(it) }

                globalNoteFilter?.let { addNoteFilter(it) }
                globalEventFilter?.let { addEventFilter(it) }

                val nCtx = ParseContext(this@run, this)

                evt.segment?.events?.forEach { bEvt ->
                    getEventParser()(bEvt, nCtx)
                }
            }
        }
    }

    private val eventParser: (UniversalChartVisitor.Event, ParseContext) -> Unit = { evt, ctx ->
        ctx.chart.run {
            timingGroup(ctx.timingGroup.name) { // add notes to context
                when (evt.name) {
                    null -> normalNoteParser(evt, ctx)
                    "hold" -> holdNoteParser(evt, ctx)
                    "arc" -> arcNoteParser(evt, ctx)
                    "timing" -> timingParser(evt, ctx)
                    "scenecontrol" -> scenecontrolParser(evt, ctx)
                    "camera" -> cameraParser(evt, ctx)
                    "timinggroup" -> timingGroupParser(evt, ctx)

                    else -> error("Unable to parse `${evt.name}(${evt.values.size})[${evt.subEvents.size}];` into a TimedObject")
                }
            }
        }
    }

    override fun getEventParser(): (UniversalChartVisitor.Event, ParseContext) -> Unit = eventParser

    open val headerParser: String.() -> Chart.Configuration = {
        this // AudioOffset:0\nChartVersion:2 `String`
            .split("\n") // AudioOffset:0 `String`
            .map { item ->
                item.split(":", limit = 2).let { Chart.Configuration.Item(it[0], it[1]) }
            } // ChartConfiguration.Item(key="AudioOffset", value="0")
            .let { items ->
                Chart.Configuration(
                    items.first { it.key == "AudioOffset" }.value.toLong()
                ).apply {
                    items.filter { it.key != "AudioOffset" }.forEach { item ->
                        addItem(item)
                    }
                }
            }
    }

    open val globalEffectFilter: TimingGroupSpecialEffectFilter = TimingGroupSpecialEffectFilter.DEFAULT
    open val globalNoteFilter: NoteFilter? = null
    open val globalEventFilter: EventFilter? = null

    override fun parse(content: String): Chart {
        val separator = "-\n"
        val sepIdx = content.indexOf(separator)

        val header = content.take(sepIdx - 1) // ignore '\n-\n'
        val bodies = content.drop(sepIdx + separator.length)

        val lexer = UniversalAffChartLexer(CharStreams.fromString(bodies))
        val tokens = CommonTokenStream(lexer)
        val parser = UniversalAffChartParser(tokens).apply {
            removeErrorListeners()
            addErrorListener(ExceptionErrorListener)
        }

        val tree = parser.chart()
        val visitor = UniversalChartVisitor()
        val body = visitor.visitChart(tree)

        return compose(headerParser(header), globalEffectFilter) {
            globalNoteFilter?.let { addNoteFilter(it) }
            globalEventFilter?.let { addEventFilter(it) }

            val nCtx = ParseContext(this, this.currentTimingGroup)
            body.events.forEach { evt ->
                getEventParser()(evt, nCtx)
            }
        }
    }
}