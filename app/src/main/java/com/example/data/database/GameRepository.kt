package com.example.data.database

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<SavedGame>> = gameDao.getAllGames()

    suspend fun getGameById(id: Int): SavedGame? {
        return gameDao.getGameById(id)
    }

    suspend fun insertGame(game: SavedGame) {
        gameDao.insertGame(game)
    }

    suspend fun deleteGame(game: SavedGame) {
        gameDao.deleteGame(game)
    }

    suspend fun deleteGameById(id: Int) {
        gameDao.deleteGameById(id)
    }
}
