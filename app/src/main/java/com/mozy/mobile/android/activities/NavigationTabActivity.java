package com.mozy.mobile.android.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.activities.adapters.TabsAdapter;
import com.mozy.mobile.android.activities.startup.RequestCodes;
import com.mozy.mobile.android.activities.startup.ResultCodes;
import com.mozy.mobile.android.activities.startup.ServerAPI;
import com.mozy.mobile.android.activities.tasks.GetDevicesTask;
import com.mozy.mobile.android.provisioning.Provisioning;
import com.mozy.mobile.android.service.MozyService;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
import com.mozy.mobile.android.views.ErrorView;

/**
 * Demonstrates combining a TabHost with a ViewPager to implement a tab UI
 * that switches between tabs and also allows the user to perform horizontal
 * flicks to move between the tabs.
 */
public class NavigationTabActivity extends FragmentSecuredActivity{
    TabHost mTabHost;
    ViewPager  mViewPager;
    TabsAdapter mTabsAdapter;


    public static final int HOMESCREENTAB = 0;
    public static final int FILESCREENTAB = 1;
    
    int tabPos = HOMESCREENTAB;
    
    private static FragmentSecuredActivity activityInstance = null;
    
    private static Object setInstanceLock = new Object();
    
    public static void setNavigationTabActivityInstance(FragmentSecuredActivity activityInstance) {
        synchronized(setInstanceLock)
        {
            NavigationTabActivity.activityInstance = activityInstance;
        }
    }

    public static FragmentSecuredActivity getNavigationTabActivityInstance() {
        return NavigationTabActivity.activityInstance;
    }
    
    private static Dialog errDialog = null;
    
 // clears the activity
    public static void clear()
    {
        if (NavigationTabActivity.getNavigationTabActivityInstance() != null)
        {
            NavigationTabActivity.getNavigationTabActivityInstance().finish();
            NavigationTabActivity.setNavigationTabActivityInstance(null);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setNavigationTabActivityInstance(this);
        
        super.setContentView(R.layout.bar_activity_layout);

        ErrorView error_view = (ErrorView)findViewById(R.id.error_view);
        if (error_view != null) {
            error_view.removeViewAt(0);
            LayoutInflater inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.fragment_tab_pager, null);
            error_view.addView(view, 0);
        }
        
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            LogUtil.debug(this, "Loading extras from intent:");
            tabPos = extras.getInt("tabPos");
        }
       

        mTabHost = (TabHost)findViewById(android.R.id.tabhost);
        mTabHost.setup();
        
        mTabHost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);
        
 
        mViewPager = (ViewPager)findViewById(R.id.pager);

        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);
        
        View tabview = createTabView(mTabHost.getContext(), getResources().getString(R.string.menu_home));

        mTabsAdapter.addTab(mTabHost.newTabSpec(getResources().getString(R.string.menu_home)).setIndicator(tabview),
                HomeScreenFragment.class, null);
        
        tabview = createTabView(mTabHost.getContext(), getResources().getString(R.string.menu_files));
        
        mTabsAdapter.addTab(mTabHost.newTabSpec(getResources().getString(R.string.menu_files)).setIndicator(tabview),
                FilesScreenFragment.class, null);

      
        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        else
            mTabHost.setCurrentTab(tabPos);
    }
    

    
    private static View createTabView(final Context context, final String text) 
    {

        View view = LayoutInflater.from(context).inflate(R.layout.tab_bg_layout, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);

        return view;
     }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("tab", mTabHost.getCurrentTabTag());
    }
    
    @Override
    public void onPause() {
        super.onPause();
        tabPos = mTabHost.getCurrentTab();
 
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() 
    {
        super.onDestroy();
        Provisioning.getInstance(this).unregisterListener(this);
        this.mTabsAdapter = null; // set tabs adapter to null
 
        if(errDialog != null)
            errDialog.dismiss();
    }
    
    @Override
    public void onResume()
    {
        super.onResume();
        NavigationTabActivity.getDevicesAsyncTask(this);
    }
    
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_HELP, 0, getResources().getString(R.string.menu_help)).setIcon(android.R.drawable.ic_menu_help);
        menu.add(0, MENU_SETTINGS, 1, getResources().getString(R.string.menu_settings)).setIcon(R.drawable.settings);
        return true;
    }
   
 
    
    public static void getDevicesAsyncTask(final Activity activity)
    {
        final Context context = activity.getApplicationContext();
           
        new GetDevicesTask(context, new GetDevicesTask.Listener() {
            @Override
            public void onCompleted(int errorCode) 
            {  
                NavigationTabActivity instance = (NavigationTabActivity) NavigationTabActivity.getNavigationTabActivityInstance();
                if(instance != null && instance.mTabsAdapter != null)
                {
                    
                    instance.mTabsAdapter.notifyDataSetChanged();
                   
                    // No containers or sync
                    if(errorCode == ServerAPI.RESULT_OK) 
                    {
                        if(SystemState.getDeviceList() != null 
                            && SystemState.getDeviceList().size() == 0)
                        {
                            AlertDialog alertDialog = new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setTitle(R.string.mozy_account_issue)
                            .setMessage(R.string.mozy_account_not_created)
                            .setIcon(context.getResources().getDrawable(R.drawable.error_icon))
                            .setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                } })
                            .create();
                            alertDialog.show();   
                        }
                    }
                    else
                    {
                        if(NavigationTabActivity.getNavigationTabActivityInstance() != null)
                        {
                            errDialog = ((NavigationTabActivity) NavigationTabActivity.getNavigationTabActivityInstance()).createErrorDialog(errorCode);
                            errDialog.show();
                        }
                    }
                }
            }
        }).execute();

    }


    private Dialog createErrorDialog(int errorCode) {
        int errorString = 0;
        int errorTitle = R.string.error;
        int dialogId = ErrorActivity.DIALOG_ERROR_NO_FINISH_ID;

        switch (errorCode)
        {
            case ServerAPI.RESULT_UNAUTHORIZED:
            case ServerAPI.RESULT_FORBIDDEN:
                errorString = R.string.errormessage_authorization_failure; 
                break;
            case ServerAPI.RESULT_INVALID_CLIENT_VER:
                errorString = R.string.client_upgrade_required;
                errorTitle = R.string.client_upgrade_title;
                
              // Stop the Mozy Service
                MozyService.stopMozyService(this.getApplicationContext());
                SystemState.setMozyServiceEnabled(false, this.getApplicationContext());
                SystemState.setManualUploadEnabled(false, this.getApplicationContext());
                
                break;
            case ServerAPI.RESULT_INVALID_TOKEN:
                errorString = R.string.device_revoked_body;
                break;
            case ServerAPI.RESULT_AUTHORIZATION_ERROR:
                errorString = R.string.authorization_error;
                break;
            case ServerAPI.RESULT_INVALID_USER:
                errorString = R.string.invalid_user;
                break;
            case ServerAPI.RESULT_CONNECTION_FAILED:
            case ServerAPI.RESULT_UNKNOWN_PARSER:
            case ServerAPI.RESULT_UNKNOWN_ERROR:
                errorString = R.string.error_not_available;
                break;
            default:
                errorString = R.string.error_not_available;
                break;
        }
        
        if (errorString != 0)
        {  
            final int errorDlgId = (errorString == R.string.device_revoked_body)  ? ErrorActivity.UNAUTHORIZED_USER_DIALOG : dialogId;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(getResources().getDrawable(R.drawable.error_icon));
            builder.setPositiveButton(R.string.ok_button_text, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onButtonClick(errorDlgId, 0);
                }

                private void onButtonClick(int dialogErrorId, int i) {
                    
                    // Wipe Mozy data and signout 
                    if(dialogErrorId == ErrorActivity.UNAUTHORIZED_USER_DIALOG)
                    {
                        MainSettingsActivity.remoteWipeAndSignOff(NavigationTabActivity.this);  
                    }
                }
            });
            builder.setTitle(errorTitle);
            builder.setMessage(errorString);

            return builder.create();
            
        }
        return null;
    }
    
    @Override
    public void onBackPressed() 
    {
       this.setResult(ResultCodes.HOME_SCREEN_BACKPRESSED);
       finish();
    }
    
    /**
     * Returns the user to homescreen. Starts the NavigationTabActivity and
     * clears the activity stack to clear the navigation 'history'.
     */
    public static void returnToHomescreen(Activity activity)
    {
        returnHome(activity, NavigationTabActivity.HOMESCREENTAB);
    }
    
    
    /**
     * Returns the user to filesscreen. Starts the NavigationTabActivity and
     * clears the activity stack to clear the navigation 'history'.
     */
    public static void returnToFilescreen(Activity activity)
    {
        returnHome(activity, NavigationTabActivity.FILESCREENTAB);    
    }
    
    public static void returnHome(Activity activity, int tabId)
    {
        Intent fileScreenIntent = new Intent(activity, NavigationTabActivity.class);
        fileScreenIntent.putExtra("tabPos", tabId);
        fileScreenIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); 
        fileScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivityForResult(fileScreenIntent,  RequestCodes.HOME_SCREEN_ACTIVITY_RESULT);

    }
   
}

