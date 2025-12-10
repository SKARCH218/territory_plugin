package kr.skarch.territory_Plugin.managers

import de.bluecolored.bluemap.api.BlueMapAPI
import de.bluecolored.bluemap.api.BlueMapMap
import de.bluecolored.bluemap.api.markers.ExtrudeMarker
import de.bluecolored.bluemap.api.markers.MarkerSet
import de.bluecolored.bluemap.api.math.Color
import de.bluecolored.bluemap.api.math.Shape
import com.flowpowered.math.vector.Vector2d
import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.Bukkit
import java.util.concurrent.ConcurrentHashMap

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
                updateMarkers()
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
        if (!isEnabled || blueMapAPI == null) {
            return
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                val api = blueMapAPI ?: return@Runnable

                // Group chunks by owner
                val territories = plugin.databaseManager.getAllTerritories()
                val groupedChunks = ConcurrentHashMap<String, MutableList<ChunkCoord>>()

                territories.forEach { (chunkKey, owner) ->
                    val parts = chunkKey.split(";")
                    if (parts.size == 3) {
                        val world = parts[0]
                        val chunkX = parts[1].toIntOrNull() ?: return@forEach
                        val chunkZ = parts[2].toIntOrNull() ?: return@forEach

                        val key = "$world:$owner"
                        groupedChunks.computeIfAbsent(key) { mutableListOf() }
                            .add(ChunkCoord(world, chunkX, chunkZ))
                    }
                }

                // Update markers for each world
                api.worlds.forEach { blueWorld ->
                    blueWorld.maps.forEach { map ->
                        updateWorldMarkers(map, groupedChunks)
                    }
                }

                plugin.logger.info("BlueMap markers updated successfully (${territories.size} chunks)")
            } catch (e: Exception) {
                plugin.logger.warning("Failed to update BlueMap markers: ${e.message}")
                e.printStackTrace()
            }
        })
    }

    /**
     * Update markers for a specific world map
     */
    private fun updateWorldMarkers(map: BlueMapMap, groupedChunks: Map<String, List<ChunkCoord>>) {
        try {
            // Get or create marker set
            val markerSet = map.markerSets[markerSetId] ?: MarkerSet.builder()
                .label(markerSetLabel)
                .build()
                .also { map.markerSets[markerSetId] = it }

            // Clear existing markers
            markerSet.markers.clear()

            // Add markers for each nation in this world
            groupedChunks.forEach { (key, chunks) ->
                val parts = key.split(":")
                if (parts.size != 2) return@forEach

                val world = parts[0]
                val owner = parts[1]

                // Only process chunks for this world
                // BlueMap API uses world.id to get world identifier
                val worldId = map.world.id
                if (world != worldId) return@forEach

                // Get nation display name and color
                val teamInfo = plugin.configManager.getTeamIds().firstOrNull { teamId ->
                    plugin.configManager.getTeamLuckPermsGroup(teamId) == owner
                }

                val displayName = if (teamInfo != null) {
                    plugin.configManager.getTeamDisplayName(teamInfo)
                } else {
                    owner
                }

                val colorHex = if (teamInfo != null) {
                    plugin.configManager.getTeamColor(teamInfo)
                } else {
                    "#FFFFFF"
                }

                // Convert hex to Color
                val color = parseColor(colorHex)

                // Create shape from chunks
                val shape = createShapeFromChunks(chunks)

                // Create extrude marker with semi-transparent fill
                // RGB: Int (0-255), Alpha: Float (0.0-1.0)
                val fillColor = Color(
                    color.red,
                    color.green,
                    color.blue,
                    0.3f  // 30% transparency (alpha as float)
                )

                val marker = ExtrudeMarker.builder()
                    .label(displayName)
                    .shape(shape, 0f, 100f) // From y=0 to y=100
                    .lineColor(color)
                    .fillColor(fillColor)
                    .build()

                markerSet.markers["territory_${owner}_${chunks.hashCode()}"] = marker
            }

        } catch (e: Exception) {
            plugin.logger.warning("Failed to update markers for world ${map.world.id}: ${e.message}")
        }
    }

    /**
     * Create a shape from chunk coordinates
     */
    private fun createShapeFromChunks(chunks: List<ChunkCoord>): Shape {
        val shapeBuilder = Shape.builder()

        if (chunks.isNotEmpty()) {
            // Create outline of chunks
            chunks.forEach { chunk ->
                val blockX = chunk.x * 16
                val blockZ = chunk.z * 16

                // Use Vector2d for BlueMap API
                shapeBuilder.addPoint(Vector2d(blockX.toDouble(), blockZ.toDouble()))
                shapeBuilder.addPoint(Vector2d((blockX + 16).toDouble(), blockZ.toDouble()))
                shapeBuilder.addPoint(Vector2d((blockX + 16).toDouble(), (blockZ + 16).toDouble()))
                shapeBuilder.addPoint(Vector2d(blockX.toDouble(), (blockZ + 16).toDouble()))
            }
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

