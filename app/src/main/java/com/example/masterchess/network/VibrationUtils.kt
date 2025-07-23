package com.example.masterchess.network

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

val FILE_VIBRATIONS = mapOf(
    'a' to 1, 'b' to 2, 'c' to 3, 'd' to 4,
    'e' to 5, 'f' to 6, 'g' to 7, 'h' to 8
)
val RANK_VIBRATIONS = mapOf(
    '1' to 1, '2' to 2, '3' to 3, '4' to 4,
    '5' to 5, '6' to 6, '7' to 7, '8' to 8
)
val PIECE_VIBRATIONS = mapOf(
    'p' to 1, 'n' to 2, 'b' to 3, 'r' to 4,
    'q' to 5, 'k' to 6
)

fun convertSANMoveToVibrations(moveRaw: String): List<Long> {
    val move = moveRaw.replace("[x:+#]".toRegex(), "")
    val pattern = mutableListOf<Long>()
    val pawnMoveRegex = Regex("^[a-h][1-8]$")
    val pawnTakeRegex = Regex("^[a-h]x[a-h][1-8]$")
    val normalPieceRegex = Regex("^[NBRQK][a-h][1-8]$")
    val disambiguateRegex = Regex("^[NBRQK][a-h][1-8][a-h][1-8]$")

    // Helper function to add a segment with pauses
    fun addSegment(count: Int) {
        if (pattern.isNotEmpty()) {
            // Add pause between segments
            pattern += 4000L  // Pause duration
            pattern += 0L      // Zero vibration for pause
        }
        pattern.addAll(vibrateCount(count))
    }

    // 1. RESET
    if (move == "RESET") {
        pattern.addAll(vibrateCount(15))
        return pattern
    }
    // 2. Castling
    if (move == "O-O") {
        pattern.addAll(vibrateCount(8))
        return pattern
    }
    if (move == "O-O-O") {
        pattern.addAll(vibrateCount(9))
        return pattern
    }
    // 3. Promotion (e.g. e8=Q)
    if (move.contains("=")) {
        val file = move[0]
        val rank = move[1]
        val promoPiece = move.last().lowercaseChar()

        addSegment(10)
        addSegment(FILE_VIBRATIONS[file] ?: 1)
        addSegment(RANK_VIBRATIONS[rank] ?: 1)
        addSegment(PIECE_VIBRATIONS[promoPiece] ?: 1)
        return pattern
    }
    // 4. Special disambiguation move (e.g. Neg3 → Ne4g3)
    if (disambiguateRegex.matches(move)) {
        val piece = move[0].lowercaseChar()
        val fromFile = move[1]
        val fromRank = move[2]
        val toFile = move[3]
        val toRank = move[4]

        addSegment(7)
        addSegment(PIECE_VIBRATIONS[piece] ?: 1)
        addSegment(FILE_VIBRATIONS[fromFile] ?: 1)
        addSegment(RANK_VIBRATIONS[fromRank] ?: 1)
        addSegment(FILE_VIBRATIONS[toFile] ?: 1)
        addSegment(RANK_VIBRATIONS[toRank] ?: 1)

        return pattern
    }
    // 5. Pawn capture (e.g. exd5 → convert to e4d5)
    if (pawnTakeRegex.matches(moveRaw)) {
        val fromFile = moveRaw[0]
        val toFile = moveRaw[2]
        val toRank = moveRaw[3]

        addSegment(1) // pawn
        addSegment(FILE_VIBRATIONS[fromFile] ?: 1)
        addSegment(FILE_VIBRATIONS[toFile] ?: 1)
        addSegment(RANK_VIBRATIONS[toRank] ?: 1)
        return pattern
    }
    // 6. Normal piece move (e.g. Nf3, Rb5, Qd2)
    if (normalPieceRegex.matches(move)) {
        val piece = move[0].lowercaseChar()
        val file = move[1]
        val rank = move[2]

        addSegment(PIECE_VIBRATIONS[piece] ?: 1)
        addSegment(FILE_VIBRATIONS[file] ?: 1)
        addSegment(RANK_VIBRATIONS[rank] ?: 1)

        return pattern
    }
    // 7. Pawn move (e.g. e4)
    if (pawnMoveRegex.matches(move)) {
        val file = move[0]
        val rank = move[1]

        addSegment(PIECE_VIBRATIONS['p'] ?: 1)
        addSegment(FILE_VIBRATIONS[file] ?: 1)
        addSegment(RANK_VIBRATIONS[rank] ?: 1)

        return pattern
    }

    // fallback (error)
    return emptyList()
}
fun vibrateCount(count: Int): List<Long> {
    val pattern = mutableListOf<Long>()
    pattern += 0L // start immediately
    repeat(count) {
        pattern += 100L // vibrate
        if (it < count - 1) {
            pattern += 300L // pause between vibrations
        }
    }
    return pattern
}
fun playVibrations(vibrator: Vibrator?, pattern: List<Long>) {
    val timings = pattern.toLongArray()
    Log.d("VIBRATION", "Vibration Pattern: ${pattern.joinToString()} (len=${timings.size})")
    vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
}