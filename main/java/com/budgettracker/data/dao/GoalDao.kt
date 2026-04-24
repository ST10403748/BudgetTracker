package com.budgettracker.data.dao
import androidx.room.*
import com.budgettracker.data.entities.MonthlyGoal

@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: MonthlyGoal): Long

    @Query("SELECT * FROM monthly_goals WHERE userId = :userId AND month = :month LIMIT 1")
    suspend fun getGoal(userId: Int, month: String): MonthlyGoal?

    @Update
    suspend fun update(goal: MonthlyGoal)
}
