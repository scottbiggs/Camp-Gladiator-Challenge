package com.sleepfuriously.campgladiatorchallenge.model

import org.json.JSONException
import org.json.JSONObject

/**
 * Describes the Location data for the CG server data
 */

class CGLocation {

    //------------------
    //  constants
    //------------------

    companion object {
        const val ID_KEY = "ID"

        // todo

        const val LOCATION_NAME_KEY = "locationName"

        // todo
    }

    //------------------
    //  data
    //------------------

    lateinit var id: String
    lateinit var locationName: String



    //------------------
    //  functions
    //------------------

    @Suppress("unused")
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

