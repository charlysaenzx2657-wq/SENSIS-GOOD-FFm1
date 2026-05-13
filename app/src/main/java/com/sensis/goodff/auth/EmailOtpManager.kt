package com.sensis.goodff.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object EmailOtpManager {

    // ── EmailJS config ──────────────────────────────────────────
    private const val EMAILJS_SERVICE_ID  = "service_fp49ijg"   // <-- tu Service ID
    private const val EMAILJS_TEMPLATE_ID = "template_430q62h"  // <-- tu Template ID
    private const val EMAILJS_PUBLIC_KEY  = "YNPoHuNx2oA3WGu4I"   // <-- tu Public Key
    private const val OWNER_EMAIL         = "charlysaenzx2657@gmail.com"
    // ────────────────────────────────────────────────────────────

    private const val PREFS_NAME   = "sgff_auth"
    private const val KEY_SESSION  = "session_ok"
    private const val KEY_EMAIL    = "user_email"
    private const val OTP_EXPIRY_MS = 10 * 60 * 1000L // 10 minutos

    private var pendingOtp: String? = null
    private var otpGeneratedAt: Long = 0L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // SharedPreferences cifrado (no legible fuera del app)
    private fun getPrefs(ctx: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx, PREFS_NAME, masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Genera y envía OTP al correo del dueño */
    suspend fun sendOtp(ctx: Context): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val code = (100000..999999).random(Random(System.currentTimeMillis())).toString()
            pendingOtp     = code
            otpGeneratedAt = System.currentTimeMillis()

            val deviceModel = android.os.Build.MODEL
            val deviceId    = android.os.Build.SERIAL.take(8).ifEmpty { "XXXXX" }

            val body = JSONObject().apply {
                put("service_id",  EMAILJS_SERVICE_ID)
                put("template_id", EMAILJS_TEMPLATE_ID)
                put("user_id",     EMAILJS_PUBLIC_KEY)
                put("template_params", JSONObject().apply {
                    put("to_email",    OWNER_EMAIL)
                    put("otp_code",    code)
                    put("device",      deviceModel)
                    put("device_id",   deviceId)
                    put("app_version", "v3.2")
                })
            }.toString()

            val request = Request.Builder()
                .url("https://api.emailjs.com/api/v1.0/email/send")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("EmailJS error ${response.code}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Verifica el código ingresado */
    fun verifyOtp(ctx: Context, inputCode: String): Boolean {
        val otp = pendingOtp ?: return false
        val elapsed = System.currentTimeMillis() - otpGeneratedAt
        if (elapsed > OTP_EXPIRY_MS) { pendingOtp = null; return false }
        val ok = inputCode.trim() == otp
        if (ok) {
            pendingOtp = null
            getPrefs(ctx).edit()
                .putBoolean(KEY_SESSION, true)
                .putLong("session_ts", System.currentTimeMillis())
                .apply()
        }
        return ok
    }

    /** Sesión ya activa (guardada de sesión anterior) */
    fun isSessionActive(ctx: Context): Boolean {
        val prefs = getPrefs(ctx)
        return prefs.getBoolean(KEY_SESSION, false)
    }

    fun clearSession(ctx: Context) {
        getPrefs(ctx).edit().clear().apply()
    }
}
