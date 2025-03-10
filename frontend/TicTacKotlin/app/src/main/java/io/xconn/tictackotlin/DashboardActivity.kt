package io.xconn.tictackotlin

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Visibility
import io.xconn.tictackotlin.App.Companion.isSessionInitialized
import io.xconn.tictackotlin.App.Companion.session
import io.xconn.tictackotlin.adapter.OnlineUsersAdapter
import io.xconn.tictackotlin.databinding.ActivityDashboardBinding
import io.xconn.tictackotlin.model.OnlineUsersModel
import io.xconn.wampproto.messages.Register
import io.xconn.xconn.Registration
import io.xconn.xconn.Result
import io.xconn.xconn.Session
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashboardBinding
    private var levelText = ""
    private var personText = ""
    private var user_id : Int = 0

    private lateinit var reg : Registration
    private val app = App()
    private lateinit var adapter: OnlineUsersAdapter
    private val onlineUsersList = ArrayList<OnlineUsersModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val isOnline = intent.getBooleanExtra("is_online", false)
        Log.e("is_playing", isOnline.toString())
        if (isOnline) {
            binding.userSelectionLayout.visibility = View.GONE
            user_id = app.getValueInt("user_id")
            Log.e("user iddd0", user_id.toString())

            lifecycleScope.launch {

                getOnlineUsers()

                if (!app.getValueBoolean("is_procedure_registered")) {
                    Log.e("error of same", "proceduree here..")
                    pairUser()
                }
            }
        }


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

        adapter = OnlineUsersAdapter(this@DashboardActivity, onlineUsersList)
        binding.usersListRV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.usersListRV.adapter = adapter
        lifecycleScope.launch {
            val users = getOnlineUsers() // Fetch online users
            adapter.updateList(users) // Update adapter
        }

    }

    private suspend fun getOnlineUsers(): ArrayList<OnlineUsersModel> {
        val usersList = ArrayList<OnlineUsersModel>()

        try {
            val args = session.call("io.xconn.tictac.users.online").await().args

            if (args != null) {
                if (args.isNotEmpty() && args[0] is List<*>) {
                    val userList = args[0] as List<Map<String, Any>>

                    for (userData in userList) {
                        val id = userData["id"] as? Int ?: -1
                        val name = userData["name"] as? String ?: "N/A"
                        val email = userData["email"] as? String ?: "N/A"
                        val createdAt = userData["created_at"] as? String ?: "N/A"

                        Log.e("user iddd", "$user_id,.,.$id")

                        if (id != user_id) {
                            usersList.add(OnlineUsersModel(id, name, email, createdAt))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("getOnlineUsers", "Error fetching users: ${e.message}")
        }

        return usersList
    }

    private suspend fun pairUser() {
        reg = session.register("io.xconn.tictac.$user_id.pair", {invocation ->

            Log.e("result args", invocation.args.toString())
            startActivity(Intent(this@DashboardActivity, MainActivity::class.java).apply {
                putExtra("user_id", "user.id")
            })
            Result(args = invocation.args, kwargs = invocation.kwargs)
        }).await()
        runOnUiThread {
            app.saveLoginOrBoolean("is_procedure_registered", true)

            Log.e("user pair..", reg.toString())
        }

    }

    private suspend fun unPairRegisteredUser() {
        if (::reg.isInitialized) {
            val result = reg?.let { session.unregister(it) }
            app.saveLoginOrBoolean("is_procedure_registered", false)
            runOnUiThread {
                Log.e("user pair..", result.toString())
            }
        }

    }

    private suspend fun setUserOnline() {
        if (isSessionInitialized) {
            Log.e("user iddd", user_id.toString())
            val result = session.publish("io.xconn.tictac.user.online.set", args = listOf(user_id))
            runOnUiThread{
                Log.e("results online", result.toString())
            }
        }
    }

    private suspend fun setUserOffline() {
        if (isSessionInitialized) {
            Log.e("user iddd off", user_id.toString())
            val result = session.publish("io.xconn.tictac.user.offline.set", args = listOf(user_id))

            runOnUiThread{
                try {
                    Log.e("results ofline", result.toString())

                } catch (e: Exception) {

                    Log.e("results ofline ee", result.toString() + e.message)
                }
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


        lifecycleScope.launch {
            setUserOnline()

        }
    }

    override fun onPause() {
        super.onPause()
        lifecycleScope.launch {
            setUserOffline()
//            unPairRegisteredUser()

        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            setUserOffline()
//            unPairRegisteredUser()

        }
    }
    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            setUserOffline()
//            unPairRegisteredUser()
        }
    }
}