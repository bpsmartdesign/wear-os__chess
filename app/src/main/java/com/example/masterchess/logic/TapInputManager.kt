package com.example.masterchess.logic

import android.content.Context
import android.os.VibrationEffect
import com.example.masterchess.network.FILE_VIBRATIONS
import com.example.masterchess.network.RANK_VIBRATIONS
import kotlinx.coroutines.*
import android.os.Vibrator

class TapInputManager(
    context: Context,
    private val onMoveReady: (String) -> Unit,
    private val onPartialUpdate: (String) -> Unit = {},
    private val onSequenceUpdate: (List<Int>) -> Unit = {}
) {
    private var tapBuffer = mutableListOf<Long>()
    private var lastTapTime = 0L
    private val sequence = mutableListOf<Int>()
    private var idleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    fun registerTap(timestamp: Long) {
        val now = timestamp
        val diff = now - lastTapTime

        if (diff < 1000) {
            tapBuffer.add(now)
        } else {
            processTapGroup()
            tapBuffer.clear()
            tapBuffer.add(now)
        }

        lastTapTime = now

        // Restart idle timer
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(10_000)
            finalizeInput()
        }

        scope.launch {
            delay(4000)
            if (lastTapTime == now) {
                processTapGroup()
            }
        }
    }
    fun clear() {
        tapBuffer.clear()
        sequence.clear()
        lastTapTime = 0L
        idleJob?.cancel()
        onPartialUpdate("")
        onSequenceUpdate(emptyList())
    }
    fun onVibrate(count: Int) {
        val pattern = mutableListOf<Long>()
        pattern += 0L
        repeat(count) {
            pattern += 100L
            pattern += 150L
        }
        vibrator.vibrate(VibrationEffect.createWaveform(pattern.toLongArray(), -1))
    }
    private fun processTapGroup() {
        if (tapBuffer.isEmpty()) return
        val count = tapBuffer.size
        sequence.add(count)
        tapBuffer.clear()
        onSequenceUpdate(sequence.toList())

        onVibrate(1)
    }
    private fun finalizeInput() {
        val move = mapToMove(sequence.toList())
        if (move != null) {
            onMoveReady(move)
        }

        sequence.clear()
        onPartialUpdate("")
        onSequenceUpdate(emptyList())

        onVibrate(2)
    }
    private fun mapToMove(seq: List<Int>): String? {
        if (seq.isEmpty()) return null

        // 1. Reset signal
        if (seq.size == 1 && seq[0] == 15) {
            onPartialUpdate("RESET") //! Check this
            return "RESET"
        }
        // 2. Castle
        if (seq.size == 1) {
            val move = if (seq[0] == 8) "O-O" else if (seq[0] == 9) "O-O-O" else null
            move?.let { onPartialUpdate(it) }
            return move
        }
        // 3. Special disambiguation: 7 + from + to (e.g. Ne4g3)
        if (seq.first() == 7 && seq.size == 6) {
            val piece = when (seq[1]) {
                2 -> "N"
                3 -> "B"
                4 -> "R"
                5 -> "Q"
                6 -> "K"
                else -> return null
            }
            val fromFile = FILE_VIBRATIONS.entries.find { it.value == seq[2] }?.key ?: return null
            val fromRank = RANK_VIBRATIONS.entries.find { it.value == seq[3] }?.key ?: return null
            val toFile = FILE_VIBRATIONS.entries.find { it.value == seq[4] }?.key ?: return null
            val toRank = RANK_VIBRATIONS.entries.find { it.value == seq[5] }?.key ?: return null
            val move = "$piece$fromFile$toFile$toRank"
            onPartialUpdate("$piece$fromFile$fromRank$toFile$toRank")
            return move
        }
        // 4. Promotion (10 + piece)
        if (seq.size == 4 && seq[0] == 10) {
            val pieceChar = when (seq[3]) {
                2 -> "N"
                3 -> "B"
                4 -> "R"
                5 -> "Q"
                6 -> "K"
                else -> return null
            }
            val file = FILE_VIBRATIONS.entries.find { it.value == seq[1] }?.key ?: return null
            val rank = RANK_VIBRATIONS.entries.find { it.value == seq[2] }?.key ?: return null

            val move = "${file}${rank}=${pieceChar}"
            onPartialUpdate(move)
            return move
        }
        // 5. Pawn capture (e.g. exd5 → e4d5)
        if (seq.size == 5 && seq[0] == 1) {
            val fromFile = FILE_VIBRATIONS.entries.find { it.value == seq[1] }?.key ?: return null
            val fromRank = RANK_VIBRATIONS.entries.find { it.value == seq[2] }?.key ?: return null
            val toFile = FILE_VIBRATIONS.entries.find { it.value == seq[3] }?.key ?: return null
            val toRank = RANK_VIBRATIONS.entries.find { it.value == seq[4] }?.key ?: return null
            val move = "${fromFile}x${toFile}$toRank"
            onPartialUpdate("$fromFile$fromRank$toFile$toRank")
            return move
        }
        // 6. Normal move
        if (seq.size == 3) {
            val piece = when (seq[0]) {
                1 -> ""
                2 -> "N"
                3 -> "B"
                4 -> "R"
                5 -> "Q"
                6 -> "K"
                else -> return null
            }
            val file = FILE_VIBRATIONS.entries.find { it.value == seq[1] }?.key ?: return null
            val rank = RANK_VIBRATIONS.entries.find { it.value == seq[2] }?.key ?: return null
            val move = "$piece$file$rank"
            onPartialUpdate(move)
            return move
        }

        return null
    }
}

