package kr.skarch.territory_Plugin.config

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * 다국어 메시지 관리자
 */
class LangManager(private val plugin: Territory_Plugin) {

    private lateinit var langFile: File
    lateinit var lang: FileConfiguration

    fun initialize() {
        langFile = File(plugin.dataFolder, "lang.yml")
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false)
        }
        lang = YamlConfiguration.loadConfiguration(langFile)
        plugin.logger.info("언어 파일 로드 완료")
    }

    fun reload() {
        lang = YamlConfiguration.loadConfiguration(langFile)
        plugin.logger.info("언어 파일 리로드 완료")
    }

    /**
     * 메시지 가져오기 (플레이스홀더 치환 지원)
     */
    fun getMessage(path: String, vararg replacements: Pair<String, Any>): String {
        var message = lang.getString(path, "§c[Missing: $path]") ?: "§c[Missing: $path]"

        // 색상 코드 변환
        message = message.replace("&", "§")

        // 플레이스홀더 치환
        replacements.forEach { (key, value) ->
            message = message.replace("{$key}", value.toString())
        }

        return message
    }

    /**
     * 리스트 메시지 가져오기
     */
    fun getMessageList(path: String, vararg replacements: Pair<String, Any>): List<String> {
        val list = lang.getStringList(path)
        return list.map { line ->
            var message = line.replace("&", "§")
            replacements.forEach { (key, value) ->
                message = message.replace("{$key}", value.toString())
            }
            message
        }
    }

    /**
     * 프리픽스와 함께 메시지 전송
     */
    fun getMessageWithPrefix(path: String, vararg replacements: Pair<String, Any>): String {
        val prefix = getMessage("prefix")
        val message = getMessage(path, *replacements)
        return prefix + message
    }

    // ===== 빠른 접근 메서드들 =====

    fun getPrefix() = getMessage("prefix")

    // 일반 메시지
    fun getNoPermission() = getMessage("no_permission")
    fun getPlayerOnly() = getMessage("player_only")

    // 아이템
    fun getItemName(itemKey: String) = getMessage("items.$itemKey.name")
    fun getItemLore(itemKey: String) = getMessageList("items.$itemKey.lore")

    // 점령석
    fun getStonePlaced() = getMessage("stone.placed")
    fun getStonePlacedNation(nation: String) = getMessage("stone.placed_nation", "nation" to nation)
    fun getStonePlacedTier(tier: String) = getMessage("stone.placed_tier", "tier" to tier)
    fun getStoneUpgraded(tier: String) = getMessage("stone.upgraded", "tier" to tier)
    fun getStoneUpgradedRadius(radius: Int, size: Int) =
        getMessage("stone.upgraded_radius", "radius" to radius, "size" to size)
    fun getStoneNotOwner() = getMessage("stone.not_owner")
    fun getStoneDestroyedConquest() = getMessage("stone.destroyed_conquest")
    fun getStoneNoStoneHere() = getMessage("stone.no_stone_here")

    // GUI
    fun getGuiTitle() = getMessage("gui.upgrade.title")
    fun getGuiCurrentTierName(tier: String) = getMessage("gui.upgrade.current_tier_name", "tier" to tier)
    fun getGuiNextTierName(tier: String) = getMessage("gui.upgrade.next_tier_name", "tier" to tier)
    fun getGuiCheckPass() = getMessage("gui.upgrade.check_pass")
    fun getGuiCheckFail() = getMessage("gui.upgrade.check_fail")

    // 영토 보호
    fun getProtectionCannotBreak(nation: String) = getMessage("protection.cannot_break", "nation" to nation)
    fun getProtectionCannotBreakUnclaimed() = getMessage("protection.cannot_break_unclaimed")
    fun getProtectionCannotPlace(nation: String) = getMessage("protection.cannot_place", "nation" to nation)
    fun getProtectionCannotPlaceUnclaimed() = getMessage("protection.cannot_place_unclaimed")
    fun getProtectionCannotInteract(nation: String) = getMessage("protection.cannot_interact", "nation" to nation)
    fun getProtectionCannotInteractUnclaimed() = getMessage("protection.cannot_interact_unclaimed")

    // 전쟁
    fun getWarCannotPvp() = getMessage("war.cannot_pvp")
    fun getWarDeclared(nation: String, minutes: Int) =
        getMessage("war.declared", "nation" to nation, "minutes" to minutes)
    fun getWarStarted(nation: String) = getMessage("war.started", "nation" to nation)

    // 상태
    fun getStatusWar() = getMessage("status.war")
    fun getStatusPeace() = getMessage("status.peace")
}

