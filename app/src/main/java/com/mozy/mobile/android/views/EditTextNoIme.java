package com.mozy.mobile.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class EditTextNoIme extends EditText {

    public EditTextNoIme(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override 
    public boolean onCheckIsTextEditor() {
        // This edit box does not allow a soft keyboard.
        return false;
    }       
}