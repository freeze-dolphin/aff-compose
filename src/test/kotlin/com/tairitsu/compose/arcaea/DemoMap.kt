package com.tairitsu.compose.arcaea

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
            idx = 11
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

                println(generateChartJson())
            }

            difficulties.eternal(-600) {
                chart.configuration.addItem("Version", 1.0)

                timing(
                    offset = 0,
                    bpm = 126,
                    beats = 4,
                )

                val tg = timingGroup {
                    timing(
                        offset = 0,
                        bpm = 126,
                        beats = 4,
                    )

                    holdNote(17140, 18807, 4)
                    arcNote(17140, 19045, 0.0 to 1.0, si, 1.0 to 1.0) {
                        arctap(17140)
                    }
                    arcNote(17140, 18569, 0 to 1, siso, 0.5 to 0) {
                        arctap(18569)
                    }
                    arcNote(17140, 18093, 0 to 1, siso, 0.25 to 0.25)
                    arcNote(17140, 17616, 0 to 1, siso, 0 to 0.5)

                    holdNote(19045, 20712, 1)

                }
                scenecontrol(19045, ScenecontrolType.TRACK_HIDE)

                println(generateChartJson())
                println(generateTimingGroupJson(tg.name))
                println(generateString())
            }
        }.writeToFolder(File(File("."), "result"))
    }

}