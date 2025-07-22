package com.example.masterchess.network

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.*

@Serializable
data class GameRequest(val playerColor: String, val level: Int)
@Serializable
data class MoveRequest(val move: String)
@Serializable
data class GameResponse(val gameId: String, val fen: String, val playerColor: String)
@Serializable
data class MoveResponse(
    val fen: String,
    val moves: List<String>,
    val stockfishReply: String?
)
@Serializable
data class GamePlayResponse(val move: String)
@Serializable
data class GameStatusResponse(
    val status: String,
    val fen: String,
    val moves: List<String> = emptyList(),
    val lastMoveAt: String,
    val playerColor: String
)

val client = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json()
    }
}
val Context.dataStore by preferencesDataStore("master chess")
val GAME_ID_KEY = stringPreferencesKey("game_id")

suspend fun createGame(color: String, context: Context): GameResponse {
    val response: GameResponse = client.post("http://10.0.2.2:3000/api/games") {
        contentType(ContentType.Application.Json)
        setBody(GameRequest(playerColor = color, level = 18))
    }.body()

    // Save to local storage
    context.dataStore.edit { prefs ->
        prefs[GAME_ID_KEY] = response.gameId
    }

    return response
}
suspend fun getGame(gameId: String): GameStatusResponse {
    return client.get("http://10.0.2.2:3000/api/games/$gameId").body()
}
suspend fun sendUserMove(gameId: String, move: String): String {
    val response: MoveResponse = client.post("http://10.0.2.2:3000/api/games/$gameId/moves") {
        contentType(ContentType.Application.Json)
        setBody(MoveRequest(move))
    }.body()

    return response.stockfishReply ?: ""
}
