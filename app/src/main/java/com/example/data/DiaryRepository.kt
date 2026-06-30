package com.example.data

import com.example.service.SupabaseService
import kotlinx.coroutines.flow.Flow

class DiaryRepository(private val diaryDao: DiaryDao) {

    val allEntries: Flow<List<DiaryEntry>> = diaryDao.getAllEntries()

    suspend fun getEntryByDate(date: String): DiaryEntry? {
        return diaryDao.getEntryByDate(date)
    }

    fun searchEntries(query: String): Flow<List<DiaryEntry>> {
        val formattedQuery = "%$query%"
        return diaryDao.searchEntries(formattedQuery)
    }

    suspend fun insertEntry(entry: DiaryEntry): Long {
        return diaryDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: DiaryEntry) {
        diaryDao.deleteEntry(entry)
    }

    /**
     * Synchronizes any unsynced entries with Supabase.
     * Returns a pair of (successCount, failureCount).
     */
    suspend fun syncUnsyncedEntries(url: String, anonKey: String, userId: String): Pair<Int, Int> {
        if (url.isEmpty() || anonKey.isEmpty()) return Pair(0, 0)
        
        var successCount = 0
        var failureCount = 0

        val unsyncedList = diaryDao.getUnsyncedEntries()
        for (entry in unsyncedList) {
            val success = SupabaseService.syncEntry(url, anonKey, userId, entry)
            if (success) {
                diaryDao.markAsSynced(entry.id)
                successCount++
            } else {
                failureCount++
            }
        }

        return Pair(successCount, failureCount)
    }
}
