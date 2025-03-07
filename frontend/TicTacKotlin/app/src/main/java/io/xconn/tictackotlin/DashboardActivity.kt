package io.xconn.tictackotlin

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.xconn.tictackotlin.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private var levelText = ""
    private var personText = ""

    private val app = App()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.easyText.setShadowLayer(10F, 5F, 5f, ContextCompat.getColor(this, R.color.white))
        binding.mediumText.setShadowLayer(10F, 5F, 5F, ContextCompat.getColor(this, R.color.oColor))
        binding.hardText.setShadowLayer(10F, 5F, 5F, ContextCompat.getColor(this, R.color.winnerColor))
        binding.titleTextView.setShadowLayer(10F, 5F, 5F, ContextCompat.getColor(this, R.color.oColor))

        binding.easyLevelCard.setOnClickListener { view ->
            handleCardClick(binding.easyLayout)
            levelText = "easy_level"
        }

        binding.mediumLevelCard.setOnClickListener { view ->
            handleCardClick(binding.mediumLayout)
            levelText = "medium_level"
        }

        binding.hardLevelCard.setOnClickListener { view ->
            handleCardClick(binding.hardLayout)
            levelText = "hard_level"
        }

        binding.p2rCard.setOnClickListener { view ->
            handlePersonClick(binding.p2rLayout)
            personText = "p2r"
        }

        binding.p2pCard.setOnClickListener { view ->
            handlePersonClick(binding.p2pLayout)
            personText = "p2p"
        }

        binding.playGame.setOnClickListener { view ->
            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
                    .putExtra("level_type", levelText)
                    .putExtra("person_type", personText)
            )
        }

        binding.backButton.setOnClickListener { view ->
            finish()
        }

        binding.p2pOnlineCard.setOnClickListener {
            if (!app.getValueBoolean("is_logged_in")) {
                startActivity(Intent(this, RegisterActivity::class.java))
            } else {
                //display list of users
            }
        }
    }

    private fun handleCardClick(selectedCard: LinearLayout) {
        // Reset all cards and texts
        resetCards()

        // Highlight the selected card and show the corresponding text
        selectedCard.setBackgroundResource(R.drawable.level_selction_bg) // Change to your desired color
//        selectedText.setVisibility(View.VISIBLE);
    }

    private fun handlePersonClick(selectedCard: LinearLayout) {
        // Reset all cards and texts
        resetPersonCards()

        // Highlight the selected card and show the corresponding text
        selectedCard.setBackgroundResource(R.drawable.level_selction_bg) // Change to your desired color
//        selectedText.setVisibility(View.VISIBLE);
    }

    private fun resetCards() {
        // Reset all cards to white
        binding.easyLayout.setBackgroundResource(R.drawable.level_card_bg)

        binding.mediumLayout.setBackgroundResource(R.drawable.level_card_bg)
        binding.hardLayout.setBackgroundResource(R.drawable.level_card_bg)
    }

    private fun resetPersonCards() {
        // Reset all cards to white
        binding.p2rLayout.setBackgroundResource(R.drawable.level_card_bg)

        binding.p2pLayout.setBackgroundResource(R.drawable.level_card_bg)
    }

    override fun onResume() {
        super.onResume()
    }
}