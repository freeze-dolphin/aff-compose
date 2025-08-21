package com.tairitsu.compose.arcaea.parser

import com.tairitsu.compose.arcaea.*
import com.tairitsu.compose.arcaea.PostProcessor.arcResolution
import com.tairitsu.compose.arcaea.PostProcessor.rawHitsound
import com.tairitsu.compose.arcaea.antlr.*
import com.tairitsu.compose.arcaea.parser.Executable.Companion.all
import org.antlr.v4.kotlinruntime.*

class ArcaeaChartANTLRLoadException(s: String) : Exception(s)

class ArcaeaChartANTLRErrorListener : BaseErrorListener() {
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException?,
    ) {
        throw ArcaeaChartANTLRLoadException(
            "\n\tLocation: [$line:$charPositionInLine]" + "\n\tOffending symbol: $offendingSymbol" + "\n\tMessage: $msg" + "\n\tMessage from exception: ${e?.message}"
        )
    }
}

class ANTLRArcaeaChartParser(
    private val aff: String,
) : IArcaeaChartParser {

    override fun parse(): Chart {
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
                            chart.configuration.tuneOffset(it.Int()!!.text.toLong())
                        }
                        conditioner.notNull { K_timingpointdensityfactor() }.exec {
                            chart.configuration.addItem("TimingPointDensityFactor", it.Float()?.text ?: it.Int()!!.text)
                        }
                        conditioner.notNull { K_version() }.exec {
                            lateinit var itemValue: String
                            conditioner.notNull { Version() }.exec {
                                itemValue = it.Version()!!.text
                            }
                            conditioner.notNull { Int() }.exec {
                                itemValue = it.Int()!!.text
                            }
                            conditioner.notNull { Float() }.exec {
                                itemValue = it.Float()!!.text
                            }
                            chart.configuration.addItem("Version", itemValue)
                        }
                        conditioner.notNull { HeaderIdentifier() }.exec {
                            val key = it.HeaderIdentifier()!!.text
                            conditioner.notNull { Float() }.exec {
                                chart.configuration.addItem(key, it.Float()!!.text)
                            }
                            conditioner.notNull { Int() }.exec {
                                chart.configuration.addItem(key, it.Int()!!.text)
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
                        cdr.ruleNotNull { cmd_timinggroup()!!.compound_timinggroup_argument().single_timinggroup_argument(0) }.exec {

                            it.cmd_timinggroup()!!.compound_timinggroup_argument().single_timinggroup_argument()
                                .forEachIndexed { idx, ctx ->
                                    lateinit var effect: Pair<TimingGroupSpecialEffectType, Int?>
                                    val type = TimingGroupSpecialEffectType.fromCodename(ctx.enum_timinggroup_effects().text)
                                    cdr.notNull {
                                        cmd_timinggroup()!!.compound_timinggroup_argument().single_timinggroup_argument(idx)!!.Int()
                                    }.exec {
                                        effect = Pair(
                                            type,
                                            it.cmd_timinggroup()!!
                                                .compound_timinggroup_argument()
                                                .single_timinggroup_argument(idx)!!
                                                .Int()!!
                                                .text
                                                .toInt()
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
                        cdr.ruleNotNull { cmd_timinggroup()!!.command_invocation(0) }.exec {
                            it.cmd_timinggroup()!!.command_invocation().forEach { ctx ->
                                processCommandInvocationContext(ctx, tg.name).invoke(this@future)
                            }
                        }
                    }.onElse {

                        // parse command invocations out of a timing group
                        processCommandInvocationContext(it).invoke(this@future)
                    }
                }

                // set `this.chart` as the result to reture
                rst = this.chart
            }
        }
        return rst
    }

    override fun processCommandInvocationContext(
        ctx: ArcaeaChartParser.Command_invocationContext,
        tgName: String,
    ): Difficulty.() -> Unit = {
        val cdr = ContextConditioner(ctx)

        // timing(Int, Float, Float);
        cdr.ruleNotNull { cmd_timing() }.exec {
            cdr.allNotNull({ cmd_timing()!!.Int() }, { cmd_timing()!!.Float(0) }, { cmd_timing()!!.Float(1) }).exec {
                timingGroup(tgName) {
                    timing(
                        ctx.cmd_timing()!!.Int()!!.text.toInt(),
                        ctx.cmd_timing()!!.Float(0)!!.text.toDouble(),
                        ctx.cmd_timing()!!.Float(1)!!.text.toDouble()
                    )
                }
            }
        }

        // [note](Int, (Int | Double));
        cdr.ruleNotNull { cmd_note() }.exec {
            cdr.notNull { cmd_note()!!.Int(1) }.exec {
                timingGroup(tgName) {
                    normalNote(ctx.cmd_note()!!.Int(0)!!.text.toLong(), ctx.cmd_note()!!.Int(1)!!.text.toInt())
                }
            }
        }

        // hold(Int, Int, (Int | Double));
        cdr.ruleNotNull { cmd_hold() }.exec {
            cdr.allNotNull({ cmd_hold()!!.Int(0) }, { cmd_hold()!!.Int(1) }).exec {

                cdr.notNull { cmd_hold()!!.Int(2) }.exec {
                    timingGroup(tgName) {
                        holdNote(
                            ctx.cmd_hold()!!.Int(0)!!.text.toLong(),
                            ctx.cmd_hold()!!.Int(1)!!.text.toLong(),
                            ctx.cmd_hold()!!.Int(2)!!.text.toInt()
                        )
                    }
                }
            }
        }

        // arc(Int, Int,    Float,  Float,  enum_arcnote_curve_type,    Float,  Float,  Int,    hitsound,   Boolean / designant,        Float?        )[...];
        // arc(t1,  t2,     x1,     x2,     easing,                     y1,     y2,     color,  hitsound,   skylineBoolean / designant, arcResolution?)[...];
        cdr.ruleNotNull { cmd_arc() }.exec {
            all(
                cdr.allNotNull(
                    { cmd_arc()!!.Int(0) },
                    { cmd_arc()!!.Int(1) },
                    { cmd_arc()!!.Float(0) },
                    { cmd_arc()!!.Float(1) },
                    { cmd_arc()!!.Float(2) },
                    { cmd_arc()!!.Float(3) },
                    { cmd_arc()!!.Int(2) },
                    // { cmd_arc().Hitsound() }
                    // { cmd_arc().Boolean() }
                ),
                cdr.ruleAllNotNull(
                    { cmd_arc()!!.enum_arcnote_curve_type() },
                )
            ).exec {
                val arcTapList = mutableListOf<Long>()

                var arcResolution = 1.0
                cdr.notNull { cmd_arc()!!.Float(4) }.exec {
                    arcResolution = ctx.cmd_arc()!!.Float(4)!!.text.toDouble()
                }

                cdr.ruleNotNull { cmd_arc()!!.compound_arctap_argument() }.exec {
                    ctx.cmd_arc()!!.compound_arctap_argument()!!.arctap().forEach { arcTapTiming ->
                        arcTapList.add(arcTapTiming.Int().text.toLong())
                    }
                }

                cdr.notNull { cmd_arc()!!.K_designant() }.exec {
                    timingGroup(tgName) {
                        arcNoteDesignant(
                            ctx.cmd_arc()!!.Int(0)!!.text.toLong(),
                            ctx.cmd_arc()!!.Int(1)!!.text.toLong(),
                            ctx.cmd_arc()!!.Float(0)!!.text.toDouble() pos ctx.cmd_arc()!!.Float(1)!!.text.toDouble(),
                            ArcNote.CurveType(ctx.cmd_arc()!!.enum_arcnote_curve_type()!!.text),
                            ctx.cmd_arc()!!.Float(2)!!.text.toDouble() pos ctx.cmd_arc()!!.Float(3)!!.text.toDouble(),
                            ArcNote.Color(ctx.cmd_arc()!!.Int(2)!!.text.toInt()),
                            isGuidingLine = false,
                            isDesignant = true,
                            arcTapList
                        ) {
                            rawHitsound(ctx.cmd_arc()?.Hitsound()?.text ?: ctx.cmd_arc()!!.Alphas()!!.text)
                            arcResolution(arcResolution)
                        }
                    }
                }.onElse {
                    timingGroup(tgName) {
                        arcNoteDesignant(
                            ctx.cmd_arc()!!.Int(0)!!.text.toLong(),
                            ctx.cmd_arc()!!.Int(1)!!.text.toLong(),
                            ctx.cmd_arc()!!.Float(0)!!.text.toDouble() pos ctx.cmd_arc()!!.Float(1)!!.text.toDouble(),
                            ArcNote.CurveType(ctx.cmd_arc()!!.enum_arcnote_curve_type()!!.text),
                            ctx.cmd_arc()!!.Float(2)!!.text.toDouble() pos ctx.cmd_arc()!!.Float(3)!!.text.toDouble(),
                            ArcNote.Color(ctx.cmd_arc()!!.Int(2)!!.text.toInt()),
                            isGuidingLine = ctx.cmd_arc()!!.Boolean()!!.text.toBoolean(),
                            isDesignant = false,
                            arcTapList
                        ) {
                            rawHitsound(ctx.cmd_arc()?.Hitsound()?.text ?: ctx.cmd_arc()!!.Alphas()!!.text)
                            arcResolution(arcResolution)
                        }
                    }
                }
            }
        }

        // scenecontrol(Int, enum_scenecontrol_type_argument, Float?, Int?);
        cdr.ruleNotNull { cmd_scenecontrol() }.exec {
            all(
                cdr.notNull { cmd_scenecontrol()!!.Int(0) },
                cdr.ruleNotNull { cmd_scenecontrol()!!.enum_scenecontrol_type_argument() }
            ).exec {
                var extraParams: Pair<Double, Int>? = null
                cdr.allNotNull(
                    { cmd_scenecontrol()!!.Float() },
                    { cmd_scenecontrol()!!.Int(1) }
                ).exec {
                    extraParams = Pair(ctx.cmd_scenecontrol()!!.Float()!!.text.toDouble(), ctx.cmd_scenecontrol()!!.Int(1)!!.text.toInt())
                }

                timingGroup(tgName) {
                    if (extraParams == null) {
                        rawScenecontrol(
                            ctx.cmd_scenecontrol()!!.Int(0)!!.text.toInt(),
                            ScenecontrolType.fromId(ctx.cmd_scenecontrol()!!.enum_scenecontrol_type_argument()!!.text),
                            null, null
                        )
                    } else {
                        rawScenecontrol(
                            ctx.cmd_scenecontrol()!!.Int(0)!!.text.toInt(),
                            ScenecontrolType.fromId(ctx.cmd_scenecontrol()!!.enum_scenecontrol_type_argument()!!.text),
                            extraParams.first, extraParams.second
                        )
                    }
                }
            }
        }

        // camera(Int, Float, Float, Float, Float, Float, Float, enum_camera_ease_type, Int);
        cdr.ruleNotNull { cmd_camera() }.exec {
            all(
                cdr.notNull { cmd_camera()!!.Int(0) },
                cdr.notNull { cmd_camera()!!.Float(0) },
                cdr.notNull { cmd_camera()!!.Float(1) },
                cdr.notNull { cmd_camera()!!.Float(2) },
                cdr.notNull { cmd_camera()!!.Float(3) },
                cdr.notNull { cmd_camera()!!.Float(4) },
                cdr.notNull { cmd_camera()!!.Float(5) },
                cdr.ruleNotNull { cmd_camera()!!.enum_camera_ease_type() },
                cdr.notNull { cmd_camera()!!.Int(1) }
            ).exec {
                timingGroup(tgName) {
                    camera(
                        ctx.cmd_camera()!!.Int(0)!!.text.toInt(),
                        ctx.cmd_camera()!!.Float(0)!!.text.toDouble(),
                        ctx.cmd_camera()!!.Float(1)!!.text.toDouble(),
                        ctx.cmd_camera()!!.Float(2)!!.text.toDouble(),
                        ctx.cmd_camera()!!.Float(3)!!.text.toDouble(),
                        ctx.cmd_camera()!!.Float(4)!!.text.toDouble(),
                        ctx.cmd_camera()!!.Float(5)!!.text.toDouble(),
                        Camera.CameraEaseType(ctx.cmd_camera()!!.enum_camera_ease_type()!!.text),
                        ctx.cmd_camera()!!.Int(1)!!.text.toInt(),
                    )
                }
            }
        }
    }
}