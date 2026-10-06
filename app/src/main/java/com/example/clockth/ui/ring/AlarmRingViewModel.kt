package com.example.clockth.ui.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.clockth.data.AlarmRepository
import com.example.clockth.data.FireKind
import com.example.clockth.domain.MathChallenge
import com.example.clockth.domain.MathQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmRingViewModel(
    private val repository: AlarmRepository,
    private val challenge: MathChallenge = MathChallenge(),
) : ViewModel() {
    private val _question = MutableStateFlow(challenge.next())
    val question: StateFlow<MathQuestion> = _question.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _wrong = MutableStateFlow(false)
    val wrong: StateFlow<Boolean> = _wrong.asStateFlow()

    private val _finished = MutableStateFlow(false)
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private var busy = false

    fun onDigit(digit: Int) {
        if (busy || _input.value.length >= 4) return
        _wrong.value = false
        _input.value += digit.toString()
    }

    fun onBackspace() {
        if (busy) return
        _input.value = _input.value.dropLast(1)
    }

    fun submit(alarmId: Long, kind: FireKind) {
        if (busy) return
        val typed = _input.value.toIntOrNull()
        if (typed == _question.value.answer) {
            busy = true
            viewModelScope.launch {
                repository.dismiss(alarmId, kind)
                _finished.value = true
            }
        } else {
            _wrong.value = true
            _input.value = ""
            _question.value = challenge.next()
        }
    }

    fun snooze(alarmId: Long) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            repository.snooze(alarmId)
            _finished.value = true
        }
    }

    companion object {
        fun factory(repository: AlarmRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlarmRingViewModel(repository) as T
                }
            }
        }
    }
}
