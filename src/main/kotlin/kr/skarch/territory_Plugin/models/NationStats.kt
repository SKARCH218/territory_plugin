package kr.skarch.territory_Plugin.models

/**
 * 국가 통계 정보
 */
data class NationStats(
    val nationName: String,
    val displayName: String,
    val totalChunks: Int,
    val totalStones: Int,
    val highestTier: StoneTier,
    val isAtWar: Boolean,
    val memberCount: Int = 0
) : Comparable<NationStats> {

    /**
     * 영토 점수 계산 (랭킹용)
     */
    fun getTerritoryScore(): Int {
        var score = totalChunks * 10
        score += totalStones * 50
        score += when (highestTier) {
            StoneTier.OUTPOST -> 0      // 전초기지는 점수 없음
            StoneTier.TIER_1 -> 0
            StoneTier.TIER_2 -> 100
            StoneTier.TIER_3 -> 300
            StoneTier.TIER_4 -> 700
            StoneTier.TIER_5 -> 1500
        }
        return score
    }

    override fun compareTo(other: NationStats): Int {
        return other.getTerritoryScore().compareTo(this.getTerritoryScore())
    }
}

