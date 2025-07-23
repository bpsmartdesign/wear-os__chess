package com.example.masterchess.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://stockfish-server-2ik3.onrender.com"

val client = HttpClient(OkHttp) {
    // Content negotiation for JSON
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            explicitNulls = false  // Optional: skip null values in serialization
        })
    }

    // Timeout configuration
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000L  // 15 seconds to establish connection
        socketTimeoutMillis = 30_000L  // 30 seconds for complete request
        requestTimeoutMillis = 45_000L  // Total timeout including retries
    }

    // Retry policy
    install(HttpRequestRetry) {
        maxRetries = 3
        retryOnExceptionIf { request, cause ->
            cause is SocketTimeoutException || cause is ConnectTimeoutException
        }
        exponentialDelay()  // Exponential backoff (default)
    }
}

@Serializable
data class GameRequest(
    val playerColor: String = "white",
    val level: Int = 5
)
@Serializable
data class GameResponse(
    val gameId: String,
    val fen: String,
    val playerColor: String
)
@Serializable
data class MoveRequest(
    val move: String
)
@Serializable
data class MoveResponse(
    val fen: String,
    val moves: List<String>,
    val stockfishReply: String
)
@Serializable
data class GameStatusResponse(
    val status: String,
    val fen: String,
    val moves: List<String>,
    val lastMoveAt: String,
    val playerColor: String
)

suspend fun createGame(color: String, context: android.content.Context): GameResponse {
    return client.post("$BASE_URL/api/games") {
        contentType(ContentType.Application.Json)
        setBody(GameRequest(playerColor = color, level = 18))
    }.body()
}
suspend fun sendUserMove(gameId: String, move: String): String {
    if (move == "RESET" || move.isBlank()) return ""

    val response: MoveResponse = client.post("$BASE_URL/api/games/$gameId/moves") {
        contentType(ContentType.Application.Json)
        setBody(MoveRequest(move))
    }.body()

    return response.stockfishReply
}
suspend fun getGame(gameId: String): GameStatusResponse {
    return client.get("$BASE_URL/api/games/$gameId")
        .body()
}
