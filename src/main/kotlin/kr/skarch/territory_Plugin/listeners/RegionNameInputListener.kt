package kr.skarch.territory_Plugin.listeners

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent

class RegionNameInputListener(private val plugin: Territory_Plugin) : Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    fun onChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val pending = plugin.pendingRegionNames[player.uniqueId] ?: return

        // 이벤트 취소 (일반 채팅으로 전송되지 않도록)
        event.isCancelled = true

        val regionName = event.message.trim()

        // 지역 이름 유효성 검사
        if (regionName.isEmpty()) {
            player.sendMessage("§c지역 이름은 비워둘 수 없습니다!")
            player.sendMessage("§e다시 입력하거나 '/territory cancel'로 취소하세요.")
            return
        }

        if (regionName.length > 32) {
            player.sendMessage("§c지역 이름은 32자를 초과할 수 없습니다!")
            player.sendMessage("§e다시 입력하거나 '/territory cancel'로 취소하세요.")
            return
        }

        // 특수문자 제한
        if (!regionName.matches(Regex("[가-힣a-zA-Z0-9_\\- ]+"))) {
            player.sendMessage("§c지역 이름에는 한글, 영문, 숫자, 언더스코어, 하이픈, 공백만 사용할 수 있습니다!")
            player.sendMessage("§e다시 입력하거나 '/territory cancel'로 취소하세요.")
            return
        }

        // 중복 체크
        if (plugin.databaseManager.isRegionNameExists(regionName)) {
            player.sendMessage("§c이미 사용 중인 지역 이름입니다!")
            player.sendMessage("§e다른 이름을 입력하거나 '/territory cancel'로 취소하세요.")
            return
        }

        // 동기 태스크로 점령석 설치
        plugin.server.scheduler.runTask(plugin, Runnable {
            val stone = plugin.territoryManager.placeStone(pending.location, pending.ownerGroup, regionName)

            if (stone != null) {
                val coloredNation = plugin.configManager.getColoredNationName(pending.ownerGroup)
                player.sendMessage("§a점령석을 설치했습니다!")
                player.sendMessage("§e국가: $coloredNation")
                player.sendMessage("§e지역: §f$regionName")
                player.sendMessage("§e티어: §f${stone.currentTier.tierName}")

                // 아이템 제거
                val item = pending.item
                if (item.amount > 1) {
                    item.amount--
                } else {
                    player.inventory.setItemInMainHand(null)
                }
            } else {
                player.sendMessage("§c이 위치에는 점령석을 설치할 수 없습니다!")
                // 아이템은 제거하지 않음
            }

            // 대기 목록에서 제거
            plugin.pendingRegionNames.remove(player.uniqueId)
        })
    }
}

