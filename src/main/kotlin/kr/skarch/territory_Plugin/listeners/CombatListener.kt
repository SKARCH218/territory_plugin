package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

/**
 * PvP 데미지 이벤트 리스너
 * 점령석 영역 내 공격/방어 보너스 적용
 * 적 영토 침입 시 PvP 허용 및 디버프 적용
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

    /**
     * 적 영토 침입자에게 디버프 적용
     * 평화 시에만 적용됨
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        // 청크 이동이 아니면 무시
        val from = event.from
        val to = event.to ?: return

        if (from.chunk == to.chunk) return

        val player = event.player
        val playerGroup = PlayerGroupCache.getPlayerGroup(player)

        // 팀 없는 플레이어는 무시
        if (playerGroup == "팀없음") return

        // 현재 청크 소유자 확인
        val chunkKey = "${to.world.name};${to.chunk.x};${to.chunk.z}"
        val chunkOwner = plugin.databaseManager.getChunkOwner(chunkKey)

        // 전쟁 중인지 확인
        val isWarActive = plugin.warManager.isInGlobalWar(playerGroup) ||
                         (chunkOwner != null && plugin.warManager.isInGlobalWar(chunkOwner))

        // 적 영토 침입 확인 (본인 팀이 아닌 영토)
        val isTrespassing = chunkOwner != null && chunkOwner != playerGroup

        // 평화 시 + 적 영토 침입 시 디버프 적용
        if (!isWarActive && isTrespassing) {
            applyTrespassDebuffs(player)

            // 침입 경고 메시지 (5초마다 한 번만)
            val lastWarning = trespassWarnings[player.uniqueId] ?: 0L
            val now = System.currentTimeMillis()
            if (now - lastWarning > 5000) {
                player.sendMessage("§c⚠ 경고! 적 영토에 침입 중입니다! 구속과 나약함이 적용됩니다!")
                trespassWarnings[player.uniqueId] = now
            }
        } else {
            // 본인 영토거나 전쟁 중이면 디버프 제거
            removeTrespassDebuffs(player)
        }
    }

    /**
     * 침입자 디버프 적용
     */
    private fun applyTrespassDebuffs(player: Player) {
        // 구속 II (이동 속도 감소)
        val slownessEffect = PotionEffectType.getByName("SLOWNESS")
            ?: PotionEffectType.getByName("SLOW")

        slownessEffect?.let {
            player.addPotionEffect(
                PotionEffect(it, 20 * 10, 1, false, false, true) // 10초, 레벨 II
            )
        }

        // 나약함 II (공격력 감소)
        val weaknessEffect = PotionEffectType.getByName("WEAKNESS")

        weaknessEffect?.let {
            player.addPotionEffect(
                PotionEffect(it, 20 * 10, 1, false, false, true) // 10초, 레벨 II
            )
        }
    }

    /**
     * 침입자 디버프 제거
     */
    private fun removeTrespassDebuffs(player: Player) {
        val slownessEffect = PotionEffectType.getByName("SLOWNESS")
            ?: PotionEffectType.getByName("SLOW")
        val weaknessEffect = PotionEffectType.getByName("WEAKNESS")

        slownessEffect?.let { player.removePotionEffect(it) }
        weaknessEffect?.let { player.removePotionEffect(it) }
    }

    companion object {
        // 침입 경고 메시지 쿨다운 (UUID -> 마지막 경고 시간)
        private val trespassWarnings = mutableMapOf<java.util.UUID, Long>()
    }
}



