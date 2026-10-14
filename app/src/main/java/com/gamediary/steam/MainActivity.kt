package com.gamediary.steam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gamediary.steam.data.model.*
import com.gamediary.steam.ui.GameDiaryViewModel
import com.gamediary.steam.ui.theme.AppThemeMode
import com.gamediary.steam.ui.theme.SteamGameDiaryTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: GameDiaryViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()

            SteamGameDiaryTheme(themeMode = themeMode) {
                MainAppScaffold(vm = vm, currentTheme = themeMode)
            }
        }
    }
}

@Composable
fun MainAppScaffold(vm: GameDiaryViewModel, currentTheme: AppThemeMode) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val statusMsg by vm.statusMessage.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val tabs = listOf("Библиотека", "Дневник", "Статистика", "Настройки")
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Text(if (index == 0) "🎮" else if (index == 1) "📓" else if (index == 2) "📊" else "⚙️") },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            statusMsg?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { vm.clearMessage() }
                ) {
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            when (selectedTab) {
                0 -> LibraryTab(vm)
                1 -> DiaryTab(vm)
                2 -> StatsTab(vm)
                3 -> SettingsTab(vm, currentTheme)
            }
        }
    }
}

@Composable
fun LibraryTab(vm: GameDiaryViewModel) {
    val games by vm.filteredSteamGames.collectAsState()
    val allGames by vm.allSteamGames.collectAsState()
    val searchQuery by vm.searchQuery.collectAsState()
    val activeFilter by vm.statusFilter.collectAsState()
    val cardSettings by vm.cardSettings.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "STEAM БИБЛИОТЕКА (${allGames.size})",
                style = MaterialTheme.typography.titleLarge
            )
            Button(onClick = { vm.pickRandomBacklogGame() }) {
                Text("🎲 Рандом")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { vm.updateSearch(it) },
            placeholder = { Text("Поиск по названию, жанру, году...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        val filters = listOf(
            "ALL" to "Все (${allGames.size})",
            "UNPLAYED" to "Не играл / 0 ч",
            "PLAYING" to "Играю",
            "BEATEN" to "Пройдено"
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { (key, label) ->
                FilterChip(
                    selected = activeFilter == key,
                    onClick = { vm.updateFilter(key) },
                    label = { Text(label) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(games, key = { it.appId }) { game ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (cardSettings.showCover) {
                            val imgUrl = if (cardSettings.useVerticalPoster) game.verticalCoverUrl else game.headerCoverUrl
                            val imgWidth = if (cardSettings.useVerticalPoster) 56.dp else 92.dp
                            val imgHeight = if (cardSettings.useVerticalPoster) 84.dp else 46.dp
                            AsyncImage(
                                model = imgUrl,
                                contentDescription = game.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width = imgWidth, height = imgHeight)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (cardSettings.showReleaseYear) {
                                    Text("📅 ${game.releaseYear}", style = MaterialTheme.typography.bodyMedium)
                                }
                                if (cardSettings.showPlaytime) {
                                    Text("⏱ ${game.playtimeHours} ч.", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (cardSettings.showGenres) {
                                Text(
                                    text = if (game.developer.isNotBlank()) "${game.genres} • ${game.developer}" else game.genres,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (game.releaseYear == "—") {
                                Text(
                                    text = "↻ Подгрузить год и жанр",
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .clickable { vm.loadMetadataForGame(game) }
                                )
                            }
                        }

                        AssistChip(
                            onClick = {
                                val next = when (game.status) {
                                    PlayStatus.BACKLOG.name -> PlayStatus.PLAYING
                                    PlayStatus.PLAYING.name -> PlayStatus.BEATEN
                                    PlayStatus.BEATEN.name -> PlayStatus.COMPLETED
                                    else -> PlayStatus.BACKLOG
                                }
                                vm.changeGameStatus(game, next)
                            },
                            label = {
                                Text(
                                    runCatching { PlayStatus.valueOf(game.status).label }
                                        .getOrDefault(game.status)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DiaryTab(vm: GameDiaryViewModel) {
    val entries by vm.diaryEntries.collectAsState()
    val allSteam by vm.allSteamGames.collectAsState()

    var titleInput by remember { mutableStateOf("") }
    var selectedPlatform by remember { mutableStateOf(GamingPlatform.PC) }
    var selectedService by remember { mutableStateOf(StoreService.STEAM) }
    var dateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var ratingInput by remember { mutableIntStateOf(9) }
    var commentInput by remember { mutableStateOf("") }

    val matchedSteamGame = remember(titleInput, allSteam) {
        if (titleInput.length < 2) null
        else allSteam.firstOrNull { it.title.contains(titleInput.trim(), ignoreCase = true) }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("ДНЕВНИК ПРОХОЖДЕНИЙ", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Добавить пройденную игру", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Название (автопоиск: база Steam + RAWG/Store)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (matchedSteamGame != null) {
                        Text(
                            text = "✓ Найдено в вашей базе Steam: ${matchedSteamGame.title} (${matchedSteamGame.playtimeHours} ч.) — обложка подтянется автоматически",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else if (titleInput.length >= 3) {
                        Text(
                            text = "🔎 Внешняя игра — при сохранении обложка и год подтянутся через RAWG / Steam Store Search",
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Платформа:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(GamingPlatform.entries) { pf ->
                            FilterChip(
                                selected = selectedPlatform == pf,
                                onClick = { selectedPlatform = pf },
                                label = { Text(pf.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Сервис / Магазин / Носитель:", style = MaterialTheme.typography.labelSmall)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(StoreService.entries) { sv ->
                            FilterChip(
                                selected = selectedService == sv,
                                onClick = { selectedService = sv },
                                label = { Text(sv.label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            label = { Text("Дата (ГГГГ-ММ-ДД)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ratingInput.toString(),
                            onValueChange = { ratingInput = it.toIntOrNull()?.coerceIn(1, 10) ?: 9 },
                            label = { Text("Оценка (1-10)") },
                            modifier = Modifier.weight(0.7f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("Заметка / впечатления") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            vm.addDiaryRecord(
                                rawTitle = matchedSteamGame?.title ?: titleInput,
                                platform = selectedPlatform,
                                service = selectedService,
                                date = dateInput,
                                rating = ratingInput,
                                status = PlayStatus.BEATEN,
                                comment = commentInput
                            )
                            titleInput = ""
                            commentInput = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сохранить в дневник")
                    }
                }
            }
        }

        items(entries, key = { it.id }) { entry ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (entry.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = entry.coverUrl,
                            contentDescription = entry.gameTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(width = 84.dp, height = 46.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(entry.gameTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("★ ${entry.rating}/10", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("🎮 ${entry.platform} • 💿 ${entry.storeService} ${if (entry.linkedSteamAppId != null) "• ✓ Steam" else ""}")
                        Text("📅 Завершено: ${entry.finishedDate} (${entry.completionStatus})")
                        if (entry.comment.isNotBlank()) {
                            Text("💬 ${entry.comment}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsTab(vm: GameDiaryViewModel) {
    val allGames by vm.allSteamGames.collectAsState()
    val diary by vm.diaryEntries.collectAsState()

    val playedCount = allGames.count { it.playtimeMinutes > 0 }
    val unplayedCount = allGames.size - playedCount
    val metadataLoadedCount = allGames.count { it.releaseYear != "—" }
    val byPlatform = diary.groupingBy { it.platform }.eachCount()
    val byService = diary.groupingBy { it.storeService }.eachCount()

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("СТАТИСТИКА БИБЛИОТЕКИ И ДНЕВНИКА", style = MaterialTheme.typography.titleLarge)
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Бэклог Steam", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Всего игр в базе Steam: ${allGames.size}")
                    Text("Запущено хотя бы раз: $playedCount")
                    Text("Ни разу не запускались (0 ч.): $unplayedCount")
                    Text("Загружено годов выпуска и жанров: $metadataLoadedCount из ${allGames.size}")
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Пройдено по платформам (всего: ${diary.size})", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    byPlatform.forEach { (pf, count) ->
                        Text("• $pf: $count игр")
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Пройдено по сервисам и носителям", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    byService.forEach { (sv, count) ->
                        Text("• $sv: $count игр")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsTab(vm: GameDiaryViewModel, currentTheme: AppThemeMode) {
    val context = LocalContext.current
    val cardSettings by vm.cardSettings.collectAsState()
    val isBatchScraping by vm.isBatchScraping.collectAsState()
    val allGames by vm.allSteamGames.collectAsState()
    val autoBackupYandex by vm.autoBackupYandex.collectAsState()
    val lastBackupInfo by vm.lastBackupInfo.collectAsState()

    var steamId by remember { mutableStateOf(vm.getSavedSteamId()) }
    var apiKey by remember { mutableStateOf(vm.getSavedApiKey()) }
    var rawgKey by remember { mutableStateOf(vm.getSavedRawgKey()) }
    var yandexToken by remember { mutableStateOf(vm.getSavedYandexToken()) }

    val loadedMetaCount = allGames.count { it.releaseYear != "—" }

    // Лаунчеры для сохранения/восстановления через приложения Google Диск и Яндекс.Диск (Storage Access Framework)
    val exportCloudLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.exportBackupToUri(context, uri)
    }

    val importCloudLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importBackupFromUri(context, uri)
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 1. ОБЛАЧНЫЙ БЭКАП (Яндекс.Диск API + Google Диск SAF + Android Auto Backup)
        item {
            Text("☁️ ОБЛАЧНОЕ СОХРАНЕНИЕ (GOOGLE / ЯНДЕКС ДИСК)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "✓ Системный автобэкап Android в Google Диск включен.
✓ Последняя синхронизация: $lastBackupInfo",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    SettingSwitchRow("Автобэкап на Яндекс.Диск при изменениях", autoBackupYandex) {
                        vm.setAutoBackupYandex(it)
                    }

                    OutlinedTextField(
                        value = yandexToken,
                        onValueChange = { yandexToken = it },
                        label = { Text("OAuth-токен Яндекс.Диска (прямая выгрузка в 1 клик)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { vm.backupToYandexDisk(yandexToken) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("☁️ Выгрузить в Яндекс.Диск")
                        }
                        Button(
                            onClick = { vm.restoreFromYandexDisk(yandexToken) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📥 Скачать с Яндекс.Диска")
                        }
                    }

                    Divider()

                    Text(
                        text = "Универсальный облачный пикер (работает с установленными приложениями Google Диск и Яндекс.Диск без токенов):",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { exportCloudLauncher.launch("game_diary_backup.json") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📂 Сохранить в Google/Яндекс Диск")
                        }
                        OutlinedButton(
                            onClick = { importCloudLauncher.launch(arrayOf("application/json", "*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📂 Восстановить из облака")
                        }
                    }
                }
            }
        }

        // 2. Выбор темы
        item {
            Text("🎨 ТЕМА ОФОРМЛЕНИЯ", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppThemeMode.entries.forEach { mode ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.setTheme(mode) }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = currentTheme == mode,
                                onClick = { vm.setTheme(mode) }
                            )
                            Column { Text(mode.title, style = MaterialTheme.typography.titleMedium); Text(mode.subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                    }
                }
            }
        }

        // 3. Отображение полей на карточке
        item {
            Text("🗂️ ОТОБРАЖЕНИЕ КАРТОЧЕК В СПИСКЕ", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SettingSwitchRow("Показывать обложки игр (Steam CDN / RAWG)", cardSettings.showCover) {
                        vm.updateCardSettings(cardSettings.copy(showCover = it))
                    }
                    SettingSwitchRow("Вертикальные постеры 600×900 (вместо баннеров)", cardSettings.useVerticalPoster) {
                        vm.updateCardSettings(cardSettings.copy(useVerticalPoster = it))
                    }
                    SettingSwitchRow("Показывать год выпуска", cardSettings.showReleaseYear) {
                        vm.updateCardSettings(cardSettings.copy(showReleaseYear = it))
                    }
                    SettingSwitchRow("Показывать наигранные часы Steam", cardSettings.showPlaytime) {
                        vm.updateCardSettings(cardSettings.copy(showPlaytime = it))
                    }
                    SettingSwitchRow("Показывать жанры и разработчика", cardSettings.showGenres) {
                        vm.updateCardSettings(cardSettings.copy(showGenres = it))
                    }
                }
            }
        }

        // 4. Скрэпинг обложек и фоновая очередь метаданных
        item {
            Text("🖼️ СКРЭПИНГ ОБЛОЖЕК И МЕТАДАННЫХ", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Обложки Steam загружаются напрямую с CDN по appId. Годы выпуска и жанры догружаются из Steam Store API с интервалом 1.6 сек.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Загружено метаданных: $loadedMetaCount из ${allGames.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { vm.toggleBatchMetadataScraping() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isBatchScraping) "⏸ Остановить фоновую догрузку" else "▶ Запустить фоновую догрузку годов и жанров")
                    }
                }
            }
        }

        // 5. Ключи API: Steam + RAWG
        item {
            Text("🔑 ИСТОЧНИКИ ДАННЫХ (STEAM & RAWG API)", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(6.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = steamId,
                        onValueChange = { steamId = it },
                        label = { Text("SteamID64 (17 цифр)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Steam Web API Key (steamcommunity.com/dev/apikey)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rawgKey,
                        onValueChange = { rawgKey = it },
                        label = { Text("RAWG API Key (опционально — обложки PS5/Switch/3DS/Xbox)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            vm.saveApiCredentials(steamId, apiKey, rawgKey, yandexToken)
                            vm.syncSteamLibrary(steamId, apiKey)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Сохранить ключи и синхронизировать 1800+ игр")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
