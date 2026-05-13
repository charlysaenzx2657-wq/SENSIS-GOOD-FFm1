package com.sensis.goodff.generator

import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.random.Random

data class SensibilidadResult(
    val marca: String,
    val modelo: String,
    val general: Int,
    val miraRojo: Int,
    val mira2x: Int,
    val mira4x: Int,
    val francotirador: Int,
    val camara360: Int,
    val botonDisparo: Int,
    val dpi: Int,          // 0 = sin DPI
    val nivelSensi: String, // baja / media / alta
    val modoHS: String,     // full / mid / normal
    val grupoHardware: String
)

object SensibilidadEngine {

    private fun rand(a: Int, b: Int) = Random.nextInt(a, b + 1)
    private fun clamp(v: Int, a: Int, b: Int) = max(a, min(b, v))

    // ── Grupos de marcas ─────────────────────────────────────────────────────
    private val GA  = setOf("Samsung","Google Pixel","OnePlus","Sony","Sony Ericsson","Razer","Sharp","Nubia","RED")
    private val GB  = setOf("Motorola","Nokia","LG","HTC","Asus","Lenovo","TCL","Meizu","Microsoft")
    private val GB2 = setOf("Xiaomi","Poco")
    private val GB3 = setOf("Honor","Huawei","HMD Global")
    private val GB4 = setOf("Realme","Oppo","Vivo")
    private val GC  = setOf("Infinix","Tecno","Itel","Alcatel","BLU","Wiko","Blackview","Doogee",
                            "Oukitel","Cubot","Ulefone","UMIDIGI","Gionee","Micromax","Lava",
                            "Intex","Karbonn","Lanix","Hyundai","Sky Devices","Condor","BQ",
                            "Kyocera","Panasonic","Philips","Prestigio","Toshiba","Vernee")

    private fun grupo(marca: String): String {
        if (marca == "iPhone") return "E"
        val vipKw = listOf("SENSI","VIP","Modificado")
        if (vipKw.any { marca.contains(it) }) return "V"
        if (GA.contains(marca))  return "A"
        if (GB.contains(marca))  return "B"
        if (GB2.contains(marca)) return "B2"
        if (GB3.contains(marca)) return "B3"
        if (GB4.contains(marca)) return "B4"
        if (GC.contains(marca))  return "C"
        return "D"
    }

    // ── Perfiles de calibración ───────────────────────────────────────────────
    data class Perfil(
        val genMin: Int, val genMax: Int,
        val rojoD: Pair<Int,Int>,
        val x2D: Pair<Int,Int>,
        val franMin: Int, val franMax: Int,
        val camaraD: Pair<Int,Int>,
        val dpiMin: Int, val dpiMax: Int
    )

    private val PERFILES = mapOf(
        "A"  to Perfil(144,200, 0 to 3, -12 to -6, 62,76, 16 to 28, 530,610),
        "B"  to Perfil(145,196, 0 to 3, -12 to -6, 55,72, 14 to 26, 530,600),
        "B2" to Perfil(143,188, 0 to 2, -14 to -7, 53,70, 13 to 24, 530,595),
        "B3" to Perfil(146,193, 0 to 2, -13 to -7, 55,73, 14 to 26, 530,598),
        "B4" to Perfil(143,196, 0 to 3, -13 to -7, 57,75, 15 to 27, 530,602),
        "C"  to Perfil(148,182, 0 to 2, -10 to -5, 50,68, 18 to 32, 530,580),
        "D"  to Perfil(145,192, 0 to 3, -12 to -6, 54,71, 14 to 26, 530,596),
        "E"  to Perfil( 85,200, 0 to 4, -18 to -10, 52,70, 20 to 36,   0,  0),
        "V"  to Perfil(162,200, 0 to 2,  -6 to -2, 66,80,  8 to 16, 600,630)
    )

    // Cola de niveles
    private var queueIdx = 0
    private val COLA = listOf("baja","baja","media","media","media","media","alta","alta")

    fun generar(marca: String, modelo: String, usarDpi: Boolean): SensibilidadResult {
        val grp = grupo(marca)
        val isIphone = grp == "E"
        val isVip = listOf("SENSI PREMIUM","SENSI XIT","Sensibilidad VIP","Teléfono Modificado").any { marca.contains(it) }
        val p = PERFILES[grp] ?: PERFILES["D"]!!

        // Resolución y Hz default (sin API, usar valores de perfil base)
        val resCat = "fhd"
        val deviceHz = 60
        val devicePanel = "ips"

        val hzBonus = when {
            deviceHz >= 240 -> 8; deviceHz >= 165 -> 6; deviceHz >= 144 -> 5
            deviceHz >= 120 -> 3; deviceHz >= 90  -> 2; else            -> 0
        }
        val resAdj = when (resCat) { "hd" -> 3; "qhd" -> -3; else -> 0 } + hzBonus
        val panelBonus = when (devicePanel) { "amoled","oled" -> 2; "lcd" -> -1; else -> 0 }
        val totalAdj = resAdj + panelBonus

        // Nivel de sensibilidad (cola)
        val nivelSensi = COLA[queueIdx % 8]
        queueIdx++

        // Rango del General según nivel
        val (genLo, genHi) = if (isIphone) {
            when (nivelSensi) {
                "baja"  -> 89 to 120
                "media" -> 121 to 169
                else    -> 172 to 200
            }
        } else {
            when (nivelSensi) {
                "baja"  -> clamp(max(p.genMin,149),149,158) to clamp(min(158,p.genMax),149,158)
                "media" -> clamp(max(p.genMin,159),159,171) to clamp(min(171,p.genMax),159,171)
                else    -> clamp(max(p.genMin,176),176,200) to clamp(min(200,p.genMax),176,200)
            }
        }
        val genLoF = if (genLo > genHi) p.genMin else genLo
        val genHiF = if (genLo > genHi) p.genMax else genHi
        val general = clamp(rand(genLoF, genHiF) + totalAdj, 100, 200)

        // Modo Headshot
        val hsRoll = Random.nextFloat()
        val modoHS = when { hsRoll < 0.60f -> "full"; hsRoll < 0.90f -> "mid"; else -> "normal" }

        // Rojo
        val rojoResBonus  = when (resCat) { "qhd" -> 3; "hd" -> -1; else -> 0 }
        val rojoPanelBonus = when (devicePanel) { "amoled","oled" -> 3; "lcd" -> -1; else -> 0 }
        val rojoHzBonus   = when { deviceHz >= 144 -> 4; deviceHz >= 120 -> 3; deviceHz >= 90 -> 1; else -> 0 }
        val rojoBonus = rojoResBonus + rojoHzBonus + rojoPanelBonus
        val rojoMinDelta = 2
        var rojo = when {
            nivelSensi == "baja" -> when (modoHS) {
                "full"   -> clamp(general + max(p.rojoD.first,  rojoMinDelta) + rojoBonus, 100, 200)
                "mid"    -> clamp(general + max((p.rojoD.first+p.rojoD.second)/2, rojoMinDelta) + rojoBonus, 100, 200)
                else     -> clamp(general + max(p.rojoD.second, rojoMinDelta+1) + rojoBonus, 100, 200)
            }
            modoHS == "full" -> clamp(general + p.rojoD.first  + rojoBonus, 100, 200)
            modoHS == "mid"  -> clamp(general + (p.rojoD.first+p.rojoD.second)/2 + rojoBonus, 100, 200)
            else             -> clamp(general + p.rojoD.second + rojoBonus, 100, 200)
        }

        val genEfec = (general + rojo) / 2

        // 2X y 4X
        val zoomResAdj = when (resCat) { "qhd" -> 3; "hd" -> -3; else -> 0 }
        val zoomHzAdj  = when { deviceHz >= 144 -> 5; deviceHz >= 120 -> 3; deviceHz >= 90 -> 2; else -> 0 }
        val zoomTotal  = zoomResAdj + zoomHzAdj
        val x2dLo = Math.abs(p.x2D.second)
        val x2dHi = Math.abs(p.x2D.first)

        var x2 = when (nivelSensi) {
            "baja" -> {
                val d2 = rand(x2dLo, x2dLo + ((x2dHi-x2dLo)*0.55).toInt()) - zoomTotal
                clamp(rojo - max(d2, x2dLo), 100, rojo - x2dLo)
            }
            "media" -> {
                val d2 = rand(x2dLo + ((x2dHi-x2dLo)*0.25).toInt(), x2dLo + ((x2dHi-x2dLo)*0.75).toInt()) - zoomTotal
                clamp(rojo - max(d2, x2dLo), 100, rojo - x2dLo)
            }
            else -> {
                val d2 = rand(x2dLo, x2dLo + ((x2dHi-x2dLo)*0.45).toInt()) - zoomTotal
                clamp(rojo - max(d2, x2dLo), 100, rojo - x2dLo)
            }
        }
        var x4 = clamp(x2 + rand(20, 27), 100, 200)

        // Francotirador
        val franResAdj = (if (resCat=="qhd") -1 else if (resCat=="hd") 1 else 0) + (if (deviceHz>=144) 1 else 0)
        val franAbsMin = max(p.franMin, 100)
        val franAbsMax = min(p.franMax, 200)
        val fDiff = franAbsMax - franAbsMin
        val (franLo, franHi) = when (nivelSensi) {
            "baja"  -> franAbsMin to (franAbsMin + fDiff*0.40).toInt()
            "media" -> (franAbsMin + fDiff*0.20).toInt() to (franAbsMin + fDiff*0.70).toInt()
            else    -> (franAbsMin + fDiff*0.48).toInt() to franAbsMax
        }
        var franco = clamp(rand(franLo, franHi) + franResAdj, franAbsMin, franAbsMax)

        // Cámara 360°
        val camResAdj = (if (resCat=="hd") 3 else if (resCat=="qhd") -1 else 0) +
                        (if (deviceHz>=120) 2 else if (deviceHz>=90) 1 else 0)
        val cDiff = p.camaraD.second - p.camaraD.first
        val (camDlo, camDhi) = when (nivelSensi) {
            "baja"  -> p.camaraD.first to (p.camaraD.first + cDiff*0.38).toInt()
            "media" -> (p.camaraD.first + cDiff*0.22).toInt() to (p.camaraD.first + cDiff*0.72).toInt()
            else    -> (p.camaraD.first + cDiff*(if(modoHS=="full") 0.45 else 0.50)).toInt() to p.camaraD.second
        }
        val camara = clamp(general + rand(camDlo, camDhi) + camResAdj, 100, 200)

        // Botón
        val btnResAdj = (if (resCat=="qhd") -1 else if (resCat=="hd") 1 else 0) +
                        (if (deviceHz>=120) -1 else 0) +
                        (if (devicePanel=="amoled"||devicePanel=="oled") 1 else 0)

        fun botonPorNivel(): Int = when (nivelSensi) {
            "baja"  -> when(modoHS) { "full"->rand(32,34); "mid"->rand(33,35); else->rand(34,36) }
            "media" -> when(modoHS) { "full"->rand(36,39); "mid"->rand(38,41); else->rand(40,43) }
            else    -> when(modoHS) { "full"->rand(40,43); "mid"->rand(42,46); else->rand(44,49) }
        }

        var boton: Int
        var dpiVal = 0
        if (isIphone || !usarDpi) {
            boton = if (isIphone) {
                clamp(when(nivelSensi){"baja"->rand(38,46);"media"->rand(42,49);else->rand(44,52)}, 35, 52)
            } else botonPorNivel()
        } else {
            val dLo = p.dpiMin; val dHi = p.dpiMax
            dpiVal = when (nivelSensi) {
                "baja"  -> rand((dLo+(dHi-dLo)*0.55).toInt(), dHi)
                "media" -> rand((dLo+(dHi-dLo)*0.25).toInt(), (dLo+(dHi-dLo)*0.75).toInt())
                else    -> rand(dLo, (dLo+(dHi-dLo)*0.45).toInt())
            }
            dpiVal = clamp(dpiVal + rand(-4,4), 530, 630)
            boton = botonPorNivel()
        }
        boton = clamp(boton + btnResAdj, 32, 52)
        val rojoGenDelta = rojo - general
        boton = when {
            modoHS=="full"||rojoGenDelta>=3 -> clamp(boton - rand(2,3), 32, boton)
            rojoGenDelta>=1                 -> clamp(boton - 1, 32, boton)
            else                            -> boton
        }

        // Validación coherencia
        if (rojo <= general)  rojo   = clamp(general + 1, 100, 200)
        if (x2   >= rojo)     x2     = clamp(rojo - x2dLo, 100, rojo - 1)
        if (x4   <= x2)       x4     = clamp(x2 + 20, 100, 200)
        if (franco >= x2)     franco = clamp(x2 - 1, franAbsMin, x2 - 1)
        rojo   = clamp(rojo,   100, 200)
        x2     = clamp(x2,     100, 200)
        x4     = clamp(x4,     100, 200)
        franco = clamp(franco, franAbsMin, franAbsMax)

        return SensibilidadResult(
            marca, modelo, general, rojo, x2, x4, franco, camara, boton,
            dpiVal, nivelSensi, modoHS, grp
        )
    }
}
