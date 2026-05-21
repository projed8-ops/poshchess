package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_games ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<SavedGame>>

    @Query("SELECT * FROM saved_games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Int): SavedGame?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: SavedGame)

    @Delete
    suspend fun deleteGame(game: SavedGame)

    @Query("DELETE FROM saved_games WHERE id = :id")
    suspend fun deleteGameById(id: Int)
}
