package com.budgettracker.data
import android.content.Context
import androidx.room.*
import com.budgettracker.data.dao.*
import com.budgettracker.data.entities.*

@Database(entities = [User::class, Category::class, Expense::class, MonthlyGoal::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "budget_tracker.db").build().also { INSTANCE = it }
            }
    }
}
