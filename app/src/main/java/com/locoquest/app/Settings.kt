package com.locoquest.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.switchmaterial.SwitchMaterial

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [Settings.newInstance] factory method to
 * create an instance of this fragment.
 */
class Settings : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get references to the switches and button in the layout
        val notificationToggle = view.findViewById<SwitchMaterial>(R.id.notificationToggle)
        val preciseLocationToggle = view.findViewById<SwitchMaterial>(R.id.preciseLocationToggle)
        val approximateLocationToggle = view.findViewById<SwitchMaterial>(R.id.approximateLocationToggle)
        val requestPermissionsButton = view.findViewById<Button>(R.id.requestPermissionsButton)

        // Set an event listener for the notification toggle switch
        notificationToggle.setOnCheckedChangeListener { _, isChecked ->
            // Get the NotificationManager system service
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Check if the notification toggle switch is checked
            if (isChecked) {
                // If the notification toggle switch is checked, enable notifications
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // For Android 8.0 and above, create a channel for notifications
                        val channel = NotificationChannel("channel_id", "channel_name", NotificationManager.IMPORTANCE_DEFAULT)
                        notificationManager.createNotificationChannel(channel)
                    }
                } catch (e: Exception) {
                    Log.e("Notification", "Error creating notification channel: ${e.message}")
                }

                // Enable notifications in the app
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            } else {
                // If the notification toggle switch is unchecked, disable notifications
                // Disable notifications in the app
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
            }
        }

        // Set an event listener for the precise location toggle switch
        preciseLocationToggle.setOnCheckedChangeListener { _, _ ->
            // Do something when the toggle is checked or unchecked
        }

        // Set an event listener for the approximate location toggle switch
        approximateLocationToggle.setOnCheckedChangeListener { _, _ ->
            // Do something when the toggle is checked or unchecked
        }

        // Set an event listener for the request permissions button
        requestPermissionsButton.setOnClickListener {
            // Do something when the button is clicked
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Settings.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            Settings().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}