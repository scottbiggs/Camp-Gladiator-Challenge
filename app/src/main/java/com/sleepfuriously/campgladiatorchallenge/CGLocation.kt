package com.sleepfuriously.campgladiatorchallenge

import org.json.JSONException
import org.json.JSONObject

/**
 * Describes the Location data for the CG server data
 */

public class CGLocation {

    //------------------
    //  constants
    //------------------

    companion object {
        public const val ID_KEY = "ID"

        // todo

        public const val LOCATION_NAME_KEY = "locationName"

        // todo
    }

    //------------------
    //  data
    //------------------

    public lateinit var id: String
    public lateinit var locationName: String



    //------------------
    //  functions
    //------------------

    constructor(_id: String, _locationName: String) {
        id = _id
        locationName = _locationName
    }

    constructor(jsonObject: JSONObject) {
        try {
            id = jsonObject.getString(ID_KEY)
            locationName = jsonObject.getString(LOCATION_NAME_KEY)
        }
        catch (e: JSONException) {
            e.printStackTrace()
        }
    }


}

