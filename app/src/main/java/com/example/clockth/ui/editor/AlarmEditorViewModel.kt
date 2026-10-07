package com.example.clockth.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.clockth.data.Alarm
import com.example.clockth.data.AlarmRepository
import com.example.clockth.data.DaysOfWeek
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class EditorState(
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val repeatDays: Int = DaysOfWeek.NONE,
    val snoozeMinutes: Int = 5,
    val wakeCheckEnabled: Boolean = true,
    val wakeCheckMinutes: Int = 8,
    val vibrate: Boolean = true,
    val mathChallengeEnabled: Boolean = true,
    val soundUri: String? = null,
    val fadeInSeconds: Int = 0,
    val isNew: Boolean = true,
    val loaded: Boolean = false,
)

class AlarmEditorViewModel(
    private val repository: AlarmRepository,
    private val alarmId: Long?,
) : ViewModel() {
    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _close = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val close: SharedFlow<Unit> = _close.asSharedFlow()

    init {
        viewModelScope.launch {
            val existing = alarmId?.let { repository.get(it) }
            if (existing != null) {
                _state.value = EditorState(
                    hour = existing.hour,
                    minute = existing.minute,
                    label = existing.label,
                    repeatDays = existing.repeatDays,
                    snoozeMinutes = existing.snoozeMinutes,
                    wakeCheckEnabled = existing.wakeCheckEnabled,
                    wakeCheckMinutes = existing.wakeCheckMinutes,
                    vibrate = existing.vibrate,
                    mathChallengeEnabled = existing.mathChallengeEnabled,
                    soundUri = existing.soundUri,
                    fadeInSeconds = existing.fadeInSeconds,
                    isNew = false,
                    loaded = true,
                )
            } else {
                val cal = Calendar.getInstance()
                cal.add(Calendar.HOUR_OF_DAY, 1)
                _state.value = defaultNewEditorState(
                    lastCreated = repository.latestCreated(),
                    hour = cal.get(Calendar.HOUR_OF_DAY),
                    minute = 0,
                )
            }
        }
    }

    fun update(transform: (EditorState) -> EditorState) {
        _state.update(transform)
    }

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            val draft = Alarm(
                id = alarmId ?: 0L,
                hour = s.hour,
                minute = s.minute,
                label = s.label.trim(),
                enabled = true,
                repeatDays = s.repeatDays,
                snoozeMinutes = s.snoozeMinutes.coerceIn(1, 30),
                wakeCheckEnabled = s.wakeCheckEnabled,
                wakeCheckMinutes = s.wakeCheckMinutes.coerceIn(1, 30),
                vibrate = s.vibrate,
                mathChallengeEnabled = s.mathChallengeEnabled,
                soundUri = s.soundUri,
                fadeInSeconds = s.fadeInSeconds.coerceIn(0, 120),
            )
            repository.save(draft)
            _close.emit(Unit)
        }
    }

    fun delete() {
        val id = alarmId ?: return
        viewModelScope.launch {
            repository.delete(id)
            _close.emit(Unit)
        }
    }

    companion object {
        fun factory(
            repository: AlarmRepository,
            alarmId: Long?,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlarmEditorViewModel(repository, alarmId) as T
                }
            }
        }
    }
}

internal fun defaultNewEditorState(
    lastCreated: Alarm?,
    hour: Int,
    minute: Int,
): EditorState {
    if (lastCreated == null) {
        return EditorState(hour = hour, minute = minute, loaded = true)
    }
    return EditorState(
        hour = hour,
        minute = minute,
        label = lastCreated.label,
        repeatDays = lastCreated.repeatDays,
        snoozeMinutes = lastCreated.snoozeMinutes,
        wakeCheckEnabled = lastCreated.wakeCheckEnabled,
        wakeCheckMinutes = lastCreated.wakeCheckMinutes,
        vibrate = lastCreated.vibrate,
        mathChallengeEnabled = lastCreated.mathChallengeEnabled,
        soundUri = lastCreated.soundUri,
        fadeInSeconds = lastCreated.fadeInSeconds,
        loaded = true,
    )
}
