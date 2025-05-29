package com.example.reminderapp.ui.viewmodel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.ReminderList
import com.example.reminderapp.data.model.SoundFetchState
import com.example.reminderapp.data.repository.IReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class ReminderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: IReminderRepository
) : ViewModel() {

    val reminderLists = repository.getAllLists().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getRemindersForList(listId: String) = repository.getRemindersForList(listId).stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addReminder(reminder: Reminder) {
        viewModelScope.launch { repository.insertReminder(reminder) }
    }
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch { repository.updateReminder(reminder) }
    }
    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch { repository.deleteReminder(reminder) }
    }
    fun addReminderList(list: ReminderList) {
        viewModelScope.launch { repository.insertList(list) }
    }
    fun updateReminderList(list: ReminderList) {
        viewModelScope.launch { repository.updateList(list) }
    }
    fun deleteReminderList(list: ReminderList) {
        viewModelScope.launch { repository.deleteList(list) }
    }

    // Get a ReminderList by ID from the current state (for Compose screens)
    fun getReminderList(listId: String): ReminderList? {
        return reminderLists.value.find { it.id == listId }
    }

    // Get a Reminder by ID from the current reminders for a list (for Compose screens)
    fun getReminder(reminderId: String, listId: String): Reminder? {
        return getRemindersForList(listId).value.find { it.id == reminderId }
    }

    // Toggle completion status of a reminder
    fun toggleReminderCompletion(reminder: Reminder) {
        val updated = reminder.copy(isCompleted = !reminder.isCompleted)
        updateReminder(updated)
    }

    // Get counts of active and completed reminders for a list as a Flow
    fun getReminderCountsForList(listId: String): Flow<Pair<Int, Int>> {
        return repository.getRemindersForList(listId).map { reminders ->
            val active = reminders.count { !it.isCompleted }
            val completed = reminders.count { it.isCompleted }
            Pair(active, completed)
        }
    }

    // Helper for addReminderList(name: String)
    fun addReminderList(name: String) {
        val list = ReminderList(name = name)
        addReminderList(list)
    }

    // Implements sound download and update logic
    fun fetchCustomSound(reminderId: String, url: String) {
        viewModelScope.launch {
            val reminder = repository.getReminderById(reminderId)
            if (reminder != null) {
                // Set state to FETCHING
                repository.updateReminder(reminder.copy(soundFetchState = SoundFetchState.FETCHING, soundFetchProgress = 0))
                val request = DownloadManager.Request(Uri.parse(url))
                    .setTitle("Reminder Sound")
                    .setDescription("Downloading custom reminder sound")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, "reminder_${reminderId}.mp3")
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val downloadId = downloadManager.enqueue(request)

                // Register receiver for download completion
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        if (id == downloadId) {
                            val cursor: Cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                            if (cursor.moveToFirst()) {
                                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                    val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                    viewModelScope.launch {
                                        val updated = reminder.copy(
                                            localSoundUri = uriString,
                                            soundFetchState = SoundFetchState.FETCHED,
                                            soundFetchProgress = 100
                                        )
                                        repository.updateReminder(updated)
                                    }
                                } else {
                                    viewModelScope.launch {
                                        repository.updateReminder(reminder.copy(soundFetchState = SoundFetchState.ERROR, soundFetchProgress = null))
                                    }
                                }
                            }
                            cursor.close()
                            // Unregister receiver
                            context.unregisterReceiver(this)
                        }
                    }                }
                context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
        }
    }
}
