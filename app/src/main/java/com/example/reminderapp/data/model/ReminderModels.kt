package com.example.reminderapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class SoundFetchState {
    IDLE, // No attempt to fetch or URL not provided
    FETCHING,
    FETCHED,
    ERROR
}

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var title: String,
    var notes: String? = null,
    var dueDate: Long? = null, // Timestamp
    var priority: Priority = Priority.NONE,
    var isCompleted: Boolean = false,
    val listId: String,

    // Notification Customization
    var isSoundEnabled: Boolean = true,
    var remoteSoundUrl: String? = null, // URL input by user
    var localSoundUri: String? = null, // URI of fetched/cached sound
    var soundFetchState: SoundFetchState = SoundFetchState.IDLE,
    var soundFetchProgress: Int? = null, // Download progress (0-100), null if not fetching or not applicable
    var isVibrateEnabled: Boolean = true,
    var advanceNotificationMinutes: Int = 0, // 0 for at the time, 5, 10, 30 etc.
    var repeatCount: Int = 0, // How many times to repeat if not viewed
    var repeatIntervalMinutes: Int = 5, // Interval for repeats
    var notificationsEnabled: Boolean = true // New field to enable/disable notifications
)

@Entity(tableName = "reminder_lists")
data class ReminderList(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var name: String
    // In a real app, you might add color, icon, etc.
)

enum class Priority {
    NONE, LOW, MEDIUM, HIGH
}
