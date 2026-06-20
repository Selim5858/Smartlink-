package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM link_buttons ORDER BY id ASC")
    fun getAllLinkButtons(): Flow<List<LinkButton>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinkButton(linkButton: LinkButton)

    @Update
    suspend fun updateLinkButton(linkButton: LinkButton)

    @Delete
    suspend fun deleteLinkButton(linkButton: LinkButton)

    @Query("DELETE FROM link_buttons WHERE id = :id")
    suspend fun deleteLinkButtonById(id: Int)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUser(username: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: User)

    @Query("SELECT COUNT(*) FROM users")
    fun getUsersCount(): Flow<Int>
}
