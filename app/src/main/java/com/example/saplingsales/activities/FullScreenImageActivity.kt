package com.example.saplingsales.activities

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.saplingsales.R
import com.example.saplingsales.adapters.FullScreenImageAdapter
import com.example.saplingsales.ImageDataHolder

class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val viewPager = findViewById<ViewPager2>(R.id.viewPagerFullScreen)
        val btnClose = findViewById<ImageButton>(R.id.btnClose)

        val images = ImageDataHolder.images
        val initialPosition = intent.getIntExtra("position", 0)

        val adapter = FullScreenImageAdapter(images)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(initialPosition, false)

        btnClose.setOnClickListener { finish() }
    }
} 