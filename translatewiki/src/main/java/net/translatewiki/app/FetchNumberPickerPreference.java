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
