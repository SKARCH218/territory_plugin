package kr.skarch.territory_Plugin.models

enum class StoneTier(val tierName: String, val radius: Int, val canUpgrade: Boolean = true) {
    OUTPOST("OUTPOST", 0, false),  // 전초기지: 1청크만, 업그레이드 불가
    TIER_1("TIER_1", 1),           // 3x3
    TIER_2("TIER_2", 4),           // 9x9
    TIER_3("TIER_3", 7),           // 15x15
    TIER_4("TIER_4", 10),          // 21x21
    TIER_5("TIER_5", 17);          // 35x35

    /**
     * Get tier level as integer (0-5, 0 = OUTPOST)
     */
    fun getTierLevel(): Int {
        return ordinal
    }

    /**
     * Check if this tier can be upgraded
     */
    fun isUpgradeable(): Boolean {
        return canUpgrade
    }

    fun getNext(): StoneTier? {
        if (!canUpgrade) return null  // 전초기지는 업그레이드 불가

        return when (this) {
            OUTPOST -> null  // 전초기지는 업그레이드 불가
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

