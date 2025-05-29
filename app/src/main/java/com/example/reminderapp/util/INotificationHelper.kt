package com.example.reminderapp.util

import android.content.Intent
import com.example.reminderapp.data.model.Reminder

interface INotificationHelper {
    fun createNotificationChannel()
    fun showNotification(reminder: Reminder, intent: Intent? = null)
}
