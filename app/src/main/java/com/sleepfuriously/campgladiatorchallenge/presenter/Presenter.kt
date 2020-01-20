package com.sleepfuriously.campgladiatorchallenge.presenter

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.LatLng
import com.sleepfuriously.campgladiatorchallenge.model.CGDatum
import org.json.JSONArray

/**
 * All access between data and UI comes through here.
 * Of course, this has to be a singleton.
 *
 * @param   ctx     any old context
 */
class Presenter constructor(ctx: Context){

    //-----------------------
    //  constants
    //-----------------------

    private val TAG = "biggs-Presenter"

    private val TEST_URL = "https://stagingapi.campgladiator.com/api/v2/places/searchbydistance?lat=30.406991&lon=-97.720310&radius=25"
//    public val TEST_URL = "https://stagingapi.campgladiator.com/api/v2/places/searchbydistance?lat=30.406991&lon=-97.720310&radius=2"

    private val BASE_URL = "https://stagingapi.campgladiator.com/api/v2/places/searchbydistance?"

    /** use this to separate components of a URL request */
    private val URL_SEPARATOR = "&"

    /** prefix for latitude portion of URL */
    private val URL_LAT = "lat="

    /** prefix for longitude portion of URL */
    private val URL_LONG = "lon="

    /** prefix for radius portion of URL */
    private val URL_RADIUS = "radius="


    //-----------------------
    //  data
    //-----------------------

    companion object {
        private var mInstance: Presenter? = null


        fun getInstance(ctx: Context) =
            // hmmm, this is dense.  todo: lookup 'also' and 'it'
            mInstance ?: synchronized(this) {
                mInstance ?: Presenter(ctx).also {
                    mInstance = it
                }
            }
    }

    /** only use this context (other contexts may go in/out of existence) */
    private val mCtx = ctx.applicationContext

    /** used by volley */
    private var mRequestQueue: RequestQueue? = null
        get() {
            if (field == null) {
                return Volley.newRequestQueue(mCtx)
            }
            return field
        }


    //-----------------------
    //  functions
    //-----------------------

    /**
     * Request to acquire a list of CGDatums from the CG server.
     *
     * @param   loc     The location to center the requests around
     *
     * @param   radius      Radius of location search
     *
     * @param   successCallback     When a successful request is made, the list
     *                              of CGDatums will be returned here.
     *
     * @param   errorCallback   On an error, this will be called with an error msg.
     */
    fun requestLocations(loc: LatLng,
                         radius: Float,
                         successCallback: (List<CGDatum>) -> Unit,
                         errorCallback: (String) -> Unit ) {

        Log.d(TAG, "requestLocations: loc = $loc, radius = $radius")

        // construct url
        val url = BASE_URL + URL_LAT + loc.latitude + URL_SEPARATOR +
                URL_LONG + loc.longitude + URL_SEPARATOR +
                URL_RADIUS + radius

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            Response.Listener { response ->
                Log.d(TAG, "success, processing through callback")

                // grab the data (it's in a jsonarray)
                val jsonArrayLocations: JSONArray? = response.getJSONArray("data")
                if (jsonArrayLocations == null) {
                    errorCallback("no data found")
                }
                else {
                    // make list
                    val locationList: ArrayList<CGDatum> = ArrayList()
                    for (i in 0 until jsonArrayLocations.length()) {
                        val locJsonObj = jsonArrayLocations.getJSONObject(i)
                        val cgDatum = CGDatum(locJsonObj)
                        locationList.add(cgDatum)
                    }

                    successCallback(locationList)
                }
            },

            Response.ErrorListener { error ->
                Log.e(TAG, "Error in requestLocations--callback unsuccessful")
                errorCallback(error.toString())
            })

        mRequestQueue?.add(jsonObjectRequest)
    }


}

//class ModelWindow constructor(ctx: Context) {
//
//    companion object {
//        @Volatile
//        private var instance: ModelWindow? = null
//
//        fun getInstance(ctx: Context) =
//            instance ?: synchronized(this) {
//                instance ?: ModelWindow(ctx).also {
//                    instance = it
//                }
//            }
//    }
//
//
//    val requestQueue: RequestQueue by lazy {
//        // application context is key, it keeps from leaking the activity
//        Volley.newRequestQueue(ctx.applicationContext)
//    }
//
//    fun <T> addToRequestQueue(req: Request<T>) {
//        requestQueue.add(req)
//    }
//
//
//    /**
//     * Returns the data at the top level of the Server's
//     * response.  This is the whole schbang.
//     */
//    public fun getTopLevel(ctx: Context) {
//
//        val queue: ModelWindow = getInstance(ctx.applicationContext).requestQueue
//    }
//
//}