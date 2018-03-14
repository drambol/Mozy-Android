package com.mozy.mobile.android.provisioning;

public interface ProvisioningListener {
    
    public static final int ALL = 0;
    public static final int ACTIVATION = 1;
    public static final int PHOTO = 2;
    public static final int AUDIO = 3;
    public static final int VIDEO = 4;
    public static final int DOCUMENTS = 5;
    public static final int CONTACTS = 6;
    public static final int CALENDAR = 7;
    public static final int BACKUP_SETTINGS_TYPE = 10;
    public static final int SECURITY_MODE = 11;
    public static final int PIN = 12;
    public static final int PERSONAL_KEY = 13;
    public static final int PERSONAL_KEY_HINT = 14;
    public static final int MANAGED_KEY = 15;
    public static final int UPLOAD = 16;
    public static final int ACCOUNT_INFO_CHANGE = 17; 
 //   public static final int HIDDEN_FILES_MODE = 18;
    
    /**
     * Notifies that something has changed.
     * 
     * @param id Area that is affected by the change.
     */
    public void onChange(int id);
}
