package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class SavedGame(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val notes: String = "",
    val startingFen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val currentFen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val playedMoves: String = "", // e.g., "e2e4,e7e5"
    val fensList: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // Semi-colon separated FEN positions
    val timestamp: Long = System.currentTimeMillis()
)
