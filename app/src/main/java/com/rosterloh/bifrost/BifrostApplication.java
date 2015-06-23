package com.rosterloh.bifrost;

import android.app.Application;
import android.app.PendingIntent;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.Logger;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tagmanager.TagManager;
import com.google.android.gms.tagmanager.ContainerHolder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.rosterloh.bifrost.services.DetectedActivitiesIntentService;
import com.rosterloh.bifrost.services.GeofenceTransitionsIntentService;

import java.util.ArrayList;
import java.util.Map;

import static com.rosterloh.bifrost.util.LogUtils.LOGD;
import static com.rosterloh.bifrost.util.LogUtils.LOGE;
import static com.rosterloh.bifrost.util.LogUtils.makeLogTag;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 19/06/2015
 */
public class BifrostApplication extends Application implements
        ConnectionCallbacks, OnConnectionFailedListener, ResultCallback<Status> {

    private static final String TAG = makeLogTag(BifrostApplication.class);
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    private RequestQueue mRequestQueue;
    private static BifrostApplication mInstance;

    public Tracker mTracker;
    public ContainerHolder mContainerHolder;
    public TagManager mTagManager;

    protected ArrayList<Geofence> mGeofenceList;

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
                .addApi(ActivityRecognition.API)
                .build();
        if (!mGoogleApiClient.isConnecting() || !mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }
        // Empty list for storing geofences.
        //mGeofenceList = new ArrayList<>();
        //populateGeofenceList();
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
        if (mGoogleApiClient.isConnecting() || mGoogleApiClient.isConnected()) {
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

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            LOGD(TAG, "Successfully added activity detection.");
        } else {
            LOGE(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void requestActivityUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            LOGE(TAG, getString(R.string.not_connected));
            return;
        }
        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
            mGoogleApiClient,
            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
            getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    public void removeActivityUpdates() {
        if (!mGoogleApiClient.isConnected()) {
            LOGE(TAG, getString(R.string.not_connected));
            return;
        }
        // Remove all activity updates for the PendingIntent that was used to request activity
        // updates.
        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
            mGoogleApiClient,
            getActivityDetectionPendingIntent()
        ).setResultCallback(this);
    }

    public void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.MY_LANDMARKS.entrySet()) {

            mGeofenceList.add(new Geofence.Builder()
                // Set the request ID of the geofence. This is a string to identify this
                // geofence.
                .setRequestId(entry.getKey())

                // Set the circular region of this geofence.
                .setCircularRegion(
                        entry.getValue().latitude,
                        entry.getValue().longitude,
                        Constants.GEOFENCE_RADIUS_IN_METERS
                )

                // Set the expiration duration of the geofence. This geofence gets automatically
                // removed after this period of time.
                .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                // Set the transition types of interest. Alerts are only generated for these
                // transition. We track entry and exit transitions in this sample.
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)

                // Create the geofence.
                .build());
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addgeoFences()
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public void addGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            LOGE(TAG, getString(R.string.not_connected));
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                mGoogleApiClient,
                // The GeofenceRequest object.
                getGeofencingRequest(),
                // A pending intent that that is reused when calling removeGeofences(). This
                // pending intent is used to generate an intent when a matched geofence
                // transition is observed.
                getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }
}
