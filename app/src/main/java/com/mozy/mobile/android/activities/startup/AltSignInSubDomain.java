package com.mozy.mobile.android.activities.startup;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.provisioning.Provisioning;


public class AltSignInSubDomain extends SignInActivity
{
    private EditText m_eTextSubDomain;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.altsigninpage_domain_layout);

        m_textErrorMessage = (TextView)findViewById(R.id.signinErrorMessageText);
        m_eTextSubDomain = (EditText)findViewById(R.id.alternateSignInSubdomainTextBox);
        
        String subDomain = Provisioning.getInstance(getApplicationContext()).getfedIDSubDomainName();
        
        if(subDomain != null && subDomain.length() != 0)
        {
            m_eTextSubDomain.setText(subDomain);
            m_eTextSubDomain.setHint(subDomain);
        }
        
        m_eTextSubDomain.requestFocus();
       
        
        m_eTextSubDomain.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    String strErrorMessage = validateSubDomain();
                    m_eTextSubDomain.clearFocus();
                    
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(strErrorMessage.trim().length() != 0)
                    {
                        setErrorMessageText(strErrorMessage);
                        imm.hideSoftInputFromWindow(m_eTextSubDomain.getWindowToken(), 0);
                    }
                    else
                    {
                        imm.hideSoftInputFromWindow(m_eTextSubDomain.getWindowToken(), 0);
                        
                        String subDomainStr =  m_eTextSubDomain.getText().toString().trim();
                        
                        // Launches a webview for Horizon
                        Intent intent = new Intent(AltSignInSubDomain.this, AltSignInWebView.class);
                        intent.putExtra("subDomain", subDomainStr);
                        startActivityForResult(intent, RequestCodes.AUTHENTICATION_RESULT);
                    }
                   
                    return true;
                }
                return false;
            }
        });
        
        
        m_eTextSubDomain.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(hasFocus)
                {
                    unSetErrorMessage();
                }

            }
        });
    }
    


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        m_eTextSubDomain.clearFocus();
        
        if(resultCode != ServerAPI.RESULT_CANCELED)
        {
            if((requestCode == RequestCodes.HOME_SCREEN_ACTIVITY_RESULT || 
                    requestCode ==  RequestCodes.AUTHENTICATION_RESULT) && resultCode == ServerAPI.RESULT_UNAUTHORIZED)
            {
                showErrorMessage(R.string.subdomain_not_verified_body);
            }
            else
                super.onActivityResult(requestCode, resultCode, data);
        }
    }


    @Override
    protected void showErrorMessage(int msgId) {
        
        // Clear out the all text entry.
        m_eTextSubDomain.setText("");
        
        super.showErrorMessage(msgId);
    }

    private String validateSubDomain()
    {
        String strSubDomain = m_eTextSubDomain.getText().toString();
        return StartupHelper.validateSubDomain(strSubDomain, getResources());
    }
   
}
