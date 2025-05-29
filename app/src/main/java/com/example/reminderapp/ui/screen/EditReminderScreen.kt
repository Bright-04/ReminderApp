package com.example.reminderapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.ui.components.AdvanceNotificationSelector
import com.example.reminderapp.ui.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.reminderapp.data.model.SoundFetchState
import com.example.reminderapp.ui.components.DateTimeChip
import com.example.reminderapp.ui.components.PrioritySelector
import androidx.compose.material3.CircularProgressIndicator
import com.example.reminderapp.data.repository.ReminderRepository // Import ReminderRepository
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.platform.LocalContext
import android.app.TimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderScreen(
    navController: NavController,
    viewModel: ReminderViewModel,
    listId: String,
    reminderId: String?
) {
    val isEditing = reminderId != null
    val existingReminder = if (isEditing) viewModel.getReminder(reminderId!!, listId) else null

    var title by rememberSaveable { mutableStateOf(existingReminder?.title ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingReminder?.notes ?: "") }

    val initialCalendar = Calendar.getInstance().apply {
        existingReminder?.dueDate?.let { timeInMillis = it }
    }
    var reminderDateTimeCalendar by remember { mutableStateOf(initialCalendar) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var dueDateEnabled by rememberSaveable { mutableStateOf(existingReminder?.dueDate != null) }

    // Notification settings states
    var soundEnabled by rememberSaveable { mutableStateOf(existingReminder?.isSoundEnabled ?: true) }
    var notificationsEnabled by rememberSaveable { mutableStateOf(existingReminder?.notificationsEnabled ?: true) } // New state
    // var customSoundUri by rememberSaveable { mutableStateOf(existingReminder?.notificationSoundUri ?: "") } // Old field
    var remoteSoundUrlInput by rememberSaveable { mutableStateOf(existingReminder?.remoteSoundUrl ?: "") }
    var vibrateEnabled by rememberSaveable { mutableStateOf(existingReminder?.isVibrateEnabled ?: true) }
    var selectedAdvanceMinutes by rememberSaveable { mutableStateOf(existingReminder?.advanceNotificationMinutes ?: 0) }
    var repeatCount by rememberSaveable { mutableStateOf(existingReminder?.repeatCount?.toString() ?: "0") }
    var repeatInterval by rememberSaveable { mutableStateOf(existingReminder?.repeatIntervalMinutes?.toString() ?: "5") }
    var selectedPriority by remember { mutableStateOf(existingReminder?.priority ?: com.example.reminderapp.data.model.Priority.NONE) }


    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(
                state = rememberDatePickerState(initialSelectedDateMillis = reminderDateTimeCalendar.timeInMillis)
                .also { state ->
                    LaunchedEffect(state.selectedDateMillis) {
                        state.selectedDateMillis?.let { millis ->
                            val newCal = Calendar.getInstance().apply { timeInMillis = millis }
                            reminderDateTimeCalendar.set(
                                newCal.get(Calendar.YEAR),
                                newCal.get(Calendar.MONTH),
                                newCal.get(Calendar.DAY_OF_MONTH)
                            )
                        }
                    }
                }
            )
        }
    }

    if (showTimePickerDialog) {
        // Improved Time Picker Dialog using Android's TimePickerDialog for better UX
        val context = LocalContext.current
        val hour = reminderDateTimeCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = reminderDateTimeCalendar.get(Calendar.MINUTE)
        DisposableEffect(showTimePickerDialog) {
            if (showTimePickerDialog) {
                val dialog = TimePickerDialog(
                    context,
                    { _, selectedHour, selectedMinute ->
                        reminderDateTimeCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                        reminderDateTimeCalendar.set(Calendar.MINUTE, selectedMinute)
                        showTimePickerDialog = false
                    },
                    hour,
                    minute,
                    false // set to true for 24-hour format
                )
                dialog.setOnCancelListener { showTimePickerDialog = false }
                dialog.show()
            }
            onDispose { }
        }
    }


    // Get all reminders for this list from the ViewModel
    val allRemindersFromRepository by viewModel.getRemindersForList(listId).collectAsState(initial = emptyList())

    // Find the live instance of the reminder being edited from the repository's list.
    val liveReminderInstance = remember(existingReminder?.id, allRemindersFromRepository) {
        if (isEditing && existingReminder?.id != null) {
            allRemindersFromRepository.find { it.id == existingReminder.id }
        } else {
            null // Not editing or no existing reminder, so no live instance from repo yet.
        }
    }
    // Determine the sound fetch status and local URI to display.
    val soundFetchStatus = liveReminderInstance?.soundFetchState ?: existingReminder?.soundFetchState ?: SoundFetchState.IDLE
    val actualLocalSoundUri = liveReminderInstance?.localSoundUri ?: existingReminder?.localSoundUri
    val soundDownloadProgress = liveReminderInstance?.soundFetchProgress ?: existingReminder?.soundFetchProgress


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Reminder" else "Add New Reminder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (title.isNotBlank()) {
                            val finalDueDate = if (dueDateEnabled) reminderDateTimeCalendar.timeInMillis else null
                            val currentRepeatCount = repeatCount.toIntOrNull() ?: 0
                            val currentRepeatInterval = repeatInterval.toIntOrNull() ?: 5

                            if (isEditing && existingReminder != null) {
                                // Preserve existing localSoundUri and fetchState unless remoteSoundUrlInput changes them
                                val finalRemoteSoundUrl = remoteSoundUrlInput.ifBlank { null }
                                var soundStateToSave = existingReminder.soundFetchState
                                var localUriToSave = existingReminder.localSoundUri

                                if (existingReminder.remoteSoundUrl != finalRemoteSoundUrl) {
                                    soundStateToSave = SoundFetchState.IDLE
                                    localUriToSave = null
                                }


                                viewModel.updateReminder(
                                    existingReminder.copy(
                                        title = title,
                                        notes = notes.ifBlank { null },
                                        dueDate = finalDueDate,
                                        priority = selectedPriority,
                                        isSoundEnabled = soundEnabled,
                                        notificationsEnabled = notificationsEnabled, // Pass new field
                                        remoteSoundUrl = finalRemoteSoundUrl,
                                        localSoundUri = localUriToSave, // Keep local if remote URL hasn't changed
                                        soundFetchState = soundStateToSave, // Keep state if remote URL hasn't changed
                                        isVibrateEnabled = vibrateEnabled,
                                        advanceNotificationMinutes = selectedAdvanceMinutes,
                                        repeatCount = currentRepeatCount,
                                        repeatIntervalMinutes = currentRepeatInterval
                                    )
                                )
                            } else {
                                val newReminder = Reminder(
                                    title = title,
                                    notes = notes.ifBlank { null },
                                    dueDate = finalDueDate,
                                    priority = selectedPriority,
                                    isCompleted = false,
                                    listId = listId,
                                    isSoundEnabled = soundEnabled,
                                    remoteSoundUrl = remoteSoundUrlInput.ifBlank { null },
                                    localSoundUri = null,
                                    soundFetchState = SoundFetchState.IDLE,
                                    soundFetchProgress = null,
                                    isVibrateEnabled = vibrateEnabled,
                                    advanceNotificationMinutes = selectedAdvanceMinutes,
                                    repeatCount = currentRepeatCount,
                                    repeatIntervalMinutes = currentRepeatInterval,
                                    notificationsEnabled = notificationsEnabled
                                )
                                viewModel.addReminder(newReminder)
                            }
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.Done, "Save Reminder")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()), // Added for scrollability
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp) // Increased spacing
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))            // Due Date/Time Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {                    Row(verticalAlignment = Alignment.CenterVertically) {                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "Due Date",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Due Date & Time", 
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = dueDateEnabled,
                            onCheckedChange = { dueDateEnabled = it }
                        )
                    }
                    
                    if (dueDateEnabled) {
                        // Date picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateTimeChip(
                                text = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
                                    .format(reminderDateTimeCalendar.time),
                                onClick = { showDatePickerDialog = true }
                            )
                            
                            DateTimeChip(
                                text = SimpleDateFormat("h:mm a", Locale.getDefault())
                                    .format(reminderDateTimeCalendar.time),
                                onClick = { showTimePickerDialog = true }
                            )
                        }
                    } else {
                        Text(
                            "No due date set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))            // Notification Settings Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notification Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Notification Settings", 
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    // Enhanced Enable Notifications Switch
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notificationsEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            contentColor = if (notificationsEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Enable Notifications",
                                tint = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Enable Notifications",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { notificationsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.error,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    uncheckedTrackColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                    // All notification settings below are grayed out if notifications are disabled
                    val sectionAlpha = if (notificationsEnabled) 1f else 0.4f
                    // Remove CompositionLocalProvider for alpha
                    // Use Modifier.alpha(sectionAlpha) for all notification-related UI blocks
                    // Remove 'enabled' param from AdvanceNotificationSelector
                    Column(
                        modifier = Modifier.alpha(sectionAlpha),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Priority selector
                        PrioritySelector(
                            selectedPriority = selectedPriority,
                            onPrioritySelected = { selectedPriority = it }
                        )
                        Divider()
                        // Sound Settings
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text("Enable Sound", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.weight(1f))
                                Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it }, enabled = notificationsEnabled)
                            }
                            if (soundEnabled) {
                                OutlinedTextField(
                                    value = remoteSoundUrlInput,
                                    onValueChange = { remoteSoundUrlInput = it },
                                    label = { Text("Custom Sound URL") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("https://example.com/sound.mp3") },
                                    enabled = notificationsEnabled
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            if (soundEnabled && remoteSoundUrlInput.isNotBlank()) {
                                                existingReminder?.id?.let { remId ->
                                                    viewModel.fetchCustomSound(remId, remoteSoundUrlInput)
                                                }
                                            }
                                        },
                                        enabled = soundEnabled && remoteSoundUrlInput.isNotBlank() && soundFetchStatus != SoundFetchState.FETCHING && isEditing && notificationsEnabled,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when (soundFetchStatus) {
                                                SoundFetchState.FETCHED -> MaterialTheme.colorScheme.tertiary
                                                SoundFetchState.ERROR -> MaterialTheme.colorScheme.errorContainer
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                    ) {
                                        Text(
                                            when (soundFetchStatus) {
                                                SoundFetchState.FETCHING -> "Fetching..."
                                                SoundFetchState.FETCHED -> "Re-fetch"
                                                SoundFetchState.ERROR -> "Retry Fetch"
                                                else -> "Download Sound"
                                            }
                                        )
                                    }
                                    
                                    when (soundFetchStatus) {
                                        SoundFetchState.FETCHING -> {
                                            if (soundDownloadProgress != null) {
                                                // Determinate progress
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    CircularProgressIndicator(
                                                        progress = { soundDownloadProgress / 100f },
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text("${soundDownloadProgress}%", style = MaterialTheme.typography.bodySmall)
                                                }
                                            } else {
                                                // Indeterminate progress
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            }
                                        }
                                        SoundFetchState.FETCHED -> Text("✅ Sound Ready", color = MaterialTheme.colorScheme.tertiary)
                                        SoundFetchState.ERROR -> Text("⚠️ Download Failed", color = MaterialTheme.colorScheme.error)
                                        SoundFetchState.IDLE -> if (remoteSoundUrlInput.isNotBlank() && soundEnabled && isEditing) 
                                                                   Text("Pending download") 
                                                               else Text("")
                                    }
                                }
                                
                                if (soundFetchStatus == SoundFetchState.FETCHED && actualLocalSoundUri != null) {
                                    Text(
                                        "Sound file: ${actualLocalSoundUri.takeLast(20)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Divider()
                        // Vibration Settings
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("Enable Vibration", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Switch(checked = vibrateEnabled, onCheckedChange = { vibrateEnabled = it }, enabled = notificationsEnabled)
                        }
                        Divider()
                        // Advanced Notification Settings
                        AdvanceNotificationSelector(
                            selectedMinutes = selectedAdvanceMinutes,
                            onMinutesSelected = { selectedAdvanceMinutes = it }
                        )
                        Divider()
                        // Repeat notification settings
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Repeat Notification", style = MaterialTheme.typography.bodyMedium)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = repeatCount,
                                    onValueChange = { 
                                        if (it.isEmpty() || it.toIntOrNull() != null) {
                                            repeatCount = it
                                        }
                                    },
                                    label = { Text("Times") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    enabled = notificationsEnabled
                                )
                                
                                OutlinedTextField(
                                    value = repeatInterval,
                                    onValueChange = { 
                                        if (it.isEmpty() || it.toIntOrNull() != null) {
                                            repeatInterval = it
                                        }
                                    },
                                    label = { Text("Interval (min)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    enabled = notificationsEnabled
                                )
                            }
                            
                            Text(
                                text = "Reminder will repeat ${repeatCount.toIntOrNull() ?: 0} times, every ${repeatInterval.toIntOrNull() ?: 5} minutes if not dismissed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Delete button (for editing only)
            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        existingReminder?.let {
                            viewModel.deleteReminder(it)
                            navController.popBackStack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Reminder")
                }
            }
        }
    }
}
