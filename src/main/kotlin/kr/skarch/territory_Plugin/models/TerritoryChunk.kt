package kr.skarch.territory_Plugin.models

import java.util.UUID

data class TerritoryChunk(
    val chunkKey: String,
    val ownerGroup: String,
    val parentStoneUuid: UUID
)

