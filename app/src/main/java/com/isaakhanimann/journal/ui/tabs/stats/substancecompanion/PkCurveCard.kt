// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.CardWithTitle
import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PkCurveCard(
    pkDoses: PkDoses,
    halfLifeHours: Double?,
    now: Instant,
    accentColor: Color,
    onSaveHalfLifeHours: (Double) -> Unit
) {
    var enteredHalfLife by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(halfLifeHours) {
        if (halfLifeHours != null) enteredHalfLife = halfLifeHours.toString()
    }
    val parsedHalfLife = enteredHalfLife.replace(',', '.').toDoubleOrNull()
        ?.takeIf { it.isFinite() && it in 0.1..10000.0 }
    val recorded = remember(pkDoses, now) { pkDoses.recordedAt(now) }
    val pastDoses = recorded.doses
    val excludedCount = recorded.excludedCount
    val points by produceState<List<PkPoint>>(emptyList(), recorded, halfLifeHours, now) {
        value = if (halfLifeHours == null) emptyList() else withContext(Dispatchers.Default) {
            PkCurve.series(
                pastDoses,
                now.minus(Duration.ofDays(30)),
                now.plus(Duration.ofDays(7)),
                halfLifeHours
            )
        }
    }

    CardWithTitle(title = i18n("pk_curve_title"), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = enteredHalfLife,
                    onValueChange = { enteredHalfLife = it },
                    label = { Text(i18n("pk_half_life_hours")) },
                    isError = enteredHalfLife.isNotBlank() && parsedHalfLife == null,
                    supportingText = { Text(i18n("pk_half_life_range")) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { parsedHalfLife?.let(onSaveHalfLifeHours) },
                    enabled = parsedHalfLife != null,
                    modifier = Modifier.padding(top = 8.dp)
                ) { Text(i18n("pk_apply")) }
            }
            Text(i18n("pk_model_note"), style = MaterialTheme.typography.bodySmall)
            if (excludedCount > 0) {
                Text(
                    i18n("pk_excluded", mapOf("count" to excludedCount.toString())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (halfLifeHours != null && pastDoses.isNotEmpty()) {
                val current = PkCurve.remaining(pastDoses, now, halfLifeHours)
                Text(
                    i18n("pk_now_amount", mapOf("amount" to String.format(Locale.ROOT, "%.4g", current))),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (points.isNotEmpty()) {
                    Text(
                        i18n("pk_axis_range", mapOf("maximum" to String.format(Locale.ROOT, "%.4g", points.maxOf { it.remainingMilligrams }.coerceAtLeast(0.001)))),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                PkLineChart(points, now, accentColor)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(i18n("pk_30_days_ago"), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(30f))
                    Text(i18n("time_now_label"), style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.weight(7f))
                    Text(i18n("pk_7_days_ahead"), style = MaterialTheme.typography.labelSmall)
                }
            } else if (pastDoses.isEmpty()) {
                Text(i18n("pk_no_doses"), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PkLineChart(points: List<PkPoint>, now: Instant, color: Color) {
    val guide = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier = Modifier.fillMaxWidth().height(170.dp)) {
        if (size.width <= 0f || size.height <= 0f || points.size < 2) return@Canvas
        val maximum = points.maxOf { it.remainingMilligrams }.coerceAtLeast(0.001)
        val first = points.first().time.toEpochMilli()
        val span = (points.last().time.toEpochMilli() - first).toDouble().coerceAtLeast(1.0)
        val nowX = ((now.toEpochMilli() - first) / span * size.width).toFloat()
        drawLine(guide, Offset(0f, size.height), Offset(size.width, size.height))
        drawLine(guide, Offset(nowX, 0f), Offset(nowX, size.height))
        val path = Path()
        points.forEachIndexed { index, point ->
            val x = ((point.time.toEpochMilli() - first) / span * size.width).toFloat()
            val y = (size.height * (1.0 - point.remainingMilligrams / maximum)).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}
