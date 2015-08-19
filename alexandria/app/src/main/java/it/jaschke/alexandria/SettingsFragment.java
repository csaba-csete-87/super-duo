package it.jaschke.alexandria;

import android.os.Bundle;
import android.preference.PreferenceFragment;

/**
 * Created by Csabi on 17-Aug-15.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }
}