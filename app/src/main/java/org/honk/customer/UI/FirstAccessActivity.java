package org.honk.customer.UI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import org.honk.customer.PreferencesHelper;
import org.honk.customer.R;

public class FirstAccessActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firstaccess);
    }

    public void openMap(View view) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(PreferencesHelper.PREFERENCE_FIRST_ACCESS, "done").apply();
        Intent intent = new Intent(this, MapsActivity.class);
        this.startActivity(intent);
        finish();
    }
}
