package com.gamediary.steam.data.local

import androidx.room.*
import com.gamediary.steam.data.model.DiaryEntryEntity
import com.gamediary.steam.data.model.SteamGameEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM steam_games ORDER BY title ASC")
    fun observeAllSteamGames(): Flow<List<SteamGameEntity>>

    @Query("SELECT * FROM steam_games")
    suspend fun getAllSteamGamesSnapshot(): List<SteamGameEntity>

    @Query("SELECT * FROM steam_games WHERE releaseYear = '—' ORDER BY playtimeMinutes DESC LIMIT :limit")
    suspend fun getGamesMissingMetadata(limit: Int): List<SteamGameEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSteamGames(games: List<SteamGameEntity>)

    @Update
    suspend fun updateSteamGame(game: SteamGameEntity)

    @Query("SELECT * FROM diary_entries ORDER BY finishedDate DESC, id DESC")
    fun observeDiaryEntries(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries ORDER BY finishedDate DESC, id DESC")
    suspend fun getAllDiaryEntriesSnapshot(): List<DiaryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDiaryEntries(entries: List<DiaryEntryEntity>)

    @Delete
    suspend fun deleteDiaryEntry(entry: DiaryEntryEntity)

    @Query("SELECT COUNT(*) FROM steam_games")
    suspend fun getSteamGamesCount(): Int
}
