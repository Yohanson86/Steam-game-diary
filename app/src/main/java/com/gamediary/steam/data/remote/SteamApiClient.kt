package com.gamediary.steam.data.remote

import com.gamediary.steam.data.model.PlayStatus
import com.gamediary.steam.data.model.SteamGameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SteamStoreMetadata(
    val releaseYear: String,
    val genres: String,
    val developer: String,
    val headerImage: String?
)

data class ExternalGameMatch(
    val title: String,
    val releaseYear: String,
    val coverUrl: String,
    val source: String
)

object SteamApiClient {

    suspend fun fetchOwnedGames(
        steamId64: String,
        apiKey: String,
        existingGamesMap: Map<Long, SteamGameEntity>
    ): Result<List<SteamGameEntity>> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = "https://api.steampowered.com/IPlayerService/GetOwnedGames/v0001/" +
                "?key=${apiKey.trim()}&steamid=${steamId64.trim()}" +
                "&include_appinfo=1&include_played_free_games=1&format=json"

            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 25000
            }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)
            val responseObj = root.optJSONObject("response") ?: JSONObject()
            val gamesArray = responseObj.optJSONArray("games") ?: return@runCatching emptyList()

            val resultList = ArrayList<SteamGameEntity>(gamesArray.length())
            for (i in 0 until gamesArray.length()) {
                val item = gamesArray.getJSONObject(i)
                val appId = item.getLong("appid")
                val title = item.optString("name", "App #$appId")
                val playtime = item.optInt("playtime_forever", 0)
                val iconHash = item.optString("img_icon_hash", "")

                val existing = existingGamesMap[appId]
                val defaultStatus = if (playtime > 0) PlayStatus.PLAYING.name else PlayStatus.BACKLOG.name

                resultList.add(
                    SteamGameEntity(
                        appId = appId,
                        title = title,
                        playtimeMinutes = playtime,
                        releaseYear = existing?.releaseYear ?: "—",
                        genres = existing?.genres ?: "Steam",
                        developer = existing?.developer ?: "",
                        iconHash = iconHash,
                        customCoverUrl = existing?.customCoverUrl,
                        status = existing?.status ?: defaultStatus,
                        userRating = existing?.userRating ?: 0,
                        notes = existing?.notes ?: ""
                    )
                )
            }
            resultList
        }
    }

    suspend fun fetchGameStoreMetadata(appId: Long): Result<SteamStoreMetadata> = withContext(Dispatchers.IO) {
        runCatching {
            val urlStr = "https://store.steampowered.com/api/appdetails?appids=$appId&l=russian"
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            val appObj = root.optJSONObject(appId.toString())
            val data = appObj?.optJSONObject("data") ?: error("Нет данных")

            val releaseDate = data.optJSONObject("release_date")?.optString("date", "—") ?: "—"
            val yearMatch = Regex("(19|20)\\d{2}").find(releaseDate)?.value ?: releaseDate

            val genresArr = data.optJSONArray("genres")
            val genresList = mutableListOf<String>()
            if (genresArr != null) {
                for (i in 0 until minOf(3, genresArr.length())) {
                    genresList.add(genresArr.getJSONObject(i).optString("description"))
                }
            }

            val devsArr = data.optJSONArray("developers")
            val developer = if (devsArr != null && devsArr.length() > 0) devsArr.optString(0) else ""
            val headerImage = data.optString("header_image").takeIf { it.isNotBlank() }

            SteamStoreMetadata(
                releaseYear = yearMatch,
                genres = if (genresList.isEmpty()) "Steam" else genresList.joinToString(", "),
                developer = developer,
                headerImage = headerImage
            )
        }
    }

    suspend fun searchExternalGameCoverAndYear(
        query: String,
        rawgApiKey: String
    ): Result<ExternalGameMatch> = withContext(Dispatchers.IO) {
        runCatching {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")

            if (rawgApiKey.isNotBlank()) {
                val rawgUrl = "https://api.rawg.io/api/games?key=${rawgApiKey.trim()}&search=$encoded&page_size=1"
                val conn = (URL(rawgUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                }
                if (conn.responseCode == 200) {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val item = results.getJSONObject(0)
                        val name = item.optString("name", query)
                        val released = item.optString("released", "")
                        val year = if (released.length >= 4) released.substring(0, 4) else "—"
                        val bgImage = item.optString("background_image", "")
                        return@runCatching ExternalGameMatch(
                            title = name,
                            releaseYear = year,
                            coverUrl = bgImage,
                            source = "RAWG Database"
                        )
                    }
                }
            }

            val steamSearchUrl = "https://store.steampowered.com/api/storesearch/?term=$encoded&l=russian&cc=ru"
            val sConn = (URL(steamSearchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            val sJson = JSONObject(sConn.inputStream.bufferedReader().use { it.readText() })
            val items = sJson.optJSONArray("items")
            if (items != null && items.length() > 0) {
                val first = items.getJSONObject(0)
                val foundId = first.getLong("id")
                val foundName = first.optString("name", query)
                val cover = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/$foundId/header.jpg"
                return@runCatching ExternalGameMatch(
                    title = foundName,
                    releaseYear = "—",
                    coverUrl = cover,
                    source = "Steam Store Search"
                )
            }

            error("Игра не найдена во внешних источниках")
        }
    }
}
