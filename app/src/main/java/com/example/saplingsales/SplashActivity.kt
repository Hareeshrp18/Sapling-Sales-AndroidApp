package com.example.saplingsales

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.example.saplingsales.activities.UserScreenActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoUri: Uri = Uri.parse("android.resource://$packageName/${R.raw.splash_video}")
        videoView.setVideoURI(videoUri)

        videoView.setOnPreparedListener {
            it.isLooping = true
            videoView.start()
        }

        // Play splash audio once
        // Delay audio playback by 1 second (1000 milliseconds)
        Handler(Looper.getMainLooper()).postDelayed({
            mediaPlayer = MediaPlayer.create(this, R.raw.splash_audio)
            mediaPlayer?.start()
        }, 1000)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserRole()
        }, 5000) // 5-second delay for splash screen
    }

    private fun checkUserRole() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            val userId = currentUser.uid
            Log.d("SplashActivity", "User ID: $userId")

            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { userDoc ->
                    if (userDoc.exists()) {
                        val role = userDoc.getString("role") ?: ""
                        Log.d("SplashActivity", "User Role: $role")
                        when (role) {
                            "user" -> startActivity(Intent(this, UserScreenActivity::class.java))
                            else -> startActivity(Intent(this, RoleSelectionActivity::class.java))
                        }
                        finish()
                    } else {
                        db.collection("saplingAdmin").document(userId)
                            .get()
                            .addOnSuccessListener { adminDoc ->
                                if (adminDoc.exists()) {
                                    Log.d("SplashActivity", "Admin found")
                                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                                } else {
                                    Log.e("SplashActivity", "No user or admin found")
                                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                                }
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.e("SplashActivity", "Firestore Admin Error: ${e.message}", e)
                                Toast.makeText(this, "Error retrieving admin data", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, RoleSelectionActivity::class.java))
                                finish()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("SplashActivity", "Firestore User Error: ${e.message}", e)
                    Toast.makeText(this, "Error retrieving user data", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, RoleSelectionActivity::class.java))
                    finish()
                }
        } else {
            Log.e("SplashActivity", "No logged-in user")
            startActivity(Intent(this, RoleSelectionActivity::class.java))
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
