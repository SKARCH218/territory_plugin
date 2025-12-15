package kr.skarch.territory_Plugin.config

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class ConfigManager(private val plugin: Territory_Plugin) {

    private lateinit var configFile: File
    private lateinit var teamFile: File

    lateinit var config: FileConfiguration
    lateinit var teamConfig: FileConfiguration

    fun initialize() {
        // Create plugin data folder if not exists
        if (!plugin.dataFolder.exists()) {
            plugin.dataFolder.mkdirs()
        }

        // Load config.yml
        configFile = File(plugin.dataFolder, "config.yml")
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false)
        }
        config = YamlConfiguration.loadConfiguration(configFile)

        // Load team.yml
        teamFile = File(plugin.dataFolder, "team.yml")
        if (!teamFile.exists()) {
            plugin.saveResource("team.yml", false)
        }
        teamConfig = YamlConfiguration.loadConfiguration(teamFile)

        // 로그 제거 (불필요)
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(configFile)
        teamConfig = YamlConfiguration.loadConfiguration(teamFile)
        // 로그 제거 (불필요)
    }

    // ===== Config.yml 설정값 가져오기 =====

    /**
     * 전쟁이 가능한 월드 목록
     */
    fun getWarWorlds(): List<String> {
        return config.getStringList("war-worlds")
    }

    /**
     * 특정 월드가 전쟁 월드인지 확인
     */
    fun isWarWorld(worldName: String): Boolean {
        return getWarWorlds().contains(worldName)
    }

    /**
     * 전쟁 준비 시간 (초)
     */
    fun getWarPreparationTime(): Int {
        return config.getInt("war.preparation-time", 600)
    }

    /**
     * 전쟁 지속 시간 (초) - NEW!
     * 기본값: 3600초 (1시간)
     */
    fun getWarDuration(): Int {
        return config.getInt("war.duration", 3600)
    }

    /**
     * 카운트다운 알림 시점 목록 (초)
     */
    fun getCountdownAlerts(): List<Int> {
        return config.getIntegerList("war.countdown-alerts").ifEmpty { listOf(300, 60, 30, 10) }
    }

    /**
     * 전쟁 최소 지속 시간 (초)
     */
    fun getWarMinimumDuration(): Int {
        return config.getInt("war.minimum-duration", 1800)
    }

    /**
     * 전쟁 선포 비용 활성화 여부
     */
    fun isWarDeclarationCostEnabled(): Boolean {
        return config.getBoolean("war.declaration-cost.enabled", true)
    }

    /**
     * 전쟁 선포 비용 (돈)
     */
    fun getWarDeclarationCost(): Double {
        return config.getDouble("war.declaration-cost.money", 50000.0)
    }

    /**
     * 전쟁 선포 쿨타임 (초)
     */
    fun getWarDeclarationCooldown(): Long {
        return config.getLong("war.declaration-cooldown", 7200L)
    }

    /**
     * 항복 기본 비용 - NEW!
     * 기본값: 100000.0
     */
    fun getSurrenderBaseCost(): Double {
        return config.getDouble("war.surrender.base-cost", 100000.0)
    }

    /**
     * 잃은 영토 1개당 항복비 감소 비율 - NEW!
     * 기본값: 0.05 (5%)
     */
    fun getSurrenderLostTerritoryDiscount(): Double {
        return config.getDouble("war.surrender.lost-territory-discount", 0.05)
    }

    /**
     * 획득한 영토 1개당 항복비 증가 비율 - NEW!
     * 기본값: 0.1 (10%)
     */
    fun getSurrenderGainedTerritoryPenalty(): Double {
        return config.getDouble("war.surrender.gained-territory-penalty", 0.1)
    }

    /**
     * 전쟁 선포국 버프 활성화 여부
     */
    fun isAttackerBuffsEnabled(): Boolean {
        return config.getBoolean("war.attacker-buffs.enabled", true)
    }

    /**
     * 전쟁 선포국 버프 지속 시간 (초)
     */
    fun getAttackerBuffsDuration(): Int {
        return config.getInt("war.attacker-buffs.duration", 1800)
    }

    /**
     * 전쟁 선포국 버프 효과 목록
     */
    fun getAttackerBuffEffects(): List<String> {
        return config.getStringList("war.attacker-buffs.effects")
    }

    /**
     * 전쟁 선포국 버프 브로드캐스트 여부
     */
    fun isAttackerBuffsBroadcast(): Boolean {
        return config.getBoolean("war.attacker-buffs.broadcast-message", true)
    }

    /**
     * 전쟁 보상 시스템 활성화 여부
     */
    fun isWarRewardsEnabled(): Boolean {
        return config.getBoolean("war.rewards.enabled", true)
    }

    /**
     * 승리 보상 돈
     */
    fun getVictoryRewardMoney(): Double {
        return config.getDouble("war.rewards.victory.money", 100000.0)
    }

    /**
     * 승리 보상 아이템 목록
     */
    fun getVictoryRewardItems(): List<String> {
        return config.getStringList("war.rewards.victory.items")
    }

    /**
     * 승리 보상 명령어 목록
     */
    fun getVictoryRewardCommands(): List<String> {
        return config.getStringList("war.rewards.victory.commands")
    }

    /**
     * 참여 보상: 몇 킬마다 보상
     */
    fun getParticipationKillsPerReward(): Int {
        return config.getInt("war.rewards.participation.kills-per-reward", 10)
    }

    /**
     * 참여 보상 돈
     */
    fun getParticipationRewardMoney(): Double {
        return config.getDouble("war.rewards.participation.money", 5000.0)
    }

    /**
     * MVP 보상 활성화 여부
     */
    fun isMvpRewardEnabled(): Boolean {
        return config.getBoolean("war.rewards.mvp.enabled", true)
    }

    /**
     * MVP 보상 돈
     */
    fun getMvpRewardMoney(): Double {
        return config.getDouble("war.rewards.mvp.money", 50000.0)
    }

    /**
     * MVP 보상 아이템 목록
     */
    fun getMvpRewardItems(): List<String> {
        return config.getStringList("war.rewards.mvp.items")
    }

    /**
     * 점령석 티어별 능력 활성화 여부
     */
    fun isStoneAbilitiesEnabled(): Boolean {
        return config.getBoolean("occupation-stone.tier-abilities.enabled", true)
    }

    /**
     * 특정 티어의 효과 반경
     */
    fun getStoneEffectRadius(tier: Int): Int {
        return config.getInt("occupation-stone.tier-abilities.tier-$tier.effect-radius", 15)
    }

    /**
     * 특정 티어의 효과 목록
     */
    fun getStoneEffects(tier: Int): List<String> {
        return config.getStringList("occupation-stone.tier-abilities.tier-$tier.effects")
    }

    /**
     * 특정 티어의 방어 보너스
     */
    fun getStoneDefenseBonus(tier: Int): Double {
        return config.getDouble("occupation-stone.tier-abilities.tier-$tier.defense-bonus", 0.0)
    }

    /**
     * 특정 티어의 공격 보너스
     */
    fun getStoneAttackBonus(tier: Int): Double {
        return config.getDouble("occupation-stone.tier-abilities.tier-$tier.attack-bonus", 0.0)
    }

    /**
     * 효과 적용 주기 (틱)
     */
    fun getEffectApplyInterval(): Long {
        return config.getLong("occupation-stone.effect-apply-interval", 60L)
    }

    /**
     * 플레이어 그룹 캐시 TTL (밀리초)
     */
    fun getPlayerGroupCacheTTL(): Long {
        return config.getLong("cache.player-group-ttl", 300000L)
    }

    /**
     * 캐시 정리 주기 (밀리초)
     */
    fun getCacheCleanupInterval(): Long {
        return config.getLong("cache.cleanup-interval", 60000L)
    }

    /**
     * 느린 쿼리 경고 임계값 (밀리초)
     */
    fun getSlowQueryThreshold(): Long {
        return config.getLong("performance.slow-query-threshold", 100L)
    }

    /**
     * 최대 DB 연결 풀 크기
     */
    fun getMaxDbConnections(): Int {
        return config.getInt("performance.max-db-connections", 10)
    }

    /**
     * 평화 시 본인 팀 땅만 상호작용 가능한지
     */
    fun isPeacefulOwnTeamOnly(): Boolean {
        return config.getBoolean("territory-protection.peaceful-own-team-only", true)
    }

    /**
     * 평화 시 주인 없는 땅 상호작용 불가인지
     */
    fun isPeacefulUnclaimedDeny(): Boolean {
        return config.getBoolean("territory-protection.peaceful-unclaimed-deny", true)
    }

    /**
     * 전쟁 중 모든 땅 상호작용 가능한지
     */
    fun isWarAllLandsAccessible(): Boolean {
        return config.getBoolean("territory-protection.war-all-lands-accessible", true)
    }

    /**
     * 같은 팀끼리 PVP 차단
     */
    fun isBlockFriendlyFire(): Boolean {
        return config.getBoolean("territory-protection.block-friendly-fire", true)
    }

    /**
     * 메시지 프리픽스
     */
    fun getMessagePrefix(): String {
        return config.getString("messages.prefix", "&6[Territory]&r ")?.replace("&", "§") ?: "§6[Territory]§r "
    }

    /**
     * 업그레이드 비용 활성화 여부
     */
    fun isUpgradeCostEnabled(): Boolean {
        return config.getBoolean("occupation-stone.upgrade-cost-enabled", false)
    }

    /**
     * 특정 티어 업그레이드에 필요한 돈
     */
    fun getUpgradeMoney(fromTier: Int, toTier: Int): Double {
        return config.getDouble("occupation-stone.upgrade-requirements.tier-$fromTier-to-$toTier.money", 0.0)
    }

    /**
     * 특정 티어 업그레이드에 필요한 점령 시간 (초)
     */
    fun getUpgradeOccupationTime(fromTier: Int, toTier: Int): Long {
        return config.getLong("occupation-stone.upgrade-requirements.tier-$fromTier-to-$toTier.occupation-time", 0)
    }

    /**
     * 점령석 생성 Y 좌표 (검증 포함)
     */
    fun getStoneSpawnY(): Int {
        val y = config.getInt("occupation-stone.spawn-y-coordinate", 70)

        // Validate Y coordinate (Minecraft 1.18+ range: -64 to 320)
        return when {
            y < -64 -> {
                plugin.logger.warning("Invalid spawn-y-coordinate: $y (too low). Using -64")
                -64
            }
            y > 320 -> {
                plugin.logger.warning("Invalid spawn-y-coordinate: $y (too high). Using 320")
                320
            }
            else -> y
        }
    }

    /**
     * 점령석 블록 재질
     */
    fun getStoneBlockMaterial(): String {
        return config.getString("occupation-stone.block-material", "OBSIDIAN") ?: "OBSIDIAN"
    }

    // ===== Team.yml 설정값 가져오기 =====

    /**
     * 모든 팀 ID 목록
     */
    fun getTeamIds(): Set<String> {
        return teamConfig.getConfigurationSection("teams")?.getKeys(false) ?: emptySet()
    }

    /**
     * 팀의 표시 이름
     */
    fun getTeamDisplayName(teamId: String): String {
        return teamConfig.getString("teams.$teamId.display-name") ?: teamId
    }

    /**
     * 팀의 LuckPerms 그룹명
     */
    fun getTeamLuckPermsGroup(teamId: String): String {
        return teamConfig.getString("teams.$teamId.luckperms-group") ?: teamId
    }

    /**
     * 팀의 색상
     */
    fun getTeamColor(teamId: String): String {
        return teamConfig.getString("teams.$teamId.color") ?: "#FFFFFF"
    }

    /**
     * 팀의 설명
     */
    fun getTeamDescription(teamId: String): String {
        return teamConfig.getString("teams.$teamId.description") ?: ""
    }

    /**
     * 팀의 영주 목록 (마인크래프트 닉네임)
     */
    fun getTeamLords(teamId: String): List<String> {
        return teamConfig.getStringList("teams.$teamId.lords")
    }

    /**
     * 특정 플레이어가 특정 팀의 영주인지 확인
     */
    fun isLord(playerName: String, teamId: String): Boolean {
        return getTeamLords(teamId).any { it.equals(playerName, ignoreCase = true) }
    }

    /**
     * 특정 플레이어가 영주인지 확인 (모든 팀 검색)
     */
    fun isLordOfAnyTeam(playerName: String): Boolean {
        return getTeamIds().any { teamId -> isLord(playerName, teamId) }
    }

    /**
     * 플레이어가 영주인 팀 ID 찾기
     */
    fun getLordTeamId(playerName: String): String? {
        return getTeamIds().firstOrNull { teamId -> isLord(playerName, teamId) }
    }

    /**
     * LuckPerms 그룹명으로 팀 ID 찾기
     */
    fun getTeamIdByLuckPermsGroup(groupName: String): String? {
        for (teamId in getTeamIds()) {
            if (getTeamLuckPermsGroup(teamId) == groupName) {
                return teamId
            }
        }
        return null
    }

    /**
     * 팀이 존재하는지 확인
     */
    fun teamExists(teamId: String): Boolean {
        return getTeamIds().contains(teamId)
    }

    /**
     * 모든 팀 ID 목록 가져오기
     */
    fun getAllTeamIds(): List<String> {
        return getTeamIds().toList()
    }

    /**
     * 팀의 전체 정보 가져오기
     */
    fun getTeamInfo(teamId: String): TeamInfo? {
        if (!teamExists(teamId)) return null

        return TeamInfo(
            id = teamId,
            displayName = getTeamDisplayName(teamId),
            luckPermsGroup = getTeamLuckPermsGroup(teamId),
            color = getTeamColor(teamId),
            description = getTeamDescription(teamId),
            lords = getTeamLords(teamId)
        )
    }

    /**
     * 모든 팀 정보 가져오기
     */
    fun getAllTeams(): List<TeamInfo> {
        return getTeamIds().mapNotNull { getTeamInfo(it) }
    }

    /**
     * 국가 이름에 색깔 적용 (LuckPerms 그룹명으로)
     */
    fun getColoredNationName(luckPermsGroup: String): String {
        val teamId = getTeamIdByLuckPermsGroup(luckPermsGroup)
        return if (teamId != null) {
            val displayName = getTeamDisplayName(teamId)
            val color = hexToMinecraftColor(getTeamColor(teamId))
            "$color$displayName"
        } else {
            "§f$luckPermsGroup"
        }
    }

    /**
     * 국가 표시명에 색깔 적용 (팀 ID로)
     */
    fun getColoredTeamDisplayName(teamId: String): String {
        val displayName = getTeamDisplayName(teamId)
        val color = hexToMinecraftColor(getTeamColor(teamId))
        return "$color$displayName"
    }

    /**
     * Hex 색상을 Minecraft 색상 코드로 변환
     */
    private fun hexToMinecraftColor(hex: String): String {
        // Remove # if present
        val cleanHex = hex.removePrefix("#")

        // If hex is valid 6-digit format, use it
        return if (cleanHex.matches(Regex("[0-9A-Fa-f]{6}"))) {
            "§x§${cleanHex[0]}§${cleanHex[1]}§${cleanHex[2]}§${cleanHex[3]}§${cleanHex[4]}§${cleanHex[5]}"
        } else {
            // Fallback to white if invalid
            "§f"
        }
    }
}

/**
 * 팀 정보 데이터 클래스
 */
data class TeamInfo(
    val id: String,
    val displayName: String,
    val luckPermsGroup: String,
    val color: String,
    val description: String,
    val lords: List<String> = emptyList()
)

