package com.mozy.mobile.android.activities.startup;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.ContextMenuActivity;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.UtilFlags;
import com.mozy.mobile.android.web.MipAPI;
import com.mozy.mobile.android.web.MipCommon;
import com.mozy.mobile.android.web.containers.ListDownload;



public class AltSignInWebView extends Activity
{
    WebView mWebView;
    String subDomainStr;
    String mAltSignInUrl;
    ProgressDialog loadingDialog = null;
    
    String redirectUriStr = "mozyapp://mozyHost"; 
    String AuthCodeStr = null;
    String mozyAuthServer = null;
    
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.altsigninpage_webview_layout);
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            LogUtil.debug(this, "Loading extras from intent:");
            subDomainStr = extras.getString("subDomain");
            LogUtil.debug(this, "Alt Sign In Subdomain: " + subDomainStr);
        }
        
       // subDomainStr = "fedid";  // temporary for qa5
       // subDomainStr = "mofi";  // temporary for staging

        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings settings = mWebView.getSettings();
        
        settings.setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new AltSignInWebViewClient());
        
        mWebView.clearHistory();
        mWebView.clearFormData();
        mWebView.clearCache(true);
        mWebView.clearSslPreferences();
        
        settings.setSavePassword(false);
        
        CookieSyncManager.createInstance(this); 
        
        CookieManager cookieManager = CookieManager.getInstance();
        
        if(cookieManager != null)
            cookieManager.removeAllCookie();

        
      //  mWebView.setPadding(0, 0, 0, 0);
      //  mWebView.setInitialScale(100);
        
        // Mozy Auth Server
        
       // mozyAuthServer = "auth.mozy.com";       //"auth2.mozy.com";
       // =   "auth.test.mozy.com" ; //"auth2.mozy.com"; // "10.135.16.141"; // For Testing purpose only  "auth.qa5.mozyops.com"; // ; //  "auth2.mozy.com"; //
        

        
        //UUID guid = UUID.randomUUID();
        
        // https://auth.mozypro.com/<subdomain>/authorize?response_type=code&client_id=<client_id>&redirect_uri=https%3A%2F%2Fauth.mozypro.com%2Foauth%2Fclient_redirect_helper%3Fredirect_uri%3Dmozy%3A
        
        // Mozy Auth Server
        mozyAuthServer =  getResources().getString(R.string.mozy_auth_server);
        
        StringBuilder uriBuilder = new StringBuilder(MipAPI.STR_HTTPS);
        uriBuilder.append(mozyAuthServer);
        uriBuilder.append("/");
        uriBuilder.append(subDomainStr);
        uriBuilder.append("/authorize?");
        uriBuilder.append("response_type=code");
        uriBuilder.append("&");
        uriBuilder.append("client_id=");
        uriBuilder.append(MipCommon.getmMIPClientKey());
        uriBuilder.append("&");
        uriBuilder.append("redirect_uri=");
        uriBuilder.append(URLEncoder.encode(redirectUriStr));
        uriBuilder.append("&");
        uriBuilder.append("state=");
        uriBuilder.append("foobar");
        
        mWebView.loadUrl(uriBuilder.toString());
          
        LogUtil.debug(this, "Url :" + uriBuilder.toString());
        
        showDialog(ContextMenuActivity.DIALOG_LOADING_ID);

    }
    
    @Override
    protected Dialog onCreateDialog(int id) 
    {

        if (id == ContextMenuActivity.DIALOG_LOADING_ID)
        {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage(
                    getResources().getString(R.string.progress_bar_loading));

            loadingDialog.setIndeterminate(true);
        }
           
        return loadingDialog;
   }
    
    @Override
    public void onDestroy()
    {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
        
        super.onDestroy();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            if (loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
        }
        AltSignInWebView.this.setResult(ServerAPI.RESULT_CANCELED);
        return super.onKeyDown(keyCode, event);
    }
    
    
    
    private class AltSignInWebViewClient extends WebViewClient {
        
        boolean timeout;
        
        public AltSignInWebViewClient()
        {
            timeout = true;
        }
        
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);  // sleep for a minute
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if(timeout) 
                    {
                        if (loadingDialog != null && loadingDialog.isShowing()) 
                        {
                            loadingDialog.dismiss();
                        }
                    }
                }
            }).start();
        }


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            LogUtil.debug(this, "Url :" + url);
            
            if(url.startsWith(redirectUriStr + "?"))
            {
                if (loadingDialog != null && loadingDialog.isShowing()) 
                {
                    loadingDialog.dismiss();
                }
                
                int startLength = redirectUriStr.length() + "?code=".length();
                int endLength = "&state=foobar".length();
                AuthCodeStr = url.substring(startLength, url.length() - endLength);
                ListDownload ld = ServerAPI.getInstance(getApplicationContext()).getRequestForToken(mozyAuthServer, subDomainStr, AuthCodeStr, redirectUriStr);
                
                if(ld.errorCode == ServerAPI.RESULT_OK)
                {        
                    ld = ServerAPI.getInstance(getApplicationContext()).getUserInfo();
                    storeSubDomain(subDomainStr);
                }
              
                AltSignInWebView.this.setResult(ld.errorCode);

                finish();
            }
            else
                view.loadUrl(url);
            
            return true;
        }
        
        private void storeSubDomain(String subDomain)
        {
            if(subDomain != null && (subDomain.equalsIgnoreCase("") == false))
            {
                Provisioning.getInstance(getApplicationContext()).setFedIdSubDomainName(subDomain);
            }
        }
        
        @Override
        public void onReceivedError (WebView view, int errorCode, String description, String failingUrl)
        {
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            LogUtil.debug(this, "Error :" + errorCode + ":" + description + " URL:" + failingUrl);
            
        }
        
        @Override
        public
        void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
        {
            LogUtil.debug(this, "Error :" + error.toString());
            if (UtilFlags.isDebugEnabled) {
                handler.proceed();
            } else {
                handler.cancel();
                
                if (error.getUrl().contains(view.getUrl())) {
                    URL url;
                    Intent intent=new Intent();
				    try {
					    url = new URL(view.getUrl());
	                    intent.putExtra("URL", url.getHost());
	                    AltSignInWebView.this.setResult(ServerAPI.RESULT_CERTIFICATE_INVALID, intent);
				    } catch (MalformedURLException e) {
					    e.printStackTrace();
				    }
                
                    finish();
                }
            }
        }
        
        
        @Override
        public void onPageFinished(WebView view, String url) 
        {
            if(url.contains("/login"))  // login screen
            {
                if (loadingDialog != null && loadingDialog.isShowing()) 
                {
                    loadingDialog.dismiss();
                }
            }
            
            timeout = false;
        }    
    }
}
