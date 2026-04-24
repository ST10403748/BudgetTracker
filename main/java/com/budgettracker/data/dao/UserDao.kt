package com.budgettracker.data.dao
import androidx.room.*
import com.budgettracker.data.entities.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :hash LIMIT 1")
    suspend fun login(username: String, hash: String): User?

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?
}
