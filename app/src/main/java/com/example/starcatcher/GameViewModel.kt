package com.example.starcatcher

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/** Holds all game state so it survives screen rotation. */
class GameViewModel : ViewModel() {
    companion object {
        const val START_LIVES = 3
    }

    var playerName: String = ""
    var resultsShown = false
    var moonTaps = 0

    val score = MutableLiveData(0)
    val lives = MutableLiveData(START_LIVES)
    val gameOver = MutableLiveData(false)

    fun addScore(points: Int) {
        if (gameOver.value == true) return
        score.value = (score.value ?: 0) + points
    }

    fun loseLife() {
        if (gameOver.value == true) return
        val left = (lives.value ?: 0) - 1
        lives.value = left.coerceAtLeast(0)
        if (left <= 0) gameOver.value = true
    }

    fun reset() {
        score.value = 0
        lives.value = START_LIVES
        gameOver.value = false
        resultsShown = false
        moonTaps = 0
    }

    /** Stars fall faster as the score rises. */
    fun fallDurationMs(): Long = (4000 - (score.value ?: 0) * 40L).coerceAtLeast(1500L)

    /** Stars spawn more often as the score rises. */
    fun spawnIntervalMs(): Long = (1200 - (score.value ?: 0) * 12L).coerceAtLeast(500L)
}
