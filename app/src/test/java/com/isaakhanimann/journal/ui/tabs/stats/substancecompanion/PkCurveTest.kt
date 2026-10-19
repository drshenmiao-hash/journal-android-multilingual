// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class PkCurveTest {
    private val origin = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun oldDoseDecaysAcrossGapAndAddsToRestartedDose() {
        val doses = listOf(PkDose(origin, 100.0), PkDose(origin.plusSeconds(72 * 3600), 50.0))
        assertEquals(12.5, PkCurve.remaining(doses, origin.plusSeconds(72 * 3600 - 1), 24.0), 0.01)
        assertEquals(62.5, PkCurve.remaining(doses, origin.plusSeconds(72 * 3600), 24.0), 1e-9)
        assertEquals(31.25, PkCurve.remaining(doses, origin.plusSeconds(96 * 3600), 24.0), 1e-9)
    }

    @Test
    fun historicalAndFutureDosesAreOnlyCountedAfterTheirOwnTime() {
        val doses = listOf(PkDose(origin.plusSeconds(3600), 10.0))
        assertEquals(0.0, PkCurve.remaining(doses, origin, 24.0), 0.0)
        assertEquals(10.0, PkCurve.remaining(doses, origin.plusSeconds(3600), 24.0), 0.0)
        val series = PkCurve.series(doses, origin, origin.plusSeconds(7200), 24.0, intervals = 3)
        val immediatelyBefore = series.single { it.time == origin.plusSeconds(3600).minusMillis(1) }
        val atDose = series.single { it.time == origin.plusSeconds(3600) }
        assertEquals(0.0, immediatelyBefore.remainingMilligrams, 0.0)
        assertEquals(10.0, atDose.remainingMilligrams, 0.0)
    }

    @Test
    fun onlyComparablePositiveMassUnitsAreConverted() {
        assertEquals(500.0, PkCurve.milligrams(0.5, "g")!!, 0.0)
        assertEquals(0.5, PkCurve.milligrams(500.0, "µg")!!, 0.0)
        assertNull(PkCurve.milligrams(10.0, "ml"))
        assertNull(PkCurve.milligrams(Double.NaN, "mg"))
    }

    @Test
    fun allHistoryContributesEvenBeforeTheVisibleWindow() {
        val start = origin.plusSeconds(40 * 86400L)
        val doses = listOf(PkDose(origin, 100.0))
        val series = PkCurve.series(doses, start, start.plusSeconds(86400), 40 * 24.0)
        assertEquals(50.0, series.first().remainingMilligrams, 1e-9)
        series.forEach { assertEquals(PkCurve.remaining(doses, it.time, 960.0), it.remainingMilligrams, 1e-9) }
    }

    @Test
    fun irregularAndSimultaneousDosesMatchIndependentSuperposition() {
        val doses = listOf(
            PkDose(origin.plusSeconds(19000), 30.0),
            PkDose(origin.minusSeconds(80000), 12.0),
            PkDose(origin.plusSeconds(8000), 20.0),
            PkDose(origin.plusSeconds(8000), 7.0),
            PkDose(origin, 3.0)
        )
        val points = PkCurve.series(doses, origin, origin.plusSeconds(40000), 3.0)
        points.forEach { assertEquals(PkCurve.remaining(doses, it.time, 3.0), it.remainingMilligrams, 1e-8) }
    }

    @Test
    fun activeIngredientUnitsAreNotMixedWithPlantMass() {
        assertEquals(5.0, PkCurve.milligrams(5.0, "mg THC", "Cannabis")!!, 0.0)
        assertEquals(4.0, PkCurve.milligrams(4.0, "mg Psilocybin", "Psilocybin mushrooms")!!, 0.0)
        assertNull(PkCurve.milligrams(1.0, "g", "Cannabis"))
        assertNull(PkCurve.milligrams(1.0, "g", "Psilocybin mushrooms"))
        assertNull(PkCurve.milligrams(1.0, "mg THC", "Caffeine"))
        listOf("mcg", "ug", "µg", "μg", "微克").forEach {
            assertEquals(0.5, PkCurve.milligrams(500.0, it)!!, 0.0)
        }
        assertEquals(1.0, PkCurve.milligrams(1.0, " MG ")!!, 0.0)
        listOf(0.0, -1.0, Double.POSITIVE_INFINITY).forEach { assertNull(PkCurve.milligrams(it, "mg")) }
        assertNull(PkCurve.milligrams(Double.MAX_VALUE, "g"))
    }

    @Test
    fun invalidHalfLivesAndWindowsAreRejected() {
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach {
            assertThrows(IllegalArgumentException::class.java) { PkCurve.remaining(emptyList(), origin, it) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            PkCurve.series(emptyList(), origin, origin.minusSeconds(1), 24.0)
        }
    }
}
