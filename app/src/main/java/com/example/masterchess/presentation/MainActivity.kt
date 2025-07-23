package com.example.masterchess.presentation

import android.content.Context
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.Composable
import com.example.masterchess.presentation.theme.MasterChessTheme
import androidx.navigation.compose.*
import androidx.wear.compose.material.Text
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material.Button
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import androidx.wear.compose.material.*
import com.example.masterchess.logic.TapInputManager
import com.example.masterchess.network.convertSANMoveToVibrations
import com.example.masterchess.network.createGame
import com.example.masterchess.network.getGame
import com.example.masterchess.network.playVibrations
import com.example.masterchess.network.sendUserMove
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            MasterChessTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
fun AppNavHost(context: Context = LocalContext.current) {
    val navController = rememberNavController()
    var gameId by remember { mutableStateOf("") }

    NavHost(navController, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen { navController.navigate("color") }
        }

        composable("color") {
            ColorSelectScreen { chosenColor ->
                CoroutineScope(Dispatchers.IO).launch {
                    val response = createGame(chosenColor, context)
                    gameId = response.gameId
                    withContext(Dispatchers.Main) {
                        navController.navigate("game")
                    }
                }
            }
        }

        composable("game") {
            GameScreen(gameId, navController)
        }
    }
}

@Composable
fun WelcomeScreen(onStartClick: () -> Unit) {
    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tap CHESS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Press the start button to begin a new game session",
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Light,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.secondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onStartClick,
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = 100.dp,
                            minHeight = 10.dp,
                        )
                        .height(30.dp)
                        .padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Cyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "New Game",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ColorSelectScreen(onColorChosen: (String) -> Unit) {

    Scaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(all = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Tap CHESS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Select your color",
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Light,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.secondary
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { onColorChosen("white") },
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = 100.dp,
                            minHeight = 10.dp,
                        )
                        .height(30.dp)
                        .padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "White",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onColorChosen("black") },
                    modifier = Modifier
                        .defaultMinSize(
                            minWidth = 100.dp,
                            minHeight = 10.dp,
                        )
                        .height(30.dp)
                        .padding(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Black,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder(
                        borderColor = Color.White, // Set your border color here
                        borderWidth = 1.dp // Adjust border thickness
                    )
                ) {
                    Text(
                        text = "Black",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(gameId: String, navController: NavController) {
    val context = LocalContext.current
    //val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
    //val vibrator = vibratorManager.getDefaultVibrator();
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val scope = rememberCoroutineScope()

    var playerColor by remember { mutableStateOf("white") }
    var moveHistory by remember { mutableStateOf<List<String>>(emptyList()) }
    var livePreview by remember { mutableStateOf("") }
    var tapSequence by remember { mutableStateOf<List<Int>>(emptyList()) }
    var turnStatus by remember { mutableStateOf("loading") } // loading | ready | sending | waiting

    val manager = remember {
        TapInputManager(
            context,
            onMoveReady = { userMove ->
                if (userMove == "RESET") {
                    moveHistory = emptyList()
                    livePreview = ""
                    tapSequence = emptyList()
                    turnStatus = "loading"

                    navController.navigate("welcome") {
                        popUpTo("welcome") { inclusive = true }
                    }

                    return@TapInputManager
                }

                livePreview = ""
                turnStatus = "sending"
                moveHistory = moveHistory + userMove

                scope.launch {
                    try {
                        val stockfishMove = sendUserMove(gameId, userMove)

                        Log.d("GameScreen", "Stockfish replied with: $stockfishMove")

                        turnStatus = "waiting"

                        if (stockfishMove.isNotBlank()) {
                            delay(300)
                            playVibrations(vibrator, convertSANMoveToVibrations(stockfishMove))

                            moveHistory = moveHistory + stockfishMove
                        } else {
                            Log.w("GameScreen", "Empty stockfishMove, fetching full state as fallback")
                            val refreshedGame = getGame(gameId)
                            moveHistory = refreshedGame.moves
                        }

                        delay(200)
                        turnStatus = "ready"
                    } catch (e: Exception) {
                        Log.e("GameScreen", "Error during move processing", e)
                        turnStatus = "ready"
                    }
                }
            },
            onPartialUpdate = { livePreview = it },
            onSequenceUpdate = { tapSequence = it }
        )
    }

    LaunchedEffect(Unit) {
        val game = getGame(gameId)
        playerColor = game.playerColor
        moveHistory = game.moves

        if (playerColor == "black" && game.moves.isNotEmpty()) {
            // Show vibration for Stockfish first move
            val firstMove = game.moves.first()
            playVibrations(vibrator, convertSANMoveToVibrations(firstMove))
        }

        turnStatus = "ready"
    }

    val turnMessage = when (turnStatus) {
        "loading" -> "Loading game..."
        "sending" -> "Sending your move..."
        "waiting" -> "Waiting for Stockfish..."
        else -> "Tap to enter move"
    }

    Scaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(all = 16.dp)
                .pointerInput(turnStatus) {
                    if (turnStatus == "ready") {
                        detectTapGestures(onTap = {
                            manager.registerTap(System.currentTimeMillis())
                        })
                    }
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = buildMoveHistory(moveHistory),
                color = Color.White,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                text = if (livePreview.isNotBlank()) "Input: $livePreview" else turnMessage,
                color = if (livePreview.isNotBlank()) Color.Yellow else Color.LightGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            if (turnStatus == "sending") {
                CircularProgressIndicator(
                    // color = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(bottom = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Text(
                text = "Tap Log: ${tapSequence.joinToString()}",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    manager.clear()
                    livePreview = ""
                    tapSequence = emptyList()
                },
                modifier = Modifier
                    .defaultMinSize(
                        minWidth = 100.dp,
                        minHeight = 10.dp,
                        )
                    .height(30.dp)
                    .padding(horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Cyan,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "Clear",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Black
                )
            }
        }
    }
}

fun buildMoveHistory(moves: List<String>): String {
    val lastMoves = if (moves.size > 2) moves.takeLast(6) else moves
    return lastMoves
        .chunked(2)
        .withIndex()
        .joinToString("\n") { (i, pair) ->
            val white = pair.getOrNull(0) ?: ""
            val black = pair.getOrNull(1) ?: ""
            val moveNumber = if (moves.size > 6) {
                (moves.size / 2) - (2 / 2) + i + 1
            } else {
                i + 1
            }
            "$moveNumber. $white $black"
        }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun WelcomePreview() {
    MasterChessTheme {
        WelcomeScreen(onStartClick = {})
    }
}
