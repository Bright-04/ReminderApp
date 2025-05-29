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
    }

    override fun createNotificationChannel() {
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

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    override fun showNotification(reminder: Reminder, intent: Intent?) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            mainActivityIntent,
            pendingIntentFlags
        )

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

        val builder = NotificationCompat.Builder(context, channelIdToUse)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText(reminder.notes ?: "Your reminder is due!")
            .setPriority(notificationPriority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (reminder.isSoundEnabled) {
            if (useCustomSound) {
                try {
                    val soundUri = Uri.parse(reminder.localSoundUri)
                    Log.d(TAG, "Attempting to use custom sound URI: $soundUri for channel $channelIdToUse")
                    builder.setSound(soundUri)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing custom sound URI: ${reminder.localSoundUri}", e)
                    builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
                }
            } else {
                Log.d(TAG, "Using default notification sound for channel $channelIdToUse.")
                builder.setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            }
        } else {
            Log.d(TAG, "Sound is disabled for this notification.")
            builder.setSound(null)
        }

        if (reminder.isVibrateEnabled) {
            Log.d(TAG, "Vibration is enabled for this notification.")
        } else {
            Log.d(TAG, "Vibration is disabled for this notification.")
            builder.setVibrate(longArrayOf(0L))
        }
        Log.i(TAG, "Showing notification for reminder: ${reminder.title} on channel $channelIdToUse")
        notificationManager.notify(reminder.id.hashCode(), builder.build())

        // Handle repeat logic
        val currentRepeatCount = intent?.getIntExtra("REPEAT_COUNT_CURRENT", reminder.repeatCount) ?: reminder.repeatCount
        if (currentRepeatCount > 0 && !reminder.isCompleted) {
            val nextRepeatTime = System.currentTimeMillis() + (reminder.repeatIntervalMinutes * 60 * 1000L)
            val repeatReminder = reminder.copy(
                dueDate = nextRepeatTime,
            )

            alarmScheduler.scheduleRepeat(
                repeatReminder,
                currentRepeatCount - 1
            )
        }
    }
}
