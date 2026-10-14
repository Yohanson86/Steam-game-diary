package com.gamediary.steam.data.remote

import com.gamediary.steam.data.model.DiaryEntryEntity
import com.gamediary.steam.data.model.SteamGameEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime

data class RestoredBackupData(
    val games: List<SteamGameEntity>,
    val diaryEntries: List<DiaryEntryEntity>,
    val exportedAt: String
)

/**
 * Управляет сериализацией базы (1800+ игр Steam + мультиплатформенный дневник)
 * и облачной синхронизацией с Яндекс.Диском (REST API) и Google Диском (через SAF / JSON).
 */
object CloudBackupManager {

    private const val YANDEX_BACKUP_FOLDER = "disk:/GameDiary"
    private const val YANDEX_BACKUP_PATH = "disk:/GameDiary/game_diary_backup.json"

    fun serializeToJson(
        games: List<SteamGameEntity>,
        diaryEntries: List<DiaryEntryEntity>
    ): String {
        val root = JSONObject()
        root.put("version", 2)
        root.put("exportedAt", LocalDateTime.now().toString())

        val gamesArr = JSONArray()
        for (g in games) {
            val obj = JSONObject()
            obj.put("appId", g.appId)
            obj.put("title", g.title)
            obj.put("playtimeMinutes", g.playtimeMinutes)
            obj.put("releaseYear", g.releaseYear)
            obj.put("genres", g.genres)
            obj.put("developer", g.developer)
            obj.put("iconHash", g.iconHash)
            obj.put("customCoverUrl", g.customCoverUrl ?: "")
            obj.put("status", g.status)
            obj.put("userRating", g.userRating)
            obj.put("notes", g.notes)
            gamesArr.put(obj)
        }
        root.put("steamGames", gamesArr)

        val diaryArr = JSONArray()
        for (d in diaryEntries) {
            val obj = JSONObject()
            obj.put("id", d.id)
            obj.put("gameTitle", d.gameTitle)
            if (d.linkedSteamAppId != null) obj.put("linkedSteamAppId", d.linkedSteamAppId)
            obj.put("coverUrl", d.coverUrl)
            obj.put("releaseYear", d.releaseYear)
            obj.put("platform", d.platform)
            obj.put("storeService", d.storeService)
            obj.put("finishedDate", d.finishedDate)
            obj.put("rating", d.rating)
            obj.put("completionStatus", d.completionStatus)
            obj.put("comment", d.comment)
            diaryArr.put(obj)
        }
        root.put("diaryEntries", diaryArr)

        return root.toString(2)
    }

    fun deserializeFromJson(jsonString: String): RestoredBackupData {
        val root = JSONObject(jsonString)
        val exportedAt = root.optString("exportedAt", "неизвестно")

        val gamesList = mutableListOf<SteamGameEntity>()
        val gamesArr = root.optJSONArray("steamGames") ?: JSONArray()
        for (i in 0 until gamesArr.length()) {
            val obj = gamesArr.getJSONObject(i)
            gamesList.add(
                SteamGameEntity(
                    appId = obj.getLong("appId"),
                    title = obj.optString("title", ""),
                    playtimeMinutes = obj.optInt("playtimeMinutes", 0),
                    releaseYear = obj.optString("releaseYear", "—"),
                    genres = obj.optString("genres", "Steam"),
                    developer = obj.optString("developer", ""),
                    iconHash = obj.optString("iconHash", ""),
                    customCoverUrl = obj.optString("customCoverUrl", "").takeIf { it.isNotBlank() },
                    status = obj.optString("status", "BACKLOG"),
                    userRating = obj.optInt("userRating", 0),
                    notes = obj.optString("notes", "")
                )
            )
        }

        val diaryList = mutableListOf<DiaryEntryEntity>()
        val diaryArr = root.optJSONArray("diaryEntries") ?: JSONArray()
        for (i in 0 until diaryArr.length()) {
            val obj = diaryArr.getJSONObject(i)
            val linkedId = if (obj.has("linkedSteamAppId")) obj.getLong("linkedSteamAppId") else null
            diaryList.add(
                DiaryEntryEntity(
                    id = obj.optLong("id", 0L),
                    gameTitle = obj.optString("gameTitle", ""),
                    linkedSteamAppId = linkedId,
                    coverUrl = obj.optString("coverUrl", ""),
                    releaseYear = obj.optString("releaseYear", "—"),
                    platform = obj.optString("platform", "PC"),
                    storeService = obj.optString("storeService", "Steam"),
                    finishedDate = obj.optString("finishedDate", ""),
                    rating = obj.optInt("rating", 9),
                    completionStatus = obj.optString("completionStatus", "Пройден сюжет"),
                    comment = obj.optString("comment", "")
                )
            )
        }

        return RestoredBackupData(gamesList, diaryList, exportedAt)
    }

    /**
     * Выгружает JSON-бэкап напрямую на Яндекс.Диск (в папку disk:/GameDiary/game_diary_backup.json)
     */
    suspend fun uploadToYandexDisk(
        oauthToken: String,
        jsonPayload: String
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val token = oauthToken.trim()
            require(token.isNotBlank()) { "Укажите OAuth-токен Яндекс.Диска" }

            // 1. Создаем папку disk:/GameDiary (если ещё не создана, код 409 игнорируем)
            val encodedFolder = URLEncoder.encode(YANDEX_BACKUP_FOLDER, "UTF-8")
            val mkdirConn = (URL("https://cloud-api.yandex.net/v1/disk/resources?path=$encodedFolder")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                setRequestProperty("Authorization", "OAuth $token")
                connectTimeout = 10000
                readTimeout = 10000
            }
            mkdirConn.responseCode // Вызов для выполнения запроса

            // 2. Запрашиваем URL для загрузки файла с перезаписью
            val encodedPath = URLEncoder.encode(YANDEX_BACKUP_PATH, "UTF-8")
            val getUploadUrlConn = (URL(
                "https://cloud-api.yandex.net/v1/disk/resources/upload?path=$encodedPath&overwrite=true"
            ).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (getUploadUrlConn.responseCode !in 200..299) {
                error("Ошибка Яндекс.Диска (${getUploadUrlConn.responseCode}): проверьте токен")
            }

            val uploadResp = JSONObject(getUploadUrlConn.inputStream.bufferedReader().use { it.readText() })
            val href = uploadResp.getString("href")

            // 3. Отправляем сам JSON-файл по полученному href
            val putConn = (URL(href).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                connectTimeout = 15000
                readTimeout = 20000
            }
            OutputStreamWriter(putConn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonPayload)
            }

            if (putConn.responseCode !in 200..299) {
                error("Не удалось загрузить файл на Яндекс.Диск (код ${putConn.responseCode})")
            }

            YANDEX_BACKUP_PATH
        }
    }

    /**
     * Скачивает и восстанавливает JSON-бэкап с Яндекс.Диска (disk:/GameDiary/game_diary_backup.json)
     */
    suspend fun downloadFromYandexDisk(oauthToken: String): Result<RestoredBackupData> = withContext(Dispatchers.IO) {
        runCatching {
            val token = oauthToken.trim()
            require(token.isNotBlank()) { "Укажите OAuth-токен Яндекс.Диска" }

            val encodedPath = URLEncoder.encode(YANDEX_BACKUP_PATH, "UTF-8")
            val getDownloadUrlConn = (URL(
                "https://cloud-api.yandex.net/v1/disk/resources/download?path=$encodedPath"
            ).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "OAuth $token")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (getDownloadUrlConn.responseCode !in 200..299) {
                error("Бэкап не найден на Яндекс.Диске (код ${getDownloadUrlConn.responseCode})")
            }

            val downloadResp = JSONObject(getDownloadUrlConn.inputStream.bufferedReader().use { it.readText() })
            val href = downloadResp.getString("href")

            val fileConn = (URL(href).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 20000
            }
            val jsonText = fileConn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            deserializeFromJson(jsonText)
        }
    }
}
