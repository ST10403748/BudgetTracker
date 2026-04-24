package com.budgettracker.ui

import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.data.entities.MonthlyGoal
import com.budgettracker.databinding.ActivityGoalBinding
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GoalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGoalBinding
    private lateinit var session: SessionManager
    private val TAG = "GoalActivity"
    private val MAX_SEEKBAR = 50000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Monthly Goals"
        session = SessionManager(this)
        val month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        binding.tvMonth.text = "Month: $month"
        binding.seekBarMax.max = MAX_SEEKBAR
        binding.seekBarMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.tvMaxGoalValue.text = "Max Goal: R$progress"
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        loadExistingGoal(month)
        binding.btnSaveGoal.setOnClickListener { saveGoal(month) }
    }

    private fun loadExistingGoal(month: String) {
        lifecycleScope.launch {
            val goal = AppDatabase.getInstance(this@GoalActivity).goalDao().getGoal(session.getUserId(), month)
            goal?.let {
                Log.d(TAG, "Existing goal: min=${it.minGoal}, max=${it.maxGoal}")
                runOnUiThread {
                    binding.etMinGoal.setText(it.minGoal.toInt().toString())
                    binding.seekBarMax.progress = it.maxGoal.toInt()
                    binding.tvMaxGoalValue.text = "Max Goal: R${it.maxGoal.toInt()}"
                }
            }
        }
    }

    private fun saveGoal(month: String) {
        val minStr = binding.etMinGoal.text.toString().trim()
        val maxVal = binding.seekBarMax.progress.toDouble()
        if (minStr.isEmpty()) { binding.etMinGoal.error = "Enter minimum goal"; return }
        val minVal = minStr.toDoubleOrNull()
        if (minVal == null || minVal < 0) { binding.etMinGoal.error = "Enter a valid amount"; return }
        if (maxVal <= 0) { Toast.makeText(this, "Set a max goal using the slider", Toast.LENGTH_SHORT).show(); return }
        if (minVal > maxVal) { binding.etMinGoal.error = "Min must be less than max"; return }
        val userId = session.getUserId()
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@GoalActivity)
            val existing = db.goalDao().getGoal(userId, month)
            if (existing != null) db.goalDao().update(existing.copy(minGoal = minVal, maxGoal = maxVal))
            else db.goalDao().insert(MonthlyGoal(userId = userId, month = month, minGoal = minVal, maxGoal = maxVal))
            Log.i(TAG, "Goal saved for $month: min=$minVal, max=$maxVal")
            runOnUiThread { Toast.makeText(this@GoalActivity, "Goal saved!", Toast.LENGTH_SHORT).show(); finish() }
        }
    }
}
