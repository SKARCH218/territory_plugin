package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

class WarManager(private val plugin: Territory_Plugin) {

    // 글로벌 전쟁 상태
    private var globalWarActive = false
    private var globalWarNumber = 0
    private var warStartTime = 0L
    private var warEndTask: BukkitRunnable? = null

    // 전쟁 준비 카운트다운
    private var preparationTask: BukkitRunnable? = null

    // 각 국가의 전쟁 통계
    private val warStats = ConcurrentHashMap<String, NationWarStats>()

    // 항복한 국가 목록 (동기화됨)
    private val surrenderedNations = Collections.synchronizedSet(mutableSetOf<String>())

    data class NationWarStats(
        var territoriesLost: Int = 0,      // 잃은 영토 수
        var territoriesGained: Int = 0,    // 획득한 영토 수
        var stonesDestroyed: Int = 0,      // 파괴한 점령석 수
        var stonesLost: Int = 0            // 잃은 점령석 수
    )

    /**
     * 글로벌 전면전 선포
     * 모든 국가가 자동으로 참여
     */
    fun declareGlobalWar() {
        // 이미 전쟁 중이면 무시
        if (globalWarActive || preparationTask != null) {
            return
        }

        val preparationTime = plugin.configManager.getWarPreparationTime()
        val nextWarNumber = plugin.databaseManager.getNextWarNumber()
        globalWarNumber = nextWarNumber

        // 초기 브로드캐스트
        broadcastComponent(
            Component.text("⚠⚠⚠ 긴급 경보! ⚠⚠⚠", NamedTextColor.DARK_RED)
        )
        broadcastComponent(
            Component.text("제 ${nextWarNumber}차 글로벌 전면전이 선포되었습니다!", NamedTextColor.RED)
        )
        broadcastComponent(
            Component.text("${preparationTime / 60}분 후 모든 국가가 전쟁 상태로 돌입합니다!", NamedTextColor.YELLOW)
        )

        val countdownAlerts = plugin.configManager.getCountdownAlerts()

        preparationTask = object : BukkitRunnable() {
            var countdown = preparationTime

            override fun run() {
                countdown--

                // 카운트다운 알림
                if (countdown in countdownAlerts) {
                    val timeText = when {
                        countdown >= 60 -> "${countdown / 60}분"
                        else -> "${countdown}초"
                    }
                    broadcastComponent(
                        Component.text("⚔ 글로벌 전쟁이 ${timeText} 후 시작됩니다!", NamedTextColor.YELLOW)
                    )
                }

                if (countdown == 0) {
                    startGlobalWar()
                    cancel()
                }
            }
        }

        preparationTask?.runTaskTimer(plugin, 0L, 20L)
    }

    /**
     * 글로벌 전쟁 시작
     */
    private fun startGlobalWar() {
        globalWarActive = true
        warStartTime = System.currentTimeMillis()
        preparationTask = null
        surrenderedNations.clear()
        warStats.clear()

        // 모든 팀 초기화 (LuckPerms 그룹 기준)
        val allTeams = plugin.configManager.getAllTeamIds()
        allTeams.forEach { teamId ->
            val luckPermsGroup = plugin.configManager.getTeamLuckPermsGroup(teamId)
            if (luckPermsGroup != null) {
                warStats[luckPermsGroup] = NationWarStats()
            }
        }

        // 데이터베이스에 전쟁 시작 기록
        plugin.databaseManager.logWarStart("GLOBAL", "GLOBAL_WAR", globalWarNumber)

        broadcastComponent(
            Component.text("⚔⚔⚔ 제 ${globalWarNumber}차 글로벌 전면전 시작! ⚔⚔⚔", NamedTextColor.DARK_RED)
        )
        broadcastComponent(
            Component.text("모든 국가가 전쟁 상태입니다!", NamedTextColor.RED)
        )

        val duration = plugin.configManager.getWarDuration()
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60

        broadcastComponent(
            Component.text("전쟁 시간: ${hours}시간 ${minutes}분", NamedTextColor.YELLOW)
        )

        // 전쟁 종료 타이머 시작
        scheduleWarEnd(duration)
    }

    /**
     * 전쟁 종료 타이머
     */
    private fun scheduleWarEnd(durationSeconds: Int) {
        warEndTask?.cancel()

        warEndTask = object : BukkitRunnable() {
            override fun run() {
                endGlobalWar(false)
            }
        }

        warEndTask?.runTaskLater(plugin, (durationSeconds * 20).toLong())
    }

    /**
     * 글로벌 전쟁 종료
     */
    private fun endGlobalWar(forcedEnd: Boolean) {
        if (!globalWarActive) return

        globalWarActive = false
        warEndTask?.cancel()
        warEndTask = null

        val endReason = if (forcedEnd) "관리자 강제 종료" else "시간 종료"

        broadcastComponent(
            Component.text("✓✓✓ 제 ${globalWarNumber}차 글로벌 전쟁 종료! ✓✓✓", NamedTextColor.GREEN)
        )
        broadcastComponent(
            Component.text("종료 사유: $endReason", NamedTextColor.YELLOW)
        )

        // 승자 결정 및 항복비 분배
        distributeWarRewards()

        // 통계 발표
        announceWarResults()

        // 전쟁 쿨타임 설정
        plugin.databaseManager.setWarCooldown("GLOBAL")

        // 데이터베이스에 전쟁 종료 기록
        plugin.databaseManager.logWarEnd("GLOBAL", 0, 0)

        warStats.clear()
        surrenderedNations.clear()
    }

    /**
     * 국가 항복
     */
    fun surrender(nationName: String, player: org.bukkit.entity.Player): Boolean {
        if (!globalWarActive) {
            player.sendMessage("§c현재 전쟁 중이 아닙니다!")
            return false
        }

        if (surrenderedNations.contains(nationName)) {
            player.sendMessage("§c이미 항복한 국가입니다!")
            return false
        }

        // 항복비 계산
        val surrenderCost = calculateSurrenderCost(nationName)

        // Vault 연동하여 국가 금고에서 차감 (추후 구현 가능)
        // 현재는 플레이어에게서 차감
        val economy = plugin.server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)?.provider

        if (economy != null && economy.has(player, surrenderCost)) {
            economy.withdrawPlayer(player, surrenderCost)

            surrenderedNations.add(nationName)

            broadcastComponent(
                Component.text("${plugin.configManager.getColoredNationName(nationName)} 국가가 항복했습니다!", NamedTextColor.YELLOW)
            )
            broadcastComponent(
                Component.text("항복비: $${String.format("%,.0f", surrenderCost)}", NamedTextColor.GOLD)
            )

            // 남은 국가가 1개면 전쟁 종료
            checkWarEndCondition()

            return true
        } else {
            player.sendMessage("§c항복 비용이 부족합니다! (필요: $${String.format("%,.0f", surrenderCost)})")
            return false
        }
    }

    /**
     * 항복비 계산
     */
    private fun calculateSurrenderCost(nationName: String): Double {
        val baseCost = plugin.configManager.getSurrenderBaseCost()
        val stats = warStats[nationName] ?: NationWarStats()

        // 잃은 영토 1개당 감소 비율
        val lostTerritoryDiscount = plugin.configManager.getSurrenderLostTerritoryDiscount()

        // 획득한 영토 1개당 증가 비율
        val gainedTerritoryPenalty = plugin.configManager.getSurrenderGainedTerritoryPenalty()

        val lostDiscount = stats.territoriesLost * lostTerritoryDiscount
        val gainedPenalty = stats.territoriesGained * gainedTerritoryPenalty

        val finalCost = baseCost * (1.0 - lostDiscount + gainedPenalty)

        return maxOf(0.0, finalCost) // 최소 0
    }

    /**
     * 전쟁 종료 조건 확인
     */
    private fun checkWarEndCondition() {
        // warStats에 있는 팀들 중 항복하지 않은 팀 확인
        val remainingTeams = warStats.keys.filter { !surrenderedNations.contains(it) }

        if (remainingTeams.size <= 1) {
            // 1개 국가만 남음 = 즉시 종료
            broadcastComponent(
                Component.text("모든 국가가 항복했습니다! 전쟁을 조기 종료합니다!", NamedTextColor.GREEN)
            )
            endGlobalWar(true)
        }
    }

    /**
     * 전쟁 보상 분배
     */
    private fun distributeWarRewards() {
        // warStats에 있는 팀들 중 항복하지 않은 팀
        val remainingTeams = warStats.keys.filter { !surrenderedNations.contains(it) }.toList()

        // 총 항복비 계산
        val totalSurrenderMoney = surrenderedNations.sumOf { calculateSurrenderCost(it) }

        if (remainingTeams.size == 1) {
            // 1개 승전국만 남음 = 모든 항복비 독식
            val winner = remainingTeams[0]
            broadcastComponent(
                Component.text("🏆 승전국: ${plugin.configManager.getColoredNationName(winner)}", NamedTextColor.GOLD)
            )
            broadcastComponent(
                Component.text("획득 항복비: $${String.format("%,.0f", totalSurrenderMoney)}", NamedTextColor.GOLD)
            )

            // 승전국 온라인 플레이어들에게 분배
            distributeToTeam(winner, totalSurrenderMoney)

        } else if (remainingTeams.size > 1) {
            // 시간 종료 - 스코어 기반 분배
            val scores = calculateCurrentWarScore()

            // 남은 팀들의 스코어만 추출
            val remainingScores = remainingTeams.associateWith { scores[it] ?: 0.0 }
            val maxScore = remainingScores.values.maxOrNull() ?: 0.0

            // 최고 점수 팀들 찾기 (부동소수점 오차 고려)
            val epsilon = 0.001
            val winners = remainingScores.filter {
                abs(it.value - maxScore) < epsilon
            }.keys.toList()

            if (winners.size == 1) {
                // 1등이 1개 팀 = 독식
                val winner = winners[0]
                broadcastComponent(
                    Component.text("🏆 1위 승전국: ${plugin.configManager.getColoredNationName(winner)}", NamedTextColor.GOLD)
                )
                broadcastComponent(
                    Component.text("최종 점수: %.1f점".format(maxScore), NamedTextColor.YELLOW)
                )
                broadcastComponent(
                    Component.text("획득 항복비: $${String.format("%,.0f", totalSurrenderMoney)}", NamedTextColor.GOLD)
                )

                distributeToTeam(winner, totalSurrenderMoney)

            } else {
                // 동점자 여러 명 = 균등 분배
                val perTeam = totalSurrenderMoney / winners.size

                broadcastComponent(
                    Component.text("⚖ 동점! 최고 점수: %.1f점".format(maxScore), NamedTextColor.YELLOW)
                )
                broadcastComponent(
                    Component.text("항복비 균등 분배 (${winners.size}개 국가)", NamedTextColor.YELLOW)
                )

                winners.forEach { luckPermsGroup ->
                    broadcastComponent(
                        Component.text("${plugin.configManager.getColoredNationName(luckPermsGroup)}: $${String.format("%,.0f", perTeam)}", NamedTextColor.GOLD)
                    )
                    distributeToTeam(luckPermsGroup, perTeam)
                }
            }
        }
    }

    /**
     * 팀에게 돈 분배
     */
    private fun distributeToTeam(teamId: String, totalMoney: Double) {
        val luckPermsGroup = plugin.configManager.getTeamLuckPermsGroup(teamId) ?: return
        val onlineMembers = Bukkit.getOnlinePlayers().filter {
            kr.skarch.territory_Plugin.utils.PlayerGroupCache.getPlayerGroup(it) == luckPermsGroup
        }

        if (onlineMembers.isEmpty()) return

        val perPlayer = totalMoney / onlineMembers.size
        val economy = plugin.server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)?.provider

        onlineMembers.forEach { player ->
            economy?.depositPlayer(player, perPlayer)
            player.sendMessage("§a전쟁 보상: §6$${String.format("%,.0f", perPlayer)}")
        }
    }

    /**
     * 전쟁 결과 발표
     */
    private fun announceWarResults() {
        broadcastComponent(
            Component.text("=== 제 ${globalWarNumber}차 글로벌 전쟁 결과 ===", NamedTextColor.GOLD)
        )

        // 스코어 계산 및 순위 정렬
        val scores = calculateCurrentWarScore()
        val allScores = warStats.keys.associateWith { nationName ->
            if (surrenderedNations.contains(nationName)) {
                -999.0 // 항복한 국가는 최하위
            } else {
                scores[nationName] ?: 0.0
            }
        }
        val sortedByScore = allScores.entries.sortedByDescending { it.value }

        broadcastComponent(Component.text(""))
        broadcastComponent(Component.text("§6📊 최종 순위:", NamedTextColor.GOLD))

        sortedByScore.forEachIndexed { index, entry ->
            val nationName = entry.key
            val score = entry.value
            val stats = warStats[nationName] ?: return@forEachIndexed
            val displayName = plugin.configManager.getColoredNationName(nationName)
            val status = if (surrenderedNations.contains(nationName)) "§c항복" else "§a생존"

            val medal = when(index) {
                0 -> "§6🥇"
                1 -> "§7🥈"
                2 -> "§c🥉"
                else -> "§e${index + 1}."
            }

            broadcastComponent(
                Component.text("$medal $displayName - $status")
            )

            if (score > -999) {
                broadcastComponent(
                    Component.text("  §7점수: §e%.1f§7점 | 점령석: §a${stats.stonesDestroyed}§7/§c${stats.stonesLost} §7| 영토: §a${stats.territoriesGained}§7/§c${stats.territoriesLost}".format(score))
                )
            } else {
                broadcastComponent(
                    Component.text("  §7점령석: §a${stats.stonesDestroyed}§7/§c${stats.stonesLost} §7| 영토: §a${stats.territoriesGained}§7/§c${stats.territoriesLost}")
                )
            }
        }
    }

    /**
     * 영토 점령 기록
     */
    fun recordTerritoryConquest(attackerNation: String, defenderNation: String, territoryCount: Int) {
        if (!globalWarActive) return

        warStats.computeIfAbsent(attackerNation) { NationWarStats() }.territoriesGained += territoryCount
        warStats.computeIfAbsent(defenderNation) { NationWarStats() }.territoriesLost += territoryCount
    }

    /**
     * 점령석 파괴 기록
     */
    fun recordStoneDestruction(attackerNation: String, defenderNation: String) {
        if (!globalWarActive) return

        warStats.computeIfAbsent(attackerNation) { NationWarStats() }.stonesDestroyed++
        warStats.computeIfAbsent(defenderNation) { NationWarStats() }.stonesLost++
    }

    /**
     * 글로벌 전쟁 상태 확인
     */
    fun isGlobalWarActive(): Boolean = globalWarActive

    /**
     * 전쟁 종료까지 남은 시간 (초)
     * @return 남은 시간 (초), 전쟁 중이 아니면 null
     */
    fun getWarTimeRemaining(): Int? {
        if (!globalWarActive) return null

        val warDuration = plugin.configManager.getWarDuration()
        val elapsed = (System.currentTimeMillis() - warStartTime) / 1000
        val remaining = warDuration - elapsed.toInt()

        return maxOf(0, remaining)
    }

    /**
     * 전쟁 중인지 확인 (하위 호환성)
     */
    fun isInGlobalWar(nationName: String): Boolean = globalWarActive && !surrenderedNations.contains(nationName)

    /**
     * 전투 가능 여부
     */
    fun canEngage(nation1: String, nation2: String): Boolean {
        return globalWarActive &&
               !surrenderedNations.contains(nation1) &&
               !surrenderedNations.contains(nation2)
    }

    /**
     * 관리자 명령어: 전쟁 강제 종료
     */
    fun forceEndWar() {
        if (globalWarActive) {
            endGlobalWar(true)
        }
    }

    /**
     * 관리자 명령어: 전쟁 즉시 시작
     */
    fun startWarImmediately() {
        preparationTask?.cancel()
        preparationTask = null
        startGlobalWar()
    }

    /**
     * 현재 전쟁의 실시간 스코어 계산
     * 공식: (점령 - 잃음) + (킬 - 데스) / 2
     */
    fun calculateCurrentWarScore(): Map<String, Double> {
        if (!globalWarActive) return emptyMap()

        val scores = mutableMapOf<String, Double>()

        warStats.forEach { (nationName, stats) ->
            // (점령한 점령석 - 잃은 점령석) + (킬 - 데스) / 2
            val stoneScore = stats.stonesDestroyed - stats.stonesLost
            val combatScore = (stats.territoriesGained - stats.territoriesLost) / 2.0
            val totalScore = stoneScore + combatScore

            scores[nationName] = totalScore
        }

        return scores.filter { !surrenderedNations.contains(it.key) } // 항복한 국가 제외
    }

    /**
     * 전쟁 통계 조회
     */
    fun getWarStats(nationName: String): NationWarStats? {
        return warStats[nationName]
    }

    /**
     * 특정 국가가 항복했는지 확인
     */
    fun hasSurrendered(nationName: String): Boolean {
        return surrenderedNations.contains(nationName)
    }

    /**
     * Adventure Component 브로드캐스트
     */
    private fun broadcastComponent(component: Component) {
        Bukkit.getServer().sendMessage(component)
    }
}
