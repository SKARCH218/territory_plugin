package kr.skarch.territory_Plugin.config

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * 아이템 커스텀 모델 데이터 관리자
 */
class ItemManager(private val plugin: Territory_Plugin) {

    private lateinit var itemsFile: File
    lateinit var items: FileConfiguration

    fun initialize() {
        itemsFile = File(plugin.dataFolder, "items.yml")
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false)
        }
        items = YamlConfiguration.loadConfiguration(itemsFile)
    }

    fun reload() {
        items = YamlConfiguration.loadConfiguration(itemsFile)
    }

    /**
     * 커스텀 모델 데이터 가져오기
     * @param itemKey 아이템 키 (예: "Occupation_Stone_Tier_I")
     * @return 커스텀 모델 데이터 (0이면 사용 안 함)
     */
    fun getCustomModelData(itemKey: String): Int {
        return items.getInt(itemKey, 0)
    }

    /**
     * 커스텀 모델 데이터 사용 여부 확인
     */
    fun hasCustomModelData(itemKey: String): Boolean {
        return getCustomModelData(itemKey) > 0
    }

    // ===== 빠른 접근 메서드들 =====

    fun getOccupationStoneTier1CustomModelData(): Int {
        return getCustomModelData("Occupation_Stone_Tier_I")
    }

    fun getWarScrollCustomModelData(): Int {
        return getCustomModelData("War_Declaration_Scroll")
    }
}

