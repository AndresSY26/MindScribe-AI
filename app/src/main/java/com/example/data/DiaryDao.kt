package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): DiaryEntry?

    @Query("SELECT * FROM diary_entries WHERE title LIKE :query OR content LIKE :query OR tags LIKE :query ORDER BY date DESC")
    fun searchEntries(query: String): Flow<List<DiaryEntry>>

    @Query("SELECT * FROM diary_entries WHERE isSynced = 0")
    suspend fun getUnsyncedEntries(): List<DiaryEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DiaryEntry): Long

    @Query("UPDATE diary_entries SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Delete
    suspend fun deleteEntry(entry: DiaryEntry)
}
