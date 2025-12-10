package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.models.NationStats
import kr.skarch.territory_Plugin.models.StoneTier
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit

/**
 * 국가 통계 및 랭킹 관리
 */
class StatsManager(private val plugin: Territory_Plugin) {

    /**
     * 특정 국가의 통계 조회
     */
    fun getNationStats(nationName: String): NationStats? {
        val teamId = plugin.configManager.getTeamIdByLuckPermsGroup(nationName) ?: return null
        val displayName = plugin.configManager.getTeamDisplayName(teamId)

        val totalChunks = plugin.databaseManager.getChunkCountByTeam(nationName)
        val stones = plugin.databaseManager.getStonesByTeam(nationName)
        val totalStones = stones.size

        val highestTier = stones.maxOfOrNull { it.currentTier } ?: StoneTier.TIER_1
        val isAtWar = plugin.warManager.isInGlobalWar(nationName)
        val memberCount = getTeamMemberCount(nationName)

        return NationStats(
            nationName = nationName,
            displayName = displayName,
            totalChunks = totalChunks,
            totalStones = totalStones,
            highestTier = highestTier,
            isAtWar = isAtWar,
            memberCount = memberCount
        )
    }

    /**
     * 모든 국가의 통계 조회 (랭킹 순)
     */
    fun getAllNationStats(): List<NationStats> {
        val stats = mutableListOf<NationStats>()

        plugin.configManager.getTeamIds().forEach { teamId ->
            val luckPermsGroup = plugin.configManager.getTeamLuckPermsGroup(teamId)
            getNationStats(luckPermsGroup)?.let { stats.add(it) }
        }

        return stats.sorted() // 점수 순으로 정렬
    }

    /**
     * 국가별 랭킹 (1위부터)
     */
    fun getNationRanking(nationName: String): Int {
        val allStats = getAllNationStats()
        return allStats.indexOfFirst { it.nationName == nationName } + 1
    }

    /**
     * 특정 국가의 모든 점령석 위치 조회
     */
    fun getStoneLocations(nationName: String): List<String> {
        val stones = plugin.databaseManager.getStonesByTeam(nationName)
        return stones.map { stone ->
            "${stone.location.world?.name} (${stone.location.blockX}, ${stone.location.blockY}, ${stone.location.blockZ}) - ${stone.currentTier.tierName}"
        }
    }

    /**
     * 가장 가까운 적 점령석 찾기
     */
    fun findNearestEnemyStone(playerLocation: org.bukkit.Location, playerTeam: String): org.bukkit.Location? {
        val allStones = plugin.databaseManager.getAllStones()

        return allStones
            .filter { it.ownerGroup != playerTeam && it.location.world?.name == playerLocation.world?.name }
            .minByOrNull { it.location.distance(playerLocation) }
            ?.location
    }

    /**
     * 팀 멤버 수 조회
     */
    private fun getTeamMemberCount(nationName: String): Int {
        return try {
            val luckPerms = LuckPermsProvider.get()
            val group = luckPerms.groupManager.getGroup(nationName)

            // 온라인 플레이어 중 해당 그룹 멤버 수
            Bukkit.getOnlinePlayers().count { player ->
                val user = luckPerms.userManager.getUser(player.uniqueId)
                user?.primaryGroup == nationName
            }
        } catch (e: Exception) {
            0
        }
    }
}

