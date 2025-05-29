package com.example.reminderapp.util

import com.example.reminderapp.data.model.Reminder

interface IAlarmScheduler {
    fun schedule(reminder: Reminder)
    fun scheduleRepeat(reminder: Reminder, remainingRepeats: Int)
    fun cancel(reminder: Reminder)
}
