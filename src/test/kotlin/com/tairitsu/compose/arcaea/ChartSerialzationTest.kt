package com.tairitsu.compose.arcaea

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChartSerialzationTest {

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
            };
        """.trimIndent()

        val aff = affString.split("\n").joinToString("\r\n")

        Json.encodeToString(
            Chart.fromAff(aff)
        ).let {
            val dec = Json.decodeFromString<Chart>(it)
            val rstAff = dec.serializeForArcaea()
            val rstAcf = dec.serializeForArcCreate()
            println(rstAff)
        }
    }

}
