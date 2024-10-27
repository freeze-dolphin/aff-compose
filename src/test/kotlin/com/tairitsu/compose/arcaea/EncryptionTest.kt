package com.tairitsu.compose.arcaea

import com.tairitsu.compose.arcaea.Encryption.encryptText
import com.tairitsu.compose.arcaea.Encryption.serializeToCbor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.Provider
import java.security.Security
import java.util.*
import java.util.stream.Collectors
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals


class EncryptionTest {

    private val affText = """
        AudioOffset:-600
        Version:1.0
        -
        timing(0,126.00,4.00);
        scenecontrol(19045,trackhide);
        arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
        arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
        arc(19045,19045,0.00,1.00,s,1.00,1.00,3,none,false);
        timinggroup(fadingholds_anglex3500){
            timing(0,126.00,4.00);
            hold(17140,18807,4);
            arc(17140,19045,0.00,1.00,si,1.00,1.00,0,none,true)[arctap(17140)];
            arc(17140,18569,0.00,0.50,siso,1.00,0.00,0,none,true)[arctap(18569)];
            arc(17140,18093,0.00,0.25,siso,1.00,0.25,0,none,true)[arctap(18093)];
            arc(17140,17616,0.00,0.00,siso,1.00,0.50,0,none,true);
            hold(19045,20712,4);
            arc(19045,20712,-0.25,1.50,b,1.00,0.00,0,none,true);
            arc(19045,20712,1.25,-0.50,b,1.00,0.00,1,none,false);
        };
    """.trimIndent().split("\n").joinToString("\r\n")

    private val chart = Chart.fromAff(affText)

    @Test
    fun `list algorithms`() {
        Arrays.stream(Security.getProviders())
            .flatMap { provider: Provider ->
                provider.services.stream()
            }
            .filter { service: Provider.Service -> "Cipher" == service.type }
            .map<Any>(Provider.Service::getAlgorithm)
            .collect(Collectors.toList())
            .map {
                println(it)
            }
    }

    @Test
    fun `test text encryption`() {
        val alg = "AES/CBC/PKCS5Padding"
        val key = SecretKeySpec("QE1yY2C7vtPLBPw1".toByteArray(), "AES")
        val iv = IvParameterSpec(ByteArray(16))
        val encrypted = chart.encryptText(alg, key, iv)

        assertEquals(affText, Encryption.decryptTextChart(encrypted, alg, key, iv).serialize())
    }

    @Test
    fun `test binary encryption`() {
        val file = Path(".", "result", "encrypted.bff").toFile()
        val password = "putabc".toByteArray()

        val xorClosure: ByteArray.() -> Unit = {
            this.forEachIndexed { idx, byte ->
                val pbyte = password[idx % password.size]
                if (byte != 0x0.toByte() && byte != pbyte) this[idx] = byte.xor(pbyte)
            }
        }

        file.writeBytes(chart.serializeToCbor(xorClosure))

        assertEquals(affText, Encryption.deserializeFromCbor(file.readBytes(), xorClosure).serialize())
    }

}