
package com.jarvis.assistant

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private lateinit var output: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 40, 24, 24)
            setBackgroundColor(Color.rgb(15, 18, 25))
        }

        val title = TextView(this).apply {
            text = "J.A.R.V.I.S"
            textSize = 30f
            setTextColor(Color.WHITE)
        }

        output = TextView(this).apply {
            text = "Salom! Men JARVIS. Buyrug'ingizni kutyapman."
            textSize = 18f
            setTextColor(Color.WHITE)
        }

        input = EditText(this).apply {
            hint = "Buyruq yoki savol yozing"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
        }

        val button = Button(this).apply {
            text = "YUBORISH"
            setOnClickListener {
                val question = input.text.toString().trim()

                if (question.isNotEmpty()) {
                    output.text =
                        "Siz: $question\n\nJARVIS: Buyrug'ingiz qabul qilindi."
                    speak("Buyrug'ingiz qabul qilindi.")
                    input.text.clear()
                }
            }
        }

        layout.addView(title)
        layout.addView(output)
        layout.addView(input)
        layout.addView(button)

        setContentView(layout)

        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ru"))
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speak(message: String) {
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "jarvis"
            )
        }
    }

    override fun onDestroy() {
        ttsReady = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}
