package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent

class StoneUpgradeListener(private val plugin: Territory_Plugin) : Listener {

    // 플레이어별 열린 GUI의 점령석 UUID 추적
    private val openGuis = mutableMapOf<java.util.UUID, java.util.UUID>()

    @EventHandler(priority = EventPriority.HIGH)
    fun onStoneInteract(event: PlayerInteractEvent) {
        // Check if player is right-clicking a block
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return

        // Check if it's obsidian (occupation stone)
        val stoneMaterial = try {
            Material.valueOf(plugin.configManager.getStoneBlockMaterial())
        } catch (e: Exception) {
            Material.OBSIDIAN
        }

        if (block.type != stoneMaterial) return

        // Try to find stone at this location or nearby (2x2x2 structure)
        val stone = findStoneAtLocation(block.location) ?: return

        // Get player's group
        val playerGroup = getPlayerGroup(event.player.uniqueId.toString())

        // Check if player owns this stone
        if (playerGroup != stone.ownerGroup) {
            event.player.sendMessage("§c이 점령석은 당신의 국가 소유가 아닙니다!")
            event.isCancelled = true
            return
        }

        // Open upgrade GUI and track it
        openGuis[event.player.uniqueId] = stone.stoneUuid
        openUpgradeGUI(event.player, stone)
        event.isCancelled = true
    }

    private fun openUpgradeGUI(player: org.bukkit.entity.Player, stone: kr.skarch.territory_Plugin.models.OccupationStone) {
        val gui = org.bukkit.Bukkit.createInventory(null, 27, "§6점령석 업그레이드")

        // Current tier info
        val currentTierItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.BEACON)
        val currentMeta = currentTierItem.itemMeta
        currentMeta?.setDisplayName("§e현재 티어: ${stone.currentTier.tierName}")
        val occupationTime = stone.getOccupationTime()
        val hours = occupationTime / 3600
        val minutes = (occupationTime % 3600) / 60
        currentMeta?.lore = listOf(
            "§7반경: ${stone.currentTier.radius} 청크",
            "§7영역: ${stone.currentTier.radius * 2 + 1}x${stone.currentTier.radius * 2 + 1}",
            "§7점령 시간: ${hours}시간 ${minutes}분"
        )
        currentTierItem.itemMeta = currentMeta
        gui.setItem(11, currentTierItem)

        // Next tier info (if available)
        val nextTier = stone.currentTier.getNext()
        if (nextTier != null) {
            val nextTierItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND)
            val nextMeta = nextTierItem.itemMeta
            nextMeta?.setDisplayName("§a다음 티어: ${nextTier.tierName}")

            val currentTierNum = stone.currentTier.ordinal + 1
            val nextTierNum = nextTier.ordinal + 1
            val requiredMoney = plugin.configManager.getUpgradeMoney(currentTierNum, nextTierNum)
            val requiredTime = plugin.configManager.getUpgradeOccupationTime(currentTierNum, nextTierNum)
            val requiredHours = requiredTime / 3600
            val requiredMinutes = (requiredTime % 3600) / 60

            val lore = mutableListOf(
                "§7반경: ${nextTier.radius} 청크",
                "§7영역: ${nextTier.radius * 2 + 1}x${nextTier.radius * 2 + 1}",
                "",
                "§e요구사항:"
            )

            if (plugin.configManager.isUpgradeCostEnabled()) {
                val hasEnoughMoney = plugin.server.pluginManager.getPlugin("Vault") != null &&
                                    checkMoney(player, requiredMoney)
                val moneyStatus = if (hasEnoughMoney) "§a✔" else "§c✖"
                lore.add("$moneyStatus §7돈: §6$${requiredMoney}")
            }

            val hasEnoughTime = occupationTime >= requiredTime
            val timeStatus = if (hasEnoughTime) "§a✔" else "§c✖"
            lore.add("$timeStatus §7점령 시간: ${requiredHours}시간 ${requiredMinutes}분")
            lore.add("")
            lore.add("§e클릭하여 업그레이드!")

            nextMeta?.lore = lore
            nextTierItem.itemMeta = nextMeta
            gui.setItem(15, nextTierItem)
        } else {
            val maxTierItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER)
            val maxMeta = maxTierItem.itemMeta
            maxMeta?.setDisplayName("§c최대 티어 도달")
            maxMeta?.lore = listOf("§7더 이상 업그레이드할 수 없습니다")
            maxTierItem.itemMeta = maxMeta
            gui.setItem(15, maxTierItem)
        }

        player.openInventory(gui)
    }

    @EventHandler
    fun onInventoryClick(event: org.bukkit.event.inventory.InventoryClickEvent) {
        if (event.view.title != "§6점령석 업그레이드") return
        event.isCancelled = true

        val player = event.whoClicked as? org.bukkit.entity.Player ?: return
        val clickedItem = event.currentItem ?: return

        if (clickedItem.type == org.bukkit.Material.DIAMOND) {
            // Get the stone UUID that was stored when GUI was opened
            // Check both sources: right-click listener and command
            val stoneUuid = openGuis[player.uniqueId]
                ?: kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap[player.uniqueId]

            if (stoneUuid == null) {
                player.sendMessage("§c점령석 정보를 찾을 수 없습니다. 다시 시도해주세요.")
                player.closeInventory()
                return
            }

            // Get stone from database by UUID (not location!)
            val stone = plugin.databaseManager.getStoneByUuid(stoneUuid)
            if (stone == null) {
                player.sendMessage("§c점령석이 존재하지 않습니다!")
                player.closeInventory()
                openGuis.remove(player.uniqueId)
                return
            }

            val nextTier = stone.currentTier.getNext()
            if (nextTier == null) {
                player.sendMessage("§c최대 티어에 도달했습니다!")
                player.closeInventory()
                openGuis.remove(player.uniqueId)
                kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(player.uniqueId)
                return
            }

            val currentTierNum = stone.currentTier.ordinal + 1
            val nextTierNum = nextTier.ordinal + 1
            val requiredMoney = plugin.configManager.getUpgradeMoney(currentTierNum, nextTierNum)
            val requiredTime = plugin.configManager.getUpgradeOccupationTime(currentTierNum, nextTierNum)

            // Check time requirement
            if (stone.getOccupationTime() < requiredTime) {
                val remainingTime = requiredTime - stone.getOccupationTime()
                val hours = remainingTime / 3600
                val minutes = (remainingTime % 3600) / 60
                player.sendMessage("§c점령 시간이 부족합니다! (남은 시간: ${hours}시간 ${minutes}분)")
                player.closeInventory()
                openGuis.remove(player.uniqueId)
                kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(player.uniqueId)
                return
            }

            // Check money requirement
            if (plugin.configManager.isUpgradeCostEnabled()) {
                if (!checkMoney(player, requiredMoney)) {
                    player.sendMessage("§c돈이 부족합니다! (필요: \$${requiredMoney})")
                    player.closeInventory()
                    openGuis.remove(player.uniqueId)
                    kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(player.uniqueId)
                    return
                }

                if (!takeMoney(player, requiredMoney)) {
                    player.sendMessage("§c돈을 차감하는데 실패했습니다!")
                    player.closeInventory()
                    openGuis.remove(player.uniqueId)
                    kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(player.uniqueId)
                    return
                }
            }

            // Perform upgrade
            if (plugin.territoryManager.upgradeStone(stone)) {
                player.sendMessage("§a점령석이 ${stone.currentTier.tierName}로 업그레이드되었습니다!")
                player.sendMessage("§e반경: ${stone.currentTier.radius} 청크 (${stone.currentTier.radius * 2 + 1}x${stone.currentTier.radius * 2 + 1})")
                player.closeInventory()
                openGuis.remove(player.uniqueId)
                kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(player.uniqueId)
            } else {
                player.sendMessage("§c업그레이드에 실패했습니다!")
                // Refund money if upgrade failed
                if (plugin.configManager.isUpgradeCostEnabled()) {
                    refundMoney(player, requiredMoney)
                }
                player.closeInventory()
                openGuis.remove(player.uniqueId)
                kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(player.uniqueId)
            }
        }
    }

    @EventHandler
    fun onInventoryClose(event: org.bukkit.event.inventory.InventoryCloseEvent) {
        if (event.view.title == "§6점령석 업그레이드") {
            // Clean up tracking when GUI is closed
            openGuis.remove(event.player.uniqueId)
            kr.skarch.territory_Plugin.commands.TerritoryCommand.stoneUpgradeMap.remove(event.player.uniqueId)
        }
    }

    private fun checkMoney(player: org.bukkit.entity.Player, amount: Double): Boolean {
        val vault = plugin.server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)
        return vault?.provider?.has(player, amount) ?: false
    }

    private fun takeMoney(player: org.bukkit.entity.Player, amount: Double): Boolean {
        val vault = plugin.server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)
        val response = vault?.provider?.withdrawPlayer(player, amount)
        return response?.transactionSuccess() ?: false
    }

    private fun refundMoney(player: org.bukkit.entity.Player, amount: Double) {
        val vault = plugin.server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)
        vault?.provider?.depositPlayer(player, amount)
    }

    /**
     * Find occupation stone at or near the clicked location
     * Since stone is 2x2x2, we check nearby blocks
     */
    private fun findStoneAtLocation(clickedLocation: org.bukkit.Location): kr.skarch.territory_Plugin.models.OccupationStone? {
        // Check exact location first
        var stone = plugin.databaseManager.getStoneByLocation(clickedLocation)
        if (stone != null) return stone

        // Check nearby blocks (within 2 blocks range for 2x2x2 structure)
        val x = clickedLocation.blockX
        val y = clickedLocation.blockY
        val z = clickedLocation.blockZ
        val world = clickedLocation.world ?: return null

        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val checkLoc = org.bukkit.Location(world, (x + dx).toDouble(), (y + dy).toDouble(), (z + dz).toDouble())
                    stone = plugin.databaseManager.getStoneByLocation(checkLoc)
                    if (stone != null) return stone
                }
            }
        }

        return null
    }

    private fun getPlayerGroup(uuid: String): String {
        return PlayerGroupCache.getPlayerGroup(java.util.UUID.fromString(uuid))
    }
}

