package com.locoquest.app

import BenchmarkService
import IBenchmarkService
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.locoquest.app.dto.Benchmark
import kotlinx.coroutines.launch

class BenchmarkAdapter(private val pids: ArrayList<String>,
                       private val longClickListener: OnLongClickListener,
                       private val clickListener: OnClickListener) : RecyclerView.Adapter<BenchmarkAdapter.ViewHolder>() {

    private val benchmarkService: IBenchmarkService = BenchmarkService()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.benchmark, parent, false)
        return ViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return pids.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pid = pids[position]
        Thread{
            val benchmarks = benchmarkService.getBenchmarks(listOf(pid))
            if(benchmarks == null || benchmarks.size != 1) return@Thread
            val benchmark = benchmarks[0]
            Handler(Looper.getMainLooper()).post{
                holder.pid.text = pid
                holder.name.text = benchmark.name
                holder.latlng.text = "${benchmark.lat} ${benchmark.lon}"

                holder.itemView.setOnLongClickListener(longClickListener)
                holder.itemView.setOnClickListener(clickListener)
            }
        }.start()
    }

    fun removeBenchmark(pid: String): Boolean? {
        /*val index = benchmarks.indexOfFirst { it.pid == pid }
        return if (index != -1) benchmarks.removeAt(index) else null*/
        return pids.remove(pid)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pid: TextView = itemView.findViewById(R.id.pid)
        val name: TextView = itemView.findViewById(R.id.name)
        val latlng: TextView = itemView.findViewById(R.id.latlng)
    }
}