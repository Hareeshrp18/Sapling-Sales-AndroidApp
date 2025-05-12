package com.example.saplingsales

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class InfoActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val infoText = findViewById<TextView>(R.id.infoText)
        infoText.text = "Welcome to Sapling Sales! Here, you can browse and purchase saplings with ease."
    }
}
