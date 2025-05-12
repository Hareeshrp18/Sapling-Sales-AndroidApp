package com.example.saplingsales

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class RoleSelectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.role_selection_activity)


        val videoView = findViewById<VideoView>(R.id.videoView)

        // Path to the video in the raw folder
        val videoUri: Uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.splash_video)

        // Set the video source
        videoView.setVideoURI(videoUri)

        // Start playing the video when ready
        videoView.setOnPreparedListener {
            it.isLooping = true // Optionally, make the video loop
            videoView.start()
        }
        // Admin Button
        val adminButton = findViewById<Button>(R.id.btnAdmin)
        adminButton.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
        }

        // User Button
        val userButton = findViewById<Button>(R.id.btnUser)
        userButton.setOnClickListener {
            val intent = Intent(this, UserLoginActivity::class.java)
            startActivity(intent)
        }
    }
}
