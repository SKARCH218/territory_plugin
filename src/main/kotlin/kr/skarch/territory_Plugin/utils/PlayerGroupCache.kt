package kr.skarch.territory_Plugin.utils

import kr.skarch.territory_Plugin.Territory_Plugin
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * LuckPerms 그룹 조회를 캐싱하여 성능 향상
 * 플레이어가 여러 그룹을 가진 경우 team.yml에 등록된 그룹을 우선 선택
 */
object PlayerGroupCache {

    private data class CachedGroup(
        val group: String,
        val timestamp: Long
    )

    private val cache = ConcurrentHashMap<UUID, CachedGroup>()
    private var cacheDurationMs = 300000L // 기본값 5분, 설정값으로 오버라이드됨

    fun getPlayerGroup(player: Player): String {
        return getPlayerGroup(player.uniqueId)
    }

    /**
     * Initialize cache with config values
     */
    fun initialize(plugin: Territory_Plugin) {
        cacheDurationMs = plugin.configManager.getPlayerGroupCacheTTL()

        // Schedule cleanup task
        val cleanupInterval = plugin.configManager.getCacheCleanupInterval()
        Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable { cleanupExpired() },
            cleanupInterval / 50, // Convert ms to ticks
            cleanupInterval / 50
        )
    }

    fun getPlayerGroup(uuid: UUID): String {
        val cached = cache[uuid]
        val now = System.currentTimeMillis()

        // Check if cache is valid
        if (cached != null && (now - cached.timestamp) < cacheDurationMs) {
            return cached.group
        }

        // Fetch from LuckPerms
        val group = try {
            val luckPerms = LuckPermsProvider.get()
            val user = luckPerms.userManager.getUser(uuid)

            if (user == null) {
                "팀없음"
            } else {
                // Get Territory Plugin instance
                val plugin = Bukkit.getPluginManager().getPlugin("territory_Plugin") as? Territory_Plugin

                if (plugin != null) {
                    // Get all groups the player has
                    val playerGroups = user.nodes
                        .filter { it.key.startsWith("group.") }
                        .map { it.key.removePrefix("group.") }
                        .toSet()

                    // Get all registered nation groups from team.yml
                    val registeredNations = plugin.configManager.getTeamIds()
                        .map { plugin.configManager.getTeamLuckPermsGroup(it) }
                        .toSet()

                    // Filter player's groups to only those registered in team.yml
                    val validNationGroups = playerGroups.filter { it in registeredNations }

                    // Return first nation group according to team.yml order
                    // This ensures that if player has multiple groups (e.g., "admin" and "korea"),
                    // only the groups registered in team.yml will be considered as nations
                    val firstMatch = if (validNationGroups.isNotEmpty()) {
                        plugin.configManager.getTeamIds()
                            .map { plugin.configManager.getTeamLuckPermsGroup(it) }
                            .firstOrNull { it in validNationGroups }
                    } else {
                        null
                    }

                    firstMatch ?: "팀없음"
                } else {
                    // Plugin not loaded yet
                    "팀없음"
                }
            }
        } catch (e: Exception) {
            "팀없음"
        }

        // Update cache
        cache[uuid] = CachedGroup(group, now)

        return group
    }

    fun invalidate(uuid: UUID) {
        cache.remove(uuid)
    }

    fun invalidateAll() {
        cache.clear()
    }

    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { (now - it.value.timestamp) >= cacheDurationMs }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): Map<String, Int> {
        return mapOf(
            "size" to cache.size,
            "expired" to cache.count { (System.currentTimeMillis() - it.value.timestamp) >= cacheDurationMs }
        )
    }
}
