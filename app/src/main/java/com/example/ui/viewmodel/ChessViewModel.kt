package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.CloudEvalResponse
import com.example.data.api.LichessApiClient
import com.example.data.database.AppDatabase
import com.example.data.database.GameRepository
import com.example.data.database.SavedGame
import com.example.data.model.ChessGameState
import com.example.data.model.ChessPiece
import com.example.data.model.ChessRules
import com.example.data.model.PieceColor
import com.example.data.model.PieceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChessViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
    }

    val savedGames: StateFlow<List<SavedGame>> = repository.allGames
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current interactive game state
    private val _gameState = MutableStateFlow(ChessGameState.createDefault())
    val gameState = _gameState.asStateFlow()

    // Board configuration mode toggle
    private val _boardEditorMode = MutableStateFlow(false)
    val boardEditorMode = _boardEditorMode.asStateFlow()

    // Selections
    private val _selectedSquare = MutableStateFlow<Int?>(null)
    val selectedSquare = _selectedSquare.asStateFlow()

    private val _validMoves = MutableStateFlow<List<Int>>(emptyList())
    val validMoves = _validMoves.asStateFlow()

    // Analysis results
    private val _lichessEvaluation = MutableStateFlow<CloudEvalResponse?>(null)
    val lichessEvaluation = _lichessEvaluation.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // Fallback local engine evaluation
    private val _localBestMove = MutableStateFlow<Pair<Int, Int>?>(null)
    val localBestMove = _localBestMove.asStateFlow()

    private val _localEvalScore = MutableStateFlow(0)
    val localEvalScore = _localEvalScore.asStateFlow()

    // Active loaded game context
    private val _activeGameId = MutableStateFlow<Int?>(null)
    val activeGameId = _activeGameId.asStateFlow()

    private val _activeGameTitle = MutableStateFlow("Unsaved Session")
    val activeGameTitle = _activeGameTitle.asStateFlow()

    private val _activeGameNotes = MutableStateFlow("")
    val activeGameNotes = _activeGameNotes.asStateFlow()

    // History tracking (allows undo - redo and forward - backward stepping)
    private var fensHistory = mutableListOf(ChessGameState.STARTING_FEN)
    private var movesHistory = mutableListOf<String>()
    
    private val _historyIndex = MutableStateFlow(0)
    val historyIndex = _historyIndex.asStateFlow()

    private val _movesList = MutableStateFlow<List<String>>(emptyList())
    val movesList = _movesList.asStateFlow()

    init {
        // Trigger initial position analysis
        analyzeCurrentPosition()
    }

    fun toggleBoardEditorMode() {
        _boardEditorMode.value = !_boardEditorMode.value
        _selectedSquare.value = null
        _validMoves.value = emptyList()
    }

    fun selectSquare(index: Int) {
        if (_boardEditorMode.value) return // Board editor handles placements separately

        val currentSelected = _selectedSquare.value
        val state = _gameState.value

        if (currentSelected == null) {
            // First tap: Select friendly piece
            val piece = state.board[index]
            if (piece != null && piece.color == state.activeColor) {
                _selectedSquare.value = index
                _validMoves.value = ChessRules.getValidMoves(index, state)
            }
        } else {
            // Second tap: Move or change selection
            if (_validMoves.value.contains(index)) {
                // Perform valid move
                performMove(currentSelected, index)
            } else {
                // Try selecting another friendly piece instead
                val piece = state.board[index]
                if (piece != null && piece.color == state.activeColor) {
                    _selectedSquare.value = index
                    _validMoves.value = ChessRules.getValidMoves(index, state)
                } else {
                    // Tap on empty space or enemy without move: clear selection
                    _selectedSquare.value = null
                    _validMoves.value = emptyList()
                }
            }
        }
    }

    private fun performMove(fromIdx: Int, toIdx: Int) {
        val state = _gameState.value
        val piece = state.board[fromIdx] ?: return

        // Save move names in algebraic form (e.g. "e2e4" or "Nf3")
        val moveName = "${ChessRules.getSquareName(fromIdx)}${ChessRules.getSquareName(toIdx)}"
        
        val nextState = ChessRules.makeMove(fromIdx, toIdx, state)

        // Trim any forward history if we made a move while looking at a past position
        val currentIndex = _historyIndex.value
        if (currentIndex < fensHistory.size - 1) {
            fensHistory = fensHistory.subList(0, currentIndex + 1).toMutableList()
            movesHistory = movesHistory.subList(0, currentIndex).toMutableList()
        }

        fensHistory.add(nextState.toFen())
        movesHistory.add(moveName)

        _gameState.value = nextState
        _historyIndex.value = fensHistory.size - 1
        _movesList.value = ArrayList(movesHistory)

        _selectedSquare.value = null
        _validMoves.value = emptyList()

        // Auto analysis & Auto database sync
        analyzeCurrentPosition()
        autoSaveIfActive()
    }

    fun stepHistory(direction: Int) {
        val currentIdx = _historyIndex.value
        val targetIdx = currentIdx + direction
        if (targetIdx in 0 until fensHistory.size) {
            _historyIndex.value = targetIdx
            val targetFen = fensHistory[targetIdx]
            _gameState.value = ChessGameState.fromFen(targetFen)
            
            _selectedSquare.value = null
            _validMoves.value = emptyList()

            analyzeCurrentPosition()
        }
    }

    fun loadFen(fen: String) {
        try {
            val state = ChessGameState.fromFen(fen)
            _gameState.value = state
            
            // Overwrite/reset the history trail to this position as the root
            fensHistory = mutableListOf(fen)
            movesHistory = mutableListOf()
            _historyIndex.value = 0
            _movesList.value = emptyList()

            _selectedSquare.value = null
            _validMoves.value = emptyList()

            analyzeCurrentPosition()
            autoSaveIfActive()
        } catch (e: Exception) {
            // Enforce recovery values
        }
    }

    fun modifySquareInEditor(index: Int, piece: ChessPiece?) {
        val currentBoard = _gameState.value.board.copyOf()
        currentBoard[index] = piece
        
        val updatedState = _gameState.value.copy(board = currentBoard)
        _gameState.value = updatedState

        // Re-analyze edited configuration
        val currentFen = updatedState.toFen()
        fensHistory[_historyIndex.value] = currentFen
        analyzeCurrentPosition()
        autoSaveIfActive()
    }

    fun setEditorTurn(color: PieceColor) {
        val updatedState = _gameState.value.copy(activeColor = color)
        _gameState.value = updatedState

        val currentFen = updatedState.toFen()
        fensHistory[_historyIndex.value] = currentFen
        analyzeCurrentPosition()
        autoSaveIfActive()
    }

    fun resetBoard() {
        val starting = ChessGameState.createDefault()
        _gameState.value = starting
        _boardEditorMode.value = false

        fensHistory = mutableListOf(ChessGameState.STARTING_FEN)
        movesHistory = mutableListOf()
        _historyIndex.value = 0
        _movesList.value = emptyList()

        _selectedSquare.value = null
        _validMoves.value = emptyList()

        analyzeCurrentPosition()
        autoSaveIfActive()
    }

    fun clearBoard() {
        val empty = ChessGameState.createEmpty()
        _gameState.value = empty
        _boardEditorMode.value = true

        val emptyFen = empty.toFen()
        fensHistory = mutableListOf(emptyFen)
        movesHistory = mutableListOf()
        _historyIndex.value = 0
        _movesList.value = emptyList()

        _selectedSquare.value = null
        _validMoves.value = emptyList()

        analyzeCurrentPosition()
    }

    private var cloudAnalysisJob: kotlinx.coroutines.Job? = null
    private var localAnalysisJob: kotlinx.coroutines.Job? = null

    private fun analyzeCurrentPosition() {
        val currentFen = _gameState.value.toFen()

        // Cancel previous running jobs to avoid CPU and network resource starvation!
        cloudAnalysisJob?.cancel()
        localAnalysisJob?.cancel()

        // 1. Trigger Async Lichess API / Stockfish cloud database evaluation
        _isAnalyzing.value = true
        _lichessEvaluation.value = null

        cloudAnalysisJob = viewModelScope.launch {
            val response = LichessApiClient.getCloudEvaluation(currentFen)
            if (response.fen == _gameState.value.toFen()) {
                _lichessEvaluation.value = response
                _isAnalyzing.value = false
            }
        }

        // 2. Trigger fallback local engine minimax in-parallel
        localAnalysisJob = viewModelScope.launch {
            val board = _gameState.value.board
            val (bestMove, score) = withContext(Dispatchers.Default) {
                val bm = ChessRules.findBestMoveOffline(_gameState.value)
                val s = ChessRules.evaluateBoard(board)
                Pair(bm, s)
            }

            _localBestMove.value = bestMove
            _localEvalScore.value = score
        }
    }

    // Room persistence logic
    fun saveNewGameSession(title: String, notes: String = "") {
        val currentFen = _gameState.value.toFen()
        val movesStr = movesHistory.joinToString(",")
        val fensStr = fensHistory.joinToString(";")
        val name = if (title.isBlank()) "Game Match - ${System.currentTimeMillis() % 10000}" else title

        viewModelScope.launch {
            val game = SavedGame(
                title = name,
                notes = notes,
                startingFen = fensHistory.firstOrNull() ?: ChessGameState.STARTING_FEN,
                currentFen = currentFen,
                playedMoves = movesStr,
                fensList = fensStr,
                timestamp = System.currentTimeMillis()
            )
            repository.insertGame(game)
        }
    }

    fun loadSavedGame(game: SavedGame) {
        _activeGameId.value = game.id
        _activeGameTitle.value = game.title
        _activeGameNotes.value = game.notes

        // Load complete move history & positions
        val listFens = game.fensList.split(";")
        val listMoves = if (game.playedMoves.isEmpty()) emptyList() else game.playedMoves.split(",")

        fensHistory = listFens.toMutableList()
        movesHistory = listMoves.toMutableList()
        _movesList.value = ArrayList(movesHistory)

        // Set pointer to the last step saved
        val targetPointer = listFens.size - 1
        _historyIndex.value = targetPointer
        
        if (targetPointer in fensHistory.indices) {
            _gameState.value = ChessGameState.fromFen(fensHistory[targetPointer])
        }

        _boardEditorMode.value = false
        _selectedSquare.value = null
        _validMoves.value = emptyList()

        analyzeCurrentPosition()
    }

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    fun updateActiveGameNotes(notes: String) {
        _activeGameNotes.value = notes
        
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            kotlinx.coroutines.delay(800)
            autoSaveIfActive()
        }
    }

    fun deleteGameSession(id: Int) {
        viewModelScope.launch {
            repository.deleteGameById(id)
            if (_activeGameId.value == id) {
                _activeGameId.value = null
                _activeGameTitle.value = "Unsaved Session"
                _activeGameNotes.value = ""
            }
        }
    }

    fun unloadActiveGame() {
        _activeGameId.value = null
        _activeGameTitle.value = "Unsaved Session"
        _activeGameNotes.value = ""
        resetBoard()
    }

    private fun autoSaveIfActive() {
        val gameId = _activeGameId.value ?: return
        val currentFen = _gameState.value.toFen()
        val movesStr = movesHistory.joinToString(",")
        val fensStr = fensHistory.joinToString(";")
        val startingFen = fensHistory.firstOrNull() ?: ChessGameState.STARTING_FEN

        viewModelScope.launch {
            val game = SavedGame(
                id = gameId,
                title = _activeGameTitle.value,
                notes = _activeGameNotes.value,
                startingFen = startingFen,
                currentFen = currentFen,
                playedMoves = movesStr,
                fensList = fensStr,
                timestamp = System.currentTimeMillis()
            )
            repository.insertGame(game)
        }
    }
}
