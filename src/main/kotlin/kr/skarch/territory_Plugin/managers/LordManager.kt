package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * 영주 시스템 관리자
 * 영주 전용 혜택 및 기능 제공
 */
class LordManager(private val plugin: Territory_Plugin) {

    /**
     * 플레이어가 영주인지 확인
     */
    fun isLord(player: Player): Boolean {
        return plugin.configManager.isLordOfAnyTeam(player.name)
    }

    /**
     * 플레이어가 특정 팀의 영주인지 확인
     */
    fun isLordOfTeam(player: Player, teamId: String): Boolean {
        return plugin.configManager.isLord(player.name, teamId)
    }

    /**
     * 영주 버프 적용
     * 영주는 항상 특수 효과를 받습니다
     */
    fun applyLordBonuses(player: Player) {
        if (!isLord(player)) return

        // 영주 전용 버프
        // 신속 II (항상)
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 20 * 15, 1, false, false, true))

        // 재생 I (항상)
        player.addPotionEffect(PotionEffect(PotionEffectType.REGENERATION, 20 * 15, 0, false, false, true))

        // 힘 I (자기 영토에서만)
        val location = player.location
        val chunkKey = "${location.world.name};${location.chunk.x};${location.chunk.z}"
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)
        val playerGroup = kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)

        if (owner == playerGroup) {
            // Minecraft 1.20+에서는 STRENGTH 대신 INCREASE_DAMAGE 사용
            val strengthEffect = PotionEffectType.getByName("STRENGTH")
                ?: PotionEffectType.getByName("INCREASE_DAMAGE")

            strengthEffect?.let {
                player.addPotionEffect(PotionEffect(it, 20 * 15, 0, false, false, true))
            }
        }
    }

    /**
     * 영주 전용 권한 확인
     * - 전쟁 선포 우선권
     * - 점령석 업그레이드 할인
     * - 특수 명령어 사용
     */
    fun hasLordPrivilege(player: Player, privilege: LordPrivilege): Boolean {
        if (!isLord(player)) return false

        return when (privilege) {
            LordPrivilege.WAR_DECLARATION -> true
            LordPrivilege.UPGRADE_DISCOUNT -> true
            LordPrivilege.TELEPORT_STONES -> true
            LordPrivilege.VIEW_ALL_STATS -> true
            LordPrivilege.MANAGE_TERRITORY -> true
        }
    }

    /**
     * 영주 업그레이드 할인율 (0.0 ~ 1.0)
     */
    fun getUpgradeDiscount(player: Player): Double {
        return if (isLord(player)) 0.2 else 0.0  // 20% 할인
    }

    /**
     * 모든 온라인 영주에게 메시지 전송
     */
    fun broadcastToLords(message: String) {
        Bukkit.getOnlinePlayers()
            .filter { isLord(it) }
            .forEach { it.sendMessage(message) }
    }

    /**
     * 특정 팀의 온라인 영주에게 메시지 전송
     */
    fun broadcastToTeamLords(teamId: String, message: String) {
        Bukkit.getOnlinePlayers()
            .filter { isLordOfTeam(it, teamId) }
            .forEach { it.sendMessage(message) }
    }

    /**
     * 영주 목록 조회 (특정 팀)
     */
    fun getTeamLords(teamId: String): List<String> {
        return plugin.configManager.getTeamLords(teamId)
    }

    /**
     * 온라인 영주 수 (특정 팀)
     */
    fun getOnlineLordCount(teamId: String): Int {
        return Bukkit.getOnlinePlayers()
            .count { isLordOfTeam(it, teamId) }
    }

    /**
     * 영주가 점령석을 관리할 수 있는지 확인
     */
    fun canManageStone(player: Player, stoneOwnerGroup: String): Boolean {
        if (!isLord(player)) return false

        val playerGroup = kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)
        return playerGroup == stoneOwnerGroup
    }
}

/**
 * 영주 특권 종류
 */
enum class LordPrivilege {
    WAR_DECLARATION,      // 전쟁 선포
    UPGRADE_DISCOUNT,     // 업그레이드 할인
    TELEPORT_STONES,      // 점령석 텔레포트
    VIEW_ALL_STATS,       // 전체 통계 조회
    MANAGE_TERRITORY      // 영토 관리
}

