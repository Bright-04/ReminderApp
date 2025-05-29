package com.example.reminderapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.SoundFetchState
import javax.inject.Inject

class AlarmScheduler @Inject constructor(private val context: Context) : IAlarmScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(reminder: Reminder) {
        // Only schedule if notifications are enabled
        if (!reminder.notificationsEnabled) {
            cancel(reminder)
            return
        }

        // The reminder.dueDate should already be the adjusted trigger time from ViewModel
        if (reminder.dueDate == null || reminder.dueDate!! <= System.currentTimeMillis() || reminder.isCompleted) {
            cancel(reminder) // Cancel if it's past, null, or completed
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            // Pass all necessary reminder data for notification and potential repeats
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_NOTES", reminder.notes)
            putExtra("REMINDER_LIST_ID", reminder.listId)
            putExtra("REMINDER_PRIORITY_ORDINAL", reminder.priority.ordinal)
            putExtra("REMINDER_SOUND_ENABLED", reminder.isSoundEnabled)
            // putExtra("REMINDER_SOUND_URI", reminder.notificationSoundUri) // Old field
            putExtra("REMINDER_REMOTE_SOUND_URL", reminder.remoteSoundUrl)
            putExtra("REMINDER_LOCAL_SOUND_URI", reminder.localSoundUri)
            putExtra("REMINDER_SOUND_FETCH_STATE_ORDINAL", reminder.soundFetchState.ordinal)
            putExtra("REMINDER_SOUND_FETCH_PROGRESS", reminder.soundFetchProgress ?: -1) // Pass progress, -1 if null
            putExtra("REMINDER_VIBRATE_ENABLED", reminder.isVibrateEnabled)
            putExtra("REMINDER_REPEAT_COUNT", reminder.repeatCount)
            putExtra("REMINDER_REPEAT_INTERVAL", reminder.repeatIntervalMinutes)
            // No need to pass advanceNotificationMinutes here as dueDate is already adjusted
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(), // Unique request code for the main alarm
            intent,
            pendingIntentFlags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Handle case where exact alarms are not permitted.
                // Maybe navigate user to settings or use inexact alarm.
                // For now, we'll just log or skip.
                println("Cannot schedule exact alarms.")
                return
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDate!!,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle SecurityException, e.g., if USE_EXACT_ALARM permission is missing or denied at runtime
            println("SecurityException while scheduling alarm: ${e.message}")
            // Potentially fall back to an inexact alarm or inform the user
        }
    }

    // New method for scheduling repeats, distinct request code
    override fun scheduleRepeat(reminder: Reminder, remainingRepeats: Int) {
        if (!reminder.notificationsEnabled) return // Only schedule if enabled
        if (reminder.dueDate == null || reminder.dueDate!! <= System.currentTimeMillis() || reminder.isCompleted || remainingRepeats < 0) {
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_NOTES", reminder.notes)
            putExtra("REMINDER_LIST_ID", reminder.listId)
            putExtra("REMINDER_PRIORITY_ORDINAL", reminder.priority.ordinal)
            putExtra("REMINDER_SOUND_ENABLED", reminder.isSoundEnabled)
            // putExtra("REMINDER_SOUND_URI", reminder.notificationSoundUri) // Old field
            putExtra("REMINDER_REMOTE_SOUND_URL", reminder.remoteSoundUrl)
            putExtra("REMINDER_LOCAL_SOUND_URI", reminder.localSoundUri)
            putExtra("REMINDER_SOUND_FETCH_STATE_ORDINAL", reminder.soundFetchState.ordinal)
            putExtra("REMINDER_SOUND_FETCH_PROGRESS", reminder.soundFetchProgress ?: -1) // Pass progress, -1 if null
            putExtra("REMINDER_VIBRATE_ENABLED", reminder.isVibrateEnabled)
            putExtra("REMINDER_REPEAT_COUNT", reminder.repeatCount) // Original repeat count
            putExtra("REMINDER_REPEAT_INTERVAL", reminder.repeatIntervalMinutes)
            putExtra("REPEAT_COUNT_CURRENT", remainingRepeats) // Current remaining repeats
            putExtra("IS_REPEAT_ALARM", true)
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        // Use a different request code for repeats to avoid cancelling the main alarm or other repeats
        val requestCode = reminder.id.hashCode() + (reminder.repeatCount - remainingRepeats) + 1000 

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            pendingIntentFlags
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                println("Cannot schedule exact alarms for repeat.")
                return
            }
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDate!!, // This dueDate is the next repeat time
                pendingIntent
            )
        } catch (e: SecurityException) {
            println("SecurityException while scheduling repeat alarm: ${e.message}")
        }
    }


    override fun cancel(reminder: Reminder) {
        // Cancel the main alarm
        val mainIntent = Intent(context, ReminderAlarmReceiver::class.java)
        val mainPendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_NO_CREATE
        }
        val mainPendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            mainIntent,
            mainPendingIntentFlags
        )
        mainPendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }

        // Cancel any potential repeat alarms
        // This requires iterating through possible request codes used for repeats
        // or a more robust way to track active repeat PendingIntents.
        // For simplicity, if repeatCount > 0, try to cancel them.
        if (reminder.repeatCount > 0) {
            for (i in 0 until reminder.repeatCount) {
                 val requestCode = reminder.id.hashCode() + i + 1000
                 val repeatPendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    mainIntent, // Intent content doesn't strictly matter for cancellation by request code & action
                    mainPendingIntentFlags
                )
                repeatPendingIntent?.let {
                    alarmManager.cancel(it)
                    it.cancel()
                }
            }
        }
    }
}
