package com.sleepfuriously.campgladiatorchallenge

import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.PersistableBundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.android.synthetic.main.activity_maps.*


class MapsActivity : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener {

    //---------------------------
    //  constants
    //---------------------------

    //  KEYS
    private val CAMERA_POS_KEY = "camera_pos_key"
    private val LOCATION_KEY = "location_key"

    private val DEFAULT_ZOOM = 13f

    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

    private val MAX_LOC_ENTRIES = 5;


    //---------------------------
    //  data
    //---------------------------

    private val TAG = "MapsActivity"

    private var mMap: GoogleMap? = null

    private var mCameraPos: CameraPosition? = null

    /** entrypoint to Places api */
    private lateinit var mPlacesClient: PlacesClient;

    /** entypoint to Fused Loc Provider */
    private lateinit var mFusedLocProviderClient: FusedLocationProviderClient


    /** last location */
    private lateinit var mLastLocation: Location


    /** default location (sydney). used when location permission is not granted */
    private var mDefaultLocation = LatLng(-33.8523341, 151.2106085)

    private val mLocationPermissionGranted = false


    // for selecting a current place

    private lateinit var mLikelyPlaceNames: Array<String>
    private lateinit var mLikelyPlaceAddresses: Array<String>
//    private lateinit var mLikelyAttribs: List<>
    private lateinit var mLikelyPlaceLatLng: Array<LatLng>



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

        setContentView(R.layout.activity_maps)

        // construct a Places client
        Places.initialize(this, getString(R.string.google_maps_key))
        mPlacesClient = Places.createClient(this)

        // construct FusedLocaitonProvider client
        mFusedLocProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }


    /**
     * Checks if the app has been granted fine location permission.
     * If it hasn't, request such permission from the user.
     */
    private fun setupMapPermissions() {
        if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
            return
        }

        // draws current loc (blue) and enables a button to center the map on current loc
        mMap?.isMyLocationEnabled = true

        // returns the most recent loc available
        mFusedLocProviderClient.lastLocation.addOnSuccessListener(this) {
            location ->
            // got last know location. could be null
            if (location != null) {
                mLastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)

                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
            }
        }

    }


    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {

        val tmpLocalMap = mMap  // necessary extra step because of kotlin's extreme null paranoia
        if (tmpLocalMap != null) {
            outState.putParcelable(CAMERA_POS_KEY, tmpLocalMap.cameraPosition)
            outState.putParcelable(LOCATION_KEY, mLastLocation)
            super.onSaveInstanceState(outState, outPersistentState)
        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.current_place_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId){
//            R.id.option_get_place ->
//                showCurrentPlace()
        }
        return true
    }


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
        mMap?.uiSettings?.isZoomControlsEnabled = true
        mMap?.setOnMarkerClickListener(this)

/*
        // Add a marker in Sydney and move the camera
//        val sydney = LatLng(-34.0, 151.0)

        mMap?.addMarker(MarkerOptions().position(mDefaultLocation).title("somewhere down under"))
//        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        val camUpdate = CameraUpdateFactory.newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM)
        mMap?.animateCamera(camUpdate)

//        mMap.animateCamera(CameraUpdateFactory.newLatLng(sydney))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
*/
        // get user's permission
        setupMapPermissions()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return false
    }


}
