package com.sensis.goodff

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.sensis.goodff.security.AppIntegrity

class MainActivity : AppCompatActivity() {

    private lateinit var webViewGenerator: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppIntegrity.verify(this)) {
            setContentView(R.layout.activity_tamper)
            return
        }

        setContentView(R.layout.activity_main)
        webViewGenerator = findViewById(R.id.webViewGenerator)

        webViewGenerator.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
        webViewGenerator.webViewClient = WebViewClient()

        webViewGenerator.loadUrl("file:///android_asset/index.html")
    }
}
