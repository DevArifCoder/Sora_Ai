package com.arif.aiassistant

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private var speechRecognizer: SpeechRecognizer? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(JsBridge(), "Android")
        webView.loadUrl("file:///android_asset/index.html")

        requestRuntimePermissions()
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.RECORD_AUDIO)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
        }
    }

    private fun startSpeechRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            webView.evaluateJavascript(
                "onSpeechError(${JSONObject.quote("Speech recognizer is available na ei device a")})",
                null
            )
            return
        }
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                webView.evaluateJavascript("onSpeechResult(${JSONObject.quote(text)})", null)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                webView.evaluateJavascript("onSpeechPartial(${JSONObject.quote(text)})", null)
            }

            override fun onError(error: Int) {
                webView.evaluateJavascript(
                    "onSpeechError(${JSONObject.quote("error code $error")})",
                    null
                )
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    inner class JsBridge {

        @JavascriptInterface
        fun startListening() {
            runOnUiThread { startSpeechRecognition() }
        }

        @JavascriptInterface
        fun stopListening() {
            runOnUiThread { speechRecognizer?.stopListening() }
        }

        @JavascriptInterface
        fun startBackgroundMic() {
            runOnUiThread {
                val intent = Intent(this@MainActivity, MicForegroundService::class.java)
                ContextCompat.startForegroundService(this@MainActivity, intent)
            }
        }

        @JavascriptInterface
        fun stopBackgroundMic() {
            runOnUiThread {
                stopService(Intent(this@MainActivity, MicForegroundService::class.java))
            }
        }

        @JavascriptInterface
        fun isAccessibilityEnabled(): Boolean {
            return VoiceAccessibilityService.instance != null
        }

        @JavascriptInterface
        fun openAccessibilitySettings() {
            runOnUiThread {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        @JavascriptInterface
        fun goHome() {
            VoiceAccessibilityService.instance?.goHome()
        }

        @JavascriptInterface
        fun goBack() {
            VoiceAccessibilityService.instance?.goBack()
        }

        @JavascriptInterface
        fun openRecents() {
            VoiceAccessibilityService.instance?.openRecents()
        }

        @JavascriptInterface
        fun openApp(packageName: String) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    runOnUiThread { startActivity(launchIntent) }
                }
            } catch (e: Exception) {
                // app not installed, ignore silently
            }
        }

        @JavascriptInterface
        fun clickOnScreenText(text: String): Boolean {
            return VoiceAccessibilityService.instance?.findAndClickText(text) ?: false
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        super.onDestroy()
    }
}
