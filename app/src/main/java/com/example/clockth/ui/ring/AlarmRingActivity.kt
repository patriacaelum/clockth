package com.example.clockth.ui.ring

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.clockth.ClockthApp
import com.example.clockth.alarm.AlarmIntents
import com.example.clockth.alarm.AlarmRingingService
import com.example.clockth.data.Alarm
import com.example.clockth.data.FireKind
import com.example.clockth.ui.theme.ClockthTheme

class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOn()
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() = Unit
            },
        )

        val alarmId = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1L)
        val kind = runCatching {
            FireKind.valueOf(
                intent.getStringExtra(AlarmIntents.EXTRA_KIND) ?: FireKind.ALARM.name,
            )
        }.getOrDefault(FireKind.ALARM)
        if (alarmId < 0L) {
            finish()
            return
        }

        val repo = (application as ClockthApp).container.repository
        enableEdgeToEdge()
        setContent {
            ClockthTheme(forceDark = true) {
                val session by AlarmRingingService.ringing.collectAsStateWithLifecycle()
                var loaded by remember { mutableStateOf<Alarm?>(null) }
                LaunchedEffect(alarmId) {
                    loaded = repo.get(alarmId)
                }
                val ringViewModel: AlarmRingViewModel = viewModel(
                    factory = AlarmRingViewModel.factory(repo),
                )
                val finished by ringViewModel.finished.collectAsStateWithLifecycle()
                LaunchedEffect(finished) {
                    if (finished) {
                        AlarmRingingService.stop(this@AlarmRingActivity)
                        finish()
                    }
                }
                val alarm = session?.alarm ?: loaded
                val fireKind = session?.kind ?: kind
                if (alarm != null) {
                    AlarmRingScreen(
                        alarm = alarm,
                        kind = fireKind,
                        viewModel = ringViewModel,
                    )
                }
            }
        }
    }

    private fun turnScreenOn() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(KeyguardManager::class.java)
                .requestDismissKeyguard(this, null)
        }
    }

    companion object {
        fun intent(context: Context, alarmId: Long, kind: FireKind): Intent {
            return Intent(context, AlarmRingActivity::class.java).apply {
                putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmIntents.EXTRA_KIND, kind.name)
            }
        }
    }
}
