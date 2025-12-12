package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.math.BigDecimal
import java.math.RoundingMode
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
        val countdownAlerts = plugin.configManager.getCountdownAlerts()
        val nextWarNumber = plugin.databaseManager.getNextWarNumber()
        val coloredNation = plugin.configManager.getColoredNationName(nationName)

        // Initial broadcast using Adventure API
        broadcastComponent(
            Component.text("⚠ WARNING: ", NamedTextColor.RED)
                .append(Component.text(coloredNation))
                .append(Component.text(" 국가가 전면전을 선포했습니다! ${preparationTime / 60}분 후 전투가 시작됩니다.", NamedTextColor.RED))
        )
        broadcastComponent(
            Component.text("제 ", NamedTextColor.YELLOW)
                .append(Component.text("${nextWarNumber}", NamedTextColor.WHITE))
                .append(Component.text("차 전쟁이 곧 시작됩니다!", NamedTextColor.YELLOW))
        )

        // Execute declaration commands
        executeDeclarationCommands(nationName)

        val task = object : BukkitRunnable() {
            var countdown = preparationTime

            override fun run() {
                countdown--
                pendingWars[nationName]?.timeLeft = countdown

                // Check if countdown matches any alert time
                if (countdown in countdownAlerts) {
                    val timeText = when {
                        countdown >= 60 -> "${countdown / 60}분"
                        else -> "${countdown}초"
                    }
                    broadcastComponent(
                        Component.text("⚔ ", NamedTextColor.YELLOW)
                            .append(Component.text(coloredNation))
                            .append(Component.text(" 전쟁이 ${timeText} 후 시작됩니다!", NamedTextColor.YELLOW))
                    )
                }

                if (countdown == 0) {
                    plugin.databaseManager.setWarState(nationName, true, nextWarNumber)
                    plugin.databaseManager.logWarStart(nationName, "GLOBAL", nextWarNumber)
                    broadcastComponent(
                        Component.text("⚔⚔⚔ 제 ", NamedTextColor.DARK_RED)
                            .append(Component.text("${nextWarNumber}", NamedTextColor.WHITE))
                            .append(Component.text("차 전쟁 시작! ", NamedTextColor.DARK_RED))
                            .append(Component.text(coloredNation))
                            .append(Component.text("이(가) 전 세계와 전쟁을 시작했습니다! ⚔⚔⚔", NamedTextColor.DARK_RED))
                    )

                    // 전쟁 선포국 버프 적용
                    applyAttackerBuffs(nationName)

                    pendingWars.remove(nationName)
                    cancel()
                }
            }
        }

        task.runTaskTimer(plugin, 0L, 20L) // Run every second
        pendingWars[nationName] = WarCountdown(task, preparationTime)
    }

    /**
     * Broadcast Adventure Component to all players
     */
    private fun broadcastComponent(component: Component) {
        Bukkit.getServer().sendMessage(component)
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

        // 쿨타임 설정
        plugin.databaseManager.setWarCooldown(nationName)

        broadcastComponent(
            Component.text("✓ ", NamedTextColor.GREEN)
                .append(Component.text(coloredNation))
                .append(Component.text("의 전쟁이 종료되었습니다.", NamedTextColor.GREEN))
        )

        // 전쟁 보상 지급
        if (plugin.configManager.isWarRewardsEnabled()) {
            distributeWarRewards()
        }
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
            broadcastComponent(
                Component.text("✓ ", NamedTextColor.GREEN)
                    .append(Component.text(coloredNation))
                    .append(Component.text("의 전쟁 선포가 취소되었습니다.", NamedTextColor.GREEN))
            )
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
        // Broadcast with war number using Adventure API
        broadcastComponent(
            Component.text("⚔⚔⚔ 제 ", NamedTextColor.DARK_RED)
                .append(Component.text("${warNumber}", NamedTextColor.WHITE))
                .append(Component.text("차 전쟁 시작! ", NamedTextColor.DARK_RED))
                .append(Component.text(coloredNation))
                .append(Component.text("이(가) 전 세계와 전쟁을 시작했습니다! ⚔⚔⚔", NamedTextColor.DARK_RED))
        )

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
     * New formula: (conquests - lost) + round((kills - deaths) / 2.0)
     */
    fun calculateCurrentWarScore(): Map<String, Int> {
        val scores = mutableMapOf<String, Int>()
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()

        // Get all teams
        plugin.configManager.getTeamIds().forEach { teamId ->
            val teamGroup = plugin.configManager.getTeamLuckPermsGroup(teamId)

            // Get conquest count (점령한 다른 나라 땅 개수)
            val conquests = plugin.databaseManager.getWarConquestCount(teamGroup, currentWarNumber)

            // Get lost count (점령당한 점령석 갯수)
            val lost = plugin.databaseManager.getWarLostCount(teamGroup, currentWarNumber)

            // Get kill and death count
            val kills = plugin.databaseManager.getWarKillCount(teamGroup, currentWarNumber)
            val deaths = plugin.databaseManager.getWarDeathCount(teamGroup, currentWarNumber)

            // Calculate score using new formula with HALF_UP rounding
            val stoneScore = conquests - lost
            val combatScore = BigDecimal((kills - deaths) / 2.0)
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()
            val score = stoneScore + combatScore

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

    /**
     * Record that a nation's stone was destroyed (increment lost)
     */
    fun recordLost(nationName: String) {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        // Only record if a war is active
        if (currentWarNumber <= 0) return
        plugin.databaseManager.incrementWarLost(nationName, currentWarNumber)
    }

    /**
     * Record that a nation's player died (increment deaths)
     */
    fun recordDeath(nationName: String) {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        if (currentWarNumber <= 0) return
        plugin.databaseManager.incrementWarDeath(nationName, currentWarNumber)
    }

    /**
     * Apply buffs to attacking nation players
     */
    private fun applyAttackerBuffs(nationName: String) {
        if (!plugin.configManager.isAttackerBuffsEnabled()) return

        val duration = plugin.configManager.getAttackerBuffsDuration() * 20 // Convert to ticks
        val effects = plugin.configManager.getAttackerBuffEffects()
        val coloredNation = plugin.configManager.getColoredNationName(nationName)

        Bukkit.getOnlinePlayers().forEach { player ->
            val playerGroup = kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)
            if (playerGroup == nationName) {
                effects.forEach { effectStr ->
                    val parts = effectStr.split(":")
                    if (parts.size == 2) {
                        try {
                            val effectType = org.bukkit.potion.PotionEffectType.getByName(parts[0])
                            val amplifier = parts[1].toInt() - 1 // Minecraft uses 0-based amplifier
                            if (effectType != null) {
                                player.addPotionEffect(
                                    org.bukkit.potion.PotionEffect(effectType, duration, amplifier)
                                )
                            }
                        } catch (e: Exception) {
                            plugin.logger.warning("Invalid effect format: $effectStr")
                        }
                    }
                }
            }
        }

        if (plugin.configManager.isAttackerBuffsBroadcast()) {
            broadcastComponent(
                Component.text("⚡ ", NamedTextColor.GOLD)
                    .append(Component.text(coloredNation))
                    .append(Component.text(" 국가의 전사들이 전쟁 버프를 받았습니다! (${plugin.configManager.getAttackerBuffsDuration() / 60}분)", NamedTextColor.YELLOW))
            )
        }
    }

    /**
     * Distribute war rewards to winner and participants
     */
    private fun distributeWarRewards() {
        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        val scores = calculateCurrentWarScore()

        if (scores.isEmpty()) return

        // 1. 승리 보상
        val winner = scores.maxByOrNull { it.value }?.key
        if (winner != null) {
            distributeVictoryReward(winner)
        }

        // 2. MVP 보상
        if (plugin.configManager.isMvpRewardEnabled()) {
            distributeMvpReward(currentWarNumber)
        }
    }

    /**
     * Distribute victory reward to winning nation
     */
    private fun distributeVictoryReward(winnerNation: String) {
        val money = plugin.configManager.getVictoryRewardMoney()
        val items = plugin.configManager.getVictoryRewardItems()
        val coloredNation = plugin.configManager.getColoredNationName(winnerNation)

        broadcastComponent(
            Component.text("🏆 ", NamedTextColor.GOLD)
                .append(Component.text(coloredNation))
                .append(Component.text(" 국가가 전쟁에서 승리했습니다!", NamedTextColor.YELLOW))
        )

        Bukkit.getOnlinePlayers().forEach { player ->
            val playerGroup = kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)
            if (playerGroup == winnerNation) {
                // Give money
                if (money > 0 && plugin.server.pluginManager.getPlugin("Vault") != null) {
                    val economy = plugin.server.servicesManager.getRegistration(
                        net.milkbowl.vault.economy.Economy::class.java
                    )?.provider
                    economy?.depositPlayer(player, money)
                }

                // Give items
                items.forEach { itemStr ->
                    val parts = itemStr.split(":")
                    if (parts.size == 2) {
                        try {
                            val material = org.bukkit.Material.valueOf(parts[0].uppercase())
                            val amount = parts[1].toInt()
                            val itemStack = org.bukkit.inventory.ItemStack(material, amount)
                            player.inventory.addItem(itemStack)
                        } catch (e: Exception) {
                            plugin.logger.warning("Invalid item format: $itemStr")
                        }
                    }
                }

                player.sendMessage("§a§l[승리 보상] §e${money}원과 아이템을 받았습니다!")
            }
        }

        // Execute victory commands
        val commands = plugin.configManager.getVictoryRewardCommands()
        commands.forEach { cmd ->
            val finalCmd = cmd.replace("{winner}", winnerNation)
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCmd)
        }
    }

    /**
     * Distribute MVP reward to top killer or conqueror
     */
    private fun distributeMvpReward(warNumber: Int) {
        val mvpMoney = plugin.configManager.getMvpRewardMoney()
        val mvpItems = plugin.configManager.getMvpRewardItems()

        // Find MVP (most kills + conquests)
        var mvpPlayer: org.bukkit.entity.Player? = null
        var maxScore = 0

        Bukkit.getOnlinePlayers().forEach { player ->
            val playerGroup = kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(player)
            val kills = plugin.databaseManager.getWarKillCount(playerGroup, warNumber)
            val conquests = plugin.databaseManager.getWarConquestCount(playerGroup, warNumber)
            val score = kills + (conquests * 5) // 점령석은 킬의 5배 가치

            if (score > maxScore) {
                maxScore = score
                mvpPlayer = player
            }
        }

        mvpPlayer?.let { player ->
            broadcastComponent(
                Component.text("⭐ MVP: ", NamedTextColor.GOLD)
                    .append(Component.text(player.name, NamedTextColor.YELLOW))
                    .append(Component.text("님이 전쟁에서 가장 많은 공헌을 했습니다!", NamedTextColor.GOLD))
            )

            // Give money
            if (mvpMoney > 0 && plugin.server.pluginManager.getPlugin("Vault") != null) {
                val economy = plugin.server.servicesManager.getRegistration(
                    net.milkbowl.vault.economy.Economy::class.java
                )?.provider
                economy?.depositPlayer(player, mvpMoney)
            }

            // Give items
            mvpItems.forEach { itemStr ->
                val parts = itemStr.split(":")
                if (parts.size == 2) {
                    try {
                        val material = org.bukkit.Material.valueOf(parts[0].uppercase())
                        val amount = parts[1].toInt()
                        val itemStack = org.bukkit.inventory.ItemStack(material, amount)
                        player.inventory.addItem(itemStack)
                    } catch (e: Exception) {
                        plugin.logger.warning("Invalid MVP item format: $itemStr")
                    }
                }
            }

            player.sendMessage("§6§l[MVP 보상] §e${mvpMoney}원과 특별 아이템을 받았습니다!")
        }
    }

    /**
     * Check if nation can declare war (cooldown check)
     */
    fun canDeclareWar(nationName: String): Pair<Boolean, Long> {
        val cooldown = plugin.configManager.getWarDeclarationCooldown()
        val remaining = plugin.databaseManager.getRemainingCooldown(nationName, cooldown)
        return Pair(remaining == 0L, remaining)
    }
}
