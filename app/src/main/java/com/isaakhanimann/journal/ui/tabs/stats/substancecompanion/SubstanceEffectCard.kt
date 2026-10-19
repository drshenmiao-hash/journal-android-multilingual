// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.isaakhanimann.journal.data.room.experiences.entities.SubstanceCompanion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.classes.Substance
import com.isaakhanimann.journal.localization.i18n
import com.isaakhanimann.journal.localization.i18nOrDefault
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.CardWithTitle
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.DataForOneEffectLine
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.ExperienceEffectTimelines
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.TimeDisplayOption
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.AllTimelines
import com.isaakhanimann.journal.ui.tabs.journal.experience.timeline.AllTimelinesModel
import com.isaakhanimann.journal.ui.utils.administrationRouteKey
import com.isaakhanimann.journal.ui.utils.getDateWithWeekdayText
import java.time.Instant

@Composable
fun SubstanceEffectCard(
    substance: Substance?,
    companion: SubstanceCompanion,
    bursts: List<IngestionsBurst>,
    consumerName: String?,
    now: Instant,
    onShowSubstance: () -> Unit
) {
    val latest = bursts.filter { burst -> burst.ingestions.any {
        it.ingestion.consumerName == consumerName && !it.ingestion.time.isAfter(now)
    } }.maxByOrNull { burst -> burst.ingestions.filter { it.ingestion.consumerName == consumerName && !it.ingestion.time.isAfter(now) }
        .maxOf { it.ingestion.time } }
    var showReference by rememberSaveable(companion.substanceName, consumerName) { mutableStateOf(false) }
    var routeName by rememberSaveable(companion.substanceName) { mutableStateOf(AdministrationRoute.ORAL.name) }
    val referenceMode = showReference || latest == null
    val routes = remember(substance) { substance?.roas?.filter { it.roaDuration?.hasEffectTimings() == true }.orEmpty() }
    val route = routes.firstOrNull { it.route.name == routeName } ?: routes.firstOrNull()
    val recorded = remember(latest, substance, companion, consumerName, now) {
        if (latest == null || substance == null) SubstanceEffectData(emptyList(), 0, 0)
        else SubstanceEffectData.fromRecords(latest.ingestions, substance, companion, consumerName, now)
    }
    CardWithTitle(title = i18n("effect_curve_title"), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (latest != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !referenceMode, onClick = { showReference = false },
                        label = { Text(i18n("effect_curve_recorded")) })
                    FilterChip(selected = referenceMode, onClick = { showReference = true },
                        label = { Text(i18n("effect_curve_reference")) })
                }
            }
            Text(i18n("effect_curve_note"), style = MaterialTheme.typography.bodySmall)
            if (referenceMode && route != null) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(routes, key = { it.route.name }) { option ->
                        FilterChip(selected = option == route, onClick = { routeName = option.route.name },
                            label = { Text(i18nOrDefault(administrationRouteKey(option.route), option.route.displayText)) })
                    }
                }
                val model = remember(route, now, companion) {
                    AllTimelinesModel(
                        dataForLines = listOf(DataForOneEffectLine(
                            substanceName = companion.substanceName, route = route.route,
                            roaDuration = route.roaDuration, height = 1f, horizontalWeight = 0.5f,
                            color = companion.color, startTime = now, endTime = null
                        )), dataForRatings = emptyList(), timedNotes = emptyList()
                    )
                }
                AllTimelines(model = model, isShowingCurrentTime = false,
                    timeDisplayOption = TimeDisplayOption.REGULAR,
                    modifier = Modifier.fillMaxWidth().height(200.dp))
            } else if (!referenceMode && recorded.elements.isNotEmpty()) {
                Text(latest!!.experience.sortDate.getDateWithWeekdayText(), style = MaterialTheme.typography.labelMedium)
                val model = remember(recorded) {
                    AllTimelinesModel(recorded.timelineLines(), emptyList(), emptyList())
                }
                AllTimelines(model = model, isShowingCurrentTime = true,
                    timeDisplayOption = TimeDisplayOption.REGULAR,
                    modifier = Modifier.fillMaxWidth().height(200.dp))
                if (recorded.excludedCount > 0) {
                    Text(i18n("effect_curve_excluded", mapOf("count" to recorded.excludedCount.toString())),
                        style = MaterialTheme.typography.bodySmall)
                }
                if (recorded.unknownDoseCount > 0) {
                    Text(i18n("effect_curve_unknown_dose"), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text(i18n("effect_curve_no_data"), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onShowSubstance) { Text(i18n("effect_curve_details")) }
        }
    }
}