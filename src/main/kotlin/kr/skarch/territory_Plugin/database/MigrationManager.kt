package kr.skarch.territory_Plugin.database

import java.sql.Connection

/**
 * Database migration manager with version control
 * Prevents rollback issues and ensures safe schema updates
 */
class MigrationManager(private val connection: Connection) {

    companion object {
        const val CURRENT_SCHEMA_VERSION = 2 // Increment when schema changes
    }

    /**
     * Initialize schema_version table and run necessary migrations
     */
    fun runMigrations() {
        createVersionTable()
        val currentVersion = getCurrentVersion()

        if (currentVersion < CURRENT_SCHEMA_VERSION) {
            println("Running database migrations from version $currentVersion to $CURRENT_SCHEMA_VERSION")

            // Run migrations sequentially
            for (targetVersion in (currentVersion + 1)..CURRENT_SCHEMA_VERSION) {
                runMigration(targetVersion)
            }

            updateVersion(CURRENT_SCHEMA_VERSION)
            println("Database migrations completed successfully")
        } else if (currentVersion > CURRENT_SCHEMA_VERSION) {
            throw IllegalStateException(
                "Database schema version ($currentVersion) is newer than plugin version ($CURRENT_SCHEMA_VERSION). " +
                "This indicates a potential downgrade. Please update the plugin or restore from backup."
            )
        } else {
            println("Database schema is up to date (version $CURRENT_SCHEMA_VERSION)")
        }
    }

    /**
     * Create schema_version table if it doesn't exist
     */
    private fun createVersionTable() {
        connection.createStatement().use { stmt ->
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS schema_version (
                    id INTEGER PRIMARY KEY DEFAULT 1,
                    version INTEGER NOT NULL DEFAULT 0,
                    updated_at LONG NOT NULL
                )
            """)

            // Initialize with version 0 if empty
            stmt.executeUpdate("""
                INSERT OR IGNORE INTO schema_version (id, version, updated_at)
                VALUES (1, 0, ${System.currentTimeMillis()})
            """)
        }
    }

    /**
     * Get current schema version
     */
    private fun getCurrentVersion(): Int {
        connection.createStatement().use { stmt ->
            val rs = stmt.executeQuery("SELECT version FROM schema_version WHERE id = 1")
            return if (rs.next()) rs.getInt("version") else 0
        }
    }

    /**
     * Update schema version
     */
    private fun updateVersion(version: Int) {
        connection.prepareStatement("""
            UPDATE schema_version SET version = ?, updated_at = ? WHERE id = 1
        """).use { stmt ->
            stmt.setInt(1, version)
            stmt.setLong(2, System.currentTimeMillis())
            stmt.executeUpdate()
        }
    }

    /**
     * Run specific migration based on target version
     */
    private fun runMigration(targetVersion: Int) {
        println("Applying migration to version $targetVersion")

        connection.createStatement().use { stmt ->
            when (targetVersion) {
                1 -> {
                    // Migration 1: Add lost and deaths columns to war_scores
                    println("  - Adding 'lost' and 'deaths' columns to war_scores table")
                    try {
                        stmt.executeUpdate("ALTER TABLE war_scores ADD COLUMN lost INT DEFAULT 0")
                    } catch (e: Exception) {
                        println("  - 'lost' column already exists, skipping")
                    }
                    try {
                        stmt.executeUpdate("ALTER TABLE war_scores ADD COLUMN deaths INT DEFAULT 0")
                    } catch (e: Exception) {
                        println("  - 'deaths' column already exists, skipping")
                    }
                }

                2 -> {
                    // Migration 2: Remove total_score column (SQLite doesn't support DROP COLUMN before 3.35.0)
                    // We'll just ignore it and document that it's deprecated
                    println("  - Marking 'total_score' column as deprecated (will be ignored)")
                    println("  - Note: SQLite doesn't support DROP COLUMN in older versions")
                    println("  - The column will remain but won't be used")
                }

                // Add future migrations here
                // 3 -> { ... }
                // 4 -> { ... }

                else -> {
                    println("  - No migration defined for version $targetVersion")
                }
            }
        }
    }

    /**
     * Create a backup of the database before migrations
     * Returns backup file path
     */
    fun createBackup(dbFile: java.io.File): java.io.File? {
        return try {
            val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(java.util.Date())
            val backupFile = java.io.File(dbFile.parentFile, "${dbFile.nameWithoutExtension}_backup_$timestamp.db")

            dbFile.copyTo(backupFile, overwrite = false)
            println("Database backup created: ${backupFile.name}")

            // Clean up old backups (keep last 7 days)
            cleanupOldBackups(dbFile.parentFile, dbFile.nameWithoutExtension)

            backupFile
        } catch (e: Exception) {
            println("Failed to create database backup: ${e.message}")
            null
        }
    }

    /**
     * Clean up backup files older than specified days
     * @param backupDir Directory containing backup files
     * @param dbNameWithoutExt Database name without extension (e.g., "territory")
     * @param daysToKeep Number of days to keep backups (default: 7)
     */
    fun cleanupOldBackups(backupDir: java.io.File, dbNameWithoutExt: String, daysToKeep: Int = 7) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)

            backupDir.listFiles { file ->
                file.name.startsWith("${dbNameWithoutExt}_backup_") && file.name.endsWith(".db")
            }?.forEach { backupFile ->
                if (backupFile.lastModified() < cutoffTime) {
                    if (backupFile.delete()) {
                        println("Deleted old backup: ${backupFile.name}")
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to cleanup old backups: ${e.message}")
        }
    }

    /**
     * Get list of available backup files
     */
    fun listBackups(backupDir: java.io.File, dbNameWithoutExt: String): List<java.io.File> {
        return backupDir.listFiles { file ->
            file.name.startsWith("${dbNameWithoutExt}_backup_") && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}

