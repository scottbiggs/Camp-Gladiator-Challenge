package com.sleepfuriously.campgladiatorchallenge.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Describes the top-level of the json data that the server returns
 */

class CGTopLevel {

    companion object {

        // todo

        public const val SUCCESS_KEY = "success"
        public const val MESSAGE_KEY = "message"
        public const val DATA_KEY = "data"

    }

    public var success: Boolean? = null
    public var message: String? = null
    public var data: ArrayList<CGDatum> = ArrayList()
//    public var data: JSONArray? = null


    constructor(_success: Boolean?, _message: String?, _data: JSONArray?) {
        success = _success
        message = _message

        // todo
//        if (_data != null) {
//            for (i in 0 until _data.length()) {
//                data.add(_data.getJSONArray(i))
//            }
//
//        }

    }

    constructor(jsonObject: JSONObject?) {
        success = jsonObject?.getBoolean(SUCCESS_KEY)
        message = jsonObject?.getString(MESSAGE_KEY)

        // todo
//        data = jsonObject?.getJSONArray(DATA_KEY)
    }

}
