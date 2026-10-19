// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import java.time.Instant
import org.junit.Assert.*
import org.junit.Test

class BuiltInPkReferencesTest {
    @Test
    fun reviewedDrugsAutomaticallySelectReferencesButUnknownDrugsDoNot() {
        assertEquals(26.0, BuiltInPkReferences.effectiveHalfLife("Sertraline", null)!!, 1e-9)
        assertEquals(10, BuiltInPkReferences.all.size)
        assertNull(BuiltInPkReferences.effectiveHalfLife("Unreviewed drug", null))
        assertNull(BuiltInPkReferences.effectiveHalfLife("Fluoxetine", null))
    }

    @Test
    fun validOverridesTakePriorityAndInvalidOnesCannotBreakTheCurve() {
        assertEquals(12.0, BuiltInPkReferences.effectiveHalfLife("Sertraline", 12.0)!!, 1e-9)
        listOf(Double.NaN, Double.POSITIVE_INFINITY, -1.0, 0.0, 10001.0).forEach {
            assertEquals(26.0, BuiltInPkReferences.effectiveHalfLife("Sertraline", it)!!, 1e-9)
        }
        assertEquals(8.0, BuiltInPkReferences.effectiveHalfLife("Custom drug", 8.0)!!, 1e-9)
    }

    @Test
    fun referencesKeepProvenanceAndRangeMidpointFlags() {
        BuiltInPkReferences.all.values.forEach {
            assertTrue(it.halfLifeHours in 0.1..10000.0)
            assertTrue(it.sourceUrl.startsWith("https://dailymed.nlm.nih.gov/"))
            assertTrue(it.sourceSetId.matches(Regex("[0-9a-f-]{36}")))
        }
        assertTrue(BuiltInPkReferences.all.getValue("Escitalopram").usesRangeMidpoint)
        assertTrue(BuiltInPkReferences.all.getValue("Mirtazapine").usesRangeMidpoint)
        assertFalse(BuiltInPkReferences.all.getValue("Olanzapine").usesRangeMidpoint)
        assertTrue(BuiltInPkReferences.all.getValue("Pregabalin").requiresNormalRenalFunction)
    }

    @Test
    fun automaticReferencePreservesResidualAfterStoppingAndRestarting() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val hours = BuiltInPkReferences.effectiveHalfLife("Sertraline", null)!!
        val doses = listOf(PkDose(now.minusSeconds(26 * 3600L), 50.0), PkDose(now, 50.0))
        assertEquals(75.0, PkCurve.remaining(doses, now, hours), 1e-9)
        assertEquals(37.5, PkCurve.remaining(doses, now.plusSeconds(26 * 3600L), hours), 1e-9)
    }
}
