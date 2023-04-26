package com.locoquest.app

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.CustomTarget
import com.google.firebase.auth.FirebaseAuth
import com.locoquest.app.dto.Benchmark
import com.locoquest.app.dto.User

class FriendsAdapter(private val context: Context,
                     private val friends: ArrayList<User>,
                     private val longClickListener: View.OnLongClickListener,
                     private val clickListener: View.OnClickListener
) : RecyclerView.Adapter<FriendsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.friend, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val friend = friends[position]
        holder.uid.text = friend.uid
        holder.name.text = friend.displayName
        Glide.with(context)
            .load(friend.photoUrl)
            .transform(CircleCrop())
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: com.bumptech.glide.request.transition.Transition<in Drawable>?
                ) {
                    holder.img.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        holder.itemView.setOnLongClickListener(longClickListener)
        holder.itemView.setOnClickListener(clickListener)
    }

    override fun getItemCount(): Int {
        return friends.size
    }

    fun removeFriend(uid: String): User? {
        val index = friends.indexOfFirst { it.uid == uid }
        return if (index != -1) friends.removeAt(index) else null
    }

    fun addFriend(friend: User) {
        friends.add(friend)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val uid: TextView = itemView.findViewById(R.id.user_id)
        val name: TextView = itemView.findViewById(R.id.user_name)
        val img: ImageView = itemView.findViewById(R.id.user_img)
    }
}