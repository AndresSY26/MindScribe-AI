package com.example.service

import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val calendarName: String
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "${sdf.format(Date(startTime))} - ${sdf.format(Date(endTime))}"
    }
}

object CalendarService {

    /**
     * Queries device calendars (e.g. Google Calendar, Microsoft Exchange, local calendars)
     * for events occurring today.
     */
    fun getEventsForToday(context: Context): List<CalendarEvent> {
        val eventsList = mutableListOf<CalendarEvent>()
        
        // Ensure calendar permission is granted
        if (ContextCompat.checkSelfPermission(
                context,
                "android.permission.READ_CALENDAR"
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        // Define time bounds (Today 00:00:00 to 23:59:59)
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis

        val endOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.CALENDAR_DISPLAY_NAME
        )

        // Query events that overlap with today
        val selection = "(${CalendarContract.Events.DTSTART} >= ?) AND (${CalendarContract.Events.DTSTART} <= ?)"
        val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        try {
            val cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use { c ->
                val idIdx = c.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleIdx = c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val startIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endIdx = c.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
                val calIdx = c.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (c.moveToNext()) {
                    eventsList.add(
                        CalendarEvent(
                            id = c.getLong(idIdx),
                            title = c.getString(titleIdx) ?: "Evento sin título",
                            description = c.getString(descIdx),
                            startTime = c.getLong(startIdx),
                            endTime = c.getLong(endIdx),
                            calendarName = c.getString(calIdx) ?: "Calendario"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return eventsList
    }
}
