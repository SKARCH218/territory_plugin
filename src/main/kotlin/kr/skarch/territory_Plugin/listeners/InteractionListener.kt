package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.models.PendingStone
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

                        // Record conquest if in war
                        if (plugin.warManager.isInGlobalWar(playerGroup) ||
                            plugin.warManager.isInGlobalWar(stone.ownerGroup)) {
                            plugin.warManager.recordConquest(playerGroup)
                        }

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
                val coloredNation = plugin.configManager.getColoredNationName(owner)
                player.sendMessage("§c이 영토(국가: $coloredNation§c)에서는 블록을 부술 수 없습니다!")
            } else {
                player.sendMessage("§c주인 없는 땅에서는 상호작용할 수 없습니다!")
                player.sendMessage("§e점령석을 설치하여 영토를 점령하세요.")
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block

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
                val coloredNation = plugin.configManager.getColoredNationName(owner)
                player.sendMessage("§c이 영토(국가: $coloredNation§c)에서는 블록을 설치할 수 없습니다!")
            } else {
                player.sendMessage("§c주인 없는 땅에서는 블록을 설치할 수 없습니다!")
                player.sendMessage("§e점령석을 설치하여 먼저 영토를 점령하세요.")
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onOccupationStoneUse(event: PlayerInteractEvent) {
        // 우클릭 체크
        if (event.action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
            event.action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            return
        }

        val player = event.player
        val item = event.item ?: return

        // 점령석 아이템 체크
        if (item.type != Material.PAPER) return
        val meta = item.itemMeta ?: return

        val itemName = plugin.langManager.getItemName("occupation_stone")
        // Check if item has display name and it matches
        if (!meta.hasDisplayName()) return
        @Suppress("DEPRECATION")
        if (meta.displayName != itemName) return

        // 이벤트 취소
        event.isCancelled = true

        // 전쟁 월드 체크
        if (!plugin.configManager.isWarWorld(player.world.name)) {
            player.sendMessage("§c이 월드에서는 점령석을 설치할 수 없습니다!")
            return
        }

        val playerGroup = getPlayerGroup(player)
        val location = player.location

        // 현재 청크의 소유자 확인
        val chunkKey = "${location.world.name};${location.chunk.x};${location.chunk.z}"
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)

        // OP가 아닌 경우 체크
        if (!player.isOp) {
            if (owner != null) {
                // 주인 있는 땅: 전쟁 시에만 점령 가능
                val isAnyWarActive = plugin.warManager.isInGlobalWar(playerGroup) ||
                                     plugin.warManager.isInGlobalWar(owner)

                if (!isAnyWarActive) {
                    player.sendMessage("§c비전쟁 시에는 다른 국가의 땅을 점령할 수 없습니다!")
                    player.sendMessage("§e전쟁을 선포하거나 주인 없는 땅을 찾아보세요.")
                    return
                }
            }
            // 주인 없는 땅은 항상 허용
        }

        // 지역 이름 입력 요청
        player.sendMessage("§6===========================================")
        player.sendMessage("§a§l점령석 설치")
        player.sendMessage("§e이 영토의 이름을 채팅창에 입력하세요.")
        player.sendMessage("§7(예: 중앙기지, 북부요새, 동쪽광산 등)")
        player.sendMessage("§6===========================================")

        // 대기 중인 플레이어 저장 (플러그인에 Map 저장)
        plugin.pendingRegionNames[player.uniqueId] = PendingStone(location, playerGroup, item)
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
                val coloredNation = plugin.configManager.getColoredNationName(owner)
                player.sendMessage("§c이 영토(국가: $coloredNation§c)에서는 상호작용할 수 없습니다!")
            } else {
                player.sendMessage("§c주인 없는 땅에서는 상호작용할 수 없습니다!")
                player.sendMessage("§e점령석을 설치하여 먼저 영토를 점령하세요.")
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

    @EventHandler
    fun onPlayerDeath(event: org.bukkit.event.entity.PlayerDeathEvent) {
        val victim = event.entity
        val killer = victim.killer ?: return

        val killerGroup = getPlayerGroup(killer)
        val victimGroup = getPlayerGroup(victim)

        // Record kill if in war and different nations
        if (killerGroup != victimGroup) {
            if (plugin.warManager.isInGlobalWar(killerGroup) ||
                plugin.warManager.isInGlobalWar(victimGroup)) {
                plugin.warManager.recordKill(killerGroup)
            }
        }
    }

    /**
     * Check if a player can interact with a territory
     * OP: 항상 가능
     * 비전쟁 시: 본인 땅만 상호작용, 주인 없는 땅은 점령만 가능 (상호작용 불가)
     * 전쟁 시: 모든 땅 상호작용 가능
     */
    private fun canInteractWithTerritory(player: Player, owner: String?, playerGroup: String = getPlayerGroup(player)): Boolean {
        // OP는 항상 허용
        if (player.isOp) {
            return true
        }

        // Check if any nation is in war
        val isAnyWarActive = plugin.warManager.isInGlobalWar(playerGroup) ||
                             (owner != null && plugin.warManager.isInGlobalWar(owner))

        return if (isAnyWarActive) {
            // 전쟁 중: 모든 땅 상호작용 가능
            true
        } else {
            // 비전쟁 시
            if (owner == null) {
                // 주인 없는 땅: 점령석 설치만 가능, 일반 상호작용 불가
                false
            } else {
                // 주인 있는 땅: 본인 팀만 가능
                owner == playerGroup
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

