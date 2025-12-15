package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.models.OccupationStone
import kr.skarch.territory_Plugin.models.StoneTier
import kr.skarch.territory_Plugin.models.TerritoryChunk
import org.bukkit.Location
import org.bukkit.Material
import java.util.UUID

class TerritoryManager(private val plugin: Territory_Plugin) {

    /**
     * Place a new Tier I stone at the specified location
     * Creates a 2x2x2 cube at chunk center
     */
    fun placeStone(location: Location, ownerGroup: String, regionName: String? = null): OccupationStone? {
        val correctedLoc = correctToChunkCenter(location)

        // Check if a stone already exists at this location
        if (plugin.databaseManager.getStoneByLocation(correctedLoc) != null) {
            return null
        }

        // Check if the location is clear (no important blocks)
        if (!isLocationClearForStone(correctedLoc)) {
            plugin.logger.warning("Cannot place stone at ${correctedLoc}: location not clear")
            return null
        }

        val stoneUuid = UUID.randomUUID()
        val stone = OccupationStone(stoneUuid, ownerGroup, StoneTier.TIER_1, correctedLoc, System.currentTimeMillis(), regionName)

        // Place the physical blocks (2x2x2 cube at chunk center)
        placeStoneStructure(correctedLoc)

        plugin.databaseManager.saveStone(stone)
        claimArea(correctedLoc, StoneTier.TIER_1.radius, ownerGroup, stoneUuid)
        plugin.blueMapManager.updateMarkers()

        return stone
    }

    /**
     * Place a new OUTPOST (전초기지) stone at the specified location
     * Creates a 2x2x2 cube at chunk center, claims only 1 chunk, cannot be upgraded
     */
    fun placeOutpost(location: Location, ownerGroup: String, regionName: String? = null): OccupationStone? {
        val correctedLoc = correctToChunkCenter(location)

        // Check if a stone already exists at this location
        if (plugin.databaseManager.getStoneByLocation(correctedLoc) != null) {
            return null
        }

        // Check if the location is clear (no important blocks)
        if (!isLocationClearForStone(correctedLoc)) {
            plugin.logger.warning("Cannot place outpost at ${correctedLoc}: location not clear")
            return null
        }

        val stoneUuid = UUID.randomUUID()
        val stone = OccupationStone(stoneUuid, ownerGroup, StoneTier.OUTPOST, correctedLoc, System.currentTimeMillis(), regionName)

        // Place the physical blocks (2x2x2 cube at chunk center)
        placeStoneStructure(correctedLoc)

        plugin.databaseManager.saveStone(stone)

        // 전초기지는 1청크만 점령 (radius = 0)
        claimArea(correctedLoc, StoneTier.OUTPOST.radius, ownerGroup, stoneUuid)
        plugin.blueMapManager.updateMarkers()

        return stone
    }

    /**
     * Check if the location is clear for stone placement
     * Returns false if there are important blocks that shouldn't be destroyed
     */
    private fun isLocationClearForStone(centerLocation: Location): Boolean {
        val world = centerLocation.world ?: return false
        val baseX = centerLocation.blockX
        val baseY = centerLocation.blockY
        val baseZ = centerLocation.blockZ

        // Check all 8 blocks of the 2x2x2 structure
        for (x in 0..1) {
            for (y in 0..1) {
                for (z in 0..1) {
                    val block = world.getBlockAt(baseX + x, baseY + y, baseZ + z)

                    // Allow placement only if blocks are replaceable
                    if (!isBlockReplaceable(block.type)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    /**
     * Check if a block type is safe to replace
     */
    private fun isBlockReplaceable(type: Material): Boolean {
        // 설치 가능한 블록 타입들 - 자연 블록과 식물들을 모두 교체 가능하도록 설정
        return when (type) {
            Material.AIR,
            Material.CAVE_AIR,
            Material.VOID_AIR,
            Material.GRASS,
            Material.TALL_GRASS,
            Material.FERN,
            Material.LARGE_FERN,
            Material.DEAD_BUSH,
            Material.SNOW,
            Material.WATER,
            Material.LAVA,
            Material.GRASS_BLOCK,
            Material.DIRT,
            Material.COARSE_DIRT,
            Material.PODZOL,
            Material.SAND,
            Material.RED_SAND,
            Material.GRAVEL,
            Material.CLAY,
            Material.STONE,
            Material.GRANITE,
            Material.DIORITE,
            Material.ANDESITE,
            Material.DEEPSLATE,
            Material.TUFF,
            Material.NETHERRACK,
            Material.SOUL_SAND,
            Material.SOUL_SOIL,
            Material.WARPED_NYLIUM,
            Material.CRIMSON_NYLIUM,
            Material.END_STONE,
            Material.SEAGRASS,
            Material.TALL_SEAGRASS,
            Material.KELP,
            Material.KELP_PLANT,
            Material.DANDELION,
            Material.POPPY,
            Material.BLUE_ORCHID,
            Material.ALLIUM,
            Material.AZURE_BLUET,
            Material.RED_TULIP,
            Material.ORANGE_TULIP,
            Material.WHITE_TULIP,
            Material.PINK_TULIP,
            Material.OXEYE_DAISY,
            Material.CORNFLOWER,
            Material.LILY_OF_THE_VALLEY,
            Material.SUNFLOWER,
            Material.LILAC,
            Material.ROSE_BUSH,
            Material.PEONY,
            Material.BROWN_MUSHROOM,
            Material.RED_MUSHROOM,
            Material.SUGAR_CANE,
            Material.VINE,
            Material.GLOW_LICHEN,
            Material.COBBLESTONE,
            Material.MOSSY_COBBLESTONE -> true
            else -> false
        }
    }

    /**
     * Place stone structure (2x2x2 cube of obsidian)
     */
    private fun placeStoneStructure(centerLocation: Location) {
        val world = centerLocation.world ?: return
        val material = try {
            Material.valueOf(plugin.configManager.getStoneBlockMaterial())
        } catch (e: Exception) {
            Material.OBSIDIAN
        }

        val baseX = centerLocation.blockX
        val baseY = centerLocation.blockY
        val baseZ = centerLocation.blockZ

        // Create 2x2x2 cube at chunk center (x: 7-8, z: 7-8, y: baseY to baseY+1)
        for (x in 0..1) {
            for (y in 0..1) {
                for (z in 0..1) {
                    val block = world.getBlockAt(baseX + x, baseY + y, baseZ + z)
                    block.type = material
                }
            }
        }
    }

    /**
     * Remove stone structure
     */
    private fun removeStoneStructure(centerLocation: Location) {
        val world = centerLocation.world ?: return
        val baseX = centerLocation.blockX
        val baseY = centerLocation.blockY
        val baseZ = centerLocation.blockZ

        // Remove 2x2x2 cube
        for (x in 0..1) {
            for (y in 0..1) {
                for (z in 0..1) {
                    val block = world.getBlockAt(baseX + x, baseY + y, baseZ + z)
                    block.type = Material.AIR
                }
            }
        }
    }

    /**
     * Upgrade an existing stone to the next tier
     * Expands territory according to the new radius
     */
    fun upgradeStone(stone: OccupationStone): Boolean {
        val nextTier = stone.currentTier.getNext() ?: return false

        stone.currentTier = nextTier
        plugin.databaseManager.updateStoneTier(stone.stoneUuid, nextTier)
        claimArea(stone.location, nextTier.radius, stone.ownerGroup, stone.stoneUuid)
        plugin.blueMapManager.updateMarkers()

        return true
    }

    /**
     * Claim chunks in a radius around the center location
     * ONLY claims chunks that are currently unclaimed (priority system)
     * 먼저 점령한 팀이 우선권을 가지며, 이미 점령된 청크는 덮어쓰지 않음
     */
    fun claimArea(center: Location, radius: Int, group: String, stoneId: UUID): Int {
        val centerChunk = center.chunk
        val world = center.world ?: return 0

        var claimedCount = 0

        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val chunkX = centerChunk.x + dx
                val chunkZ = centerChunk.z + dz
                val chunkKey = "${world.name};$chunkX;$chunkZ"

                // Priority system: Only claim if chunk is unclaimed
                // 먼저 점령한 팀이 우선권을 가짐 - 겹치는 영역은 기존 소유자 유지
                val existingOwner = plugin.databaseManager.getChunkOwner(chunkKey)
                if (existingOwner == null) {
                    val chunk = TerritoryChunk(chunkKey, group, stoneId)
                    plugin.databaseManager.saveChunk(chunk)
                    claimedCount++
                }
                // If already owned, skip it (do not overwrite)
                // 이미 점령된 청크는 건드리지 않음 - 우선권 시스템
            }
        }

        return claimedCount
    }

    /**
     * Destroy a stone and transfer all its chunks to the new owner
     */
    fun destroyStone(stone: OccupationStone, newOwnerGroup: String) {
        val victimGroup = stone.ownerGroup
        val affectedChunks = plugin.databaseManager.removeChunksByStone(stone.stoneUuid)
        plugin.databaseManager.removeStone(stone.stoneUuid)

        // Notify all online members of the victim nation
        notifyStoneDestruction(stone, newOwnerGroup, affectedChunks.size)

        // Transfer all chunks to new owner
        affectedChunks.forEach { chunkKey ->
            val chunk = TerritoryChunk(chunkKey, newOwnerGroup, stone.stoneUuid)
            plugin.databaseManager.saveChunk(chunk)
        }

        // 전쟁 통계 기록
        plugin.warManager.recordStoneDestruction(newOwnerGroup, victimGroup)
        plugin.warManager.recordTerritoryConquest(newOwnerGroup, victimGroup, affectedChunks.size)

        // Remove physical structure
        removeStoneStructure(stone.location)

        plugin.blueMapManager.updateMarkers()
    }

    /**
     * Notify nation members when their stone is destroyed
     */
    private fun notifyStoneDestruction(stone: OccupationStone, attackerGroup: String, chunksLost: Int) {
        val victimGroup = stone.ownerGroup
        val location = stone.location

        org.bukkit.Bukkit.getOnlinePlayers().forEach { player ->
            val playerGroup = kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)

            when {
                playerGroup == victimGroup -> {
                    // Notify victims
                    player.sendMessage("§c§l⚠ 경고! 점령석이 파괴되었습니다!")
                    player.sendMessage("§e위치: ${location.world?.name} (${location.blockX}, ${location.blockY}, ${location.blockZ})")
                    player.sendMessage("§e공격자: §c$attackerGroup")
                    player.sendMessage("§e잃은 영토: §c$chunksLost 청크")
                    player.playSound(player.location, org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f)
                }
                playerGroup == attackerGroup -> {
                    // Notify attackers
                    player.sendMessage("§a§l✓ 점령석 파괴 성공!")
                    player.sendMessage("§e획득한 영토: §a$chunksLost 청크")
                    player.playSound(player.location, org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f)
                }
            }
        }
    }

    /**
     * Correct a location to the chunk center (x=7, z=7) at configured Y
     * Returns the bottom-left corner of the 2x2 structure
     */
    private fun correctToChunkCenter(location: Location): Location {
        val chunk = location.chunk
        val world = location.world ?: return location

        // Chunk center 4 blocks: (7,7), (7,8), (8,7), (8,8)
        // We use (7,7) as the base coordinate
        val centerX = chunk.x * 16 + 7
        val centerZ = chunk.z * 16 + 7
        val configY = plugin.configManager.getStoneSpawnY()

        return Location(world, centerX.toDouble(), configY.toDouble(), centerZ.toDouble())
    }

    /**
     * Check if a player can interact with a chunk (based on ownership)
     */
    fun canInteractWithChunk(chunkKey: String, playerGroup: String): Boolean {
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)
        return owner == null || owner == playerGroup
    }
}

