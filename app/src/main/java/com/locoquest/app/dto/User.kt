package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.AppModule
import com.locoquest.app.AppModule.Companion.db

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    var displayName: String = "",
    var photoUrl: String = "",
    var balance: Long = 0,
    val visited: HashMap<String, Benchmark> = HashMap(),
    val friends: ArrayList<String> = ArrayList()
){

    fun update(){
        Thread{ db?.localUserDAO()?.update(this) }.start()
        push()
    }

    fun push(){
        val visitedList = ArrayList<HashMap<String, Any>>()
        visited.forEach { x -> visitedList.add(hashMapOf(
            "pid" to x.key,
            "name" to x.value.name,
            "location" to GeoPoint(x.value.lat, x.value.lon),
            "lastVisited" to Timestamp(x.value.lastVisitedSeconds, 0)
        )) }
        Firebase.firestore.collection("users").document(AppModule.user.uid)
            .set(hashMapOf(
                "name" to displayName,
                "photoUrl" to photoUrl,
                "balance" to balance,
                "visited" to visitedList.toList(),
                "uids" to friends.toList()
            ))
    }
    override fun toString(): String {
        return displayName
    }
}
