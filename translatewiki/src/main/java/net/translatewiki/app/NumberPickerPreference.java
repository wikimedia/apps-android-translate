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
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.NumberPicker;

public class NumberPickerPreference extends DialogPreference {

    NumberPicker picker;
    Integer initialValue;
    Integer minVal;
    Integer maxVal;

    public Integer getStep(){ return 1; }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPositiveButtonText("Set");
        setDialogIcon(null);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, Integer minVal, Integer maxVal) {
        super(context, attrs);
        this.minVal = minVal;
        this.maxVal = maxVal;
        setDialogIcon(null);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        picker = (NumberPicker)view.findViewById(R.id.numberPicker);
        picker.setMinValue(minVal);
        picker.setMaxValue(maxVal);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i2) {
                if (i+1==i2)
                    numberPicker.setValue(i+getStep());
                else if (i==i2+1)
                    numberPicker.setValue(i-getStep());
            }
        });
        if (initialValue!=null){
            picker.setValue(initialValue);
        }
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue)
    {
        if ( restorePersistedValue ) {
            int def = ( defaultValue instanceof Number ) ? (Integer)defaultValue
                    : ( defaultValue != null ) ? Integer.parseInt(defaultValue.toString()) : 1;
            initialValue = getPersistedInt(def);
        }
        else initialValue = (Integer)defaultValue;
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        int val = a.getInt(index, 1);
        return val;
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        // When the user selects "OK", persist the new value
        if (positiveResult) {
            int val = picker.getValue();
            persistInt(val);
        }
    }
}
