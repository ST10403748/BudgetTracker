package com.budgettracker.data.dao
import androidx.room.*
import com.budgettracker.data.entities.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :from AND :to ORDER BY date DESC, startTime DESC")
    fun getByPeriod(userId: Int, from: String, to: String): Flow<List<Expense>>

    @Query("SELECT categoryId, SUM(amount) as total FROM expenses WHERE userId = :userId AND date BETWEEN :from AND :to GROUP BY categoryId")
    suspend fun getTotalByCategory(userId: Int, from: String, to: String): List<CategoryTotal>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date LIKE :monthPrefix || '%'")
    suspend fun getTotalForMonth(userId: Int, monthPrefix: String): Double?
}

data class CategoryTotal(val categoryId: Int?, val total: Double)
