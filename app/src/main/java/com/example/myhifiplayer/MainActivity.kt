package com.example.myhifiplayer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "My HiFi Player\n\nСборка работает!"
            textSize = 22f
        }
        setContentView(textView)
    }
}
