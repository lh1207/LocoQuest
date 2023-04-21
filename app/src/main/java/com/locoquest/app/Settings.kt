package com.locoquest.app

import android.Manifest
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class Settings : Fragment() {
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var rationaleDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                // Location permission is granted, display a toast message
                Toast.makeText(
                    requireContext(),
                    "Location permission is granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Location permission is denied, display a toast message
                Toast.makeText(
                    requireContext(),
                    "Location permission is denied",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Initialize the rationale dialog
        rationaleDialog = AlertDialog.Builder(requireContext())
            .setTitle("Location Permissions")
            .setMessage("LocoQuest needs your location to provide accurate directions. Please grant location permissions.")
            .setPositiveButton("OK") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
                requestLocationPermission()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get reference to the request permissions button in the layout
        val requestPermissionsButton = view.findViewById<Button>(R.id.requestPermissionsButton)

        // Set an event listener for the request permissions button
        requestPermissionsButton.setOnClickListener {
            if (hasLocationPermission()) {
                // Location permission is already granted, display a toast message
                Toast.makeText(
                    requireContext(),
                    "Location permission is already granted",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Location permission is not granted, request the permission
                if (shouldShowRequestPermissionRationale()) {
                    // Show rationale dialog before requesting the permission
                    rationaleDialog.show()
                } else {
                    requestLocationPermission()
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun shouldShowRequestPermissionRationale(): Boolean {
        return shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Function to request location permission
    private fun requestLocationPermission() {
        if (hasLocationPermission()) {
            // Location permission is already granted, display a toast message
            Toast.makeText(
                requireContext(),
                "Location permission is already granted",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            // Location permission is not granted, request the permission
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}