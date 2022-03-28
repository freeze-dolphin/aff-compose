package com.lcwiro.demoMap

import com.tairitsu.compose.*

fun main() {
    mapSet {
        id = "demomap"
        titleLocalized = LocalizedString("Demo Map").apply {
            zhHans = "示例谱面"
            zhHant = "示範譜面"
            ja = "デモマップ"
            ko = "데모 맵"
        }

        difficulties.future {
            timing(
                offset = 0,
                bpm = 120,
                beats = 4,
            )

            (0..4).forEach { i ->
                normalNote(i * 1000 * 3, 1)
            }
            normalNote(14 * 1000 * 3, 1)


            timingGroup {
                timing(
                    offset = 0,
                    bpm = 120 * 4,
                    beats = 4,
                )
            }
        }
    }.build(path = "F:/demoMap/")
}
