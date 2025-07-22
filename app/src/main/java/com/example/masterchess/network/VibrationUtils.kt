package com.example.masterchess.network

import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log

// ADD PROMOTE VIBRATION / TAP SEQUENCE (10) FOLLOWED BY PIECE
// HANDLE TAKING WITH PROMOTION
// ADD RESET GAME / TAP SEQUENCE (15)
// PAWN TAKING (5 TAPS SEQUENCE / 2 SEQUENCE VIBRATION) UNLESS SPECIAL MOVE
// SPECIAL MOVE SHOULD BE REVIEWED (7 XY_DEP XY_DEST) HANDLE  VIBRATION SEQUENCE THE SAME WAY
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
    val move = moveRaw.replace("[x:+#]".toRegex(), "") // strip captures/checks
    val pattern = mutableListOf<Long>()

    if (move == "O-O") {
        return vibrateCount(8)
    } else if (move == "O-O-O") {
        return vibrateCount(9)
    }

    val pieceChar = if (move.first().isUpperCase() && move.first() != 'O') move.first().lowercaseChar() else 'p'
    val pureMove = if (pieceChar == 'p') move else move.drop(1)

    // Disambiguation: starts with file or rank
    var disambiguationFile: Char? = null
    var disambiguationRank: Char? = null
    var toFile: Char? = null
    var toRank: Char? = null

    when (pureMove.length) {
        2 -> { // e.g. e4
            toFile = pureMove[0]
            toRank = pureMove[1]
        }
        3 -> { // disambiguation file or rank + target
            if (pureMove[0] in 'a'..'h') disambiguationFile = pureMove[0]
            if (pureMove[0] in '1'..'8') disambiguationRank = pureMove[0]
            toFile = pureMove[1]
            toRank = pureMove[2]
        }
        4 -> { // disambiguation file + rank + target
            disambiguationFile = pureMove[0]
            disambiguationRank = pureMove[1]
            toFile = pureMove[2]
            toRank = pureMove[3]
        }
        else -> return emptyList()
    }

    // Add disambiguation if needed
    if (disambiguationFile != null || disambiguationRank != null) {
        pattern += vibrateCount(7) // disambiguation marker
        disambiguationFile?.let { pattern += vibrateCount(FILE_VIBRATIONS[it] ?: 1) }
        disambiguationRank?.let { pattern += vibrateCount(RANK_VIBRATIONS[it] ?: 1) }
        pattern += longPause()
    }

    // Piece type
    pattern += vibrateCount(PIECE_VIBRATIONS[pieceChar] ?: 1)
    pattern += longPause()

    // To square
    toFile?.let { pattern += vibrateCount(FILE_VIBRATIONS[it] ?: 1) }
    pattern += longPause()
    toRank?.let { pattern += vibrateCount(RANK_VIBRATIONS[it] ?: 1) }

    return pattern
}

fun vibrateCount(count: Int): List<Long> {
    return List(count * 2) { i -> if (i % 2 == 0) 500L else 100L } // ON/OFF
}

fun longPause(): List<Long> = listOf(4000L)

fun playVibrations(vibrator: Vibrator?, pattern: List<Long>) {
    val timings = pattern.toLongArray()
    Log.d("VIBRATION", "Pattern: ${pattern.joinToString()}")
    vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
}
