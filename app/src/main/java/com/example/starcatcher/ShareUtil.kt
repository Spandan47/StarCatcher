package com.example.starcatcher

import android.content.Context
import android.content.Intent

object ShareUtil {
    /** Implicit intent: lets the user send their score through any app that accepts text. */
    fun shareScore(context: Context, name: String, score: Int) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_text, name, score))
        }
        context.startActivity(Intent.createChooser(send, context.getString(R.string.share_chooser)))
    }
}
