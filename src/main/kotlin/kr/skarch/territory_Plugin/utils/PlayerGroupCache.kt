package kr.skarch.territory_Plugin.utils

import net.luckperms.api.LuckPermsProvider
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * LuckPerms 그룹 조회를 캐싱하여 성능 향상
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
            user?.primaryGroup ?: "default"
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

