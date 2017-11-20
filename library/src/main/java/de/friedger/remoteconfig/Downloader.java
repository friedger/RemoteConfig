package de.friedger.remoteconfig;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Downloader {

    /**
     * Checks if there is wifi or mobile connection available
     *
     * @param context The application context
     * @return true if there is network connection available
     */
    public static boolean isNetworkConnection(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }

    /**
     * Reads a stream and writes it into a string. Closes inputStream when done.
     *
     * @param inputStream The stream to read
     * @return A string, containing stream data
     * @throws java.io.IOException
     */
    public static String stringFromStream(InputStream inputStream) throws java.io.IOException {
        String encoding = "UTF-8";
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public static String readJSONFeedString(String urlString) {
        if (urlString == null)
            return null;

        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url(urlString)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();

        } catch (IOException ex) {
            return "";
        }
    }
}
