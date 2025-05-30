package com.example.reminderapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.reminderapp.data.model.Priority
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.SoundFetchState
import com.example.reminderapp.di.Injector

class ReminderAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ReminderAlarmReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        try {
            Log.d(TAG, "Alarm received, extracting reminder data from intent")
            
            // Extract reminder ID - the most critical piece
            val reminderId = intent.getStringExtra("REMINDER_ID")
            if (reminderId == null) {
                Log.e(TAG, "Failed to process alarm: Missing REMINDER_ID in intent")
                return
            }

            // Extract other reminder data with safe fallbacks
            val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
            val reminderNotes = intent.getStringExtra("REMINDER_NOTES")
            val listId = intent.getStringExtra("REMINDER_LIST_ID") ?: ""
            
            // Extract priority with error handling
            val priorityOrdinal = intent.getIntExtra("REMINDER_PRIORITY_ORDINAL", Priority.NONE.ordinal)
            val priority = try {
                Priority.entries.getOrElse(priorityOrdinal) { Priority.NONE }
            } catch (e: Exception) {
                Log.w(TAG, "Invalid priority ordinal: $priorityOrdinal, using NONE", e)
                Priority.NONE
            }
            
            // Extract sound settings
            val soundEnabled = intent.getBooleanExtra("REMINDER_SOUND_ENABLED", true)
            val remoteSoundUrl = intent.getStringExtra("REMINDER_REMOTE_SOUND_URL")
            val localSoundUri = intent.getStringExtra("REMINDER_LOCAL_SOUND_URI")
            
            // Extract sound fetch state with error handling
            val soundFetchStateOrdinal = intent.getIntExtra("REMINDER_SOUND_FETCH_STATE_ORDINAL", SoundFetchState.IDLE.ordinal)
            val soundFetchState = try {
                SoundFetchState.entries.getOrElse(soundFetchStateOrdinal) { SoundFetchState.IDLE }
            } catch (e: Exception) {
                Log.w(TAG, "Invalid sound fetch state ordinal: $soundFetchStateOrdinal, using IDLE", e)
                SoundFetchState.IDLE
            }
            
            // Extract remaining reminder settings
            val soundFetchProgressExtra = intent.getIntExtra("REMINDER_SOUND_FETCH_PROGRESS", -1)
            val soundFetchProgress = if (soundFetchProgressExtra == -1) null else soundFetchProgressExtra
            val vibrateEnabled = intent.getBooleanExtra("REMINDER_VIBRATE_ENABLED", true)
            val repeatCount = intent.getIntExtra("REMINDER_REPEAT_COUNT", 0)
            val repeatInterval = intent.getIntExtra("REMINDER_REPEAT_INTERVAL", 5)
            val currentRepeatCountForNotification = intent.getIntExtra("REPEAT_COUNT_CURRENT", repeatCount)
            val isRepeat = intent.getBooleanExtra("IS_REPEAT_ALARM", false)

            Log.d(TAG, "Creating reminder object for notification. ID: $reminderId, Title: $reminderTitle, IsRepeat: $isRepeat")
            
            // Create reminder object with the extracted data
            val reminder = Reminder(
                id = reminderId,
                title = reminderTitle,
                notes = reminderNotes,
                listId = listId,
                priority = priority,
                isSoundEnabled = soundEnabled,
                remoteSoundUrl = remoteSoundUrl,
                localSoundUri = localSoundUri,
                soundFetchState = soundFetchState,
                soundFetchProgress = soundFetchProgress,
                isVibrateEnabled = vibrateEnabled,
                repeatCount = repeatCount,
                repeatIntervalMinutes = repeatInterval,
                notificationsEnabled = true
            )
            
            // Show notification
            try {
                val notificationHelper = Injector.getNotificationHelper(context)
                notificationHelper.showNotification(reminder, intent)
                Log.d(TAG, "Successfully requested notification for reminder: $reminderId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification for reminder: $reminderId", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error processing alarm", e)
        }
    }
}
