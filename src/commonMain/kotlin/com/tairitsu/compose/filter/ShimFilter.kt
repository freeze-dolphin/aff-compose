package com.tairitsu.compose.filter

import com.tairitsu.compose.EventFilter
import com.tairitsu.compose.Scenecontrol
import com.tairitsu.compose.ScenecontrolType
import com.tairitsu.compose.TimedObject
import com.tairitsu.compose.TimingGroup
import com.tairitsu.compose.TimingGroupSpecialEffectFilter
import com.tairitsu.compose.toAffFormat
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Shim Filter
 *
 * Shim Filter is used to adapt event parameters from Arcaea for ArcCreate.
 * For example, param1 of trackdisplay scenecontrol is in seconds in Arcaea, but in ArcCreate it's in milliseconds.
 * The adapter will convert the parameters to the correct format.
 */
object ShimFilter {

    val needTimeConversion = listOf("trackdisplay", "arcahvdistort", "arcahvdebris")

    /**
     * Convert from Arcaea to ArcCreate
     */
    object A2C : EventFilter(), TimingGroupSpecialEffectFilter {
        override fun filterScenecontrol(sc: Scenecontrol): TimedObject = when (sc.type.value) {
            ScenecontrolType.TRACK_HIDE.value -> Scenecontrol(sc.time, ScenecontrolType.TRACK_DISPLAY) {
                listOf(
                    1000, 0
                ).map { it.toString() }
            }

            ScenecontrolType.TRACK_SHOW.value -> Scenecontrol(sc.time, ScenecontrolType.TRACK_DISPLAY) {
                listOf(
                    1000, 255
                ).map { it.toString() }
            }

            in needTimeConversion -> Scenecontrol(sc.time, sc.type) {
                listOf(
                    sc.params[0].toDouble().times(1000).roundToLong().toString(), sc.params[1]
                )
            }

            else -> sc
        }

        override fun filter(fx: TimingGroup.SpecialEffect): TimingGroup.SpecialEffect = when (fx.type.value) {
            "anglex" -> fx.copy(param = (fx.param!!.toDouble() / 10).toString())
            "angley" -> fx.copy(param = (fx.param!!.toDouble() / -10).toString())

            else -> fx
        }
    }

    /**
     * Convert from ArcCreate to Arcaea
     */
    object C2A : EventFilter(), TimingGroupSpecialEffectFilter {
        override fun filterScenecontrol(sc: Scenecontrol): TimedObject = when (sc.type.value) {
            in needTimeConversion -> Scenecontrol(sc.time, sc.type) {
                listOf(
                    sc.params[0].toDouble().times(0.001).toAffFormat(), sc.params[1]
                )
            }

            else -> sc
        }

        internal fun Double.toArcaeaAngleFormat(multiplyWith: Double = 1.0): String = if (this < 0) {
            ((3600 + (this * 10).roundToInt()) % 3600 * multiplyWith).toString()
        } else {
            ((this % 360) * 10 * multiplyWith).roundToInt().toString()
        }

        override fun filter(fx: TimingGroup.SpecialEffect): TimingGroup.SpecialEffect = when (fx.type.value) {
            "anglex" -> fx.copy(param = fx.param!!.toDouble().toArcaeaAngleFormat())
            "angley" -> fx.copy(param = fx.param!!.toDouble().toArcaeaAngleFormat(-1.0))

            else -> fx
        }
    }
}