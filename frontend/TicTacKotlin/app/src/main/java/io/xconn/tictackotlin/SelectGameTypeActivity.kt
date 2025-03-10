package io.xconn.tictackotlin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.xconn.tictackotlin.databinding.ActivityRegisterBinding
import io.xconn.tictackotlin.databinding.ActivitySelectGameTypeBinding

class SelectGameTypeActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySelectGameTypeBinding
    private val app = App()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySelectGameTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.p2pOnlineCard.setOnClickListener {
            if (!app.getValueBoolean("is_logged_in")) {
                startActivity(Intent(this, RegisterActivity::class.java))
            } else {
                //display list of users
                startActivity(Intent(this, DashboardActivity::class.java).apply {
                    putExtra("is_online", true)
                })

            }
        }

        binding.p2pOflineCard.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java).apply {
                putExtra("is_online", false)
            })
        }
    }
}