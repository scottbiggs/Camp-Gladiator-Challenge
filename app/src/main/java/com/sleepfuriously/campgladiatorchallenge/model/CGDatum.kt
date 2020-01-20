package com.sleepfuriously.campgladiatorchallenge.model

import android.util.Log
import org.json.JSONObject

/**
 * Describes a Place for the CG server data.
 */
class CGDatum {

    //------------------
    //  constants
    //------------------

    private val TAG = "CGDatum"

    companion object {
        const val PLACE_ID_KEY = "placeID"
        const val REGION_ID_KEY = "regionID"
        const val REGION_SUB_ID_KEY = "subRegionID"
        const val PLACE_NAME_KEY = "placeName"
        const val PLACE_DESC_KEY = "placeDesc"
        const val PLACE_ADD_INFO_KEY = "placeAdditionalInfo"
        const val PLACE_ADD1_KEY = "placeAddress1"
        const val PLACE_ADD2_KEY = "placeAddress2"
        const val PLACE_CITY_KEY = "placeCity"
        const val PLACE_STATE_KEY = "placeState"
        const val PLACE_ZIP_KEY = "placeZipcode"
        const val PLACE_COUNTRY_KEY = "placeCountry"
        const val PLACE_LAT_KEY = "placeLatitude"
        const val PLACE_LON_KEY = "placeLongitude"
        const val PLACE_ACTIVE_KEY = "placeActive"
        const val DIST_KEY = "distance"
        const val LOCATION_KEY = "location"
    }

    //------------------
    //  data
    //------------------

    var placeID: String? = null
    var regionID: String? = null
    var subRegionID: String? = null
    var placeName: String? = null
    var placeDesc: String? = null
    var placeAdditionalInfo: String? = null
    var placeAddress1: String? = null
    var placeAddress2: String? = null
    var placeCity: String? = null
    var placeState: String? = null
    var placeZipcode: String? = null
    var placeCountry: String? = null
    var latitude: Double = 0.0
    var longitude: Double = 0.0
    var placeActive: Int? = null
    var distance: Double? = null
    var locations: ArrayList<CGLocation> = ArrayList<CGLocation>()


    //------------------
    //  functions
    //------------------

    @Suppress("unused")
    constructor(_placeID: String?,
                _regionID: String?,
                _subRegionID: String?,
                _placeName: String?,
                _placeDesc: String?,
                _placeAdditionalInfo: String?,
                _placeAddress1: String?,
                _placeAddress2: String?,
                _placeCity: String?,
                _placeState: String?,
                _placeZipcode: String?,
                _placeCountry: String?,
                _placeLat: Double,
                _placeLon: Double,
                _placeActive: Int?,
                _dist: Double?,
                _locations: ArrayList<CGLocation>?) {
        placeID = _placeID
        regionID = _regionID
        subRegionID = _subRegionID
        placeName = _placeName
        placeDesc = _placeDesc
        placeAdditionalInfo = _placeAdditionalInfo
        placeAddress1 = _placeAddress1
        placeAddress2 = _placeAddress2
        placeCity = _placeCity
        placeState = _placeState
        placeZipcode = _placeZipcode
        placeCountry = _placeCountry
        latitude = _placeLat
        longitude = _placeLon
        placeActive = _placeActive
        distance = _dist
        if (_locations == null) {
            locations = ArrayList<CGLocation>()
        }
        else {
            locations = _locations
        }
    }

    constructor(jsonObject: JSONObject) {
        placeID = jsonObject.getString(PLACE_ID_KEY)
        regionID = jsonObject.getString(REGION_ID_KEY)
        subRegionID = jsonObject.getString(REGION_SUB_ID_KEY)
        placeName = jsonObject.getString(PLACE_NAME_KEY)
        placeDesc = jsonObject.getString(PLACE_DESC_KEY)
        placeAdditionalInfo = jsonObject.getString(PLACE_ADD_INFO_KEY)
        placeAddress1 = jsonObject.getString(PLACE_ADD1_KEY)
        placeAddress2 = jsonObject.getString(PLACE_ADD2_KEY)
        placeCity = jsonObject.getString(PLACE_CITY_KEY)
        placeState = jsonObject.getString(PLACE_STATE_KEY)
        placeZipcode = jsonObject.getString(PLACE_ZIP_KEY)
        placeCountry = jsonObject.getString(PLACE_COUNTRY_KEY)
        latitude = jsonObject.getDouble(PLACE_LAT_KEY)
        longitude = jsonObject.getDouble(PLACE_LON_KEY)
        placeActive = jsonObject.getInt(PLACE_ACTIVE_KEY)
        distance = jsonObject.getDouble(DIST_KEY)

        val jsonArray = jsonObject.getJSONArray(LOCATION_KEY)
        for (i in 0 until jsonArray.length()) {
            val cgLocation = CGLocation(jsonArray.getJSONObject(i))
            locations.add(cgLocation)
        }
    }

}
