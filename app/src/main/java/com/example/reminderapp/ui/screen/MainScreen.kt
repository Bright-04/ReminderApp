package com.example.reminderapp.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.reminderapp.data.model.ReminderList
import com.example.reminderapp.ui.navigation.Routes
import com.example.reminderapp.ui.viewmodel.ReminderViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: ReminderViewModel) {
    val listsState = viewModel.reminderLists.collectAsStateWithLifecycle(initialValue = emptyList())
    val lists = listsState.value
    
    when {
        lists.isEmpty() -> {
            // Empty state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No lists found. Tap + to add a new list.")
            }
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { 
                            Column {
                                Text("Promptly")
                                Text(
                                    text = "${lists.size} list${if (lists.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { navController.navigate(Routes.ADD_LIST_SCREEN) }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add List")
                    }
                }
            ) { paddingValues ->
                Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
                    Text(
                        text = "My Lists",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(lists) { list ->
                            ReminderListRow(
                                list = list,
                                onClick = {
                                    navController.navigate(Routes.listDetail(list.id))
                                },
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderListRow(list: ReminderList, onClick: () -> Unit, viewModel: ReminderViewModel) {
    val countsState by viewModel.getReminderCountsForList(list.id).collectAsStateWithLifecycle(initialValue = 0 to 0)
    val activeCount = countsState.first
    val completedCount = countsState.second
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // List icon with a color accent
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "Reminder List",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            
            // List name and summary
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                val summaryText = when {
                    activeCount == 0 && completedCount == 0 -> "No reminders yet"
                    activeCount > 0 -> "$activeCount active reminder${if (activeCount > 1) "s" else ""}"
                    else -> "All reminders completed"
                }
                
                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Add indicators for reminders status
            Row {
                if (activeCount > 0 || completedCount > 0) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Completed reminders",
                        tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = completedCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Pending reminders",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = activeCount.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
