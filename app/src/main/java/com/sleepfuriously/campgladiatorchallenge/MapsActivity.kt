package com.sleepfuriously.campgladiatorchallenge

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.android.synthetic.main.activity_maps.*
import java.io.IOException


class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    //---------------------------
    //  constants
    //---------------------------

    private val TAG = "MapsActivity"

    //  KEYS
    private val CAMERA_POS_KEY = "camera_pos_key"
    private val LOCATION_KEY = "location_key"

    private val DEFAULT_ZOOM = 13f

    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    private val MAX_LOC_ENTRIES = 5;


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        /** request code passed to onActivityResult() */
        private const val REQUEST_CHECK_SETTINGS = 2

        public const val TEST_URL = "https://stagingapi.campgladiator.com/api/v2/places/searchbydistance?lat=30.406991&lon=-97.720310&radius=25"
    }


    //---------------------------
    //  widgets
    //---------------------------

    /** displays loading message */
    private lateinit var mLoadingTv: TextView

    /** displays loading animation */
    private lateinit var mLoadingProgressBar: ProgressBar

    //---------------------------
    //  data
    //---------------------------

    /** the primary data for this app */
    private lateinit var mMap: GoogleMap

    private var mCameraPos: CameraPosition? = null

    /** entrypoint to Places api */
    private lateinit var mPlacesClient: PlacesClient;

    /** entypoint to Fused Loc Provider */
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    /** will be used when location callback occurs */
    private lateinit var mLocationCallback: LocationCallback

    private lateinit var mLocationRequest: LocationRequest

    private var mLocationUpdateState = false

    /** last location */
    private lateinit var mLastLocation: Location

    /** stores the last marker so it can be deleted */
    private var mLastMarker: Marker? = null


    /** default location (sydney). used when location permission is not granted */
    private var mDefaultLocation = LatLng(-33.8523341, 151.2106085)

    private val mLocationPermissionGranted = false

    /** used by volley */
    private val mRequestQ by lazy {
        Volley.newRequestQueue(this)
    }

    // for selecting a current place

    private lateinit var mLikelyPlaceNames: Array<String>
    private lateinit var mLikelyPlaceAddresses: Array<String>
//    private lateinit var mLikelyAttribs: List<>
    private lateinit var mLikelyPlaceLatLng: Array<LatLng>

    /**
     * Progress UI is only invisible when this number is 0.
     * Every time an asynchronous call is maded, this number
     * is incremented.  Thus multiple asyncs can start, each
     * incrementing this datum by calling [enableProgressUI].
     * The progress bar will only go away when the last thread's
     * return calls [disableProgressUI]
     */
    @Volatile private var mProgressVisible: Int = 0


    //---------------------------
    //  functions
    //---------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        // Retrieve location and camera position from saved instance state.
//        if (savedInstanceState != null) {
//            mLastLocation = savedInstanceState.getParcelable(LOCATION_KEY)
//            mCameraPos = savedInstanceState.getParcelable(CAMERA_POS_KEY)
//        }

        if (checkLocPermissions() == false) {
            return
        }

        completeSetup()
    }


    /**
     * Called to finish initialization, but only after it's
     * certain that the app has the proper permissions.
     */
    private fun completeSetup() {

        setContentView(R.layout.activity_maps)

        // load widgets
        mLoadingProgressBar = findViewById(R.id.progress_bar)
        mLoadingTv = findViewById(R.id.progress_tv)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                mLastLocation = p0.lastLocation
                placeMarkerOnMap(LatLng(mLastLocation.latitude, mLastLocation.longitude))
            }
        }

        createLocationRequest()

        // construct a Places client // todo: is this used yet?
        Places.initialize(this, getString(R.string.google_maps_key))
        mPlacesClient = Places.createClient(this)

        volleyLoadData()
    }


    private fun volleyLoadData() {

        val queue = Volley.newRequestQueue(this)

        val stringRequest = StringRequest(Request.Method.GET, TEST_URL,
            Response.Listener { response ->
                // just show the first bit of the response
                Log.d(TAG, "response: ${response.substring(0, 250)}")
                disableProgressUI()
            },
            Response.ErrorListener {
                Log.e(TAG, "nope")
                disableProgressUI()
            })

        queue.add(stringRequest)
        enableProgressUI()
    }


    /**
     * Adds a marker on the map at the given location.  If there's a previous
     * marker, it's removed.
     */
    private fun placeMarkerOnMap(location: LatLng) {
        // create a marker object using the given location
        val markerOptions = MarkerOptions().position(location)

        // use custom marker
//        val bitmap = BitmapFactory.decodeResource(resources, R.mipmap.ic_user_location)
//        val bitmapDescriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
//        markerOptions.icon(bitmapDescriptor)

        // change color from the default red
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

        // display the address with this marker
        val addressStr = getAddress(location)
        markerOptions.title(addressStr)

        // remove last marker (if it exists)
        mLastMarker?.remove()

        // add this marker to the map
        mLastMarker = mMap.addMarker(markerOptions)
    }


    /**
     * Checks to see if the app has permissions to use fine location.  If not, asks the
     * user for permission.
     *
     * To see if the user actually DID grant permission, check out [onRequestPermissionsResult].
     *
     * @return  True if permission already exists.
     *          False means we are currently asking if the user will grant permission.
     */
    private fun checkLocPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
            PackageManager.PERMISSION_GRANTED) {
            val locPermission = arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
            ActivityCompat.requestPermissions(this, locPermission, LOCATION_PERMISSION_REQUEST_CODE)
            return false
        }
        return true
    }


    /**
     * Creates an asynchronous location request.  Uses a listener that when
     * successful calls [startLocationUpdates].
     */
    private fun createLocationRequest() {

        mLocationRequest = LocationRequest()
        mLocationRequest.interval = 10000
        mLocationRequest.fastestInterval = 5000
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(mLocationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            // listener for when location settings successful
            mLocationUpdateState = true
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null)
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // unable to satisfy location settings. use dialog to get more info
                try {
                    e.startResolutionForResult(this@MapsActivity, REQUEST_CHECK_SETTINGS)
                }
                catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, e.message ?: "error: unable to resolve createLocationRequest error")
                }
            }
        }

    }


    /**
     * Given the latitude & longitude, return a string that describes the
     * address.  Returns empty string if no address found.
     */
    private fun getAddress(latLng: LatLng): String {
        /** object to translate between lat/lon into addresses */
        val geocoder = Geocoder(this)
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // todo: should be in a different thread
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            // convert first address (if any) to a string.
            if (addresses != null && !addresses.isEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex + 1) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        }
        catch (e: IOException) {
            Log.e(TAG, e.localizedMessage ?: "problem getting address")
        }

        return addressText
    }


    @Synchronized
    private fun enableProgressUI() {
        mLoadingTv.visibility = View.VISIBLE
        mLoadingProgressBar.visibility = View.VISIBLE
        mProgressVisible++
    }

    @Synchronized
    private fun disableProgressUI() {
        Log.d(TAG, "disableProgressUI(), mProgressVisible is at $mProgressVisible")
        mProgressVisible--
        if (mProgressVisible <= 0) {
            mLoadingTv.visibility = View.GONE
            mLoadingProgressBar.visibility = View.GONE
            mProgressVisible = 0
        }
    }


    //----------------------------------
    //  callbacks
    //----------------------------------

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setOnMarkerClickListener(this)


        // disable the little blue dot that typically shows your current loc.
        // I'm using a custom that is much easier to see.
        mMap.isMyLocationEnabled = false

        mFusedLocationClient.lastLocation.addOnSuccessListener(this) {
                location ->
            // check for null (it's possible)
            if (location != null) {
                mLastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng) // add our current location marker

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }

    }


    /**
     * called after a dialog has requested permission from the user
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            completeSetup()
        }
        else {
            Toast.makeText(this, R.string.need_fine_location_permission,
                Toast.LENGTH_LONG).show()
            finish()
        }
    }


    /**
     * User has clicked on a marker
     */
    override fun onMarkerClick(marker: Marker?): Boolean {

        return false
    }




}
