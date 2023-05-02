package com.locoquest.app

import android.app.AlertDialog
import android.content.Intent
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
import android.widget.FrameLayout
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
import com.locoquest.app.AppModule.Companion.guest
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User


class Profile(private val user: User,
              private val enableEdit: Boolean,
              private val fragmentListener: ISecondaryFragment,
              private val profileListener: ProfileListener) : Fragment(), OnLongClickListener, OnClickListener {

    interface ProfileListener {
        fun onBenchmarkClicked(benchmark: Benchmark)
        fun onLogin()
        fun onSignOut()
    }
    private lateinit var adapter: BenchmarkAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var benchmarkCount: TextView
    private lateinit var name: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        view.findViewById<ImageView>(R.id.close_btn).setOnClickListener { fragmentListener.onClose(this) }

        view.findViewById<TextView>(R.id.friends_tv).text = "Friends (${user.friends.size})"

        view.findViewById<TextView>(R.id.profile_balance).text = user.balance.toString()

        benchmarkCount = view.findViewById(R.id.benchmarkCount)

        val signBtn = view.findViewById<Button>(R.id.sign_in_out_btn)
        val editNameBtn = view.findViewById<ImageView>(R.id.editNameBtn)

        if(enableEdit) {
            signBtn.text =
                if (user.uid == guest.uid)
                    getString(R.string.login)
                else
                    getString(R.string.sign_out)
            signBtn.setOnClickListener {
                if (user.uid == guest.uid) {
                    profileListener.onLogin()
                } else {
                    profileListener.onSignOut()
                }
            }

            editNameBtn.setOnClickListener {showEditNameDialog()}
        }else {
            signBtn.visibility = View.GONE
            editNameBtn.visibility = View.GONE
        }

        recyclerView = view.findViewById(R.id.benchmarks)
        recyclerView.layoutManager = LinearLayoutManager(context)
        adapter = BenchmarkAdapter(
            ArrayList(user.visited.values.toList().sortedByDescending { x-> x.lastVisited }),
            this, this)
        recyclerView.adapter = adapter
        benchmarkCount.text = "(${adapter.itemCount})"

        Glide.with(this)
            .load(user.photoUrl)
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

        name = view.findViewById(R.id.nameTextView)
        name.text = user.displayName

        view.findViewById<TextView>(R.id.friends_tv).setOnClickListener {
            FriendsActivity.user = user
            startActivity(Intent(context, FriendsActivity::class.java))
        }

        view.findViewById<FrameLayout>(R.id.profile_bg).setOnTouchListener { _, _ -> true }
        return view
    }

    override fun onLongClick(view: View?): Boolean {
        if(enableEdit) return false
        AlertDialog.Builder(context)
            .setTitle("Remove Benchmark?")
            .setMessage("Warning! This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove"){ _, _ ->
                val pidT = view?.findViewById<TextView>(R.id.pid)?.text.toString()
                val pid = pidT.substring(0, pidT.length-1)
                adapter.removeBenchmark(pid)
                adapter.notifyDataSetChanged()
                user.visited.remove(pid)
                user.update()
                benchmarkCount.text = "(${adapter.itemCount})"
            }
            .show()
        return true
    }

    override fun onClick(view: View?) {
        if(!enableEdit) return
        val pidT = view?.findViewById<TextView>(R.id.pid)?.text.toString()
        val pid = pidT.substring(0, pidT.length-1)
        if(user.visited.contains(pid)) profileListener.onBenchmarkClicked(user.visited[pid]!!)
    }

    private fun showEditNameDialog(){
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Enter Your Name")

        val input = EditText(context)
        input.setText(user.displayName)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.setPadding((16 * resources.displayMetrics.density).toInt())
        builder.setView(input)

        builder.setPositiveButton("OK") { _, _ ->
            user.displayName = input.text.toString()
            name.text = user.displayName
            (activity as AppCompatActivity?)!!.supportActionBar?.title = user.displayName
            user.update()
        }
        val dialog = builder.create()
        dialog.show()

        val okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        okButton.isEnabled = false

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                okButton.isEnabled = s.toString().isNotBlank()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        })
    }
}