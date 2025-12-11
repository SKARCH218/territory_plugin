package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.concurrent.ConcurrentHashMap

class WarManager(private val plugin: Territory_Plugin) {

    private val pendingWars = ConcurrentHashMap<String, WarCountdown>()

    data class WarCountdown(
        val task: BukkitRunnable,
        var timeLeft: Int
    )

    /**
     * Declare global war for a nation
     * Broadcasts warning and starts countdown
     */
    fun declareGlobalWar(nationName: String) {
        // Cancel any existing pending war
        pendingWars[nationName]?.task?.cancel()

        val preparationTime = plugin.configManager.getWarPreparationTime()
        val nextWarNumber = plugin.databaseManager.getNextWarNumber()
        val coloredNation = plugin.configManager.getColoredNationName(nationName)

        Bukkit.broadcastMessage("§c⚠ WARNING: ${coloredNation}§c 국가가 전면전을 선포했습니다! ${preparationTime / 60}분 후 전투가 시작됩니다.")
        Bukkit.broadcastMessage("§e제 §f${nextWarNumber}§e차 전쟁이 곧 시작됩니다!")

        // Execute declaration commands
        executeDeclarationCommands(nationName)

        val task = object : BukkitRunnable() {
            var countdown = preparationTime

            override fun run() {
                countdown--
                pendingWars[nationName]?.timeLeft = countdown

                when (countdown) {
                    300 -> Bukkit.broadcastMessage("§e⚔ ${coloredNation}§e 전쟁이 5분 후 시작됩니다!")
                    60 -> Bukkit.broadcastMessage("§e⚔ ${coloredNation}§e 전쟁이 1분 후 시작됩니다!")
                    30 -> Bukkit.broadcastMessage("§e⚔ ${coloredNation}§e 전쟁이 30초 후 시작됩니다!")
                    10 -> Bukkit.broadcastMessage("§e⚔ ${coloredNation}§e 전쟁이 10초 후 시작됩니다!")
                    0 -> {
                        plugin.databaseManager.setWarState(nationName, true, nextWarNumber)
                        plugin.databaseManager.logWarStart(nationName, "GLOBAL", nextWarNumber)
                        Bukkit.broadcastMessage("§4⚔⚔⚔ 제 §f${nextWarNumber}§4차 전쟁 시작! ${coloredNation}§4이(가) 전 세계와 전쟁을 시작했습니다! ⚔⚔⚔")
                        pendingWars.remove(nationName)
                        cancel()
                    }
                }
            }
        }

        task.runTaskTimer(plugin, 0L, 20L) // Run every second
        pendingWars[nationName] = WarCountdown(task, preparationTime)
    }

    /**
     * Execute console commands on war declaration
     */
    private fun executeDeclarationCommands(nationName: String) {
        val commands = plugin.configManager.config.getStringList("war.declaration-commands")
        commands.forEach { cmd ->
            val finalCmd = cmd.replace("{team}", nationName)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd)
        }
    }

    /**
     * Get remaining time until war starts (in seconds)
     * Returns null if not in preparation
     */
    fun getWarTimeLeft(nationName: String): Int? {
        return pendingWars[nationName]?.timeLeft
    }

    /**
     * Check if a nation is currently in global war
     */
    fun isInGlobalWar(nationName: String): Boolean {
        return plugin.databaseManager.isInGlobalWar(nationName)
    }

    /**
     * End global war for a nation
     */
    fun endGlobalWar(nationName: String, stonesDestroyed: Int = 0, chunksConquered: Int = 0) {
        // Cancel pending war if exists
        pendingWars[nationName]?.task?.cancel()
        pendingWars.remove(nationName)

        val coloredNation = plugin.configManager.getColoredNationName(nationName)
        plugin.databaseManager.setWarState(nationName, false)
        plugin.databaseManager.logWarEnd(nationName, stonesDestroyed, chunksConquered)
        Bukkit.broadcastMessage("§a✓ ${coloredNation}§a의 전쟁이 종료되었습니다.")
    }

    /**
     * Check if two nations can engage in combat
     * Returns true if either nation is in global war
     */
    fun canEngage(nation1: String, nation2: String): Boolean {
        return isInGlobalWar(nation1) || isInGlobalWar(nation2)
    }

    /**
     * Cancel a pending war declaration
     */
    fun cancelPendingWar(nationName: String): Boolean {
        val countdown = pendingWars.remove(nationName)
        if (countdown != null) {
            countdown.task.cancel()
            val coloredNation = plugin.configManager.getColoredNationName(nationName)
            Bukkit.broadcastMessage("§a✓ ${coloredNation}§a의 전쟁 선포가 취소되었습니다.")
            return true
        }
        return false
    }

    /**
     * Check if a nation has a pending war declaration
     */
    fun hasPendingWar(nationName: String): Boolean {
        return pendingWars.containsKey(nationName)
    }

    /**
     * Start war immediately without countdown (admin command)
     */
    fun startWarImmediately(nationName: String) {
        // Cancel any pending countdown
        pendingWars[nationName]?.task?.cancel()
        pendingWars.remove(nationName)

        // Start war immediately
        val warNumber = plugin.databaseManager.getNextWarNumber()
        plugin.databaseManager.setWarState(nationName, true, warNumber)
        plugin.databaseManager.logWarStart(nationName, "GLOBAL", warNumber)

        val coloredNation = plugin.configManager.getColoredNationName(nationName)
        // Broadcast with war number
        Bukkit.broadcastMessage("§4⚔⚔⚔ 제 §f${warNumber}§4차 전쟁 시작! ${coloredNation}§4이(가) 전 세계와 전쟁을 시작했습니다! ⚔⚔⚔")

        // Execute declaration commands
        executeDeclarationCommands(nationName)
    }

    /**
     * Get all nations currently in war
     */
    fun getActiveWars(): List<String> {
        return plugin.databaseManager.getActiveWarNations()
    }

    /**
     * Calculate current war score for all nations
     * Score = conquered chunks + (kills / 2)
     */
    fun calculateCurrentWarScore(): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()

        // Get all teams
        plugin.configManager.getTeamIds().forEach { teamId ->
            val teamGroup = plugin.configManager.getTeamLuckPermsGroup(teamId)

            // Get conquest count (점령한 다른 나라 땅 개수)
            val conquests = plugin.databaseManager.getWarConquestCount(teamGroup, currentWarNumber)

            // Get kill count
            val kills = plugin.databaseManager.getWarKillCount(teamGroup, currentWarNumber)

            // Calculate score
            val score = conquests + (kills / 2)

            if (score > 0) {
                scores[teamGroup] = score
            }
        }

        return scores
    }

    /**
     * Get conquest count for a nation in current war
     */
    fun getConquestCount(nationName: String): Int {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        return plugin.databaseManager.getWarConquestCount(nationName, currentWarNumber)
    }

    /**
     * Get kill count for a nation in current war
     */
    fun getKillCount(nationName: String): Int {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        return plugin.databaseManager.getWarKillCount(nationName, currentWarNumber)
    }

    /**
     * Record a conquest (when occupation stone is destroyed)
     */
    fun recordConquest(nationName: String) {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        plugin.databaseManager.incrementWarConquest(nationName, currentWarNumber)
    }

    /**
     * Record a kill
     */
    fun recordKill(killerNation: String) {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        plugin.databaseManager.incrementWarKill(killerNation, currentWarNumber)
    }
}

