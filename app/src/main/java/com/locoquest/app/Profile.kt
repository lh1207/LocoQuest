package com.locoquest.app

import android.app.AlertDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

class Profile : Fragment(), OnLongClickListener, OnClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BenchmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        recyclerView = view.findViewById(R.id.benchmarks)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BenchmarkAdapter(ArrayList(AppModule.user.benchmarks.values.toList()), this, this)
        recyclerView.adapter = adapter

        return view
    }

    override fun onLongClick(view: View?): Boolean {
        AlertDialog.Builder(context)
            .setTitle("Remove Benchmark?")
            .setMessage("Warning! This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove"){ _, _ ->
                val pid = view?.findViewById<TextView>(R.id.pid)?.text.toString()
                adapter.removeBenchmark(pid)
                adapter.notifyDataSetChanged()
                AppModule.user.benchmarks.remove(pid)
                Thread{ AppModule.db?.localUserDAO()?.update(AppModule.user) }.start()
            }
            .show()
        return true
    }

    override fun onClick(view: View?) {
        val pidT = view?.findViewById<TextView>(R.id.pid)?.text.toString()
        val pid = pidT.substring(0, pidT.length-1)
        Home.selectedBenchmark = AppModule.user.benchmarks[pid]
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.home
    }
}