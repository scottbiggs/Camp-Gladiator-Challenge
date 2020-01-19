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
        public const val PLACE_ID_KEY = "placeID"
        public const val REGION_ID_KEY = "regionID"
        public const val REGION_SUB_ID_KEY = "subRegionID"
        public const val PLACE_NAME_KEY = "placeName"
        public const val PLACE_DESC_KEY = "placeDesc"
        public const val PLACE_ADD_INFO_KEY = "placeAdditionalInfo"
        public const val PLACE_ADD1_KEY = "placeAddress1"
        public const val PLACE_ADD2_KEY = "placeAddress2"
        public const val PLACE_CITY_KEY = "placeCity"
        public const val PLACE_STATE_KEY = "placeState"
        public const val PLACE_ZIP_KEY = "placeZipcode"
        public const val PLACE_COUNTRY_KEY = "placeCountry"
        public const val PLACE_LAT_KEY = "placeLatitude"
        public const val PLACE_LON_KEY = "placeLongitude"
        public const val PLACE_ACTIVE_KEY = "placeActive"
        public const val DIST_KEY = "distance"
        public const val LOCATION_KEY = "location"
    }

    //------------------
    //  data
    //------------------

    public var placeID: String? = null
    public var regionID: String? = null
    public var subRegionID: String? = null
    public var placeName: String? = null
    public var placeDesc: String? = null
    public var placeAdditionalInfo: String? = null
    public var placeAddress1: String? = null
    public var placeAddress2: String? = null
    public var placeCity: String? = null
    public var placeState: String? = null
    public var placeZipcode: String? = null
    public var placeCountry: String? = null
    public var latitude: Double? = null
    public var longitude: Double? = null
    public var placeActive: Int? = null
    public var distance: Double? = null
    public var locations: ArrayList<CGLocation> = ArrayList<CGLocation>()


    //------------------
    //  functions
    //------------------

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
                _placeLat: Double?,
                _placeLon: Double?,
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
