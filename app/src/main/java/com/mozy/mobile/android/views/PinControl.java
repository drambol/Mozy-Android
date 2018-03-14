package com.mozy.mobile.android.views;

import com.mozy.mobile.android.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class PinControl extends LinearLayout
{
    private int currentBoxIndex;

    private EditText [] mPinEntry;
    private static final int C_PIN = 4;
    private static final int [] EDIT_PIN_TEXT_BOX = {R.id.pinEntry1EditTextBox,
                                                        R.id.pinEntry2EditTextBox,
                                                        R.id.pinEntry3EditTextBox,
                                                        R.id.pinEntry4EditTextBox,
                                                    };
    private static final int buttonIds[] = {
                                            R.id.pinEntryButton0,
                                            R.id.pinEntryButton1,
                                            R.id.pinEntryButton2,
                                            R.id.pinEntryButton3,
                                            R.id.pinEntryButton4,
                                            R.id.pinEntryButton5,
                                            R.id.pinEntryButton6,
                                            R.id.pinEntryButton7,
                                            R.id.pinEntryButton8,
                                            R.id.pinEntryButton9,
                                          };

    public PinControl(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.pin_control, this);

        // Attributes for this LinearLayout.
        // Not defined in XML to avoid having nested LLs.
        this.setOrientation(VERTICAL);
        this.setFocusable(true);

        initControls();

        // Start on the first box.
        currentBoxIndex = 0;
        mPinEntry[0].requestFocus();
    }

    public String getPin()
    {
        String strPin = "";
        for (int i = 0; i < C_PIN; i++)
        {
            strPin += mPinEntry[i].getText().toString();
        }
        return strPin;
    }

    public void clearPin()
    {
        for (int i = 0; i < C_PIN; i++)
        {
            mPinEntry[i].setText("");
        }
        // Reset tracking. (Focus handler will adjust currentBoxIndex.
        mPinEntry[0].requestFocus();
    }

    private void initControls()
    {
        // Hook up the PIN edit control boxes and their handlers
        mPinEntry = new EditText[C_PIN];
        for (int i = 0; i < C_PIN; i++)
        {
            mPinEntry[i] = (EditText)findViewById(EDIT_PIN_TEXT_BOX[i]);

            // Don't show cursor in the edit boxes.
            mPinEntry[i].setCursorVisible(false);

            mPinEntry[i].setOnFocusChangeListener(new View.OnFocusChangeListener()
            {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    // If manual focus is being set, clear out the info and adjust the "current" reference
                    if (hasFocus)
                    {
                        ((EditText)v).setText("");
                        PinControl.this.moveToBox(v);
                    }                
                }
            });
            mPinEntry[i].setOnKeyListener(new OnKeyListener()
            {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event)
                {
                    if ((keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
                            && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        moveToNextBox(1);                        
                    }
                    else if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_UP)
                    {
                        moveToNextBox(-1);                        
                    }
                    return false;
                }
            });
        }

        // Hook up handlers for the number buttons
        for (int i = 0; i < buttonIds.length; i++)
        {
            Button button = (Button)findViewById(buttonIds[i]);
            button.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    handleNumberClick(((Button)v).getText());
                }
            });
        }        
    }

    private void moveToBox(View box)
    {
        for (int i = 0; i < C_PIN; i++)
        {
            if (box == mPinEntry[i])
            {
                currentBoxIndex = i;
                break;
            }
        }        
    }

    private void moveToNextBox(int offset)
    {
        currentBoxIndex += offset;
        if (currentBoxIndex >= C_PIN)
        {
            // Notify any listeners that the 4th number has been entered.
            FireOnPinAvailable();
        }
        else
        {
            // Move to next one...
            if (currentBoxIndex < 0)
            {
                currentBoxIndex = 0;
            }
            mPinEntry[currentBoxIndex].requestFocus();
        }
    }

    private void handleNumberClick(CharSequence numberEntered)
    {
        try {
            mPinEntry[currentBoxIndex].setText(numberEntered);
            moveToNextBox(1);
        } catch (Exception ex) {
        }
    }

    // Notification event for when the PIN is "ready"
    public interface OnPinReadyListener
    {
        public abstract void onPinReady();
    }
    OnPinReadyListener onPinReadyListener = null;
    public void setOnPinReadyListener(OnPinReadyListener listener)
    {
        onPinReadyListener = listener;
    }
    private void FireOnPinAvailable()
    {
        if (onPinReadyListener != null)
        {
            onPinReadyListener.onPinReady();
        }
    }    
}