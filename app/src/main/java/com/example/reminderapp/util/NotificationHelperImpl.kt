package com.example.reminderapp.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.reminderapp.MainActivity
import com.example.reminderapp.R
import com.example.reminderapp.data.model.Priority
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.SoundFetchState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelperImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmScheduler: IAlarmScheduler
) : INotificationHelper {
    companion object {
        const val CHANNEL_ID = "reminder_channel_id"
        private const val CHANNEL_NAME = "Reminder Notifications"
        private const val CHANNEL_DESCRIPTION = "Shows notifications for reminders"
        private const val TAG = "NotificationHelper" // For logging

        // Channel for silent/low priority notifications
        const val SILENT_CHANNEL_ID = "reminder_silent_channel_id"
        private const val SILENT_CHANNEL_NAME = "Silent Reminders"
        private const val SILENT_CHANNEL_DESCRIPTION = "Shows silent notifications for reminders"
    }    override fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Default/High Importance Channel
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()

                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                    description = CHANNEL_DESCRIPTION
                    setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, audioAttributes)
                    enableVibration(true)
                }

                // Silent/Low Importance Channel
                val silentImportance = NotificationManager.IMPORTANCE_LOW
                val silentChannel = NotificationChannel(SILENT_CHANNEL_ID, SILENT_CHANNEL_NAME, silentImportance).apply {
                    description = SILENT_CHANNEL_DESCRIPTION
                    setSound(null, null)
                    enableVibration(false)
                }

                val notificationManager: NotificationManager? =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                
                if (notificationManager == null) {
                    Log.e(TAG, "Failed to create notification channels: NotificationManager service not available")
                    return
                }
                
                try {
                    notificationManager.createNotificationChannel(channel)
                    Log.d(TAG, "Successfully created notification channel: $CHANNEL_ID")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating notification channel: $CHANNEL_ID", e)
                }
                
                try {
                    notificationManager.createNotificationChannel(silentChannel)
                    Log.d(TAG, "Successfully created notification channel: $SILENT_CHANNEL_ID")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating silent notification channel: $SILENT_CHANNEL_ID", e)
                }
            } else {
                Log.d(TAG, "Skipping notification channel creation for Android version < Oreo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error creating notification channels", e)
        }
    }    override fun showNotification(reminder: Reminder, intent: Intent?) {
        try {
            Log.d(TAG, "Preparing to show notification for reminder: ${reminder.id}, title: ${reminder.title}")
            
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                
            if (notificationManager == null) {
                Log.e(TAG, "Failed to show notification: NotificationManager service not available")
                return
            }
            
            // Create pending intent for notification tap action
            val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("REMINDER_ID_FROM_NOTIFICATION", reminder.id)
                putExtra("LIST_ID_FROM_NOTIFICATION", reminder.listId)
            }
            
            val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            
            val pendingIntent: PendingIntent
            try {
                pendingIntent = PendingIntent.getActivity(
                    context,
                    reminder.id.hashCode(),
                    mainActivityIntent,
                    pendingIntentFlags
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create PendingIntent for notification", e)
                // Continue without click action rather than failing completely
                return
            }

            // Determine notification channel and priority based on reminder settings
            val channelIdToUse: String
            val notificationPriority: Int

            // Determine if a custom sound is successfully fetched and enabled
            val useCustomSound = reminder.isSoundEnabled &&
                    reminder.soundFetchState == SoundFetchState.FETCHED &&
                    reminder.localSoundUri != null

            if (useCustomSound) {
                channelIdToUse = CHANNEL_ID
                notificationPriority = when (reminder.priority) {
                    Priority.HIGH -> NotificationCompat.PRIORITY_MAX
                    Priority.MEDIUM -> NotificationCompat.PRIORITY_HIGH
                    Priority.LOW -> NotificationCompat.PRIORITY_LOW
                    Priority.NONE -> NotificationCompat.PRIORITY_DEFAULT
                }
            } else {
                when (reminder.priority) {
                    Priority.HIGH -> {
                        channelIdToUse = CHANNEL_ID
                        notificationPriority = NotificationCompat.PRIORITY_MAX
                    }
                    Priority.MEDIUM -> {
                        channelIdToUse = CHANNEL_ID
                        notificationPriority = NotificationCompat.PRIORITY_HIGH
                    }
                    Priority.LOW -> {
                        channelIdToUse = CHANNEL_ID
                        notificationPriority = NotificationCompat.PRIORITY_LOW
                    }
                    Priority.NONE -> {
                        channelIdToUse = SILENT_CHANNEL_ID
                        notificationPriority = NotificationCompat.PRIORITY_DEFAULT
                    }
                }
            }

            // Build the notification
            val builder = NotificationCompat.Builder(context, channelIdToUse)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(reminder.title)
                .setContentText(reminder.notes ?: "Your reminder is due!")
                .setPriority(notificationPriority)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            // Configure sound settings
            if (reminder.isSoundEnabled) {
                if (useCustomSound) {
                    try {
                        val soundUri = Uri.parse(reminder.localSoundUri)
                        Log.d(TAG, "Attempting to use custom sound URI: $soundUri for channel $channelIdToUse")
                        builder.setSound(soundUri)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing custom sound URI: ${reminder.localSoundUri}", e)
                        // Fallback to default sound
                        Log.d(TAG, "Falling back to default notification sound")
                        builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                    }
                } else {
                    Log.d(TAG, "Using default notification sound for channel $channelIdToUse")
                    builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                }
            } else {
                Log.d(TAG, "Sound is disabled for this notification")
                builder.setSound(null)
            }

            // Configure vibration settings
            if (reminder.isVibrateEnabled) {
                Log.d(TAG, "Vibration is enabled for this notification")
                // Default vibration pattern will be used
            } else {
                Log.d(TAG, "Vibration is disabled for this notification")
                builder.setVibrate(longArrayOf(0L))
            }
            
            // Show the notification
            try {
                val notificationId = reminder.id.hashCode()
                notificationManager.notify(notificationId, builder.build())
                Log.i(TAG, "Successfully showed notification (ID: $notificationId) for reminder: ${reminder.title} on channel $channelIdToUse")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show notification for reminder: ${reminder.id}", e)
            }

            // Handle repeat logic
            if (!reminder.isCompleted) {
                val currentRepeatCount = intent?.getIntExtra("REPEAT_COUNT_CURRENT", reminder.repeatCount) ?: reminder.repeatCount
                if (currentRepeatCount > 0) {
                    try {
                        val nextRepeatTime = System.currentTimeMillis() + (reminder.repeatIntervalMinutes * 60 * 1000L)
                        val repeatReminder = reminder.copy(
                            dueDate = nextRepeatTime,
                        )

                        Log.d(TAG, "Scheduling repeat for reminder: ${reminder.id}, remaining repeats: ${currentRepeatCount - 1}")
                        alarmScheduler.scheduleRepeat(
                            repeatReminder,
                            currentRepeatCount - 1
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to schedule repeat for reminder: ${reminder.id}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error showing notification", e)
        }
    }
}
