package com.tairitsu.compose.arcaea

import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChartSerialzationTest {

    @Test
    fun main() {
        Json.decodeFromString<Chart>(
            """
            {"configuration":{"audioOffset":-600,"extra":[{"name":"Version","value":"1.0"}]},"mainTiming":{"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"notes":[{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]}],"scenecontrols":[{"time":19045,"type":"TRACK_HIDE","param1":null,"param2":null}]},"subTiming":{"d11693ee-2be5-49f1-90fc-ff75510c5d68":{"specialEffects":[{"effect":"FADING_HOLDS","extraParam":null},{"effect":"ANGLEX","extraParam":3600}],"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"notes":[{"type":"hold","time":17140,"endTime":18807,"column":4},{"type":"arc","time":17140,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"si","endPosition":[1.0,1.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[17140]},{"type":"arc","time":17140,"endTime":18569,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.5,0.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18569]},{"type":"arc","time":17140,"endTime":18093,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.25,0.25],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18093]},{"type":"arc","time":17140,"endTime":17616,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.0,0.5],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"arc","time":19045,"endTime":20712,"startPosition":[1.25,1.0],"curveType":"b","endPosition":[-0.5,0.0],"color":1,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":19045,"endTime":20712,"startPosition":[-0.25,1.0],"curveType":"b","endPosition":[1.5,0.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"hold","time":19045,"endTime":20712,"column":4}]}}}
            """.trimIndent()
        )
    }

}
