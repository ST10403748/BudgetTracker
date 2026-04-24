package com.budgettracker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.databinding.ActivityCategoryReportBinding
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class CategoryReportActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryReportBinding
    private lateinit var session: SessionManager
    private var fromDate = LocalDate.now().withDayOfMonth(1).toString()
    private var toDate = LocalDate.now().toString()
    private val TAG = "CategoryReportActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryReportBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Category Report"
        session = SessionManager(this)
        updateLabels()
        loadReport()
        binding.btnFromDate.setOnClickListener { pickDate(true) }
        binding.btnToDate.setOnClickListener { pickDate(false) }
    }

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val date = "%04d-%02d-%02d".format(y, m + 1, d)
            if (isFrom) fromDate = date else toDate = date
            updateLabels()
            loadReport()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateLabels() {
        binding.btnFromDate.text = "From: $fromDate"
        binding.btnToDate.text = "To: $toDate"
    }

    private fun loadReport() {
        val db = AppDatabase.getInstance(this)
        val userId = session.getUserId()
        lifecycleScope.launch {
            val totals = db.expenseDao().getTotalByCategory(userId, fromDate, toDate)
            val cats = db.categoryDao().getByUserOnce(userId).associateBy { it.id }
            Log.d(TAG, "Report: ${totals.size} categories")
            runOnUiThread {
                binding.llReport.removeAllViews()
                if (totals.isEmpty()) {
                    binding.llReport.addView(TextView(this@CategoryReportActivity).apply {
                        text = "No expenses in this period."
                        setPadding(8, 16, 8, 16)
                    })
                    binding.tvGrandTotal.text = "Grand Total: R0.00"
                    return@runOnUiThread
                }
                var grandTotal = 0.0
                totals.sortedByDescending { it.total }.forEach { ct ->
                    grandTotal += ct.total
                    val name = cats[ct.categoryId]?.name ?: "Uncategorised"
                    binding.llReport.addView(TextView(this@CategoryReportActivity).apply {
                        text = "$name:  R${"%.2f".format(ct.total)}"
                        textSize = 16f
                        setPadding(8, 16, 8, 16)
                    })
                }
                binding.tvGrandTotal.text = "Grand Total: R${"%.2f".format(grandTotal)}"
            }
        }
    }
}
