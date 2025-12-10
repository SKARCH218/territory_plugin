package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent

class InteractionListener(private val plugin: Territory_Plugin) : Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val block = event.block

        // Check if in war world
        if (!plugin.configManager.isWarWorld(block.world.name)) {
            return // Not in war world, allow all actions
        }

        val chunkKey = "${block.world.name};${block.chunk.x};${block.chunk.z}"
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)

        // Check if breaking an occupation stone (check obsidian block)
        val stoneMaterial = try {
            Material.valueOf(plugin.configManager.getStoneBlockMaterial())
        } catch (e: Exception) {
            Material.OBSIDIAN
        }

        val stone = if (block.type == stoneMaterial) {
            findStoneAtLocation(block.location)
        } else null

        if (stone != null) {
            val playerGroup = getPlayerGroup(player)

            // Check if player can attack this stone
            if (canInteractWithTerritory(player, stone.ownerGroup)) {
                // Enemy is breaking the stone - conquest!
                if (playerGroup != stone.ownerGroup) {
                    // Cancel the event first to prevent normal block break
                    event.isCancelled = true

                    // Re-verify stone still exists (prevent race condition)
                    // Check using findStoneAtLocation to handle 2x2x2 structure
                    val verifyStone = findStoneAtLocation(block.location)
                    if (verifyStone != null && verifyStone.stoneUuid == stone.stoneUuid) {
                        // Destroy the stone (this removes all 8 blocks and transfers territory)
                        plugin.territoryManager.destroyStone(stone, playerGroup)
                        player.sendMessage("§a점령석을 파괴하고 영토를 점령했습니다!")

                        // Play break effect
                        block.world.createExplosion(block.location, 0f, false, false)
                    } else {
                        player.sendMessage("§c이 점령석은 이미 파괴되었습니다!")
                    }
                    return
                } else {
                    // Owner is trying to break their own stone
                    if (player.hasPermission("territory.admin")) {
                        plugin.territoryManager.destroyStone(stone, "none")
                        player.sendMessage("§e점령석을 제거했습니다.")
                        event.isDropItems = false
                        return
                    } else {
                        event.isCancelled = true
                        player.sendMessage("§c자신의 점령석은 파괴할 수 없습니다!")
                        return
                    }
                }
            } else {
                event.isCancelled = true
                player.sendMessage("§c전쟁 중이 아니면 점령석을 파괴할 수 없습니다!")
                return
            }
        }

        // Check territory protection
        val playerGroup = getPlayerGroup(player)

        if (!canInteractWithTerritory(player, owner, playerGroup)) {
            event.isCancelled = true
            if (owner != null) {
                player.sendMessage("§c이 영토(국가: $owner)에서는 블록을 부술 수 없습니다!")
            } else {
                player.sendMessage("§c주인 없는 땅에서는 블록을 부술 수 없습니다!")
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block

        // Check if placing an occupation stone (by item name)
        val itemMeta = event.itemInHand.itemMeta
        if (itemMeta?.displayName == "§6Occupation Stone (Tier I)") {
            // Always cancel the original block placement
            event.isCancelled = true

            // Check if in war world
            if (!plugin.configManager.isWarWorld(block.world.name)) {
                player.sendMessage("§c이 월드에서는 점령석을 설치할 수 없습니다!")
                return
            }

            val playerGroup = getPlayerGroup(player)
            val stone = plugin.territoryManager.placeStone(block.location, playerGroup)

            if (stone != null) {
                player.sendMessage("§a점령석을 설치했습니다!")
                player.sendMessage("§e국가: $playerGroup")
                player.sendMessage("§e티어: ${stone.currentTier.tierName}")

                // Remove item from hand
                if (event.itemInHand.amount > 1) {
                    event.itemInHand.amount--
                } else {
                    player.inventory.setItemInMainHand(null)
                }
            } else {
                player.sendMessage("§c이 위치에는 점령석을 설치할 수 없습니다!")
            }
            return
        }

        // Check if in war world
        if (!plugin.configManager.isWarWorld(block.world.name)) {
            return // Not in war world, allow all actions
        }

        val chunkKey = "${block.world.name};${block.chunk.x};${block.chunk.z}"
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)
        val playerGroup = getPlayerGroup(player)

        // Check territory protection
        if (!canInteractWithTerritory(player, owner, playerGroup)) {
            event.isCancelled = true
            if (owner != null) {
                player.sendMessage("§c이 영토(국가: $owner)에서는 블록을 설치할 수 없습니다!")
            } else {
                player.sendMessage("§c주인 없는 땅에서는 블록을 설치할 수 없습니다!")
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return

        // Check if in war world
        if (!plugin.configManager.isWarWorld(block.world.name)) {
            return // Not in war world, allow all actions
        }

        val chunkKey = "${block.world.name};${block.chunk.x};${block.chunk.z}"
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)
        val player = event.player
        val playerGroup = getPlayerGroup(player)

        if (!canInteractWithTerritory(player, owner, playerGroup)) {
            event.isCancelled = true
            if (owner != null) {
                player.sendMessage("§c이 영토(국가: $owner)에서는 상호작용할 수 없습니다!")
            } else {
                player.sendMessage("§c주인 없는 땅에서는 상호작용할 수 없습니다!")
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPvP(event: EntityDamageByEntityEvent) {
        if (event.damager !is Player || event.entity !is Player) return

        val attacker = event.damager as Player
        val defender = event.entity as Player

        val attackerGroup = getPlayerGroup(attacker)
        val defenderGroup = getPlayerGroup(defender)

        // Same nation cannot attack each other (if config enabled)
        if (plugin.configManager.isBlockFriendlyFire() && attackerGroup == defenderGroup) {
            event.isCancelled = true
            return
        }

        // Check if war is active
        if (!plugin.warManager.canEngage(attackerGroup, defenderGroup)) {
            event.isCancelled = true
            attacker.sendMessage("§c전쟁 중이 아니면 공격할 수 없습니다!")
        }
    }

    /**
     * Check if a player can interact with a territory
     * 전쟁 중이 아닐 때: 본인 팀 땅만 상호작용 가능, 주인 없는 땅 상호작용 불가
     * 전쟁 중일 때: 모든 땅 상호작용 가능
     */
    private fun canInteractWithTerritory(player: Player, owner: String?, playerGroup: String = getPlayerGroup(player)): Boolean {
        // Check if any nation is in war
        val isAnyWarActive = plugin.warManager.isInGlobalWar(playerGroup) ||
                             (owner != null && plugin.warManager.isInGlobalWar(owner))

        return if (isAnyWarActive) {
            // 전쟁 중: 모든 땅 상호작용 가능
            plugin.configManager.isWarAllLandsAccessible()
        } else {
            // 평화 시
            if (owner == null) {
                // 주인 없는 땅
                !plugin.configManager.isPeacefulUnclaimedDeny()
            } else {
                // 주인 있는 땅: 본인 팀만 가능
                if (plugin.configManager.isPeacefulOwnTeamOnly()) {
                    owner == playerGroup
                } else {
                    true
                }
            }
        }
    }

    /**
     * Overloaded version for checking with just owner
     */
    private fun canInteractWithTerritory(player: Player, owner: String?): Boolean {
        return canInteractWithTerritory(player, owner, getPlayerGroup(player))
    }

    /**
     * Find occupation stone at or near the clicked location
     * Checks 2x2x2 cube area to find the stone base location
     */
    private fun findStoneAtLocation(clickedLocation: org.bukkit.Location): kr.skarch.territory_Plugin.models.OccupationStone? {
        val x = clickedLocation.blockX
        val y = clickedLocation.blockY
        val z = clickedLocation.blockZ
        val world = clickedLocation.world ?: return null

        // Check nearby blocks (within 2 blocks range for 2x2x2 structure)
        // The stone base is stored at (chunk*16+7, configY, chunk*16+7)
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    val checkLoc = org.bukkit.Location(
                        world,
                        (x + dx).toDouble(),
                        (y + dy).toDouble(),
                        (z + dz).toDouble()
                    )
                    val stone = plugin.databaseManager.getStoneByLocation(checkLoc)
                    if (stone != null) return stone
                }
            }
        }

        return null
    }

    private fun getPlayerGroup(player: Player): String {
        return PlayerGroupCache.getPlayerGroup(player)
    }
}

