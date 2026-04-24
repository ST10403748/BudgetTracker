package com.budgettracker.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.data.entities.Category
import com.budgettracker.data.entities.Expense
import com.budgettracker.databinding.ActivityAddExpenseBinding
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddExpenseBinding
    private lateinit var session: SessionManager
    private var categories = listOf<Category>()
    private var photoPath: String? = null
    private var photoUri: Uri? = null
    private val TAG = "AddExpenseActivity"

    // Camera launcher - FIXED: Use ActivityResultLauncher<Uri>
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d(TAG, "Photo captured: $photoPath")
            photoUri?.let { uri ->
                binding.ivExpensePhoto.setImageURI(uri)
                binding.ivExpensePhoto.visibility = android.view.View.VISIBLE
            }
        }
    }

    // Gallery launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            photoPath = it.toString()
            photoUri = it
            binding.ivExpensePhoto.setImageURI(it)
            binding.ivExpensePhoto.visibility = android.view.View.VISIBLE
            Log.d(TAG, "Photo selected from gallery: $photoPath")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddExpenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Add Expense"

        session = SessionManager(this)
        loadCategories()

        // Date picker
        binding.etDate.setOnClickListener { showDatePicker() }

        // Time pickers
        binding.etStartTime.setOnClickListener { showTimePicker(true) }
        binding.etEndTime.setOnClickListener { showTimePicker(false) }

        binding.btnTakePhoto.setOnClickListener { takePhoto() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
        binding.btnSave.setOnClickListener { saveExpense() }
    }

    private fun loadCategories() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            categories = db.categoryDao().getByUserOnce(session.getUserId())
            val names = categories.map { it.name }.toMutableList()
            if (names.isNotEmpty()) {
                names.add(0, "-- Select Category --")
            } else {
                names.add("-- No Categories - Add One First --")
            }
            runOnUiThread {
                val adapter = ArrayAdapter(
                    this@AddExpenseActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    names
                )
                binding.spinnerCategory.adapter = adapter
            }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            val date = String.format("%04d-%02d-%02d", y, m + 1, d)
            binding.etDate.setText(date)
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, h, min ->
            val time = String.format("%02d:%02d", h, min)
            if (isStart) {
                binding.etStartTime.setText(time)
            } else {
                binding.etEndTime.setText(time)
            }
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
    }

    private fun takePhoto() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timestamp.jpg"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
        photoPath = file.absolutePath
        photoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            file
        )
        photoUri?.let { cameraLauncher.launch(it) }
    }

    private fun saveExpense() {
        val description = binding.etDescription.text.toString().trim()
        val amountStr = binding.etAmount.text.toString().trim()
        val date = binding.etDate.text.toString().trim()
        val startTime = binding.etStartTime.text.toString().trim()
        val endTime = binding.etEndTime.text.toString().trim()
        val categoryPos = binding.spinnerCategory.selectedItemPosition

        // Validation
        if (description.isEmpty()) {
            binding.etDescription.error = "Description required"
            return
        }
        if (amountStr.isEmpty()) {
            binding.etAmount.error = "Amount required"
            return
        }
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.etAmount.error = "Enter a valid amount"
            return
        }
        if (date.isEmpty()) {
            binding.etDate.error = "Date required"
            return
        }
        if (startTime.isEmpty()) {
            binding.etStartTime.error = "Start time required"
            return
        }
        if (endTime.isEmpty()) {
            binding.etEndTime.error = "End time required"
            return
        }
        if (categories.isEmpty() || categoryPos == 0) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val categoryId = categories[categoryPos - 1].id
        val expense = Expense(
            userId = session.getUserId(),
            categoryId = categoryId,
            description = description,
            amount = amount,
            date = date,
            startTime = startTime,
            endTime = endTime,
            photoPath = photoPath
        )

        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            db.expenseDao().insert(expense)
            Log.i(TAG, "Expense saved: $description, R$amount on $date")
            runOnUiThread {
                Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}