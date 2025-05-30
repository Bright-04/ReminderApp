package com.example.reminderapp.ui.viewmodel

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.Log
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
    }    // Implements sound download and update logic with improved error handling
    fun fetchCustomSound(reminderId: String, url: String) {
        viewModelScope.launch {
            try {
                val reminder = repository.getReminderById(reminderId)
                if (reminder == null) {
                    Log.e("ReminderViewModel", "Failed to fetch sound: Reminder with ID $reminderId not found")
                    return@launch
                }
                
                // Validate URL format
                if (!isValidUrl(url)) {
                    Log.e("ReminderViewModel", "Invalid URL format: $url")
                    repository.updateReminder(reminder.copy(
                        soundFetchState = SoundFetchState.ERROR,
                        soundFetchProgress = null
                    ))
                    return@launch
                }
                
                // Set state to FETCHING
                repository.updateReminder(reminder.copy(soundFetchState = SoundFetchState.FETCHING, soundFetchProgress = 0))
                
                try {
                    val request = DownloadManager.Request(Uri.parse(url))
                        .setTitle("Reminder Sound")
                        .setDescription("Downloading custom reminder sound")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MUSIC, "reminder_${reminderId}.mp3")
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                    
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    
                    if (downloadManager == null) {
                        Log.e("ReminderViewModel", "DownloadManager service not available")
                        viewModelScope.launch {
                            handleDownloadError(reminder, "Download service not available")
                        }
                        return@launch
                    }
                    
                    val downloadId = downloadManager.enqueue(request)
                    
                    if (downloadId == -1L) {                        Log.e("ReminderViewModel", "Failed to enqueue download request")
                        viewModelScope.launch {
                            handleDownloadError(reminder, "Failed to start download")
                        }
                        return@launch
                    }
                    
                    // Set up timeout for download (5 minutes)
                    val timeoutJob = viewModelScope.launch {
                        kotlinx.coroutines.delay(5 * 60 * 1000L) // 5 minutes
                        // Check if download is still in progress
                        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                        if (cursor.moveToFirst()) {
                            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                            if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) {
                                // Download still in progress after timeout
                                downloadManager.remove(downloadId)
                                handleDownloadError(reminder, "Download timed out")
                            }
                        }
                        cursor.close()
                    }

                    // Register receiver for download completion
                    val receiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context, intent: Intent) {
                            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (id == downloadId) {
                                timeoutJob.cancel() // Cancel the timeout job
                                
                                try {
                                    val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                                    if (cursor.moveToFirst()) {
                                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                            // Get the downloaded file URI
                                            val uriString = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                            if (uriString != null) {
                                                viewModelScope.launch {
                                                    val updated = reminder.copy(
                                                        localSoundUri = uriString,
                                                        soundFetchState = SoundFetchState.FETCHED,
                                                        soundFetchProgress = 100
                                                    )
                                                    repository.updateReminder(updated)
                                                    Log.d("ReminderViewModel", "Sound download successful: $uriString")
                                                }
                                            } else {                                                viewModelScope.launch {
                                                    handleDownloadError(reminder, "Downloaded file URI is null")
                                                }
                                            }
                                        } else {
                                            // Get reason for failure
                                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                            Log.e("ReminderViewModel", "Download failed with status: $status, reason: $reason")
                                            viewModelScope.launch {
                                                handleDownloadError(reminder, "Download failed (status: $status)")
                                            }
                                        }
                                    } else {                                        viewModelScope.launch {
                                            handleDownloadError(reminder, "Download query returned no results")
                                        }
                                    }
                                    cursor.close()
                                } catch (e: Exception) {
                                    Log.e("ReminderViewModel", "Error processing download result", e)
                                    viewModelScope.launch {
                                        handleDownloadError(reminder, "Error processing download: ${e.message}")
                                    }
                                } finally {
                                    // Unregister receiver
                                    try {
                                        context.unregisterReceiver(this)
                                    } catch (e: IllegalArgumentException) {
                                        // Receiver might already be unregistered
                                        Log.w("ReminderViewModel", "Failed to unregister download receiver", e)
                                    }
                                }
                            }
                        }
                    }
                    
                    try {
                        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                    } catch (e: Exception) {
                        Log.e("ReminderViewModel", "Failed to register download receiver", e)
                        viewModelScope.launch {
                            handleDownloadError(reminder, "Failed to register download receiver")
                        }
                        timeoutJob.cancel()
                    }
                } catch (e: Exception) {
                    Log.e("ReminderViewModel", "Error setting up download", e)
                    viewModelScope.launch {
                        handleDownloadError(reminder, "Error setting up download: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReminderViewModel", "Unexpected error in fetchCustomSound", e)
            }
        }
    }
    
    // Helper method to validate URL
    private fun isValidUrl(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            uri.scheme != null && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
        }
    }
    
    // Helper method to handle download errors
    private suspend fun handleDownloadError(reminder: Reminder, errorMessage: String) {
        Log.e("ReminderViewModel", errorMessage)
        repository.updateReminder(reminder.copy(
            soundFetchState = SoundFetchState.ERROR,
            soundFetchProgress = null
        ))
    }
}
