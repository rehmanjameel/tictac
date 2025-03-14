package io.xconn.tictackotlin

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import io.xconn.tictackotlin.App.Companion.session
import io.xconn.tictackotlin.databinding.ActivityRegisterBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private val app = App()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerUser.setOnClickListener {
            lifecycleScope.launch {
                isValidData()
            }
        }
    }

    private suspend fun isValidData() {
        val email = binding.emailTIET.text.toString()
        val userName = binding.userNameTIET.text.toString()

        if (email.isEmpty() || userName.isEmpty()) {
            binding.emailTIET.error = "Field required"
            binding.userNameTIET.error = "Field required"
        } else if (!isValidEmail(email)) {
            binding.emailTIET.error = "Field required or invalid email"

        } else if (userName.isEmpty()) {
            binding.userNameTIET.error = "Field required"

        } else {
            registerUser(email, userName)
        }
    }

    private suspend fun registerUser(email: String, userName: String) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    try {
                        val result = session.call(
                            "io.xconn.tictac.account.create",
                            args = listOf(userName, email)
                        ).await()
                        val args = result.args // This is expected to be a List
                        if (args != null) {
                            if (args.isNotEmpty() && args[0] is Map<*, *>) {
                                val userData = args[0] as Map<*, *>
                                val createdAt = userData["created_at"] as? String ?: "N/A"
                                val userEmail = userData["email"] as? String ?: "N/A"
                                val id = userData["id"] as? Int ?: -1
                                val name = userData["name"] as? String ?: "N/A"
                                runOnUiThread {
                                    // Stuff that updates the UI
                                    binding.userDetails.text = "${result.details}"
                                    if (result.args!!.isNotEmpty()) {
                                        binding.emailTIET.setText("")
                                        binding.userNameTIET.setText("").toString()
                                        app.saveLoginOrBoolean("is_logged_in", true)
                                        app.saveInt("user_id", id)
                                        app.saveString("user_name", name)
                                        app.saveString("email", userEmail)

                                        startActivity(
                                            Intent(
                                                this@RegisterActivity,
                                                DashboardActivity::class.java
                                            ).apply {
                                                putExtra("is_online", true)
                                            })
                                        finish()
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "User registered successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            this@RegisterActivity,
                                            "User not registered successfully!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Log.e("Error", "Unexpected data format in result.args")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(
                                this@RegisterActivity,
                                "Server Connection Problem..",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Coroutine", "Connection failed", e)
            }
        }
    }

    private fun isValidEmail(target: CharSequence?): Boolean {
        return if (TextUtils.isEmpty(target)) {
            false
        } else {
            Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }
}
