// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import com.isaakhanimann.journal.data.room.experiences.entities.CustomUnit
import com.isaakhanimann.journal.data.room.experiences.entities.Experience
import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.room.experiences.relations.IngestionWithExperienceAndCustomUnit
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.ReleaseForm
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PkDosesTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val experience = Experience(1, "", "", now, now, false, null)
    private val ingestion = Ingestion(
        substanceName = "Caffeine",
        time = now,
        creationDate = now,
        administrationRoute = AdministrationRoute.ORAL,
        dose = 20.0,
        isDoseAnEstimate = false,
        estimatedDoseStandardDeviation = null,
        units = "mg",
        experienceId = 1,
        notes = null,
        stomachFullness = null,
        consumerName = null,
        customUnitId = null
    )
    private val custom = CustomUnit(
        id = 7, substanceName = "Caffeine", name = "Tablet", creationDate = now,
        administrationRoute = AdministrationRoute.ORAL, dose = 0.1,
        estimatedDoseStandardDeviation = null, isEstimate = false, isArchived = false,
        unit = "tablet", originalUnit = "g", note = ""
    )

    private fun entry(i: Ingestion = ingestion, c: CustomUnit? = null) =
        IngestionWithExperienceAndCustomUnit(i, experience, c)

    @Test
    fun ownerAndNamedConsumersNeverCombineAndOtherSubstancesDoNotCountAsExcluded() {
        val entries = listOf(entry(), entry(ingestion.copy(consumerName = "Alex", dose = 10.0)),
            entry(ingestion.copy(substanceName = "Other")))
        val owner = PkDoses.fromEntries(entries, "Caffeine", null)
        val alex = PkDoses.fromEntries(entries, "Caffeine", "Alex")
        assertEquals(listOf(PkDose(now, 20.0)), owner.doses)
        assertEquals(listOf(PkDose(now, 10.0)), alex.doses)
        assertEquals(0, owner.excludedCount)
        assertEquals(0, PkDoses.fromEntries(entries, "Caffeine", "Unknown").doses.size)
    }

    @Test
    fun customUnitsMultiplyCountByMassPerUnitIncludingArchivedDefinitions() {
        val doses = PkDoses.fromEntries(listOf(
            entry(ingestion.copy(dose = 2.5, customUnitId = 7, units = "tablet"), custom.copy(isArchived = true))
        ), "Caffeine", null)
        assertEquals(250.0, doses.doses.single().milligrams, 1e-9)
    }

    @Test
    fun brokenCustomReferencesNeverFallBackToARawMassDose() {
        val i = ingestion.copy(customUnitId = 7)
        val entries = listOf(
            entry(i), entry(i, custom.copy(dose = null)), entry(i, custom.copy(dose = -1.0)),
            entry(i, custom.copy(id = 8)), entry(i, custom.copy(substanceName = "Other")),
            entry(i, custom.copy(originalUnit = "ml"))
        )
        val doses = PkDoses.fromEntries(entries, "Caffeine", null)
        assertEquals(0, doses.doses.size)
        assertEquals(entries.size, doses.excludedCount)
    }

    @Test
    fun unknownDosesExtendedReleaseAndDurationRecordsAreExplicitlyExcluded() {
        val entries = listOf(
            entry(ingestion.copy(dose = null)), entry(ingestion.copy(dose = Double.NaN)),
            entry(ingestion.copy(units = "ml")),
            entry(ingestion.copy(releaseForm = ReleaseForm.EXTENDED_RELEASE)),
            entry(ingestion.copy(endTime = now.plusSeconds(3600))),
            entry(ingestion.copy(releaseForm = ReleaseForm.IMMEDIATE_RELEASE))
        )
        val doses = PkDoses.fromEntries(entries, "Caffeine", null)
        assertEquals(1, doses.doses.size)
        assertEquals(5, doses.excludedCount)
    }

    @Test
    fun futureRecordsAreOmittedUntilTheirTimeAndNotProjectedAsScheduledDoses() {
        val input = PkDoses.fromEntries(listOf(entry(), entry(ingestion.copy(time = now.plusSeconds(1)))), "Caffeine", null)
        assertEquals(1, input.recordedAt(now).doses.size)
        assertEquals(1, input.recordedAt(now).excludedCount)
        assertEquals(2, input.recordedAt(now.plusSeconds(1)).doses.size)
        assertEquals(0, input.recordedAt(now.plusSeconds(1)).excludedCount)
    }
}
