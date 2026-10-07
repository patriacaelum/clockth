package com.example.clockth.ui.editor

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clockth.R
import com.example.clockth.alarm.AlarmSounds
import com.example.clockth.data.DaysOfWeek
import com.example.clockth.domain.AlarmTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmEditorScreen(
    viewModel: AlarmEditorViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    val soundPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val picked = result.data.pickedRingtoneUri()
        viewModel.update { it.copy(soundUri = picked?.toString()) }
    }

    LaunchedEffect(viewModel) {
        viewModel.close.collect { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isNew) stringResource(R.string.add_alarm)
                        else stringResource(R.string.app_name),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = viewModel::delete) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.delete_alarm),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = AlarmTimeFormatter.formatClock(context, state.hour, state.minute),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Light,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .clickable { showPicker = true }
                    .padding(vertical = 8.dp),
            )
            TextButton(onClick = { showPicker = true }) {
                Text(stringResource(R.string.change_time))
            }
            if (state.isNew) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val shortcuts = listOf(
                        30 to R.string.time_in_30_min,
                        60 to R.string.time_in_1_hour,
                        120 to R.string.time_in_2_hours,
                    )
                    shortcuts.forEach { (minutes, labelRes) ->
                        AssistChip(
                            onClick = { viewModel.setTimeFromNow(minutes) },
                            label = { Text(stringResource(labelRes)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.repeat), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                DaysOfWeek.mondayFirstCalendarDays.forEach { day ->
                    val selected = DaysOfWeek.contains(state.repeatDays, day)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            viewModel.update {
                                it.copy(repeatDays = DaysOfWeek.toggle(it.repeatDays, day))
                            }
                        },
                        label = { Text(DaysOfWeek.oneLetterLabel(day)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (state.repeatDays == DaysOfWeek.NONE) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.once_helper),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.label,
                onValueChange = { value -> viewModel.update { it.copy(label = value) } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.label)) },
                singleLine = true,
            )

            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.mathChallengeEnabled,
                        role = Role.Switch,
                        onValueChange = { value ->
                            viewModel.update { it.copy(mathChallengeEnabled = value) }
                        },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.math_challenge),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.math_challenge_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.mathChallengeEnabled,
                    onCheckedChange = null,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.snooze_minutes, state.snoozeMinutes),
                style = MaterialTheme.typography.titleMedium,
            )
            Slider(
                value = state.snoozeMinutes.toFloat(),
                onValueChange = { value ->
                    viewModel.update { it.copy(snoozeMinutes = value.toInt().coerceIn(1, 30)) }
                },
                valueRange = 1f..30f,
                steps = 28,
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.wakeCheckEnabled,
                        role = Role.Switch,
                        onValueChange = { value ->
                            viewModel.update { it.copy(wakeCheckEnabled = value) }
                        },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.wake_check),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.wake_check_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.wakeCheckEnabled,
                    onCheckedChange = null,
                )
            }
            if (state.wakeCheckEnabled) {
                Text(
                    stringResource(R.string.wake_check_minutes, state.wakeCheckMinutes),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = state.wakeCheckMinutes.toFloat(),
                    onValueChange = { value ->
                        viewModel.update {
                            it.copy(wakeCheckMinutes = value.toInt().coerceIn(1, 30))
                        }
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                )
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        soundPicker.launch(
                            ringtonePickerIntent(
                                currentUri = state.soundUri,
                                title = context.getString(R.string.choose_alarm_sound),
                            ),
                        )
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.sound),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        AlarmSounds.title(context, state.soundUri),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            val fadeEnabled = state.fadeInSeconds > 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = fadeEnabled,
                        role = Role.Switch,
                        onValueChange = { enabled ->
                            viewModel.update {
                                it.copy(fadeInSeconds = if (enabled) 30 else 0)
                            }
                        },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.volume_fade),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        stringResource(R.string.volume_fade_helper),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = fadeEnabled,
                    onCheckedChange = null,
                )
            }
            if (fadeEnabled) {
                Text(
                    stringResource(R.string.volume_fade_seconds, state.fadeInSeconds),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Slider(
                    value = state.fadeInSeconds.toFloat(),
                    onValueChange = { value ->
                        viewModel.update {
                            it.copy(fadeInSeconds = value.toInt().coerceIn(5, 120))
                        }
                    },
                    valueRange = 5f..120f,
                    steps = 22,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = state.vibrate,
                        role = Role.Switch,
                        onValueChange = { value -> viewModel.update { it.copy(vibrate = value) } },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.vibrate),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = state.vibrate, onCheckedChange = null)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.save_alarm))
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showPicker) {
        val is24 = DateFormat.is24HourFormat(context)
        val pickerState = rememberTimePickerState(state.hour, state.minute, is24)
        AlertDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.update {
                            it.copy(hour = pickerState.hour, minute = pickerState.minute)
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
            text = { TimePicker(state = pickerState) },
        )
    }
}

private fun ringtonePickerIntent(currentUri: String?, title: String): Intent {
    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
        putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
        putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            currentUri?.let(Uri::parse) ?: defaultUri,
        )
    }
}

private fun Intent?.pickedRingtoneUri(): Uri? {
    this ?: return null
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
}
