package com.tairitsu.compose.parser

import com.tairitsu.compose.ArcTapNote
import com.tairitsu.compose.UniversalChartVisitor
import com.tairitsu.compose.arctap

class ArcCreateChartParser : ArcaeaChartParser() {

    companion object {
        val Instance by lazy { ArcCreateChartParser() }
    }

    override val arcTapNoteParser: (UniversalChartVisitor.Event, MutableList<ArcTapNote>) -> Unit = { evt, arcTapList ->
        arcTapList.run {
            when (evt.name) {
                "arctap" -> {
                    require(evt.values.size in 1..2)
                    require(evt.values[0].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                    if (evt.values.size == 2) {
                        require(evt.values[1].type == UniversalChartVisitor.ValueType.ALGEBRAIC)

                        add(ArcTapNote(evt.values[0].num.toLong(), evt.values[1].num.toDouble()))
                    } else {
                        val time = evt.values[0].num
                        arctap(time)
                    }
                }
            }
        }
    }

}