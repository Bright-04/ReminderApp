package com.example.reminderapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.activity.viewModels
import com.example.reminderapp.ui.navigation.AppNavigation
import com.example.reminderapp.ui.theme.ReminderAppTheme
import com.example.reminderapp.ui.viewmodel.ReminderViewModel
import com.example.reminderapp.util.INotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your app.
            } else {
                // Explain to the user that the feature is unavailable because the
                // feature requires a permission that the user has denied.
                // At the same time, respect the user's decision. Don't link to
                // system settings in an effort to convince the user to change their decision.
            }
        }

    // Use Hilt to inject the ViewModel
    private val reminderViewModel: ReminderViewModel by viewModels()
    
    // Use Hilt to inject the NotificationHelper
    @Inject
    lateinit var notificationHelper: INotificationHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channels using the injected NotificationHelper
        notificationHelper.createNotificationChannel()
        requestNotificationPermission()

        setContent {
            ReminderAppTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.bgv1),
                        modifier = Modifier.fillMaxSize().alpha(0.4f),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                    // App content overlays the background
                    AppNavigation(
                        reminderViewModel = reminderViewModel,
                    )
                }
            }
        }
        handleIntent(intent) // Handle intent if app was opened from notification
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val reminderId = intent.getStringExtra("REMINDER_ID_FROM_NOTIFICATION")
        val listId = intent.getStringExtra("LIST_ID_FROM_NOTIFICATION")

        if (reminderId != null && listId != null) {
            // Here you would typically use your NavController to navigate
            // to the specific reminder detail screen.
            // This requires the NavController to be accessible here or
            // a mechanism to signal the AppNavigation composable.
            // For simplicity, we'll just log it.
            println("App opened from notification for Reminder ID: $reminderId, List ID: $listId")
            // Example: navController.navigate(Routes.editReminder(listId, reminderId))
            // Make sure AppNavigation is set up to handle this, possibly by passing
            // these IDs as arguments to its startDestination or via a SharedViewModel.
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                // In an educational UI, explain to the user why your app requires this
                // permission for a specific feature to behave as expected, and what
                // features are disabled if it's declined. In this UI, include a
                // "cancel" or "no thanks" button that lets the user continue
                // using your app without granting the permission.
                // showInContextUI(...)
                }
                else -> {
                    // You can directly ask for the permission.
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}