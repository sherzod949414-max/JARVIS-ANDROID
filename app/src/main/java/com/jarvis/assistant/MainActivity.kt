
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

    private lateinit var tts: TextToSpeech
    private lateinit var output: TextView
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(24, 40, 24, 24)
        layout.setBackgroundColor(Color.rgb(15, 18, 25))

        val title = TextView(this)
        title.text = "J.A.R.V.I.S"
        title.textSize = 30f
        title.setTextColor(Color.WHITE)

        output = TextView(this)
        output.text = "Salom! Men JARVIS. Buyrug'ingizni kutyapman."
        output.textSize = 18f
        output.setTextColor(Color.WHITE)

        input = EditText(this)
        input.hint = "Buyruq yoki savol yozing"
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.LTGRAY)

        val button = Button(this)
        button.text = "YUBORISH"

        button.setOnClickListener {
            val question = input.text.toString().trim()
            if (question.isNotEmpty()) {
                output.text = "Siz: $question\n\nJARVIS: Buyrug'ingiz qabul qilindi."
                speak("Buyrug'ingiz qabul qilindi.")
                input.text.clear()
            }
        }

        layout.addView(title)
        layout.addView(output)
        layout.addView(input)
        layout.addView(button)

        setContentView(layout)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale("ru")
        }
    }

    private fun speak(message: String) {
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
