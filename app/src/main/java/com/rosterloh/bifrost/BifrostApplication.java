package com.rosterloh.bifrost;

import android.app.Application;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.Logger;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tagmanager.TagManager;
import com.google.android.gms.tagmanager.ContainerHolder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import static com.rosterloh.bifrost.util.LogUtils.LOGD;
import static com.rosterloh.bifrost.util.LogUtils.LOGE;
import static com.rosterloh.bifrost.util.LogUtils.makeLogTag;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 19/06/2015
 */
public class BifrostApplication extends Application implements
        ConnectionCallbacks, OnConnectionFailedListener  {

    private static final String TAG = makeLogTag(BifrostApplication.class);
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    private RequestQueue mRequestQueue;
    private static BifrostApplication mInstance;

    public Tracker mTracker;
    public ContainerHolder mContainerHolder;
    public TagManager mTagManager;

    // Get the Tag Manager
    public TagManager getTagManager () {
        if (mTagManager == null) {
            // create the TagManager, save it in mTagManager
            mTagManager = TagManager.getInstance(this);
        }
        return mTagManager;
    }

    // Set the ContainerHolder
    public void setContainerHolder (ContainerHolder containerHolder) {
        mContainerHolder = containerHolder;
    }

    // Get the ContainerHolder
    public ContainerHolder getContainerHolder() {
        return mContainerHolder;
    }

    /**
     * Get the tracker associated with this app
     */
    public void startTracking() {

        // Initialize an Analytics tracker using a Google Analytics property ID.

        // Does the Tracker already exist?
        // If not, create it

        if (mTracker == null) {
            GoogleAnalytics ga = GoogleAnalytics.getInstance(this);

            // Get the config data for the tracker
            mTracker = ga.newTracker(R.xml.track_app);

            // Enable tracking of activities
            ga.enableAutoActivityReports(this);

            // Set the log level to verbose.
            ga.getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        }
    }

    public Tracker getTracker() {
        // Make sure the tracker exists
        startTracking();

        // Then return the tracker
        return mTracker;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    /**
     * Singleton main method. Provides the global static instance of the helper class.
     * @return The BifrostApplication instance.
     */
    public static synchronized BifrostApplication getInstance() {
        return mInstance;
    }

    /**
     * Provides the general Volley request queue.
     */
    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    /**
     * Adds the request to the general queue.
     * @param req The object Request
     * @param <T> The type of the request result.
     */
    public <T> void add(Request<T> req) {
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    /**
     * Cancels all the pending requests.
     */
    public void cancel() {
        mRequestQueue.cancelAll(TAG);
        if(mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Runs when GoogleApiClient object successfully connects.
     * @param connectionHint Bundled connection data
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if(mLastLocation != null) {
            LOGD(TAG, "Found location as Lat:" + String.valueOf(mLastLocation.getLatitude()) + " Long:"+  String.valueOf(mLastLocation.getLongitude()));
        }
    }

    /**
     * The connection to Google Play services was lost for some reason. We call connect() to
     * attempt to re-establish the connection.
     * @param cause The cause of the disconnect
     */
    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    /**
     * Called if connection to Google Play services fails
     * @param connectionResult The error result
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        LOGE(TAG, "Connection Failed: " + connectionResult.getErrorCode());
    }

    /**
     * Called by Google Play services if the connection to GoogleApiClient drops because of an
     * error.
     */
    public void onDisconnected() {
        LOGD(TAG, "Disconnected");
    }
}
