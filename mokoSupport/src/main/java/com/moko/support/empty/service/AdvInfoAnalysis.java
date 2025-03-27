package com.moko.support.empty.service;


import com.moko.support.empty.entity.DeviceInfo;

public interface AdvInfoAnalysis<T> {
    T analyseDeviceInfo(DeviceInfo deviceInfo);
}
