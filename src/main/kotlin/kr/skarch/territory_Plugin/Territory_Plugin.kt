package kr.skarch.territory_Plugin

import kr.skarch.territory_Plugin.commands.TerritoryCommand
import kr.skarch.territory_Plugin.commands.TerritoryTabCompleter
import kr.skarch.territory_Plugin.commands.WarConfirmCommand
import kr.skarch.territory_Plugin.config.ConfigManager
import kr.skarch.territory_Plugin.config.ItemManager
import kr.skarch.territory_Plugin.config.LangManager
import kr.skarch.territory_Plugin.database.DatabaseManager
import kr.skarch.territory_Plugin.listeners.CombatListener
import kr.skarch.territory_Plugin.listeners.InteractionListener
import kr.skarch.territory_Plugin.listeners.LuckPermsListener
import kr.skarch.territory_Plugin.listeners.StoneUpgradeListener
import kr.skarch.territory_Plugin.listeners.WarDeclarationListener
import kr.skarch.territory_Plugin.managers.BlueMapManager
import kr.skarch.territory_Plugin.managers.StatsManager
import kr.skarch.territory_Plugin.managers.StoneAbilityManager
import kr.skarch.territory_Plugin.managers.TerritoryManager
import kr.skarch.territory_Plugin.managers.WarManager
import kr.skarch.territory_Plugin.utils.PlayerGroupCache
import net.luckperms.api.LuckPermsProvider
import org.bukkit.plugin.java.JavaPlugin

class Territory_Plugin : JavaPlugin() {

    lateinit var configManager: ConfigManager
    lateinit var langManager: LangManager
    lateinit var itemManager: ItemManager
    lateinit var databaseManager: DatabaseManager
    lateinit var territoryManager: TerritoryManager
    lateinit var warManager: WarManager
    lateinit var blueMapManager: BlueMapManager
    lateinit var statsManager: StatsManager
    lateinit var stoneAbilityManager: StoneAbilityManager
    private var luckPermsListener: LuckPermsListener? = null

    // 점령석 설치 대기 중인 플레이어들 (UUID -> PendingStone)
    val pendingRegionNames = mutableMapOf<java.util.UUID, kr.skarch.territory_Plugin.models.PendingStone>()

    override fun onEnable() {
        logger.info("Territory Plugin 초기화 중...")

        // Initialize configuration
        configManager = ConfigManager(this)
        configManager.initialize()
        logger.info("설정 파일 로드 완료")

        // Initialize language
        langManager = LangManager(this)
        langManager.initialize()

        // Initialize items
        itemManager = ItemManager(this)
        itemManager.initialize()

        // Initialize database
        databaseManager = DatabaseManager(this)
        databaseManager.initialize()
        logger.info("데이터베이스 초기화 완료")

        // Initialize managers
        territoryManager = TerritoryManager(this)
        warManager = WarManager(this)
        blueMapManager = BlueMapManager(this)
        statsManager = StatsManager(this)
        stoneAbilityManager = StoneAbilityManager(this)
        logger.info("관리자 시스템 초기화 완료")

        // Initialize PlayerGroupCache with config values
        PlayerGroupCache.initialize(this)
        logger.info("플레이어 그룹 캐시 초기화 완료")

        // Initialize StoneAbilityManager
        stoneAbilityManager.initialize()

        // Register listeners
        val warDeclarationListener = WarDeclarationListener(this)
        server.pluginManager.registerEvents(StoneUpgradeListener(this), this)
        server.pluginManager.registerEvents(InteractionListener(this), this)
        server.pluginManager.registerEvents(warDeclarationListener, this)
        server.pluginManager.registerEvents(kr.skarch.territory_Plugin.listeners.RegionNameInputListener(this), this)
        server.pluginManager.registerEvents(CombatListener(this), this)
        logger.info("이벤트 리스너 등록 완료")

        // Register commands
        getCommand("territory")?.setExecutor(TerritoryCommand(this))
        getCommand("territory")?.tabCompleter = TerritoryTabCompleter(this)
        getCommand("war-confirm")?.setExecutor(WarConfirmCommand(this, warDeclarationListener))
        logger.info("명령어 등록 완료")

        // Register PlaceholderAPI expansion
        if (server.pluginManager.getPlugin("PlaceholderAPI") != null) {
            kr.skarch.territory_Plugin.integrations.TerritoryPlaceholderExpansion(this).register()
            logger.info("PlaceholderAPI 확장 등록 완료")
        }

        // Register LuckPerms event listener for cache invalidation
        try {
            val luckPerms = LuckPermsProvider.get()
            luckPermsListener = LuckPermsListener(this, luckPerms)
            luckPermsListener?.register()
            logger.info("LuckPerms 이벤트 리스너 등록 완료")
        } catch (e: Exception) {
            logger.warning("LuckPerms를 찾을 수 없습니다. 그룹 캐시 자동 무효화가 비활성화됩니다.")
        }

        logger.info("Territory Plugin 활성화 완료!")
    }

    override fun onDisable() {
        // Unregister LuckPerms listener
        luckPermsListener?.unregister()

        // Shutdown ability manager
        if (::stoneAbilityManager.isInitialized) {
            stoneAbilityManager.shutdown()
        }

        databaseManager.close()
        logger.info("Territory Plugin 비활성화 완료!")
    }
}
