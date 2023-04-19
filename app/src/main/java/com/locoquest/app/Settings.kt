package com.locoquest.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch

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
        val notificationToggle = view.findViewById<Switch>(R.id.notificationToggle)
        val preciseLocationToggle = view.findViewById<Switch>(R.id.preciseLocationToggle)
        val approximateLocationToggle = view.findViewById<Switch>(R.id.approximateLocationToggle)
        val requestPermissionsButton = view.findViewById<Button>(R.id.requestPermissionsButton)

        // Set an event listener for the notification toggle switch
        notificationToggle.setOnCheckedChangeListener { _, _ ->
            // Do something when the toggle is checked or unchecked
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