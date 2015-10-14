package com.example.learn_opentok_android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.opentok.android.Session;
import com.opentok.android.Stream;
import com.opentok.android.OpentokError;

public class MainActivity extends AppCompatActivity implements Session.SessionListener{

    public String TAG = "MainActivity";

    private String mApiKey;
    private String mSessionId;
    private String mToken;
    private Session mSession;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO: Use a WEB SERVICE to get the sessionid, apikey, token
        mSessionId = ApiConfig.mSessionID;
        mToken = ApiConfig.mToken;
        mApiKey = ApiConfig.mApiKey;

        initializeSession();
    }

    private void initializeSession() {
        mSession = new Session(this, mApiKey, mSessionId);
        mSession.setSessionListener(this);
        mSession.connect(mToken);
    }
    private void logOpenTokError(OpentokError opentokError) {
        Log.e(TAG, "Error Domain: " + opentokError.getErrorDomain().name());
        Log.e(TAG, "Error Code: " + opentokError.getErrorCode().name());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /* Session Listener methods */

    @Override
    public void onConnected(Session session) {
        Log.i(TAG, "Session Connected");
    }

    @Override
    public void onDisconnected(Session session) {
        Log.i(TAG, "Session Disconnected");
    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        Log.i(TAG, "Stream Received");
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.i(TAG, "Stream Dropped");
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {
        logOpenTokError(opentokError);
    }
}
