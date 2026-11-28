package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val worker = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("jarvis_private", MODE_PRIVATE) }
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private lateinit var transcript: TextView
    private lateinit var input: EditText
    private lateinit var sendButton: Button
    private lateinit var listenButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        tts = TextToSpeech(this, this)
        if (prefs.getString("gemini_key", null).isNullOrBlank()) {
            showKeyDialog()
        } else {
            addLine("JARVIS", "Tizim tayyor. Savolingizni yozing yoki mikrofonni bosing.")
        }
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 30, 22, 18)
            setBackgroundColor(0xFF101722.toInt())
        }
        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(0xFF65D9FF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "PERSONAL AI ASSISTANT"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(0xFF9FB2C8.toInt())
        }
        transcript = TextView(this).apply {
            textSize = 16f
            setTextColor(0xFFF2F6FC.toInt())
            setPadding(8, 14, 8, 14)
        }
        val scroll = ScrollView(this).apply {
            addView(transcript)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }
        input = EditText(this).apply {
            hint = "JARVISga yozing..."
            setSingleLine(false)
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0xFF9FB2C8.toInt())
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        listenButton = Button(this).apply {
            text = "🎙"
            setOnClickListener { startListening() }
        }
        sendButton = Button(this).apply {
            text = "YUBORISH"
            setOnClickListener { submitQuestion() }
        }
        val settings = Button(this).apply {
            text = "⚙"
            setOnClickListener { showKeyDialog() }
        }
        row.addView(listenButton, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(sendButton, LinearLayout.LayoutParams(0, -2, 2f))
        row.addView(settings, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(title)
        root.addView(subtitle)
        root.addView(scroll)
        root.addView(input)
        root.addView(row)
        setContentView(root)
    }

    private fun addLine(who: String, message: String) {
        transcript.append("\n$who:\n$message\n")
    }

    private fun showKeyDialog() {
        val field = EditText(this).apply {
            hint = "Gemini API kalitini shu yerga kiriting"
            inputType =  textInputTypePassword()
            setText(prefs.getString("gemini_key", ""))
        }
        AlertDialog.Builder(this)
            .setTitle("Gemini ulanishi")
            .setMessage("Kalit faqat telefoningizdagi ilova xotirasida saqlanadi. Uni GitHubga joylamang va hech kimga yubormang.")
            .setView(field)
            .setPositiveButton("SAQLASH") { _, _ ->
                val key = field.text.toString().trim()
                if (key.isNotEmpty()) {
                    prefs.edit().putString("gemini_key", key).apply()
                    addLine("JARVIS", "Gemini kaliti saqlandi. Endi savol berishingiz mumkin.")
                }
            }
            .setNegativeButton("BEKOR", null)
            .show()
    }

    private fun textInputType(): Int = android.text.InputType.TYPE_CLASS_TEXT or
        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

    private fun submitQuestion() {
        val question = input.text.toString().trim()
        if (question.isEmpty()) return
        input.setText("")
        addLine("SIZ", question)
        if (runPhoneCommand(question)) return
        val key = prefs.getString("gemini_key", null)
        if (key.isNullOrBlank()) {
            addLine("JARVIS", "Avval ⚙ tugmasidan Gemini API kalitini kiriting.")
            showKeyDialog()
            return
        }
        sendButton.isEnabled = false
        addLine("JARVIS", "O‘ylayapman...")
        worker.execute {
            val answer = askGemini(key, question)
            runOnUiThread {
                sendButton.isEnabled = true
                transcript.append("\n$answer\n")
                speak(answer)
            }
        }
    }

    private fun askGemini(key: String, question: String): String {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20000
                readTimeout = 30000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", key)
            }
            val history = JSONArray()
            val saved = JSONArray(prefs.getString("chat_history", "[]"))
            for (i in 0 until saved.length()) history.put(saved.getJSONObject(i))
            history.put(JSONObject().put("role", "user").put("parts", JSONArray().put(
                JSONObject().put("text", question)
            )))
            val body = JSONObject()
                .put("system_instruction", JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", "Sen JARVIS nomli shaxsiy sun'iy intellekt yordamchisisan. Foydalanuvchiga hurmat bilan, avvalo o'zbek tilida, u ruscha gapirsa rus tilida javob ber. Aniq, foydali va rostgo'y bo'l. Telefonni boshqardim deb yolg'on aytma. Imkoning bo'lmagan ishni ochiq ayt.")
                )))
                .put("contents", history)
                .put("generationConfig", JSONObject().put("temperature", 0.7).put("maxOutputTokens", 1200))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val detail = try { JSONObject(response).optJSONObject("error")?.optString("message") } catch (_: Exception) { null }
                "Gemini xatosi ($code): ${detail ?: "Ulanishda xatolik"}"
            } else {
                val json = JSONObject(response)
                val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
                val parts = candidate?.optJSONObject("content")?.optJSONArray("parts")
                val answer = parts?.optJSONObject(0)?.optString("text")?.trim()
                if (answer.isNullOrBlank()) "Gemini javob qaytarmadi. Qayta urinib ko‘ring." else {
                    val updated = JSONArray()
                    for (i in 0 until saved.length()) updated.put(saved.getJSONObject(i))
                    updated.put(JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", question))))
                    updated.put(JSONObject().put("role", "model").put("parts", JSONArray().put(JSONObject().put("text", answer))))
                    while (updated.length() > 20) updated.remove(0)
                    prefs.edit().putString("chat_history", updated.toString()).apply()
                    answer
                }
            }
        } catch (e: Exception) {
            "Ulanish amalga oshmadi: ${e.message ?: "internet yoki API kalitini tekshiring"}"
        } finally {
            connection?.disconnect()
        }
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "uz-UZ")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVISga gapiring")
            }
            startActivityForResult(intent, 42)
        } catch (_: Exception) {
            Toast.makeText(this, "Ovozli tanish xizmati mavjud emas", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == 41 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) startListening()
    }

    @Deprecated("Legacy speech result callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK) {
            val spoken = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                input.setText(spoken)
                submitQuestion()
            }
        }
    }

    private fun runPhoneCommand(text: String): Boolean {
        val q = text.lowercase(Locale.ROOT)
        val pkg = when {
            "telegram" in q -> "org.telegram.messenger"
            "youtube" in q -> "com.google.android.youtube"
            "chrome" in q || "brauzer" in q -> "com.android.chrome"
            "whatsapp" in q -> "com.whatsapp"
            "sozlam" in q || "настройк" in q -> null
            else -> return false
        }
        return try {
            val intent = if (pkg == null) Intent(android.provider.Settings.ACTION_SETTINGS)
                else packageManager.getLaunchIntentForPackage(pkg)
            if (intent == null) {
                addLine("JARVIS", "Bu ilova telefonda topilmadi.")
            } else {
                startActivity(intent)
                addLine("JARVIS", "Bajarildi.")
            }
            true
        } catch (_: Exception) {
            addLine("JARVIS", "Ilovani ochib bo‘lmadi.")
            true
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val uz = tts?.setLanguage(Locale("uz"))
            ttsReady = uz != TextToSpeech.LANG_MISSING_DATA && uz != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                val ru = tts?.setLanguage(Locale("ru"))
                ttsReady = ru != TextToSpeech.LANG_MISSING_DATA && ru != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    private fun speak(message: String) {
        if (ttsReady) tts?.speak(message.take(3500), TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        worker.shutdownNow()
        super.onDestroy()
    }
}
