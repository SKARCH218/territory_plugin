package kr.skarch.territory_Plugin.models

enum class StoneTier(val tierName: String, val radius: Int) {
    TIER_1("TIER_1", 1),    // 3x3
    TIER_2("TIER_2", 4),    // 9x9
    TIER_3("TIER_3", 7),    // 15x15
    TIER_4("TIER_4", 10),   // 21x21
    TIER_5("TIER_5", 17);   // 35x35

    /**
     * Get tier level as integer (1-5)
     */
    fun getTierLevel(): Int {
        return ordinal + 1
    }

    fun getNext(): StoneTier? {
        return when (this) {
            TIER_1 -> TIER_2
            TIER_2 -> TIER_3
            TIER_3 -> TIER_4
            TIER_4 -> TIER_5
            TIER_5 -> null
        }
    }

    companion object {
        fun fromString(tier: String): StoneTier {
            return values().find { it.tierName == tier } ?: TIER_1
        }
    }
}

