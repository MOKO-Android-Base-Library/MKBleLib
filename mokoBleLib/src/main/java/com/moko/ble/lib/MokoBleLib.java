package com.moko.ble.lib;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.elvishew.xlog.XLog;
import com.moko.ble.lib.callback.MokoResponseCallback;
import com.moko.ble.lib.task.OrderTask;
import com.moko.ble.lib.task.OrderTaskResponse;
import com.moko.ble.lib.utils.MokoUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class MokoBleLib implements MokoResponseCallback {
    private BluetoothAdapter mBluetoothAdapter;
    private final Map<String, BluetoothGatt> mGattMap = new HashMap<>();
    private final Map<String, BlockingQueue<OrderTask>> mQueueMap = new HashMap<>();
    private Context mContext;
    private final Map<String, MokoBleManager> mBleManagerMap = new HashMap<>();
    private Handler mHandler;

    public MokoBleLib() {
//        mQueue = new LinkedBlockingQueue<>();
    }

    public void init(Context context) {
        mContext = context;
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler(Looper.getMainLooper());
//        mokoBleManager = getMokoBleManager();
    }

    public void connDevice(@NonNull String address) {
        if (null == mBleManagerMap.get(address)) {
            mBleManagerMap.put(address, getMokoBleManager());
        }
        if (!isBluetoothOpen()) {
            XLog.i("connDevice: bluetooth close");
            return;
        }
        if (isConnDevice(address)) {
            XLog.i("connDevice: device connected");
            disConnectBle(address);
            return;
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device != null) {
            mHandler.post(() -> {
                XLog.i("start connect");
                MokoBleManager mokoBleManager = mBleManagerMap.get(address);
                assert null != mokoBleManager;
                mokoBleManager.connect(device)
                        .retry(5, 200)
                        .timeout(50000)
                        .enqueue();
            });
        } else {
            XLog.i("connDevice: the device is null");
        }
    }

    public boolean isBluetoothOpen() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    @SuppressLint("MissingPermission")
    public boolean isConnDevice(String address) {
        if (mContext == null || mBluetoothAdapter == null) return false;
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        int connState = bluetoothManager.getConnectionState(mBluetoothAdapter.getRemoteDevice(address), BluetoothProfile.GATT);
        return connState == BluetoothProfile.STATE_CONNECTED;
    }

    public void disConnectBle(String address) {
        MokoBleManager mokoBleManager = mBleManagerMap.get(address);
        if (null == mokoBleManager) return;
        mokoBleManager.disconnect().enqueue();
    }

    @SuppressLint("MissingPermission")
    public void enableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        }
    }

    @SuppressLint("MissingPermission")
    public void disableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.disable();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    /// ////////////////////////////////////////////////////////////////////////

    public void sendOrder(OrderTask... orderTasks) {
        if (orderTasks.length == 0) {
            return;
        }
        if (!isSyncData()) {
            for (OrderTask ordertask : orderTasks) {
                if (ordertask == null) {
                    continue;
                }
                BlockingQueue<OrderTask> mQueue = mQueueMap.get(ordertask.response.address);
                if (null == mQueue) continue;
                mQueue.offer(ordertask);
            }
            executeTask();
        } else {
            for (OrderTask ordertask : orderTasks) {
                if (ordertask == null) {
                    continue;
                }
                BlockingQueue<OrderTask> mQueue = mQueueMap.get(ordertask.response.address);
                if (null == mQueue) continue;
                mQueue.offer(ordertask);
            }
        }
    }

    public void executeTask() {
        if (!isSyncData()) {
            orderFinish();
            return;
        }
//        if (mQueue.isEmpty()) {
//            return;
//        }
        BlockingQueue<OrderTask> mQueue = null;
        for (String key : mQueueMap.keySet()) {
            BlockingQueue<OrderTask> mapValue = mQueueMap.get(key);
            if (null != mapValue && !mapValue.isEmpty()) {
                mQueue = mapValue;
                break;
            }
        }
        assert null != mQueue;
        final OrderTask orderTask = mQueue.peek();
        assert null != orderTask;
        String address = orderTask.response.address;
        BluetoothGatt mBluetoothGatt = mGattMap.get(address);
        XLog.i("mac=" + address);
        if (mBluetoothGatt == null) {
            XLog.i("executeTask : BluetoothGatt is null");
            disConnectBle(address);
            return;
        }
//        if (orderTask == null) {
//            XLog.i("executeTask : orderTask is null");
//            disConnectBle(address);
//            return;
//        }
        if (isCHARNull(address)) {
            XLog.i("executeTask : characteristicMap is null");
            disConnectBle(address);
            return;
        }
        final Enum orderCHAR = orderTask.orderCHAR;
        final BluetoothGattCharacteristic characteristic = getCharacteristic(address, orderCHAR);
        if (characteristic == null) {
            XLog.i("executeTask : mokoCharacteristic is null");
            disConnectBle(address);
            return;
        }
        sendOrder(orderTask, characteristic, mBluetoothGatt);
        timeoutHandler(orderTask);
    }

    @SuppressLint("MissingPermission")
    private void sendOrder(OrderTask orderTask, final BluetoothGattCharacteristic characteristic, BluetoothGatt mBluetoothGatt) {
        if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_READ) {
            XLog.i("app to device read : " + orderTask.orderCHAR.name());
            mHandler.postDelayed(() -> mBluetoothGatt.readCharacteristic(characteristic), 50);
        } else if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE) {
            XLog.i("app to device write : " + orderTask.orderCHAR.name());
            XLog.i(MokoUtils.bytesToHexString(orderTask.assemble()));
            characteristic.setValue(orderTask.assemble());
            mHandler.postDelayed(() -> mBluetoothGatt.writeCharacteristic(characteristic), 50);
        } else if (orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE_NO_RESPONSE) {
            XLog.i("app to device write no response : " + orderTask.orderCHAR.name());
            XLog.i(MokoUtils.bytesToHexString(orderTask.assemble()));
            characteristic.setValue(orderTask.assemble());
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mHandler.postDelayed(() -> mBluetoothGatt.writeCharacteristic(characteristic), 50);
        }
    }

    @SuppressLint("MissingPermission")
    public void sendDirectOrder(OrderTask orderTask) {
        String address = orderTask.response.address;
        BluetoothGatt mBluetoothGatt = mGattMap.get(address);
        if (null == mBluetoothGatt) {
            XLog.i("executeTask : BluetoothGatt is null");
            disConnectBle(address);
            return;
        }
        final BluetoothGattCharacteristic characteristic = getCharacteristic(orderTask.response.address, orderTask.orderCHAR);
        XLog.i("app to device write direct : " + orderTask.orderCHAR.name());
        XLog.i(MokoUtils.bytesToHexString(orderTask.assemble()));
        characteristic.setValue(orderTask.assemble());
        mHandler.postDelayed(() -> mBluetoothGatt.writeCharacteristic(characteristic), 50);
    }

    public synchronized boolean isSyncData() {
        //遍历map集合
        boolean isSyn = false;
        for (String key : mQueueMap.keySet()) {
            BlockingQueue<OrderTask> mQueue = mQueueMap.get(key);
            if (null != mQueue && !mQueue.isEmpty()) {
                isSyn = true;
                break;
            }
        }
        return isSyn;
//        return mQueue != null && !mQueue.isEmpty();
    }

    public void pollTask(String address) {
        BlockingQueue<OrderTask> mQueue = mQueueMap.get(address);
        if (mQueue != null && !mQueue.isEmpty()) {
            OrderTask orderTask = mQueue.peek();
            XLog.i("remove " + orderTask.orderCHAR.name());
            mQueue.poll();
        }
    }

    public void timeoutHandler(final OrderTask orderTask) {
        Runnable timeoutRunner = () -> {
            if (orderTask.orderStatus != OrderTask.ORDER_STATUS_SUCCESS) {
                if (orderTask.timeoutPreTask()) {
                    pollTask(orderTask.response.address);
                    executeTask();
                    orderTimeout(orderTask.response);
                }
            }
        };
        mHandler.postDelayed(timeoutRunner, orderTask.delayTime);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    /// ////////////////////////////////////////////////////////////////////////
    @Override
    public void onCharacteristicChanged(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!orderNotify(characteristic, value)) {
            if (isSyncData()) {
                BlockingQueue<OrderTask> mQueue = null;
                for (String key : mQueueMap.keySet()) {
                    BlockingQueue<OrderTask> mapValue = mQueueMap.get(key);
                    if (null != mapValue && !mapValue.isEmpty()) {
                        mQueue = mapValue;
                        break;
                    }
                }
                assert null != mQueue;
                OrderTask orderTask = mQueue.peek();
                if (value != null
                        && value.length > 0
                        && orderTask != null
                        && orderTask.response != null
                        && orderResponseValid(characteristic, orderTask)
                        && orderTask.parseValue(value)) {
                    formatCommonOrder(orderTask, value);
                }
            }
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!isSyncData()) {
            return;
        }
        BlockingQueue<OrderTask> mQueue = null;
        for (String key : mQueueMap.keySet()) {
            BlockingQueue<OrderTask> mapValue = mQueueMap.get(key);
            if (null != mapValue && !mapValue.isEmpty()) {
                mQueue = mapValue;
                break;
            }
        }
        assert null != mQueue;
        OrderTask orderTask = mQueue.peek();
        if (value != null
                && value.length > 0
                && orderTask != null
                && orderTask.response != null
                && orderTask.response.responseType == OrderTask.RESPONSE_TYPE_WRITE
                && orderResponseValid(characteristic, orderTask)
                && orderTask.parseValue(value)) {
            formatCommonOrder(orderTask, value);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGattCharacteristic characteristic, byte[] value) {
        if (!isSyncData()) {
            return;
        }
        BlockingQueue<OrderTask> mQueue = null;
        for (String key : mQueueMap.keySet()) {
            BlockingQueue<OrderTask> mapValue = mQueueMap.get(key);
            if (null != mapValue && !mapValue.isEmpty()) {
                mQueue = mapValue;
                break;
            }
        }
        assert null != mQueue;
        OrderTask orderTask = mQueue.peek();
        if (value != null
                && value.length > 0
                && orderTask != null
                && orderTask.response != null
                && orderTask.response.responseType == OrderTask.RESPONSE_TYPE_READ
                && orderResponseValid(characteristic, orderTask)
                && orderTask.parseValue(value)) {
            formatCommonOrder(orderTask, value);
        }
    }

    private void formatCommonOrder(OrderTask task, byte[] value) {
        task.orderStatus = OrderTask.ORDER_STATUS_SUCCESS;
        task.response.responseValue = value;
        BlockingQueue<OrderTask> mQueue = mQueueMap.get(task.response.address);
        mQueue.poll();
        executeTask();
        orderResult(task.response);
    }

    ///////////////////////////////////////////////////////////////////////////
    //
    /// ////////////////////////////////////////////////////////////////////////

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        mGattMap.put(gatt.getDevice().getAddress(), gatt);
        //顺带初始化任务队列
        mQueueMap.put(gatt.getDevice().getAddress(), new LinkedBlockingQueue<>());
        onDeviceConnected(gatt);
    }

    @Override
    public void onDeviceDisconnected(BluetoothDevice device, int reason) {
        mQueueMap.remove(device.getAddress());
//        if (isSyncData()) {
//            mQueue.clear();
//        }
        XLog.i("disconnected reason:" + reason);
        mGattMap.remove(device.getAddress());
        mBleManagerMap.remove(device.getAddress());
        onDeviceDisconnected(device);
    }

    public abstract MokoBleManager getMokoBleManager();

    public abstract void onDeviceConnected(BluetoothGatt gatt);

    public abstract void onDeviceDisconnected(BluetoothDevice device);

    public abstract BluetoothGattCharacteristic getCharacteristic(String address, Enum orderCHAR);

    public abstract boolean isCHARNull(String address);

    public abstract void orderFinish();

    public abstract void orderTimeout(OrderTaskResponse response);

    public abstract void orderResult(OrderTaskResponse response);

    public abstract boolean orderNotify(BluetoothGattCharacteristic characteristic, byte[] value);

    public abstract boolean orderResponseValid(BluetoothGattCharacteristic characteristic, OrderTask orderTask);
}
