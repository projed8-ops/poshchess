package com.example.data.model

import java.util.UUID

enum class PieceColor {
    WHITE, BLACK;
    
    fun alternate(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType {
    PAWN, ROOK, KNIGHT, BISHOP, QUEEN, KING
}

data class ChessPiece(
    val color: PieceColor,
    val type: PieceType
) {
    fun getUnicodeSymbol(): String {
        return when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
    }

    fun getSymbolChar(): Char {
        return when (color) {
            PieceColor.WHITE -> when (type) {
                PieceType.KING -> 'K'
                PieceType.QUEEN -> 'Q'
                PieceType.ROOK -> 'R'
                PieceType.BISHOP -> 'B'
                PieceType.KNIGHT -> 'N'
                PieceType.PAWN -> 'P'
            }
            PieceColor.BLACK -> when (type) {
                PieceType.KING -> 'k'
                PieceType.QUEEN -> 'q'
                PieceType.ROOK -> 'r'
                PieceType.BISHOP -> 'b'
                PieceType.KNIGHT -> 'n'
                PieceType.PAWN -> 'p'
            }
        }
    }
}

data class ChessGameState(
    val board: Array<ChessPiece?>, // 64 size array
    val activeColor: PieceColor,
    val castlingRights: String, // "KQkq" or "-"
    val enPassantTarget: String, // "e3" or "-"
    val halfmoveClock: Int,
    val fullmoveNumber: Int
) {
    fun toFen(): String {
        val boardStr = StringBuilder()
        for (row in 0..7) {
            var emptyCount = 0
            for (col in 0..7) {
                val piece = board[row * 8 + col]
                if (piece == null) {
                    emptyCount++
                } else {
                    if (emptyCount > 0) {
                        boardStr.append(emptyCount)
                        emptyCount = 0
                    }
                    boardStr.append(piece.getSymbolChar())
                }
            }
            if (emptyCount > 0) {
                boardStr.append(emptyCount)
            }
            if (row < 7) {
                boardStr.append('/')
            }
        }
        val colorStr = if (activeColor == PieceColor.WHITE) "w" else "b"
        val castling = if (castlingRights.isEmpty()) "-" else castlingRights
        val ep = if (enPassantTarget.isEmpty()) "-" else enPassantTarget
        return "$boardStr $colorStr $castling $ep $halfmoveClock $fullmoveNumber"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessGameState) return false
        if (!board.contentEquals(other.board)) return false
        if (activeColor != other.activeColor) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantTarget != other.enPassantTarget) return false
        if (halfmoveClock != other.halfmoveClock) return false
        if (fullmoveNumber != other.fullmoveNumber) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + activeColor.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + enPassantTarget.hashCode()
        result = 31 * result + halfmoveClock
        result = 31 * result + fullmoveNumber
        return result
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String): ChessGameState {
            val parts = fen.trim().split("\\s+".toRegex())
            val board = Array<ChessPiece?>(64) { null }
            if (parts.isEmpty()) return createDefault()

            val boardPart = parts[0]
            val rows = boardPart.split('/')
            for (r in 0 until minOf(8, rows.size)) {
                val rowStr = rows[r]
                var col = 0
                for (char in rowStr) {
                    if (col >= 8) break
                    if (char.isDigit()) {
                        col += char.digitToInt()
                    } else {
                        val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                        val type = when (char.lowercaseChar()) {
                            'p' -> PieceType.PAWN
                            'r' -> PieceType.ROOK
                            'n' -> PieceType.KNIGHT
                            'b' -> PieceType.BISHOP
                            'q' -> PieceType.QUEEN
                            'k' -> PieceType.KING
                            else -> PieceType.PAWN
                        }
                        if (col in 0..7) {
                            board[r * 8 + col] = ChessPiece(color, type)
                        }
                        col++
                    }
                }
            }

            val activeColor = if (parts.size > 1 && parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
            val castlingRights = if (parts.size > 2) parts[2] else "KQkq"
            val enPassantTarget = if (parts.size > 3) parts[3] else "-"
            val halfmoveClock = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            val fullmoveNumber = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

            return ChessGameState(board, activeColor, castlingRights, enPassantTarget, halfmoveClock, fullmoveNumber)
        }

        fun createDefault(): ChessGameState {
            return fromFen(STARTING_FEN)
        }

        fun createEmpty(): ChessGameState {
            return ChessGameState(
                board = Array(64) { null },
                activeColor = PieceColor.WHITE,
                castlingRights = "-",
                enPassantTarget = "-",
                halfmoveClock = 0,
                fullmoveNumber = 1
            )
        }
    }
}
