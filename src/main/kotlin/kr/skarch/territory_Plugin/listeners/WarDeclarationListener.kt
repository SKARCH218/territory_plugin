package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class WarDeclarationListener(private val plugin: Territory_Plugin) : Listener {

    private val pendingConfirmations = ConcurrentHashMap<UUID, PendingDeclaration>()

    data class PendingDeclaration(val nationName: String, val timestamp: Long)

    @EventHandler(priority = EventPriority.HIGH)
    fun onWarScrollUse(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (item.type != Material.PAPER || !item.hasItemMeta()) return

        val itemMeta = item.itemMeta ?: return
        if (itemMeta.displayName != "§cWar Declaration Scroll") return

        val player = event.player

        // Check if player has permission
        if (!player.hasPermission("territory.war.declare")) {
            player.sendMessage("§c전쟁을 선포할 권한이 없습니다!")
            event.isCancelled = true
            return
        }

        // Check if already in war
        if (plugin.warManager.isGlobalWarActive()) {
            player.sendMessage("§c이미 글로벌 전쟁이 진행 중입니다!")
            event.isCancelled = true
            return
        }

        // Check cooldown
        val remainingCooldown = plugin.databaseManager.getRemainingCooldown("GLOBAL", plugin.configManager.getWarDeclarationCooldown())
        if (remainingCooldown > 0) {
            val hours = remainingCooldown / 3600
            val minutes = (remainingCooldown % 3600) / 60
            player.sendMessage("§c전쟁 선포 쿨타임이 남아있습니다! (${hours}시간 ${minutes}분)")
            event.isCancelled = true
            return
        }

        // Check cost
        if (plugin.configManager.isWarDeclarationCostEnabled()) {
            val cost = plugin.configManager.getWarDeclarationCost()
            val economy = plugin.server.servicesManager.getRegistration(
                net.milkbowl.vault.economy.Economy::class.java
            )?.provider

            if (economy != null) {
                if (economy.getBalance(player) < cost) {
                    player.sendMessage("§c전쟁 선포 비용이 부족합니다! (필요: ${cost}원)")
                    event.isCancelled = true
                    return
                }
            }
        }

        // Store pending confirmation
        pendingConfirmations[player.uniqueId] = PendingDeclaration("GLOBAL", System.currentTimeMillis())

        // Get next war number for display
        val nextWarNumber = plugin.databaseManager.getCurrentWarNumber() + 1

        // Send clickable confirmation message
        val message = Component.text("제 ${nextWarNumber}차 글로벌 전면전을 시작하시겠습니까? ", NamedTextColor.RED)
            .append(Component.text("모든 국가가 참여합니다!", NamedTextColor.YELLOW))
            .append(Component.text("\n"))
            .append(
                Component.text("[YES]", NamedTextColor.GREEN)
                    .clickEvent(ClickEvent.runCommand("/war-confirm yes"))
            )
            .append(Component.text(" / "))
            .append(
                Component.text("[NO]", NamedTextColor.RED)
                    .clickEvent(ClickEvent.runCommand("/war-confirm no"))
            )

        player.sendMessage(message)
        player.sendMessage("§e경고: 전면전이 선포되면 10분 후 모든 국가와 전쟁 상태가 됩니다!")
        player.sendMessage("§e당신의 국가는 다른 모든 국가를 공격할 수 있지만, 동시에 모든 국가로부터 공격받을 수 있습니다!")

        event.isCancelled = true
    }

    fun confirmWar(player: Player, confirm: Boolean) {
        val pending = pendingConfirmations.remove(player.uniqueId)

        if (pending == null) {
            player.sendMessage("§c확인할 전쟁 선포가 없습니다!")
            return
        }

        // Check if expired (30 seconds)
        if (System.currentTimeMillis() - pending.timestamp > 30000) {
            player.sendMessage("§c전쟁 선포 확인 시간이 만료되었습니다!")
            return
        }

        if (confirm) {
            // Charge cost
            if (plugin.configManager.isWarDeclarationCostEnabled()) {
                val cost = plugin.configManager.getWarDeclarationCost()
                val economy = plugin.server.servicesManager.getRegistration(
                    net.milkbowl.vault.economy.Economy::class.java
                )?.provider

                if (economy != null) {
                    if (economy.getBalance(player) < cost) {
                        player.sendMessage("§c전쟁 선포 비용이 부족합니다! (필요: ${cost}원)")
                        return
                    }
                    economy.withdrawPlayer(player, cost)
                    player.sendMessage("§e전쟁 선포 비용 ${cost}원이 차감되었습니다.")
                }
            }

            // 글로벌 전쟁 선포
            plugin.warManager.declareGlobalWar()

            val prepTime = plugin.configManager.getWarPreparationTime()
            player.sendMessage("§c글로벌 전면전이 선포되었습니다! ${prepTime/60}분 후 모든 국가가 전쟁 상태로 돌입합니다!")
        } else {
            player.sendMessage("§a전쟁 선포가 취소되었습니다.")
        }
    }

    private fun getPlayerGroup(player: Player): String {
        return PlayerGroupCache.getPlayerGroup(player)
    }
}

