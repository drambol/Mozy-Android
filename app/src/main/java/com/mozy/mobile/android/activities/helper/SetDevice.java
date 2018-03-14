package com.mozy.mobile.android.activities.helper;

import com.mozy.mobile.android.files.Device;

public interface SetDevice {
    public void setCloudDevice(Device device);
    public void errorGettingDevices(int errorCode);
    public Device getCloudDevice();
}
