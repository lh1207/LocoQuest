package com.locoquest.app

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.locoquest.app.AppModule.Companion.user
import com.locoquest.app.dto.User


class Profile : Fragment(), OnLongClickListener, OnClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BenchmarkAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        recyclerView = view.findViewById(R.id.benchmarks)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BenchmarkAdapter(ArrayList(user.benchmarks.values.toList()), this, this)
        recyclerView.adapter = adapter

        Glide.with(this)
            .load(FirebaseAuth.getInstance().currentUser?.photoUrl)
            .transform(CircleCrop())
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                ) {
                    view.findViewById<ImageView>(R.id.profileImageView).setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        view.findViewById<TextView>(R.id.nameTextView).text = user.displayName

        view.findViewById<Button>(R.id.editNameButton).setOnClickListener {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Enter Your Name")

            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            input.setPadding((16 * resources.displayMetrics.density).toInt())
            builder.setView(input)

            builder.setPositiveButton("OK") { _, _ ->
                user.displayName = input.text.toString()
                (activity as AppCompatActivity?)!!.supportActionBar?.title = user.displayName
                Thread{ AppModule.db?.localUserDAO()?.update(user) }.start()
            }
            val dialog = builder.create()
            dialog.show()

            val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            okButton.isEnabled = false

            input.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    okButton.isEnabled = s.toString().isNotBlank()
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

        }

        return view
    }

    override fun onLongClick(view: View?): Boolean {
        AlertDialog.Builder(context)
            .setTitle("Remove Benchmark?")
            .setMessage("Warning! This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove"){ _, _ ->
                val pidT = view?.findViewById<TextView>(R.id.pid)?.text.toString()
                val pid = pidT.substring(0, pidT.length-1)
                adapter.removeBenchmark(pid)
                adapter.notifyDataSetChanged()
                user.benchmarks.remove(pid)
                Thread{ AppModule.db?.localUserDAO()?.update(user) }.start()
            }
            .show()
        return true
    }

    override fun onClick(view: View?) {
        val pidT = view?.findViewById<TextView>(R.id.pid)?.text.toString()
        val pid = pidT.substring(0, pidT.length-1)
        Home.selectedBenchmark = user.benchmarks[pid]
        activity?.findViewById<BottomNavigationView>(R.id.bottomNavigationView)?.selectedItemId = R.id.home
    }
}