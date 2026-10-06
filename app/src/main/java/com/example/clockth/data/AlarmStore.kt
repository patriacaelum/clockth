package com.example.clockth.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

interface AlarmStorePort {
    val alarms: StateFlow<List<Alarm>>
    suspend fun getAll(): List<Alarm>
    suspend fun get(id: Long): Alarm?
    suspend fun upsert(alarm: Alarm): Alarm
    suspend fun delete(id: Long)
}

class AlarmStore(context: Context) : AlarmStorePort {
    private val file: File = File(
        context.applicationContext.createDeviceProtectedStorageContext().filesDir,
        "alarms.json",
    )
    private val mutex = Mutex()
    private var loaded = false
    private var nextId = 1L
    private val _alarms = MutableStateFlow<List<Alarm>>(emptyList())
    override val alarms: StateFlow<List<Alarm>> = _alarms.asStateFlow()

    override suspend fun getAll(): List<Alarm> = mutex.withLock {
        ensureLoadedLocked()
        _alarms.value
    }

    override suspend fun get(id: Long): Alarm? = mutex.withLock {
        ensureLoadedLocked()
        _alarms.value.firstOrNull { it.id == id }
    }

    override suspend fun upsert(alarm: Alarm): Alarm = mutex.withLock {
        ensureLoadedLocked()
        val saved = if (alarm.id == 0L) {
            alarm.copy(id = nextId++)
        } else {
            alarm
        }
        val current = _alarms.value.toMutableList()
        val index = current.indexOfFirst { it.id == saved.id }
        if (index >= 0) {
            current[index] = saved
        } else {
            current.add(saved)
        }
        current.sortWith(compareBy({ it.hour }, { it.minute }, { it.id }))
        _alarms.value = current
        persistLocked()
        saved
    }

    override suspend fun delete(id: Long) = mutex.withLock {
        ensureLoadedLocked()
        _alarms.value = _alarms.value.filterNot { it.id == id }
        persistLocked()
    }

    private fun ensureLoadedLocked() {
        if (loaded) return
        if (file.exists()) {
            val root = JSONObject(file.readText())
            nextId = root.optLong("nextId", 1L)
            val array = root.optJSONArray("alarms") ?: JSONArray()
            val items = buildList {
                for (i in 0 until array.length()) {
                    add(decode(array.getJSONObject(i)))
                }
            }.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id }))
            _alarms.value = items
            val maxId = items.maxOfOrNull { it.id } ?: 0L
            if (nextId <= maxId) nextId = maxId + 1
        }
        loaded = true
    }

    private fun persistLocked() {
        val array = JSONArray()
        _alarms.value.forEach { array.put(encode(it)) }
        val root = JSONObject()
            .put("nextId", nextId)
            .put("alarms", array)
        file.writeText(root.toString())
    }

    private fun encode(alarm: Alarm): JSONObject {
        return JSONObject()
            .put("id", alarm.id)
            .put("hour", alarm.hour)
            .put("minute", alarm.minute)
            .put("label", alarm.label)
            .put("enabled", alarm.enabled)
            .put("repeatDays", alarm.repeatDays)
            .put("snoozeMinutes", alarm.snoozeMinutes)
            .put("wakeCheckEnabled", alarm.wakeCheckEnabled)
            .put("wakeCheckMinutes", alarm.wakeCheckMinutes)
            .put("vibrate", alarm.vibrate)
            .put("mathChallengeEnabled", alarm.mathChallengeEnabled)
            .put("soundUri", alarm.soundUri ?: JSONObject.NULL)
            .put("fadeInSeconds", alarm.fadeInSeconds)
            .put("pendingKind", alarm.pendingKind.name)
            .put(
                "pendingAtEpochMillis",
                alarm.pendingAtEpochMillis ?: JSONObject.NULL,
            )
            .put(
                "nextOccurrenceEpochMillis",
                alarm.nextOccurrenceEpochMillis ?: JSONObject.NULL,
            )
    }

    private fun decode(json: JSONObject): Alarm {
        return Alarm(
            id = json.getLong("id"),
            hour = json.getInt("hour"),
            minute = json.getInt("minute"),
            label = json.optString("label"),
            enabled = json.optBoolean("enabled", true),
            repeatDays = json.optInt("repeatDays", DaysOfWeek.NONE),
            snoozeMinutes = json.optInt("snoozeMinutes", 5),
            wakeCheckEnabled = json.optBoolean("wakeCheckEnabled", true),
            wakeCheckMinutes = json.optInt("wakeCheckMinutes", 8),
            vibrate = json.optBoolean("vibrate", true),
            mathChallengeEnabled = json.optBoolean("mathChallengeEnabled", true),
            soundUri = json.optionalString("soundUri"),
            fadeInSeconds = json.optInt("fadeInSeconds", 0).coerceIn(0, 120),
            pendingKind = runCatching {
                FireKind.valueOf(json.optString("pendingKind", FireKind.NONE.name))
            }.getOrDefault(FireKind.NONE),
            pendingAtEpochMillis = json.optionalLong("pendingAtEpochMillis"),
            nextOccurrenceEpochMillis = json.optionalLong("nextOccurrenceEpochMillis"),
        )
    }

    private fun JSONObject.optionalLong(key: String): Long? {
        val raw = opt(key)
        return if (raw == null || raw == JSONObject.NULL) null else optLong(key)
    }

    private fun JSONObject.optionalString(key: String): String? {
        val raw = opt(key)
        if (raw == null || raw == JSONObject.NULL) return null
        return optString(key).takeIf { it.isNotBlank() }
    }
}
