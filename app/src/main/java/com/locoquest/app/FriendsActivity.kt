package com.locoquest.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User

class FriendsActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener,
    Profile.ProfileListener, ISecondaryFragment {
    private val users: ArrayList<User> = ArrayList()
    private lateinit var fab: FloatingActionButton
    private lateinit var adapter: FriendsAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var friendCount: TextView
    private var profile: Profile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_friends)

        friendCount = findViewById(R.id.friend_count)
        friendCount.text = "(${user.friends.size})"
        adapter = FriendsAdapter(this, ArrayList(), this, this)
        recyclerView = findViewById(R.id.friends)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        user.friends.forEach {
            Firebase.firestore.collection("users").document(it)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.data == null) return@addOnSuccessListener
                    adapter.addFriend(
                        User(
                            doc.id,
                            doc["name"] as String,
                            doc["photoUrl"] as String
                        )
                    )
                    friendCount.text = "(${adapter.itemCount})"
                }
        }

        fab = findViewById(R.id.add_friend_fab)
        if(user == AppModule.user) {
            fab.setOnClickListener {
                if (users.isEmpty()) Firebase.firestore.collection("users")
                    .get().addOnSuccessListener {
                        it.documents.forEach { doc ->
                            if (doc.id != user.uid && !user.friends.contains(doc.id)
                                && doc["name"] != null && doc["name"] != "" && doc["photoUrl"] != null) {
                                users.add(
                                    User(
                                        doc.id,
                                        doc["name"] as String,
                                        doc["photoUrl"] as String
                                    )
                                )
                            }
                        }
                        showAddUserDialog()
                    } else showAddUserDialog()
            }
        }else fab.visibility = View.GONE
    }

    override fun onClick(view: View?) {
        val uid = view?.findViewById<TextView>(R.id.user_id)?.text.toString()
        Firebase.firestore.collection("users").document(uid)
            .get().addOnSuccessListener {

                val name = if(it["name"] == null) "" else it["name"] as String
                val photoUrl = if(it["photoUrl"] == null) "" else it["photoUrl"] as String
                val balance = if(it["balance"] == null) 0L else it["balance"] as Long
                val lastRadiusBoost = if(it["lastRadiusBoost"] == null) Timestamp(0,0)
                                      else it["lastRadiusBoost"] as Timestamp

                val visited = HashMap<String, Benchmark>()
                val visitedList = if(it["visited"] == null) ArrayList() else it["visited"] as ArrayList<HashMap<String, Any>>
                visitedList.forEach { x ->
                    val pid = x["pid"] as String
                    visited[pid] = Benchmark.new(pid, x["name"] as String, x["location"] as GeoPoint, x["lastVisited"] as Timestamp)
                }

                val friends = if(it["friends"] == null) ArrayList() else it["friends"] as ArrayList<String>

                val friend = User(uid, name, photoUrl, balance, lastRadiusBoost, visited, friends)

                profile = Profile(friend, false, this, this)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_container, profile!!).commit()
                fab.visibility = View.GONE
            }
    }

    override fun onLongClick(view: View?): Boolean {
        android.app.AlertDialog.Builder(this)
            .setTitle("Remove Friend?")
            .setMessage("Warning! This cannot be undone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Remove"){ _, _ ->
                val uid = view?.findViewById<TextView>(R.id.user_id)?.text.toString()
                adapter.removeFriend(uid)
                adapter.notifyDataSetChanged()
                user.friends.remove(uid)
                user.update()
                friendCount.text = "(${adapter.itemCount})"
            }
            .show()
        return true
    }

    private fun showAddUserDialog() {
        var selectedUser: User? = null
        val autoCompleteEditText = AutoCompleteTextView(this)
        autoCompleteEditText.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                users
            )
        )

        autoCompleteEditText.onItemClickListener =
            AdapterView.OnItemClickListener { parent, _, position, _ ->
                selectedUser = parent?.getItemAtPosition(position) as User?
            }

        autoCompleteEditText.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedUser = parent?.getItemAtPosition(position) as User?
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedUser = null
            }
        }


        AlertDialog.Builder(this)
            .setTitle("Pick a Friend")
            .setView(autoCompleteEditText)
            .setPositiveButton("OK") { _, _ ->
                if (selectedUser != null) {
                    user.friends.add(selectedUser!!.uid)
                    user.update()
                    adapter.addFriend(selectedUser!!)
                    friendCount.text = "(${adapter.itemCount})"
                } else {
                    Toast.makeText(this, "Please select a friend", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onBenchmarkClicked(benchmark: Benchmark) {
        TODO("Not yet implemented")
    }

    override fun onLogin() {
        TODO("Not yet implemented")
    }

    override fun onSignOut() {
        TODO("Not yet implemented")
    }

    override fun onClose(fragment: Fragment) {
        supportFragmentManager.beginTransaction().remove(fragment).commit()
        if(user == AppModule.user) fab.visibility = View.VISIBLE
    }

    companion object{
        var user: User = AppModule.user
    }
}