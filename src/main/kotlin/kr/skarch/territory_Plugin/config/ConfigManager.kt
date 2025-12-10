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

        plugin.logger.info("설정 파일 로드 완료")
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(configFile)
        teamConfig = YamlConfiguration.loadConfiguration(teamFile)
        plugin.logger.info("설정 파일 리로드 완료")
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
     * 전쟁 최소 지속 시간 (초)
     */
    fun getWarMinimumDuration(): Int {
        return config.getInt("war.minimum-duration", 1800)
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
     * 팀의 전체 정보 가져오기
     */
    fun getTeamInfo(teamId: String): TeamInfo? {
        if (!teamExists(teamId)) return null

        return TeamInfo(
            id = teamId,
            displayName = getTeamDisplayName(teamId),
            luckPermsGroup = getTeamLuckPermsGroup(teamId),
            color = getTeamColor(teamId),
            description = getTeamDescription(teamId)
        )
    }

    /**
     * 모든 팀 정보 가져오기
     */
    fun getAllTeams(): List<TeamInfo> {
        return getTeamIds().mapNotNull { getTeamInfo(it) }
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
    val description: String
)

