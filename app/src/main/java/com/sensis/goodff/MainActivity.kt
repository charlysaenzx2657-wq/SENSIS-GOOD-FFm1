package com.sensis.goodff

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sensis.goodff.auth.EmailOtpManager
import com.sensis.goodff.databinding.ActivityMainBinding
import com.sensis.goodff.generator.SensibilidadEngine
import com.sensis.goodff.security.AppIntegrity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ── 1. Verificar integridad del APK ─────────────────────────
        if (!AppIntegrity.verify(this)) {
            showTamperScreen()
            return
        }

        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ── 2. Verificar sesión guardada ──────────────────────────────
        if (EmailOtpManager.isSessionActive(this)) {
            showGenerator()
        } else {
            showLoginScreen()
        }

        setupListeners()
    }

    private fun setupListeners() {
        // Botón: Enviar código OTP
        b.btnSendOtp.setOnClickListener {
            lifecycleScope.launch {
                b.btnSendOtp.isEnabled = false
                b.progressOtp.visibility = View.VISIBLE
                b.tvOtpStatus.text = "Enviando código..."

                val result = EmailOtpManager.sendOtp(this@MainActivity)
                b.progressOtp.visibility = View.GONE
                b.btnSendOtp.isEnabled = true

                if (result.isSuccess) {
                    b.tvOtpStatus.text = "✅ Código enviado al correo del administrador"
                    b.layoutOtpInput.visibility = View.VISIBLE
                } else {
                    b.tvOtpStatus.text = "❌ ${result.exceptionOrNull()?.message}"
                }
            }
        }

        // Botón: Verificar código OTP
        b.btnVerifyOtp.setOnClickListener {
            val code = b.etOtpCode.text.toString().trim()
            if (code.length != 6) {
                b.tvOtpStatus.text = "⚠️ El código tiene 6 dígitos"
                return@setOnClickListener
            }
            if (EmailOtpManager.verifyOtp(this, code)) {
                Toast.makeText(this, "✅ Acceso concedido", Toast.LENGTH_SHORT).show()
                showGenerator()
            } else {
                b.tvOtpStatus.text = "❌ Código incorrecto o expirado"
                b.etOtpCode.text?.clear()
            }
        }

        // Botón: Generar sensibilidades
        b.btnGenerar.setOnClickListener {
            val marca  = b.spinnerMarca.selectedItem?.toString() ?: ""
            val modelo = b.etModelo.text.toString().trim()
            val usarDpi = b.switchDpi.isChecked

            if (marca.isEmpty() || marca == "Selecciona tu marca") {
                b.tvError.text = "⚠️ Selecciona tu marca"
                b.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            if (modelo.isEmpty()) {
                b.tvError.text = "⚠️ Escribe el modelo"
                b.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            b.tvError.visibility = View.GONE

            val result = SensibilidadEngine.generar(marca, modelo, usarDpi)
            displayResult(result)
        }

        // Botón: Copiar resultado
        b.btnCopiar.setOnClickListener {
            val txt = b.tvResultado.text.toString()
            val clipboard = getSystemService(android.content.ClipboardManager::class.java)
            val clip = android.content.ClipData.newPlainText("SENSIS GOOD FF", txt)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, "📋 Copiado", Toast.LENGTH_SHORT).show()
        }

        // Cerrar sesión
        b.btnLogout.setOnClickListener {
            EmailOtpManager.clearSession(this)
            showLoginScreen()
        }
    }

    private fun displayResult(r: com.sensis.goodff.generator.SensibilidadResult) {
        val nivelEmoji = when(r.nivelSensi) { "baja" -> "🟢"; "media" -> "🔵"; else -> "🔴" }
        val hsLabel = when(r.modoHS) { "full" -> "🎯 CONFIG HEADSHOT"; "mid" -> "🔥 CONFIG HEADSHOT"; else -> "" }

        val sb = StringBuilder()
        sb.appendLine("=======================")
        sb.appendLine(" |📱 MARCA: ${r.marca}")
        sb.appendLine(" |📱 MODELO: ${r.modelo}")
        sb.appendLine(" |$nivelEmoji NIVEL: ${r.nivelSensi.uppercase()}")
        if (hsLabel.isNotEmpty()) sb.appendLine(" |$hsLabel")
        sb.appendLine("=======================")
        sb.appendLine(" |🎯 General        : ${r.general}")
        sb.appendLine(" |🔴 Mira Rojo      : ${r.miraRojo}")
        sb.appendLine(" |🔍 Mira 2X        : ${r.mira2x}")
        sb.appendLine(" |🎯 Mira 4X        : ${r.mira4x}")
        sb.appendLine(" |⚫ Francotirador  : ${r.francotirador}")
        sb.appendLine(" |📸 Cámara 360°    : ${r.camara360}")
        sb.appendLine(" |🔘 Botón Disparo  : ${r.botonDisparo}")
        if (r.dpi > 0) sb.appendLine(" |🔵 DPI            : ${r.dpi}")
        sb.appendLine("=======================")
        sb.appendLine(" ⚡ SENSIS GOOD FF v3.2")

        b.tvResultado.text = sb.toString()
        b.cardResultado.visibility = View.VISIBLE

        // Vibración
        val vibrator = getSystemService(android.os.Vibrator::class.java)
        val pattern = when {
            r.botonDisparo <= 35 -> longArrayOf(0,30,15,30)
            r.botonDisparo <= 45 -> longArrayOf(0,50)
            else                 -> longArrayOf(0,80,20,40)
        }
        vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
    }

    private fun showLoginScreen() {
        b.screenLogin.visibility    = View.VISIBLE
        b.screenGenerator.visibility = View.GONE
        b.screenTamper.visibility   = View.GONE
        b.layoutOtpInput.visibility = View.GONE
        b.tvOtpStatus.text = ""
    }

    private fun showGenerator() {
        b.screenLogin.visibility    = View.GONE
        b.screenGenerator.visibility = View.VISIBLE
        b.screenTamper.visibility   = View.GONE
    }

    private fun showTamperScreen() {
        setContentView(R.layout.activity_tamper)
    }
}
