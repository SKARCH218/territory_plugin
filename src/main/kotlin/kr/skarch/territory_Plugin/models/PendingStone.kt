package kr.skarch.territory_Plugin.models

import org.bukkit.Location
import org.bukkit.inventory.ItemStack

/**
 * 점령석 설치 대기 중인 정보
 */
data class PendingStone(
    val location: Location,
    val ownerGroup: String,
    val item: ItemStack,
    val isOutpost: Boolean = false  // 전초기지 여부
)

