package com.locoquest.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment

class CoinCollectedDialog(private val fragmentListener: ISecondaryFragment,
                          private val watchAdButtonClickListener: IWatchAdButtonClickListener) : Fragment() {
    interface IWatchAdButtonClickListener{
        fun onWatchAdButtonClicked()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_coin_collected_dialog, container, false)
        view.findViewById<Button>(R.id.watch_ad_for_extra_coin_btn).setOnClickListener {
            watchAdButtonClickListener.onWatchAdButtonClicked()
            fragmentListener.onClose(this)
        }
        view.findViewById<ImageView>(R.id.close_dialog_btn).setOnClickListener { fragmentListener.onClose(this) }
        return view
    }
}