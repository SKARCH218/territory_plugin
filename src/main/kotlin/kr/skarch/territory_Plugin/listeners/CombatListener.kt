package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * PvP 데미지 이벤트 리스너
 * 점령석 영역 내 공격/방어 보너스 적용
 */
class CombatListener(private val plugin: Territory_Plugin) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        if (!plugin.configManager.isStoneAbilitiesEnabled()) return

        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return

        // Get bonuses
        val attackBonus = plugin.stoneAbilityManager.getAttackBonus(attacker)
        val defenseBonus = plugin.stoneAbilityManager.getDefenseBonus(victim)

        var finalDamage = event.damage

        // Apply attack bonus
        if (attackBonus > 0) {
            finalDamage *= (1.0 + attackBonus)
        }

        // Apply defense bonus (damage reduction)
        if (defenseBonus > 0) {
            finalDamage *= (1.0 - defenseBonus)
        }

        event.damage = finalDamage
    }
}

