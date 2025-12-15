package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

/**
 * PvP 데미지 이벤트 리스너
 * 점령석 영역 내 공격/방어 보너스 적용
 * 적 영토 침입 시 PvP 허용
 */
class CombatListener(private val plugin: Territory_Plugin) : Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onPlayerDamage(event: EntityDamageByEntityEvent) {
        val victim = event.entity as? Player ?: return
        val attacker = event.damager as? Player ?: return

        val attackerGroup = PlayerGroupCache.getPlayerGroup(attacker)
        val victimGroup = PlayerGroupCache.getPlayerGroup(victim)

        // 같은 팀끼리 공격 차단
        if (attackerGroup == victimGroup && plugin.configManager.isBlockFriendlyFire()) {
            event.isCancelled = true
            attacker.sendMessage("§c같은 팀원을 공격할 수 없습니다!")
            return
        }

        // 현재 위치의 영토 소유자 확인
        val location = victim.location
        val chunkKey = "${location.world.name};${location.chunk.x};${location.chunk.z}"
        val chunkOwner = plugin.databaseManager.getChunkOwner(chunkKey)

        // PvP 허용 조건:
        // 1. 전쟁 중
        // 2. 피해자가 적 영토에 침입한 경우 (피해자 팀 != 청크 소유 팀 && 공격자 팀 == 청크 소유 팀)
        val isWarActive = plugin.warManager.isInGlobalWar(attackerGroup) ||
                         plugin.warManager.isInGlobalWar(victimGroup)

        val isVictimTrespassing = chunkOwner != null &&
                                  chunkOwner != victimGroup &&
                                  chunkOwner == attackerGroup

        // PvP 차단 조건: 전쟁 중도 아니고, 피해자가 적 영토 침입도 아닌 경우
        if (!isWarActive && !isVictimTrespassing) {
            event.isCancelled = true
            attacker.sendMessage("§c전쟁 중이거나 적이 우리 영토에 침입했을 때만 공격할 수 있습니다!")
            return
        }

        // 적 영토 침입 시 메시지
        if (isVictimTrespassing && !isWarActive) {
            attacker.sendMessage("§e적 영토 방어! §c${victim.name}§e를 공격합니다!")
            victim.sendMessage("§c경고! 적 영토에 침입하여 공격받고 있습니다!")
        }

        // 점령석 능력 적용
        if (!plugin.configManager.isStoneAbilitiesEnabled()) return

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



