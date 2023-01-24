package com.keyvani.countrynametocountrycode


import android.Manifest
import android.content.Context
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.PendingResult
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.keyvani.countrynametocountrycode.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.InputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequestId = 100

    private var googleApiClient: GoogleApiClient? = null
    private val requestLocation = 199


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.apply {
            btnGetLocation.setOnClickListener {
                getLocation()
            }
        }
    }

    private fun getLocation() {

        if (checkForLocationPermission()) {
            if (isLocationEnable()) {
                updateLocation()
            } else {
                enableLoc()
                updateLocation()
            }
        } else {
            askLocationPermission()
        }
    }

    private fun isLocationEnable(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission
                (this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        mFusedLocationProviderClient.requestLocationUpdates(
            locationRequest(), mLocationCallback,
            Looper.myLooper()
        )
    }

    private var mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {

            val location: Location? = p0.lastLocation

            if (location != null) {
                updateAddressUI(location)
            }

        }
    }

    fun updateAddressUI(location: Location) {

        val addressList: ArrayList<Address>

        val geocoder = Geocoder(applicationContext, Locale.getDefault())

        addressList = geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1
        ) as ArrayList<Address>

        binding.apply {
            val countryName = addressList[0].countryName
            tvCountryName.text = countryName
            tvCountryCode.text = countryToCode(countryName)

        }

    }

    private fun countryToCode(countryName: String): String {
        var country = ""
        var value = ""
        val json = loadJsonObjectFromAsset("countries.json")
        val refArray = json!!.getJSONArray("countries")
        for (i in 0 until refArray.length()) {
            country = refArray.getJSONObject(i).getString("name")
            value = refArray.getJSONObject(i).getString("value")
            if (countryName == country) break
        }
        return value
    }

    private fun checkForLocationPermission(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
            return true
        return false
    }

    private fun loadJsonObjectFromAsset(assetName: String): JSONObject? {
        try {
            val json = loadStringFromAsset(assetName)
            return JSONObject(json)
        } catch (e: Exception) {
            Log.e("JsonUtils", e.toString())
        }
        return null
    }

    @Throws(Exception::class)
    private fun loadStringFromAsset(assetName: String): String {
        val `is`: InputStream = this.assets.open(assetName)
        val size: Int = `is`.available()
        val buffer = ByteArray(size)
        `is`.read(buffer)
        `is`.close()
        return String(buffer)
    }

    private fun askLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            locationRequestId
        )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestId) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        }

    }

    private fun enableLoc() {
        googleApiClient = GoogleApiClient.Builder(this@MainActivity)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(object : GoogleApiClient.ConnectionCallbacks {
                override fun onConnected(bundle: Bundle?) {}
                override fun onConnectionSuspended(i: Int) {
                    googleApiClient!!.connect()
                }
            })
            .addOnConnectionFailedListener { connectionResult ->
                Log.d("Location error", "Location error " + connectionResult.errorCode)
            }.build()
        googleApiClient!!.connect()
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest())
        builder.setAlwaysShow(true)
        val result: PendingResult<LocationSettingsResult> = LocationServices.SettingsApi
            .checkLocationSettings(googleApiClient!!, builder.build())
        result.setResultCallback {
            val status: Status = it.status
            when (status.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    status.startResolutionForResult(this@MainActivity, requestLocation)
                } catch (_: SendIntentException) {
                }
            }
        }
    }

    private fun locationRequest(): LocationRequest {
        return LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(1000)
            .build()
    }

}