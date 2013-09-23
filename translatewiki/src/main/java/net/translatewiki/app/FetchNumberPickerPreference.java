/*
 * @(#)TranslateWikiApp       1.0 15/9/2013
 *
 *  Copyright (c) 2013 Or Sagi.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package net.translatewiki.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.AttributeSet;

/**
 * concrete NumberPickerPreference to handle picking of messages fetch size.
 *
 * @author      Or Sagi
 * @version     %I%, %G%
 * @since       1.0
 */
public class FetchNumberPickerPreference extends NumberPickerPreference
        implements SharedPreferences.OnSharedPreferenceChangeListener{

    public FetchNumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs,1,50); //define the range of the picker
        SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreference.registerOnSharedPreferenceChangeListener(this);
        Integer val = sharedPreference.getInt(context.getString(R.string.fetch_size_key), -1);
        setSummary(String.valueOf( val != -1 ? val : MainActivity.FETCH_SIZE));
    }

    /** {@inheritDoc} */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(getContext().getString(R.string.fetch_size_key)))
            setSummary(String.valueOf(getPersistedInt(initialValue)));
    }
}
