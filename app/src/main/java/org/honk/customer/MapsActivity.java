package org.honk.customer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.honk.customer.domain.SellerLocation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;

    private static final String TAG_SUCCESS = "success";
    private static final String TAG_LOCATIONS = "locations";
    private static final String TAG_LATITUDE = "latitude";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_NAME = "name";
    private static final String TAG_DESCRIPTION = "description";

    private ProgressDialog progressDialog;

    ArrayList<SellerLocation> locationsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Loading nearby locations from the remote server.
        new LoadLocations().execute();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    public class LoadLocations extends AsyncTask<String, String, String> {
        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MapsActivity.this);
            progressDialog.setMessage("Loading locations. Please wait...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        /**
         * Gets all locations from URL
         * */
        protected String doInBackground(String... args) {

            // getting JSON string from URL
            JSONObject json = JSONParser.makeHttpRequest("https://beced59b-6416-4446-af12-5e35670f307a.mock.pstmn.io/GetNearbyLocations");

            locationsList = new ArrayList<SellerLocation>();

            try {
                // Checking for SUCCESS TAG
                int success = json.getInt(TAG_SUCCESS);

                if (success == 1) {
                    // products found
                    // Getting the array of locations.
                    JSONArray locations = json.getJSONArray(TAG_LOCATIONS);

                    // Looping through all locations.
                    for (int i = 0; i < locations.length(); i++) {
                        JSONObject c = locations.getJSONObject(i);

                        // Storing each json item in a variable.
                        Double latitude = c.getDouble(TAG_LATITUDE);
                        Double longitude = c.getDouble(TAG_LONGITUDE);
                        String name = c.getString(TAG_NAME);
                        String description = c.getString((TAG_DESCRIPTION));

                        // adding HashList to ArrayList
                        locationsList.add(new SellerLocation(name, description, latitude, longitude));
                    }
                } else {
                    // no locations found
                    //TODO Launch search activity
                    Intent i = new Intent(getApplicationContext(), MapsActivity.class);
                    // Closing all previous activities
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        protected void onPostExecute(String file_url) {
            // Dismissing the dialog after getting all locations.
            progressDialog.dismiss();
            // Updating UI from background thread.
            runOnUiThread(new Runnable() {
                public void run() {
                    // Add markers to the map and move the camera.
                    if (googleMap != null) {
                        if (locationsList != null) {
                            for(SellerLocation sellerLocation : locationsList) {
                                googleMap.addMarker(new MarkerOptions()
                                        .position(sellerLocation.location)
                                        .title(sellerLocation.seller.name)
                                        .snippet(sellerLocation.seller.description));
                            }
                        }
                    }

                    LatLng userLocation = new LatLng(45.9711883,12.1673287);
                    float maxZoomLevel = googleMap.getMaxZoomLevel();
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, Math.min(15f, maxZoomLevel)));
                }
            });
        }
    }
}
