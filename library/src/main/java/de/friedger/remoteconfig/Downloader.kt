package de.friedger.remoteconfig

import android.content.Context
import android.net.ConnectivityManager

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

import okhttp3.OkHttpClient
import okhttp3.Request

object Downloader {

    /**
     * Checks if there is wifi or mobile connection available
     *
     * @param context The application context
     * @return true if there is network connection available
     */
    fun isNetworkConnection(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    /**
     * Reads a stream and writes it into a string. Closes inputStream when done.
     *
     * @param inputStream The stream to read
     * @return A string, containing stream data
     * @throws java.io.IOException
     */
    fun stringFromStream(inputStream: InputStream): String {
        val encoding = "UTF-8"
        val builder = StringBuilder()
        val reader = BufferedReader(InputStreamReader(inputStream, encoding))
        var line: String
        while (true) {
            line = reader.readLine();
            if (line == null) {
                break;
            }
            builder.append(line)
        }
        reader.close()
        return builder.toString()
    }

    fun readJSONFeedString(urlString: String?): String? {
        if (urlString == null)
            return null

        try {
            val client = OkHttpClient()

            val request = Request.Builder()
                    .url(urlString)
                    .build()

            val response = client.newCall(request).execute()
            return response.body()!!.string()

        } catch (ex: IOException) {
            return ""
        }

    }
}
