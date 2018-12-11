package org.honk.customer.UI;

import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
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

public class MapsActivity extends RequirementsCheckerActivity {

    private GoogleMap googleMap;

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_LOCATIONS = "locations";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_NAME = "name";
    private static final String TAG_DESCRIPTION = "description";

    ArrayList<Marker> markers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Before loading the map, check the requirements for location detection.
        this.checkRequirementsAndPermissions("android.permission.ACCESS_FINE_LOCATION", PackageManager.FEATURE_LOCATION_GPS);

        /* Failure to check requirements or permissions causes the activity to close, so if we reach this line,
         * we can proceed. */

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync((googleMap) -> {
            /* This callback is triggered when the map is ready to be used.
             * This is where we can add markers or lines, add listeners or move the camera.
             * If Google Play services is not installed on the device, the user will be prompted to install
             * it inside the SupportMapFragment. This method will only be triggered once the user has
             * installed Google Play services and returned to the app. */

            this.googleMap = googleMap;

            // Get the user's location and set up the map.
            new LocationHelper().getCurrentLocation(this, (location) -> {

                // Got last known location. In some rare situations this can be null.
                if (location != null) {
                    LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude());
                    float maxZoomLevel = this.googleMap.getMaxZoomLevel();
                    this.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, Math.min(14f, maxZoomLevel)));

                    // The map is set: add a listener to detect camera movements.
                    this.googleMap.setOnCameraIdleListener(() -> {
                        LatLngBounds bounds = this.googleMap.getProjection().getVisibleRegion().latLngBounds;
                        // TODO Fetch data included within the northeast and southwest boundaries.
                        // Loading nearby locations from the remote server.
                        new LoadLocations(this).execute();
                    });
                }
            });

            // Set min and max zoom level: we don't want to load data for an area that is too large.
            this.googleMap.setMinZoomPreference(12);
        });

        // TODO A link in a placeholder should open google maps to get directions.
    }

    @Override
    protected void handlePermissionDeniedMessageClick() {
        this.finishAffinity();
    }

    public static class LoadLocations extends AsyncTask<String, String, String> {

        /* A weak reference object does not prevent their referents from being made finalizable, finalized, and then reclaimed.
         * In this case, this avoids memory leaks. */
        private WeakReference<MapsActivity> mapsActivity;

        ArrayList<SellerLocation> locationsList;

        LoadLocations(MapsActivity parentActivity) {
            mapsActivity = new WeakReference<>(parentActivity);
        }

        /* Gets all locations from URL. */
        protected String doInBackground(String... args) {

            JSONObject json = JSONParser.makeHttpRequest("https://beced59b-6416-4446-af12-5e35670f307a.mock.pstmn.io/GetNearbyLocations");

            locationsList = new ArrayList<>();

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
            // Update UI from background thread.
            mapsActivity.get().runOnUiThread(new Runnable() {
                public void run() {
                    if (mapsActivity.get().googleMap != null) {
                        // Remove all the markers from the map.
                        ArrayList<Marker> markersToBeRemoved = new ArrayList<>();
                        for (Marker marker : mapsActivity.get().markers) {
                            /* If the popup of this marker is currently shown, the marker must not be
                             * removed, otherwise the popup will be closed. */
                            if (marker.isInfoWindowShown()) {
                                /* Get the corresponding location returned by the web service, if any, and
                                 * remove it. */
                                SellerLocation correspondingLocation = null;
                                for(SellerLocation sellerLocation : locationsList) {
                                    if (sellerLocation.seller.name == marker.getTitle()) {
                                        correspondingLocation = sellerLocation;
                                        break;
                                    }
                                }

                                if (correspondingLocation != null) {
                                    locationsList.remove(correspondingLocation);
                                }
                            } else {
                                marker.remove();
                                markersToBeRemoved.add(marker);
                            }
                        }

                        for (Marker marker : markersToBeRemoved) {
                            mapsActivity.get().markers.remove(marker);
                        }

                        // Add new markers to the map.
                        if (locationsList != null) {
                            for(SellerLocation sellerLocation : locationsList) {
                                mapsActivity.get().markers.add(mapsActivity.get().googleMap.addMarker(new MarkerOptions()
                                        .position(sellerLocation.location)
                                        .title(sellerLocation.seller.name)
                                        .snippet(sellerLocation.seller.description)));
                            }
                        }
                    }
                }
            });
        }
    }
}
