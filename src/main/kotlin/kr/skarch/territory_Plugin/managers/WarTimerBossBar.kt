package kr.skarch.territory_Plugin.managers

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.Bukkit
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.boss.BossBar
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable

class WarTimerBossBar(private val plugin: Territory_Plugin) {

    private var bossBar: BossBar? = null
    private var updateTask: BukkitRunnable? = null
    private var isEnabled = false

    /**
     * 보스바 시스템 초기화
     */
    fun initialize() {
        isEnabled = plugin.configManager.isWarTimerBossBarEnabled()

        if (!isEnabled) {
            return
        }

        // 1초마다 보스바 업데이트
        updateTask = object : BukkitRunnable() {
            override fun run() {
                updateBossBar()
            }
        }
        updateTask?.runTaskTimer(plugin, 0L, 20L) // 1초마다
    }

    /**
     * 보스바 업데이트
     */
    private fun updateBossBar() {
        if (!isEnabled) return

        val warActive = plugin.warManager.isGlobalWarActive()
        val remaining = plugin.warManager.getWarTimeRemaining()

        if (warActive && remaining != null && remaining > 0) {
            // 전쟁 중 - 보스바 표시
            if (bossBar == null) {
                createBossBar()
            }

            val warDuration = plugin.configManager.getWarDuration()
            val progress = remaining.toDouble() / warDuration.toDouble()

            // 제목 업데이트
            val hours = remaining / 3600
            val minutes = (remaining % 3600) / 60
            val seconds = remaining % 60

            val title = if (hours > 0) {
                "§c⚔ 전쟁 종료까지: §f${hours}시간 ${minutes}분 ${seconds}초"
            } else if (minutes > 0) {
                "§c⚔ 전쟁 종료까지: §f${minutes}분 ${seconds}초"
            } else {
                "§c⚔ 전쟁 종료까지: §f${seconds}초"
            }

            bossBar?.setTitle(title)
            bossBar?.progress = progress.coerceIn(0.0, 1.0)

            // 남은 시간에 따라 색상 변경
            val color = when {
                remaining > 1800 -> BarColor.GREEN    // 30분 이상: 초록색
                remaining > 600 -> BarColor.YELLOW    // 10분 이상: 노란색
                remaining > 300 -> BarColor.RED       // 5분 이상: 빨간색
                else -> BarColor.PURPLE               // 5분 미만: 보라색
            }
            bossBar?.color = color

            // 모든 온라인 플레이어 추가
            Bukkit.getOnlinePlayers().forEach { player ->
                if (!bossBar!!.players.contains(player)) {
                    bossBar?.addPlayer(player)
                }
            }

        } else {
            // 전쟁 중이 아님 - 보스바 제거
            removeBossBar()
        }
    }

    /**
     * 보스바 생성
     */
    private fun createBossBar() {
        bossBar = Bukkit.createBossBar(
            "§c⚔ 전쟁 진행 중...",
            BarColor.GREEN,
            BarStyle.SOLID
        )
        bossBar?.isVisible = true
    }

    /**
     * 보스바 제거
     */
    private fun removeBossBar() {
        bossBar?.removeAll()
        bossBar = null
    }

    /**
     * 플레이어가 접속했을 때 보스바 추가
     */
    fun addPlayer(player: Player) {
        if (!isEnabled) return
        if (plugin.warManager.isGlobalWarActive() && bossBar != null) {
            bossBar?.addPlayer(player)
        }
    }

    /**
     * 플레이어가 퇴장했을 때 보스바 제거
     */
    fun removePlayer(player: Player) {
        bossBar?.removePlayer(player)
    }

    /**
     * 시스템 종료
     */
    fun shutdown() {
        updateTask?.cancel()
        updateTask = null
        removeBossBar()
    }

    /**
     * 설정 리로드
     */
    fun reload() {
        shutdown()
        initialize()
    }
}

