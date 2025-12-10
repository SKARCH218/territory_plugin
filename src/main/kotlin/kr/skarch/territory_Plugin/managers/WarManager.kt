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

        Bukkit.broadcastMessage("§c⚠ WARNING: Nation [$nationName] has declared TOTAL WAR! Combat starts in ${preparationTime / 60} minutes.")

        // Execute declaration commands
        executeDeclarationCommands(nationName)

        val task = object : BukkitRunnable() {
            var countdown = preparationTime

            override fun run() {
                countdown--
                pendingWars[nationName]?.timeLeft = countdown

                when (countdown) {
                    300 -> Bukkit.broadcastMessage("§e⚔ Nation [$nationName] war starts in 5 minutes!")
                    60 -> Bukkit.broadcastMessage("§e⚔ Nation [$nationName] war starts in 1 minute!")
                    30 -> Bukkit.broadcastMessage("§e⚔ Nation [$nationName] war starts in 30 seconds!")
                    10 -> Bukkit.broadcastMessage("§e⚔ Nation [$nationName] war starts in 10 seconds!")
                    0 -> {
                        plugin.databaseManager.setWarState(nationName, true)
                        plugin.databaseManager.logWarStart(nationName, "GLOBAL")
                        Bukkit.broadcastMessage("§4⚔ TOTAL WAR ACTIVE: Nation [$nationName] is now at war with the world!")
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

        plugin.databaseManager.setWarState(nationName, false)
        plugin.databaseManager.logWarEnd(nationName, stonesDestroyed, chunksConquered)
        Bukkit.broadcastMessage("§aWar for Nation [$nationName] has ended.")
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
            Bukkit.broadcastMessage("§aWar declaration for Nation [$nationName] has been cancelled.")
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
}

