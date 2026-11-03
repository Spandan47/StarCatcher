package com.example.starcatcher

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StartActivity : AppCompatActivity() {

    private lateinit var bestScoreText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val nameInput = findViewById<EditText>(R.id.nameInput)
        bestScoreText = findViewById(R.id.bestScoreText)

        findViewById<Button>(R.id.playButton).setOnClickListener {
            val name = nameInput.text.toString().trim().ifEmpty { getString(R.string.default_name) }
            startActivity(
                Intent(this, GameActivity::class.java).putExtra(GameActivity.EXTRA_NAME, name)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val best = ScoreStore.top(this).maxOfOrNull { it.score } ?: 0
        bestScoreText.text = getString(R.string.best_score, best)
    }
}
