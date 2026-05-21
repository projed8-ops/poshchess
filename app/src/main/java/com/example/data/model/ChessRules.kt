package com.example.data.model

import kotlin.math.abs

object ChessRules {
    fun getValidMoves(index: Int, state: ChessGameState): List<Int> {
        val piece = state.board[index] ?: return emptyList()
        val moves = mutableListOf<Int>()
        val row = index / 8
        val col = index % 8

        when (piece.type) {
            PieceType.PAWN -> {
                val direction = if (piece.color == PieceColor.WHITE) -1 else 1
                val nextRow = row + direction
                
                // Single step forward
                if (nextRow in 0..7) {
                    val targetIdx = nextRow * 8 + col
                    if (state.board[targetIdx] == null) {
                        moves.add(targetIdx)
                        
                        // Double step forward
                        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
                        val doubleRow = row + 2 * direction
                        if (row == startRow && doubleRow in 0..7) {
                            val doubleIdx = doubleRow * 8 + col
                            if (state.board[doubleIdx] == null) {
                                moves.add(doubleIdx)
                            }
                        }
                    }
                }

                // Standard diagonal captures
                for (dCol in listOf(-1, 1)) {
                    val targetCol = col + dCol
                    if (targetCol in 0..7 && nextRow in 0..7) {
                        val targetIdx = nextRow * 8 + targetCol
                        val targetPiece = state.board[targetIdx]
                        if (targetPiece != null && targetPiece.color != piece.color) {
                            moves.add(targetIdx)
                        }
                    }
                }

                // En Passant
                if (state.enPassantTarget != "-") {
                    val epColChar = state.enPassantTarget.getOrNull(0)
                    val epRowChar = state.enPassantTarget.getOrNull(1)
                    if (epColChar != null && epRowChar != null) {
                        val epCol = epColChar - 'a'
                        val epRowDigit = epRowChar.digitToIntOrNull()
                        if (epRowDigit != null) {
                            val epRow = 8 - epRowDigit
                            if (epRow == nextRow && abs(epCol - col) == 1) {
                                moves.add(epRow * 8 + epCol)
                            }
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for (offset in offsets) {
                    val r = row + offset.first
                    val c = col + offset.second
                    if (r in 0..7 && c in 0..7) {
                        val targetIdx = r * 8 + c
                        val targetPiece = state.board[targetIdx]
                        if (targetPiece == null || targetPiece.color != piece.color) {
                            moves.add(targetIdx)
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                val dirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
                for (dir in dirs) {
                    var r = row + dir.first
                    var c = col + dir.second
                    while (r in 0..7 && c in 0..7) {
                        val targetIdx = r * 8 + c
                        val targetPiece = state.board[targetIdx]
                        if (targetPiece == null) {
                            moves.add(targetIdx)
                        } else {
                            if (targetPiece.color != piece.color) {
                                moves.add(targetIdx)
                            }
                            break // Blocked
                        }
                        r += dir.first
                        c += dir.second
                    }
                }
            }

            PieceType.ROOK -> {
                val dirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
                for (dir in dirs) {
                    var r = row + dir.first
                    var c = col + dir.second
                    while (r in 0..7 && c in 0..7) {
                        val targetIdx = r * 8 + c
                        val targetPiece = state.board[targetIdx]
                        if (targetPiece == null) {
                            moves.add(targetIdx)
                        } else {
                            if (targetPiece.color != piece.color) {
                                moves.add(targetIdx)
                            }
                            break // Blocked
                        }
                        r += dir.first
                        c += dir.second
                    }
                }
            }

            PieceType.QUEEN -> {
                val dirs = listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                )
                for (dir in dirs) {
                    var r = row + dir.first
                    var c = col + dir.second
                    while (r in 0..7 && c in 0..7) {
                        val targetIdx = r * 8 + c
                        val targetPiece = state.board[targetIdx]
                        if (targetPiece == null) {
                            moves.add(targetIdx)
                        } else {
                            if (targetPiece.color != piece.color) {
                                moves.add(targetIdx)
                            }
                            break // Blocked
                        }
                        r += dir.first
                        c += dir.second
                    }
                }
            }

            PieceType.KING -> {
                val dirs = listOf(
                    Pair(-1, -1), Pair(-1, 0), Pair(-1, 1),
                    Pair(0, -1),              Pair(0, 1),
                    Pair(1, -1),  Pair(1, 0),  Pair(1, 1)
                )
                for (dir in dirs) {
                    val r = row + dir.first
                    val c = col + dir.second
                    if (r in 0..7 && c in 0..7) {
                        val targetIdx = r * 8 + c
                        val targetPiece = state.board[targetIdx]
                        if (targetPiece == null || targetPiece.color != piece.color) {
                            moves.add(targetIdx)
                        }
                    }
                }

                // Castling checks (Simple path verification)
                if (piece.color == PieceColor.WHITE) {
                    if (row == 7 && col == 4) {
                        if (state.castlingRights.contains('K')) {
                            if (state.board[7 * 8 + 5] == null && state.board[7 * 8 + 6] == null) {
                                moves.add(7 * 8 + 6)
                            }
                        }
                        if (state.castlingRights.contains('Q')) {
                            if (state.board[7 * 8 + 3] == null && state.board[7 * 8 + 2] == null && state.board[7 * 8 + 1] == null) {
                                moves.add(7 * 8 + 2)
                            }
                        }
                    }
                } else {
                    if (row == 0 && col == 4) {
                        if (state.castlingRights.contains('k')) {
                            if (state.board[0 * 8 + 5] == null && state.board[0 * 8 + 6] == null) {
                                moves.add(0 * 8 + 6)
                            }
                        }
                        if (state.castlingRights.contains('q')) {
                            if (state.board[0 * 8 + 3] == null && state.board[0 * 8 + 2] == null && state.board[0 * 8 + 1] == null) {
                                moves.add(0 * 8 + 2)
                            }
                        }
                    }
                }
            }
        }
        return moves
    }

    fun makeMove(fromIdx: Int, toIdx: Int, state: ChessGameState): ChessGameState {
        val board = state.board.copyOf()
        val piece = board[fromIdx] ?: return state
        
        val fromRow = fromIdx / 8
        val fromCol = fromIdx % 8
        val toRow = toIdx / 8
        val toCol = toIdx % 8

        board[fromIdx] = null

        // Handle Castling moves (adjust King and Rook)
        if (piece.type == PieceType.KING && abs(fromCol - toCol) == 2) {
            if (piece.color == PieceColor.WHITE) {
                if (toCol == 6) { // Kingside
                    board[7 * 8 + 6] = piece
                    board[7 * 8 + 5] = board[7 * 8 + 7]
                    board[7 * 8 + 7] = null
                } else if (toCol == 2) { // Queenside
                    board[7 * 8 + 2] = piece
                    board[7 * 8 + 3] = board[7 * 8 + 0]
                    board[7 * 8 + 0] = null
                }
            } else {
                if (toCol == 6) { // Kingside
                    board[0 * 8 + 6] = piece
                    board[0 * 8 + 5] = board[0 * 8 + 7]
                    board[0 * 8 + 7] = null
                } else if (toCol == 2) { // Queenside
                    board[0 * 8 + 2] = piece
                    board[0 * 8 + 3] = board[0 * 8 + 0]
                    board[0 * 8 + 0] = null
                }
            }
        } else if (piece.type == PieceType.PAWN && toIdx == (fromIdx + (if (piece.color == PieceColor.WHITE) -8 else 8)) + (toCol - fromCol) && board[toIdx] == null && abs(fromCol - toCol) == 1) {
            // En Passant capture target
            val direction = if (piece.color == PieceColor.WHITE) 1 else -1
            val captureRow = toRow + direction
            board[captureRow * 8 + toCol] = null
            board[toIdx] = piece
        } else {
            // Standard piece pawn promotion with auto promotion
            if (piece.type == PieceType.PAWN && (toRow == 0 || toRow == 7)) {
                board[toIdx] = ChessPiece(piece.color, PieceType.QUEEN)
            } else {
                board[toIdx] = piece
            }
        }

        // Compute new Castling rights
        var rights = state.castlingRights
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                rights = rights.replace("K", "").replace("Q", "")
            } else {
                rights = rights.replace("k", "").replace("q", "")
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (piece.color == PieceColor.WHITE) {
                if (fromIdx == 7 * 8 + 7) rights = rights.replace("K", "")
                if (fromIdx == 7 * 8 + 0) rights = rights.replace("Q", "")
            } else {
                if (fromIdx == 0 * 8 + 7) rights = rights.replace("k", "")
                if (fromIdx == 0 * 8 + 0) rights = rights.replace("q", "")
            }
        }

        // Set new En Passant target
        var enPassant = "-"
        if (piece.type == PieceType.PAWN && abs(fromRow - toRow) == 2) {
            val midRow = (fromRow + toRow) / 2
            val colChar = ('a' + toCol).toString()
            val rowChar = (8 - midRow).toString()
            enPassant = "$colChar$rowChar"
        }

        val activeColor = state.activeColor.alternate()
        val halfmove = if (piece.type == PieceType.PAWN || board[toIdx] != piece) 0 else state.halfmoveClock + 1
        val fullmove = if (state.activeColor == PieceColor.BLACK) state.fullmoveNumber + 1 else state.fullmoveNumber

        return ChessGameState(board, activeColor, rights, enPassant, halfmove, fullmove)
    }

    fun getSquareName(index: Int): String {
        val row = index / 8
        val col = index % 8
        val colChar = ('a' + col).toString()
        val rowVal = 8 - row
        return "$colChar$rowVal"
    }

    fun parseSquareName(name: String): Int? {
        if (name.length < 2) return null
        val col = name[0] - 'a'
        val rowDigit = name[1].digitToIntOrNull() ?: return null
        val row = 8 - rowDigit
        if (col !in 0..7 || row !in 0..7) return null
        return row * 8 + col
    }

    fun evaluateBoard(board: Array<ChessPiece?>): Int {
        var score = 0
        val pieceValues = mapOf(
            PieceType.PAWN to 100,
            PieceType.KNIGHT to 320,
            PieceType.BISHOP to 330,
            PieceType.ROOK to 500,
            PieceType.QUEEN to 900,
            PieceType.KING to 20000
        )

        for (i in 0..63) {
            val p = board[i] ?: continue
            val sign = if (p.color == PieceColor.WHITE) 1 else -1
            val row = i / 8
            val col = i % 8
            
            var valPiece = pieceValues[p.type] ?: 0
            
            if (p.type == PieceType.PAWN || p.type == PieceType.KNIGHT || p.type == PieceType.BISHOP) {
                val centerDistance = abs(row - 3.5) + abs(col - 3.5)
                valPiece += ((8.0 - centerDistance) * 5).toInt()
            }
            if (p.type == PieceType.KING) {
                val baseRank = if (p.color == PieceColor.WHITE) 7 else 0
                if (row == baseRank && (col == 1 || col == 2 || col == 6 || col == 7)) {
                    valPiece += 50
                }
            }

            score += sign * valPiece
        }
        return score
    }

    suspend fun findBestMoveOffline(state: ChessGameState, depth: Int = 3): Pair<Int, Int>? {
        val board = state.board
        val color = state.activeColor
        var bestMove: Pair<Int, Int>? = null
        
        var bestVal = if (color == PieceColor.WHITE) Int.MIN_VALUE else Int.MAX_VALUE
        
        val moves = mutableListOf<Pair<Int, Int>>()
        for (fromIdx in 0..63) {
            val piece = board[fromIdx] ?: continue
            if (piece.color == color) {
                val targets = getValidMoves(fromIdx, state)
                for (toIdx in targets) {
                    moves.add(Pair(fromIdx, toIdx))
                }
            }
        }
        
        if (moves.isEmpty()) return null
        
        for (move in moves) {
            kotlinx.coroutines.yield()
            val nextState = makeMove(move.first, move.second, state)
            val score = minimax(nextState, depth - 1, Int.MIN_VALUE, Int.MAX_VALUE, nextState.activeColor == PieceColor.WHITE)
            if (color == PieceColor.WHITE) {
                if (score > bestVal) {
                    bestVal = score
                    bestMove = move
                }
            } else {
                if (score < bestVal) {
                    bestVal = score
                    bestMove = move
                }
            }
        }
        return bestMove
    }

    private suspend fun minimax(state: ChessGameState, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        if (depth == 0) {
            return evaluateBoard(state.board)
        }
        kotlinx.coroutines.yield()
        
        var currentAlpha = alpha
        var currentBeta = beta

        val moves = mutableListOf<Pair<Int, Int>>()
        for (fromIdx in 0..63) {
            val piece = state.board[fromIdx] ?: continue
            if (piece.color == (if (isMaximizing) PieceColor.WHITE else PieceColor.BLACK)) {
                val targets = getValidMoves(fromIdx, state)
                for (toIdx in targets) {
                    moves.add(Pair(fromIdx, toIdx))
                }
            }
        }

        if (moves.isEmpty()) {
            return evaluateBoard(state.board)
        }

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in moves) {
                kotlinx.coroutines.yield()
                val nextState = makeMove(move.first, move.second, state)
                val evaluation = minimax(nextState, depth - 1, currentAlpha, currentBeta, false)
                maxEval = maxOf(maxEval, evaluation)
                currentAlpha = maxOf(currentAlpha, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in moves) {
                kotlinx.coroutines.yield()
                val nextState = makeMove(move.first, move.second, state)
                val evaluation = minimax(nextState, depth - 1, currentAlpha, currentBeta, true)
                minEval = minOf(minEval, evaluation)
                currentBeta = minOf(currentBeta, evaluation)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }
}
