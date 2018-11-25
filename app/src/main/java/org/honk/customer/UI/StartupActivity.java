package org.honk.customer.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import org.honk.customer.PreferencesHelper;

public class StartupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPreferences.getString(PreferencesHelper.PREFERENCE_FIRST_ACCESS, "").equals("")) {
            // This is the first access to the app: launch the explanations activity.
            intent = new Intent(this, FirstAccessActivity.class);
        } else {
            // This is not the first access to the app: launch the map activity.
            intent = new Intent(this, MapsActivity.class);
        }

        startActivity(intent);
        finish();
    }
}
