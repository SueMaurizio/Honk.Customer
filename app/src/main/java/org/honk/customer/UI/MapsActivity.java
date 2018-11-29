package org.honk.customer.UI;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.honk.customer.JSONParser;
import org.honk.customer.R;
import org.honk.customer.domain.SellerLocation;
import org.honk.sharedlibrary.LocationHelper;
import org.honk.sharedlibrary.UI.RequirementsCheckerActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class MapsActivity extends RequirementsCheckerActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_LOCATIONS = "locations";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_NAME = "name";
    private static final String TAG_DESCRIPTION = "description";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Before loading the map, check the requirements for location detection.
        this.checkRequirementsAndPermissions();

        /* Failure to check requirements or permissions causes the activity to close, so if we reach this line,
         * we can proceed. */
        // Loading nearby locations from the remote server.
        new LoadLocations(this).execute();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        /* TODO a click on a placeholder should open google maps to get directions. */
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    @Override
    protected void handlePermissionDeniedMessageClick() {
        this.finishAffinity();
    }

    public static class LoadLocations extends AsyncTask<String, String, String> {

        /* A weak reference object does not prevent their referents from being made finalizable, finalized, and then reclaimed.
         * In this case, this avoids memory leaks. */
        private WeakReference<MapsActivity> mapsActivity;

        private static ProgressDialog progressDialog;

        ArrayList<SellerLocation> locationsList;

        LoadLocations(MapsActivity parentActivity) {
            mapsActivity = new WeakReference<MapsActivity>(parentActivity);
        }

        /**
         * Before starting background threads, show a progress dialog.
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(mapsActivity.get());
            progressDialog.setMessage("");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        /**
         * Gets all locations from URL.
         * */
        protected String doInBackground(String... args) {

            JSONObject json = JSONParser.makeHttpRequest("https://beced59b-6416-4446-af12-5e35670f307a.mock.pstmn.io/GetNearbyLocations");

            locationsList = new ArrayList<SellerLocation>();

            try {
                // Check for SUCCESS TAG.
                if (json.getInt(TAG_SUCCESS) == 1) {
                    // Get the array of locations.
                    JSONArray locations = json.getJSONArray(TAG_LOCATIONS);

                    // Loop through all locations.
                    for (int i = 0; i < locations.length(); i++) {
                        JSONObject c = locations.getJSONObject(i);

                        // Store each JSON item in a variable.
                        Double latitude = c.getDouble(TAG_LATITUDE);
                        Double longitude = c.getDouble(TAG_LONGITUDE);
                        String name = c.getString(TAG_NAME);
                        String description = c.getString((TAG_DESCRIPTION));

                        locationsList.add(new SellerLocation(name, description, latitude, longitude));
                    }

                    // TODO Show a welcome toast with basic directions
                } else {
                    // No locations found.
                    // TODO Show hint toast
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String file_url) {
            // Dismiss the dialog after getting all locations.
            progressDialog.dismiss();

            // Update UI from background thread.
            mapsActivity.get().runOnUiThread(new Runnable() {
                public void run() {
                    // Add markers to the map and move the camera.
                    if (mapsActivity.get().googleMap != null) {
                        if (locationsList != null) {
                            for(SellerLocation sellerLocation : locationsList) {
                                mapsActivity.get().googleMap.addMarker(new MarkerOptions()
                                        .position(sellerLocation.location)
                                        .title(sellerLocation.seller.name)
                                        .snippet(sellerLocation.seller.description));
                            }
                        }
                    }

                    // TODO User's location must be more accurate than this.
                    // Get the user's location.
                    new LocationHelper().getCurrentLocation(mapsActivity.get(), (location) -> {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                            float maxZoomLevel = mapsActivity.get().googleMap.getMaxZoomLevel();
                            mapsActivity.get().googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, Math.min(15f, maxZoomLevel)));
                        }
                    });
                }
            });
        }
    }
}
