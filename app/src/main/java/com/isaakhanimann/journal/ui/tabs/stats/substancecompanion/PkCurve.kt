// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

import java.time.Duration
import java.time.Instant
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln

/** An amount converted to milligrams; entries with incompatible units are omitted. */
data class PkDose(val time: Instant, val milligrams: Double)

data class PkPoint(val time: Instant, val remainingMilligrams: Double)

/**
 * A one-compartment, instantaneous-input, first-order elimination illustration.
 * The result is an amount-equivalent curve, NOT a measured blood concentration.
 * Bioavailability, absorption, metabolites and individual clearance are not modeled.
 */
object PkCurve {
    fun milligrams(amount: Double?, unit: String?, substanceName: String? = null): Double? {
        if (amount == null || !amount.isFinite() || amount <= 0.0) return null
        val normalizedUnit = unit?.trim()?.lowercase(Locale.ROOT)
        // These catalog entries distinguish active ingredient from plant/fungus mass.
        // Never add grams of material to milligrams of its active ingredient.
        if (substanceName == "Cannabis" || substanceName == "Psilocybin mushrooms") {
            val activeUnit = if (substanceName == "Cannabis") "mg thc" else "mg psilocybin"
            return amount.takeIf { normalizedUnit == activeUnit }
        }
        val factor = when (normalizedUnit) {
            "mg", "毫克" -> 1.0
            "g", "克" -> 1000.0
            "mcg", "μg", "µg", "ug", "微克" -> 0.001
            else -> return null
        }
        return (amount * factor).takeIf { it.isFinite() && it > 0.0 }
    }

    fun remaining(doses: List<PkDose>, at: Instant, halfLifeHours: Double): Double {
        require(halfLifeHours.isFinite() && halfLifeHours > 0.0)
        return doses.sumOf { dose ->
            if (dose.time.isAfter(at) || !dose.milligrams.isFinite() || dose.milligrams <= 0.0) {
                0.0
            } else {
                val elapsedHours = Duration.between(dose.time, at).toMillis() / 3_600_000.0
                dose.milligrams * exp(-ln(2.0) * elapsedHours / halfLifeHours)
            }
        }
    }

    fun series(
        doses: List<PkDose>,
        start: Instant,
        end: Instant,
        halfLifeHours: Double,
        intervals: Int = 240
    ): List<PkPoint> {
        require(intervals in 1..1000 && !end.isBefore(start))
        require(halfLifeHours.isFinite() && halfLifeHours > 0.0)
        val durationMillis = Duration.between(start, end).toMillis()
        val times = (0..intervals).mapTo(sortedSetOf<Instant>()) { index ->
            start.plusMillis(durationMillis * index / intervals)
        }
        doses.forEach { dose ->
            if (dose.time.isAfter(start) && !dose.time.isAfter(end)) {
                val justBefore = dose.time.minusMillis(1)
                if (!justBefore.isBefore(start)) times.add(justBefore)
                times.add(dose.time)
            }
        }
        val events = doses.filter {
            it.time.isAfter(start) && !it.time.isAfter(end) &&
                it.milligrams.isFinite() && it.milligrams > 0.0
        }.sortedBy { it.time }
        var eventIndex = 0
        var previous = start
        var amount = remaining(doses, start, halfLifeHours)
        return times.map { at ->
            val elapsedHours = Duration.between(previous, at).toMillis() / 3_600_000.0
            amount *= exp(-ln(2.0) * elapsedHours / halfLifeHours)
            while (eventIndex < events.size && !events[eventIndex].time.isAfter(at)) {
                amount += events[eventIndex++].milligrams
            }
            previous = at
            PkPoint(at, amount)
        }
    }
}
