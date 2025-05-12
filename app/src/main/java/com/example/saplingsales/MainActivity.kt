package com.example.saplingsales

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.saplingsales.activities.UserScreenActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Simulate a splash screen delay before navigating to UserScreenActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, UserScreenActivity::class.java)
            startActivity(intent)
            finish() // Close the splash screen so the user can't go back to it
        }, 2000) // 2-second delay
    }
}
