package de.friedger.remoteconfig.example.remoteconfig;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import de.friedger.remoteconfig.RemoteConfig;
import de.friedger.remoteconfig.RemoteConfig.RemoteConfigListener;

public class MainActivity extends Activity implements RemoteConfigListener {
    private TextView mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mStatus = (TextView) findViewById(R.id.textview_status);
        RemoteConfig.getInstance().addRemoteConfigListener(this);

        String remoteString = RemoteConfig.getInstance().getString("remoteString");
        int remoteInt = RemoteConfig.getInstance().getInt("remoteInt");
        String remoteDeepString = RemoteConfig.getInstance().getString("remoteObject.remoteObject0");
        String remoteJsonArray = RemoteConfig.getInstance().getString("remoteArray");

        try {
            // Just to show that the jsonArray is valid, I cast the string to a JSONArray object and back
            mStatus.setText(String.format("" +
                            "Remote string: %s \n" +
                            "Remote int: %d \n" +
                            "Remote deep string: %s \n" +
                            "Remote json array: %s"
                    , remoteString, remoteInt, remoteDeepString, new JSONArray(remoteJsonArray).toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onValueUpdated(String key, Object value) {

        String remoteString = RemoteConfig.getInstance().getString("remoteString");
        int remoteInt = RemoteConfig.getInstance().getInt("remoteInt");
        String remoteDeepString = RemoteConfig.getInstance().getString("remoteObject.remoteObject0");
        String remoteJsonArray = RemoteConfig.getInstance().getString("remoteArray");

        try {
            // Just to show that the jsonArray is valid, I cast the string to a JSONArray object and back
            mStatus.setText(String.format("" +
                            "Remote string: %s \n" +
                            "Remote int: %d \n" +
                            "Remote deep string: %s \n" +
                            "Remote json array: %s"
                    , remoteString, remoteInt, remoteDeepString, new JSONArray(remoteJsonArray).toString()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConfigComplete() {

    }

    @Override
    public void onConfigError(String string) {

    }
}
