package com.rosterloh.bifrost;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 23/06/2015
 */
public final class Constants {

    private Constants() {
    }

    public static final String PACKAGE_NAME = "com.rosterloh.bifrost";

    public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

    public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";

    /**
     * The desired time between activity detections. Larger values result in fewer activity
     * detections while improving battery life. A value of 0 results in activity detections at the
     * fastest possible rate. Getting frequent updates negatively impact battery life and a real
     * app may prefer to request less frequent updates.
     */
    public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * List of DetectedActivity types that we monitor in this sample.
     */
    protected static final int[] MONITORED_ACTIVITIES = {
        DetectedActivity.STILL,
        DetectedActivity.ON_FOOT,
        DetectedActivity.WALKING,
        DetectedActivity.RUNNING,
        DetectedActivity.ON_BICYCLE,
        DetectedActivity.IN_VEHICLE,
        DetectedActivity.TILTING,
        DetectedActivity.UNKNOWN
    };

    /**
     * Used to set an expiration time for a geofence. After this amount of time Location Services
     * stops tracking the geofence.
     */
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;

    /**
     * For this sample, geofences expire after twelve hours.
     */
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;

    public static final float GEOFENCE_RADIUS_IN_METERS = 50;

    /**
     * Map for storing information about important places.
     */
    public static final HashMap<String, LatLng> MY_LANDMARKS = new HashMap<>();
    static {
        MY_LANDMARKS.put("WORK", new LatLng(51.638025, -0.468819));
        MY_LANDMARKS.put("HOME", new LatLng(51.412520, -0.228371));
    }
}
