package com.example.starcatcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "extra_name"
    }

    private val vm: GameViewModel by viewModels()
    private lateinit var gameView: GameView
    private lateinit var hudText: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val resultsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val playAgain = result.resultCode == RESULT_OK &&
                result.data?.getBooleanExtra(ResultsActivity.EXTRA_PLAY_AGAIN, false) == true
            if (playAgain) vm.reset() else finish()
        }

    private val spawner = object : Runnable {
        override fun run() {
            if (vm.gameOver.value == true) return
            gameView.fallDurationMs = vm.fallDurationMs()
            gameView.spawnStar(Random.nextFloat() < 0.15f)
            handler.postDelayed(this, vm.spawnIntervalMs())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        setSupportActionBar(findViewById(R.id.toolbar))

        if (vm.playerName.isEmpty()) {
            vm.playerName = intent.getStringExtra(EXTRA_NAME) ?: getString(R.string.default_name)
        }

        gameView = findViewById(R.id.gameView)
        hudText = findViewById(R.id.hudText)

        gameView.onStarCaught = { points -> vm.addScore(points) }
        gameView.onStarMissed = { vm.loseLife() }
        gameView.onMoonTapped = { onMoonTapped() }

        vm.score.observe(this) { updateHud() }
        vm.lives.observe(this) { updateHud() }
        vm.gameOver.observe(this) { over ->
            if (over && !vm.resultsShown) {
                vm.resultsShown = true
                handler.removeCallbacks(spawner)
                gameView.pauseGame()
                val score = vm.score.value ?: 0
                ScoreStore.add(this, vm.playerName, score)
                resultsLauncher.launch(
                    Intent(this, ResultsActivity::class.java)
                        .putExtra(ResultsActivity.EXTRA_NAME, vm.playerName)
                        .putExtra(ResultsActivity.EXTRA_SCORE, score)
                )
            }
        }
    }

    private fun updateHud() {
        val lives = vm.lives.value ?: 0
        hudText.text = getString(
            R.string.hud_format,
            vm.playerName,
            vm.score.value ?: 0,
            "♥".repeat(lives.coerceAtLeast(0))
        )
    }

    /** Easter egg: tap the moon 5 times for a golden star shower. */
    private fun onMoonTapped() {
        vm.moonTaps++
        if (vm.moonTaps >= 5) {
            vm.moonTaps = 0
            Toast.makeText(this, R.string.easter_egg, Toast.LENGTH_SHORT).show()
            for (i in 0 until 10) {
                gameView.postDelayed({ gameView.spawnStar(true) }, i * 250L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.resumeGame()
        if (vm.gameOver.value != true) {
            handler.removeCallbacks(spawner)
            handler.postDelayed(spawner, 600)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(spawner)
        gameView.pauseGame()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_game, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_share) {
            ShareUtil.shareScore(this, vm.playerName, vm.score.value ?: 0)
            true
        } else super.onOptionsItemSelected(item)
    }
}
