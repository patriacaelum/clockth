package com.example.clockth.ui.ring

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.clockth.R
import com.example.clockth.data.Alarm
import com.example.clockth.data.FireKind
import com.example.clockth.domain.AlarmTimeFormatter
import com.example.clockth.ui.theme.Amber
import com.example.clockth.ui.theme.Cream
import com.example.clockth.ui.theme.Danger
import com.example.clockth.ui.theme.Navy
import com.example.clockth.ui.theme.NavyCard

@Composable
fun AlarmRingScreen(
    alarm: Alarm,
    kind: FireKind,
    viewModel: AlarmRingViewModel,
) {
    val question by viewModel.question.collectAsStateWithLifecycle()
    val input by viewModel.input.collectAsStateWithLifecycle()
    val wrong by viewModel.wrong.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val promptColor by animateColorAsState(
        targetValue = if (wrong) Danger else Cream,
        label = "prompt",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = AlarmTimeFormatter.formatClock(context, alarm.hour, alarm.minute),
            color = Cream,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
        )
        val heading = when (kind) {
            FireKind.WAKE_CHECK -> stringResource(R.string.still_awake)
            else -> alarm.label.ifBlank { stringResource(R.string.default_alarm_label) }
        }
        Text(
            text = heading,
            color = Amber,
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = if (wrong) {
                stringResource(R.string.wrong_answer)
            } else {
                stringResource(R.string.solve_to_dismiss)
            },
            color = promptColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = question.prompt,
            color = Cream,
            fontSize = 42.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = input.ifEmpty { " " },
            color = Amber,
            fontSize = 36.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.weight(1f))

        Keypad(
            onDigit = viewModel::onDigit,
            onBackspace = viewModel::onBackspace,
            onSubmit = { viewModel.submit(alarm.id, kind) },
        )

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { viewModel.snooze(alarm.id) }) {
            Text(
                text = stringResource(R.string.snooze_action, alarm.snoozeMinutes),
                color = Amber,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun Keypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "✓"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { label ->
                    val modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1.4f)
                    when (label) {
                        "⌫" -> Key(
                            label = label,
                            modifier = modifier.semantics {
                                contentDescription = "Delete digit"
                            },
                            onClick = onBackspace,
                        )

                        "✓" -> Key(
                            label = label,
                            modifier = modifier.semantics {
                                contentDescription = "Submit answer"
                            },
                            emphasized = true,
                            onClick = onSubmit,
                        )

                        else -> Key(
                            label = label,
                            modifier = modifier,
                            onClick = { onDigit(label.toInt()) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(
    label: String,
    modifier: Modifier,
    emphasized: Boolean = false,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.clip(CircleShape),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (emphasized) Amber else NavyCard,
            contentColor = if (emphasized) Navy else Cream,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 24.sp, fontWeight = FontWeight.Medium)
        }
    }
}
