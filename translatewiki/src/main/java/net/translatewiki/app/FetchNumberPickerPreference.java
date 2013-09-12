package net.translatewiki.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

/**
 * Created by orsa on 22/8/13.
 */
public class FetchNumberPickerPreference  extends NumberPickerPreference implements SharedPreferences.OnSharedPreferenceChangeListener{
    public FetchNumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs,1,50); //define the range of the picker
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        Integer val = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(getContext().getString(R.string.fetch_size_key),-1);
        if (val!=-1)
            setSummary(String.valueOf(val));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

        if (s.equals(getContext().getString(R.string.fetch_size_key)))
            setSummary(String.valueOf(getPersistedInt(initialValue)));
    }
}
