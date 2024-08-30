package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.ANTLRChartParser.Executable.Companion.all
import com.tairitsu.compose.arcaea.antlr.ArcCreateChartLexer
import com.tairitsu.compose.arcaea.antlr.ArcCreateChartParser
import com.tairitsu.compose.arcaea.antlr.ArcaeaChartLexer
import com.tairitsu.compose.arcaea.antlr.ArcaeaChartParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.tree.TerminalNode

object ANTLRChartParser {

    class ArcaeaChartANTLRLoadException(s: String) : Exception(s)

    class ArcaeaChartANTLRErrorListener : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String?, e: RecognitionException?
        ) {
            throw ArcaeaChartANTLRLoadException(
                "\n\tLocation: [$line:$charPositionInLine]" + "\n\tOffending symbol: $offendingSymbol" + "\n\tMessage: $msg" + "\n\tMessage from exception: ${e?.message}"
            )
        }
    }

    private class ElseBranch(val isElseBranch: Boolean) {
        fun onElse(closure: Unit.() -> Unit) {
            if (isElseBranch) {
                closure.invoke(Unit)
            }
        }
    }

    private class Executable(val isExecutable: Boolean) {
        fun exec(closure: Unit.() -> Unit): ElseBranch {
            if (isExecutable) {
                closure.invoke(Unit)
                return ElseBranch(false)
            } else {
                return ElseBranch(true)
            }
        }

        companion object {
            infix fun Executable.and(another: Executable): Executable = Executable(this.isExecutable && another.isExecutable)

            fun all(vararg executables: Executable): Executable = Executable(executables.all { it.isExecutable })
        }
    }

    private class ContextConditioner<out T : ParserRuleContext>(val ctx: T) {
        fun notNull(closure: T.() -> TerminalNode?): Executable = Executable(closure(ctx) != null)
        fun ruleNotNull(closure: T.() -> ParserRuleContext?): Executable = Executable(closure(ctx) != null)

        fun allNotNull(vararg closure: T.() -> TerminalNode?): Executable = Executable(closure.all {
            notNull(it).isExecutable
        })

        fun ruleAllNotNull(vararg closure: T.() -> ParserRuleContext?): Executable = Executable(closure.all {
            ruleNotNull(it).isExecutable
        })

    }

    data class ArcCreateChartParseReport(
        val ignoredScenecontrols: MutableList<Pair<String, String>>,
        val ignoredTimingGroupEffects: MutableList<Pair<String, String>>
    )

    fun fromAcf(acf: String): Pair<Chart, ArcCreateChartParseReport> {
        val reporter = ArcCreateChartParseReport(mutableListOf(), mutableListOf())
        lateinit var rst: Chart
        mapSet {
            difficulties.future {
                val stream = CharStreams.fromString(acf)

                val lexer = ArcCreateChartLexer(stream)
                val tokens = CommonTokenStream(lexer)
                val parser = ArcCreateChartParser(tokens)

                lexer.removeErrorListeners()
                lexer.addErrorListener(ArcaeaChartANTLRErrorListener())

                parser.removeErrorListeners()
                parser.addErrorListener(ArcaeaChartANTLRErrorListener())

                val pchart = parser.chart_()

                // parse headers if there are
                if (pchart.header().isEmpty.not()) {
                    pchart.header().header_item().forEach {
                        val conditioner = ContextConditioner(it)
                        conditioner.notNull { K_audiooffset() }.exec {
                            chart.configuration.tuneOffset(it.Int().text.toLong())
                        }
                        conditioner.notNull { K_timingpointdensityfactor() }.exec {
                            conditioner.notNull { K_timingpointdensityfactor() }
                            chart.configuration.addItem("TimingPointDensityFactor", it.let {
                                it.Int() ?: it.Float()!!
                            }.text)
                        }
                        conditioner.notNull { K_version() }.exec {
                            lateinit var itemValue: String
                            conditioner.notNull { Version() }.exec {
                                itemValue = it.Version().text
                            }
                            conditioner.notNull { Int() }.exec {
                                itemValue = it.Int().text
                            }
                            conditioner.notNull { Float() }.exec {
                                itemValue = it.Float().text
                            }
                            chart.configuration.addItem("Version", itemValue)
                        }
                        conditioner.notNull { HeaderIdentifier() }.exec {
                            val key = it.HeaderIdentifier().text
                            conditioner.notNull { Float() }.exec {
                                chart.configuration.addItem(key, it.Float().text)
                            }
                            conditioner.notNull { Int() }.exec {
                                chart.configuration.addItem(key, it.Int().text)
                            }
                        }
                    }
                }

                // parse command invocations
                pchart.command_invocation().forEach {
                    val cdr = ContextConditioner(it)

                    // parse timing groups
                    cdr.ruleNotNull { cmd_timinggroup() }.exec {
                        val tg = timingGroup {}

                        // handle special effects of the timing group

                        // make sure the `single_timinggroup_argument` list is not empty
                        cdr.ruleNotNull { cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(0) }.exec {

                            it.cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument()
                                .forEachIndexed { idx, ctx ->
                                    cdr.notNull {
                                        cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx)
                                            .K_timinggroup_name()
                                    }.exec {
                                        // ignored, named timingGroups is not supported
                                    }.onElse {
                                        cdr.ruleNotNull {
                                            cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx)
                                                .enum_timinggroup_jremap(0)
                                        }.exec {
                                            // judgement remapping is not supported
                                            // apply noinput instead
                                            if (tg.specialEffects.none { tgse ->
                                                    tgse.type == TimingGroupSpecialEffectType.NO_INPUT
                                                } // haven't applied noinput
                                            ) {
                                                tg.addSpecialEffect(TimingGroupSpecialEffectType.NO_INPUT)
                                            }
                                            reporter.ignoredTimingGroupEffects.add(
                                                Pair(
                                                    it.cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx)
                                                        .enum_timinggroup_jremap(0).text,
                                                    it.cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx)
                                                        .enum_timinggroup_jremap(1).text
                                                )
                                            )
                                        }.onElse {
                                            lateinit var effect: Pair<TimingGroupSpecialEffectType, Int?>
                                            try {
                                                val type = TimingGroupSpecialEffectType.fromCodename(ctx.Lowers().text)
                                                cdr.notNull {
                                                    cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx)
                                                        .Float()
                                                }.exec {
                                                    effect = Pair(
                                                        type,
                                                        (it.cmd_timinggroup().compound_timinggroup_argument()
                                                            .single_timinggroup_argument(idx)
                                                            .Float().text.toDouble() * 10).toInt()
                                                    )
                                                }.onElse {
                                                    effect = Pair(type, null)
                                                }
                                                if (effect.second == null) {
                                                    tg.addSpecialEffect(effect.first)
                                                } else {
                                                    tg.addSpecialEffect(effect.first, effect.second!!)
                                                }
                                            } catch (ex: IllegalArgumentException) {
                                                reporter.ignoredTimingGroupEffects.add(Pair(ctx.Lowers().text, ctx.Float().text))
                                            }
                                        }
                                    }
                                }

                        }

                        // handle command invocations inside a timing group

                        // make sure the `command_invocation` list is not empty
                        cdr.ruleNotNull { cmd_timinggroup().command_invocation(0) }.exec {
                            it.cmd_timinggroup().command_invocation().forEach { ctx ->
                                processAcfCommandInvocationContext(ctx, reporter, tg.name).invoke(this@future)
                            }
                        }
                    }.onElse {

                        // parse command invocations out of a timing group
                        processAcfCommandInvocationContext(it, reporter).invoke(this@future)
                    }
                }

                // set `this.chart` as the result to reture
                rst = this.chart
            }
        }
        return Pair(rst, reporter)
    }

    private fun processAcfCommandInvocationContext(
        ctx: ArcCreateChartParser.Command_invocationContext,
        reporter: ArcCreateChartParseReport,
        tgName: String = "main"
    ): Difficulty.() -> Unit = {
        val cdr = ContextConditioner(ctx)

        // timing(Int, Float, Float);
        cdr.ruleNotNull { cmd_timing() }.exec {
            cdr.allNotNull({ cmd_timing().Int() }, { cmd_timing().Float(0) }, { cmd_timing().Float(1) }).exec {

                timingGroup(tgName) {
                    timing(
                        ctx.cmd_timing().Int().text.toInt().coerceAtLeast(0),
                        ctx.cmd_timing().Float(0).text.toDouble(),
                        ctx.cmd_timing().Float(1).text.toDouble().let {
                            if (it == 0.0) 4.0 else it
                        }
                    )
                }
            }
        }

        // [note](Int, (Int | Double));
        cdr.ruleNotNull { cmd_note() }.exec {
            cdr.notNull { cmd_note().Int(1) }.exec {
                timingGroup(tgName) {
                    normalNote(ctx.cmd_note().Int(0).text.toLong(), ctx.cmd_note().Int(1).text.toInt())
                }
            }
        }

        // hold(Int, Int, (Int | Double));
        cdr.ruleNotNull { cmd_hold() }.exec {
            cdr.allNotNull({ cmd_hold().Int(0) }, { cmd_hold().Int(1) }).exec {

                cdr.notNull { cmd_hold().Int(2) }.exec {
                    timingGroup(tgName) {
                        holdNote(
                            ctx.cmd_hold().Int(0).text.toLong(),
                            ctx.cmd_hold().Int(1).text.toLong(),
                            ctx.cmd_hold().Int(2).text.toInt()
                        )
                    }
                }
            }
        }

        // arc(Int, Int,    Float,  Float,  enum_arcnote_curve_type,    Float,  Float,  Int,    hitsound,   Boolean         )[...];
        // arc(t1,  t2,     x1,     x2,     easing,                     y1,     y2,     color,  hitsound,   skylineBoolean  )[...];
        cdr.ruleNotNull { cmd_arc() }.exec {
            all(
                cdr.allNotNull(
                    { cmd_arc().Int(0) },
                    { cmd_arc().Int(1) },
                    { cmd_arc().Float(0) },
                    { cmd_arc().Float(1) },
                    { cmd_arc().Float(2) },
                    { cmd_arc().Float(3) },
                    { cmd_arc().Int(2) },
                    { cmd_arc().Boolean() }
                ),
                cdr.ruleAllNotNull(
                    { cmd_arc().enum_arcnote_curve_type() },
                    { cmd_arc().hitsound() }
                )
            ).exec {
                val arcTapList: ArcNote.ArcTapList = ArcNote.ArcTapList(mutableListOf())
                val vlArcTapList = mutableListOf<Triple<Long, Position, Double>>()

                cdr.ruleNotNull { cmd_arc().compound_arctap_argument() }.exec {
                    ctx.cmd_arc().compound_arctap_argument().arctap().forEach { arcTapTiming ->
                        cdr.notNull { arcTapTiming.Float() }.exec {
                            // var-len arctaps
                            val arcTapTime = arcTapTiming.Int().text.toLong()
                            val arcTime = ctx.cmd_arc().Int(0).text.toLong()
                            val arcEndTime = ctx.cmd_arc().Int(1).text.toLong()
                            val arcStartPosition = Position(
                                ctx.cmd_arc().Float(0).text.toDouble(),
                                ctx.cmd_arc().Float(2).text.toDouble()
                            )
                            val arcEndPosition = Position(
                                ctx.cmd_arc().Float(1).text.toDouble(),
                                ctx.cmd_arc().Float(3).text.toDouble()
                            )

                            val ease = ArcNote.CurveType(ctx.cmd_arc().enum_arcnote_curve_type().text)
                            vlArcTapList.add(
                                Triple(
                                    arcTapTime, // arctap time
                                    ArcNote.getEasingFunction3D(arcStartPosition, arcEndPosition, ease)
                                        .invoke(
                                            (arcTapTime.toDouble() - arcTime) / (arcEndTime - arcTime),
                                            arcStartPosition,
                                            arcEndPosition
                                        ), // calc position
                                    arcTapTiming.Float().text.toDouble() // arctap length
                                )
                            )

                        }.onElse {
                            // fixed arctaps
                            arcTapList.tap(arcTapTiming.Int().text.toLong())
                        }
                    }
                }

                timingGroup(tgName) {
                    arcNoteLegacy(
                        ctx.cmd_arc().Int(0).text.toLong(),
                        ctx.cmd_arc().Int(1).text.toLong(),
                        ctx.cmd_arc().Float(0).text.toDouble(),
                        ctx.cmd_arc().Float(1).text.toDouble(),
                        ArcNote.CurveType(ctx.cmd_arc().enum_arcnote_curve_type().text),
                        ctx.cmd_arc().Float(2).text.toDouble(),
                        ctx.cmd_arc().Float(3).text.toDouble(),
                        ArcNote.Color(ctx.cmd_arc().Int(2).text.toInt()),
                        ctx.cmd_arc().Boolean().text.toBoolean()
                    ) {
                        arcTapList.data.forEach { arcTapTiming ->
                            this.tap(arcTapTiming)
                        }
                    }.withRawHitsound(ctx.cmd_arc().hitsound().text)

                    vlArcTapList.forEach { data ->
                        vlArctapWithDistance(data.first, data.second.toPair(), data.third / 2) // conversion
                    }
                }
            }
        }

        // scenecontrol(Int, enum_scenecontrol_type_argument, Float?, Int?);
        cdr.ruleNotNull { cmd_scenecontrol() }.exec {
            all(
                cdr.notNull { cmd_scenecontrol().Int(0) },
                cdr.notNull { cmd_scenecontrol().Lowers() }
            ).exec outer@{
                var extraParams: Pair<Double, Int>? = null
                cdr.allNotNull(
                    { cmd_scenecontrol().Int(1) },
                    { cmd_scenecontrol().Int(2) }
                ).exec {
                    extraParams = Pair(ctx.cmd_scenecontrol().Int(1).text.toDouble(), ctx.cmd_scenecontrol().Int(2).text.toInt())
                }

                cdr.allNotNull(
                    { cmd_scenecontrol().Float(0) },
                    { cmd_scenecontrol().Int(1) }
                ).exec {
                    extraParams = Pair(ctx.cmd_scenecontrol().Float(0).text.toDouble(), ctx.cmd_scenecontrol().Int(1).text.toInt())
                }

                try {
                    timingGroup(tgName) {
                        if (extraParams == null) {
                            rawScenecontrol(
                                ctx.cmd_scenecontrol().Int(0).text.toInt().coerceAtLeast(0),
                                ScenecontrolType.fromId(ctx.cmd_scenecontrol().Lowers().text),
                                null, null
                            )
                        } else {
                            val timeConverted = if (ScenecontrolType.fromId(ctx.cmd_scenecontrol().Lowers().text).needTimeConversion()) {
                                extraParams!!.first / 1000
                            } else {
                                extraParams!!.first
                            }

                            rawScenecontrol(
                                ctx.cmd_scenecontrol().Int(0).text.toInt().coerceAtLeast(0),
                                ScenecontrolType.fromId(ctx.cmd_scenecontrol().Lowers().text),
                                timeConverted, extraParams!!.second
                            )
                        }

                    }
                } catch (ex: IllegalArgumentException) {
                    reporter.ignoredScenecontrols.add(Pair(ctx.cmd_scenecontrol().Lowers().text, ctx.cmd_scenecontrol().Int(0).text))
                    return@outer
                }
            }
        }

        // camera(Int, Float, Float, Float, Float, Float, Float, enum_camera_ease_type, Int);
        cdr.ruleNotNull { cmd_camera() }.exec {
            all(cdr.notNull { cmd_camera().Int(0) },
                cdr.notNull { cmd_camera().Float(0) },
                cdr.notNull { cmd_camera().Float(1) },
                cdr.notNull { cmd_camera().Float(2) },
                cdr.notNull { cmd_camera().Float(3) },
                cdr.notNull { cmd_camera().Float(4) },
                cdr.notNull { cmd_camera().Float(5) },
                cdr.ruleNotNull { cmd_camera().enum_camera_ease_type() },
                cdr.notNull { cmd_camera().Int(1) }
            ).exec {
                timingGroup(tgName) {
                    camera(
                        ctx.cmd_camera().Int(0).text.toInt(),
                        ctx.cmd_camera().Float(0).text.toDouble(),
                        ctx.cmd_camera().Float(1).text.toDouble(),
                        ctx.cmd_camera().Float(2).text.toDouble(),
                        ctx.cmd_camera().Float(3).text.toDouble(),
                        ctx.cmd_camera().Float(4).text.toDouble(),
                        ctx.cmd_camera().Float(5).text.toDouble(),
                        Camera.CameraEaseType(ctx.cmd_camera().enum_camera_ease_type().text),
                        ctx.cmd_camera().Int(1).text.toInt(),
                    )
                }
            }
        }
    }

    fun fromAff(aff: String): Chart {
        lateinit var rst: Chart
        mapSet {
            difficulties.future {
                val stream = CharStreams.fromString(aff)

                val lexer = ArcaeaChartLexer(stream)
                val tokens = CommonTokenStream(lexer)
                val parser = ArcaeaChartParser(tokens)

                lexer.removeErrorListeners()
                lexer.addErrorListener(ArcaeaChartANTLRErrorListener())

                parser.removeErrorListeners()
                parser.addErrorListener(ArcaeaChartANTLRErrorListener())

                val pchart = parser.chart_()

                // parse headers if there are
                if (pchart.header().isEmpty.not()) {
                    pchart.header().header_item().forEach {
                        val conditioner = ContextConditioner(it)
                        conditioner.notNull { K_audiooffset() }.exec {
                            chart.configuration.tuneOffset(it.Int().text.toLong())
                        }
                        conditioner.notNull { K_timingpointdensityfactor() }.exec {
                            chart.configuration.addItem("TimingPointDensityFactor", it.let {
                                it.Int() ?: it.Float()!!
                            }.text)
                        }
                        conditioner.notNull { K_version() }.exec {
                            lateinit var itemValue: String
                            conditioner.notNull { Version() }.exec {
                                itemValue = it.Version().text
                            }
                            conditioner.notNull { Int() }.exec {
                                itemValue = it.Int().text
                            }
                            conditioner.notNull { Float() }.exec {
                                itemValue = it.Float().text
                            }
                            chart.configuration.addItem("Version", itemValue)
                        }
                        conditioner.notNull { HeaderIdentifier() }.exec {
                            val key = it.HeaderIdentifier().text
                            conditioner.notNull { Float() }.exec {
                                chart.configuration.addItem(key, it.Float().text)
                            }
                            conditioner.notNull { Int() }.exec {
                                chart.configuration.addItem(key, it.Int().text)
                            }
                        }
                    }
                }

                // parse command invocations
                pchart.command_invocation().forEach {
                    val cdr = ContextConditioner(it)

                    // parse timing groups
                    cdr.ruleNotNull { cmd_timinggroup() }.exec {
                        val tg = timingGroup { }

                        // handle special effects of the timing group

                        // make sure the `single_timinggroup_argument` list is not empty
                        cdr.ruleNotNull { cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(0) }.exec {

                            it.cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument()
                                .forEachIndexed { idx, ctx ->
                                    lateinit var effect: Pair<TimingGroupSpecialEffectType, Int?>
                                    val type = TimingGroupSpecialEffectType.fromCodename(ctx.enum_timinggroup_effects().text)
                                    cdr.notNull {
                                        cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx).Int()
                                    }.exec {
                                        effect = Pair(
                                            type,
                                            it.cmd_timinggroup().compound_timinggroup_argument().single_timinggroup_argument(idx)
                                                .Int().text.toInt()
                                        )
                                    }.onElse {
                                        effect = Pair(type, null)
                                    }
                                    if (effect.second == null) {
                                        tg.addSpecialEffect(effect.first)
                                    } else {
                                        tg.addSpecialEffect(effect.first, effect.second!!)
                                    }
                                }

                        }

                        // handle command invocations inside a timing group

                        // make sure the `command_invocation` list is not empty
                        cdr.ruleNotNull { cmd_timinggroup().command_invocation(0) }.exec {
                            it.cmd_timinggroup().command_invocation().forEach { ctx ->
                                processAffCommandInvocationContext(ctx, tg.name).invoke(this@future)
                            }
                        }
                    }.onElse {

                        // parse command invocations out of a timing group
                        processAffCommandInvocationContext(it).invoke(this@future)
                    }
                }

                // set `this.chart` as the result to reture
                rst = this.chart
            }
        }
        return rst
    }

    private fun processAffCommandInvocationContext(
        ctx: ArcaeaChartParser.Command_invocationContext,
        tgName: String = "main"
    ): Difficulty.() -> Unit = {
        val cdr = ContextConditioner(ctx)

        // timing(Int, Float, Float);
        cdr.ruleNotNull { cmd_timing() }.exec {
            cdr.allNotNull({ cmd_timing().Int() }, { cmd_timing().Float(0) }, { cmd_timing().Float(1) }).exec {

                timingGroup(tgName) {
                    timing(
                        ctx.cmd_timing().Int().text.toInt(),
                        ctx.cmd_timing().Float(0).text.toDouble(),
                        ctx.cmd_timing().Float(1).text.toDouble()
                    )
                }
            }
        }

        // [note](Int, (Int | Double));
        cdr.ruleNotNull { cmd_note() }.exec {
            cdr.notNull { cmd_note().Int(1) }.exec {
                timingGroup(tgName) {
                    normalNote(ctx.cmd_note().Int(0).text.toLong(), ctx.cmd_note().Int(1).text.toInt())
                }
            }
        }

        // hold(Int, Int, (Int | Double));
        cdr.ruleNotNull { cmd_hold() }.exec {
            cdr.allNotNull({ cmd_hold().Int(0) }, { cmd_hold().Int(1) }).exec {

                cdr.notNull { cmd_hold().Int(2) }.exec {
                    timingGroup(tgName) {
                        holdNote(
                            ctx.cmd_hold().Int(0).text.toLong(),
                            ctx.cmd_hold().Int(1).text.toLong(),
                            ctx.cmd_hold().Int(2).text.toInt()
                        )
                    }
                }
            }
        }

        // arc(Int, Int,    Float,  Float,  enum_arcnote_curve_type,    Float,  Float,  Int,    hitsound,   Boolean         )[...];
        // arc(t1,  t2,     x1,     x2,     easing,                     y1,     y2,     color,  hitsound,   skylineBoolean  )[...];
        cdr.ruleNotNull { cmd_arc() }.exec {
            all(
                cdr.allNotNull(
                    { cmd_arc().Int(0) },
                    { cmd_arc().Int(1) },
                    { cmd_arc().Float(0) },
                    { cmd_arc().Float(1) },
                    { cmd_arc().Float(2) },
                    { cmd_arc().Float(3) },
                    { cmd_arc().Int(2) },
                    { cmd_arc().Boolean() }
                ),
                cdr.ruleAllNotNull(
                    { cmd_arc().enum_arcnote_curve_type() },
                    { cmd_arc().hitsound() }
                )
            ).exec {
                val arcTapList: ArcNote.ArcTapList = ArcNote.ArcTapList(mutableListOf())

                cdr.ruleNotNull { cmd_arc().compound_arctap_argument() }.exec {
                    ctx.cmd_arc().compound_arctap_argument().arctap().forEach { arcTapTiming ->
                        arcTapList.tap(arcTapTiming.Int().text.toLong())
                    }
                }

                timingGroup(tgName) {
                    arcNoteLegacy(
                        ctx.cmd_arc().Int(0).text.toLong(),
                        ctx.cmd_arc().Int(1).text.toLong(),
                        ctx.cmd_arc().Float(0).text.toDouble(),
                        ctx.cmd_arc().Float(1).text.toDouble(),
                        ArcNote.CurveType(ctx.cmd_arc().enum_arcnote_curve_type().text),
                        ctx.cmd_arc().Float(2).text.toDouble(),
                        ctx.cmd_arc().Float(3).text.toDouble(),
                        ArcNote.Color(ctx.cmd_arc().Int(2).text.toInt()),
                        ctx.cmd_arc().Boolean().text.toBoolean()
                    ) {
                        arcTapList.data.forEach { arcTapTiming ->
                            this.tap(arcTapTiming)
                        }
                    }.withRawHitsound(ctx.cmd_arc().hitsound().text)
                }
            }
        }

        // scenecontrol(Int, enum_scenecontrol_type_argument, Float?, Int?);
        cdr.ruleNotNull { cmd_scenecontrol() }.exec {
            all(
                cdr.notNull { cmd_scenecontrol().Int(0) },
                cdr.ruleNotNull { cmd_scenecontrol().enum_scenecontrol_type_argument() }
            ).exec {
                var extraParams: Pair<Double, Int>? = null
                cdr.allNotNull(
                    { cmd_scenecontrol().Float() },
                    { cmd_scenecontrol().Int(1) }
                ).exec {
                    extraParams = Pair(ctx.cmd_scenecontrol().Float().text.toDouble(), ctx.cmd_scenecontrol().Int(1).text.toInt())
                }

                timingGroup(tgName) {
                    if (extraParams == null) {
                        rawScenecontrol(
                            ctx.cmd_scenecontrol().Int(0).text.toInt(),
                            ScenecontrolType.fromId(ctx.cmd_scenecontrol().enum_scenecontrol_type_argument().text),
                            null, null
                        )
                    } else {
                        rawScenecontrol(
                            ctx.cmd_scenecontrol().Int(0).text.toInt(),
                            ScenecontrolType.fromId(ctx.cmd_scenecontrol().enum_scenecontrol_type_argument().text),
                            extraParams!!.first, extraParams!!.second
                        )
                    }
                }
            }
        }

        // camera(Int, Float, Float, Float, Float, Float, Float, enum_camera_ease_type, Int);
        cdr.ruleNotNull { cmd_camera() }.exec {
            all(cdr.notNull { cmd_camera().Int(0) },
                cdr.notNull { cmd_camera().Float(0) },
                cdr.notNull { cmd_camera().Float(1) },
                cdr.notNull { cmd_camera().Float(2) },
                cdr.notNull { cmd_camera().Float(3) },
                cdr.notNull { cmd_camera().Float(4) },
                cdr.notNull { cmd_camera().Float(5) },
                cdr.ruleNotNull { cmd_camera().enum_camera_ease_type() },
                cdr.notNull { cmd_camera().Int(1) }
            ).exec {
                timingGroup(tgName) {
                    camera(
                        ctx.cmd_camera().Int(0).text.toInt(),
                        ctx.cmd_camera().Float(0).text.toDouble(),
                        ctx.cmd_camera().Float(1).text.toDouble(),
                        ctx.cmd_camera().Float(2).text.toDouble(),
                        ctx.cmd_camera().Float(3).text.toDouble(),
                        ctx.cmd_camera().Float(4).text.toDouble(),
                        ctx.cmd_camera().Float(5).text.toDouble(),
                        Camera.CameraEaseType(ctx.cmd_camera().enum_camera_ease_type().text),
                        ctx.cmd_camera().Int(1).text.toInt(),
                    )
                }
            }
        }
    }
}

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