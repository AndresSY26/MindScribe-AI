package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "nightly_diary_reminders"

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recordatorios de Diario",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones diarias para escribir en tu diario."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when clicking the notification (opens main activity)
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Sentiment/Inspirational titles for daily writing reminder
        val reminders = listOf(
            Pair("¿Cómo estuvo tu día hoy?", "Tómate 2 minutos para escribir tus pensamientos y liberar tu mente."),
            Pair("Tu diario te espera ✨", "Escribe sobre lo que lograste hoy, por más pequeño que sea."),
            Pair("Es hora de reflexionar 🧘", "Cuéntale a tu IA cómo te fue y recibe un consejo motivador hoy."),
            Pair("Mantén el hábito constante 📝", "Un hábito diario de gratitud y diario íntimo transforma tu vida.")
        )
        val reminder = reminders.random()

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_edit) // Standard Android edit icon
            .setContentTitle(reminder.first)
            .setContentText(reminder.second)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)
    }
}
