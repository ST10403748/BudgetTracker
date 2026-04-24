package com.budgettracker.ui
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.databinding.ActivityLoginBinding
import com.budgettracker.utils.HashUtils
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        if (session.isLoggedIn()) { goToMain(); return }
        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnRegister.setOnClickListener { startActivity(Intent(this, RegisterActivity::class.java)) }
    }

    private fun attemptLogin() {
        val username = binding.etUsername.text.toString().trim()
        val password = binding.etPassword.text.toString()
        if (username.isEmpty()) { binding.etUsername.error = "Username is required"; return }
        if (password.isEmpty()) { binding.etPassword.error = "Password is required"; return }
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val user = db.userDao().login(username, HashUtils.sha256(password))
            if (user != null) {
                Log.i(TAG, "Login successful: ${user.username}")
                session.saveSession(user.id, user.username)
                goToMain()
            } else {
                Log.w(TAG, "Login failed for: $username")
                runOnUiThread { Toast.makeText(this@LoginActivity, "Invalid username or password", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun goToMain() { startActivity(Intent(this, MainActivity::class.java)); finish() }
}
