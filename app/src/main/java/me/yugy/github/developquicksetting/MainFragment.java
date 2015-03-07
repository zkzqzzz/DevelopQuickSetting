package me.yugy.github.developquicksetting;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;

import java.io.IOException;

public class MainFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener{

    private CheckBoxPreference mLayoutBorderPreference;
    private CheckBoxPreference mDisplayOverdrawPreference;
    private CheckBoxPreference mProfileGPURenderingPreference;
    private CheckBoxPreference mImmediatelyDestroyActivitiesPreference;
    private BroadcastReceiver mRefreshUIReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.main);
        findPreferences();
        setPreferencesVisibility();

        mRefreshUIReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                removePreferencesListener();
                updatePreferencesState();
                setPreferencesListener();
                if (intent.getBooleanExtra("result", false)) {
                    ((MainActivity)getActivity()).showSnackBar(R.string.set_property_success_prompt);
                } else {
                    ((MainActivity)getActivity()).showSnackBar(R.string.set_property_failed);
                }
            }
        };
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRefreshUIReceiver, new IntentFilter(Conf.ACTION_REFRESH_UI));
    }

    @Override
    public void onResume() {
        super.onResume();
        // Remove previous listener(for onResume() will be called when create and resume),
        // prevent next update will trigger the listener, after state update, set the listener back.
        removePreferencesListener();
        updatePreferencesState();
        setPreferencesListener();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRefreshUIReceiver);
    }

    private void findPreferences() {
        mLayoutBorderPreference = (CheckBoxPreference) findPreference(getString(R.string.key_layout_border));
        mDisplayOverdrawPreference = (CheckBoxPreference) findPreference(getString(R.string.key_display_overdraw));
        mProfileGPURenderingPreference = (CheckBoxPreference) findPreference(getString(R.string.key_profile_gpu_rendering));
        mImmediatelyDestroyActivitiesPreference = (CheckBoxPreference) findPreference(getString(R.string.key_always_destroy_activities));
    }

    private void setPreferencesVisibility() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ((PreferenceCategory)findPreference(getString(R.string.preference_category_key))).removePreference(mProfileGPURenderingPreference);
        }
    }

    private void removePreferencesListener() {
        mLayoutBorderPreference.setOnPreferenceChangeListener(null);
        mDisplayOverdrawPreference.setOnPreferenceChangeListener(null);
        mProfileGPURenderingPreference.setOnPreferenceChangeListener(null);
        mImmediatelyDestroyActivitiesPreference.setOnPreferenceChangeListener(null);
    }

    private void setPreferencesListener() {
        mLayoutBorderPreference.setOnPreferenceChangeListener(this);
        mDisplayOverdrawPreference.setOnPreferenceChangeListener(this);
        mProfileGPURenderingPreference.setOnPreferenceChangeListener(this);
        mImmediatelyDestroyActivitiesPreference.setOnPreferenceChangeListener(this);
    }

    public void updatePreferencesState() {
        if (getActivity() != null) {
            getActivity().invalidateOptionsMenu();
        }
        new RefreshPreferencesStateTask().execute();
    }

    private class RefreshPreferencesStateTask extends AsyncTask<Void, Void, boolean[]> {

        @Override
        protected boolean[] doInBackground(Void... params) {
            if (getActivity() != null) {
                try {
                    long startTime = System.currentTimeMillis();
                    boolean[] result = new boolean[5];
                    result[0] = DeveloperSettings.isAdbEnabled(getActivity());
                    result[1] = DeveloperSettings.isDebugLayoutEnabled();
                    result[2] = DeveloperSettings.isShowOverdrawEnabled();
                    result[3] = DeveloperSettings.isShowProfileGPURendering();
                    result[4] = DeveloperSettings.isImmediatelyDestroyActivities(getActivity());
                    Utils.log("RefreshPreferencesStateTask spends " + (System.currentTimeMillis() - startTime) + "ms in background.");
                    return result;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(boolean[] result) {
            if (getActivity() != null) {
                if (result != null) {
                    setOtherPreferencesEnabled(result[0]);
                    mLayoutBorderPreference.setChecked(result[1]);
                    mDisplayOverdrawPreference.setChecked(result[2]);
                    mProfileGPURenderingPreference.setChecked(result[3]);
                    mImmediatelyDestroyActivitiesPreference.setChecked(result[4]);
                } else {
                    ((MainActivity)getActivity()).showSnackBar(R.string.update_checkbox_state_failed);
                }
            }
        }
    }

    private void setOtherPreferencesEnabled(boolean enabled) {
        mLayoutBorderPreference.setEnabled(enabled);
        mDisplayOverdrawPreference.setEnabled(enabled);
        mProfileGPURenderingPreference.setEnabled(enabled);
        mImmediatelyDestroyActivitiesPreference.setEnabled(enabled);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.equals(mLayoutBorderPreference)) {
            //debug.layout
            DevelopSettingsService.newTask(getActivity(), DevelopSettingsService.ACTION_SET_SHOW_LAYOUT_BORDER);
        } else if (preference.equals(mDisplayOverdrawPreference)) {
            //overdraw
            DevelopSettingsService.newTask(getActivity(), DevelopSettingsService.ACTION_SET_DISPLAY_OVERDRAW);
        } else if (preference.equals(mProfileGPURenderingPreference)) {
            //profile gpu rendering
            DevelopSettingsService.newTask(getActivity(), DevelopSettingsService.ACTION_SET_PROFILE_GPU_RENDERING);
        } else if (preference.equals(mImmediatelyDestroyActivitiesPreference)) {
            //always destroy activities
            DevelopSettingsService.newTask(getActivity(), DevelopSettingsService.ACTION_SET_IMMEDIATELY_DESTROY_ACTIVITIES);
        }
        return false;
    }

}
