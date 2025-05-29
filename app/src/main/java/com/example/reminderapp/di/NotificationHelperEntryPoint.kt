package com.example.reminderapp.di

import android.content.Context
import com.example.reminderapp.util.INotificationHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationHelperEntryPoint {
    fun getNotificationHelper(): INotificationHelper
}

object Injector {
    fun getNotificationHelper(context: Context): INotificationHelper {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            NotificationHelperEntryPoint::class.java
        )
        return entryPoint.getNotificationHelper()
    }
}
