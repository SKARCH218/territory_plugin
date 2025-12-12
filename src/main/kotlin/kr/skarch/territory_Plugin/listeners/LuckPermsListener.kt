package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.EventBus
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.node.NodeAddEvent
import net.luckperms.api.event.node.NodeRemoveEvent
import net.luckperms.api.event.user.UserDataRecalculateEvent

/**
 * Listens to LuckPerms events to invalidate player group cache
 * Ensures cache is always up-to-date when permissions change
 */
class LuckPermsListener(private val plugin: Territory_Plugin, private val luckPerms: LuckPerms) {

    private val subscriptions = mutableListOf<EventSubscription<*>>()

    fun register() {
        val eventBus: EventBus = luckPerms.eventBus

        // Listen for group changes
        eventBus.subscribe(plugin, NodeAddEvent::class.java) { event ->
            if (event.isUser && event.node.key.startsWith("group.")) {
                val userId = (event.target as? net.luckperms.api.model.user.User)?.uniqueId
                if (userId != null) {
                    PlayerGroupCache.invalidate(userId)
                    plugin.logger.info("Invalidated group cache for user $userId (group added)")
                }
            }
        }.also { subscriptions.add(it) }

        eventBus.subscribe(plugin, NodeRemoveEvent::class.java) { event ->
            if (event.isUser && event.node.key.startsWith("group.")) {
                val userId = (event.target as? net.luckperms.api.model.user.User)?.uniqueId
                if (userId != null) {
                    PlayerGroupCache.invalidate(userId)
                    plugin.logger.info("Invalidated group cache for user $userId (group removed)")
                }
            }
        }.also { subscriptions.add(it) }

        // Listen for data recalculation
        eventBus.subscribe(plugin, UserDataRecalculateEvent::class.java) { event ->
            PlayerGroupCache.invalidate(event.user.uniqueId)
            plugin.logger.info("Invalidated group cache for user ${event.user.uniqueId} (data recalculated)")
        }.also { subscriptions.add(it) }

        plugin.logger.info("LuckPerms event listeners registered successfully")
    }

    fun unregister() {
        subscriptions.forEach { it.close() }
        subscriptions.clear()
        plugin.logger.info("LuckPerms event listeners unregistered")
    }
}
