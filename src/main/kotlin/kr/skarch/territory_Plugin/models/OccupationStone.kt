package kr.skarch.territory_Plugin.models

import org.bukkit.Location
import java.util.UUID

data class OccupationStone(
    val stoneUuid: UUID,
    val ownerGroup: String,
    var currentTier: StoneTier,
    val location: Location,
    val createdAt: Long = System.currentTimeMillis(),
    var regionName: String? = null
) {
    /**
     * 점령석이 생성된 후 경과한 시간 (초)
     */
    fun getOccupationTime(): Long {
        return (System.currentTimeMillis() - createdAt) / 1000
    }
}

