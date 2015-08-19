package it.jaschke.alexandria;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by saj on 27/01/15.
 */
public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getFragmentManager().beginTransaction().replace(R.id.container, new SettingsFragment()).commit();
    }
}
