package kr.skarch.territory_Plugin.database

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.models.OccupationStone
import kr.skarch.territory_Plugin.models.StoneTier
import kr.skarch.territory_Plugin.models.TerritoryChunk
import org.bukkit.Bukkit
import org.bukkit.Location
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
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

        // Run migrations with backup
        try {
            val migrationManager = MigrationManager(connection)

            // Create backup before migrations (only if DB already exists)
            if (dbFile.exists() && dbFile.length() > 0) {
                migrationManager.createBackup(dbFile)
            }

            migrationManager.runMigrations()
        } catch (e: Exception) {
            plugin.logger.severe("Failed to run database migrations: ${e.message}")
            plugin.logger.severe("Please check the backup files and restore if necessary")
            throw e
        }
    }

    private fun connect() {
        connection = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        connection.createStatement().use { stmt ->
            stmt.execute("PRAGMA foreign_keys = ON")
            // Enable WAL mode for better concurrency
            stmt.execute("PRAGMA journal_mode = WAL")
            // Set busy timeout to 5 seconds
            stmt.execute("PRAGMA busy_timeout = 5000")
            // Synchronous = NORMAL for better performance
            stmt.execute("PRAGMA synchronous = NORMAL")
        }
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
                    created_at LONG DEFAULT 0,
                    region_name VARCHAR(128) UNIQUE
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
                    chunks_conquered INT DEFAULT 0,
                    war_number INT DEFAULT 0
                )
            """)

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_scores (
                    war_number INT NOT NULL,
                    nation_name VARCHAR(64) NOT NULL,
                    conquests INT DEFAULT 0,
                    kills INT DEFAULT 0,
                    lost INT DEFAULT 0,
                    deaths INT DEFAULT 0,
                    PRIMARY KEY (war_number, nation_name)
                )
            """)

            // Migrations are now handled by MigrationManager

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_counter (
                    id INT PRIMARY KEY DEFAULT 1,
                    current_war_number INT DEFAULT 0
                )
            """)

            // Initialize war counter if not exists
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO war_counter (id, current_war_number) VALUES (1, 0)
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

            // 전쟁 선포 쿨타임 테이블
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS war_cooldowns (
                    nation_name VARCHAR(64) PRIMARY KEY,
                    last_war_end_time LONG NOT NULL
                )
            """)
        }
    }

    fun saveStone(stone: OccupationStone) {
        ensureConnection()
        val sql = """
            INSERT OR REPLACE INTO occupation_stones 
            (stone_uuid, owner_group, current_tier, world, x, y, z, created_at, region_name)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setString(9, stone.regionName)
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
                    rs.getLong("created_at"),
                    rs.getString("region_name")
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
                    rs.getLong("created_at"),
                    rs.getString("region_name")
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
                        rs.getLong("created_at"),
                        rs.getString("region_name")
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
                        rs.getLong("created_at"),
                        rs.getString("region_name")
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

    // ===== 전쟁 넘버링 및 점수 시스템 =====

    /**
     * Get next war number and increment (with transaction safety)
     */
    fun getNextWarNumber(): Int {
        ensureConnection()
        val startTime = System.currentTimeMillis()

        try {
            connection.autoCommit = false
            connection.createStatement().use { stmt ->
                stmt.executeUpdate("UPDATE war_counter SET current_war_number = current_war_number + 1 WHERE id = 1")
            }
            val warNumber = getCurrentWarNumber()
            connection.commit()

            val duration = System.currentTimeMillis() - startTime
            if (duration > plugin.configManager.getSlowQueryThreshold()) {
                plugin.logger.warning("Slow query detected: getNextWarNumber took ${duration}ms")
            }

            return warNumber
        } catch (e: Exception) {
            connection.rollback()
            plugin.logger.severe("Failed to get next war number: ${e.message}")
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    /**
     * Get current war number
     */
    fun getCurrentWarNumber(): Int {
        ensureConnection()
        connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT current_war_number FROM war_counter WHERE id = 1")
            return if (rs.next()) rs.getInt("current_war_number") else 0
        }
    }

    /**
     * Get total war count
     */
    fun getWarCount(): Int {
        return getCurrentWarNumber()
    }

    /**
     * Set war state with war number (with transaction safety)
     */
    fun setWarState(nationName: String, isWar: Boolean, warNumber: Int) {
        ensureConnection()
        val startTime = System.currentTimeMillis()

        try {
            connection.autoCommit = false

            val sql = """
                INSERT OR REPLACE INTO war_states 
                (nation_name, is_global_war, war_start_time)
                VALUES (?, ?, ?)
            """
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, nationName)
                stmt.setBoolean(2, isWar)
                stmt.setLong(3, System.currentTimeMillis())
                stmt.executeUpdate()
            }

            // Initialize war score entry
            if (isWar) {
                val scoreSql = """
                    INSERT OR IGNORE INTO war_scores (war_number, nation_name, conquests, kills, lost, deaths)
                    VALUES (?, ?, 0, 0, 0, 0)
                """
                connection.prepareStatement(scoreSql).use { scoreStmt ->
                    scoreStmt.setInt(1, warNumber)
                    scoreStmt.setString(2, nationName)
                    scoreStmt.executeUpdate()
                }
            }

            connection.commit()

            val duration = System.currentTimeMillis() - startTime
            if (duration > plugin.configManager.getSlowQueryThreshold()) {
                plugin.logger.warning("Slow query detected: setWarState took ${duration}ms")
            }
        } catch (e: Exception) {
            connection.rollback()
            plugin.logger.severe("Failed to set war state for $nationName: ${e.message}")
            throw e
        } finally {
            connection.autoCommit = true
        }
    }

    /**
     * Log war start with war number
     */
    fun logWarStart(nationName: String, warType: String, warNumber: Int) {
        ensureConnection()
        val sql = """
            INSERT INTO war_history (nation_name, war_type, start_time, end_time, war_number)
            VALUES (?, ?, ?, NULL, ?)
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            stmt.setString(2, warType)
            stmt.setLong(3, System.currentTimeMillis())
            stmt.setInt(4, warNumber)
            stmt.executeUpdate()
        }
    }

    /**
     * Get active war nations
     */
    fun getActiveWarNations(): List<String> {
        ensureConnection()
        val nations = mutableListOf<String>()
        val sql = "SELECT nation_name FROM war_states WHERE is_global_war = TRUE"
        connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery(sql)
            while (rs.next()) {
                nations.add(rs.getString("nation_name"))
            }
        }
        return nations
    }

    /**
     * Increment conquest count for a nation in current war
     */
    fun incrementWarConquest(nationName: String, warNumber: Int) {
        ensureConnection()
        val sql = """
            INSERT INTO war_scores (war_number, nation_name, conquests, kills, lost, deaths)
            VALUES (?, ?, 1, 0, 0, 0)
            ON CONFLICT(war_number, nation_name) 
            DO UPDATE SET conquests = conquests + 1
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            stmt.executeUpdate()
        }
    }

    /**
     * Increment kill count for a nation in current war
     */
    fun incrementWarKill(nationName: String, warNumber: Int) {
        ensureConnection()
        val sql = """
            INSERT INTO war_scores (war_number, nation_name, conquests, kills, lost, deaths)
            VALUES (?, ?, 0, 1, 0, 0)
            ON CONFLICT(war_number, nation_name) 
            DO UPDATE SET kills = kills + 1
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            stmt.executeUpdate()
        }
    }

    /**
     * Increment lost (stones destroyed) for a nation in current war
     */
    fun incrementWarLost(nationName: String, warNumber: Int) {
        ensureConnection()
        val sql = """
            INSERT INTO war_scores (war_number, nation_name, conquests, kills, lost, deaths)
            VALUES (?, ?, 0, 0, 1, 0)
            ON CONFLICT(war_number, nation_name)
            DO UPDATE SET lost = lost + 1
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            stmt.executeUpdate()
        }
    }

    /**
     * Increment deaths (player deaths) for a nation in current war
     */
    fun incrementWarDeath(nationName: String, warNumber: Int) {
        ensureConnection()
        val sql = """
            INSERT INTO war_scores (war_number, nation_name, conquests, kills, lost, deaths)
            VALUES (?, ?, 0, 0, 0, 1)
            ON CONFLICT(war_number, nation_name)
            DO UPDATE SET deaths = deaths + 1
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            stmt.executeUpdate()
        }
    }

    /**
     * Get conquest count for a nation in specific war
     */
    fun getWarConquestCount(nationName: String, warNumber: Int): Int {
        ensureConnection()
        val sql = "SELECT conquests FROM war_scores WHERE war_number = ? AND nation_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("conquests") else 0
        }
    }

    /**
     * Get lost (stones destroyed) count for a nation in specific war
     */
    fun getWarLostCount(nationName: String, warNumber: Int): Int {
        ensureConnection()
        val sql = "SELECT lost FROM war_scores WHERE war_number = ? AND nation_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("lost") else 0
        }
    }

    /**
     * Get kill count for a nation in specific war
     */
    fun getWarKillCount(nationName: String, warNumber: Int): Int {
        ensureConnection()
        val sql = "SELECT kills FROM war_scores WHERE war_number = ? AND nation_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("kills") else 0
        }
    }

    /**
     * Get death (player deaths) count for a nation in specific war
     */
    fun getWarDeathCount(nationName: String, warNumber: Int): Int {
        ensureConnection()
        val sql = "SELECT deaths FROM war_scores WHERE war_number = ? AND nation_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            stmt.setString(2, nationName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("deaths") else 0
        }
    }

    /**
     * Get war score for specific war number
     * Calculates score in real-time: (conquests - lost) + round((kills - deaths) / 2.0)
     */
    fun getWarScore(warNumber: Int): Map<String, Int> {
        ensureConnection()
        val scores = mutableMapOf<String, Int>()
        val sql = "SELECT nation_name, conquests, kills, lost, deaths FROM war_scores WHERE war_number = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setInt(1, warNumber)
            val rs = stmt.executeQuery()
            while (rs.next()) {
                val conquests = rs.getInt("conquests")
                val kills = rs.getInt("kills")
                val lost = rs.getInt("lost")
                val deaths = rs.getInt("deaths")

                // Calculate score with HALF_UP rounding
                val stoneScore = conquests - lost
                val combatScore = BigDecimal((kills - deaths) / 2.0)
                    .setScale(0, RoundingMode.HALF_UP)
                    .toInt()
                val totalScore = stoneScore + combatScore

                scores[rs.getString("nation_name")] = totalScore
            }
        }
        return scores
    }

    /**
     * Check if region name already exists
     */
    fun isRegionNameExists(regionName: String): Boolean {
        ensureConnection()
        val sql = "SELECT COUNT(*) as count FROM occupation_stones WHERE region_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, regionName)
            val rs = stmt.executeQuery()
            return if (rs.next()) rs.getInt("count") > 0 else false
        }
    }

    /**
     * Set war cooldown for a nation
     */
    fun setWarCooldown(nationName: String, endTime: Long = System.currentTimeMillis()) {
        ensureConnection()
        val sql = """
            INSERT OR REPLACE INTO war_cooldowns (nation_name, last_war_end_time)
            VALUES (?, ?)
        """
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            stmt.setLong(2, endTime)
            stmt.executeUpdate()
        }
    }

    /**
     * Get remaining cooldown time for a nation (in seconds)
     * Returns 0 if no cooldown or cooldown expired
     */
    fun getRemainingCooldown(nationName: String, cooldownSeconds: Long): Long {
        ensureConnection()
        val sql = "SELECT last_war_end_time FROM war_cooldowns WHERE nation_name = ?"
        connection.prepareStatement(sql).use { stmt ->
            stmt.setString(1, nationName)
            val rs = stmt.executeQuery()
            if (rs.next()) {
                val lastEndTime = rs.getLong("last_war_end_time")
                val cooldownMs = cooldownSeconds * 1000
                val elapsed = System.currentTimeMillis() - lastEndTime
                val remaining = cooldownMs - elapsed
                return if (remaining > 0) remaining / 1000 else 0
            }
            return 0
        }
    }

    /**
     * Check if nation can declare war (cooldown expired)
     */
    fun canDeclareWar(nationName: String, cooldownSeconds: Long): Boolean {
        return getRemainingCooldown(nationName, cooldownSeconds) == 0L
    }

    fun close() {
        if (::connection.isInitialized && !connection.isClosed) {
            connection.close()
        }
    }
}
