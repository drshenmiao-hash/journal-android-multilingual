// SPDX-License-Identifier: GPL-3.0-only

package com.isaakhanimann.journal.ui.tabs.stats.substancecompanion

/**
 * Adult oral parent-drug elimination references, reviewed 2026-10-20.
 * These are NOT effect durations or individual measurements. See docs/pk-references.md.
 */
data class PkReference(
    val halfLifeHours: Double,
    val sourceSetId: String,
    val rangeHours: String? = null,
    val usesRangeMidpoint: Boolean = false,
    val requiresNormalRenalFunction: Boolean = false
) {
    val sourceUrl: String
        get() = "https://dailymed.nlm.nih.gov/dailymed/drugInfo.cfm?setid=$sourceSetId"
}

object BuiltInPkReferences {
    val all: Map<String, PkReference> = mapOf(
        "Alprazolam" to PkReference(11.2, "60c90b4b-174f-4fd2-b65f-fc166c16bdc1", "6.3–26.9"),
        "Lorazepam" to PkReference(12.0, "8602cef8-167e-4a8c-b10f-fc3295a3abcc"),
        "Sertraline" to PkReference(26.0, "232a00a4-761e-422f-b62a-1a16de631eb6"),
        "Escitalopram" to PkReference(29.5, "84b2028d-eff4-434d-b71e-6a3c0812f625", "27–32", true),
        "Citalopram" to PkReference(35.0, "598cc779-06be-b27a-e053-2a91aa0a0909"),
        "Mirtazapine" to PkReference(30.0, "edc62571-3808-b4b4-e053-2a95a90ab436", "20–40", true),
        "Olanzapine" to PkReference(30.0, "024e8e9b-a4fc-4882-b080-c070467166a6", "21–54"),
        "Quetiapine" to PkReference(6.0, "5dd65e85-430b-4e25-bfe0-5b4c332c39c0"),
        "Pregabalin" to PkReference(6.3, "0c5d6728-be43-4e1b-9f14-a7ce920e12a3",
            requiresNormalRenalFunction = true),
        // The LA label explicitly reports the adult immediate-release Ritalin tablet result.
        "Methylphenidate" to PkReference(3.5, "effd952d-ac94-47bb-b107-589a4934dcca", "1.3–7.7")
    )

    fun effectiveHalfLife(substanceName: String, override: Double?): Double? =
        override?.takeIf { it.isFinite() && it in 0.1..10000.0 } ?: all[substanceName]?.halfLifeHours
}
