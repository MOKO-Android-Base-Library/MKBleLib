package com.moko.ble.lib.task;

import com.elvishew.xlog.XLog;

public abstract class OrderTask {
    public static final long DEFAULT_DELAY_TIME = 6000;
    public static final int RESPONSE_TYPE_READ = 0;
    public static final int RESPONSE_TYPE_WRITE = 1;
    public static final int RESPONSE_TYPE_WRITE_NO_RESPONSE = 2;
    public static final int ORDER_STATUS_SUCCESS = 1;
    public Enum orderCHAR;
    public OrderTaskResponse response;
    public long delayTime = DEFAULT_DELAY_TIME;
    public int orderStatus;

    public OrderTask(Enum orderCHAR, int responseType) {
        response = new OrderTaskResponse();
        this.orderCHAR = orderCHAR;
        this.response.orderCHAR = orderCHAR;
        this.response.responseType = responseType;
    }

    public abstract byte[] assemble();

    public boolean parseValue(byte[] value) {
        return true;
    }

    public boolean timeoutPreTask() {
        XLog.i(orderCHAR.name() + "超时");
        return true;
    }

}
