package com.example.starcatcher

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScoreAdapter(private val items: List<ScoreStore.Entry>) :
    RecyclerView.Adapter<ScoreAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.rowName)
        val score: TextView = view.findViewById(R.id.rowScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.name.text = holder.itemView.context.getString(R.string.score_row, position + 1, item.name)
        holder.score.text = item.score.toString()
    }

    override fun getItemCount() = items.size
}
