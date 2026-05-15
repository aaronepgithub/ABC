package com.aaronep.abc

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.aaronep.abc.databinding.FragmentFirstBinding

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private val locationUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BikeTrackingService.ACTION_LOCATION_UPDATE) {
                val speed = intent.getFloatExtra(BikeTrackingService.EXTRA_SPEED, 0f)
                val power = intent.getDoubleExtra(BikeTrackingService.EXTRA_POWER, 0.0)

                binding.textSpeed.text = String.format("Speed: %.1f km/h", speed * 3.6)
                binding.textPower.text = String.format("Power: %.0f W", power)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            startTrackingService()
        } else {
            Toast.makeText(context, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            checkPermissionsAndStart()
        }

        binding.buttonFinish.setOnClickListener {
            stopTrackingService()
            TCXExporter.export(requireContext(), DataStore.getPoints())
        }
    }

    private fun checkPermissionsAndStart() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            startTrackingService()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startTrackingService() {
        val weight = binding.editWeight.text.toString().toDoubleOrNull() ?: 70.0
        val wind = binding.editWind.text.toString().toIntOrNull() ?: 1

        val intent = Intent(requireContext(), BikeTrackingService::class.java).apply {
            action = BikeTrackingService.ACTION_START
            putExtra(BikeTrackingService.EXTRA_WEIGHT, weight)
            putExtra(BikeTrackingService.EXTRA_WIND, wind)
        }

        ContextCompat.startForegroundService(requireContext(), intent)

        binding.buttonStart.isEnabled = false
        binding.buttonFinish.isEnabled = true
    }

    private fun stopTrackingService() {
        val intent = Intent(requireContext(), BikeTrackingService::class.java).apply {
            action = BikeTrackingService.ACTION_STOP
        }
        requireContext().stopService(intent)

        binding.buttonStart.isEnabled = true
        binding.buttonFinish.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BikeTrackingService.ACTION_LOCATION_UPDATE)
        ContextCompat.registerReceiver(requireContext(), locationUpdateReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(locationUpdateReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
