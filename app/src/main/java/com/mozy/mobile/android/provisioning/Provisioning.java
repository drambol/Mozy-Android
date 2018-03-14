/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
 *                                   _           _
 *       _                 _        | |         | |
 *     _| |_ _____  ____ _| |_ _____| |    _____| |__
 *    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
 *      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
 *       \__)_____|\____)  \__)_____)\_)  \_____|____/
 *
 */

package com.mozy.mobile.android.provisioning;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Vector;

import android.content.Context;
import android.content.SharedPreferences;
import com.mozy.mobile.android.R;
import com.mozy.mobile.android.files.DecryptKey;
import com.mozy.mobile.android.files.Device;
import com.mozy.mobile.android.security.Cryptation;
import com.mozy.mobile.android.security.SeedCreator;
import com.mozy.mobile.android.utils.LogUtil;
import com.mozy.mobile.android.utils.SystemState;
/**
 * Contains user data.
 * WARNING: Try to read values from here as rarely as possible. Will cause some delays on frequent usage. 
 * Rather store values in context when it can be certain that data will not change. Services are not 
 * allowed to change data in Provisioning.
 * 
 * @author Daniel Olofsson (daniel.olofsson@tactel.se)
 *
 */
public class Provisioning {

    /**
     * Strings for AUTOMATIC BACKUP settings.
     */
    private static final String PROVISIONING_SEED = "hf2uh2uhf2bncjb2qiu";
    private static final String PROVISIONING_VALUE_TRUE = "provisioning_active";
    private static final String PROVISIONING_VALUE_FALSE = "provisioning_inactive";

    private static final String BACKUP_SETTINGS_SEED = "whfeh2rg32uifgh23uigf23pgf23hfgoc";

    private static final String USERNAME_SEED = "u239u2fn23ofn2389hf2h2rh23hr23hfu32hf2f3ui";

    private Vector<ProvisioningListener> provisioningListeners;

    private static final String STRING_TRUE = "true";
    private static final String STRING_FALSE = "false";

    /**
     * Provisioning variables.
     */
    private String member_email_id = "";
    private String[] member_all_email_ids = null;
    private String member_domainString = "";
    private String member_mipPortString = "";
    private String member_oAuthSecret = "";
    private boolean member_security_mode = true;
//    private boolean member_hidden_files_mode = true;
    private String member_pin = "";
    private Hashtable<String, String>  member_passphrase = new Hashtable<String, String>();
    private Hashtable<String, DecryptKey>  member_decryptKey_hint = new Hashtable<String, DecryptKey>();
    private int member_uploadSettings = 0;
    private boolean member_firstRunShown = false;
    private boolean member_firstRunPinPrompt = false;
    private boolean member_accept_all_certs = true;
    private boolean member_invalid_signin_lock = false;
    private byte[] member_managedKey = null;
    private String member_mip_account_token = null;
    private String  member_mip_account_token_secret = null;
    private String member_fedId_subDomain = "";
    private String  member_user_id = null;


    /**
     * Provisioning data valid
     */
    private boolean valid = false;

    private Context context;

    /**
     * SINGLETON: An instance of Provisioning.
     */
    private static Provisioning instance = null; 

    /**
     * Locks
     */
    private static final Object instance_lock = new Object();
    private static final Object preference_lock = new Object();

    /**
     * Retrieves instance.
     * 
     * @param context Context owned by application.
     * @return Provisioning instance.
     */
    public static Provisioning getInstance(Context context) {
        synchronized (instance_lock) {
            if (instance == null) {
                instance = new Provisioning(context.getApplicationContext());
            }
        }

        return instance;
    }

    private Provisioning(Context context) {
        this.context = context;
        this.valid = false;
        this.provisioningListeners = new Vector<ProvisioningListener>();
        this.validateData();
    }

    /**
     * Used only by SeedCreator.
     * @param context
     * @return
     */
    public static String getUsername(Context context) {
        String value = "";
        if (context != null) {
            getInternalUsername(context);
        }
        return value;
    }

    private synchronized void validateData() {
        if (!valid) {

            member_accept_all_certs = getInternalAcceptCerts(context);
            member_security_mode = getInternalSecurityMode(context);
     //       member_hidden_files_mode = getInternalHiddenFilesMode(context);
            member_pin = getInternalPin(context);
            member_all_email_ids = getInternalAllEmailIds(context);
            member_email_id = getEmailId();
            member_domainString = getDomainName();
            member_mipPortString = getMipPort();
            member_oAuthSecret = getOAuthSecret();
            member_uploadSettings = getInternalUploadSettings(context);
            member_firstRunShown = getInternalFirstRunShown(context);
            member_firstRunPinPrompt = getInternalFirstRunPinPrompt(context);
            member_mip_account_token = getInternalMipAccountToken(context);
            member_mip_account_token_secret = getInternalMipAccountTokenSecret(context);
            member_invalid_signin_lock = getInternalSignInLock(context);
            member_fedId_subDomain = getfedIDSubDomainName();
            member_user_id = getInternalMipAccountUserId(context);
            
            valid = true;
        }
    }


    public void clearData() {
        setSecurityMode(false);
//        setHiddenFilesMode(true);
        setPin("");
        try {
            setUploadSettings(0);
        } catch (Exception e) {
        }
        setFirstRunShown(false);
        setFirstRunPinPrompt(false);
        setSignInLock(false);
        clearPersonalKeys();
        clearManagedKey();
        
        setMipAccountToken("");
        setMipAccountTokenSecret("");
        setMipAccountUserId("");
        
        
    }
    
    
    public void clearManagedKey()
    {  
        synchronized (preference_lock) {
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.remove(context.getString(R.string.PREFS_MANAGED_KEY)); 
            edit.commit();
        }
    }
    
   
    
  //Clears all personal keys 
    void clearPersonalKeys()
    {
         ArrayList<Object> encryptedDeviceList = SystemState.getEncryptedDeviceList();
             
         int numEncryptedDevices = 0;
         
         if(encryptedDeviceList != null)
             numEncryptedDevices = encryptedDeviceList.size();
         
        for(int i = 0; i < numEncryptedDevices; i++)
        {
            String deviceId = ((Device) encryptedDeviceList.get(i)).getId();
            synchronized (preference_lock) {
                SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
                edit.remove("Key"+ deviceId); 
                edit.commit();
            }
            
            removePersonalKeyHint(deviceId);
        }
        
        //Clear hash table
        member_passphrase.clear();
        member_decryptKey_hint.clear();
        
        notifyListeners(ProvisioningListener.PERSONAL_KEY);
    }
    
    
    public void clearEmailIds()
    {  
        synchronized (preference_lock) {
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.remove(context.getString(R.string.PREFS_EMAIL_ID)); 
            edit.commit();
        }
    }
    
    
    /**
     * Retrieves emailId.
     * 
     * @return emailId.
     */
    public synchronized String getEmailId() {
        
        if (member_all_email_ids != null) {
            int size = member_all_email_ids.length;
            member_email_id = member_all_email_ids[size - 1];
        }
        return member_email_id;
    }
    
    
    /**
     * Retrieves All emailIds.
     * 
     * @return emailId.
     */
    public synchronized String[] getAllEmailIds() {
        if (!valid) {
            member_all_email_ids = getInternalAllEmailIds(context);
            int size = member_all_email_ids.length;
            member_email_id = member_all_email_ids[size - 1];
        }
        return member_all_email_ids;
    }

    /**
     * Retrieves user PIN.
     * 
     * @return user PIN.
     */
    public synchronized String getPin() {
        if (!valid) {
            member_pin = getInternalPin(context);
        }
        return member_pin;
    }

    
    /**
     * Retrieves Managed Key.
     * 
     * @return user PIN.
     */
    public synchronized byte[] getManagedKey() {
        
        member_managedKey = getInternalManagedKey(context);
        return member_managedKey;
    }
   
    /**
     * Returns security mode.
     * 
     * @return true if security is enabled, else false if disabled.
     */
    public synchronized boolean getSecurityMode() {
        if (!valid) {
            member_security_mode = getInternalSecurityMode(context);
        }
        return member_security_mode;
    }
    
    
//    /**
//     * Returns security mode.
//     * 
//     * @return true if security is enabled, else false if disabled.
//     */
//    public synchronized boolean getHiddenFilesMode() {
//        if (!valid) {
//            member_hidden_files_mode = getInternalHiddenFilesMode(context);
//        }
//        return member_hidden_files_mode;
//    }

    public synchronized boolean bAcceptAllCertificates() {
        if (!valid) {
            member_accept_all_certs = true; /*getInternalAcceptCerts(context);*/
        }
        else {
            member_accept_all_certs = true;
        }

        return member_accept_all_certs;
    }

    /**
     * Returns domain string.
     * 
     * @return domain String.
     */
    public synchronized String getDomainName() {

        if (!valid) 
        {
            member_domainString = getInternalDomainName(context);
            if("".equals(member_domainString))
            {
                member_domainString = context.getString(R.string.domain);
            }
        }
        return member_domainString;
    }
    
    
    /**
     * Returns sub domain string for fed id sign in.
     * 
     */
    public synchronized String getfedIDSubDomainName() {

        if (!valid) 
        {
            member_fedId_subDomain = getInternalFedIDSubDomainName(context);
        }
        return member_fedId_subDomain;
    }

    public synchronized String getMipPort() {
        if (!valid)
        {
            // Blank is OK.
            member_mipPortString = getInternalMipPort(context);
        }
        return member_mipPortString;
    }
    
    public synchronized String getOAuthSecret() {
        if (!valid)
        {
            member_oAuthSecret = getInternalOAuthSecret(context);
            
            if("".equals(member_oAuthSecret))
            {
                member_oAuthSecret = context.getString(R.string.oauth_secret);
            }
        }
        return member_oAuthSecret;
    }


    public synchronized boolean getSignInLock()
    {
        if (!valid)
        {
            member_invalid_signin_lock = getInternalSignInLock(context);
        }
        return member_invalid_signin_lock;
    }
    
    public String getMipAccountToken()
    {
        if (member_mip_account_token.trim().length() == 0) {
            member_mip_account_token = getInternalMipAccountToken(context);
        }
        return member_mip_account_token;
    }
    
    public String getMipAccountTokenSecret()
    {
        if (member_mip_account_token_secret.trim().length() == 0) {
            member_mip_account_token_secret = getInternalMipAccountTokenSecret(context);
        }
        return member_mip_account_token_secret;
    }
    
    public String getMipAccountUserId()
    {
        if (member_user_id.trim().length() == 0) {
            member_user_id = getInternalMipAccountUserId(context);
        }
        return member_user_id;
    }
    
    
    public synchronized  String getPassPhraseForContainer(String containerId) 
    {   
        if(getPassPhrase() != null && getPassPhrase().isEmpty() == false)
            return getPassPhrase().get(containerId);
        else
            return null;
    }

    /**
     * Retrieves user Personal Key.
     * 
     * @return user PersonalKey.
     */
    public synchronized  Hashtable<String, String> getPassPhrase() {
        
        member_passphrase =  getInternalPassPhrase(context);
       
        return member_passphrase;
    }
    
    
    public synchronized  DecryptKey getKeyHintForContainer(String containerId) 
    {   
        if(getPersonalKeyHint() != null)
            return getPersonalKeyHint().get(containerId);
        else
            return null;
    }
    
    /**
     * Retrieves Key Hint.
     * 
     * @return user PersonalKey.
     */
    public synchronized Hashtable<String, DecryptKey> getPersonalKeyHint() 
    {
        member_decryptKey_hint = getInternalKeyHint(context);

        return member_decryptKey_hint;
    }
    
    public synchronized int getUploadSettings()
    {
        if (!valid)
        {
            member_uploadSettings = getInternalUploadSettings(context);
        }
        return member_uploadSettings;
    }

    
    public synchronized boolean getFirstRunPinPrompt() {
        if (!valid) {
            member_firstRunPinPrompt = getInternalFirstRunPinPrompt(context);
        }
        return member_firstRunPinPrompt;
    }

    /**
     * Sets emailId.
     * @param email_id new emailId.
     */
    public void setEmailId(String email_id) {
        
        String setVal = null;

        synchronized (preference_lock) {
            setVal = insertEmailInAllEmailStr(email_id);
            String value = "";
            if(setVal != null)
            {
                try {
                    value = Cryptation.encrypt(USERNAME_SEED, setVal);
                } catch (Exception e) {
                    value = "";
                    LogUtil.exception("Provisioning", "setEmailId", e);
                }
              
                SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
                edit.putString(context.getString(R.string.PREFS_EMAIL_ID), value);
            
                edit.commit();
            }
        }
        synchronized (this) {       
            member_all_email_ids = getInternalAllEmailIds(context);;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }

    /**
     * @param email_id
     * @param setVal
     * @param isExists
     * @return
     */
    public String insertEmailInAllEmailStr(String email_id) 
    {
        String[] emails = null;
        String setVal = null;
        
        final int maxEmails = 5;
        
        boolean isExists = false;
        emails = getInternalAllEmailIds(context);
        
        if(email_id == "") 
        {
            if(emails != null)
            {
                // Remove the last stored email id that would be in end of the string
                
                for(int i = 0; i < emails.length - 1; i++)
                {
                    if(setVal == null)
                    {
                        setVal = emails[i];
                    }
                    else
                    {                 
                        setVal = setVal + "|" + emails[i] ;
                    }
                }
            }
        }
        else
        {
            if(emails != null)
            {
                // check email_id  exists in emails
                
                for(int i = 0; i < emails.length; i++)
                {
                    if(emails[i].equalsIgnoreCase(email_id) == true)
                    {
                        String[] emails_tmp = new String[emails.length];
                        
                        // Reprioritize bringing it to the end in the email_tmp array
                        
                        emails_tmp[emails.length -1] = email_id;
                        
                        for(int j = 0, k = 0;  k < emails.length -1; k++)
                        {
                            // copy all except the one matched
                            if(i != j)
                            {
                               emails_tmp[k] = emails[j];
                               j++;
                            }
                            else
                            {
                                j++;   // skip this one that matched without copying
                            }
                        }

                        emails = emails_tmp;
                        
                        // Seperate with | marker
                        
                        
                        for(int p =0; p < emails.length; p++)
                        {
                            if(setVal == null)
                            {
                                setVal = emails[p];
                            }
                            else
                            {                 
                                setVal = setVal + "|" + emails[p] ;
                            }
                        }
                        isExists = true;
                        break;
                    }
                }
                
                
                if(isExists == false)
                {
                    if(emails.length == maxEmails )
                    {
                        // Rearrange and drop the first one that is the oldest
                        setVal = emails[1] + "|" + emails[2] +"|" + emails[3] + "|" + emails[4] + "|" + email_id;
                    }
                    else if(emails.length > 0 && emails.length < maxEmails)
                    {
                        for(int i = 0; i < emails.length; i++)
                        {
                            if(setVal == null)
                            {
                                setVal = emails[i];
                            }
                            else
                            {                 
                                setVal = setVal + "|" + emails[i] ;
                            }
                        }
                        
                        setVal = setVal + "|" + email_id;
                    }
                    else if (emails.length == 0)
                    {
                        setVal = email_id;
                    }
                }
            }
            else
            {
                setVal = email_id;
            }
            
        }
        return setVal;
    }
    
    
    /**
     * @param token
     */
    protected void setStringPreference(int resId, String resValue) {
        synchronized (preference_lock) {
            String value = "";
            try {
                value = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), resValue);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "setStringPreference", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(resId), value);
            edit.commit();
        }
    }
    
    protected void setBooleanPreference(int resId, boolean resValue) {
    
        synchronized (preference_lock) {
            String value = "";
            try {
                value = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), resValue? STRING_TRUE : STRING_FALSE);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "setBooleanPreference", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(resId), value);
            edit.commit();
        }
    }


    /**
     * Sets Http mode to true of false.
     * @param bHttpMode true if http requests should be enable, else false.
     */

    public void setAcceptAllCertificates(boolean bAcceptAllCerts)
    {
        
        setBooleanPreference(R.string.PREFS_ACCEPTALLCERTS, bAcceptAllCerts);
        
        synchronized(this){
            member_accept_all_certs = bAcceptAllCerts;
        }
    }

    /**
     * Sets PIN security as active or inactive.
     * @param security_active true if PIN security should be active, else false.
     */
    public void setSecurityMode(boolean security_active) {
//        boolean hasChanged = false;
//        if (getSecurityMode() != security_active) {
//            hasChanged = true;
//        }
        synchronized (preference_lock) {
            String value = "";
            if (security_active) {
                value = PROVISIONING_VALUE_TRUE;
            } else {
                value = PROVISIONING_VALUE_FALSE;
            }
            try {
                value = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), value);
            } catch (Exception e) {
                value = "0";
                LogUtil.exception("Provisioning", "setSecurityMode", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(R.string.PREFS_SECURITY_MODE), value);
            edit.commit();
        }
        synchronized (this) {
            member_security_mode = security_active;
        }
//        if (hasChanged) {
//            notifyListeners(ProvisioningListener.HIDDEN_FILES_MODE);
//        }
    }
   
    
//    public void setHiddenFilesMode(boolean turnedOn) {
//        boolean hasChanged = false;
//        if (getHiddenFilesMode() != turnedOn) {
//            hasChanged = true;
//        }
//        synchronized (preference_lock) {
//            String value = "";
//            if (turnedOn) {
//                value = PROVISIONING_VALUE_TRUE;
//            } else {
//                value = PROVISIONING_VALUE_FALSE;
//            }
//            try {
//                value = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), value);
//            } catch (Exception e) {
//                value = "0";
//                LogUtil.exception("Provisioning", "setHiddenFilesMode", e);
//            }
//            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
//            edit.putString(context.getString(R.string.PREFS_HIDDEN_FILE_MODE), value);
//            edit.commit();
//        }
//        synchronized (this) {
//            member_hidden_files_mode = turnedOn;
//        }
//        if (hasChanged) {
//            notifyListeners(ProvisioningListener.HIDDEN_FILES_MODE);
//        }
//    }
    
    
    public void setMipAccountToken(String token) {

        setStringPreference(R.string.PREFS_MIP_ACCOUNT_TOKEN, token);
        synchronized (this) {
            member_mip_account_token = token;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }

   
    public void setMipAccountTokenSecret(String tokenSecret) {

        setStringPreference(R.string.PREFS_MIP_ACCOUNT_TOKEN_SECRET, tokenSecret);
        
        synchronized (this) {
            member_mip_account_token_secret = tokenSecret;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }
    
    public void setMipAccountUserId(String userId) {

        setStringPreference(R.string.PREFS_USERID, userId);
        synchronized (this) {
            member_user_id = userId;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }
    
    
    /**
     * 
     */
    public static void saveTokenAndSecret(Provisioning provisioning, String token, String secret) {
        provisioning.setMipAccountToken(token);
        provisioning.setMipAccountTokenSecret(secret);
    }

    /**
     * Sets domain.
     * @param strDomain new strDomain.
     */
    public void setDomainName(String strDomain) {
        
        setStringPreference(R.string.PREFS_DOMAINSTRING, strDomain);
       
        synchronized (this) {
            member_domainString = strDomain;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }

    public void setMipPort(String strMipPort) {
        
        setStringPreference(R.string.PREFS_MIPPORTSTRING, strMipPort);
        
        synchronized (this) {
            member_mipPortString = strMipPort;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }
    
    public void setOAuthSecret(String strOAuthSecret) {
        
        setStringPreference(R.string.PREFS_OAUTHSECRET, strOAuthSecret);
        
        synchronized (this) {
            member_oAuthSecret = strOAuthSecret;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }

    public void setSignInLock(boolean bSignInLock)
    {
        setBooleanPreference(R.string.PREFS_INVALIDSIGNINLOCK, bSignInLock);
       
        synchronized(this){
            member_invalid_signin_lock = bSignInLock;
        }
    }
    
    /**
     * Sets sub domain for fed id.
     * @param strDomain new strDomain.
     */
    public void setFedIdSubDomainName(String strDomain) {
        
        setStringPreference(R.string.PREFS_FEDID_SUBDOMAIN, strDomain);
       
        synchronized (this) {
            member_fedId_subDomain = strDomain;
        }
        notifyListeners(ProvisioningListener.ACCOUNT_INFO_CHANGE);
    }

    /*
     * Set all the upload settings at once, and fire just one notification to listeners.
     */
    public void setUploadSettings(int uploadSettings) 
    {
        synchronized (this) 
        {
            String value = "";
            try {
                value = Cryptation.encrypt(BACKUP_SETTINGS_SEED, Integer.toString(uploadSettings));
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "setUploadSettings", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(R.string.PREFS_UPLOAD_SETTINGS), value);
            edit.commit();
        }
        synchronized (this) {
            member_uploadSettings = uploadSettings;
        }

        notifyListeners(ProvisioningListener.UPLOAD);
    }

  
    public void setFirstRunPinPrompt(boolean firstRunPinPrompt)
    {
        synchronized (preference_lock) 
        {
            String value = "";
            
            try {
                 value = Cryptation.encrypt(BACKUP_SETTINGS_SEED, firstRunPinPrompt ? STRING_TRUE : STRING_FALSE);
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "setFirstRunPinPrompt", e);
                return;
            }
            
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(R.string.PREFS_FIRST_RUN_PIN_PROMPT), value);
            edit.commit();
          }
         
          synchronized(this)
          {
              member_firstRunPinPrompt = firstRunPinPrompt;
          }
     }

     public void setFirstRunShown(boolean firstRunShown)
     {
         synchronized (preference_lock) {
              String value = "";
              try {
                  value = Cryptation.encrypt(BACKUP_SETTINGS_SEED, firstRunShown ? STRING_TRUE : STRING_FALSE);
              } catch (Exception e) {
                  LogUtil.exception("Provisioning", "setFirstRunShown", e);
                  return;
              }
              SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
              edit.putString(context.getString(R.string.PREFS_FIRST_RUN_SHOWN), value);
              edit.commit();
           }
            
          synchronized(this)
          {
              member_firstRunShown = firstRunShown;
          }
       }

    public synchronized boolean firstRunShown() {
        if (!valid) {
            member_firstRunShown = getInternalFirstRunShown(context);
        }
        return member_firstRunShown;
    }


    /**
     * Sets new pin.
     * @param pin new pin.
     */
    public void setPin(String pin) {
        boolean hasChanged = false;
        if (!getPin().equals(pin)) {
            hasChanged = true;
        }
        synchronized (preference_lock) {
            String value = "";
            try {
                value = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), pin);
            } catch (Exception e) {
                value = "0";
                LogUtil.exception("Provisioning", "setPin", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(R.string.PREFS_PIN), value);
            edit.commit();
        }
        synchronized (this) {
            member_pin = pin;
        }
        if (hasChanged) {
            notifyListeners(ProvisioningListener.PIN);
        }
    }
      
   
    /**
     * @param containerId
     * @param personalKey
     */
    public void setPassPhraseForContainer(String containerId, String personalKey) {
        
        boolean hasChanged = false;
        
        String passphrase = getPassPhraseForContainer(containerId);
        
        if (personalKey.equals(passphrase) == false) {
            hasChanged = true;
       
            synchronized (preference_lock) {
                String value = "";
                try {
                    value = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), personalKey);
                } catch (Exception e) {
                    LogUtil.exception("Provisioning", "setPersonalKey", e);
                }
                SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
                edit.putString("Key"+ containerId, value);
                edit.commit();
            }
            synchronized (this) {
                member_passphrase.put(containerId, personalKey);
             }
             if (hasChanged) {
                 notifyListeners(ProvisioningListener.PERSONAL_KEY);
             }
        }
    }

    /**
     * @param containerId
     */
    public void removePersonalKeyHint( String containerId) {
        
        synchronized (preference_lock) {          
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.remove("Scheme_KeyHint_"+ containerId);
            edit.remove("key_KeyHint_"+ containerId);
            
            edit.commit();
        }
        notifyListeners(ProvisioningListener.PERSONAL_KEY_HINT);
    }
   
 
    /**
     * @param containerId
     * @param Keyhint
     */
    public void setPersonalKeyHint( String containerId, DecryptKey Keyhint) {
        boolean hasChanged = false;
        
        
        if (Keyhint.equals(getKeyHintForContainer(containerId)) == false) {
            hasChanged = true;
        }
        synchronized (preference_lock) {
            String valueScheme = "";
            String valueKey = "";
            String valueStrKey = "";
            try {

                valueScheme = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), Keyhint.get_scheme());
                
                valueStrKey= Cryptation.byteArrayToHexString(Keyhint.get_key());
                valueKey = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED),valueStrKey);
                
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "setPersonalKey", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString("Scheme_KeyHint_"+ containerId, valueScheme);
            edit.putString("key_KeyHint_"+ containerId, valueKey);
            
            edit.commit();
        }
        synchronized (this) {
           member_decryptKey_hint.put(containerId, Keyhint);
        }
        if (hasChanged) {
            notifyListeners(ProvisioningListener.PERSONAL_KEY_HINT);
        }
    }
    
    
    
    public void setManagedKey(byte [] managed_key) {
        
        synchronized (preference_lock) {
            String valueManagedKey = "";
            String valueKey = "";
            
            try {
                valueManagedKey= Cryptation.byteArrayToHexString(managed_key);
                valueKey = Cryptation.encrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED),valueManagedKey);
                
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "setManagedKey", e);
            }
            SharedPreferences.Editor edit = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE).edit();
            edit.putString(context.getString(R.string.PREFS_MANAGED_KEY), valueKey);
            
            edit.commit();
        }
        synchronized (this) {
            member_managedKey  = managed_key;
        }
    }
    
    
    private static byte[] getInternalManagedKey(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String managedKey = prefs.getString(context.getString(R.string.PREFS_MANAGED_KEY), "");
        String value = "";
        byte[] theByteArray = null;
        
        if(managedKey != null && managedKey.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), managedKey);
                theByteArray = Cryptation.getByteFromHex(value);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalManagedKey", e);
            }
        }
        
        return theByteArray;
    }
    
    private static int getInternalUploadSettings(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String uploadSettings_encrypted = prefs.getString(context.getString(R.string.PREFS_UPLOAD_SETTINGS), "");
        String uploadSettings_decrypted = "";
        int val = 0;

        
        if(uploadSettings_encrypted != null && uploadSettings_encrypted.length() != 0)
        {
            try {
                uploadSettings_decrypted = Cryptation.decrypt(BACKUP_SETTINGS_SEED, uploadSettings_encrypted);
            } catch (Exception e) {
                uploadSettings_decrypted = "0";
                LogUtil.exception("Provisioning", "getInternalUploadSettings", e);
            }
            val = Integer.valueOf(uploadSettings_decrypted).intValue();
        }

        return val;
    }
    
    private static boolean getInternalFirstRunShown(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String firstRunShown_encrypted = prefs.getString(context.getString(R.string.PREFS_FIRST_RUN_SHOWN), "");
        String firstRunShown_decrypted = "";

        if(firstRunShown_encrypted != null && firstRunShown_encrypted.length() != 0)
        {
            try {
                firstRunShown_decrypted = Cryptation.decrypt(BACKUP_SETTINGS_SEED, firstRunShown_encrypted);
            } catch (Exception e) {
                firstRunShown_decrypted = STRING_FALSE;
                LogUtil.exception("Provisioning", "getInternalFirstRunShown", e);
            }
        }

        return STRING_TRUE.equalsIgnoreCase(firstRunShown_decrypted);
    }
    
    private static boolean getInternalFirstRunPinPrompt(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String firstRunPinPrompt_encrypted = prefs.getString(context.getString(R.string.PREFS_FIRST_RUN_PIN_PROMPT), "");
        String firstRunPinPrompt_decrypted = "";

        if(firstRunPinPrompt_encrypted != null && firstRunPinPrompt_encrypted.length() != 0)
        {
            try {
                firstRunPinPrompt_decrypted = Cryptation.decrypt(BACKUP_SETTINGS_SEED, firstRunPinPrompt_encrypted);
            } catch (Exception e) {
                firstRunPinPrompt_decrypted = STRING_FALSE;
                LogUtil.exception("Provisioning", "getInternalFirstRunPinPrompt", e);
            }
        }

        return STRING_TRUE.equalsIgnoreCase(firstRunPinPrompt_decrypted);
    }

    private static boolean getInternalSignInLock(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String signInLock_encrypted = prefs.getString(context.getString(R.string.PREFS_INVALIDSIGNINLOCK), "");
        String signInLock_decrypted = "";

        if(signInLock_encrypted != null && signInLock_encrypted.length() != 0)
        {
            try {
                signInLock_decrypted = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), signInLock_encrypted);
            } catch (Exception e) {
                signInLock_decrypted = STRING_FALSE;
                LogUtil.exception("Provisioning", "getInternalSignInLock", e);
            }
        }

        return STRING_TRUE.equalsIgnoreCase(signInLock_decrypted);
    }
    
    
    
    /**
     * Decrypting internal variables from preferences.
     */

    private static String[] getInternalAllEmailIds(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String email_id = prefs.getString(context.getString(R.string.PREFS_EMAIL_ID), "");
        String value = "";
        String [] emails = null;
        
        if(email_id != null && email_id.length() != 0)
        {
            try {
                value = Cryptation.decrypt(USERNAME_SEED, email_id);
                
                // We use | as a workaround to seperate emails as Set  is not supported for lower than API 11
                
                 emails = value.split("\\|");
                
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "getInternalEmailId", e);
            }
        }

        return emails;
    }

    private static String getInternalPin(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String pin = prefs.getString(context.getString(R.string.PREFS_PIN), "");
        String value = "";

        if(pin != null && pin.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), pin);
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "getInternalPin", e);
            }
        }

        return value;
    }
    
    
    private Hashtable<String, String> getInternalPassPhrase(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String key = null;
        String value = "";
        
        ArrayList<Object> encryptedDeviceList = SystemState.getEncryptedDeviceList();
        
        int numEncryptedDevices = 0;
        
        if(encryptedDeviceList != null)
            numEncryptedDevices = encryptedDeviceList.size();
        
        for(int i = 0; i < numEncryptedDevices; i++)
        {
            String containerId = ((Device) encryptedDeviceList.get(i)).getId();
            
            if(member_passphrase.get(containerId) == null)
            {
                key = prefs.getString("Key" + containerId, "");
                
                if(key != null && key.length() != 0)
                {
                    try {
                        value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), key);
                        member_passphrase.put(containerId, value);
                    } catch (Exception e) {
                        LogUtil.exception("Provisioning", "getInternalPassPhrase " + containerId, e);
                    }
                }
            }
        }
        return member_passphrase;
    }

    
    private Hashtable<String, DecryptKey> getInternalKeyHint(Context context) {
        DecryptKey decryptKey = null;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
       
        
        String valueScheme = "";
        String valueKey = "";
        
        String key = null;
        String scheme = null;

        
        ArrayList<Object> encryptedDeviceList = SystemState.getEncryptedDeviceList();
    
        int numEncryptedDevices = 0;
        
        if(encryptedDeviceList != null)
            numEncryptedDevices = encryptedDeviceList.size();
   
        for(int i = 0; i < numEncryptedDevices; i++)
        {
            String containerId = ((Device) encryptedDeviceList.get(i)).getId();
            
            if(member_decryptKey_hint.get(containerId) == null)
            {
                key = prefs.getString("key_KeyHint_" + containerId, "");
                scheme = prefs.getString("Scheme_KeyHint_" + containerId, "");

            
                if((key != null && key.length() != 0 ) && (scheme != null && scheme.length() != 0))
                {
                    try {
                        valueScheme = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), scheme);
                        valueKey = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), key);
                     
                        byte[] theByteArray = valueKey.getBytes();
         
                        decryptKey = new DecryptKey(theByteArray,valueScheme);
                        member_decryptKey_hint.put(containerId, decryptKey);
                        
                    } catch (Exception e) {
                        LogUtil.exception("Provisioning", "getInternalKeyHint " + containerId, e);
                    }
                }
            }
        }

        return member_decryptKey_hint;
    }
    
    private static boolean getInternalSecurityMode(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String security_mode = prefs.getString(context.getString(R.string.PREFS_SECURITY_MODE), "");
        String value = "";

        if(security_mode != null && security_mode.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), security_mode);
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "getInternalSecurityMode", e);
            }
        }

        boolean ret = true;
        if (value.equals(PROVISIONING_VALUE_FALSE)) {
            ret = false;
        }

        return ret;
    }
    
    
//    private static boolean getInternalHiddenFilesMode(Context context) {
//        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
//        String hidden_files_mode = prefs.getString(context.getString(R.string.PREFS_HIDDEN_FILE_MODE), "");
//        String value = "";
//
//        if(hidden_files_mode != null && hidden_files_mode.length() != 0)
//        {
//            try {
//                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), hidden_files_mode);
//            } catch (Exception e) {
//                LogUtil.exception("Provisioning", "getInternalHiddenFilesMode", e);
//            }
//        }
//
//        boolean ret = true;
//        if (value.equals(PROVISIONING_VALUE_FALSE)) {
//            ret = false;
//        }
//
//        return ret;
//    }
    
    private static String getInternalMipAccountToken(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String account_token = prefs.getString(context.getString(R.string.PREFS_MIP_ACCOUNT_TOKEN), "");
        String value = "";

        if(account_token != null && account_token.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), account_token);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalMipAccountToken", e);
            }
        }

        return value;
    }
    
    private static String getInternalMipAccountTokenSecret(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String account_token_secret = prefs.getString(context.getString(R.string.PREFS_MIP_ACCOUNT_TOKEN_SECRET), "");
        String value = "";

        if(account_token_secret != null && account_token_secret.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), account_token_secret);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalMipAccountTokenSecret", e);
            }
        }

        return value;
    }
    
    private static String getInternalMipAccountUserId(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String user_id = prefs.getString(context.getString(R.string.PREFS_USERID), "");
        String value = "";

        if(user_id != null && user_id.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), user_id);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalMipAccountUserId", e);
            }
        }

        return value;
    }

    private static boolean getInternalAcceptCerts(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String accept_allCerts_encrypted = prefs.getString(context.getString(R.string.PREFS_ACCEPTALLCERTS), "");
        String accept_allCerts_decrypted = "";

        if(accept_allCerts_encrypted != null && accept_allCerts_encrypted.length() != 0)
        {
            try {
                accept_allCerts_decrypted = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), accept_allCerts_encrypted);
            } catch (Exception e) {
                accept_allCerts_decrypted = "";
                LogUtil.exception("Provisioning", "getInternalAcceptCerts", e);
            }
        }

        return STRING_TRUE.equalsIgnoreCase(accept_allCerts_decrypted);
    }

    private static String getInternalDomainName(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String strPhoneNumber = prefs.getString(context.getString(R.string.PREFS_DOMAINSTRING), "");
        String value = "";

        
        if(strPhoneNumber != null && strPhoneNumber.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), strPhoneNumber);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalDomainName", e);
            }
        }

        return value;
    }

    private static String getInternalMipPort(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String strMipPort = prefs.getString(context.getString(R.string.PREFS_MIPPORTSTRING), "");
        String value = "";

        if(strMipPort != null && strMipPort.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), strMipPort);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalMipPort", e);
            }
        }

        return value;
    }
    
    
    private static String getInternalOAuthSecret(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String strOAuthSecret = prefs.getString(context.getString(R.string.PREFS_OAUTHSECRET), "");
        String value = "";

        if(strOAuthSecret != null && strOAuthSecret.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), strOAuthSecret);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalOAuthSecret", e);
            }
        }

        return value;
    }

    private static String getInternalUsername(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String username = prefs.getString(context.getString(R.string.PREFS_USERNAME), "");
        String value = "";

        if(username != null && username.length() != 0)
        {
            try {
                value = Cryptation.decrypt(USERNAME_SEED, username);
            } catch (Exception e) {
                LogUtil.exception("Provisioning", "getInternalUsername", e);
            }
        }

        return value;
    }
    
    
    private static String getInternalFedIDSubDomainName(Context context)
    {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.PREFS), Context.MODE_PRIVATE);
        String subDomainStr = prefs.getString(context.getString(R.string.PREFS_FEDID_SUBDOMAIN), "");
        String value = "";

        
        if(subDomainStr != null && subDomainStr.length() != 0)
        {
            try {
                value = Cryptation.decrypt(SeedCreator.getProvisioningSeed(context, PROVISIONING_SEED), subDomainStr);
            } catch (Exception e) {
                value = "";
                LogUtil.exception("Provisioning", "getInternalFedIDSubDomainName", e);
            }
        }

        return value;
    }

    /**
     * Register as listener to provisioning changes.
     * 
     * @param listener ProvisioningListener object.
     */
    public void registerListener(ProvisioningListener listener) {
        if (!provisioningListeners.contains(listener)) {
            provisioningListeners.add(listener);
        }
    }

    /**
     * Removes listener to provisioning changes.
     * 
     * @param listener ProvisioningListener object.
     */
    public void unregisterListener(ProvisioningListener listener) {
        synchronized (provisioningListeners) {
            provisioningListeners.remove(listener);
        }
    }

    private void notifyListeners(int id) {
        synchronized (provisioningListeners) {
            if (!provisioningListeners.isEmpty()) {
                
                int numProvisioningListeners =  provisioningListeners.size();
                for (int i = 0; i < numProvisioningListeners; ++i) {
                    provisioningListeners.get(i).onChange(id);
                }
            }
        }
    }

}
