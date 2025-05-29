package com.example.reminderapp.data.repository

import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.ReminderList
import kotlinx.coroutines.flow.Flow

interface IReminderRepository {
    fun getRemindersForList(listId: String): Flow<List<Reminder>>
    suspend fun getReminderById(id: String): Reminder?
    suspend fun insertReminder(reminder: Reminder)
    suspend fun updateReminder(reminder: Reminder)
    suspend fun deleteReminder(reminder: Reminder)
    suspend fun deleteReminderById(id: String)

    fun getAllLists(): Flow<List<ReminderList>>
    suspend fun getListById(id: String): ReminderList?
    suspend fun insertList(list: ReminderList)
    suspend fun updateList(list: ReminderList)
    suspend fun deleteList(list: ReminderList)
    suspend fun deleteListById(id: String)
}
