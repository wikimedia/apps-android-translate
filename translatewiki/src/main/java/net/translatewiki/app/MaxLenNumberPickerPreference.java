package net.translatewiki.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

/**
 * Created by orsa on 22/8/13.
 */
public class MaxLenNumberPickerPreference extends NumberPickerPreference implements SharedPreferences.OnSharedPreferenceChangeListener{
    public MaxLenNumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs,10,200); //define the range of the picker
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
        Integer val = PreferenceManager.getDefaultSharedPreferences(getContext()).getInt(getContext().getString(R.string.max_length_for_message_key),-1);
        if (val!=-1)
            setSummary(String.valueOf(val));
    }

    @Override
    public Integer getStep() { return 10; } //define the step of the picker

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getContext().getString(R.string.max_length_for_message_key)))
            setSummary(String.valueOf(getPersistedInt(initialValue)));
    }
}