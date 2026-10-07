package com.example.clockth.data

import com.example.clockth.domain.NextAlarmCalculator
import kotlinx.coroutines.flow.StateFlow

interface AlarmSchedulerPort {
    fun schedule(alarmId: Long, triggerAtEpochMillis: Long, kind: FireKind)
    fun cancel(alarmId: Long, kind: FireKind)
    fun cancelAll(alarmId: Long)
}

class AlarmRepository(
    private val store: AlarmStorePort,
    private val scheduler: AlarmSchedulerPort,
    private val nowMillis: () -> Long = { System.currentTimeMillis() },
) {
    val alarms: StateFlow<List<Alarm>> = store.alarms

    suspend fun get(id: Long): Alarm? = store.get(id)

    suspend fun latestCreated(): Alarm? = store.getAll().maxByOrNull { it.id }

    suspend fun save(draft: Alarm): Alarm {
        val persisted = store.upsert(draft)
        scheduler.cancelAll(persisted.id)
        if (!persisted.enabled) {
            return store.upsert(
                persisted.copy(
                    pendingKind = FireKind.NONE,
                    pendingAtEpochMillis = null,
                    nextOccurrenceEpochMillis = null,
                ),
            )
        }
        val trigger = nextOccurrence(persisted, nowMillis())
        val scheduled = persisted.copy(
            pendingKind = FireKind.ALARM,
            pendingAtEpochMillis = trigger,
            nextOccurrenceEpochMillis = trigger,
        )
        store.upsert(scheduled)
        scheduler.schedule(scheduled.id, trigger, FireKind.ALARM)
        return scheduled
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) {
        val alarm = store.get(id) ?: return
        save(alarm.copy(enabled = enabled))
    }

    suspend fun delete(id: Long) {
        scheduler.cancelAll(id)
        store.delete(id)
    }

    suspend fun snooze(id: Long) {
        val alarm = store.get(id) ?: return
        val trigger = nowMillis() + alarm.snoozeMinutes * 60_000L
        scheduler.cancel(id, FireKind.SNOOZE)
        scheduler.cancel(id, FireKind.WAKE_CHECK)
        val updated = alarm.copy(
            enabled = true,
            pendingKind = FireKind.SNOOZE,
            pendingAtEpochMillis = trigger,
            nextOccurrenceEpochMillis = if (alarm.isOneTime) {
                null
            } else {
                nextOccurrence(alarm, nowMillis())
            },
        )
        store.upsert(updated)
        scheduler.schedule(id, trigger, FireKind.SNOOZE)
    }

    suspend fun dismiss(id: Long, fromKind: FireKind) {
        val alarm = store.get(id) ?: return
        scheduler.cancel(id, FireKind.SNOOZE)
        scheduler.cancel(id, fromKind)

        val needsWakeCheck = alarm.wakeCheckEnabled && fromKind != FireKind.WAKE_CHECK
        if (needsWakeCheck) {
            val wakeAt = nowMillis() + alarm.wakeCheckMinutes * 60_000L
            val next = if (alarm.isOneTime) null else nextOccurrence(alarm, nowMillis())
            if (next != null) {
                scheduler.schedule(id, next, FireKind.ALARM)
            }
            store.upsert(
                alarm.copy(
                    enabled = true,
                    pendingKind = FireKind.WAKE_CHECK,
                    pendingAtEpochMillis = wakeAt,
                    nextOccurrenceEpochMillis = next,
                ),
            )
            scheduler.schedule(id, wakeAt, FireKind.WAKE_CHECK)
            return
        }

        if (alarm.isOneTime) {
            scheduler.cancelAll(id)
            store.delete(id)
            return
        }

        val next = nextOccurrence(alarm, nowMillis())
        store.upsert(
            alarm.copy(
                pendingKind = FireKind.ALARM,
                pendingAtEpochMillis = next,
                nextOccurrenceEpochMillis = next,
            ),
        )
        scheduler.schedule(id, next, FireKind.ALARM)
    }

    suspend fun cancelNextAlarm(id: Long) {
        val alarm = store.get(id) ?: return
        if (!alarm.canCancelNextAlarm(nowMillis())) return
        when (alarm.pendingKind) {
            FireKind.SNOOZE -> {
                scheduler.cancel(id, FireKind.SNOOZE)
                if (alarm.isOneTime) {
                    save(alarm.copy(enabled = false))
                    return
                }
                val next = alarm.nextOccurrenceEpochMillis ?: nextOccurrence(alarm, nowMillis())
                store.upsert(
                    alarm.copy(
                        pendingKind = FireKind.ALARM,
                        pendingAtEpochMillis = next,
                        nextOccurrenceEpochMillis = next,
                    ),
                )
                scheduler.schedule(id, next, FireKind.ALARM)
            }

            FireKind.WAKE_CHECK -> {
                if (alarm.isOneTime || alarm.nextOccurrenceEpochMillis == null) return
                val skipped = nextOccurrence(alarm, alarm.nextOccurrenceEpochMillis)
                scheduler.cancel(id, FireKind.ALARM)
                store.upsert(alarm.copy(nextOccurrenceEpochMillis = skipped))
                scheduler.schedule(id, skipped, FireKind.ALARM)
            }

            FireKind.ALARM -> {
                scheduler.cancel(id, FireKind.ALARM)
                if (alarm.isOneTime) {
                    save(alarm.copy(enabled = false))
                    return
                }
                val current = alarm.pendingAtEpochMillis ?: nextOccurrence(alarm, nowMillis())
                val skipped = nextOccurrence(alarm, current)
                store.upsert(
                    alarm.copy(
                        pendingKind = FireKind.ALARM,
                        pendingAtEpochMillis = skipped,
                        nextOccurrenceEpochMillis = skipped,
                    ),
                )
                scheduler.schedule(id, skipped, FireKind.ALARM)
            }

            FireKind.NONE -> Unit
        }
    }

    suspend fun cancelWakeCheck(id: Long) {
        val alarm = store.get(id) ?: return
        if (alarm.pendingKind != FireKind.WAKE_CHECK) return
        scheduler.cancel(id, FireKind.WAKE_CHECK)
        if (alarm.isOneTime) {
            scheduler.cancelAll(id)
            store.delete(id)
            return
        }
        val next = alarm.nextOccurrenceEpochMillis ?: nextOccurrence(alarm, nowMillis())
        store.upsert(
            alarm.copy(
                pendingKind = FireKind.ALARM,
                pendingAtEpochMillis = next,
                nextOccurrenceEpochMillis = next,
            ),
        )
        scheduler.schedule(id, next, FireKind.ALARM)
    }

    suspend fun rescheduleAll() {
        val now = nowMillis()
        store.getAll().forEach { alarm ->
            if (!alarm.enabled) {
                scheduler.cancelAll(alarm.id)
                if (alarm.pendingKind != FireKind.NONE ||
                    alarm.pendingAtEpochMillis != null ||
                    alarm.nextOccurrenceEpochMillis != null
                ) {
                    store.upsert(
                        alarm.copy(
                            pendingKind = FireKind.NONE,
                            pendingAtEpochMillis = null,
                            nextOccurrenceEpochMillis = null,
                        ),
                    )
                }
                return@forEach
            }

            val pending = alarm.pendingAtEpochMillis
            val kind = alarm.pendingKind
            if (pending != null && pending > now && kind != FireKind.NONE) {
                scheduler.schedule(alarm.id, pending, kind)
                if (kind == FireKind.WAKE_CHECK && !alarm.isOneTime) {
                    val next = alarm.nextOccurrenceEpochMillis
                        ?.takeIf { it > now }
                        ?: nextOccurrence(alarm, now)
                    if (next != alarm.nextOccurrenceEpochMillis) {
                        store.upsert(alarm.copy(nextOccurrenceEpochMillis = next))
                    }
                    scheduler.schedule(alarm.id, next, FireKind.ALARM)
                }
            } else {
                val next = nextOccurrence(alarm, now)
                store.upsert(
                    alarm.copy(
                        pendingKind = FireKind.ALARM,
                        pendingAtEpochMillis = next,
                        nextOccurrenceEpochMillis = next,
                    ),
                )
                scheduler.cancelAll(alarm.id)
                scheduler.schedule(alarm.id, next, FireKind.ALARM)
            }
        }
    }

    private fun nextOccurrence(alarm: Alarm, afterMillis: Long): Long {
        return NextAlarmCalculator.nextTriggerMillis(
            nowMillis = afterMillis,
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDays = alarm.repeatDays,
        )
    }
}
