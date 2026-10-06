package com.example.clockth

import android.content.Context
import com.example.clockth.alarm.AlarmScheduler
import com.example.clockth.data.AlarmRepository
import com.example.clockth.data.AlarmStore

class AppContainer(context: Context) {
    val store = AlarmStore(context)
    val scheduler = AlarmScheduler(context)
    val repository = AlarmRepository(store, scheduler)
}
