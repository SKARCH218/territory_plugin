package kr.skarch.territory_Plugin.database

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.models.OccupationStone
import kr.skarch.territory_Plugin.models.StoneTier
import kr.skarch.territory_Plugin.models.TerritoryChunk
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID

class DatabaseManager(private val plugin: Territory_Plugin) {

    private lateinit var connection: Connection
    private lateinit var dbFile: File

    fun initialize() {
        dbFile = File(plugin.dataFolder, "territory.db")
        if (!dbFile.exists()) {
            plugin.dataFolder.mkdirs()
        }

        connect()
        createTables()
    }

    private fun connect() {
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().execute("PRAGMA foreign_keys = ON")
    }

    private fun ensureConnection() {
        try {
            if (connection.isClosed || !connection.isValid(5)) {
                plugin.logger.warning("Database connection lost, reconnecting...")
                connect()
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to ensure database connection: ${e.message}")
            connect()
        }
    }

    private fun createTables() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS occupation_stones (
                    stone_uuid VARCHAR(36) PRIMARY KEY,
                    owner_group VARCHAR(64) NOT NULL,
                    current_tier VARCHAR(20) DEFAULT 'TIER_1',
                    world VARCHAR(64),
                    x INT, y INT, z INT,
                    created_at LONG DEFAULT 0
                )
            """)

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS territory_chunks (
                    chunk_key VARCHAR(64) PRIMARY KEY,
                    owner_group VARCHAR(64) NOT NULL,
                    parent_stone_uuid VARCHAR(36)
                )
            """)

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_states (
                    nation_name VARCHAR(64) PRIMARY KEY,
                    is_global_war BOOLEAN DEFAULT FALSE,
                    war_start_time LONG
                )
            """)

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nation_name VARCHAR(64) NOT NULL,
                    war_type VARCHAR(20) NOT NULL,
                    start_time LONG NOT NULL,
                    end_time LONG,
                    stones_destroyed INT DEFAULT 0,
                    chunks_conquered INT DEFAULT 0
                )
            """)

            // Create indexes for better performance
            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_chunks_owner 
                ON territory_chunks(owner_group)
            """)

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_chunks_parent 
                ON territory_chunks(parent_stone_uuid)
            """)

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_stones_owner 
                ON occupation_stones(owner_group)
            """)

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_war_history_nation 
                ON war_history(nation_name, start_time DESC)
            """)
        }
    }

    fun saveStone(stone: OccupationStone) {
        ensureConnection()
        val sql = """
            INSERT OR REPLACE INTO occupation_stones 
            (stone_uuid, owner_group, current_tier, world, x, y, z, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, stone.stoneUuid.toString())
            stmt.setString(2, stone.ownerGroup)
            stmt.setString(3, stone.currentTier.tierName)
            stmt.setString(4, stone.location.world?.name)
            stmt.setInt(5, stone.location.blockX)
            stmt.setInt(6, stone.location.blockY)
            stmt.setInt(7, stone.location.blockZ)
            stmt.setLong(8, stone.createdAt)
            stmt.executeUpdate()
        }
    }

    fun updateStoneTier(stoneUuid: UUID, tier: StoneTier) {
        val sql = "UPDATE occupation_stones SET current_tier = ? WHERE stone_uuid = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, tier.tierName)
            stmt.setString(2, stoneUuid.toString())
            stmt.executeUpdate()
        }
    }

    fun saveChunk(chunk: TerritoryChunk) {
        val sql = """
            INSERT OR REPLACE INTO territory_chunks 
            (chunk_key, owner_group, parent_stone_uuid)
            VALUES (?, ?, ?)
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, chunk.chunkKey)
            stmt.setString(2, chunk.ownerGroup)
            stmt.setString(3, chunk.parentStoneUuid.toString())
            stmt.executeUpdate()
        }
    }

    fun getChunkOwner(chunkKey: String): String? {
        ensureConnection()
        val sql = "SELECT owner_group FROM territory_chunks WHERE chunk_key = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, chunkKey)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getString("owner_group") else null
        }
    }

    fun getChunkParentStone(chunkKey: String): UUID? {
        val sql = "SELECT parent_stone_uuid FROM territory_chunks WHERE chunk_key = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, chunkKey)
            val rs = stmt.executeQuery()
            return if (rs.next()) UUID.fromString(rs.getString("parent_stone_uuid")) else null
        }
    }

    fun getStoneByLocation(location: Location): OccupationStone? {
        val sql = "SELECT * FROM occupation_stones WHERE world = ? AND x = ? AND y = ? AND z = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, location.world?.name)
            stmt.setInt(2, location.blockX)
            stmt.setInt(3, location.blockY)
            stmt.setInt(4, location.blockZ)
            val rs = stmt.executeQuery()
            return if (rs.next()) {
                OccupationStone(
                    UUID.fromString(rs.getString("stone_uuid")),
                    rs.getString("owner_group"),
                    StoneTier.fromString(rs.getString("current_tier")),
                    location,
                    rs.getLong("created_at")
                )
            } else null
        }
    }

    fun getStoneByUuid(stoneUuid: UUID): OccupationStone? {
        val sql = "SELECT * FROM occupation_stones WHERE stone_uuid = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, stoneUuid.toString())
            val rs = stmt.executeQuery()
            return if (rs.next()) {
                val world = Bukkit.getWorld(rs.getString("world"))
                val location = Location(world, rs.getInt("x").toDouble(), rs.getInt("y").toDouble(), rs.getInt("z").toDouble())
                OccupationStone(
                    stoneUuid,
                    rs.getString("owner_group"),
                    StoneTier.fromString(rs.getString("current_tier")),
                    location,
                    rs.getLong("created_at")
                )
            } else null
        }
    }

    fun getChunkCountByTeam(teamName: String): Int {
        ensureConnection()
        val sql = "SELECT COUNT(*) as count FROM territory_chunks WHERE owner_group = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, teamName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("count") else 0
        }
    }

    fun getStonesByTeam(teamName: String): List<OccupationStone> {
        ensureConnection()
        val stones = mutableListOf<OccupationStone>()
        val sql = "SELECT * FROM occupation_stones WHERE owner_group = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, teamName)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val world = Bukkit.getWorld(rs.getString("world"))
                val location = Location(
                    world,
                    rs.getInt("x").toDouble(),
                    rs.getInt("y").toDouble(),
                    rs.getInt("z").toDouble()
                )
                stones.add(
                    OccupationStone(
                        UUID.fromString(rs.getString("stone_uuid")),
                        rs.getString("owner_group"),
                        StoneTier.fromString(rs.getString("current_tier")),
                        location,
                        rs.getLong("created_at")
                    )
                )
            }
        }
        return stones
    }

    fun getAllStones(): List<OccupationStone> {
        ensureConnection()
        val stones = mutableListOf<OccupationStone>()
        val sql = "SELECT * FROM occupation_stones"
        connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery(sql)
            while (rs.next()) {
                val world = Bukkit.getWorld(rs.getString("world"))
                val location = Location(
                    world,
                    rs.getInt("x").toDouble(),
                    rs.getInt("y").toDouble(),
                    rs.getInt("z").toDouble()
                )
                stones.add(
                    OccupationStone(
                        UUID.fromString(rs.getString("stone_uuid")),
                        rs.getString("owner_group"),
                        StoneTier.fromString(rs.getString("current_tier")),
                        location,
                        rs.getLong("created_at")
                    )
                )
            }
        }
        return stones
    }

    fun removeStone(stoneUuid: UUID) {
        connection.prepareStatement("DELETE FROM occupation_stones WHERE stone_uuid = ?").use {
            it.setString(1, stoneUuid.toString())
            it.executeUpdate()
        }
    }

    fun getChunksByStone(stoneUuid: UUID): List<String> {
        val chunks = mutableListOf<String>()
        connection.prepareStatement("SELECT chunk_key FROM territory_chunks WHERE parent_stone_uuid = ?").use {
            it.setString(1, stoneUuid.toString())
            val rs = it.executeQuery()
            while (rs.next()) {
                chunks.add(rs.getString("chunk_key"))
            }
        }
        return chunks
    }

    fun removeChunksByStone(stoneUuid: UUID): List<String> {
        val chunks = getChunksByStone(stoneUuid)

        connection.prepareStatement("DELETE FROM territory_chunks WHERE parent_stone_uuid = ?").use {
            it.setString(1, stoneUuid.toString())
            it.executeUpdate()
        }

        return chunks
    }

    fun getAllTerritories(): Map<String, String> {
        val territories = mutableMapOf<String, String>()
        connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT chunk_key, owner_group FROM territory_chunks")
            while (rs.next()) {
                territories[rs.getString("chunk_key")] = rs.getString("owner_group")
            }
        }
        return territories
    }

    fun setWarState(nationName: String, isWar: Boolean, startTime: Long = System.currentTimeMillis()) {
        val sql = """
            INSERT OR REPLACE INTO war_states 
            (nation_name, is_global_war, war_start_time)
            VALUES (?, ?, ?)
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            stmt.setBoolean(2, isWar)
            stmt.setLong(3, startTime)
            stmt.executeUpdate()
        }
    }

    fun isInGlobalWar(nationName: String): Boolean {
        ensureConnection()
        val sql = "SELECT is_global_war FROM war_states WHERE nation_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getBoolean("is_global_war") else false
        }
    }

    fun logWarStart(nationName: String, warType: String = "GLOBAL") {
        ensureConnection()
        val sql = """
            INSERT INTO war_history (nation_name, war_type, start_time, end_time)
            VALUES (?, ?, ?, NULL)
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            stmt.setString(2, warType)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.executeUpdate()
        }
    }

    fun logWarEnd(nationName: String, stonesDestroyed: Int = 0, chunksConquered: Int = 0) {
        ensureConnection()
        val sql = """
            UPDATE war_history 
            SET end_time = ?, stones_destroyed = ?, chunks_conquered = ?
            WHERE nation_name = ? AND end_time IS NULL
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setLong(1, System.currentTimeMillis())
            stmt.setInt(2, stonesDestroyed)
            stmt.setInt(3, chunksConquered)
            stmt.setString(4, nationName)
            stmt.executeUpdate()
        }
    }

    fun getWarHistory(nationName: String, limit: Int = 10): List<Map<String, Any>> {
        ensureConnection()
        val history = mutableListOf<Map<String, Any>>()
        val sql = """
            SELECT * FROM war_history 
            WHERE nation_name = ? 
            ORDER BY start_time DESC 
            LIMIT ?
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            stmt.setInt(2, limit)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                history.add(
                    mapOf(
                        "war_type" to rs.getString("war_type"),
                        "start_time" to rs.getLong("start_time"),
                        "end_time" to (rs.getLong("end_time").takeIf { !rs.wasNull() } ?: 0L),
                        "stones_destroyed" to rs.getInt("stones_destroyed"),
                        "chunks_conquered" to rs.getInt("chunks_conquered")
                    )
                )
            }
        }
        return history
    }

    fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }
}

