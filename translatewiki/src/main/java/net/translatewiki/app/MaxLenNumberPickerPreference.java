package net.translatewiki.app;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by orsa on 22/8/13.
 */
public class MaxLenNumberPickerPreference extends NumberPickerPreference{
    public MaxLenNumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs,10,200); //define the range of the picker
    }

    @Override
    public Integer getStep() { return 10; } //define the step of the picker
}
