package com.tairitsu.compose.arcaea

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChartSerialzationTest {

    @Test
    fun main() {
        val chart = Json.decodeFromString<Chart>(
            """
            {"configuration":{"audioOffset":-660,"extra":[]},"mainTiming":{"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"scenecontrols":[{"time":19045,"type":"TRACK_HIDE","param1":null,"param2":null}]},"subTiming":{"0bfa3477-8a57-4929-b76e-cb45e05bdac2":{"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"notes":[{"type":"hold","time":17140,"endTime":18807,"column":4},{"type":"arc","time":17140,"endTime":19045,"startPosition":{"x":0.0,"y":1.0},"curveType":"si","endPosition":{"x":1.0,"y":1.0},"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[17140]},{"type":"arc","time":17140,"endTime":18569,"startPosition":{"x":0.0,"y":1.0},"curveType":"siso","endPosition":{"x":0.5,"y":0.0},"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18569]},{"type":"arc","time":17140,"endTime":18093,"startPosition":{"x":0.0,"y":1.0},"curveType":"siso","endPosition":{"x":0.25,"y":0.25},"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"arc","time":17140,"endTime":17616,"startPosition":{"x":0.0,"y":1.0},"curveType":"siso","endPosition":{"x":0.0,"y":0.5},"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"hold","time":19045,"endTime":20712,"column":1}]}}}
            """.trimIndent()
        )
    }

}
