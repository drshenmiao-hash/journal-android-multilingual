// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import com.isaakhanimann.journal.data.room.experiences.entities.SubstanceCompanion
import com.isaakhanimann.journal.data.room.experiences.relations.IngestionWithCompanionAndCustomUnit
import com.isaakhanimann.journal.data.substances.classes.Substance
import com.isaakhanimann.journal.data.substances.classes.roa.RoaDuration
import com.isaakhanimann.journal.ui.tabs.journal.experience.models.IngestionElement
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.DataForOneEffectLine
import com.isaakhanimann.journal.ui.tabs.journal.experience.components.getStrengthRelativeToCommonDose
import java.time.Instant

data class SubstanceEffectData(
    val elements: List<IngestionElement>,
    val excludedCount: Int,
    val unknownDoseCount: Int
) {
    fun timelineLines(): List<DataForOneEffectLine> = elements.map { element ->
        val rendered = element.ingestionWithCompanionAndCustomUnit
        val ingestion = rendered.ingestion
        // The native fallback averages doses. Keep unlike routes/units out of that average.
        val comparable = elements.map { it.ingestionWithCompanionAndCustomUnit }.filter {
            it.ingestion.administrationRoute == ingestion.administrationRoute &&
                it.ingestion.units == ingestion.units
        }
        DataForOneEffectLine(
            substanceName = ingestion.substanceName,
            route = ingestion.administrationRoute,
            roaDuration = element.roaDuration,
            height = getStrengthRelativeToCommonDose(rendered, comparable, element.roaDose),
            horizontalWeight = element.numDots?.let { (it.toFloat() / 4f).coerceIn(0f, 1f) } ?: 0.5f,
            color = rendered.substanceCompanion!!.color,
            startTime = ingestion.time,
            endTime = ingestion.endTime
        )
    }

    companion object {
        fun fromRecords(
            records: List<IngestionsBurst.IngestionAndCustomUnit>,
            substance: Substance,
            companion: SubstanceCompanion,
            consumerName: String?,
            now: Instant
        ): SubstanceEffectData {
            val matching = records.filter {
                it.ingestion.substanceName == substance.name && it.ingestion.consumerName == consumerName
            }
            val elements = matching.mapNotNull { record ->
                val ingestion = record.ingestion
                if (ingestion.time.isAfter(now)) return@mapNotNull null
                if (ingestion.endTime?.isBefore(ingestion.time) == true) return@mapNotNull null
                val roa = substance.getRoa(ingestion.administrationRoute, ingestion.releaseForm)
                    ?: return@mapNotNull null
                if (roa.roaDuration?.hasEffectTimings() != true) return@mapNotNull null
                val custom = record.customUnit
                val raw = ingestion.dose?.takeIf { it.isFinite() && it > 0.0 }
                val validCustom = custom?.takeIf {
                    it.id == ingestion.customUnitId && it.substanceName == substance.name &&
                        it.administrationRoute == ingestion.administrationRoute
                }
                val amount = if (ingestion.customUnitId != null) {
                    validCustom?.dose?.takeIf { it.isFinite() && it > 0.0 }?.let { perUnit -> raw?.times(perUnit) }
                } else raw
                val originalUnit = if (ingestion.customUnitId != null) validCustom?.originalUnit else ingestion.units
                val targetUnit = roa.roaDose?.units ?: when (substance.name) {
                    "Cannabis" -> "mg THC"
                    "Psilocybin mushrooms" -> "mg Psilocybin"
                    else -> if (PkCurve.milligrams(1.0, originalUnit) != null) "mg" else originalUnit
                }
                val normalisedDose = if (roa.roaDose == null && PkCurve.milligrams(1.0, originalUnit, substance.name) == null) {
                    null
                } else if (originalUnit == targetUnit) amount else {
                    val mass = PkCurve.milligrams(amount, originalUnit, substance.name)
                    val targetMass = PkCurve.milligrams(1.0, targetUnit, substance.name)
                    if (mass != null && targetMass != null) mass / targetMass else null
                }?.takeIf { it.isFinite() && it > 0.0 && it.toFloat().isFinite() }
                // A rendering-only copy: journal entries and custom-unit definitions are never rewritten.
                val rendered = IngestionWithCompanionAndCustomUnit(
                    ingestion = ingestion.copy(dose = normalisedDose, units = targetUnit, customUnitId = null),
                    substanceCompanion = companion,
                    customUnit = null
                )
                IngestionElement(
                    ingestionWithCompanionAndCustomUnit = rendered,
                    roaDuration = roa.roaDuration,
                    roaDose = roa.roaDose,
                    numDots = roa.roaDose?.getNumDots(normalisedDose, targetUnit)
                )
            }.sortedBy { it.ingestionWithCompanionAndCustomUnit.ingestion.time }
            return SubstanceEffectData(elements, matching.size - elements.size,
                elements.count { it.ingestionWithCompanionAndCustomUnit.ingestion.dose == null })
        }
    }
}

fun RoaDuration.hasEffectTimings(): Boolean = listOf(onset, comeup, peak, offset, total).any { range ->
    val seconds = range?.maxInSec ?: range?.minInSec
    seconds != null && seconds.isFinite() && seconds > 0f
}