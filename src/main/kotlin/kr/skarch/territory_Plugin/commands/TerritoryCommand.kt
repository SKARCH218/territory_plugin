package kr.skarch.territory_Plugin.commands

import kr.skarch.territory_Plugin.Territory_Plugin
import net.luckperms.api.LuckPermsProvider
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

class TerritoryCommand(private val plugin: Territory_Plugin) : CommandExecutor {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("§c이 명령어는 플레이어만 사용할 수 있습니다!")
            return true
        }

        if (args.isEmpty()) {
            sendHelp(sender)
            return true
        }

        when (args[0].lowercase()) {
            "stone" -> giveOccupationStone(sender)
            "scroll" -> giveWarScroll(sender)
            "info" -> showTerritoryInfo(sender)
            "upgrade" -> openUpgradeGUI(sender)
            "endwar" -> endWar(sender, args)
            "startwar" -> startWar(sender, args)
            "reload" -> reloadConfig(sender)
            "team", "teams" -> showTeams(sender)
            "stats" -> showStats(sender, args)
            "ranking", "rank" -> showRanking(sender)
            "find" -> findNearestEnemy(sender)
            "stones" -> listStones(sender, args)
            "history" -> showWarHistory(sender, args)
            "score" -> showWarScore(sender, args)
            "scorenow" -> showCurrentWarScore(sender)
            "cancel" -> cancelRegionInput(sender)
            else -> sendHelp(sender)
        }

        return true
    }

    private fun sendHelp(player: Player) {
        player.sendMessage("§6=== Territory Plugin Commands ===")
        player.sendMessage("§e/territory stone §7- Tier I 점령석을 받습니다 (관리자)")
        player.sendMessage("§e/territory scroll §7- 전쟁 선포 두루마리를 받습니다 (관리자)")
        player.sendMessage("§e/territory info §7- 현재 위치의 영토 정보를 확인합니다")
        player.sendMessage("§e/territory upgrade §7- 현재 청크의 점령석 업그레이드 GUI를 엽니다")
        player.sendMessage("§e/territory team §7- 등록된 팀 목록을 확인합니다")
        player.sendMessage("§e/territory stats [팀] §7- 국가 통계를 확인합니다")
        player.sendMessage("§e/territory ranking §7- 국가 랭킹을 확인합니다")
        player.sendMessage("§e/territory find §7- 가장 가까운 적 점령석을 찾습니다")
        player.sendMessage("§e/territory stones [팀] §7- 점령석 목록을 확인합니다")
        player.sendMessage("§e/territory history [팀] §7- 전쟁 이력을 확인합니다")
        player.sendMessage("§e/territory score <차수> §7- 전쟁 점수를 확인합니다")
        player.sendMessage("§e/territory scoreNow §7- 현재 전쟁의 실시간 점수를 확인합니다")
        player.sendMessage("§e/territory cancel §7- 지역 이름 입력을 취소합니다")
        player.sendMessage("§e/territory reload §7- 설정 파일을 리로드합니다 (관리자)")
        player.sendMessage("§e/territory startwar [nation] §7- 전쟁을 즉시 시작합니다 (관리자)")
        player.sendMessage("§e/territory endwar [nation] §7- 전쟁을 종료합니다 (관리자)")
    }

    private fun giveOccupationStone(player: Player) {
        if (!player.hasPermission("territory.admin")) {
            player.sendMessage(plugin.langManager.getNoPermission())
            return
        }

        val stone = ItemStack(Material.PAPER)
        val meta = stone.itemMeta
        meta?.setDisplayName(plugin.langManager.getItemName("occupation_stone"))
        meta?.lore = plugin.langManager.getItemLore("occupation_stone")

        // 커스텀 모델 데이터 적용
        val customModelData = plugin.itemManager.getOccupationStoneTier1CustomModelData()
        if (customModelData > 0) {
            meta?.setCustomModelData(customModelData)
        }

        stone.itemMeta = meta

        player.inventory.addItem(stone)
        player.sendMessage(plugin.langManager.getMessage("commands.stone_given"))
    }

    private fun giveWarScroll(player: Player) {
        if (!player.hasPermission("territory.admin")) {
            player.sendMessage(plugin.langManager.getNoPermission())
            return
        }

        val scroll = ItemStack(Material.PAPER)
        val meta = scroll.itemMeta
        meta?.setDisplayName(plugin.langManager.getItemName("war_scroll"))
        meta?.lore = plugin.langManager.getItemLore("war_scroll")

        // 커스텀 모델 데이터 적용
        val customModelData = plugin.itemManager.getWarScrollCustomModelData()
        if (customModelData > 0) {
            meta?.setCustomModelData(customModelData)
        }

        scroll.itemMeta = meta

        player.inventory.addItem(scroll)
        player.sendMessage(plugin.langManager.getMessage("commands.scroll_given"))
    }

    private fun showTerritoryInfo(player: Player) {
        val location = player.location
        val chunkKey = "${location.world.name};${location.chunk.x};${location.chunk.z}"
        val owner = plugin.databaseManager.getChunkOwner(chunkKey)

        player.sendMessage(plugin.langManager.getMessage("commands.info_header"))
        player.sendMessage(plugin.langManager.getMessage("commands.info_chunk",
            "x" to location.chunk.x, "z" to location.chunk.z))

        if (owner != null) {
            player.sendMessage(plugin.langManager.getMessage("commands.info_owner", "nation" to owner))

            // Try to find stone in this chunk
            val chunk = location.chunk
            val world = location.world
            val centerX = chunk.x * 16 + 7
            val centerZ = chunk.z * 16 + 7
            val configY = plugin.configManager.getStoneSpawnY()
            val stoneLocation = org.bukkit.Location(world, centerX.toDouble(), configY.toDouble(), centerZ.toDouble())

            val stone = plugin.databaseManager.getStoneByLocation(stoneLocation)
            if (stone != null) {
                player.sendMessage(plugin.langManager.getMessage("commands.info_stone_location",
                    "x" to stone.location.blockX, "y" to stone.location.blockY, "z" to stone.location.blockZ))
                player.sendMessage(plugin.langManager.getMessage("commands.info_stone_tier",
                    "tier" to stone.currentTier.tierName))
            }

            val isAtWar = plugin.warManager.isInGlobalWar(owner)
            val warStatus = if (isAtWar) plugin.langManager.getStatusWar() else plugin.langManager.getStatusPeace()
            player.sendMessage(plugin.langManager.getMessage("commands.info_war_status", "status" to warStatus))
        } else {
            player.sendMessage(plugin.langManager.getMessage("commands.info_no_owner"))
        }

        val playerGroup = getPlayerGroup(player)
        player.sendMessage(plugin.langManager.getMessage("commands.info_your_nation", "nation" to playerGroup))
    }


    private fun endWar(player: Player, args: Array<out String>) {
        if (!player.hasPermission("territory.admin")) {
            player.sendMessage("§c권한이 없습니다!")
            return
        }

        if (args.size < 2) {
            player.sendMessage("§c사용법: /territory endwar <nation>")
            return
        }

        val nationName = args[1]
        plugin.warManager.endGlobalWar(nationName)
        player.sendMessage("§a국가 ${nationName}의 전쟁을 종료했습니다.")
    }

    private fun startWar(player: Player, args: Array<out String>) {
        if (!player.hasPermission("territory.admin")) {
            player.sendMessage("§c권한이 없습니다!")
            return
        }

        if (args.size < 2) {
            player.sendMessage("§c사용법: /territory startwar <nation>")
            return
        }

        val nationName = args[1]

        // Check if already in war
        if (plugin.warManager.isInGlobalWar(nationName)) {
            player.sendMessage("§c${nationName}은(는) 이미 전쟁 중입니다!")
            return
        }

        // Get war number before starting
        val warNumber = plugin.databaseManager.getCurrentWarNumber() + 1

        // Start war immediately (skip countdown)
        plugin.warManager.startWarImmediately(nationName)
        player.sendMessage("§a${nationName}의 제 ${warNumber}차 전쟁을 즉시 시작했습니다!")
    }

    private fun showWarScore(player: Player, args: Array<out String>) {
        if (args.size < 2) {
            player.sendMessage("§c사용법: /territory score <전쟁차수>")
            player.sendMessage("§e예시: /territory score 1")
            return
        }

        val warNumber = args[1].toIntOrNull()
        if (warNumber == null || warNumber < 1) {
            player.sendMessage("§c올바른 전쟁 차수를 입력하세요!")
            return
        }

        val scores = plugin.databaseManager.getWarScore(warNumber)
        if (scores.isEmpty()) {
            player.sendMessage("§c${warNumber}차 전쟁 기록을 찾을 수 없습니다!")
            return
        }

        player.sendMessage("§6=== ${warNumber}차 전쟁 점수 ===")
        val sortedScores = scores.entries.sortedByDescending { it.value }
        sortedScores.forEachIndexed { index, entry ->
            val medal = when(index) {
                0 -> "§6🥇"
                1 -> "§7🥈"
                2 -> "§c🥉"
                else -> "§e${index + 1}."
            }
            val coloredNation = plugin.configManager.getColoredNationName(entry.key)
            player.sendMessage("$medal $coloredNation §7- §e${entry.value}점")
        }
    }

    private fun showCurrentWarScore(player: Player) {
        val activeWars = plugin.warManager.getActiveWars()

        if (activeWars.isEmpty()) {
            player.sendMessage("§c현재 진행 중인 전쟁이 없습니다!")
            return
        }

        player.sendMessage("§6=== 현재 전쟁 실시간 점수 ===")

        val currentWarNumber = plugin.databaseManager.getCurrentWarNumber()
        player.sendMessage("§e전쟁 차수: §f${currentWarNumber}차")
        player.sendMessage("")

        val scores = plugin.warManager.calculateCurrentWarScore()
        val sortedScores = scores.entries.sortedByDescending { it.value }

        sortedScores.forEachIndexed { index, entry ->
            val medal = when(index) {
                0 -> "§6🥇"
                1 -> "§7🥈"
                2 -> "§c🥉"
                else -> "§e${index + 1}."
            }

            val conquests = plugin.warManager.getConquestCount(entry.key)
            val kills = plugin.warManager.getKillCount(entry.key)
            val coloredNation = plugin.configManager.getColoredNationName(entry.key)

            player.sendMessage("$medal $coloredNation")
            player.sendMessage("  §7점수: §e${entry.value}점 §7(점령: ${conquests}, 킬: ${kills})")
        }
    }

    private fun reloadConfig(sender: CommandSender) {
        if (!sender.hasPermission("territory.admin")) {
            sender.sendMessage(plugin.langManager.getNoPermission())
            return
        }

        plugin.configManager.reload()
        plugin.langManager.reload()
        plugin.itemManager.reload()

        sender.sendMessage(plugin.langManager.getMessage("reload_success"))
        sender.sendMessage(plugin.langManager.getMessage("reload_files", "file" to "config.yml"))
        sender.sendMessage(plugin.langManager.getMessage("reload_files", "file" to "team.yml"))
        sender.sendMessage(plugin.langManager.getMessage("reload_files", "file" to "lang.yml"))
        sender.sendMessage(plugin.langManager.getMessage("reload_files", "file" to "items.yml"))

        // BlueMap 마커 업데이트
        if (plugin.blueMapManager.isBlueMapEnabled()) {
            sender.sendMessage("§eBlueMap 마커를 업데이트하는 중...")
            plugin.blueMapManager.updateMarkers()
        }
    }

    private fun showTeams(player: Player) {
        val teams = plugin.configManager.getAllTeams()

        if (teams.isEmpty()) {
            player.sendMessage("§c등록된 팀이 없습니다!")
            return
        }

        player.sendMessage("§6=== 등록된 팀 목록 ===")
        teams.forEach { team ->
            val coloredName = plugin.configManager.getColoredTeamDisplayName(team.id)
            player.sendMessage("$coloredName §7(${team.id})")
            player.sendMessage("  §7LuckPerms 그룹: §f${team.luckPermsGroup}")
            player.sendMessage("  §7색상: §f${team.color}")
            if (team.description.isNotEmpty()) {
                player.sendMessage("  §7설명: §f${team.description}")
            }

            // Check if team is in war
            if (plugin.warManager.isInGlobalWar(team.luckPermsGroup)) {
                player.sendMessage("  §c⚔ 전쟁 중!")
            }
        }
        player.sendMessage("§6총 ${teams.size}개의 팀")
    }

    private fun showStats(player: Player, args: Array<out String>) {
        val targetTeam = if (args.size > 1) {
            args[1]
        } else {
            getPlayerGroup(player)
        }

        val stats = plugin.statsManager.getNationStats(targetTeam)
        if (stats == null) {
            player.sendMessage("§c국가 정보를 찾을 수 없습니다!")
            return
        }

        val coloredNation = plugin.configManager.getColoredNationName(targetTeam)
        player.sendMessage("§6=== $coloredNation §6통계 ===")
        player.sendMessage("§e영토: §f${stats.totalChunks} 청크")
        player.sendMessage("§e점령석: §f${stats.totalStones}개")
        player.sendMessage("§e최고 티어: §f${stats.highestTier.tierName}")
        player.sendMessage("§e온라인 멤버: §f${stats.memberCount}명")
        player.sendMessage("§e전쟁 상태: ${if (stats.isAtWar) "§c전쟁 중" else "§a평화"}")
        player.sendMessage("§e영토 점수: §f${stats.getTerritoryScore()}")
        player.sendMessage("§e순위: §f#${plugin.statsManager.getNationRanking(targetTeam)}")
    }

    private fun showRanking(player: Player) {
        val allStats = plugin.statsManager.getAllNationStats()

        player.sendMessage("§6=== 국가 랭킹 (영토 점수) ===")
        allStats.take(10).forEachIndexed { index, stats ->
            val rank = index + 1
            val medal = when (rank) {
                1 -> "§6🥇"
                2 -> "§7🥈"
                3 -> "§c🥉"
                else -> "§e${rank}."
            }
            val coloredNation = plugin.configManager.getColoredNationName(stats.nationName)
            player.sendMessage("$medal $coloredNation §7- §e${stats.getTerritoryScore()} §7(청크: ${stats.totalChunks}, 점령석: ${stats.totalStones})")
        }
    }

    private fun findNearestEnemy(player: Player) {
        val playerTeam = getPlayerGroup(player)
        val nearestStone = plugin.statsManager.findNearestEnemyStone(player.location, playerTeam)

        if (nearestStone == null) {
            player.sendMessage("§c근처에 적 점령석이 없습니다!")
            return
        }

        val distance = player.location.distance(nearestStone)
        player.sendMessage("§e가장 가까운 적 점령석:")
        player.sendMessage("§7위치: §f${nearestStone.blockX}, ${nearestStone.blockY}, ${nearestStone.blockZ}")
        player.sendMessage("§7거리: §f${distance.toInt()}m")

        // Set compass target
        player.compassTarget = nearestStone
        player.sendMessage("§a나침반이 점령석을 가리킵니다!")
    }

    private fun listStones(player: Player, args: Array<out String>) {
        val targetTeam = if (args.size > 1) {
            args[1]
        } else {
            getPlayerGroup(player)
        }

        val locations = plugin.statsManager.getStoneLocations(targetTeam)

        if (locations.isEmpty()) {
            player.sendMessage("§c점령석이 없습니다!")
            return
        }

        player.sendMessage("§6=== ${targetTeam} 점령석 목록 ===")
        locations.forEachIndexed { index, location ->
            player.sendMessage("§e${index + 1}. §f$location")
        }
    }

    private fun showWarHistory(player: Player, args: Array<out String>) {
        val targetTeam = if (args.size > 1) {
            args[1]
        } else {
            getPlayerGroup(player)
        }

        val history = plugin.databaseManager.getWarHistory(targetTeam, 5)

        if (history.isEmpty()) {
            player.sendMessage("§c전쟁 이력이 없습니다!")
            return
        }

        player.sendMessage("§6=== ${targetTeam} 전쟁 이력 (최근 5개) ===")
        history.forEach { war ->
            val startTime = war["start_time"] as Long
            val endTime = war["end_time"] as Long
            val duration = if (endTime > 0) {
                val mins = (endTime - startTime) / 60000
                "${mins}분"
            } else {
                "진행 중"
            }
            val stonesDestroyed = war["stones_destroyed"] as Int
            val chunksConquered = war["chunks_conquered"] as Int

            val date = java.text.SimpleDateFormat("MM/dd HH:mm").format(java.util.Date(startTime))
            player.sendMessage("§e$date §7- §f지속: $duration §7| 파괴: §c$stonesDestroyed §7| 점령: §a$chunksConquered")
        }
    }

    private fun openUpgradeGUI(player: Player) {
        // Get player's current chunk
        val chunk = player.location.chunk
        val world = player.world

        // Calculate the chunk center location where stone should be
        val centerX = chunk.x * 16 + 7
        val centerZ = chunk.z * 16 + 7
        val configY = plugin.configManager.getStoneSpawnY()

        val stoneLocation = org.bukkit.Location(world, centerX.toDouble(), configY.toDouble(), centerZ.toDouble())

        // Try to find stone at this chunk center
        val stone = plugin.databaseManager.getStoneByLocation(stoneLocation)

        if (stone == null) {
            player.sendMessage("§c이 청크에는 점령석이 없습니다!")
            return
        }

        // Check if player owns this stone
        val playerGroup = getPlayerGroup(player)
        if (playerGroup != stone.ownerGroup) {
            player.sendMessage("§c이 점령석은 당신의 국가 소유가 아닙니다!")
            return
        }

        // Check permission
        if (!player.hasPermission("territory.upgrade")) {
            player.sendMessage("§c점령석을 업그레이드할 권한이 없습니다!")
            return
        }

        // Open the upgrade GUI using the StoneUpgradeListener's method
        // We need to pass the stone to the listener
        player.sendMessage("§a점령석 업그레이드 GUI를 엽니다...")

        // Call the GUI open method from StoneUpgradeListener
        val listener = plugin.server.pluginManager.getPlugin("territory_Plugin")?.let {
            // Get the listener instance
            org.bukkit.Bukkit.getScheduler().runTask(plugin, Runnable {
                // Open GUI in next tick to avoid conflicts
                openStoneUpgradeGUI(player, stone)
            })
        }
    }

    private fun openStoneUpgradeGUI(player: Player, stone: kr.skarch.territory_Plugin.models.OccupationStone) {
        val gui = org.bukkit.Bukkit.createInventory(null, 27, "§6점령석 업그레이드")

        // Current tier info
        val currentTierItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.BEACON)
        val currentMeta = currentTierItem.itemMeta
        currentMeta?.setDisplayName("§e현재 티어: ${stone.currentTier.tierName}")
        val occupationTime = stone.getOccupationTime()
        val hours = occupationTime / 3600
        val minutes = (occupationTime % 3600) / 60
        currentMeta?.lore = listOf(
            "§7반경: ${stone.currentTier.radius} 청크",
            "§7영역: ${stone.currentTier.radius * 2 + 1}x${stone.currentTier.radius * 2 + 1}",
            "§7점령 시간: ${hours}시간 ${minutes}분"
        )
        currentTierItem.itemMeta = currentMeta
        gui.setItem(11, currentTierItem)

        // Next tier info (if available)
        val nextTier = stone.currentTier.getNext()
        if (nextTier != null) {
            val nextTierItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.DIAMOND)
            val nextMeta = nextTierItem.itemMeta
            nextMeta?.setDisplayName("§a다음 티어: ${nextTier.tierName}")

            val currentTierNum = stone.currentTier.ordinal + 1
            val nextTierNum = nextTier.ordinal + 1
            val requiredMoney = plugin.configManager.getUpgradeMoney(currentTierNum, nextTierNum)
            val requiredTime = plugin.configManager.getUpgradeOccupationTime(currentTierNum, nextTierNum)
            val requiredHours = requiredTime / 3600
            val requiredMinutes = (requiredTime % 3600) / 60

            val lore = mutableListOf(
                "§7반경: ${nextTier.radius} 청크",
                "§7영역: ${nextTier.radius * 2 + 1}x${nextTier.radius * 2 + 1}",
                "",
                "§e요구사항:"
            )

            if (plugin.configManager.isUpgradeCostEnabled()) {
                val vault = plugin.server.servicesManager.getRegistration(net.milkbowl.vault.economy.Economy::class.java)
                val hasEnoughMoney = vault?.provider?.has(player, requiredMoney) ?: false
                val moneyStatus = if (hasEnoughMoney) "§a✔" else "§c✖"
                lore.add("$moneyStatus §7돈: §6$${requiredMoney}")
            }

            val hasEnoughTime = occupationTime >= requiredTime
            val timeStatus = if (hasEnoughTime) "§a✔" else "§c✖"
            lore.add("$timeStatus §7점령 시간: ${requiredHours}시간 ${requiredMinutes}분")
            lore.add("")
            lore.add("§e클릭하여 업그레이드!")

            nextMeta?.lore = lore
            nextTierItem.itemMeta = nextMeta
            gui.setItem(15, nextTierItem)
        } else {
            val maxTierItem = org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER)
            val maxMeta = maxTierItem.itemMeta
            maxMeta?.setDisplayName("§c최대 티어 도달")
            maxMeta?.lore = listOf("§7더 이상 업그레이드할 수 없습니다")
            maxTierItem.itemMeta = maxMeta
            gui.setItem(15, maxTierItem)
        }

        // Store the stone UUID for this player's GUI
        stoneUpgradeMap[player.uniqueId] = stone.stoneUuid

        player.openInventory(gui)
    }

    private fun cancelRegionInput(player: Player) {
        if (plugin.pendingRegionNames.containsKey(player.uniqueId)) {
            plugin.pendingRegionNames.remove(player.uniqueId)
            player.sendMessage("§e지역 이름 입력을 취소했습니다.")
        } else {
            player.sendMessage("§c진행 중인 지역 이름 입력이 없습니다.")
        }
    }

    companion object {
        // Store player UUID -> stone UUID mapping for upgrade GUI
        val stoneUpgradeMap = mutableMapOf<java.util.UUID, java.util.UUID>()
    }

    private fun getPlayerGroup(player: Player): String {
        return try {
            val luckPerms = LuckPermsProvider.get()
            val user = luckPerms.userManager.getUser(player.uniqueId)
            user?.primaryGroup ?: "default"
        } catch (e: Exception) {
            "default"
        }
    }
}

