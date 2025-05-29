package com.example.reminderapp.di

import android.content.Context
import com.example.reminderapp.data.repository.IReminderRepository
import com.example.reminderapp.data.repository.ReminderRepository
import com.example.reminderapp.util.AlarmScheduler
import com.example.reminderapp.util.IAlarmScheduler
import com.example.reminderapp.util.INotificationHelper
import com.example.reminderapp.util.NotificationHelperImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideReminderRepository(@ApplicationContext context: Context): IReminderRepository {
        return ReminderRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideAlarmScheduler(@ApplicationContext context: Context): IAlarmScheduler {
        return AlarmScheduler(context)
    }
    
    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context,
        alarmScheduler: IAlarmScheduler
    ): INotificationHelper {
        return NotificationHelperImpl(context, alarmScheduler)
    }
}
