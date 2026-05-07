package com.moko.ble.lib.task;


import java.io.Serializable;

public class OrderTaskResponse implements Serializable {
    public Enum orderCHAR;
    public int responseType;
    public byte[] responseValue;
    public String address;
}
