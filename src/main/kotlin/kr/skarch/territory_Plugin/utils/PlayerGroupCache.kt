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
    private val CACHE_DURATION_MS = 300000L // 5분 (밀리초)

    fun getPlayerGroup(player: Player): String {
        return getPlayerGroup(player.uniqueId)
    }

    fun getPlayerGroup(uuid: UUID): String {
        val cached = cache[uuid]
        val now = System.currentTimeMillis()

        // Check if cache is valid
        if (cached != null && (now - cached.timestamp) < CACHE_DURATION_MS) {
            return cached.group
        }

        // Fetch from LuckPerms
        val group = try {
            val luckPerms = LuckPermsProvider.get()
            val user = luckPerms.userManager.getUser(uuid)

            if (user == null) {
                "default"
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

                    // Find intersection: player's groups that are registered as nations
                    val validNationGroups = playerGroups.intersect(registeredNations)

                    // Return first valid nation group, or primary group, or default
                    validNationGroups.firstOrNull() ?: user.primaryGroup
                } else {
                    // Plugin not loaded yet, use primary group
                    user.primaryGroup
                }
            }
        } catch (e: Exception) {
            "default"
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
        cache.entries.removeIf { (now - it.value.timestamp) >= CACHE_DURATION_MS }
    }
}

