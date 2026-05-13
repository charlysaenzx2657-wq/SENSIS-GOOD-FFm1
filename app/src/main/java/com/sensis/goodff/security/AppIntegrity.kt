package com.sensis.goodff.security

import android.content.Context
import android.content.pm.PackageManager
import java.security.MessageDigest

object AppIntegrity {

    // SHA-256 del certificado de firma ORIGINAL (se llena en el CI con el keystore real)
    // Si el APK es modificado y re-firmado con otra key → firma diferente → acceso bloqueado
    private const val EXPECTED_CERT_SHA256 = "BUILD_CERT_HASH_PLACEHOLDER"

    // Package name hardcodeado — si alguien cambia el package y re-firma tampoco entra
    private const val EXPECTED_PACKAGE = "com.sensis.goodff"

    fun verify(ctx: Context): Boolean {
        return try {
            // 1. Verificar package name
            if (ctx.packageName != EXPECTED_PACKAGE) return false

            // 2. Verificar firma del certificado
            val pm = ctx.packageManager
            val sig = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(ctx.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()
            } ?: return false

            val md = MessageDigest.getInstance("SHA-256")
            val certHash = md.digest(sig.toByteArray())
                .joinToString("") { "%02x".format(it) }

            // Si el placeholder no fue reemplazado (debug), pasar
            if (EXPECTED_CERT_SHA256 == "BUILD_CERT_HASH_PLACEHOLDER") return true

            certHash == EXPECTED_CERT_SHA256

        } catch (e: Exception) {
            false
        }
    }

    // Detecta si corre en emulador (protección anti-análisis)
    fun isEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || android.os.Build.BRAND.startsWith("generic")
                || android.os.Build.DEVICE.startsWith("generic"))
    }
}
