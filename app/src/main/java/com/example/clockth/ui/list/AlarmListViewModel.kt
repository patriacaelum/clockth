package com.example.clockth.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.clockth.data.Alarm
import com.example.clockth.data.AlarmRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repository: AlarmRepository,
) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = repository.alarms

    val nextEvents: StateFlow<NextEvents> = repository.alarms
        .map { list -> NextEvents.from(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NextEvents())

    fun setEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun cancelNextAlarm(id: Long) {
        viewModelScope.launch { repository.cancelNextAlarm(id) }
    }

    fun cancelWakeCheck(id: Long) {
        viewModelScope.launch { repository.cancelWakeCheck(id) }
    }

    companion object {
        fun factory(repository: AlarmRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlarmListViewModel(repository) as T
                }
            }
        }
    }
}

data class NextEvents(
    val nextAlarm: Alarm? = null,
    val nextWakeCheck: Alarm? = null,
) {
    companion object {
        fun from(alarms: List<Alarm>): NextEvents {
            val enabled = alarms.filter { it.enabled }
            return NextEvents(
                nextAlarm = enabled
                    .filter { it.upcomingAlarmMillis != null }
                    .minByOrNull { it.upcomingAlarmMillis!! },
                nextWakeCheck = enabled
                    .filter { it.upcomingWakeCheckMillis != null }
                    .minByOrNull { it.upcomingWakeCheckMillis!! },
            )
        }
    }
}
