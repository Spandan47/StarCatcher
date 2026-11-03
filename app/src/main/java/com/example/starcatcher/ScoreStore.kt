package com.example.starcatcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Saves the top 5 scores in SharedPreferences as JSON. */
object ScoreStore {
    data class Entry(val name: String, val score: Int)

    private const val PREFS = "scores"
    private const val KEY = "top"
    private const val MAX = 5

    fun top(context: Context): List<Entry> {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Entry(o.getString("name"), o.getInt("score"))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, name: String, score: Int) {
        val updated = (top(context) + Entry(name, score))
            .sortedByDescending { it.score }
            .take(MAX)
        val arr = JSONArray()
        updated.forEach {
            arr.put(JSONObject().put("name", it.name).put("score", it.score))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }
}
