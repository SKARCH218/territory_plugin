package kr.skarch.territory_Plugin.integrations

import kr.skarch.territory_Plugin.Territory_Plugin
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player

class TerritoryPlaceholderExpansion(private val plugin: Territory_Plugin) : PlaceholderExpansion() {

    override fun getIdentifier(): String = "territory"

    override fun getAuthor(): String = "skarch"

    override fun getVersion(): String = plugin.description.version

    override fun persist(): Boolean = true

    override fun onPlaceholderRequest(player: Player?, params: String): String? {
        if (player == null) {
            return handleGlobalPlaceholders(params)
        }

        return when (params.lowercase()) {
            // 플레이어의 국가명
            "team" -> getPlayerTeam(player)

            // 플레이어의 국가 표시 이름
            "team_display" -> {
                val team = getPlayerTeam(player)
                plugin.configManager.getTeamIdByLuckPermsGroup(team)?.let {
                    plugin.configManager.getTeamDisplayName(it)
                } ?: team
            }

            // 플레이어 국가가 전쟁 중인지
            "team_in_war" -> {
                val team = getPlayerTeam(player)
                if (plugin.warManager.isInGlobalWar(team)) "예" else "아니오"
            }

            // 글로벌 전쟁 상태
            "global_war_active" -> {
                if (plugin.warManager.isGlobalWarActive()) "예" else "아니오"
            }

            // 전쟁 종료까지 남은 시간 (초)
            "war_time_remaining" -> {
                plugin.warManager.getWarTimeRemaining()?.toString() ?: "0"
            }

            // 전쟁 종료까지 남은 시간 (MM:SS 포맷)
            "war_time_remaining_formatted" -> {
                val remaining = plugin.warManager.getWarTimeRemaining() ?: 0
                formatTime(remaining)
            }

            // 전쟁 종료까지 남은 시간 (HH:MM:SS 포맷)
            "war_time_remaining_full" -> {
                val remaining = plugin.warManager.getWarTimeRemaining() ?: 0
                formatTimeFull(remaining)
            }

            // 현재 위치 청크 소유자
            "chunk_owner" -> {
                val chunkKey = "${player.world.name};${player.chunk.x};${player.chunk.z}"
                plugin.databaseManager.getChunkOwner(chunkKey) ?: "없음"
            }

            // 현재 위치 청크 소유자 표시 이름
            "chunk_owner_display" -> {
                val chunkKey = "${player.world.name};${player.chunk.x};${player.chunk.z}"
                val owner = plugin.databaseManager.getChunkOwner(chunkKey) ?: return "없음"
                plugin.configManager.getTeamIdByLuckPermsGroup(owner)?.let {
                    plugin.configManager.getTeamDisplayName(it)
                } ?: owner
            }

            // 플레이어가 소유한 청크 수
            "owned_chunks" -> {
                val team = getPlayerTeam(player)
                plugin.databaseManager.getChunkCountByTeam(team).toString()
            }

            else -> null
        }
    }

    private fun handleGlobalPlaceholders(params: String): String? {
        return when {
            // 특정 팀의 전쟁 상태: %territory_war_<팀명>%
            params.startsWith("war_") -> {
                val teamName = params.substring(4)
                if (plugin.warManager.isInGlobalWar(teamName)) "전쟁 중" else "평화"
            }


            // 전체 팀 수
            params == "total_teams" -> {
                plugin.configManager.getTeamIds().size.toString()
            }

            // 전쟁 중인 팀 수
            params == "teams_at_war" -> {
                plugin.configManager.getTeamIds().count {
                    plugin.warManager.isInGlobalWar(plugin.configManager.getTeamLuckPermsGroup(it))
                }.toString()
            }

            else -> null
        }
    }

    private fun getPlayerTeam(player: Player): String {
        return kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)
    }

    private fun formatTime(seconds: Int): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format("%02d:%02d", minutes, secs)
    }

    private fun formatTimeFull(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}

