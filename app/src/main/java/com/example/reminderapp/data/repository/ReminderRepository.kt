package com.example.reminderapp.data.repository

import android.content.Context
import com.example.reminderapp.data.ReminderDatabase
import com.example.reminderapp.data.model.Reminder
import com.example.reminderapp.data.model.ReminderList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderRepository @Inject constructor(context: Context) : IReminderRepository {
    private val db = ReminderDatabase.getDatabase(context)
    private val reminderDao = db.reminderDao()
    private val reminderListDao = db.reminderListDao()
    
    override fun getRemindersForList(listId: String): Flow<List<Reminder>> = reminderDao.getRemindersForList(listId)
    override suspend fun getReminderById(id: String): Reminder? = reminderDao.getReminderById(id)
    override suspend fun insertReminder(reminder: Reminder) = reminderDao.insert(reminder)
    override suspend fun updateReminder(reminder: Reminder) = reminderDao.update(reminder)
    override suspend fun deleteReminder(reminder: Reminder) = reminderDao.delete(reminder)
    override suspend fun deleteReminderById(id: String) = reminderDao.deleteById(id)

    override fun getAllLists(): Flow<List<ReminderList>> = reminderListDao.getAllLists()
    override suspend fun getListById(id: String): ReminderList? = reminderListDao.getListById(id)
    override suspend fun insertList(list: ReminderList) = reminderListDao.insert(list)
    override suspend fun updateList(list: ReminderList) = reminderListDao.update(list)
    override suspend fun deleteList(list: ReminderList) = reminderListDao.delete(list)
    override suspend fun deleteListById(id: String) = reminderListDao.deleteById(id)
}
