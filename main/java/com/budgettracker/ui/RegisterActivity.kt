package com.budgettracker.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.data.entities.User
import com.budgettracker.databinding.ActivityRegisterBinding
import com.budgettracker.utils.HashUtils
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding  // This was missing!
    private val TAG = "RegisterActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize binding - THIS IS CRITICAL
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Create Account"

        binding.btnRegister.setOnClickListener { attemptRegister() }
    }

    private fun attemptRegister() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val confirm = binding.etConfirmPassword.text.toString()

        // Fixed validation logic
        if (username.length < 3) {
            binding.etUsername.error = "Username must be at least 3 characters"
            return
        }
        if (password.length < 6) {
            binding.etPassword.error = "Password must be at least 6 characters"
            return
        }
        if (password != confirm) {  // Fixed: was comparing confirm != confirm
            binding.etConfirmPassword.error = "Passwords do not match"
            return
        }

        // Proceed with registration
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            try {
                val existing = db.userDao().findByUsername(username)
                if (existing != null) {
                    runOnUiThread {
                        binding.etUsername.error = "Username already taken"
                    }
                    return@launch
                }

                val newUser = User(
                    username = username,
                    passwordHash = HashUtils.sha256(password)
                )
                db.userDao().insert(newUser)

                Log.i(TAG, "New user registered: $username")
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Account created! Please log in.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Registration error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Registration failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}