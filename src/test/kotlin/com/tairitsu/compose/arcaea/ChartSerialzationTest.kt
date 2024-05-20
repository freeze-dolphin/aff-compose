package com.tairitsu.compose.arcaea

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChartSerialzationTest {

    @Test
    fun `test deserialize from json`() {
        Json.decodeFromString<Chart>(
            """
            {"configuration":{"audioOffset":-600,"extra":[{"name":"Version","value":"1.0"}]},"mainTiming":{"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"notes":[{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":19045,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"s","endPosition":[1.0,1.0],"color":3,"hitSound":"none","isGuidingLine":false,"tapList":[]}],"scenecontrols":[{"time":19045,"type":"TRACK_HIDE","param1":null,"param2":null}]},"subTiming":{"d11693ee-2be5-49f1-90fc-ff75510c5d68":{"specialEffects":[{"effect":"FADING_HOLDS","extraParam":null},{"effect":"ANGLEX","extraParam":3600}],"timing":[{"offset":0,"bpm":126.0,"beats":4.0}],"notes":[{"type":"hold","time":17140,"endTime":18807,"column":4},{"type":"arc","time":17140,"endTime":19045,"startPosition":[0.0,1.0],"curveType":"si","endPosition":[1.0,1.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[17140]},{"type":"arc","time":17140,"endTime":18569,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.5,0.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18569]},{"type":"arc","time":17140,"endTime":18093,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.25,0.25],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[18093]},{"type":"arc","time":17140,"endTime":17616,"startPosition":[0.0,1.0],"curveType":"siso","endPosition":[0.0,0.5],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"arc","time":19045,"endTime":20712,"startPosition":[1.25,1.0],"curveType":"b","endPosition":[-0.5,0.0],"color":1,"hitSound":"none","isGuidingLine":false,"tapList":[]},{"type":"arc","time":19045,"endTime":20712,"startPosition":[-0.25,1.0],"curveType":"b","endPosition":[1.5,0.0],"color":0,"hitSound":"none","isGuidingLine":true,"tapList":[]},{"type":"hold","time":19045,"endTime":20712,"column":4}]}}}
            """.trimIndent()
        )
    }

    @Test
    fun `test deserialize from aff`() {
        val affString = """
            AudioOffset:-600
            TimingPointDensityFactor:1
            Version:1.0
            -
            timing(0,126.00,4.00);
            scenecontrol(19045,trackhide);
            (1,2);
            arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
            scenecontrol(40960,redline,1.88,0);
            arc(17140,19045,0.00,1.00,s,1.00,1.00,3,none,false)[arctap(17140),arctap(19045)];
            arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
            timinggroup(fadingholds_anglex3600){
                timing(0,126.00,4.00);
                hold(17140,18807,4);
                arc(17140,19045,0.00,1.00,si,1.00,1.00,0,none,true)[arctap(17140),arctap(19045)];
                arc(17140,18569,0.00,0.50,siso,1.00,0.00,0,none,true)[arctap(18569)];
                arc(17140,18093,0.00,0.25,siso,1.00,0.25,0,none,true)[arctap(18093)];
                arc(17140,17616,0.00,0.00,siso,1.00,0.50,0,none,true);
                hold(19045,20712,4);
                arc(19045,20712,-0.25,1.50,b,1.00,0.00,0,none,true);
                arc(19045,20712,1.25,-0.50,b,1.00,0.00,1,none,false);
            };
            timinggroup(){
                timing(0,216.00,4.00);
                camera(21112,0.00,0.00,1.00,0.00,0.00,0.00,l,1);
                camera(38750,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(40000,0.00,-900.00,500.00,0.00,30.00,90.00,qo,556);
                camera(58887,0.00,900.00,-500.00,0.00,20.00,90.00,qo,556);
                camera(68333,0.00,0.00,0.00,0.00,-50.00,180.00,l,1111);
                camera(69444,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(78333,1700.00,0.00,0.00,0.00,0.00,0.00,qo,2424);
                camera(82122,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,453);
                camera(82575,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(84546,1700.00,0.00,0.00,0.00,0.00,0.00,qo,453);
                camera(84999,1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(86970,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,453);
                camera(87423,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(89394,1700.00,0.00,0.00,0.00,0.00,0.00,qi,453);
                camera(89847,1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(91819,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,453);
                camera(92272,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(94243,1700.00,0.00,0.00,0.00,0.00,0.00,qo,453);
                camera(94696,1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(96667,-1700.00,0.00,0.00,0.00,0.00,0.00,qi,453);
                camera(97120,-1700.00,0.00,0.00,0.00,0.00,0.00,qo,605);
                camera(99091,1700.00,0.00,0.00,0.00,0.00,0.00,qo,453);
                camera(99544,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(100151,1700.00,0.00,0.00,0.00,0.00,0.00,l,1212);
                camera(101363,-3400.00,0.00,0.00,0.00,0.00,0.00,l,6061);
                camera(107423,850.00,0.00,0.00,0.00,0.00,0.00,qi,302);
                camera(107725,850.00,0.00,0.00,0.00,0.00,0.00,qo,302);
                camera(108028,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(108636,0.00,0.00,150.00,0.00,0.00,0.00,qi,1111);
                camera(109747,0.00,0.00,0.00,0.00,0.00,-10.00,qo,277);
                camera(111691,0.00,0.00,0.00,0.00,0.00,10.00,l,277);
                camera(111969,0.00,0.00,0.00,0.00,0.00,10.00,qo,277);
                camera(113914,0.00,0.00,0.00,0.00,0.00,-10.00,l,277);
                camera(114191,0.00,0.00,0.00,0.00,0.00,-10.00,qo,277);
                camera(116136,0.00,0.00,0.00,0.00,0.00,10.00,l,277);
                camera(116414,0.00,0.00,0.00,0.00,0.00,10.00,qo,277);
                camera(118358,0.00,0.00,0.00,0.00,0.00,-10.00,qi,277);
                camera(118636,0.00,0.00,0.00,0.00,0.00,-10.00,qo,277);
                camera(120580,0.00,0.00,0.00,0.00,0.00,10.00,l,277);
                camera(120858,0.00,0.00,0.00,0.00,0.00,10.00,qo,277);
                camera(122803,0.00,0.00,0.00,0.00,0.00,-10.00,l,277);
                camera(123080,0.00,0.00,0.00,0.00,0.00,-10.00,qo,277);
                camera(124955,0.00,0.00,-150.00,0.00,0.00,10.00,qo,277);
                camera(125303,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(127523,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(145302,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(150580,0.00,0.00,0.00,0.00,0.00,0.00,reset,0);
                camera(152664,0.00,-900.00,500.00,0.00,30.00,-90.00,qo,277);
            };
        """.trimIndent()

        val aff = affString.split("\n").joinToString("\r\n")

        Json.encodeToString(
            Chart.fromAff(aff)
        ).let {
            val rst = Json.decodeFromString<Chart>(it).serializeForArcaea()
            println(rst)
        }
    }

}
