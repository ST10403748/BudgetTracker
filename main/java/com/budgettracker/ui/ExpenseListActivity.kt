package com.budgettracker.ui

import android.app.DatePickerDialog
import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.budgettracker.R
import com.budgettracker.data.AppDatabase
import com.budgettracker.data.entities.Category
import com.budgettracker.data.entities.Expense
import com.budgettracker.databinding.ActivityExpenseListBinding
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.util.Calendar

class ExpenseListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityExpenseListBinding
    private lateinit var session: SessionManager
    private lateinit var adapter: ExpenseAdapter
    private var fromDate = LocalDate.now().withDayOfMonth(1).toString()
    private var toDate = LocalDate.now().toString()
    private val TAG = "ExpenseListActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExpenseListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Expense List"
        session = SessionManager(this)
        adapter = ExpenseAdapter(emptyList(), emptyMap()) { path -> showPhotoDialog(path) }
        binding.rvExpenses.layoutManager = LinearLayoutManager(this)
        binding.rvExpenses.adapter = adapter
        updateDateLabels()
        loadExpenses()
        binding.btnFromDate.setOnClickListener { pickDate(true) }
        binding.btnToDate.setOnClickListener { pickDate(false) }
    }

    private fun pickDate(isFrom: Boolean) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val date = "%04d-%02d-%02d".format(y, m + 1, d)
            if (isFrom) fromDate = date else toDate = date
            updateDateLabels()
            loadExpenses()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateLabels() {
        binding.btnFromDate.text = "From: $fromDate"
        binding.btnToDate.text = "To: $toDate"
    }

    private fun loadExpenses() {
        val db = AppDatabase.getInstance(this)
        val userId = session.getUserId()
        lifecycleScope.launch {
            val cats = db.categoryDao().getByUserOnce(userId).associateBy { it.id }
            db.expenseDao().getByPeriod(userId, fromDate, toDate).collectLatest { list ->
                Log.d(TAG, "Loaded ${list.size} expenses from $fromDate to $toDate")
                runOnUiThread {
                    adapter.update(list, cats)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    binding.tvTotal.text = "Total: R${"%.2f".format(list.sumOf { it.amount })}"
                }
            }
        }
    }

    private fun showPhotoDialog(path: String) {
        val dialog = Dialog(this)
        val iv = ImageView(this).apply { adjustViewBounds = true }
        dialog.setContentView(iv)
        Glide.with(this)
            .load(if (path.startsWith("content://")) Uri.parse(path) else File(path))
            .into(iv)
        iv.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    inner class ExpenseAdapter(
        private var items: List<Expense>,
        private var categoryMap: Map<Int, Category>,
        private val onPhotoClick: (String) -> Unit
    ) : RecyclerView.Adapter<ExpenseAdapter.VH>() {

        fun update(newItems: List<Expense>, newMap: Map<Int, Category>) {
            items = newItems; categoryMap = newMap; notifyDataSetChanged()
        }

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvDesc: TextView = view.findViewById(R.id.tvDesc)
            val tvAmount: TextView = view.findViewById(R.id.tvAmount)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val ivPhoto: ImageView = view.findViewById(R.id.ivPhoto)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_expense, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val e = items[position]
            holder.tvDesc.text = e.description
            holder.tvAmount.text = "R${"%.2f".format(e.amount)}"
            holder.tvDate.text = "${e.date}  ${e.startTime}-${e.endTime}"
            holder.tvCategory.text = categoryMap[e.categoryId]?.name ?: "Uncategorised"
            if (e.photoPath != null) {
                holder.ivPhoto.visibility = View.VISIBLE
                Glide.with(holder.itemView.context)
                    .load(if (e.photoPath.startsWith("content://")) Uri.parse(e.photoPath) else File(e.photoPath))
                    .thumbnail(0.1f).into(holder.ivPhoto)
                holder.ivPhoto.setOnClickListener { onPhotoClick(e.photoPath) }
            } else {
                holder.ivPhoto.visibility = View.GONE
            }
        }
    }
}
