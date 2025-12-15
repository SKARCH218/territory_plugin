package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.models.StoneTier
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitTask

/**
 * 점령석 티어별 특수 능력 관리
 * - 영역 내 아군에게 버프 적용
 * - 공격/방어 보너스 제공
 */
class StoneAbilityManager(private val plugin: Territory_Plugin) {

    private var abilityTask: BukkitTask? = null

    /**
     * Initialize and start ability system
     */
    fun initialize() {
        if (!plugin.configManager.isStoneAbilitiesEnabled()) {
            plugin.logger.info("점령석 특수 능력 시스템이 비활성화되어 있습니다.")
            return
        }

        val interval = plugin.configManager.getEffectApplyInterval()

        abilityTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            applyStoneAbilities()
        }, interval, interval)

        plugin.logger.info("점령석 특수 능력 시스템이 활성화되었습니다. (${interval}틱마다 적용)")
    }

    /**
     * Apply abilities to all players near their nation's stones
     */
    private fun applyStoneAbilities() {
        val allStones = plugin.databaseManager.getAllStones()

        Bukkit.getOnlinePlayers().forEach { player ->
            val playerGroup = PlayerGroupCache.getPlayerGroup(player)
            if (playerGroup == "팀없음") return@forEach

            // 영주 버프 먼저 적용
            if (plugin.lordManager.isLord(player)) {
                plugin.lordManager.applyLordBonuses(player)
            }

            // Find closest stone of player's nation
            val closestStone = allStones
                .filter { it.ownerGroup == playerGroup }
                .filter { it.location.world == player.world }
                .minByOrNull { it.location.distance(player.location) }

            closestStone?.let { stone ->
                val tier = stone.currentTier.getTierLevel()
                val radius = plugin.configManager.getStoneEffectRadius(tier)
                val distance = stone.location.distance(player.location)

                // Check if player is within radius
                if (distance <= radius) {
                    applyEffects(player, tier)
                }
            }
        }
    }

    /**
     * Apply potion effects to player based on stone tier
     */
    private fun applyEffects(player: Player, tier: Int) {
        val effects = plugin.configManager.getStoneEffects(tier)
        val duration = (plugin.configManager.getEffectApplyInterval() * 2) // Duration slightly longer than interval

        effects.forEach { effectStr ->
            val parts = effectStr.split(":")
            if (parts.size == 2) {
                try {
                    val effectType = PotionEffectType.getByName(parts[0])
                    val amplifier = parts[1].toInt() - 1 // Minecraft uses 0-based amplifier

                    if (effectType != null) {
                        // Remove existing effect if present to refresh duration
                        if (player.hasPotionEffect(effectType)) {
                            player.removePotionEffect(effectType)
                        }

                        player.addPotionEffect(
                            PotionEffect(
                                effectType,
                                duration.toInt(),
                                amplifier,
                                true,  // ambient
                                false, // particles (less visual clutter)
                                true   // icon
                            )
                        )
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("Invalid effect format in tier-$tier: $effectStr")
                }
            }
        }
    }

    /**
     * Get defense bonus for player at location
     * Returns 0.0 to 1.0 (0% to 100% damage reduction)
     */
    fun getDefenseBonus(player: Player): Double {
        if (!plugin.configManager.isStoneAbilitiesEnabled()) return 0.0

        val playerGroup = PlayerGroupCache.getPlayerGroup(player)
        if (playerGroup == "팀없음") return 0.0

        val allStones = plugin.databaseManager.getAllStones()
        val closestStone = allStones
            .filter { it.ownerGroup == playerGroup }
            .filter { it.location.world == player.world }
            .minByOrNull { it.location.distance(player.location) }

        closestStone?.let { stone ->
            val tier = stone.currentTier.getTierLevel()
            val radius = plugin.configManager.getStoneEffectRadius(tier)
            val distance = stone.location.distance(player.location)

            if (distance <= radius) {
                return plugin.configManager.getStoneDefenseBonus(tier)
            }
        }

        return 0.0
    }

    /**
     * Get attack bonus for player at location
     * Returns 0.0 to 1.0 (0% to 100% damage increase)
     */
    fun getAttackBonus(player: Player): Double {
        if (!plugin.configManager.isStoneAbilitiesEnabled()) return 0.0

        val playerGroup = PlayerGroupCache.getPlayerGroup(player)
        if (playerGroup == "팀없음") return 0.0

        val allStones = plugin.databaseManager.getAllStones()
        val closestStone = allStones
            .filter { it.ownerGroup == playerGroup }
            .filter { it.location.world == player.world }
            .minByOrNull { it.location.distance(player.location) }

        closestStone?.let { stone ->
            val tier = stone.currentTier.getTierLevel()
            val radius = plugin.configManager.getStoneEffectRadius(tier)
            val distance = stone.location.distance(player.location)

            if (distance <= radius) {
                return plugin.configManager.getStoneAttackBonus(tier)
            }
        }

        return 0.0
    }

    /**
     * Shutdown ability system
     */
    fun shutdown() {
        abilityTask?.cancel()
        abilityTask = null
    }
}

