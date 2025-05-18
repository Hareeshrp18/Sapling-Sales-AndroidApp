package com.example.saplingsales

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.content.pm.PackageManager

class InfoActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        // Get version name from package info
        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }

        // Update version text
        findViewById<TextView>(R.id.versionText)?.text = "Version $versionName"

        // Update welcome text with dynamic content
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText?.text = buildString {
            append("Welcome to Sapling Sales!\n\n")
            append("Your one-stop destination for quality saplings and plants. ")
            append("Browse our extensive collection, place orders, and contribute to a greener environment.\n\n")
            append("We are committed to providing you with the best quality plants and excellent service.")
        }
    }
}
