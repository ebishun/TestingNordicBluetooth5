package promosys.com.testingnordicbluetooth5;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;

import java.util.prefs.PreferenceChangeListener;

public class FragmentSettings extends PreferenceFragment{

    private Context context;
    private MainActivity mainActivity;

    //ListPreference prefDuration;
    CheckBoxPreference prefCrc;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.terminal_settings);

        mainActivity = (MainActivity)getActivity();
        context = mainActivity.getApplicationContext();
        initPreference();
    }

    private void initPreference(){
        /*
        prefDuration = (ListPreference)findPreference("timer_duration");
        prefDuration.setOnPreferenceChangeListener(new ListPreference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                Log.i("FragmentSettings","prefDurationChangedTo: " + prefDuration.getEntry());
                if(prefDuration.getEntry().equals("None")){
                    mainActivity.refreshTimer.cancel();
                    mainActivity.isTimerRunning = false;
                    mainActivity.isTimerEnable = false;
                }else {
                    mainActivity.timerDuration = Integer.parseInt(prefDuration.getEntry().toString())*1000;
                    mainActivity.isTimerEnable = true;
                    mainActivity.initTimer();
                }

                return true;
            }
        });

        if(!(prefDuration.getEntry().equals("None"))){
            mainActivity.isTimerEnable = true;
            mainActivity.timerDuration = Integer.parseInt(prefDuration.getEntry().toString())*1000;
            mainActivity.initTimer();
        }else {
            mainActivity.isTimerEnable = false;
        }
        */

        Log.i("FragmentSettings","timerDuration: " + mainActivity.timerDuration);

        prefCrc = (CheckBoxPreference)findPreference("include_crc");
        prefCrc.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                mainActivity.isIncludeCrcLength = prefCrc.isChecked();
                Log.i("fragmentSettings","isIncludeCrcLength: " + mainActivity.isIncludeCrcLength);
                return false;
            }
        });
        mainActivity.isIncludeCrcLength = prefCrc.isChecked();
    }

}
