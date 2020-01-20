package com.sleepfuriously.campgladiatorchallenge.view

import android.content.Context
import android.content.IntentSender
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.sleepfuriously.campgladiatorchallenge.R
import com.sleepfuriously.campgladiatorchallenge.model.CGDatum
import com.sleepfuriously.campgladiatorchallenge.presenter.Presenter
import java.io.IOException


class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    //---------------------------
    //  constants
    //---------------------------

    private val TAG = "biggs-MapsActivity"

    //  KEYS
    private val CAMERA_POS_KEY = "camera_pos_key"
    private val LOCATION_KEY = "location_key"
    private val ZOOM_KEY = "zoom_key"

    private val DEFAULT_ZOOM = 13f

    private val MAX_LOC_ENTRIES = 5;

    private val DEFAULT_REQUEST_INTERVAL: Long = 10000

    private val DEFAULT_FAST_REQUEST_INTERVAL: Long = 5000


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        /** request code passed to onActivityResult() */
        private const val REQUEST_CHECK_SETTINGS = 2
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

    private var mZoom: Float = DEFAULT_ZOOM

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

    /** holds list of all markers that are currently in the map */
    private var mCurrentMarkers: ArrayList<CGDatum> = ArrayList()

    /** default location (sydney). used when location permission is not granted */
    private var mDefaultLocation = LatLng(-33.8523341, 151.2106085)

    private val mLocationPermissionGranted = false

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

        // Lock device in current orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

//        // Retrieve location and camera position from saved instance state.
//        if (savedInstanceState != null) {
//            mZoom = savedInstanceState.getFloat(ZOOM_KEY)
//            mLastLocation = savedInstanceState.getParcelable<Location>(LOCATION_KEY) as Location
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

        val searchEt: EditText = findViewById(R.id.search_bar_et)
        val searchButton: Button = findViewById(R.id.search_btn)

        // Set callback for user search
        searchButton.setOnClickListener {
            val searchText = searchEt.text
            requestCGLocations(searchText.toString())

            (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.apply {
                hideSoftInputFromWindow(searchButton.windowToken, 0)
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        /** callback when location results come in. Will occur periodically. */
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
//                Log.d(TAG, "mLocationCallback.onLocationResult()")

                mLastLocation = locationResult.lastLocation
                placeMyLocationMarkerOnMap(LatLng(mLastLocation.latitude, mLastLocation.longitude))
            }
        }

        createPeriodicLocationRequest()

        // construct a Places client // todo: is this used yet?
        Places.initialize(this, getString(R.string.google_maps_key))
        mPlacesClient = Places.createClient(this)
    }


//    /**
//     * A chance to save data
//     */
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        Log.d(TAG, "onSaveInstanceState() called")
//
//        outState.putParcelable(LOCATION_KEY, mLastLocation)
//        outState.putParcelable(CAMERA_POS_KEY, mCameraPos)
//        outState.putFloat(ZOOM_KEY, mMap.cameraPosition.zoom)
//    }


    /**
     * Initiates server request for locations to add to the map.
     * Results can be found at [locationCallbackSuccess] and
     * [locationCallbackError].
     */
    private fun requestCGLocations() {

        Log.d(TAG, "requestLocations() start")

        val presenter = Presenter(this)
            presenter.requestLocations(LatLng(mLastLocation.latitude, mLastLocation.longitude),
                mZoom,
                this::locationCallbackSuccess,
                this::locationCallbackError)

        enableProgressUI()
    }

    /**
     * initiates server request for locations to add to the map based on user's text.
     */
    private fun requestCGLocations(str: String) {

        Log.d(TAG, "requestLocations($str) start")

        val latLng = getLocFromString(str)
        if (latLng == null) {
            Toast.makeText(this, R.string.unable_to_find_loc_string, Toast.LENGTH_SHORT).show()
            return
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, mZoom))

        val presenter = Presenter(this)
        presenter.requestLocations(latLng, mZoom,
            this::locationCallbackSuccess,
            this::locationCallbackError)

        enableProgressUI()
    }


    /**
     * Adds a bunch of markers at once.
     *
     * @param   locs    An array of locations to add
     *
     * @param   hue     The color to draw for these markers
     */
    private fun placeMarkersOnMap(locs: ArrayList<CGDatum>, hue: Float) {

        // precalc this because it's always the same and rather slow to init
        val bitmapDescriptor = BitmapDescriptorFactory.defaultMarker(hue)

        for (loc in locs) {
            val markerOptions = MarkerOptions()
                .icon(bitmapDescriptor)
                .draggable(false)
                .position(LatLng(loc.latitude, loc.longitude))
                .title(loc.placeName)
            mMap.addMarker(markerOptions)
        }
    }


    /**
     * Adds a special marker on the map at the given location, representing
     * the user's position.  If there's a previous marker, it's removed.
     */
    private fun placeMyLocationMarkerOnMap(location: LatLng) {
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
            ActivityCompat.requestPermissions(this, locPermission,
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }


    /**
     * Creates an asynchronous location request for the user's current location.
     * These occur at specified intervals (approx) and are handled here in lambda functions.
     */
    private fun createPeriodicLocationRequest() {

        mLocationRequest = LocationRequest()
        mLocationRequest.interval = DEFAULT_REQUEST_INTERVAL
        mLocationRequest.fastestInterval = DEFAULT_FAST_REQUEST_INTERVAL
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
                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                }
                catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, e.message ?: "error: unable to resolve createLocationRequest error")
                }
            }
        }

    }


    private fun getLocFromString(str: String): LatLng? {

        val geocoder = Geocoder(this)
        val addresses: List<Address> = geocoder.getFromLocationName(str, 1)    // only use the 1st

        if (addresses.isEmpty()) {
            return null
        }

        return LatLng(addresses[0].latitude, addresses[0].longitude)
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


    /**
     * Signal to user that data is in transit.  May be called multiple
     * times as long as there's a call to [disableProgressUI] for each
     * call.
     */
    @Synchronized
    private fun enableProgressUI() {
        Log.d(TAG, "enableProgressUI")

        mLoadingTv.visibility = View.VISIBLE
        mLoadingProgressBar.visibility = View.VISIBLE
        mProgressVisible++
    }

    /**
     * Removes any animations or layouts that indicate the app is
     * waiting.  Will only disable the progress UI when the last
     * pending wait is complete.
     */
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
                placeMyLocationMarkerOnMap(currentLatLng) // add our current location marker

                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, mZoom))
                requestCGLocations()
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
            Toast.makeText(this,
                R.string.need_fine_location_permission,
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


    /**
     * Callback when server has successfully found data of
     * CG locations.
     */
    private fun locationCallbackSuccess(dataList: List<CGDatum>) {

//        Log.d(TAG, "locationCallbackSuccess start with ${dataList.size} locations")

        // parse the dataList and add it to the map (but only add new items)
        for (cgDatum in dataList) {
            val lat = cgDatum.latitude
            val long = cgDatum.longitude

            // only add if it's not already in our list
            var found = false
            for (inList in mCurrentMarkers) {
                if ((inList.latitude == lat) && (inList.longitude == long)) {
                    found = true
                    break
                }
            }
            if (!found) {
                mCurrentMarkers.add(cgDatum)
            }
        }

        placeMarkersOnMap(mCurrentMarkers, BitmapDescriptorFactory.HUE_GREEN)

        disableProgressUI()
        Log.d(TAG, "locationCallbackSuccess done")
    }

    /**
     * Callback when CG location request resulted in error.
     */
    private fun locationCallbackError(errStr: String) {
        Log.e(TAG, "error in data callback")
        Toast.makeText(this, "Error receiving data", Toast.LENGTH_LONG).show()
        disableProgressUI()
    }

}
