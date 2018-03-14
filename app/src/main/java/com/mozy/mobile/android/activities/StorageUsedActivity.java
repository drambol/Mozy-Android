package com.mozy.mobile.android.activities;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mozy.mobile.android.R;
import com.mozy.mobile.android.utils.SystemState;

public class StorageUsedActivity extends SecuredActivity
{

    private static final long BYTES_PER_GIG = (1024*1024*1024);
    private static final int FIRST_USER_DIALOG = 0;
    private static final int DIALOG_ERROR_ID = FIRST_USER_DIALOG + 0;
    private static final int DIALOG_NORETRY_ERROR_ID = FIRST_USER_DIALOG + 1;

    private static final int[] ERROR_IDS = new int[] {ErrorManager.ERROR_TYPE_GENERIC};

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_used_layout);

        if (SystemState.cloudContainer != null)
        {
            this.setQuotaText();
        }
        else
        {
            final TextView notification = (TextView)findViewById(R.id.notification);
            notification.setText(R.string.settings_storage_quota_nocloud);
            notification.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void setQuotaText()
    {
        long quota = SystemState.cloudContainer.getQuota();
        long amountUsed = SystemState.cloudContainer.getSize();
        double quotaPercentage = 0;
        if (quota != 0)
        {
             quotaPercentage = (((double)amountUsed/quota) * 100);
             // Only need two significant digits
             quotaPercentage = Math.floor(quotaPercentage * 100) / 100.0;
        }

        final ProgressBar quotaProgressBar = (ProgressBar)findViewById(R.id.quota_progress_bar);
        quotaProgressBar.setProgress((int)Math.round(quotaPercentage));

        String strQuotaText = getString(R.string.settings_storage_quota_progress);

        strQuotaText = strQuotaText.replace("$PERCENTAGE", Double.toString(100 - quotaPercentage));
        strQuotaText = strQuotaText.replace("$GIGABYTES", Long.toString(Math.round(quota/BYTES_PER_GIG)));

        final TextView quotaTextView = (TextView)findViewById(R.id.quota_progress_text);
        quotaTextView.setText(strQuotaText);

        final View quotaLayout = findViewById(R.id.quota_layout);
        quotaLayout.setVisibility(View.VISIBLE);
    }

    @Override
    protected int[] getErrorIds()
    {
        return ERROR_IDS;
    }


    @Override
    protected int handleError(int errorCode, int errorType)
    {
        switch (errorCode)
        {
            case ErrorCodes.ERROR_HTTP_IO:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_NOT_FOUND:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_PARSER:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_SERVER:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_BAD_GATEWAY:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_GATEWAY_TIMEOUT:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_SERVICE_UNAVAILABLE:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_UNAUTHORIZED:
                return ACTION_DIALOG_ERROR;
            case ErrorCodes.ERROR_HTTP_UNKNOWN:
                return ACTION_DIALOG_ERROR;
        }

        return ACTION_NONE;
    }

    @Override
    protected Dialog createErrorDialog(int errorCode)
    {
        switch (errorCode)
        {
            case ErrorCodes.ERROR_HTTP_IO:
            case ErrorCodes.ERROR_HTTP_NOT_FOUND:
            case ErrorCodes.ERROR_HTTP_SERVER:
            case ErrorCodes.ERROR_HTTP_BAD_GATEWAY:
            case ErrorCodes.ERROR_HTTP_GATEWAY_TIMEOUT:
            case ErrorCodes.ERROR_HTTP_SERVICE_UNAVAILABLE:
            case ErrorCodes.ERROR_HTTP_UNKNOWN:
                return createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.error_not_available, R.string.alert_dialog_retry, R.string.cancel_button_text);
            case ErrorCodes.ERROR_HTTP_PARSER:
                return createGenericErrorDialog(DIALOG_NORETRY_ERROR_ID, R.string.error, R.string.error_not_available, R.string.ok_button_text);
            case ErrorCodes.ERROR_HTTP_UNAUTHORIZED:
                return createGenericErrorDialog(DIALOG_ERROR_ID, R.string.error, R.string.errormessage_authorization_failure, R.string.alert_dialog_retry, R.string.cancel_button_text);
        }

        return null;
    }
}
