package com.budgettracker.ui
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.databinding.ActivityMainBinding
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var session: SessionManager
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        session = SessionManager(this)
        binding.tvWelcome.text = "Welcome, ${session.getUsername()}!"
        Log.d(TAG, "Dashboard loaded for userId=${session.getUserId()}")
        binding.btnCategories.setOnClickListener { startActivity(Intent(this, CategoryActivity::class.java)) }
        binding.btnAddExpense.setOnClickListener { startActivity(Intent(this, AddExpenseActivity::class.java)) }
        binding.btnViewExpenses.setOnClickListener { startActivity(Intent(this, ExpenseListActivity::class.java)) }
        binding.btnCategoryReport.setOnClickListener { startActivity(Intent(this, CategoryReportActivity::class.java)) }
        binding.btnGoals.setOnClickListener { startActivity(Intent(this, GoalActivity::class.java)) }
        binding.btnLogout.setOnClickListener { session.clearSession(); startActivity(Intent(this, LoginActivity::class.java)); finish() }
    }

    override fun onResume() { super.onResume(); loadMonthSummary() }

    private fun loadMonthSummary() {
        val db = AppDatabase.getInstance(this)
        val userId = session.getUserId()
        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        lifecycleScope.launch {
            val total = db.expenseDao().getTotalForMonth(userId, month) ?: 0.0
            val goal = db.goalDao().getGoal(userId, month)
            runOnUiThread {
                binding.tvMonthTotal.text = "This month: R${"%.2f".format(total)}"
                binding.tvGoalStatus.text = if (goal == null) "No goal set for this month"
                    else when {
                        total < goal.minGoal -> "Below minimum goal (R${"%.2f".format(goal.minGoal)})"
                        total > goal.maxGoal -> "Exceeded max goal (R${"%.2f".format(goal.maxGoal)})"
                        else -> "Within goal range"
                    }
            }
        }
    }
}
