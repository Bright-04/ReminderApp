package com.example.reminderapp.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.SoundFetchState
import javax.inject.Inject

class AlarmScheduler @Inject constructor(private val context: Context) : IAlarmScheduler {
    companion object {
        private const val TAG = "AlarmScheduler"
    }
      private val alarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }
    
    override fun schedule(reminder: Reminder) {
        // Only schedule if notifications are enabled
        if (!reminder.notificationsEnabled) {
            Log.d(TAG, "Notifications disabled for reminder: ${reminder.id}, canceling any existing alarms")
            cancel(reminder)
            return
        }

        // The reminder.dueDate should already be the adjusted trigger time from ViewModel
        if (reminder.dueDate == null) {
            Log.e(TAG, "Cannot schedule alarm: dueDate is null for reminder: ${reminder.id}")
            cancel(reminder)
            return
        }
        
        if (reminder.dueDate!! <= System.currentTimeMillis()) {
            Log.d(TAG, "Due date is in the past for reminder: ${reminder.id}, canceling alarm")
            cancel(reminder)
            return
        }
        
        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder is completed: ${reminder.id}, canceling alarm")
            cancel(reminder)
            return
        }

        if (alarmManager == null) {
            Log.e(TAG, "Failed to schedule alarm: AlarmManager service not available")
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

        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id.hashCode(), // Unique request code for the main alarm
                intent,
                pendingIntentFlags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager?.canScheduleExactAlarms() ?: false
                } else {
                    true // Assume we can schedule exact alarms on older Android versions
                }
                
                if (!canScheduleExact) {
                    Log.w(TAG, "Cannot schedule exact alarms. This may affect reminder precision.")
                    // Guide user to enable exact alarms permission                    Log.i(TAG, "Falling back to non-exact alarm. Consider enabling exact alarms in Settings > Apps > Special app access > Alarms & reminders")
                    alarmManager?.set(
                        AlarmManager.RTC_WAKEUP,
                        reminder.dueDate!!,
                        pendingIntent
                    )
                    return
                }
            }

            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDate!!,
                pendingIntent
            )
            Log.d(TAG, "Successfully scheduled alarm for reminder: ${reminder.id} at time: ${reminder.dueDate}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling alarm: ${e.message}")
            // Fall back to non-exact alarm as a last resort
            try {
                val fallbackPendingIntent = PendingIntent.getBroadcast(
                    context,
                    reminder.id.hashCode(),
                    intent,
                    pendingIntentFlags
                )
                alarmManager?.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueDate!!,
                    fallbackPendingIntent
                )
                Log.i(TAG, "Fallback to non-exact alarm successful for reminder: ${reminder.id}")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule even fallback alarm: ${e2.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scheduling alarm: ${e.message}", e)
        }
    }

    // New method for scheduling repeats, distinct request code
    override fun scheduleRepeat(reminder: Reminder, remainingRepeats: Int) {
        if (!reminder.notificationsEnabled) {
            Log.d(TAG, "Notifications disabled for repeat reminder: ${reminder.id}, skipping")
            return
        }
        
        if (reminder.dueDate == null) {
            Log.e(TAG, "Cannot schedule repeat alarm: dueDate is null for reminder: ${reminder.id}")
            return
        }
        
        if (reminder.dueDate!! <= System.currentTimeMillis()) {
            Log.d(TAG, "Repeat due date is in the past for reminder: ${reminder.id}, skipping")
            return
        }
        
        if (reminder.isCompleted) {
            Log.d(TAG, "Reminder is completed: ${reminder.id}, skipping repeat")
            return
        }
        
        if (remainingRepeats < 0) {
            Log.d(TAG, "No repeats remaining for reminder: ${reminder.id}, skipping")
            return
        }
        
        if (alarmManager == null) {
            Log.e(TAG, "Failed to schedule repeat alarm: AlarmManager service not available")
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra("REMINDER_ID", reminder.id)
            putExtra("REMINDER_TITLE", reminder.title)
            putExtra("REMINDER_NOTES", reminder.notes)
            putExtra("REMINDER_LIST_ID", reminder.listId)
            putExtra("REMINDER_PRIORITY_ORDINAL", reminder.priority.ordinal)
            putExtra("REMINDER_SOUND_ENABLED", reminder.isSoundEnabled)
            putExtra("REMINDER_REMOTE_SOUND_URL", reminder.remoteSoundUrl)
            putExtra("REMINDER_LOCAL_SOUND_URI", reminder.localSoundUri)
            putExtra("REMINDER_SOUND_FETCH_STATE_ORDINAL", reminder.soundFetchState.ordinal)
            putExtra("REMINDER_SOUND_FETCH_PROGRESS", reminder.soundFetchProgress ?: -1)
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

        try {
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                pendingIntentFlags
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager?.canScheduleExactAlarms() ?: false
                } else {
                    true // Assume we can schedule exact alarms on older Android versions
                }
                
                if (!canScheduleExact) {                    Log.w(TAG, "Cannot schedule exact alarms for repeat. This may affect reminder precision.")
                    // Fallback to non-exact alarm
                    Log.i(TAG, "Falling back to non-exact repeat alarm.")
                    alarmManager?.set(
                        AlarmManager.RTC_WAKEUP,
                        reminder.dueDate!!,
                        pendingIntent
                    )
                    return
                }
            }

            alarmManager?.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.dueDate!!, // This dueDate is the next repeat time
                pendingIntent
            )
            Log.d(TAG, "Successfully scheduled repeat alarm for reminder: ${reminder.id} at time: ${reminder.dueDate}, remaining repeats: $remainingRepeats")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException while scheduling repeat alarm: ${e.message}")
            // Fall back to non-exact alarm as a last resort
            try {
                val fallbackPendingIntent = PendingIntent.getBroadcast(                    context,
                    requestCode,
                    intent,
                    pendingIntentFlags
                )

                alarmManager?.set(
                    AlarmManager.RTC_WAKEUP,
                    reminder.dueDate!!,
                    fallbackPendingIntent
                )
                Log.i(TAG, "Fallback to non-exact repeat alarm successful for reminder: ${reminder.id}")
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule even fallback repeat alarm: ${e2.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error scheduling repeat alarm: ${e.message}", e)
        }
    }
    override fun cancel(reminder: Reminder) {
        if (alarmManager == null) {
            Log.e(TAG, "Failed to cancel alarm: AlarmManager service not available")
            return
        }

        Log.d(TAG, "Attempting to cancel alarms for reminder: ${reminder.id}")
        
        try {
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
            
            if (mainPendingIntent != null) {
                try {
                    alarmManager?.cancel(mainPendingIntent)
                    mainPendingIntent.cancel()
                    Log.d(TAG, "Successfully canceled main alarm for reminder: ${reminder.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error canceling main alarm for reminder: ${reminder.id}", e)
                }
            } else {
                Log.d(TAG, "No active main alarm found for reminder: ${reminder.id}")
            }

            // Cancel any potential repeat alarms
            if (reminder.repeatCount > 0) {
                Log.d(TAG, "Attempting to cancel ${reminder.repeatCount} repeat alarms for reminder: ${reminder.id}")
                var canceledCount = 0
                
                for (i in 0 until reminder.repeatCount) {
                    val requestCode = reminder.id.hashCode() + i + 1000
                    val repeatPendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        mainIntent, // Intent content doesn't strictly matter for cancellation by request code & action
                        mainPendingIntentFlags                    )
                    if (repeatPendingIntent != null) {
                        try {
                            alarmManager?.cancel(repeatPendingIntent)
                            repeatPendingIntent.cancel()
                            canceledCount++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error canceling repeat alarm #$i for reminder: ${reminder.id}", e)
                        }
                    }
                }
                
                Log.d(TAG, "Canceled $canceledCount/${reminder.repeatCount} repeat alarms for reminder: ${reminder.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while canceling alarms for reminder: ${reminder.id}", e)
        }
    }
}
