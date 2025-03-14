package io.xconn.tictackotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class SplashSCreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Check if device is Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installSplashScreen()
            startMainActivity()
        } else {
            // Use the traditional method for older versions
            Handler().postDelayed({ this.startMainActivity() }, 2000)
        }
    }

    private fun startMainActivity() {
        val intent = Intent(
            this,
            SelectGameTypeActivity::class.java
        )
        startActivity(intent)
        finish()
    }
}
