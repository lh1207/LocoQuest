package com.locoquest.app

import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.locoquest.app.dto.Benchmark

class BenchmarkAdapter(private val benchmarks: ArrayList<Benchmark>,
                       private val longClickListener: OnLongClickListener,
                       private val clickListener: OnClickListener) : RecyclerView.Adapter<BenchmarkAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.benchmark, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return benchmarks.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val benchmark = benchmarks[position]
        holder.pid.text = "${benchmark.pid}:"
        holder.name.text = benchmark.name
        holder.latlng.text = "${benchmark.lat} ${benchmark.lon}"
        holder.lastVisited.text = Converters.formatSeconds(benchmark.lastVisitedSeconds)

        holder.itemView.setOnLongClickListener(longClickListener)
        holder.itemView.setOnClickListener(clickListener)
    }

    fun removeBenchmark(pid: String): Benchmark? {
        val index = benchmarks.indexOfFirst { it.pid == pid }
        return if (index != -1) benchmarks.removeAt(index) else null
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pid: TextView = itemView.findViewById(R.id.pid)
        val name: TextView = itemView.findViewById(R.id.name)
        val latlng: TextView = itemView.findViewById(R.id.latlng)
        val lastVisited: TextView = itemView.findViewById(R.id.last)
    }
}