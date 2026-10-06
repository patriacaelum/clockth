package com.example.clockth

import android.app.Application
import com.example.clockth.alarm.AlarmNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ClockthApp : Application() {
    lateinit var container: AppContainer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AlarmNotifications.ensureChannel(this)
        applicationScope.launch {
            container.repository.rescheduleAll()
        }
    }
}
