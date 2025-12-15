package kr.skarch.territory_Plugin.api

import kr.skarch.territory_Plugin.Territory_Plugin
import org.bukkit.Location
import org.bukkit.entity.Player

/**
 * Territory API for external plugins (e.g., Skript)
 * 외부 플러그인 (스크립트 등)을 위한 Territory API
 */
class TerritoryAPI(private val plugin: Territory_Plugin) {

    /**
     * 특정 위치의 영토 이름(지역명) 조회
     *
     * @param location 조회할 위치
     * @return 영토 이름 (지역명), 없으면 null
     *
     * 예시:
     * ```
     * val regionName = api.getRegionNameAt(player.location)
     * if (regionName != null) {
     *     player.sendMessage("현재 위치: $regionName")
     * }
     * ```
     */
    fun getRegionNameAt(location: Location): String? {
        // 청크 중앙 좌표로 점령석 위치 계산
        val chunk = location.chunk
        val world = location.world ?: return null
        val centerX = chunk.x * 16 + 7
        val centerZ = chunk.z * 16 + 7
        val configY = plugin.configManager.getStoneSpawnY()

        val stoneLocation = Location(world, centerX.toDouble(), configY.toDouble(), centerZ.toDouble())

        // 해당 위치의 점령석 조회
        val stone = plugin.databaseManager.getStoneByLocation(stoneLocation)

        return stone?.regionName
    }

    /**
     * 플레이어가 서 있는 위치의 영토 이름 조회
     *
     * @param player 플레이어
     * @return 영토 이름 (지역명), 없으면 null
     *
     * 예시:
     * ```
     * val regionName = api.getRegionNameAt(player)
     * player.sendMessage("현재 지역: ${regionName ?: "무주지"}")
     * ```
     */
    fun getRegionNameAt(player: Player): String? {
        return getRegionNameAt(player.location)
    }

    /**
     * 영토의 소유권 변경 (광역 세뇌 스킬용)
     *
     * @param regionName 변경할 영토 이름 (지역명)
     * @param newOwnerGroup 새 소유자 그룹명 (LuckPerms 그룹)
     * @return 성공 여부
     *
     * 예시:
     * ```
     * val success = api.transferTerritoryOwnership("서울", "korea")
     * if (success) {
     *     player.sendMessage("§a영토를 탈취했습니다!")
     * } else {
     *     player.sendMessage("§c영토를 찾을 수 없습니다!")
     * }
     * ```
     *
     * 참고:
     * - BlueMap 자동 반영됨
     * - 해당 지역의 모든 청크 소유권이 변경됨
     */
    fun transferTerritoryOwnership(regionName: String, newOwnerGroup: String): Boolean {
        // 지역명으로 점령석 찾기
        val stone = plugin.databaseManager.getStoneByRegionName(regionName) ?: return false

        val oldOwner = stone.ownerGroup

        // 소유권 변경 (데이터베이스에서만)
        plugin.databaseManager.updateStoneOwner(stone.stoneUuid, newOwnerGroup)

        // 해당 점령석의 모든 청크 소유권 변경
        val chunks = plugin.databaseManager.getChunksByStone(stone.stoneUuid)
        chunks.forEach { chunkKey ->
            plugin.databaseManager.updateChunkOwner(chunkKey, newOwnerGroup)
        }

        // BlueMap 업데이트
        plugin.blueMapManager.updateMarkers()

        // 알림 메시지
        plugin.server.broadcast(
            net.kyori.adventure.text.Component.text("§c⚠ 광역 세뇌! §e${regionName}§c 지역이 §f${oldOwner}§c에서 §f${newOwnerGroup}§c으로 탈취되었습니다!")
        )

        return true
    }

    /**
     * 특정 위치의 영토 소유자 그룹 조회
     *
     * @param location 조회할 위치
     * @return 소유자 그룹명 (LuckPerms 그룹), 없으면 null
     */
    fun getTerritoryOwnerAt(location: Location): String? {
        val chunkKey = "${location.world.name};${location.chunk.x};${location.chunk.z}"
        return plugin.databaseManager.getChunkOwner(chunkKey)
    }

    /**
     * 플레이어가 서 있는 위치의 영토 소유자 조회
     *
     * @param player 플레이어
     * @return 소유자 그룹명, 없으면 null
     */
    fun getTerritoryOwnerAt(player: Player): String? {
        return getTerritoryOwnerAt(player.location)
    }

    /**
     * 특정 지역명의 점령석 소유자 조회
     *
     * @param regionName 지역명
     * @return 소유자 그룹명, 없으면 null
     */
    fun getRegionOwner(regionName: String): String? {
        val stone = plugin.databaseManager.getStoneByRegionName(regionName)
        return stone?.ownerGroup
    }

    /**
     * 특정 지역의 청크 수 조회
     *
     * @param regionName 지역명
     * @return 청크 수, 지역이 없으면 0
     */
    fun getRegionChunkCount(regionName: String): Int {
        val stone = plugin.databaseManager.getStoneByRegionName(regionName) ?: return 0
        return plugin.databaseManager.getChunksByStone(stone.stoneUuid).size
    }

    /**
     * 모든 지역명 목록 조회
     *
     * @return 지역명 리스트
     */
    fun getAllRegionNames(): List<String> {
        return plugin.databaseManager.getAllStones()
            .mapNotNull { it.regionName }
            .distinct()
    }

    /**
     * 특정 그룹이 소유한 모든 지역명 조회
     *
     * @param ownerGroup 소유자 그룹명
     * @return 지역명 리스트
     */
    fun getRegionsByOwner(ownerGroup: String): List<String> {
        return plugin.databaseManager.getStonesByTeam(ownerGroup)
            .mapNotNull { it.regionName }
    }

    /**
     * 지역 존재 여부 확인
     *
     * @param regionName 지역명
     * @return 존재 여부
     */
    fun doesRegionExist(regionName: String): Boolean {
        return plugin.databaseManager.getStoneByRegionName(regionName) != null
    }

    /**
     * 특정 위치가 특정 지역에 속하는지 확인
     *
     * @param location 위치
     * @param regionName 지역명
     * @return 속하는지 여부
     */
    fun isLocationInRegion(location: Location, regionName: String): Boolean {
        val currentRegion = getRegionNameAt(location)
        return currentRegion == regionName
    }

    /**
     * 특정 영지의 모든 청크 위치 반환
     *
     * @param regionName 영지명 (지역명)
     * @return 청크 위치 목록 ["world;x;z", ...], 영지가 없으면 빈 리스트
     *
     * 예시:
     * ```
     * val chunks = api.getRegionChunkLocations("서울")
     * chunks.forEach { chunkKey ->
     *     // chunkKey = "world;10;20" 형식
     *     val parts = chunkKey.split(";")
     *     val world = parts[0]
     *     val chunkX = parts[1].toInt()
     *     val chunkZ = parts[2].toInt()
     * }
     * ```
     *
     * Skript 예시:
     * ```skript
     * set {_chunks::*} to {_api}.getRegionChunkLocations("서울")
     * loop {_chunks::*}:
     *     # loop-value = "world;10;20"
     *     set {_parts::*} to loop-value split at ";"
     *     set {_world} to {_parts::1}
     *     set {_chunkX} to {_parts::2} parsed as integer
     *     set {_chunkZ} to {_parts::3} parsed as integer
     * ```
     */
    fun getRegionChunkLocations(regionName: String): List<String> {
        val stone = plugin.databaseManager.getStoneByRegionName(regionName) ?: return emptyList()
        return plugin.databaseManager.getChunksByStone(stone.stoneUuid)
    }
}

