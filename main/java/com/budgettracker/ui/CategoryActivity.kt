package com.budgettracker.ui
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.budgettracker.data.AppDatabase
import com.budgettracker.data.entities.Category
import com.budgettracker.databinding.ActivityCategoryBinding
import com.budgettracker.utils.SessionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCategoryBinding
    private lateinit var session: SessionManager
    private val categories = mutableListOf<Category>()
    private lateinit var adapter: ArrayAdapter<String>
    private val TAG = "CategoryActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Categories"
        session = SessionManager(this)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        binding.lvCategories.adapter = adapter
        observeCategories()
        binding.btnAddCategory.setOnClickListener {
            val name = binding.etCategoryName.text.toString().trim()
            if (name.isEmpty()) { binding.etCategoryName.error = "Category name required"; return@setOnClickListener }
            lifecycleScope.launch {
                AppDatabase.getInstance(this@CategoryActivity).categoryDao().insert(Category(userId = session.getUserId(), name = name))
                Log.i(TAG, "Category added: $name")
                runOnUiThread { binding.etCategoryName.text?.clear() }
            }
        }
        binding.lvCategories.setOnItemLongClickListener { _, _, pos, _ ->
            lifecycleScope.launch { AppDatabase.getInstance(this@CategoryActivity).categoryDao().delete(categories[pos]) }
            true
        }
    }

    private fun observeCategories() {
        lifecycleScope.launch {
            AppDatabase.getInstance(this@CategoryActivity).categoryDao().getByUser(session.getUserId()).collectLatest { list ->
                categories.clear(); categories.addAll(list)
                adapter.clear(); adapter.addAll(list.map { it.name }); adapter.notifyDataSetChanged()
                Log.d(TAG, "Loaded ${list.size} categories")
            }
        }
    }
}
