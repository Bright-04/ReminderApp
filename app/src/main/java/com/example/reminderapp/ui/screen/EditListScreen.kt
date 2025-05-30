package com.example.reminderapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.reminderapp.data.model.ReminderList
import com.example.reminderapp.ui.viewmodel.ReminderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListScreen(
    navController: NavController,
    viewModel: ReminderViewModel,
    listId: String?
) {
    val isEditing = listId != null
    val existingList = if (isEditing) viewModel.getReminderList(listId!!) else null

    var listName by remember { mutableStateOf(existingList?.name ?: "") }

    val isLoading = isEditing && existingList == null

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit List" else "Add New List") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (listName.isNotBlank()) {
                            if (isEditing && existingList != null) {
                                viewModel.updateReminderList(existingList.copy(name = listName))
                            } else {
                                viewModel.addReminderList(listName)
                            }
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Filled.Done, "Save List")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = listName,
                onValueChange = { listName = it },
                label = { Text("List Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }
    }
}
