package com.gamediary.steam.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gamediary.steam.data.local.AppDatabase
import com.gamediary.steam.data.model.*
import com.gamediary.steam.data.remote.CloudBackupManager
import com.gamediary.steam.data.remote.SteamApiClient
import com.gamediary.steam.ui.theme.AppThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CardDisplaySettings(
    val showCover: Boolean = true,
    val showReleaseYear: Boolean = true,
    val showPlaytime: Boolean = true,
    val showGenres: Boolean = true,
    val useVerticalPoster: Boolean = false
)

class GameDiaryViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).gameDao()
    private val prefs = application.getSharedPreferences("game_diary_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.RETRO_NES.name) ?: AppThemeMode.RETRO_NES.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _cardSettings = MutableStateFlow(
        CardDisplaySettings(
            showCover = prefs.getBoolean("show_cover", true),
            showReleaseYear = prefs.getBoolean("show_year", true),
            showPlaytime = prefs.getBoolean("show_playtime", true),
            showGenres = prefs.getBoolean("show_genres", true),
            useVerticalPoster = prefs.getBoolean("use_vertical_poster", false)
        )
    )
    val cardSettings: StateFlow<CardDisplaySettings> = _cardSettings.asStateFlow()

    private val _autoBackupYandex = MutableStateFlow(prefs.getBoolean("auto_backup_yandex", false))
    val autoBackupYandex: StateFlow<Boolean> = _autoBackupYandex.asStateFlow()

    private val _lastBackupInfo = MutableStateFlow(prefs.getString("last_backup_info", "Ещё не выполнялся") ?: "Ещё не выполнялся")
    val lastBackupInfo: StateFlow<String> = _lastBackupInfo.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL")
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBatchScraping = MutableStateFlow(false)
    val isBatchScraping: StateFlow<Boolean> = _isBatchScraping.asStateFlow()

    private var scrapeJob: Job? = null

    val allSteamGames: StateFlow<List<SteamGameEntity>> = dao.observeAllSteamGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredSteamGames: StateFlow<List<SteamGameEntity>> = combine(
        allSteamGames,
        _searchQuery,
        _statusFilter
    ) { games, query, filter ->
        games.filter { game ->
            val matchesQuery = query.isBlank() ||
                game.title.contains(query, ignoreCase = true) ||
                game.genres.contains(query, ignoreCase = true) ||
                game.releaseYear.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                "UNPLAYED" -> game.playtimeMinutes == 0 && game.status == PlayStatus.BACKLOG.name
                "PLAYING" -> game.status == PlayStatus.PLAYING.name
                "BEATEN" -> game.status == PlayStatus.BEATEN.name || game.status == PlayStatus.COMPLETED.name
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diaryEntries: StateFlow<List<DiaryEntryEntity>> = dao.observeDiaryEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            if (dao.getSteamGamesCount() == 0) {
                // При чистой установке: если есть сохраненный токен Яндекс.Диска, пробуем восстановить бэкап автоматически
                val yandexToken = getSavedYandexToken()
                if (yandexToken.isNotBlank()) {
                    CloudBackupManager.downloadFromYandexDisk(yandexToken).onSuccess { restored ->
                        dao.upsertSteamGames(restored.games)
                        dao.upsertDiaryEntries(restored.diaryEntries)
                        _statusMessage.value = "☁️ Данные автоматически восстановлены с Яндекс.Диска!"
                        return@launch
                    }
                }
                seedStarterData()
            }
        }
    }

    fun setTheme(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setAutoBackupYandex(enabled: Boolean) {
        _autoBackupYandex.value = enabled
        prefs.edit().putBoolean("auto_backup_yandex", enabled).apply()
    }

    fun updateCardSettings(newSettings: CardDisplaySettings) {
        _cardSettings.value = newSettings
        prefs.edit()
            .putBoolean("show_cover", newSettings.showCover)
            .putBoolean("show_year", newSettings.showReleaseYear)
            .putBoolean("show_playtime", newSettings.showPlaytime)
            .putBoolean("show_genres", newSettings.showGenres)
            .putBoolean("use_vertical_poster", newSettings.useVerticalPoster)
            .apply()
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun updateFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun pickRandomBacklogGame() {
        val candidates = allSteamGames.value.filter { it.playtimeMinutes == 0 || it.status == PlayStatus.BACKLOG.name }
        val chosen = candidates.randomOrNull() ?: allSteamGames.value.randomOrNull()
        if (chosen != null) {
            _statusMessage.value = "🎲 Рандомайзер выбрал: ${chosen.title} (${chosen.releaseYear})"
        }
    }

    fun saveApiCredentials(steamId64: String, steamKey: String, rawgKey: String, yandexToken: String) {
        prefs.edit()
            .putString("steam_id", steamId64.trim())
            .putString("steam_key", steamKey.trim())
            .putString("rawg_key", rawgKey.trim())
            .putString("yandex_token", yandexToken.trim())
            .apply()
        _statusMessage.value = "Ключи API и токен облака сохранены"
    }

    /**
     * Выгрузка бэкапа на Яндекс.Диск
     */
    fun backupToYandexDisk(tokenInput: String) {
        val token = tokenInput.trim()
        prefs.edit().putString("yandex_token", token).apply()
        viewModelScope.launch {
            _statusMessage.value = "☁️ Выгрузка бэкапа на Яндекс.Диск..."
            val games = dao.getAllSteamGamesSnapshot()
            val diary = dao.getAllDiaryEntriesSnapshot()
            val json = CloudBackupManager.serializeToJson(games, diary)

            CloudBackupManager.uploadToYandexDisk(token, json).onSuccess { path ->
                val info = "Яндекс.Диск ($path) • ${games.size} игр, ${diary.size} записей"
                _lastBackupInfo.value = info
                prefs.edit().putString("last_backup_info", info).apply()
                _statusMessage.value = "☁️ Бэкап успешно сохранен на Яндекс.Диске!"
            }.onFailure { err ->
                _statusMessage.value = "Ошибка бэкапа: ${err.localizedMessage}"
            }
        }
    }

    /**
     * Восстановление базы с Яндекс.Диска
     */
    fun restoreFromYandexDisk(tokenInput: String) {
        val token = tokenInput.trim()
        prefs.edit().putString("yandex_token", token).apply()
        viewModelScope.launch {
            _statusMessage.value = "📥 Загрузка бэкапа с Яндекс.Диска..."
            CloudBackupManager.downloadFromYandexDisk(token).onSuccess { restored ->
                dao.upsertSteamGames(restored.games)
                dao.upsertDiaryEntries(restored.diaryEntries)
                val info = "Восстановлено (${restored.exportedAt}): ${restored.games.size} игр"
                _lastBackupInfo.value = info
                prefs.edit().putString("last_backup_info", info).apply()
                _statusMessage.value = "✓ Восстановлено ${restored.games.size} игр и ${restored.diaryEntries.size} записей дневника!"
            }.onFailure { err ->
                _statusMessage.value = "Ошибка восстановления: ${err.localizedMessage}"
            }
        }
    }

    /**
     * Сохранение бэкапа в Google Диск / Яндекс.Диск через системный пикер Android (SAF)
     */
    fun exportBackupToUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val games = dao.getAllSteamGamesSnapshot()
                val diary = dao.getAllDiaryEntriesSnapshot()
                val json = CloudBackupManager.serializeToJson(games, diary)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write(json)
                    }
                }
                val info = "Облачный файл (Google/Яндекс Диск) • ${games.size} игр"
                _lastBackupInfo.value = info
                prefs.edit().putString("last_backup_info", info).apply()
                _statusMessage.value = "☁️ Бэкап сохранен в выбранное облако!"
            }.onFailure { err ->
                _statusMessage.value = "Ошибка записи файла: ${err.localizedMessage}"
            }
        }
    }

    /**
     * Восстановление бэкапа из Google Диска / Яндекс.Диска через системный пикер Android (SAF)
     */
    fun importBackupFromUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val jsonText = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("Не удалось прочитать файл")
                }
                val restored = CloudBackupManager.deserializeFromJson(jsonText)
                dao.upsertSteamGames(restored.games)
                dao.upsertDiaryEntries(restored.diaryEntries)
                _statusMessage.value = "✓ Из облачного файла восстановлено ${restored.games.size} игр и ${restored.diaryEntries.size} записей!"
            }.onFailure { err ->
                _statusMessage.value = "Ошибка импорта: ${err.localizedMessage}"
            }
        }
    }

    fun syncSteamLibrary(steamId64: String, apiKey: String) {
        if (steamId64.isBlank() || apiKey.isBlank()) {
            _statusMessage.value = "Укажите ваш SteamID64 и Steam Web API Key в настройках"
            return
        }
        prefs.edit().putString("steam_id", steamId64.trim()).putString("steam_key", apiKey.trim()).apply()
        viewModelScope.launch {
            _statusMessage.value = "Загрузка списка игр и обложек со Steam..."
            val currentMap = allSteamGames.value.associateBy { it.appId }
            val res = SteamApiClient.fetchOwnedGames(steamId64, apiKey, currentMap)
            res.onSuccess { list ->
                dao.upsertSteamGames(list)
                _statusMessage.value = "Синхронизировано игр из Steam: ${list.size}"
                triggerAutoBackupIfEnabled()
            }.onFailure { err ->
                _statusMessage.value = "Ошибка Steam API: ${err.localizedMessage ?: "проверьте ключ и открытость профиля"}"
            }
        }
    }

    fun toggleBatchMetadataScraping() {
        if (_isBatchScraping.value) {
            scrapeJob?.cancel()
            _isBatchScraping.value = false
            _statusMessage.value = "Фоновая догрузка метаданных приостановлена"
            return
        }

        _isBatchScraping.value = true
        scrapeJob = viewModelScope.launch {
            val missing = dao.getGamesMissingMetadata(limit = 150)
            if (missing.isEmpty()) {
                _isBatchScraping.value = false
                _statusMessage.value = "У всех игр в базе уже загружены годы выпуска и жанры!"
                return@launch
            }
            _statusMessage.value = "Запущен безопасный скрэпинг метаданных (${missing.size} игр в очереди)..."
            for (game in missing) {
                if (!isActive) break
                SteamApiClient.fetchGameStoreMetadata(game.appId).onSuccess { meta ->
                    dao.updateSteamGame(
                        game.copy(
                            releaseYear = meta.releaseYear,
                            genres = meta.genres,
                            developer = meta.developer,
                            customCoverUrl = meta.headerImage ?: game.customCoverUrl
                        )
                    )
                }
                delay(1600L)
            }
            _isBatchScraping.value = false
            _statusMessage.value = "Пакетная догрузка метаданных завершена!"
            triggerAutoBackupIfEnabled()
        }
    }

    fun loadMetadataForGame(game: SteamGameEntity) {
        viewModelScope.launch {
            SteamApiClient.fetchGameStoreMetadata(game.appId).onSuccess { meta ->
                dao.updateSteamGame(
                    game.copy(
                        releaseYear = meta.releaseYear,
                        genres = meta.genres,
                        developer = meta.developer,
                        customCoverUrl = meta.headerImage ?: game.customCoverUrl
                    )
                )
                _statusMessage.value = "Обновлены данные: ${game.title} (${meta.releaseYear})"
            }
        }
    }

    fun changeGameStatus(game: SteamGameEntity, newStatus: PlayStatus) {
        viewModelScope.launch {
            dao.updateSteamGame(game.copy(status = newStatus.name))
            triggerAutoBackupIfEnabled()
        }
    }

    fun addDiaryRecord(
        rawTitle: String,
        platform: GamingPlatform,
        service: StoreService,
        date: String,
        rating: Int,
        status: PlayStatus,
        comment: String
    ) {
        if (rawTitle.isBlank()) return
        viewModelScope.launch {
            val matchedSteam = allSteamGames.value.firstOrNull {
                it.title.equals(rawTitle.trim(), ignoreCase = true) ||
                    it.title.contains(rawTitle.trim(), ignoreCase = true)
            }

            var finalTitle = matchedSteam?.title ?: rawTitle.trim()
            var coverUrl = matchedSteam?.headerCoverUrl ?: ""
            var releaseYear = matchedSteam?.releaseYear ?: "—"

            if (matchedSteam == null) {
                val rawgKey = getSavedRawgKey()
                SteamApiClient.searchExternalGameCoverAndYear(rawTitle, rawgKey).onSuccess { ext ->
                    finalTitle = ext.title
                    coverUrl = ext.coverUrl
                    releaseYear = ext.releaseYear
                }
            }

            dao.insertDiaryEntry(
                DiaryEntryEntity(
                    gameTitle = finalTitle,
                    linkedSteamAppId = matchedSteam?.appId,
                    coverUrl = coverUrl,
                    releaseYear = releaseYear,
                    platform = platform.label,
                    storeService = service.label,
                    finishedDate = date,
                    rating = rating,
                    completionStatus = status.label,
                    comment = comment
                )
            )

            if (matchedSteam != null) {
                dao.updateSteamGame(matchedSteam.copy(status = status.name, userRating = rating))
            }
            _statusMessage.value = "Запись «$finalTitle» сохранена в дневник!"
            triggerAutoBackupIfEnabled()
        }
    }

    private suspend fun triggerAutoBackupIfEnabled() {
        val token = getSavedYandexToken()
        if (_autoBackupYandex.value && token.isNotBlank()) {
            val games = dao.getAllSteamGamesSnapshot()
            val diary = dao.getAllDiaryEntriesSnapshot()
            val json = CloudBackupManager.serializeToJson(games, diary)
            CloudBackupManager.uploadToYandexDisk(token, json).onSuccess { path ->
                val info = "Автобэкап Яндекс.Диск ($path)"
                _lastBackupInfo.value = info
                prefs.edit().putString("last_backup_info", info).apply()
            }
        }
    }

    fun getSavedSteamId(): String = prefs.getString("steam_id", "") ?: ""
    fun getSavedApiKey(): String = prefs.getString("steam_key", "") ?: ""
    fun getSavedRawgKey(): String = prefs.getString("rawg_key", "") ?: ""
    fun getSavedYandexToken(): String = prefs.getString("yandex_token", "") ?: ""

    private suspend fun seedStarterData() {
        val sampleSteam = listOf(
            SteamGameEntity(1091500, "Cyberpunk 2077", 6750, "2020", "RPG, Action", "CD PROJEKT RED", status = PlayStatus.BEATEN.name, userRating = 9),
            SteamGameEntity(1145360, "Hades", 5040, "2020", "Roguelike, Action", "Supergiant Games", status = PlayStatus.COMPLETED.name, userRating = 10),
            SteamGameEntity(613830, "Chrono Trigger", 0, "1995", "JRPG, Retro", "Square Enix", status = PlayStatus.BACKLOG.name, userRating = 0),
            SteamGameEntity(1245620, "Elden Ring", 2760, "2022", "RPG, Souls-like", "FromSoftware", status = PlayStatus.PLAYING.name, userRating = 10),
            SteamGameEntity(367520, "Hollow Knight", 0, "2017", "Metroidvania", "Team Cherry", status = PlayStatus.BACKLOG.name, userRating = 0),
            SteamGameEntity(632470, "Disco Elysium", 2300, "2019", "CRPG, Detective", "ZA/UM", status = PlayStatus.BEATEN.name, userRating = 10)
        )
        dao.upsertSteamGames(sampleSteam)

        dao.insertDiaryEntry(
            DiaryEntryEntity(
                gameTitle = "Cyberpunk 2077",
                linkedSteamAppId = 1091500,
                coverUrl = "https://shared.akamai.steamstatic.com/store_item_assets/steam/apps/1091500/header.jpg",
                releaseYear = "2020",
                platform = GamingPlatform.PC.label,
                storeService = StoreService.STEAM.label,
                finishedDate = "2026-08-19",
                rating = 9,
                completionStatus = PlayStatus.BEATEN.label,
                comment = "Пройдено вместе с дополнением Phantom Liberty"
            )
        )
    }
}
