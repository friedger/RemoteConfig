package de.friedger.remoteconfig

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.AsyncTask
import android.support.v4.content.LocalBroadcastManager

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UnsupportedEncodingException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLEncoder
import java.util.ArrayList
import java.util.HashMap

class RemoteConfig private constructor() {
    private var mConfigLocation: URL? = null
    private var mUpdateTime: Long = 0
    private var mPreferences: SharedPreferences? = null
    private var mContext: Context? = null
    private var mListeners: ArrayList<RemoteConfigListener>? = null
    private var mVersion: Int = 0
    private var mRemoteConfigAssetFile = REMOTE_CONFIG_ASSET_FILE

    val config: JSONObject?
        get() {
            val completeConfig = getString(COMPLETE_CONFIG_KEY)
            var completeJSON: JSONObject? = null
            try {
                if (completeConfig == null) {
                    return null
                }
                completeJSON = JSONObject(completeConfig)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return completeJSON
        }

    fun setDefaults(configFileUnderAssets: String) {
        if (!isNullOrEmpty(configFileUnderAssets)) {
            mRemoteConfigAssetFile = configFileUnderAssets
        }
    }

    private fun isNullOrEmpty(string: String?): Boolean {
        return string == null || string.length == 0
    }

    /**
     * Use this method to initialize the remote config. Using this init method you would have to have the
     * string named rc_config_location with the config url as value somewhere in your xml files.
     *
     * @param context Can be application context
     * @param version For version control. If this isn't increased with new key/value pairs won't ever be added
     */
    @Synchronized
    fun init(context: Context, version: Int, useDefault: Boolean) {
        init(context, version, useDefault, context.getString(context.resources.getIdentifier("rc_config_location", "string", context.packageName)))
    }

    /**
     * Use this method to initialize the remote config with a custom config location.
     *
     * @param context    Can be application context
     * @param version    For version control. If this isn't increased with new key/value pairs won't ever be added
     * @param useDefault If true then use the assets/rc.json file as default values
     * @param location   The location of the remote config
     */
    @SuppressLint("CommitPrefEdits")
    @Synchronized
    fun init(context: Context, version: Int, useDefault: Boolean, location: String) {
        mContext = context
        mVersion = version
        setConfigImpl(location)
        mUpdateTime = context.resources.getInteger(context.resources.getIdentifier("rc_config_update_interval", "integer", context.packageName)).toLong()
        val oldVersion = mPreferences!!.getInt(SP_VERSION_KEY, -1)
        if (version > oldVersion || BuildConfig.DEBUG) {
            mPreferences!!.edit().clear().apply()
            if (useDefault) {
                initializeConfigFromLocaleFile()
            }
        }
        checkForUpdate() // We'll fetch new config on launch
    }

    @SuppressLint("NewApi")
    private fun initializeConfigFromLocaleFile() {
        // Start with parsing the assets/rc.json file into JSONObject
        val remoteConfig = initialFileToJsonObject()
        if (remoteConfig != null) {
            jsonObjectIntoPreferences(remoteConfig)
        } else {
            throw RuntimeException(String.format("Unable to read local %s file in asset folder. Are you sure it exists in the assets folder?", mRemoteConfigAssetFile))
        }
    }

    private fun setConfigImpl(location: String) {
        val locationUrl: URL
        try {
            locationUrl = URL(location)
        } catch (e: MalformedURLException) {
            throw RuntimeException(String.format("Unable to parse config URL %s", location))
        }

        mConfigLocation = locationUrl
        try {
            mPreferences = mContext!!.getSharedPreferences(URLEncoder.encode(mConfigLocation!!.toString(), "UTF-8"), Context.MODE_PRIVATE)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

    }

    fun setConfig(location: String) {
        setConfigImpl(location)
        val updateNeeded = checkForUpdate()
        if (!updateNeeded) {
            if (mListeners != null && mListeners!!.size > 0) {
                for (listener in mListeners!!) {
                    listener.onConfigComplete()
                }
            }
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Synchronized private fun jsonObjectIntoPreferences(jsonObject: JSONObject) {
        val editor = mPreferences!!.edit()
        editor.putInt(SP_VERSION_KEY, mVersion)
        editor.putString(COMPLETE_CONFIG_KEY, jsonObject.toString())
        val changedKeys = HashMap<String, Any>()
        val allKeys = getAllKeysFromJSONObject(jsonObject, null)
        for (newKey in allKeys) {

            // If the key is inside an inner JSON dictionary it is defined with
            // a dot like dictionary1.dictionary2. That's why we split the string
            // here
            val deepKeys = newKey.split(DEEP_DICTIONARY_SEPARATOR_REGEX.toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()

            var deepDictionary = jsonObject

            for (i in 0 until deepKeys.size - 1) {
                if (deepDictionary.has(deepKeys[i])) {
                    try {
                        deepDictionary = deepDictionary.getJSONObject(deepKeys[i])
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                }
            }
            val key = deepKeys[deepKeys.size - 1]

            try {
                val value = deepDictionary.get(key)
                if (value is JSONArray) {
                    val oldValue = mPreferences!!.getString(newKey, null)
                    val newValue = value.toString()
                    if (newValue != oldValue) {
                        editor.putString(newKey, newValue)
                        changedKeys.put(newKey, newValue)
                    }
                } else if (value is String) {
                    val oldValue = mPreferences!!.getString(newKey, null)
                    if (value != oldValue) {
                        editor.putString(newKey, value)
                        changedKeys.put(newKey, value)
                    }

                } else if (value is Int) {
                    val oldValue = mPreferences!!.getInt(newKey, -1)
                    if (value != oldValue) {
                        editor.putInt(newKey, value)
                        changedKeys.put(newKey, value)
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        editor.apply()
        //Let someone know we have a new value
        val it = changedKeys.keys.iterator()
        if (mListeners != null && mListeners!!.size > 0) {
            for (listener in mListeners!!) {
                while (it.hasNext()) {
                    val key = it.next()
                    listener.onValueUpdated(key, changedKeys[key])
                }
                listener.onConfigComplete()
            }
        }
        LocalBroadcastManager.getInstance(mContext!!).sendBroadcast(Intent(LOCAL_BROADCAST_INTENT))
    }

    fun registerForBroadcast(context: Context, receiver: BroadcastReceiver) {
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, IntentFilter(LOCAL_BROADCAST_INTENT))
    }

    private fun initialFileToJsonObject(): JSONObject? {
        var remoteConfig: JSONObject? = null
        var inputStream: InputStream? = null
        val total = StringBuilder()
        try {
            inputStream = mContext!!.resources.assets.open(mRemoteConfigAssetFile)
            val r = BufferedReader(InputStreamReader(inputStream!!))
            var line: String
            while (true) {
                line = r.readLine()
                if (line == null) {
                    break;
                }
                total.append(line)
            }
            val jsonString = total.toString()
            remoteConfig = JSONObject(jsonString)
        } catch (e: Exception) {
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return remoteConfig
    }

    /**
     * Checks if it is time for update based on the updateTime variable.
     */
    fun checkForUpdate(): Boolean {
        if (RemoteConfig.shouldUpdate(mPreferences, mUpdateTime)) {
            // Fetch the config
            FetchConfigAsyncTask().execute()
            return true
        }
        return false
    }

    /**
     * Takes in the map parameter and returns the mapping if available. If the mapping is not available it
     * returns the default value. If the user has never used this before or there has been a long time since
     * last check for updated config, new config will be downloaded.
     *
     * @param mapping The map parameter to fetch something that should be in the remote config
     * @return Returns the mapping for the parameter from the shared defaults
     */
    fun getString(mapping: String): String? {
        checkForUpdate()
        return mPreferences!!.getString(mapping, null)
    }

    fun getInt(mapping: String): Int {
        checkForUpdate()
        return mPreferences!!.getInt(mapping, -1)
    }

    private fun getAllKeysFromJSONObject(jsonObject: JSONObject, prefix: String?): ArrayList<String> {
        val allKeys = ArrayList<String>()
        val iter = jsonObject.keys()
        while (iter.hasNext()) {
            try {
                val key = iter.next() as String
                val value = jsonObject.get(key)
                var newKey: String? = null
                if (prefix != null) {
                    newKey = prefix + DEEP_DICTIONARY_SEPARATOR + key
                } else {
                    newKey = key
                }
                if (value is JSONObject) {
                    allKeys.addAll(getAllKeysFromJSONObject(jsonObject.get(key) as JSONObject, newKey))
                } else {
                    allKeys.add(newKey)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }

        }
        return allKeys
    }

    /**
     * Adds a listener to the remote config that can react to new values being downloaded
     *
     * @param listener The listener to listen for new config values
     */
    fun addRemoteConfigListener(listener: RemoteConfigListener) {
        if (mListeners == null)
            mListeners = ArrayList()
        mListeners!!.add(listener)
    }

    /**
     * Removes a listener
     *
     * @param listener The listener to remove
     */
    fun removeRemoteConfigListener(listener: RemoteConfigListener) {
        if (mListeners != null) {
            mListeners!!.remove(listener)
        }
    }

    interface RemoteConfigListener {
        /**
         * This method is called when the config has been downloaded and it's values are being put into shared preferences
         *
         * @param key   The key for the new value in shared preferences
         * @param value The updated value
         */
        fun onValueUpdated(key: String, value: Any)

        /**
         * This is called after every new value has been put into shared preferences
         */
        fun onConfigComplete()

        /**
         * In case of error, this is called
         *
         * @param string The error message
         */
        fun onConfigError(string: String)
    }

    private inner class FetchConfigAsyncTask : AsyncTask<Void, Void, JSONObject>() {

        override fun doInBackground(vararg params: Void): JSONObject? {
            try {
                val jsonString = Downloader.readJSONFeedString(mConfigLocation!!.toString()) ?: return null
                return JSONObject(jsonString)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            return null
        }

        override fun onPostExecute(config: JSONObject?) {
            if (config != null) {
                val editor = mPreferences!!.edit()
                editor.putLong(RemoteConfig.LAST_DOWNLOADED_CONFIG_KEY, System.currentTimeMillis())
                editor.apply()
                jsonObjectIntoPreferences(config)
            } else {
                if (mListeners != null) {
                    for (i in mListeners!!.indices) {
                        mListeners!![i].onConfigError("Unable to read remote config")
                    }
                }
            }
        }
    }

    companion object {
        private val LAST_DOWNLOADED_CONFIG_KEY = "lastDownloadedConfig"
        // This is just a dot, since we have regular expression we have to have the backslashes as well
        private val DEEP_DICTIONARY_SEPARATOR_REGEX = "\\."
        private val DEEP_DICTIONARY_SEPARATOR = "."
        private val REMOTE_CONFIG_ASSET_FILE = "rc.json"
        private val SP_VERSION_KEY = "rc_version"
        private val LOCAL_BROADCAST_INTENT = "remote_config_download_complete"
        private val COMPLETE_CONFIG_KEY = "rc_complete_config"


        @SuppressLint("NewApi")
        @Synchronized private fun shouldUpdate(preferences: SharedPreferences?, updateTime: Long): Boolean {
            val lastDownloadedConfig = preferences!!.getLong(RemoteConfig.LAST_DOWNLOADED_CONFIG_KEY, 0)
            return lastDownloadedConfig + updateTime < System.currentTimeMillis() || BuildConfig.DEBUG
        }
    }
}

