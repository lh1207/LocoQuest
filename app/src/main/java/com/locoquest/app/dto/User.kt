package com.locoquest.app.dto

import androidx.room.Entity
import androidx.room.PrimaryKey
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
    val pids: ArrayList<String> = ArrayList(),
    val friends: ArrayList<String> = ArrayList()
){

    fun update(){
        Thread{ db?.localUserDAO()?.update(this) }.start()
        push()
    }

    fun push(){
        Firebase.firestore.collection("users").document(AppModule.user.uid)
            .set(hashMapOf(
                "name" to displayName,
                "photoUrl" to photoUrl,
                "pids" to pids.toList(),
                "uids" to friends.toList()
            ))
    }
    override fun toString(): String {
        return displayName
    }
}
