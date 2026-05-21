package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.api.CloudEvalResponse
import com.example.data.database.SavedGame
import com.example.data.model.ChessGameState
import com.example.data.model.ChessPiece
import com.example.data.model.ChessRules
import com.example.data.model.PieceColor
import com.example.data.model.PieceType
import com.example.ui.viewmodel.ChessViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun ChessBoardScreen(
    viewModel: ChessViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val isEditorMode by viewModel.boardEditorMode.collectAsStateWithLifecycle()
    val selectedSquare by viewModel.selectedSquare.collectAsStateWithLifecycle()
    val validMoves by viewModel.validMoves.collectAsStateWithLifecycle()
    
    val evaluation by viewModel.lichessEvaluation.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    
    val localBestMove by viewModel.localBestMove.collectAsStateWithLifecycle()
    val localEvalScore by viewModel.localEvalScore.collectAsStateWithLifecycle()
    
    val savedGames by viewModel.savedGames.collectAsStateWithLifecycle()
    val activeGameId by viewModel.activeGameId.collectAsStateWithLifecycle()
    val activeGameTitle by viewModel.activeGameTitle.collectAsStateWithLifecycle()
    val activeGameNotes by viewModel.activeGameNotes.collectAsStateWithLifecycle()
    
    val historyIndex by viewModel.historyIndex.collectAsStateWithLifecycle()
    val movesList by viewModel.movesList.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0: Analysis, 1: Saved History DB
    var showFenDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    // Board editor selected tool/piece
    var editorSelectedPiece by remember { mutableStateOf<ChessPiece?>(ChessPiece(PieceColor.WHITE, PieceType.PAWN)) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(0.dp))
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analysis") },
                    label = { Text("Analysis Board") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, contentDescription = "History shelf") },
                    label = { Text("Saved Sessions") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header bar in Immersive UI: px-4 py-3 bg-[#252429] border-b border-[#313033]
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chess Analyzer",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = if (activeGameId != null) "Active Study: $activeGameTitle" else "Position Setup & Analysis Mode",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = { showFenDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Terminal, contentDescription = "Load FEN", tint = MaterialTheme.colorScheme.primary)
                        }

                        IconButton(
                            onClick = { showSaveDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save active state", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        if (activeGameId != null) {
                            IconButton(
                                onClick = { viewModel.unloadActiveGame() },
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF3E1F21))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Exit game file", tint = Color(0xFFF96666))
                            }
                        }
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }

            AnimatedContent(
                targetState = selectedTab,
                label = "TabTransition",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    0 -> {
                        // Analysis dashboard
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Turn Banner / Quick Actions using Immersive Card rounded-2xl
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
                                            .background(
                                                if (gameState.activeColor == PieceColor.WHITE) Color.White else Color.Black
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (gameState.activeColor == PieceColor.WHITE) "White to Move" else "Black to Move",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = { viewModel.toggleBoardEditorMode() },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (isEditorMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isEditorMode) Icons.Default.Check else Icons.Default.Edit,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEditorMode) "Done Setup" else "Setup Board", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { viewModel.resetBoard() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Restart standard",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom interactive board
                            ChessBoardView(
                                gameState = gameState,
                                selectedSquare = selectedSquare,
                                validMoves = validMoves,
                                isEditorMode = isEditorMode,
                                editorSelectedPiece = editorSelectedPiece,
                                onSquareClick = { idx ->
                                    if (isEditorMode) {
                                        viewModel.modifySquareInEditor(idx, editorSelectedPiece)
                                    } else {
                                        viewModel.selectSquare(idx)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Piece placement palette if configured
                            if (isEditorMode) {
                                BoardEditorPalette(
                                    selectedPiece = editorSelectedPiece,
                                    onSelectPiece = { editorSelectedPiece = it },
                                    activeColor = gameState.activeColor,
                                    onChangeTurnColor = { viewModel.setEditorTurn(it) },
                                    onClearBoard = { viewModel.clearBoard() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // Steppers & History Controller
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.stepHistory(-1) },
                                        enabled = historyIndex > 0,
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Step Back", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Undo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { viewModel.stepHistory(1) },
                                        enabled = historyIndex < (movesList.size),
                                        shape = RoundedCornerShape(50),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Redo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Step Forward", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Text(
                                    text = "Move ${historyIndex}/${movesList.size}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Stockfish Analysis Feed panel
                            StockfishAnalysisPanel(
                                evaluation = evaluation,
                                isAnalyzing = isAnalyzing,
                                localEvalScore = localEvalScore,
                                localBestMove = localBestMove,
                                activeColor = gameState.activeColor,
                                movesList = movesList,
                                currentHistoryIndex = historyIndex,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            )
                            
                            // Game notes manager if a save game is active
                            if (activeGameId != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E262E))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Study Notes / Self-Analysis:",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = activeGameNotes,
                                            onValueChange = { viewModel.updateActiveGameNotes(it) },
                                            placeholder = { Text("Write down key variations, blunder records or setup plans here...", color = Color.Gray, fontSize = 13.sp) },
                                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                            minLines = 3,
                                            maxLines = 5,
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF5CC2F2),
                                                unfocusedBorderColor = Color(0xFF34404C)
                                            )
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                    1 -> {
                        // Saved shelf view
                        SavedGamesDBView(
                            savedGames = savedGames,
                            onLoadGame = { game ->
                                viewModel.loadSavedGame(game)
                                selectedTab = 0 // jump back to screen
                            },
                            onDeleteGame = { id ->
                                viewModel.deleteGameSession(id)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    // FEN Importer / Exporter Dialog
    if (showFenDialog) {
        val clipboardManager = LocalClipboardManager.current
        var inputFen by remember { mutableStateOf(gameState.toFen()) }

        Dialog(onDismissRequest = { showFenDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E262E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Forsyth-Edwards (FEN) Parser",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Paste FEN text from Lichess or Chess.com state to load analysis board position instantly:",
                        fontSize = 12.sp,
                        color = Color(0xFF8C9BA5)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputFen,
                        onValueChange = { inputFen = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5CC2F2),
                            unfocusedBorderColor = Color(0xFF34404C)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val current = gameState.toFen()
                                clipboardManager.setText(AnnotatedString(current))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Copy Active")
                        }

                        Button(
                            onClick = {
                                if (inputFen.isNotBlank()) {
                                    viewModel.loadFen(inputFen.trim())
                                    showFenDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5CC2F2)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Load Board")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showFenDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    // Save Game Dialog
    if (showSaveDialog) {
        var inputTitle by remember { mutableStateOf(if (activeGameId != null) activeGameTitle else "Study Session #${System.currentTimeMillis() % 1000}") }
        var inputNotes by remember { mutableStateOf(if (activeGameId != null) activeGameNotes else "") }

        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E262E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Text(
                        text = "Save Analysis Study",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = inputTitle,
                        onValueChange = { inputTitle = it },
                        label = { Text("Game Title / Variation Name", color = Color.Gray) },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5CC2F2),
                            focusedLabelColor = Color(0xFF5CC2F2)
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputNotes,
                        onValueChange = { inputNotes = it },
                        label = { Text("Study Goals / OpenersNotes", color = Color.Gray) },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF5CC2F2),
                            focusedLabelColor = Color(0xFF5CC2F2)
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (inputTitle.isNotBlank()) {
                                viewModel.saveNewGameSession(inputTitle, inputNotes)
                                showSaveDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4EE4A2)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Store Board File", color = Color(0xFF0D1114), fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(
                        onClick = { showSaveDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = Color.Gray)
                    }
                }
            }
        }
    }
}

// 8x8 ChessBoard rendering view
@Composable
fun ChessBoardView(
    gameState: ChessGameState,
    selectedSquare: Int?,
    validMoves: List<Int>,
    isEditorMode: Boolean,
    editorSelectedPiece: ChessPiece?,
    onSquareClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Light and Dark Square theme configuration
    val lightSquareColor = Color(0xFFEBECD0)
    val darkSquareColor = Color(0xFF779556)

    Column(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .border(1.5.dp, Color(0xFF444746), RoundedCornerShape(8.dp))
            .background(Color(0xFF1C1B1F))
    ) {
        for (row in 0..7) {
            Row(modifier = Modifier.weight(1f)) {
                for (col in 0..7) {
                    val index = row * 8 + col
                    val isDarkSquare = (row + col) % 2 != 0
                    
                    val piece = gameState.board[index]
                    val isSelected = selectedSquare == index
                    val isValidTarget = validMoves.contains(index)

                    val baseColor = if (isDarkSquare) darkSquareColor else lightSquareColor

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                when {
                                    isValidTarget && piece != null -> Color(0xFFFF5252).copy(alpha = 0.85f) // Capturable highlighted
                                    else -> baseColor
                                }
                            )
                            .clickable { onSquareClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Yellow select overlay matching relative / absolute yellow-400/40 design
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFFFEB3B).copy(alpha = 0.45f))
                            )
                        }
                        
                        // 1. Draw Board Coordinates (e.g. 'a8' bottom, '1-8' ranks left)
                        if (col == 0) {
                            Text(
                                text = (8 - row).toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkSquare) lightSquareColor.copy(alpha = 0.5f) else darkSquareColor.copy(
                                    alpha = 0.6f
                                ),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            )
                        }
                        if (row == 7) {
                            Text(
                                text = ('a' + col).toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkSquare) lightSquareColor.copy(alpha = 0.5f) else darkSquareColor.copy(
                                    alpha = 0.6f
                                ),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(horizontal = 3.dp, vertical = 2.dp)
                            )
                        }

                        // 2. Draw piece or target indicators
                        if (isValidTarget && piece == null) {
                            // Empty target dot
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2E7D32).copy(alpha = 0.60f))
                            )
                        }

                        if (piece != null) {
                            // Beautiful unicode pieces styled with distinct fills/strokes
                            val pieceColor = if (piece.color == PieceColor.WHITE) Color.White else Color(0xFF1E1E1E)
                            
                            val textShadow = if (piece.color == PieceColor.WHITE) {
                                Shadow(
                                    color = Color.Black.copy(alpha = 0.7f),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 3f
                                )
                            } else {
                                Shadow(
                                    color = Color.White.copy(alpha = 0.25f),
                                    offset = Offset(-1.5f, -1.5f),
                                    blurRadius = 2f
                                )
                            }

                            Text(
                                text = when (piece.type) {
                                    PieceType.KING -> "\u265A"
                                    PieceType.QUEEN -> "\u265B"
                                    PieceType.ROOK -> "\u265C"
                                    PieceType.BISHOP -> "\u265D"
                                    PieceType.KNIGHT -> "\u265E"
                                    PieceType.PAWN -> "\u265F"
                                },
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Normal,
                                color = pieceColor,
                                style = TextStyle(shadow = textShadow),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom palette block for painting custom board positions (Board Editor)
@Composable
fun BoardEditorPalette(
    selectedPiece: ChessPiece?,
    onSelectPiece: (ChessPiece?) -> Unit,
    activeColor: PieceColor,
    onChangeTurnColor: (PieceColor) -> Unit,
    onClearBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E262E))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Setup Position Palette (Tap a piece to paint onto the board):",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF8C9BA5)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // White pieces row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("White:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                PieceType.values().forEach { type ->
                    val item = ChessPiece(PieceColor.WHITE, type)
                    val isSelected = selectedPiece?.color == PieceColor.WHITE && selectedPiece.type == type
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF5CC2F2).copy(alpha = 0.35f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF5CC2F2) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectPiece(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (type) {
                                PieceType.KING -> "\u265A"
                                PieceType.QUEEN -> "\u265B"
                                PieceType.ROOK -> "\u265C"
                                PieceType.BISHOP -> "\u265D"
                                PieceType.KNIGHT -> "\u265E"
                                PieceType.PAWN -> "\u265F"
                            },
                            fontSize = 28.sp,
                            color = Color.White,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.Black,
                                    offset = Offset(1f, 1f),
                                    blurRadius = 2f
                                )
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Black pieces row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Black:", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp))
                PieceType.values().forEach { type ->
                    val item = ChessPiece(PieceColor.BLACK, type)
                    val isSelected = selectedPiece?.color == PieceColor.BLACK && selectedPiece.type == type
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color(0xFF5CC2F2).copy(alpha = 0.35f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF5CC2F2) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onSelectPiece(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (type) {
                                PieceType.KING -> "\u265A"
                                PieceType.QUEEN -> "\u265B"
                                PieceType.ROOK -> "\u265C"
                                PieceType.BISHOP -> "\u265D"
                                PieceType.KNIGHT -> "\u265E"
                                PieceType.PAWN -> "\u265F"
                            },
                            fontSize = 28.sp,
                            color = Color(0xFF1F1F1F),
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color.White.copy(alpha = 0.4f),
                                    offset = Offset(-1f, -1f),
                                    blurRadius = 1f
                                )
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Control Actions turn & clearing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Eraser tool
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedPiece == null) Color(0xFFFF5252).copy(alpha = 0.25f) else Color(0xFF2A343D))
                        .border(
                            1.dp,
                            if (selectedPiece == null) Color(0xFFFF5252) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectPiece(null) }
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Eraser tool", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eraser tool", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Active player turn editor setting
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val activeClassW = if (activeColor == PieceColor.WHITE) ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black) else ButtonDefaults.filledTonalButtonColors()
                    val activeClassB = if (activeColor == PieceColor.BLACK) ButtonDefaults.buttonColors(containerColor = Color(0xFF1E1E1E), contentColor = Color.White) else ButtonDefaults.filledTonalButtonColors()
                    
                    Button(
                        onClick = { onChangeTurnColor(PieceColor.WHITE) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = activeClassW,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("w to play", fontSize = 10.sp)
                    }
                    Button(
                        onClick = { onChangeTurnColor(PieceColor.BLACK) },
                        modifier = Modifier.height(30.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = activeClassB,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("b to play", fontSize = 10.sp)
                    }
                }

                TextButton(
                    onClick = onClearBoard,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF5252))
                ) {
                    Text("Clear Board", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Beautiful stockfish reporting layout panel (Network + On-Device hybrid)
@Composable
fun StockfishAnalysisPanel(
    evaluation: CloudEvalResponse?,
    isAnalyzing: Boolean,
    localEvalScore: Int,
    localBestMove: Pair<Int, Int>?,
    activeColor: PieceColor,
    movesList: List<String>,
    currentHistoryIndex: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Network vs Fallback Engine
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isAnalyzing) Color(0xFFFFCC00) else MaterialTheme.colorScheme.tertiary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAnalyzing) "Querying Cloud Engine..." else "Stockfish Cloud Synced",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Stockfish 16.1",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isAnalyzing && evaluation == null) {
                // Large styled linear loading bar
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            } else {
                val hasMatch = evaluation != null && !evaluation.isNotFound && evaluation.error == null
                
                if (hasMatch && evaluation != null) {
                    // We found an exact cloud evaluation database match!
                    val primaryPv = evaluation.pvs.firstOrNull()
                    val scoreText = primaryPv?.getEvalString() ?: "0.00"
                    val bestMoveLabel = primaryPv?.moves?.split(" ")?.firstOrNull() ?: "None"
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Best Move
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "BEST MOVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = bestMoveLabel,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${evaluation.depth} plies depth",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Card 2: Evaluation
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "ACCURACY SCORE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = scoreText,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (scoreText.startsWith("-")) Color(0xFFFF5252) else MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (scoreText.startsWith("-")) "Black advantage" else "White advantage",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (primaryPv != null) {
                        Text(
                            text = "Recommended Line:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Psychology, 
                                contentDescription = "Recommended Best Line", 
                                tint = MaterialTheme.colorScheme.primary, 
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = primaryPv.getFormattedMoves(5),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else {
                    // fall back to offline minimax generated engine logic! Shows the user is never stuck!
                    val score = localEvalScore / 100.0
                    val formatted = String.format("%.2f", score)
                    val prefix = if (score > 0) "+$formatted" else formatted

                    val localBestText = if (localBestMove != null) {
                        "${ChessRules.getSquareName(localBestMove.first)} → ${ChessRules.getSquareName(localBestMove.second)}"
                    } else "No moves found"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1: Best Move
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "LOCAL MOVE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = localBestText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "On-Device Minimax",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Card 2: Evaluation
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "EVALUATION SCORE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = prefix,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (score < 0) Color(0xFFFF5252) else MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (score == 0.0) "Centered draw" else if (score > 0) "White favored" else "Black favored",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Suggested Best Calculation Move:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                            .padding(11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Memory, 
                            contentDescription = "Calculated", 
                            tint = Color(0xFFFFCC00), 
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = localBestText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Interactive moves registry list display
            if (movesList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outline)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "History moves played:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                // horizontal wrapping moves list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    movesList.takeLast(6).forEachIndexed { index, move ->
                        val globalIdx = movesList.size - movesList.takeLast(6).size + index + 1
                        val isHighlighted = globalIdx == currentHistoryIndex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$globalIdx. $move",
                                fontSize = 11.sp,
                                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// Bottom shelf for persistent DB list items
@Composable
fun SavedGamesDBView(
    savedGames: List<SavedGame>,
    onLoadGame: (SavedGame) -> Unit,
    onDeleteGame: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (savedGames.isEmpty()) {
        Box(
            modifier = modifier.padding(24.dp).fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No Saved Positions Found",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Go to the Analysis screen and hit the folder 'Save' action to archive match configurations and opening lines.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(savedGames) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLoadGame(game) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (game.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = game.notes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            
                            val df = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            Text(
                                text = "Modified: ${df.format(Date(game.timestamp))}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onDeleteGame(game.id) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete saved study",
                                    tint = Color(0xFFFF5252)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
