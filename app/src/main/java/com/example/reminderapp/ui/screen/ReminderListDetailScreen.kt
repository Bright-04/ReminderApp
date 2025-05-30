package com.example.reminderapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.ui.components.DueDateChip
import com.example.reminderapp.ui.components.PriorityChip
import com.example.reminderapp.ui.components.StatusIcon
import com.example.reminderapp.ui.navigation.Routes
import com.example.reminderapp.ui.viewmodel.ReminderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListDetailScreen(
    navController: NavController,
    viewModel: ReminderViewModel,
    listId: String
) {
    val list = viewModel.getReminderList(listId)
    val remindersState = viewModel.getRemindersForList(listId).collectAsStateWithLifecycle(initialValue = emptyList())
    val reminders = remindersState.value

    // Derived states to avoid flicker
    val isLoading = remember(reminders) { reminders.isEmpty() }
    val isEmpty = remember(reminders) { reminders.isEmpty() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(list?.name ?: "Reminders")
                        if (reminders.isNotEmpty()) {
                            val activeCount = reminders.count { it is com.example.reminderapp.data.model.Reminder && !it.isCompleted }
                            val completedCount = reminders.count { it is com.example.reminderapp.data.model.Reminder && it.isCompleted }
                            Text(
                                text = "$activeCount active, $completedCount completed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Routes.addReminder(listId)) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Reminder")
            }
        }
    ) { paddingValues ->
        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            isEmpty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No reminders in this list.") }
            else -> LazyColumn(
                modifier = Modifier.padding(paddingValues).fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(reminders) { reminder ->
                    ReminderRow(
                        reminder = reminder,
                        onToggleComplete = { viewModel.toggleReminderCompletion(reminder) },
                        onClick = { navController.navigate(Routes.editReminder(listId, reminder.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReminderRow(reminder: Reminder, onToggleComplete: () -> Unit, onClick: () -> Unit) {
    val alpha by animateFloatAsState(targetValue = if (reminder.isCompleted) 0.6f else 1f, label = "opacity")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isCompleted) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) 
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status icon with checkbox
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                StatusIcon(
                    isCompleted = reminder.isCompleted,
                    dueDate = reminder.dueDate
                )
                
                Checkbox(
                    checked = reminder.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.alpha(0.01f) // Make it nearly invisible but clickable
                )
            }
            
            // Content column with title, notes, and metadata
            Column(modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .alpha(alpha)
            ) {
                // Title row with priority indicator
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (reminder.priority != com.example.reminderapp.data.model.Priority.NONE) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(com.example.reminderapp.ui.components.getPriorityColor(reminder.priority))
                                .padding(end = 8.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Notes if available
                reminder.notes?.let {
                    if (it.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Chips for metadata (due date and priority)
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Due date chip
                    reminder.dueDate?.let { dueDate ->
                        DueDateChip(dueDate)
                    }
                    
                    // Priority chip if not NONE
                    if (reminder.priority != com.example.reminderapp.data.model.Priority.NONE) {
                        PriorityChip(reminder.priority)
                    }
                }
            }
        }
    }
}
