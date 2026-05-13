package com.sensis.goodff

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sensis.goodff.auth.EmailOtpManager
import com.sensis.goodff.security.AppIntegrity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var screenLogin: LinearLayout
    private lateinit var webViewGenerator: WebView
    private lateinit var btnSendOtp: Button
    private lateinit var btnVerifyOtp: Button
    private lateinit var progressOtp: ProgressBar
    private lateinit var tvOtpStatus: TextView
    private lateinit var layoutOtpInput: LinearLayout
    private lateinit var etOtpCode: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppIntegrity.verify(this)) {
            setContentView(R.layout.activity_tamper)
            return
        }

        setContentView(R.layout.activity_main)

        screenLogin      = findViewById(R.id.screenLogin)
        webViewGenerator = findViewById(R.id.webViewGenerator)
        btnSendOtp       = findViewById(R.id.btnSendOtp)
        btnVerifyOtp     = findViewById(R.id.btnVerifyOtp)
        progressOtp      = findViewById(R.id.progressOtp)
        tvOtpStatus      = findViewById(R.id.tvOtpStatus)
        layoutOtpInput   = findViewById(R.id.layoutOtpInput)
        etOtpCode        = findViewById(R.id.etOtpCode)

        // Configurar WebView
        webViewGenerator.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webViewGenerator.webViewClient = WebViewClient()

        // Sesión activa → ir directo al generador
        if (EmailOtpManager.isSessionActive(this)) {
            showGenerator()
        } else {
            showLogin()
        }

        btnSendOtp.setOnClickListener {
            lifecycleScope.launch {
                btnSendOtp.isEnabled = false
                progressOtp.visibility = View.VISIBLE
                tvOtpStatus.text = "Enviando código..."
                val result = EmailOtpManager.sendOtp(this@MainActivity)
                progressOtp.visibility = View.GONE
                btnSendOtp.isEnabled = true
                if (result.isSuccess) {
                    tvOtpStatus.text = "✅ Código enviado al correo del administrador"
                    layoutOtpInput.visibility = View.VISIBLE
                } else {
                    tvOtpStatus.text = "❌ ${result.exceptionOrNull()?.message}"
                }
            }
        }

        btnVerifyOtp.setOnClickListener {
            val code = etOtpCode.text.toString().trim()
            if (code.length != 6) {
                tvOtpStatus.text = "⚠️ El código tiene 6 dígitos"
                return@setOnClickListener
            }
            if (EmailOtpManager.verifyOtp(this, code)) {
                showGenerator()
            } else {
                tvOtpStatus.text = "❌ Código incorrecto o expirado"
                etOtpCode.text?.clear()
            }
        }
    }

    private fun showLogin() {
        screenLogin.visibility      = View.VISIBLE
        webViewGenerator.visibility = View.GONE
    }

    private fun showGenerator() {
        screenLogin.visibility      = View.GONE
        webViewGenerator.visibility = View.VISIBLE
        webViewGenerator.loadUrl("file:///android_asset/index.html")
    }
}
