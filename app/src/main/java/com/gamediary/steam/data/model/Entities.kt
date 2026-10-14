package com.gamediary.steam.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PlayStatus(val label: String) {
    BACKLOG("Не играл / Бэклог"),
    PLAYING("Играю сейчас"),
    BEATEN("Пройден сюжет"),
    COMPLETED("100% / Платина"),
    DROPPED("Дропнуто"),
    ENDLESS("Сессионка")
}

enum class GamingPlatform(val label: String) {
    PC("PC"),
    STEAM_DECK("Steam Deck"),
    PS5("PlayStation 5"),
    XBOX("Xbox Series / One"),
    SWITCH("Nintendo Switch"),
    N3DS("Nintendo 3DS")
}

enum class StoreService(val label: String) {
    STEAM("Steam"),
    EGS("Epic Games Store"),
    GOG("GOG"),
    GAME_PASS("Xbox Game Pass"),
    PSN("PS Store / PS Plus"),
    ESHOP("Nintendo eShop"),
    PHYSICAL("Картридж / Диск")
}

@Entity(tableName = "steam_games")
data class SteamGameEntity(
    @PrimaryKey val appId: Long,
    val title: String,
    val playtimeMinutes: Int = 0,
    val releaseYear: String = "—",
    val genres: String = "Steam",
    val developer: String = "",
    val iconHash: String = "",
    val customCoverUrl: String? = null,
    val status: String = PlayStatus.BACKLOG.name,
    val userRating: Int = 0,
    val notes: String = ""
) {
    val playtimeHours: Double
        get() = (playtimeMinutes / 6.0).toInt() / 10.0

    val headerCoverUrl: String
        get() = customCoverUrl ?: "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/header.jpg"

    val verticalCoverUrl: String
        get() = customCoverUrl ?: "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$appId/library_600x900.jpg"
}

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameTitle: String,
    val linkedSteamAppId: Long? = null,
    val coverUrl: String = "",
    val releaseYear: String = "—",
    val platform: String = GamingPlatform.PC.label,
    val storeService: String = StoreService.STEAM.label,
    val finishedDate: String,
    val rating: Int = 9,
    val completionStatus: String = PlayStatus.BEATEN.label,
    val comment: String = ""
)
