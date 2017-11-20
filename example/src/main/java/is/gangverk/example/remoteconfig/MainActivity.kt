package `is`.gangverk.example.remoteconfig

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

import org.json.JSONArray
import org.json.JSONException

import de.friedger.remoteconfig.RemoteConfig
import de.friedger.remoteconfig.RemoteConfig.RemoteConfigListener

class MainActivity : Activity(), RemoteConfigListener {
    private var mStatus: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mStatus = findViewById(R.id.textview_status)
        val rc = RemoteConfig.getInstance()
        rc!!.addRemoteConfigListener(this)

        val remoteString = rc.getString("remoteString")
        val remoteInt = rc.getInt("remoteInt")
        val remoteDeepString = rc.getString("remoteObject.remoteObject0")
        val remoteJsonArray = rc.getString("remoteArray")

        try {
            // Just to show that the jsonArray is valid, I cast the string to a JSONArray object and back
            mStatus!!.text = String.format("" +
                    "Remote string: %s \n" +
                    "Remote int: %d \n" +
                    "Remote deep string: %s \n" +
                    "Remote json array: %s", remoteString, remoteInt, remoteDeepString, JSONArray(remoteJsonArray).toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    override fun onValueUpdated(key: String, value: Any) {

        val remoteString = RemoteConfig.getInstance()!!.getString("remoteString")
        val remoteInt = RemoteConfig.getInstance()!!.getInt("remoteInt")
        val remoteDeepString = RemoteConfig.getInstance()!!.getString("remoteObject.remoteObject0")
        val remoteJsonArray = RemoteConfig.getInstance()!!.getString("remoteArray")

        try {
            // Just to show that the jsonArray is valid, I cast the string to a JSONArray object and back
            mStatus!!.text = String.format("" +
                    "Remote string: %s \n" +
                    "Remote int: %d \n" +
                    "Remote deep string: %s \n" +
                    "Remote json array: %s", remoteString, remoteInt, remoteDeepString, JSONArray(remoteJsonArray).toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

    }

    override fun onConfigComplete() {

    }

    override fun onConfigError(string: String) {

    }
}
