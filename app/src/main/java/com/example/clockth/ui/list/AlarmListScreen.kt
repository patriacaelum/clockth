package com.example.clockth.ui.list

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clockth.R
import com.example.clockth.alarm.AlarmScheduler
import com.example.clockth.data.Alarm
import com.example.clockth.data.DaysOfWeek
import com.example.clockth.data.FireKind
import com.example.clockth.domain.AlarmTimeFormatter
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmListScreen(
    viewModel: AlarmListViewModel,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val nextEvents by viewModel.nextEvents.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_alarm))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = AlarmTimeFormatter.formatLiveClock(context, now),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1).sp,
                )
                Text(
                    text = AlarmTimeFormatter.formatLiveDate(context, now),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NextEventBanner(
                    alarm = nextEvents.nextAlarm,
                    timeMillis = nextEvents.nextAlarm?.upcomingAlarmMillis,
                    labelRes = R.string.next_alarm,
                    showCancel = nextEvents.nextAlarm?.canCancelNextAlarm(now) == true,
                    onCancel = { alarm -> viewModel.cancelNextAlarm(alarm.id) },
                )
                NextEventBanner(
                    alarm = nextEvents.nextWakeCheck,
                    timeMillis = nextEvents.nextWakeCheck?.upcomingWakeCheckMillis,
                    labelRes = R.string.next_wake_check,
                    onCancel = { alarm -> viewModel.cancelWakeCheck(alarm.id) },
                )
                Spacer(Modifier.height(8.dp))
                PermissionBanners()
            }

            if (alarms.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.empty_alarms_title),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.empty_alarms_body),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        nowMillis = now,
                        onToggle = { viewModel.setEnabled(alarm.id, it) },
                        onClick = { onEdit(alarm.id) },
                        onCancelNextAlarm = { viewModel.cancelNextAlarm(alarm.id) },
                        onCancelWakeCheck = { viewModel.cancelWakeCheck(alarm.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun NextEventBanner(
    alarm: Alarm?,
    timeMillis: Long?,
    labelRes: Int,
    showCancel: Boolean = true,
    onCancel: (Alarm) -> Unit,
) {
    if (alarm == null || timeMillis == null) return
    val context = LocalContext.current
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(
                labelRes,
                AlarmTimeFormatter.formatDateTime(context, timeMillis),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (showCancel) {
            TextButton(onClick = { onCancel(alarm) }) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    nowMillis: Long,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onCancelNextAlarm: () -> Unit,
    onCancelWakeCheck: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = AlarmTimeFormatter.formatClock(context, alarm.hour, alarm.minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (alarm.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                val subtitle = buildList {
                    if (alarm.label.isNotBlank()) add(alarm.label)
                    add(DaysOfWeek.summary(alarm.repeatDays))
                }.joinToString(" · ")
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val pending = alarm.pendingAtEpochMillis
                if (alarm.enabled && pending != null) {
                    val pendingText = when (alarm.pendingKind) {
                        FireKind.SNOOZE -> stringResource(
                            R.string.pending_snooze,
                            AlarmTimeFormatter.formatInstant(context, pending),
                        )

                        FireKind.WAKE_CHECK -> stringResource(
                            R.string.pending_wake_check,
                            AlarmTimeFormatter.formatInstant(context, pending),
                        )

                        else -> null
                    }
                    if (pendingText != null) {
                        Text(
                            text = pendingText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                if (alarm.enabled) {
                    val canCancelNext = alarm.canCancelNextAlarm(nowMillis)
                    val canCancelWakeCheck = alarm.pendingKind == FireKind.WAKE_CHECK
                    if (canCancelNext || canCancelWakeCheck) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (canCancelNext) {
                                TextButton(onClick = onCancelNextAlarm) {
                                    Text(
                                        stringResource(
                                            when {
                                                alarm.pendingKind == FireKind.SNOOZE -> R.string.cancel_snooze
                                                alarm.isOneTime -> R.string.cancel_next_alarm
                                                else -> R.string.skip_next_alarm
                                            },
                                        ),
                                    )
                                }
                            }
                            if (canCancelWakeCheck) {
                                TextButton(onClick = onCancelWakeCheck) {
                                    Text(stringResource(R.string.cancel_wake_check_action))
                                }
                            }
                        }
                    }
                }
            }
            Switch(
                checked = alarm.enabled,
                onCheckedChange = onToggle,
            )
        }
    }
}

@Composable
private fun PermissionBanners() {
    val context = LocalContext.current
    var notifyGranted by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var exactGranted by remember {
        mutableStateOf(AlarmScheduler(context).canScheduleExactAlarms())
    }
    var fullScreenGranted by remember {
        mutableStateOf(canUseFullScreen(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notifyGranted = hasNotificationPermission(context)
            exactGranted = AlarmScheduler(context).canScheduleExactAlarms()
            fullScreenGranted = canUseFullScreen(context)
        }
    }

    val notifyLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> notifyGranted = granted }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifyGranted) {
        PermissionCard(
            message = stringResource(R.string.permission_notifications),
            onGrant = {
                notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            },
        )
    }
    if (!exactGranted) {
        PermissionCard(
            message = stringResource(R.string.permission_exact),
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:${context.packageName}")
                        },
                    )
                }
                exactGranted = AlarmScheduler(context).canScheduleExactAlarms()
            },
        )
    }
    if (!fullScreenGranted) {
        PermissionCard(
            message = stringResource(R.string.permission_fullscreen),
            onGrant = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                            data = Uri.parse("package:${context.packageName}")
                        },
                    )
                }
                fullScreenGranted = canUseFullScreen(context)
            },
        )
    }
}

@Composable
private fun PermissionCard(message: String, onGrant: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onGrant) {
                Text(stringResource(R.string.grant))
            }
        }
    }
}

private fun hasNotificationPermission(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun canUseFullScreen(context: android.content.Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
    val manager = context.getSystemService(NotificationManager::class.java)
    return manager.canUseFullScreenIntent()
}
