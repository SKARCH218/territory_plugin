package kr.skarch.territory_Plugin.managers

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapMap
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.math.Color
import de.bluecolored.bluemap.api.math.Shape
import com.flowpowered.math.vector.Vector2d
import com.flowpowered.math.vector.Vector3d
import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.Bukkit

class BlueMapManager(private val plugin: Territory_Plugin) {

    private var blueMapAPI: BlueMapAPI? = null
    private var isEnabled = false
    private val markerSetId = "territory_claims"
    private val markerSetLabel = "Territory Claims"

    init {
        // Try to detect and initialize BlueMap
        if (Bukkit.getPluginManager().getPlugin("BlueMap") != null) {
            BlueMapAPI.onEnable { api ->
                blueMapAPI = api
                isEnabled = true
                plugin.logger.info("BlueMap API connected! Territory markers enabled.")

                // Update markers on enable
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    updateMarkers()
                }, 20L) // Wait 1 second for everything to load
            }

            BlueMapAPI.onDisable { api ->
                blueMapAPI = null
                isEnabled = false
                plugin.logger.info("BlueMap API disconnected.")
            }
        } else {
            plugin.logger.info("BlueMap not detected. Map integration disabled.")
        }
    }

    /**
     * Update all territory markers on BlueMap
     */
    fun updateMarkers() {
        val api = blueMapAPI
        if (!isEnabled || api == null) {
            plugin.logger.warning("BlueMap not enabled, skipping marker update")
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                plugin.logger.info("Starting BlueMap marker update...")

                // Get all territories from database
                val territories = plugin.databaseManager.getAllTerritories()
                plugin.logger.info("Found ${territories.size} territories to display")

                // Group chunks by world and owner
                val groupedByWorld = mutableMapOf<String, MutableMap<String, MutableList<ChunkCoord>>>()

                territories.forEach { (chunkKey, owner) ->
                    val parts = chunkKey.split(";")
                    if (parts.size == 3) {
                        val worldName = parts[0]
                        val chunkX = parts[1].toIntOrNull() ?: return@forEach
                        val chunkZ = parts[2].toIntOrNull() ?: return@forEach

                        groupedByWorld
                            .computeIfAbsent(worldName) { mutableMapOf() }
                            .computeIfAbsent(owner) { mutableListOf() }
                            .add(ChunkCoord(worldName, chunkX, chunkZ))
                    }
                }

                // Update markers for each world
                api.worlds.forEach { blueWorld ->
                    val worldId = blueWorld.id
                    // Extract world name from BlueMap world ID (e.g., "world#minecraft:overworld" -> "world")
                    val worldName = worldId.split("#").firstOrNull() ?: worldId

                    // Skip if not a war world
                    if (!plugin.configManager.isWarWorld(worldName)) {
                        plugin.logger.info("Skipping non-war world: $worldName")
                        return@forEach
                    }

                    plugin.logger.info("Processing BlueMap world: $worldId (mapped to: $worldName)")

                    blueWorld.maps.forEach { map ->
                        val mapId = map.id
                        plugin.logger.info("  Processing map: $mapId")

                        // Get or create marker set
                        val markerSet = map.markerSets[markerSetId] ?: MarkerSet.builder()
                            .label(markerSetLabel)
                            .build()
                            .also {
                                map.markerSets[markerSetId] = it
                                plugin.logger.info("    Created new marker set: $markerSetId")
                            }

                        // Clear existing markers
                        markerSet.markers.clear()

                        // Add markers for this world (use extracted world name)
                        val worldGroups = groupedByWorld[worldName]
                        if (worldGroups != null) {
                            plugin.logger.info("    Found ${worldGroups.size} territories for $worldName")
                            worldGroups.forEach { (owner, chunks) ->
                                createTerritoryMarker(markerSet, owner, chunks, map)
                            }
                            plugin.logger.info("    Added ${worldGroups.size} territory markers")
                        } else {
                            plugin.logger.info("    No territories for this world (worldName: $worldName)")
                            plugin.logger.info("    Available worlds: ${groupedByWorld.keys.joinToString()}")
                        }
                    }
                }

                plugin.logger.info("BlueMap markers updated successfully!")
            } catch (e: Exception) {
                plugin.logger.severe("Failed to update BlueMap markers: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    /**
     * Create territory markers for each stone (region) separately
     */
    private fun createTerritoryMarker(markerSet: MarkerSet, owner: String, chunks: List<ChunkCoord>, map: BlueMapMap) {
        try {
            // Get nation display name and color
            val teamInfo = plugin.configManager.getTeamIds().firstOrNull { teamId ->
                plugin.configManager.getTeamLuckPermsGroup(teamId) == owner
            }

            val nationDisplayName = if (teamInfo != null) {
                plugin.configManager.getTeamDisplayName(teamInfo)
            } else {
                owner
            }

            val colorHex = if (teamInfo != null) {
                plugin.configManager.getTeamColor(teamInfo)
            } else {
                "#FFFFFF"
            }

            // Parse color
            val color = parseColor(colorHex)

            // Create semi-transparent fill color (30% opacity)
            val fillColor = Color(
                color.red,
                color.green,
                color.blue,
                0.3f
            )

            // Group chunks by parent stone to create separate markers for each region
            val stoneGroups = mutableMapOf<java.util.UUID, MutableList<ChunkCoord>>()

            chunks.forEach { chunk ->
                val chunkKey = "${chunk.world};${chunk.x};${chunk.z}"
                val parentStoneUuid = plugin.databaseManager.getChunkParentStone(chunkKey)

                if (parentStoneUuid != null) {
                    stoneGroups.computeIfAbsent(parentStoneUuid) { mutableListOf() }.add(chunk)
                }
            }

            // Create a marker for each stone (region)
            stoneGroups.forEach { (stoneUuid, stoneChunks) ->
                val stone = plugin.databaseManager.getStoneByUuid(stoneUuid)

                if (stone != null) {
                    val regionName = stone.regionName ?: "지역${stoneChunks.hashCode()}"
                    val tierName = stone.currentTier.tierName

                    // Label: <국가이름> <지역이름> <티어>
                    val label = "$nationDisplayName $regionName $tierName"

                    // Create shape from chunks
                    val shape = createShapeFromChunks(stoneChunks)

                    // Use stone location as center
                    val centerX = stone.location.blockX.toDouble()
                    val centerY = stone.location.blockY.toDouble()
                    val centerZ = stone.location.blockZ.toDouble()

                    // Create Shape marker
                    val marker = de.bluecolored.bluemap.api.markers.ShapeMarker(
                        label,
                        Vector3d(centerX, centerY, centerZ),
                        shape,
                        centerY.toFloat()
                    )

                    // Set colors and properties
                    marker.lineWidth = 5
                    marker.lineColor = color
                    marker.fillColor = fillColor
                    marker.isDepthTestEnabled = false

                    // Add to marker set
                    val markerId = "region_${stoneUuid}"
                    markerSet.markers[markerId] = marker

                    plugin.logger.info("      Created marker: $label (${stoneChunks.size} chunks)")
                }
            }

        } catch (e: Exception) {
            plugin.logger.warning("      Failed to create marker for $owner: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Create a shape from chunk coordinates
     * Creates a polygon from chunk boundaries
     */
    private fun createShapeFromChunks(chunks: List<ChunkCoord>): Shape {
        if (chunks.isEmpty()) {
            return Shape.builder().build()
        }

        // Create set for fast lookup
        val chunkSet = chunks.map { Pair(it.x, it.z) }.toSet()

        // Find outline points
        val outlinePoints = mutableSetOf<Vector2d>()

        chunks.forEach { chunk ->
            val blockX = chunk.x * 16
            val blockZ = chunk.z * 16

            // Check each corner and edge
            // Top-left corner
            if (!chunkSet.contains(Pair(chunk.x - 1, chunk.z)) ||
                !chunkSet.contains(Pair(chunk.x, chunk.z - 1))) {
                outlinePoints.add(Vector2d(blockX.toDouble(), blockZ.toDouble()))
            }

            // Top-right corner
            if (!chunkSet.contains(Pair(chunk.x + 1, chunk.z)) ||
                !chunkSet.contains(Pair(chunk.x, chunk.z - 1))) {
                outlinePoints.add(Vector2d((blockX + 16).toDouble(), blockZ.toDouble()))
            }

            // Bottom-right corner
            if (!chunkSet.contains(Pair(chunk.x + 1, chunk.z)) ||
                !chunkSet.contains(Pair(chunk.x, chunk.z + 1))) {
                outlinePoints.add(Vector2d((blockX + 16).toDouble(), (blockZ + 16).toDouble()))
            }

            // Bottom-left corner
            if (!chunkSet.contains(Pair(chunk.x - 1, chunk.z)) ||
                !chunkSet.contains(Pair(chunk.x, chunk.z + 1))) {
                outlinePoints.add(Vector2d(blockX.toDouble(), (blockZ + 16).toDouble()))
            }
        }

        // If no outline found, use bounding box
        if (outlinePoints.isEmpty()) {
            val minX = chunks.minOf { it.x }
            val maxX = chunks.maxOf { it.x }
            val minZ = chunks.minOf { it.z }
            val maxZ = chunks.maxOf { it.z }

            val blockMinX = (minX * 16).toDouble()
            val blockMaxX = ((maxX + 1) * 16).toDouble()
            val blockMinZ = (minZ * 16).toDouble()
            val blockMaxZ = ((maxZ + 1) * 16).toDouble()

            return Shape.builder()
                .addPoint(Vector2d(blockMinX, blockMinZ))
                .addPoint(Vector2d(blockMaxX, blockMinZ))
                .addPoint(Vector2d(blockMaxX, blockMaxZ))
                .addPoint(Vector2d(blockMinX, blockMaxZ))
                .build()
        }

        // Sort points to create proper polygon
        val centerX = outlinePoints.map { it.x }.average()
        val centerZ = outlinePoints.map { it.y }.average()

        val sortedPoints = outlinePoints.sortedBy { point ->
            kotlin.math.atan2(point.y - centerZ, point.x - centerX)
        }

        // Build shape
        val shapeBuilder = Shape.builder()
        sortedPoints.forEach { point ->
            shapeBuilder.addPoint(point)
        }

        return shapeBuilder.build()
    }

    /**
     * Parse hex color string to BlueMap Color
     * RGB: Int (0-255), Alpha: Float (0.0-1.0)
     */
    private fun parseColor(hex: String): Color {
        return try {
            val cleanHex = hex.removePrefix("#")
            val r = cleanHex.substring(0, 2).toInt(16)
            val g = cleanHex.substring(2, 4).toInt(16)
            val b = cleanHex.substring(4, 6).toInt(16)
            Color(r, g, b, 1.0f)  // Alpha: 1.0f = fully opaque
        } catch (_: Exception) {
            Color(255, 255, 255, 1.0f) // Default white
        }
    }

    /**
     * Check if BlueMap is enabled
     */
    fun isBlueMapEnabled(): Boolean = isEnabled

    /**
     * Data class for chunk coordinates
     */
    private data class ChunkCoord(val world: String, val x: Int, val z: Int)
}

