package com.mozy.mobile.android.activities.startup;

import java.util.regex.Pattern;

import com.mozy.mobile.android.R;
import android.content.res.Resources;

public class StartupHelper {
    
    public static final String STR_QUOTA_INTENTPARAM = "QUOTA";
    private static final String WHITESPACE_SPACE = " ";
    private static final String WHITESPACE_TAB = "\t";
    private static final String EMPTY_STRING = "";
    
    private static final String EMAIL_PATTERN ="[a-z0-9\\!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|\\}~\\-]+(?:\\.[a-z0-9\\!#\\$%&'\\*\\+/=\\?\\^_`\\{\\|\\}~\\-]+)*@(?:[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9\\-]*[a-z0-9])?";
    private static final String SUBDOMAIN_PATTERN ="^[0-9a-zA-Z][0-9a-zA-Z-]+[0-9a-zA-Z]$";
    /**
     * 
     * @param str
     * @return
     */
    public static boolean IsStringNullOrEmtpy(String str)
    {
        return str == null || str.trim().length() == 0;
    }
    
    public static boolean IsContainsWhiteSpaceCharacters(String str)
    {
        return str.contains(WHITESPACE_SPACE) || str.contains(WHITESPACE_TAB);
    }
    

    public static String validateEmail(String strEmail, Resources res) 
    {
        if(IsStringNullOrEmtpy(strEmail))
            return res.getString(R.string.errormessage_email_text_empty);
        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        if(!pattern.matcher(strEmail.trim()).matches())
            return res.getString(R.string.errormessage_email_not_valid);
        return EMPTY_STRING;
    }
    
    public static String validatePassword(String strPassword, Resources res)
    {
        if(IsStringNullOrEmtpy(strPassword))
            return res.getString(R.string.errormessage_password_text_empty);
        if(IsContainsWhiteSpaceCharacters(strPassword))
            return res.getString(R.string.errormessage_passwords_contains_whitespaces);
        return EMPTY_STRING;        
    }
    
    
    public static String validateSubDomain(String strSubDomain, Resources res)
    {
        if(IsStringNullOrEmtpy(strSubDomain))
            return res.getString(R.string.subdomain_not_verified_body);;
        Pattern pattern = Pattern.compile(SUBDOMAIN_PATTERN);
        if(!pattern.matcher(strSubDomain.trim()).matches())
            return res.getString(R.string.subdomain_not_verified_body);
        return EMPTY_STRING;
    }
}
