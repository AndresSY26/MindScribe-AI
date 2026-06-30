package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // format "YYYY-MM-DD"
    val title: String,
    val content: String,
    val mood: String, // "Excelente", "Bueno", "Neutral", "Triste", "Estresado"
    val tags: String = "", // comma-separated strings
    val aiSentiment: String = "", // analysis description
    val aiAdvice: String = "", // motivational or reflective advice
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) : Serializable
