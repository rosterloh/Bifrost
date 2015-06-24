package com.rosterloh.bifrost.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tagmanager.TagManager;
import com.rosterloh.bifrost.BifrostApplication;
import com.rosterloh.bifrost.Constants;
import com.rosterloh.bifrost.CustomJsonRequest;
import com.rosterloh.bifrost.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;

import static com.rosterloh.bifrost.util.LogUtils.LOGD;
import static com.rosterloh.bifrost.util.LogUtils.LOGE;
import static com.rosterloh.bifrost.util.LogUtils.makeLogTag;

/**
 * @author Richard Osterloh <richard.osterloh.com>
 * @version 1
 * @since 19/06/2015
 */
public class MainActivity extends Activity {

    private static final String TAG = makeLogTag(MainActivity.class);
    ImageView mImageView;
    TextView mTxtDegrees, mTxtWeather, mTxtError;
    FloatingActionButton mFab;

    BifrostApplication helper = BifrostApplication.getInstance();
    int today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
    int mainColor = Color.parseColor("#FF5722");
    SharedPreferences mSharedPref;

    final static String
            FLICKR_API_KEY = "4bf7ab8766886f2ed4468f2a41ae892c",
            IMAGES_API_ENDPOINT = "https://api.flickr.com/services/rest/?format=json&nojsoncallback=1&sort=random&method=flickr.photos.search&" +
                    "tags=mars,planet,rover&tag_mode=all&api_key=",
            RECENT_API_ENDPOINT = "http://marsweather.ingenology.com/v1/latest/",

            SHARED_PREFS_IMG_KEY = "img",
            SHARED_PREFS_DAY_KEY = "day";

    TagManager mTagManager;

    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Make sure that Analytics tracking has started
        ((BifrostApplication) getApplication()).startTracking();

        // Load the TagManager container
        loadGTMContainer();

        // Views setup
        mImageView = (ImageView) findViewById(R.id.main_bg);
        mTxtDegrees = (TextView) findViewById(R.id.degrees);
        mTxtWeather = (TextView) findViewById(R.id.weather);
        mTxtError = (TextView) findViewById(R.id.error);
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                LOGD(TAG, "FAB clicked");
            }
        });

        // Font
        mTxtDegrees.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Lato-light.ttf"));
        mTxtWeather.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/Lato-light.ttf"));

        // SharedPreferences setup
        mSharedPref = getPreferences(Context.MODE_PRIVATE);

        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        // Picture
        if (mSharedPref.getInt(SHARED_PREFS_DAY_KEY, 0) != today) {
            // search and load a random mars pict.
            try {
                searchRandomImage();
            } catch (Exception e) {
                // please remember to set your own Flickr API!
                // otherwise I won't be able to show
                // a random Mars picture
                imageError(e);
            }
        } else {
            // we already have a pict of the day: let's load it!
            loadImg(mSharedPref.getString(SHARED_PREFS_IMG_KEY, ""));
        }

        // Weather data
        loadWeatherData();

    }

    // Load a TagManager container
    public void loadGTMContainer () {
        // Get the TagManager
        mTagManager = ((BifrostApplication) getApplication()).getTagManager();

        // Enable verbose logging
        mTagManager.setVerboseLoggingEnabled(true);
/*
        // Load the container
        PendingResult pending =
                mTagManager.loadContainerPreferFresh("GTM-123456",
                        R.raw.gtm_default);

        // Define the callback to store the loaded container
        pending.setResultCallback(new ResultCallback<ContainerHolder>() {
            @Override
            public void onResult(ContainerHolder containerHolder) {

                // If unsuccessful, return
                if (!containerHolder.getStatus().isSuccess()) {
                    // Deal with failure
                    return;
                }

                // Manually refresh the container holder
                // Can only do this once every 15 minutes or so
                containerHolder.refresh();

                // Set the container holder, only want one per running app
                // We can retrieve it later as needed
                ((BifrostApplication) getApplication()).setContainerHolder(
                        containerHolder);

            }
        }, 2, TimeUnit.SECONDS);*/
    }

    @Override
    protected void onStop() {
        super.onStop();
        // This will tell to Volley to cancel all the pending requests
        helper.cancel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver that informs this activity of the DetectedActivity
        // object broadcast sent by the intent service.
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));
    }

    @Override
    protected void onPause() {
        // Unregister the broadcast receiver that was registered during onResume().
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onPause();
    }

    /**
     * Fetches a random picture of Mars, using Flickr APIs, and then displays it.
     * @throws Exception When a working API key is not provided.
     */
    private void searchRandomImage() throws Exception {
        if (FLICKR_API_KEY.equals(""))
            throw new Exception("You didn't provide a working Flickr API key!");

        CustomJsonRequest request = new CustomJsonRequest
                (Request.Method.GET, IMAGES_API_ENDPOINT+ FLICKR_API_KEY, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // if you want to debug: Log.v(getString(R.string.app_name), response.toString());

                        try {
                            JSONArray images = response.getJSONObject("photos").getJSONArray("photo");
                            int index = new Random().nextInt(images.length());

                            JSONObject imageItem = images.getJSONObject(index);

                            String imageUrl = "http://farm" + imageItem.getString("farm") +
                                    ".static.flickr.com/" + imageItem.getString("server") + "/" +
                                    imageItem.getString("id") + "_" + imageItem.getString("secret") + "_" + "c.jpg";

                            // store the pict of the day
                            SharedPreferences.Editor editor = mSharedPref.edit();
                            editor.putInt(SHARED_PREFS_DAY_KEY, today);
                            editor.putString(SHARED_PREFS_IMG_KEY, imageUrl);
                            editor.apply();

                            // and finally load it
                            loadImg(imageUrl);

                        } catch (Exception e) {
                            imageError(e);
                        }

                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        imageError(error);
                    }
                });

        request.setPriority(Request.Priority.LOW);
        helper.add(request);

    }

    /**
     * Downloads and displays the picture using Volley.
     * @param imageUrl the URL of the picture.
     */
    private void loadImg(String imageUrl) {
        // Retrieves an image specified by the URL, and displays it in the UI
        ImageRequest request = new ImageRequest(imageUrl,
                new Response.Listener<Bitmap>() {
                    @Override
                    public void onResponse(Bitmap bitmap) {
                        mImageView.setImageBitmap(bitmap);
                    }
                }, 0, 0, ImageView.ScaleType.CENTER_CROP, Bitmap.Config.ARGB_8888,
                new Response.ErrorListener() {
                    public void onErrorResponse(VolleyError error) {
                        imageError(error);
                    }
                });

        // we don't need to set the priority here;
        // ImageRequest already comes in with
        // priority set to LOW, that is exactly what we need.
        helper.add(request);
    }

    /**
     * Fetches and displays the weather data of Mars.
     */
    private void loadWeatherData() {

        CustomJsonRequest request = new CustomJsonRequest
                (Request.Method.GET, RECENT_API_ENDPOINT, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        // if you want to debug: Log.v(getString(R.string.app_name), response.toString());
                        try {

                            String minTemp, maxTemp, atmo;
                            int avgTemp;

                            response = response.getJSONObject("report");

                            minTemp = response.getString("min_temp"); minTemp = minTemp.substring(0, minTemp.indexOf("."));
                            maxTemp = response.getString("max_temp"); maxTemp = maxTemp.substring(0, maxTemp.indexOf("."));

                            avgTemp = (Integer.parseInt(minTemp)+Integer.parseInt(maxTemp))/2;

                            atmo = response.getString("atmo_opacity");


                            mTxtDegrees.setText(avgTemp+"°");
                            mTxtWeather.setText(atmo);

                        } catch (Exception e) {
                            txtError(e);
                        }

                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        txtError(error);
                    }
                });

        request.setPriority(Request.Priority.HIGH);
        helper.add(request);

    }

    private void imageError(Exception e) {
        mImageView.setBackgroundColor(mainColor);
        e.printStackTrace();
    }

    private void txtError(Exception e) {
        mTxtError.setVisibility(View.VISIBLE);
        e.printStackTrace();
    }

    /**
     * Returns a human readable String corresponding to a detected activity type.
     */
    public String getActivityString(int detectedActivityType) {
        Resources resources = this.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<DetectedActivity> updatedActivities =
                    intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);

            String strStatus = "";
            for(DetectedActivity thisActivity: updatedActivities){
                strStatus +=  getActivityString(thisActivity.getType()) + thisActivity.getConfidence() + "%\n";
            }
            LOGD(TAG, strStatus);
        }
    }
}