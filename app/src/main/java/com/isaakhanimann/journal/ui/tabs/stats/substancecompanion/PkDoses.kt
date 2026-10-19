// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import com.isaakhanimann.journal.data.room.experiences.relations.IngestionWithExperienceAndCustomUnit
import com.isaakhanimann.journal.data.substances.ReleaseForm
import java.time.Instant

data class PkDoses(val doses: List<PkDose>, val excludedCount: Int) {
    /** Future records are not treated as planned doses in the projection. */
    fun recordedAt(now: Instant): PkDoses {
        val past = doses.filter { !it.time.isAfter(now) }
        return PkDoses(past, excludedCount + doses.size - past.size)
    }

    companion object {
        fun fromEntries(
            entries: List<IngestionWithExperienceAndCustomUnit>,
            substanceName: String,
            consumerName: String?
        ): PkDoses {
            // null denotes the owner, not all consumers.
            val matching = entries.filter {
                it.ingestion.substanceName == substanceName && it.ingestion.consumerName == consumerName
            }
            val doses = matching.mapNotNull { entry ->
                val ingestion = entry.ingestion
                val raw = ingestion.dose?.takeIf { it.isFinite() && it > 0.0 }
                    ?: return@mapNotNull null
                // Instant input is not a suitable representation of an infusion or ER dose.
                if (ingestion.endTime != null || ingestion.releaseForm == ReleaseForm.EXTENDED_RELEASE) {
                    return@mapNotNull null
                }
                val custom = entry.customUnit
                val amount: Double?
                val unit: String?
                if (ingestion.customUnitId != null) {
                    if (custom == null || custom.id != ingestion.customUnitId ||
                        custom.substanceName != substanceName ||
                        custom.administrationRoute != ingestion.administrationRoute
                    ) return@mapNotNull null
                    val perUnit = custom.dose?.takeIf { it.isFinite() && it > 0.0 }
                        ?: return@mapNotNull null
                    amount = perUnit * raw
                    unit = custom.originalUnit
                } else {
                    amount = raw
                    unit = ingestion.units
                }
                PkCurve.milligrams(amount, unit, substanceName)?.let { PkDose(ingestion.time, it) }
            }
            return PkDoses(doses, matching.size - doses.size)
        }
    }
}
