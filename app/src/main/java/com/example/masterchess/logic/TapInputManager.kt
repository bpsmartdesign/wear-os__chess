package com.example.masterchess.logic

import com.example.masterchess.network.FILE_VIBRATIONS
import com.example.masterchess.network.RANK_VIBRATIONS
import kotlinx.coroutines.*

class TapInputManager(
    private val onMoveReady: (String) -> Unit,
    private val onPartialUpdate: (String) -> Unit = {},
    private val onSequenceUpdate: (List<Int>) -> Unit = {}
) {
    private var tapBuffer = mutableListOf<Long>()
    private var lastTapTime = 0L
    private val sequence = mutableListOf<Int>()
    private var idleJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

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

    private fun processTapGroup() {
        if (tapBuffer.isEmpty()) return
        val count = tapBuffer.size
        sequence.add(count)
        tapBuffer.clear()
        onSequenceUpdate(sequence.toList())
    }

    private fun finalizeInput() {
        val move = mapToMove(sequence.toList())
        if (move != null) {
            onMoveReady(move)
        }

        sequence.clear()
        onPartialUpdate("")
        onSequenceUpdate(emptyList())
    }

    private fun mapToMove(seq: List<Int>): String? {
        if (seq.isEmpty()) return null

        return if (seq.first() == 7 && seq.size == 5) {
            // Disambiguated move
            val fromFile = FILE_VIBRATIONS.entries.find { it.value == seq[1] }?.key ?: return null
            val fromRank = RANK_VIBRATIONS.entries.find { it.value == seq[2] }?.key ?: return null
            val toFile = FILE_VIBRATIONS.entries.find { it.value == seq[3] }?.key ?: return null
            val toRank = RANK_VIBRATIONS.entries.find { it.value == seq[4] }?.key ?: return null
            val move = "$fromFile$fromRank$toFile$toRank" // e.g. g1f3
            onPartialUpdate(move)
            move
        } else if (seq.size == 3) {
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
            move
        } else if(seq.size == 1) {
            val move = if (seq[0] == 8) "O-O" else "O-O-O"
            onPartialUpdate(move)
            move
        } else {
            null // Invalid
        }
    }
}

