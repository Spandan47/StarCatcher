package com.example.starcatcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ResultsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NAME = "results_name"
        const val EXTRA_SCORE = "results_score"
        const val EXTRA_PLAY_AGAIN = "results_play_again"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_results)

        val name = intent.getStringExtra(EXTRA_NAME) ?: getString(R.string.default_name)
        val score = intent.getIntExtra(EXTRA_SCORE, 0)

        findViewById<TextView>(R.id.summaryText).text =
            getString(R.string.results_summary, name, score)

        findViewById<RecyclerView>(R.id.scoreList).apply {
            layoutManager = LinearLayoutManager(this@ResultsActivity)
            adapter = ScoreAdapter(ScoreStore.top(this@ResultsActivity))
        }

        findViewById<Button>(R.id.shareButton).setOnClickListener {
            ShareUtil.shareScore(this, name, score)
        }

        // Passes data back to the first screen: "play again"
        findViewById<Button>(R.id.playAgainButton).setOnClickListener {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_PLAY_AGAIN, true))
            finish()
        }
    }
}
