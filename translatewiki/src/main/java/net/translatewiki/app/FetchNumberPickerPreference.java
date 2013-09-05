package net.translatewiki.app;

import android.content.Context;
import android.util.AttributeSet;

/**
 * Created by orsa on 22/8/13.
 */
public class FetchNumberPickerPreference  extends NumberPickerPreference{
    public FetchNumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs,1,50); //define the range of the picker
    }
}
