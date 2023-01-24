package com.example.demoapp

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.example.demoapp.databinding.ActivityMainBinding
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private var lat: Double = 0.0
    private var long: Double = 0.0
    private var addressString: String = ""
    private lateinit var viewModel: ViewModel
    private var done = false

    private val cropActivityResultContract = object : ActivityResultContract<Any?, Uri?>() {
        override fun createIntent(context: Context, input: Any?): Intent {
            return CropImage.activity()
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setAspectRatio(10, 9)
                .setOutputCompressQuality(50)
                .getIntent(this@MainActivity)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return CropImage.getActivityResult(intent)?.uri
        }
    }

    private lateinit var cropActivityResultLauncher: ActivityResultLauncher<Any?>

    @SuppressLint("SetTextI18n", "ResourceAsColor", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[ViewModel::class.java]

        checkPermissions()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.onResume()
        binding.mapView.getMapAsync(this)
        getLocation()
        Thread(Runnable {
            while (true) {
                runOnUiThread {
                    binding.timeStamp.text =
                        SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(Date())
                    val min = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                    if((Integer.parseInt(min) % 15 == 0) && !done) {
                        viewModel.uploadData(getIMEI(), binding.connectivityText.text.toString(),
                            binding.chargingPercentage.text.toString(), isCharging(),
                            binding.currentLocation.text.toString())
                        Toast.makeText(this, "Data uploaded", Toast.LENGTH_SHORT).show()
                        done = true
                    }
                    if((Integer.parseInt(min) % 15 != 0) && done) done = false
                    if (checkForInternet()) {
                        binding.connectionOk.visibility = View.VISIBLE
                        binding.connectionLost.visibility = View.GONE
                        binding.connectivityText.text = "Connection is perfect"
                        binding.connectivityText.setTextColor(getColor(R.color.nion))
                    } else {
                        binding.connectionOk.visibility = View.GONE
                        binding.connectionLost.visibility = View.VISIBLE
                        binding.connectivityText.text = "Connection was lost"
                        binding.connectivityText.setTextColor(Color.RED)
                    }
                    binding.imeiNo.text = "IMEI No : " + getIMEI()
                    binding.chargingPercentage.text = getBatteryPercentage().toString() + "%"
                    binding.chargingStatus.text = "Charging status : " + isCharging()
                }
                Thread.sleep(1000)
            }
        }).start()

        cropActivityResultLauncher = registerForActivityResult(cropActivityResultContract) { uri ->
            if (uri != null) {
                viewModel.uploadImage(uri)
                Toast.makeText(this, "Image uploaded to storage", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No image is selected", Toast.LENGTH_SHORT).show()
            }
        }

        binding.captureDataBtn.setOnClickListener {
            viewModel.uploadData(getIMEI(), binding.connectivityText.text.toString(),
                binding.chargingPercentage.text.toString(), isCharging(),
                binding.currentLocation.text.toString())
            Toast.makeText(this, "Data uploaded", Toast.LENGTH_SHORT).show()
        }

        binding.cameraBtn.setOnClickListener {
            cropActivityResultLauncher.launch(null)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this@MainActivity,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(ACCESS_FINE_LOCATION),
                2
            )
        }
        fusedLocationClient.lastLocation.addOnSuccessListener {
            if (it != null) {
                lat = it.latitude
                long = it.longitude
                getCurrentLocation()
                onMapReady(googleMap)
            } else {
                Toast.makeText(this, "Turn on your location", Toast.LENGTH_LONG).show()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getCurrentLocation() {
        val geocoder = Geocoder(this@MainActivity)
        var addressList = mutableListOf<Address>()
        try {
            addressList = geocoder.getFromLocation(lat, long, 1)!!
            if (addressList.isNotEmpty()) {
                val address = addressList[0]
                val sb = StringBuilder()
                for (i in 0 until address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append("\n")
                }
                if (address.premises != null)
                    sb.append(address.premises).append(", ")

                sb.append(address.subLocality).append(", ")
                sb.append(address.locality).append(", ")
                sb.append(address.adminArea).append(", ")
                sb.append(address.countryName).append(", ")
                sb.append(address.postalCode)

                val temp = StringBuilder()
                temp.append(address.subLocality).append(",")
                temp.append(address.locality)

                addressString = temp.toString()
                binding.currentLocation.text = sb.toString()
            }

        } catch (e: Exception) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        map.let {
            googleMap = it
        }
        val location = LatLng(lat, long)
        googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(addressString)
        )
        val pos = LatLng(lat, long)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15.0f))
    }

    private fun getBatteryPercentage(): Int {
        val bm = this.getSystemService(BATTERY_SERVICE) as BatteryManager
        val bp = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        when (bp) {
            in 71..100 -> {
                binding.batteryImage.setImageResource(R.drawable.battery_level_good)
            }
            in 51..70 -> {
                binding.batteryImage.setImageResource(R.drawable.battery_level_medium)
            }
            in 21..50 -> {
                binding.batteryImage.setImageResource(R.drawable.battery_level_low)
            }
            else -> {
                binding.batteryImage.setImageResource(R.drawable.battery_level_dead)
            }
        }
        return bp
    }

    private fun isCharging(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            this.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING
                || status == BatteryManager.BATTERY_STATUS_FULL

        return if (isCharging) {
            binding.chargingImage.visibility = View.VISIBLE
            "Charging"
        } else {
            binding.chargingImage.visibility = View.GONE
            "Not charging"
        }
    }

    private fun getIMEI(): String {
        val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(READ_PHONE_STATE), 1)
        }
        var output = "Can't access on ANDROID 10 and newer versions"
        try {
            output = telephonyManager.imei
        } catch (_: java.lang.Exception) {
        }
        return output
    }

    private fun checkForInternet(): Boolean {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(READ_PHONE_STATE), 1)
        } else if (ActivityCompat.checkSelfPermission(
                this@MainActivity,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this@MainActivity,
                ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(ACCESS_FINE_LOCATION),
                2
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                checkPermissions()
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
        if (requestCode == 2) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                Toast.makeText(this, "Permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}