package io.xconn.tictackotlin

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
                    println("-----------------------------------------------------------")


                    try {

                        println("----------------------after connect")
                        val result = session.call(
                            "io.xconn.tictac.account.create",
                            args = listOf(userName, email)
                        ).await()

                        println(result)
                        Log.e("json result", "${result.args}")
                        runOnUiThread {
                            // Stuff that updates the UI
                            binding.userDetails.text = "${result.args}"

                            if (result.args!!.isNotEmpty()) {
                                binding.emailTIET.setText("")
                                binding.userNameTIET.setText("").toString()
                                app.saveLoginOrBoolean("is_logged_in", true)
                                Toast.makeText(this@RegisterActivity, "User registered successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@RegisterActivity, "User not registered successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@RegisterActivity, "$e", Toast.LENGTH_SHORT).show()

                        }
                    }



                }
            } catch (e: Exception) {
                Log.e("Coroutine", "Connection failed", e)

            }

        }
    }


    fun isValidEmail(target: CharSequence?): Boolean {
        return if (TextUtils.isEmpty(target)) {
            false
        } else {
            Patterns.EMAIL_ADDRESS.matcher(target).matches()
        }
    }
}