package com.treemapper

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.treemapper.databinding.ActivityMainBinding
import java.io.OutputStream

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var markerStorage: MarkerStorage
    private lateinit var locationCallback: LocationCallback

    private val treeMarkers = mutableListOf<TreeMarker>()
    private val mapMarkers = mutableMapOf<Int, Marker>()
    private var currentLocation: LatLng? = null
    private var locationPermissionGranted = false

    // Terrain center (465 Route des Hubacs, 26160 Le Poet Laval)
    private val terrainCenter = LatLng(44.5283, 5.0167)
    private val initialZoom = 19f

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                locationPermissionGranted = true
                enableMyLocation()
                startLocationUpdates()
            }
            else -> {
                Toast.makeText(this, getString(R.string.location_permission_denied), Toast.LENGTH_LONG).show()
            }
        }
    }

    private val storagePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            exportMap()
        } else {
            Toast.makeText(this, getString(R.string.storage_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        markerStorage = MarkerStorage(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        treeMarkers.addAll(markerStorage.loadMarkers())

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupButtons()
    }

    private fun setupButtons() {
        binding.fabPlantFound.setOnClickListener {
            addTreeAtCurrentLocation()
        }

        binding.fabListTrees.setOnClickListener {
            showTreeList()
        }

        binding.fabExport.setOnClickListener {
            checkStoragePermissionAndExport()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = true

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(terrainCenter, initialZoom))

        checkLocationPermission()
        restoreMarkersOnMap()

        map.setOnMarkerClickListener { marker ->
            val treeId = mapMarkers.entries.find { it.value == marker }?.key
            if (treeId != null) {
                showMarkerOptions(treeId)
                true
            } else {
                false
            }
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                locationPermissionGranted = true
                enableMyLocation()
                startLocationUpdates()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        if (locationPermissionGranted && ::map.isInitialized) {
            try {
                map.isMyLocationEnabled = true
            } catch (e: SecurityException) {
                // Permission not granted
            }
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 5000L
        ).setMinUpdateIntervalMillis(2000L).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    currentLocation = LatLng(location.latitude, location.longitude)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun addTreeAtCurrentLocation() {
        val location = currentLocation
        if (location == null) {
            Toast.makeText(this, getString(R.string.no_location_yet), Toast.LENGTH_SHORT).show()
            return
        }

        val newId = if (treeMarkers.isEmpty()) 1 else treeMarkers.maxOf { it.id } + 1
        val treeMarker = TreeMarker(newId, location.latitude, location.longitude)
        treeMarkers.add(treeMarker)
        markerStorage.saveMarkers(treeMarkers)

        addMarkerToMap(treeMarker)

        Toast.makeText(
            this,
            getString(R.string.tree_added, newId),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun addMarkerToMap(treeMarker: TreeMarker) {
        val position = LatLng(treeMarker.latitude, treeMarker.longitude)
        val markerBitmap = createTreeMarkerBitmap(treeMarker.id)
        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(getString(R.string.tree_title, treeMarker.id))
                .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
        )
        if (marker != null) {
            mapMarkers[treeMarker.id] = marker
        }
    }

    private fun createTreeMarkerBitmap(treeId: Int): Bitmap {
        val size = 120
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background circle (green)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.FILL
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, bgPaint)

        // Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, borderPaint)

        // Tree emoji text
        val treePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 48f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("🌳", size / 2f, size / 2f + 16f, treePaint)

        // Number below emoji
        val numPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        canvas.drawText(treeId.toString(), size / 2f, size - 8f, numPaint)

        return bitmap
    }

    private fun restoreMarkersOnMap() {
        for (treeMarker in treeMarkers) {
            addMarkerToMap(treeMarker)
        }
    }

    private fun showMarkerOptions(treeId: Int) {
        val treeMarker = treeMarkers.find { it.id == treeId } ?: return
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.tree_title, treeId))
            .setMessage(
                getString(
                    R.string.tree_coordinates,
                    treeMarker.latitude,
                    treeMarker.longitude
                )
            )
            .setNegativeButton(R.string.delete) { _, _ ->
                deleteTree(treeId)
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun deleteTree(treeId: Int) {
        treeMarkers.removeAll { it.id == treeId }
        mapMarkers[treeId]?.remove()
        mapMarkers.remove(treeId)
        markerStorage.saveMarkers(treeMarkers)
        Toast.makeText(
            this,
            getString(R.string.tree_deleted, treeId),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showTreeList() {
        if (treeMarkers.isEmpty()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.tree_list_title)
                .setMessage(R.string.no_trees_yet)
                .setPositiveButton(R.string.close, null)
                .show()
            return
        }

        val items = treeMarkers.map { marker ->
            getString(
                R.string.tree_list_item,
                marker.id,
                marker.latitude,
                marker.longitude
            )
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(R.string.tree_list_title)
            .setItems(items) { _, index ->
                val treeMarker = treeMarkers[index]
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(treeMarker.latitude, treeMarker.longitude),
                        initialZoom
                    )
                )
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    private fun checkStoragePermissionAndExport() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No permission needed on Android 10+
            exportMap()
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> exportMap()
                else -> storagePermissionRequest.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    private fun exportMap() {
        map.snapshot { bitmap ->
            if (bitmap == null) {
                Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
                return@snapshot
            }
            saveBitmapToGallery(bitmap)
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val filename = "TreeMapper_${System.currentTimeMillis()}.png"
        var outputStream: OutputStream? = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TreeMapper")
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                outputStream = uri?.let { contentResolver.openOutputStream(it) }
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val treeMapperDir = java.io.File(picturesDir, "TreeMapper")
                treeMapperDir.mkdirs()
                val file = java.io.File(treeMapperDir, filename)
                outputStream = java.io.FileOutputStream(file)
            }

            outputStream?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                Toast.makeText(this, getString(R.string.export_success, filename), Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.export_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (locationPermissionGranted && ::locationCallback.isInitialized) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
