package com.tairitsu.compose.arcaea.parser

import com.tairitsu.compose.arcaea.*
import com.tairitsu.compose.arcaea.antlr.ArcCreateChartLexer
import com.tairitsu.compose.arcaea.antlr.ArcCreateChartParser
import com.tairitsu.compose.arcaea.parser.Executable.Companion.all
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

data class ArcCreateChartParseReport(
    val ignoredScenecontrols: MutableList<Pair<String, String>>,
    val ignoredTimingGroupEffects: MutableList<Pair<String, String?>>,
)

class ANTLRArcCreateChartParser(
    private val acf: String,
) : IArcCreateChartParser {

    override fun parse(): Pair<Chart, ArcCreateChartParseReport> {
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
                            chart.configuration.addItem("TimingPointDensityFactor", it.Float()?.text ?: it.Int().text)
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
                                                val type = TimingGroupSpecialEffectType.fromCodename(ctx.Alphas().text)
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
                                                val value = if (ctx.Float() == null) {
                                                    null
                                                } else ctx.Float().text
                                                reporter.ignoredTimingGroupEffects.add(Pair(ctx.Alphas().text, value))
                                            }
                                        }
                                    }
                                }

                        }

                        // handle command invocations inside a timing group

                        // make sure the `command_invocation` list is not empty
                        cdr.ruleNotNull { cmd_timinggroup().command_invocation(0) }.exec {
                            it.cmd_timinggroup().command_invocation().forEach { ctx ->
                                processCommandInvocationContext(ctx, reporter, tg.name).invoke(this@future)
                            }
                        }
                    }.onElse {

                        // parse command invocations out of a timing group
                        processCommandInvocationContext(it, reporter).invoke(this@future)
                    }
                }

                // set `this.chart` as the result to reture
                rst = this.chart
            }
        }
        return Pair(rst, reporter)
    }

    override fun processCommandInvocationContext(
        ctx: ArcCreateChartParser.Command_invocationContext,
        reporter: ArcCreateChartParseReport,
        tgName: String,
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

        // scenecontrol(Int, Alphas, Float?, Int?);
        cdr.ruleNotNull { cmd_scenecontrol() }.exec {
            all(
                cdr.notNull { cmd_scenecontrol().Int(0) },
                cdr.notNull { cmd_scenecontrol().Alphas(0) }
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
                        val time = ctx.cmd_scenecontrol().Int(0).text.toInt().coerceAtLeast(0)
                        when (val scId = ctx.cmd_scenecontrol().Alphas(0).text) { // special scenecontrols
                            "groupalpha" -> {
                                val alpha = ctx.cmd_scenecontrol().Int(2).text.toInt()
                                if (alpha > 0) {
                                    rawScenecontrol(time, ScenecontrolType.HIDE_GROUP, 0.0, 0)
                                } else {
                                    rawScenecontrol(time, ScenecontrolType.HIDE_GROUP, 0.0, 1)
                                }
                            }

                            else -> {
                                if (extraParams == null) {
                                    rawScenecontrol(
                                        time,
                                        ScenecontrolType.fromId(scId),
                                        null, null
                                    )
                                } else {
                                    val timeConverted =
                                        if (ScenecontrolType.fromId(scId).needTimeConversion()) {
                                            extraParams!!.first / 1000
                                        } else {
                                            extraParams!!.first
                                        }

                                    rawScenecontrol(
                                        time,
                                        ScenecontrolType.fromId(scId),
                                        timeConverted, extraParams!!.second
                                    )
                                }
                            }
                        }
                    }
                } catch (ex: IllegalArgumentException) {
                    reporter.ignoredScenecontrols.add(Pair(ctx.cmd_scenecontrol().Alphas(0).text, ctx.cmd_scenecontrol().Int(0).text))
                    return@outer
                }
            }
        }

        // camera(Int, Float, Float, Float, Float, Float, Float, enum_camera_ease_type, Int);
        cdr.ruleNotNull { cmd_camera() }.exec {
            all(
                cdr.notNull { cmd_camera().Int(0) },
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