package com.example.clockth.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import androidx.core.app.ServiceCompat
import com.example.clockth.ClockthApp
import com.example.clockth.data.Alarm
import com.example.clockth.data.FireKind
import com.example.clockth.domain.VolumeFade
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmRingingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var player: MediaPlayer? = null
    private var fadeJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var vibrator: Vibrator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        AlarmNotifications.ensureChannel(this)
        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "clockth:ringing").apply {
            setReferenceCounted(false)
            acquire(30 * 60 * 1000L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1L) ?: -1L
        val kind = runCatching {
            FireKind.valueOf(
                intent?.getStringExtra(AlarmIntents.EXTRA_KIND) ?: FireKind.ALARM.name,
            )
        }.getOrDefault(FireKind.ALARM)
        if (alarmId < 0) {
            stopEverything()
            return START_NOT_STICKY
        }

        val placeholder = AlarmNotifications.ringing(
            this,
            Alarm(
                id = alarmId,
                hour = 0,
                minute = 0,
                label = getString(com.example.clockth.R.string.default_alarm_label)
            ),
            kind,
        )
        startAsForeground(placeholder)

        scope.launch {
            val repo = (application as ClockthApp).container.repository
            val alarm = repo.get(alarmId)
            if (alarm == null || !alarm.enabled) {
                stopEverything()
                return@launch
            }
            session.value = RingingSession(alarm, kind)
            startAsForeground(AlarmNotifications.ringing(this@AlarmRingingService, alarm, kind))
            withContext(Dispatchers.IO) {
                startSoundAndVibrate(alarm)
            }
            val ringIntent = com.example.clockth.ui.ring.AlarmRingActivity.intent(
                this@AlarmRingingService,
                alarm.id,
                kind,
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { startActivity(ringIntent) }
        }
        return START_STICKY
    }

    private fun startAsForeground(notification: android.app.Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            AlarmNotifications.RINGING_ID,
            notification,
            type,
        )
    }

    private fun startSoundAndVibrate(alarm: Alarm) {
        stopSoundAndVibrate()
        val uri = AlarmSounds.resolve(this, alarm.soundUri)
        val fadeMs = alarm.fadeInSeconds.coerceIn(0, 120) * 1_000L
        try {
            val mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@AlarmRingingService, uri)
                isLooping = true
                prepare()
                val initial = VolumeFade.volumeAt(0L, fadeMs)
                setVolume(initial, initial)
                start()
            }
            player = mediaPlayer
            if (fadeMs > 0L) {
                fadeJob = scope.launch {
                    val startedAt = SystemClock.elapsedRealtime()
                    while (isActive) {
                        val elapsed = SystemClock.elapsedRealtime() - startedAt
                        val volume = VolumeFade.volumeAt(elapsed, fadeMs)
                        mediaPlayer.setVolume(volume, volume)
                        if (volume >= 1f) break
                        delay(200)
                    }
                }
            }
        } catch (_: Exception) {
            player?.release()
            player = null
        }
        if (alarm.vibrate) {
            vibrator = vibrator()
            val pattern = longArrayOf(0, 600, 400, 600, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }
    }

    private fun vibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun stopSoundAndVibrate() {
        fadeJob?.cancel()
        fadeJob = null
        player?.run {
            runCatching { if (isPlaying) stop() }
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun stopEverything() {
        session.value = null
        stopSoundAndVibrate()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        session.value = null
        stopSoundAndVibrate()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private val session = MutableStateFlow<RingingSession?>(null)
        val ringing: StateFlow<RingingSession?> = session.asStateFlow()

        fun start(context: Context, alarmId: Long, kind: FireKind) {
            val intent = Intent(context, AlarmRingingService::class.java).apply {
                putExtra(AlarmIntents.EXTRA_ALARM_ID, alarmId)
                putExtra(AlarmIntents.EXTRA_KIND, kind.name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmRingingService::class.java))
        }
    }
}

data class RingingSession(
    val alarm: Alarm,
    val kind: FireKind,
)
