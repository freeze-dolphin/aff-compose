package com.tairitsu.compose.arcaea.parser

import com.tairitsu.compose.arcaea.*

@Deprecated("Refactor to use ANTLR instead", ReplaceWith("com.tairitsu.compose.arcaea.ANTLRChartParser"))
object RawChartParser {

    class FailedToParseAffToChartException(s: String) : Exception(s)

    private enum class AffProcedure {
        NORMAL_NOTE, HOLD_NOTE, ARC_NOTE, TIMING, TIMING_GROUP_START, TIMING_GROUP_END, SCENECONTROL;

        companion object {
            fun fromString(procedure: String): AffProcedure = when (procedure) {
                "hold" -> HOLD_NOTE
                "arc" -> ARC_NOTE
                "timing" -> TIMING
                "timinggroup" -> TIMING_GROUP_START
                "};" -> TIMING_GROUP_END
                "scenecontrol" -> SCENECONTROL
                else -> NORMAL_NOTE
            }
        }
    }

    private fun extractIntFromString(s: String): Int = s.filter {
        it.isDigit()
    }.toInt()

    internal fun fromAff(aff: String): Chart {
        var result = Chart()
        mapSet {
            difficulties.future {
                val lines = aff.split("\r\n")
                val metaIdx = lines.indexOf("-")

                // set headers
                if (metaIdx != -1) {
                    lines.subList(0, metaIdx).forEach {
                        parseHeaders(it).let { header ->
                            if (header.first == "AudioOffset") {
                                chart.configuration.tuneOffset(header.second.toLong())
                            } else {
                                chart.configuration.addItem(header.first, header.second)
                            }
                        }
                    }
                }

                val commands = lines.subList(metaIdx + 1, lines.size)

                val mgr = TimingGroupManager(this.chart)

                commands.forEach { cmd ->
                    try {
                        val parser = ParamParser(cmd)

                        when (parser.affProcedure) {
                            AffProcedure.NORMAL_NOTE -> parser.parse {
                                timingGroup(mgr.currentTimingGroup.name) {
                                    normalNote(nextParam().toInt(), nextParam().toInt())
                                }
                            }

                            AffProcedure.HOLD_NOTE -> parser.parse {
                                timingGroup(mgr.currentTimingGroup.name) {
                                    holdNote(
                                        nextParam().toInt(), nextParam().toInt(), nextParam().toInt()
                                    )
                                }
                            }

                            AffProcedure.ARC_NOTE -> parser.parse {
                                timingGroup(mgr.currentTimingGroup.name) {
                                    if (this@parse.data[9].toBoolean().not()) {
                                        arcNoteLegacy(
                                            nextParam().toInt(),
                                            nextParam().toInt(),
                                            nextParam().toDouble(),
                                            nextParam().toDouble(),
                                            ArcNote.CurveType(nextParam()),
                                            nextParam().toDouble(),
                                            nextParam().toDouble(),
                                            ArcNote.Color(nextParam().toInt()),
                                            false
                                        )
                                    } else {
                                        arcNoteLegacy(
                                            nextParam().toInt(),
                                            nextParam().toInt(),
                                            nextParam().toDouble(),
                                            nextParam().toDouble(),
                                            ArcNote.CurveType(nextParam()),
                                            nextParam().toDouble(),
                                            nextParam().toDouble(),
                                            ArcNote.Color(nextParam().toInt()),
                                            true
                                        ) {
                                            parser.arctapList.forEach {
                                                tap(it)
                                            }
                                        }
                                    }
                                }
                            }

                            AffProcedure.TIMING -> parser.parse {
                                timingGroup(mgr.currentTimingGroup.name) {
                                    timing(nextParam().toLong(), nextParam().toDouble(), nextParam().toDouble())
                                }
                            }

                            AffProcedure.SCENECONTROL -> parser.parse {
                                timingGroup(mgr.currentTimingGroup.name) {
                                    val extended = paramSize() == 4
                                    if (extended) {
                                        scenecontrol(
                                            nextParam().toLong(),
                                            ScenecontrolType.fromId(nextParam()),
                                            nextParam().toDouble(),
                                            nextParam().toInt()
                                        )
                                    } else {
                                        scenecontrol(
                                            nextParam().toLong(), ScenecontrolType.fromId(nextParam())
                                        )
                                    }
                                }
                            }

                            AffProcedure.TIMING_GROUP_START -> parser.parse {
                                mgr.notifyTimingGroup(timingGroup {
                                    val effects = nextParam().split("_")

                                    run outer@{
                                        effects.forEach {
                                            when (it) {
                                                "noinput" -> addSpecialEffect(TimingGroupSpecialEffectType.NO_INPUT)
                                                "fadingholds" -> addSpecialEffect(TimingGroupSpecialEffectType.FADING_HOLDS)
                                                else -> {
                                                    if (it.startsWith("anglex")) {
                                                        addSpecialEffect(TimingGroupSpecialEffectType.ANGLEX, extractIntFromString(it))
                                                        return@outer
                                                    }
                                                    if (it.startsWith("angley")) {
                                                        addSpecialEffect(TimingGroupSpecialEffectType.ANGLEY, extractIntFromString(it))
                                                        return@outer
                                                    }
                                                }
                                            }
                                        }
                                    }
                                })
                            }

                            AffProcedure.TIMING_GROUP_END -> {
                                mgr.notifyTimingGroup(null)
                            }
                        }
                    } catch (e: Exception) {
                        throw FailedToParseAffToChartException(e.message + "\n\twhile parsing line: `$cmd`\n").apply {
                            this.stackTrace = e.stackTrace
                        }
                    }
                }
                result = this.chart
            }
        }
        return result
    }

    private fun parseHeaders(header: String): Pair<String, String> = header.split(":").let {
        it[0] to it[1]
    }

    private class TimingGroupManager(private val chart: Chart) {
        var currentTimingGroup: TimingGroup = chart.mainTiming

        fun notifyTimingGroup(tg: TimingGroup?) {
            currentTimingGroup = tg ?: chart.mainTiming
        }
    }

    private class ParamParser(private val cmd: String) {
        val regex = "([a-zA-Z]*)\\(([^)]+)\\)".toRegex()
        val matchRsts: Sequence<MatchResult> = regex.findAll(cmd)
        val affProcedure: AffProcedure by lazy {
            AffProcedure.fromString(cmd.let {
                if (it.trimIndent().startsWith("timinggroup")) {
                    "timinggroup"
                } else if (it.trimIndent() == "};") {
                    "};"
                } else {
                    matchRsts.first().groups[1]?.value ?: " "
                }
            })
        }

        val arctapList: List<Long> by lazy {
            matchRsts.drop(1).map {
                it.groups[2]!!.value.toLong()
            }.toList()
        }

        val params: String? by lazy {
            if (!matchRsts.iterator().hasNext()) "" else matchRsts.first().groups[2]?.value
        }

        fun parse(closure: ParamList.() -> Unit) {
            ParamList(
                (params)!!.split(",")
            ).let(closure)
        }

        class ParamList(val data: List<String>) {
            val iter = data.iterator()

            fun nextParam(): String = iter.next()
            fun paramSize(): Int = data.size
        }
    }
}