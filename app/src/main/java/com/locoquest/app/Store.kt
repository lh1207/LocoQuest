package com.locoquest.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.locoquest.app.AppModule.Companion.user

class Store(private val fragmentListener: ISecondaryFragment) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store, container, false)
        view.findViewById<Button>(R.id.radius_booster_btn_coin).setOnClickListener {
            if(user.balance < 2) {
                Toast.makeText(requireContext(), "Insufficient Funds", Toast.LENGTH_SHORT).show()
            }else {
                user.balance -= 2
                user.lastRadiusBoost = Timestamp.now()
                user.update()
                fragmentListener.onClose(this)
            }
        }
        view.findViewById<Button>(R.id.radius_booster_btn_ad).setOnClickListener {
            startActivity(Intent(requireContext(), RadiusBoosterAdMobActivity::class.java))
            fragmentListener.onClose(this)
        }
        view.findViewById<ImageView>(R.id.close_store_btn).setOnClickListener { fragmentListener.onClose(this) }
        view.findViewById<FrameLayout>(R.id.store_bg).setOnTouchListener { _, _ -> true }
        return view
    }
}