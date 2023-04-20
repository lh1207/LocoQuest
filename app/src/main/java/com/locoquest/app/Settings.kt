package com.locoquest.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"
private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
private const val APPROXIMATE_LOCATION_PERMISSION_REQUEST_CODE = 101

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
        val preciseLocationToggle = view.findViewById<SwitchMaterial>(R.id.preciseLocationToggle)
        val approximateLocationToggle = view.findViewById<SwitchMaterial>(R.id.approximateLocationToggle)
        val requestPermissionsButton = view.findViewById<Button>(R.id.requestPermissionsButton)
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            // Check if the permission is granted and do something accordingly
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                // Permission is granted, do something
            } else {
                // Permission is not granted, show a message or do something else
            }
        }

        // Set an event listener for the precise location toggle switch
        // If permissions are not granted, request permissions
        preciseLocationToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Do something when the toggle is checked
                if (hasPreciseLocationPermission()) {
                    // Do something with precise location
                } else {
                    requestPreciseLocationPermission()
                }
            } else {
                // Do something when the toggle is unchecked
            }
        }

        // Set an event listener for the approximate location toggle switch
        // If permissions are not granted, request permissions
        approximateLocationToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Do something when the toggle is checked
                if (hasApproximateLocationPermission()) {
                    // Do something with approximate location
                } else {
                    requestApproximateLocationPermission()
                }
            } else {
                // Do something when the toggle is unchecked
            }
        }

        // Set an event listener for the request permissions button
        // Only shows if permissions are not granted
        requestPermissionsButton.setOnClickListener {
            if (!hasPreciseLocationPermission()) {
                requestPreciseLocationPermission()
            } else if (!hasApproximateLocationPermission()) {
                requestApproximateLocationPermission()
            }
        }

        // Set an event listener for the precise location toggle switch
        // If permissions are not granted, request permissions
        preciseLocationToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Do something when the toggle is checked
                if (hasPreciseLocationPermission()) {
                    // Do something with precise location
                } else {
                    requestPreciseLocationPermission()
                }
            } else {
                // Do something when the toggle is unchecked
            }
        }

    }

    private fun hasPreciseLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPreciseLocationPermission() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        )
    }

    private fun hasApproximateLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestApproximateLocationPermission() {
        permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
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