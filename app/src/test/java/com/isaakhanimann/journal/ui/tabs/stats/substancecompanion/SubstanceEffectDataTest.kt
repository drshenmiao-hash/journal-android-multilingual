// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import com.isaakhanimann.journal.data.room.experiences.entities.AdaptiveColor
import com.isaakhanimann.journal.data.room.experiences.entities.CustomUnit
import com.isaakhanimann.journal.data.room.experiences.entities.Ingestion
import com.isaakhanimann.journal.data.room.experiences.entities.SubstanceCompanion
import com.isaakhanimann.journal.data.substances.AdministrationRoute
import com.isaakhanimann.journal.data.substances.ReleaseForm
import com.isaakhanimann.journal.data.substances.parse.SubstanceParser
import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class SubstanceEffectDataTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")
    private val companion = SubstanceCompanion("Test drug", AdaptiveColor.RED)
    private val substance = SubstanceParser().parseSubstance("""
        {"name":"Test drug","roas":[
          {"name":"oral","dose":{"units":"mg","lightMin":1,"commonMin":10,"strongMin":30,"heavyMin":50},
           "duration":{"onset":{"min":10,"max":20,"units":"minutes"},
                       "total":{"min":2,"max":4,"units":"hours"}}},
          {"name":"insufflated","duration":{"total":{"min":1,"max":2,"units":"hours"}}}
        ]}
    """)!!
    private val ingestion = Ingestion(
        substanceName = substance.name, time = now, creationDate = now,
        administrationRoute = AdministrationRoute.ORAL, dose = 20.0,
        isDoseAnEstimate = false, estimatedDoseStandardDeviation = null,
        units = "mg", experienceId = 1, notes = null, stomachFullness = null,
        consumerName = null, customUnitId = null
    )
    private val custom = CustomUnit(
        id = 7, substanceName = substance.name, name = "Tablet", creationDate = now,
        administrationRoute = AdministrationRoute.ORAL, dose = 0.1,
        estimatedDoseStandardDeviation = null, isEstimate = false, isArchived = true,
        unit = "tablet", originalUnit = "g", note = ""
    )
    private fun record(i: Ingestion = ingestion, c: CustomUnit? = null) =
        IngestionsBurst.IngestionAndCustomUnit(i, c)
    private fun data(records: List<IngestionsBurst.IngestionAndCustomUnit>, consumer: String? = null) =
        SubstanceEffectData.fromRecords(records, substance, companion, consumer, now)

    @Test
    fun effectsNeedNoHalfLifeAndConsumersStaySeparate() {
        val records = listOf(record(), record(ingestion.copy(consumerName = "Alex", dose = 40.0)),
            record(ingestion.copy(substanceName = "Other")))
        assertEquals(1, data(records).elements.size)
        assertEquals(1f, data(records).timelineLines().single().height, 1e-6f)
        assertEquals(2f, data(records, "Alex").timelineLines().single().height, 1e-6f)
        assertEquals(0, data(records).excludedCount)
        assertTrue(data(records, "Unknown").elements.isEmpty())
    }

    @Test
    fun equivalentMassAndCustomUnitsProduceEqualHeightsWithoutRewritingRecords() {
        val grams = ingestion.copy(dose = 0.02, units = "g")
        val tablets = ingestion.copy(dose = 0.2, units = "tablet", customUnitId = 7)
        val result = data(listOf(record(), record(grams), record(tablets, custom)))
        result.timelineLines().forEach { assertEquals(1f, it.height, 1e-6f) }
        assertEquals(0, result.unknownDoseCount)
        assertEquals("g", grams.units)
        assertEquals(0.02, grams.dose!!, 1e-12)
        assertEquals(7, tablets.customUnitId)
        assertEquals("g", custom.originalUnit)
    }

    @Test
    fun missingOrMismatchedCustomUnitsNeverFallBackToRawNumbers() {
        val broken = ingestion.copy(customUnitId = 7)
        val result = data(listOf(record(broken), record(broken, custom.copy(id = 8)),
            record(broken, custom.copy(substanceName = "Other")),
            record(broken, custom.copy(administrationRoute = AdministrationRoute.INSUFFLATED)),
            record(broken, custom.copy(dose = null))))
        assertEquals(5, result.unknownDoseCount)
        result.timelineLines().forEach { assertEquals(1f, it.height, 1e-6f) }
    }

    @Test
    fun incompatibleAndInvalidDosesHaveNeutralHeightAndNotice() {
        val result = data(listOf(record(ingestion.copy(units = "ml")),
            record(ingestion.copy(dose = Double.NaN)), record(ingestion.copy(dose = -5.0)),
            record(ingestion.copy(dose = null))))
        assertEquals(4, result.unknownDoseCount)
        result.timelineLines().forEach { assertEquals(1f, it.height, 1e-6f) }
    }

    @Test
    fun futureUnsupportedRouteExtendedReleaseAndInvalidDurationAreExcluded() {
        val result = data(listOf(record(), record(ingestion.copy(time = now.plusSeconds(1))),
            record(ingestion.copy(releaseForm = ReleaseForm.EXTENDED_RELEASE)),
            record(ingestion.copy(administrationRoute = AdministrationRoute.INTRAVENOUS)),
            record(ingestion.copy(endTime = now.minusSeconds(1)))))
        assertEquals(1, result.elements.size)
        assertEquals(4, result.excludedCount)
    }

    @Test
    fun fallbackDoseAverageCannotMixRoutesOrUnknownVolumeUnits() {
        val result = data(listOf(record(), record(ingestion.copy(
            administrationRoute = AdministrationRoute.INSUFFLATED, dose = 0.1, units = "g")),
            record(ingestion.copy(administrationRoute = AdministrationRoute.INSUFFLATED, units = "ml"))))
        assertEquals(1, result.unknownDoseCount)
        result.timelineLines().forEach { assertEquals(1f, it.height, 1e-6f) }
    }

    @Test
    fun timingWithoutUnitsOrWithoutDurationDoesNotCreateAnEffectCurve() {
        val unavailable = SubstanceParser().parseSubstance("""
            {"name":"Test drug","roas":[{"name":"oral",
            "duration":{"onset":{"min":1,"max":2}}}]}
        """)!!
        val result = SubstanceEffectData.fromRecords(listOf(record()), unavailable, companion, null, now)
        assertTrue(result.elements.isEmpty())
        assertEquals(1, result.excludedCount)
    }

    @Test
    fun timelinesPreserveHistoricalTimesAndContinuousAdministrationEndpoints() {
        val start = now.minusSeconds(3600)
        val result = data(listOf(record(), record(ingestion.copy(time = start, endTime = now))))
        assertEquals(listOf(start, now), result.timelineLines().map { it.startTime })
        assertEquals(now, result.timelineLines().first().endTime)
    }
}
