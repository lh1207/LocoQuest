package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.locoquest.app.AppModule
import com.locoquest.app.AppModule.Companion.BOOSTED_DURATION
import com.locoquest.app.AppModule.Companion.BOOSTED_REACH
import com.locoquest.app.AppModule.Companion.DEFAULT_REACH
import com.locoquest.app.AppModule.Companion.db

@Entity
data class User(
    @PrimaryKey
    val uid: String,
    var displayName: String = "",
    var photoUrl: String = "",
    var balance: Long = 0,
    var lastRadiusBoost: Timestamp = Timestamp(0,0),
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
            "pid" to x.value.pid,
            "name" to x.value.name,
            "location" to GeoPoint(x.value.lat, x.value.lon),
            "lastVisited" to Timestamp(x.value.lastVisited, 0),
            "notify" to x.value.notify
        )) }
        Firebase.firestore.collection("users").document(AppModule.user.uid)
            .set(hashMapOf(
                "name" to displayName,
                "photoUrl" to photoUrl,
                "balance" to balance,
                "lastRadiusBoost" to lastRadiusBoost,
                "visited" to visitedList.toList(),
                "friends" to friends.toList()
            ))
    }

    fun isBoosted(): Boolean {
        return AppModule.user.lastRadiusBoost.seconds + BOOSTED_DURATION > Timestamp.now().seconds
    }

    fun getReach(): Double {
        return if (isBoosted()) BOOSTED_REACH else DEFAULT_REACH
    }

    fun dump() : String{
        return "uid: $uid, name:$displayName, photoUrl:$photoUrl, balance:$balance, lastRadiusBoost:${lastRadiusBoost.seconds}, visited:${visited.size}, friends:${friends.size}"
    }

    override fun toString(): String {
        return displayName
    }
}
