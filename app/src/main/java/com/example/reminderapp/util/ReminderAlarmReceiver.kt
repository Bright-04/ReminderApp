package com.example.reminderapp.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.reminderapp.data.model.Priority
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.SoundFetchState
import com.example.reminderapp.di.Injector

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID")
        val reminderTitle = intent.getStringExtra("REMINDER_TITLE") ?: "Reminder"
        val reminderNotes = intent.getStringExtra("REMINDER_NOTES")
        val listId = intent.getStringExtra("REMINDER_LIST_ID") ?: ""
        val priorityOrdinal = intent.getIntExtra("REMINDER_PRIORITY_ORDINAL", Priority.NONE.ordinal)
        val priority = Priority.entries.getOrElse(priorityOrdinal) { Priority.NONE }
        val soundEnabled = intent.getBooleanExtra("REMINDER_SOUND_ENABLED", true)
        val remoteSoundUrl = intent.getStringExtra("REMINDER_REMOTE_SOUND_URL")
        val localSoundUri = intent.getStringExtra("REMINDER_LOCAL_SOUND_URI")
        val soundFetchStateOrdinal = intent.getIntExtra("REMINDER_SOUND_FETCH_STATE_ORDINAL", SoundFetchState.IDLE.ordinal)
        val soundFetchState = SoundFetchState.entries.getOrElse(soundFetchStateOrdinal) { SoundFetchState.IDLE }
        val soundFetchProgressExtra = intent.getIntExtra("REMINDER_SOUND_FETCH_PROGRESS", -1)
        val soundFetchProgress = if (soundFetchProgressExtra == -1) null else soundFetchProgressExtra
        val vibrateEnabled = intent.getBooleanExtra("REMINDER_VIBRATE_ENABLED", true)
        val repeatCount = intent.getIntExtra("REMINDER_REPEAT_COUNT", 0)
        val repeatInterval = intent.getIntExtra("REMINDER_REPEAT_INTERVAL", 5)
        val currentRepeatCountForNotification = intent.getIntExtra("REPEAT_COUNT_CURRENT", repeatCount)
        val isRepeat = intent.getBooleanExtra("IS_REPEAT_ALARM", false)

        if (reminderId != null) {
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
            val notificationHelper = Injector.getNotificationHelper(context)
            notificationHelper.showNotification(reminder)
        }
    }
}
