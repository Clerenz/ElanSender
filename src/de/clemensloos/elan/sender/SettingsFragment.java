package de.clemensloos.elan.sender;



import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.widget.Toast;



public class SettingsFragment extends PreferenceFragment {


	SharedPreferences sharedPrefs;
    Preference addressPreference;
    Preference portPreference;


	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.pref_general);
        
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        String address = sharedPrefs.getString(
                getResources().getString(R.string.pref_ip_key),
                getResources().getString(R.string.pref_ip_default));

        addressPreference = findPreference(getResources().getString(R.string.pref_ip_key));
        addressPreference.setSummary(getString(R.string.pref_ip_desc) + " " + address);
        addressPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                addressPreference.setSummary(getString(R.string.pref_ip_desc) + " " + newValue.toString());
                return true;
            }
        });

        String port = sharedPrefs.getString(
        		getResources().getString(R.string.pref_port_key),
        		getResources().getString(R.string.pref_port_default));

        portPreference = findPreference(getResources().getString(R.string.pref_port_key));
        portPreference.setSummary(getString(R.string.pref_port_desc) + " " + port);
        portPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
			    if( !newValue.toString().equals("")  &&  newValue.toString().matches("\\d*") ) {
			        int port = Integer.parseInt(newValue.toString());
			        if( port < 49152 || port > 65535) {
			        	Toast.makeText(SettingsFragment.this.getActivity(), R.string.pref_toast_invalid_port, Toast.LENGTH_LONG).show();
			        	return false;
			        }
			        portPreference.setSummary(getString(R.string.pref_port_desc) + " " + newValue.toString());
			    	return true;
			    }
			    else {
			        Toast.makeText(SettingsFragment.this.getActivity(), R.string.pref_toast_invalid_port, Toast.LENGTH_LONG).show();
			        return false;
			    }
			}
		});
    }

}
