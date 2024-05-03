package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.filter.mirror
import com.tairitsu.compose.arcaea.serializer.generateChartJson
import com.tairitsu.compose.arcaea.serializer.generateTimingGroupJson
import java.io.File
import kotlin.test.Test

object DemoMap {

    @Test
    fun main() {
        mapSet {
            id = "avenue11"
            titleLocalized = LocalizedString("Avenue 11").apply {
                zhHans = "十一大道"
            }
            idx = 11 // This field is used in LinkPlay
            artist = "Sound Souler"
            bpm = "126"
            bpmBase = 126.0
            set = "single"
            purchase = ""
            audioPreview = 31985L
            audioPreviewEnd = 51340L
            side = MapSet.Side.LIGHT

            difficulties.future(-320) {
                timing(
                    offset = 0,
                    bpm = 126,
                    beats = 4,
                )

                println("Chart Configuration [FTR] : " + generateChartJson())
            }

            difficulties.eternal(-600) {

                // Here's how to add a property in chart
                chart.configuration.addItem("Version", 1.0)

                // You can also use this to adjust the audioOffset
                chart.configuration.tuneOffset(-600)

                // The main timing
                timing(
                    offset = 0,
                    bpm = 126,
                    beats = 4,
                )

                val tg = timingGroup(
                    "SpecialTG",
                    TimingGroupSpecialEffect.fadingholds,
                    TimingGroupSpecialEffect.anglex(-100)
                ) {

                    // Here's how to add special effects to a timing group using the legacy way
                    // addSpecialEffect(TimingGroupSpecialEffectType.FADING_HOLDS)
                    // addSpecialEffect(TimingGroupSpecialEffectType.ANGLEX, -100)

                    timing(
                        offset = 0,
                        bpm = 126,
                        beats = 4,
                    )

                    holdNote(17140, 18807, 4)

                    // arcNotes without color automatically become a trace, you can attach arcTap on it with a closure
                    arcNote(17140, 19045, 0.0 to 1.0, si, 1.0 to 1.0) {
                        // [ArcTapList.arctap] and [ArcTapList.tap] are exactly the same!
                        // use `parent` field to access arcNote object
                        arctap(parent.time) // => arctap(17140)
                    }

                    // You can use infix function `pos` to get a [Position] object, which is also a proper way to represent the coordination
                    // The [Position] uses `x` and `y` as field, which is more clearly and is preferred
                    arcNote(17140, 18569, 0 pos 1, siso, 0.5 pos 0) {
                        arctap(parent.endTime)
                    }
                    arcNote(17140, 18093, 0 to 1, siso, 0.25 to 0.25) {
                        arctap(parent.endTime)
                    }
                    arcNote(17140, 17616, 0 to 1, siso, 0 to 0.5)

                    // arcNotes with color becomes a arc,
                    arcNote(19045, 20712, 1.25 pos 1.0, b, -0.5 pos 0, red)
                    mirror {
                        // but you can still manually set it to a trace
                        arcNote(19045, 20712, 1.25 pos 1.0, b, -0.5 pos 0, red, true)
                    }

                    holdNote(19045, 20712, 4)
                }

                // Here's how to create a var-len arcTap (added in 5.5.6)
                // 1. By specifying the position of the Arc with color 3
                vlArctap(19045, 0.0 to 1.0, 1.0 to 1.0)
                // 2. By specifying the center position and a radius of distance
                vlArctapWithRadius(19045, 0.5 to 1.0, 0.5)
                vlArctapWithDistance(19045, 0.5 to 1.0, 1.0)
                // The effects of all these three var-len arcTap are exactly the same

                // Adding a scenecontrol
                scenecontrol(19045, ScenecontrolType.TRACK_HIDE)

                // Serialize the whole chart as json (containing metadata and every ratingClass)
                println("Chart : " + generateChartJson())

                // Serialize the specific timing group as json
                println(tg.name + " : " + generateTimingGroupJson(tg.name))

                // Serialize to aff
                println("Chart [ETR] : \r\n" + generateString())
            }
        }.writeToFolder(File(File("."), "result"))
    }

}