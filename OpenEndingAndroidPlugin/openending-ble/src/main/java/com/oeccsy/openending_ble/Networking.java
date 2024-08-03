package com.oeccsy.openending_ble;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Networking extends Plugin {

    private static Networking _instance;

    public static Networking getInstance() {
        if (_instance == null) {
            _instance = new Networking();
        }
        return _instance;
    }

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private Handler mainThreadHandler;


    private BluetoothGattServer bluetoothGattServer;

    private ArrayList<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
    private ArrayList<BluetoothDevice> scanResults = new ArrayList<BluetoothDevice>();
    private HashMap<String, BluetoothDevice> connectedDevices = new HashMap();
    private HashMap<String, BluetoothGatt> connectedGATT = new HashMap();




    public void initBluetoothSystem() {
        bluetoothManager = (BluetoothManager) _context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mainThreadHandler = new Handler();

        AndroidUtils.toast("init done!");
    }

    public void startAdvertising() {
        if(!bluetoothAdapter.isEnabled()) return;

        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(GameProfile.GAME_SERVICE))
                .build();

        mainThreadHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopAdvertising();
            }
        }, 2 * 60 * 1000);

        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);
        AndroidUtils.toast("start advertising!");
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            AndroidUtils.toast("Advertising started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            AndroidUtils.toast("Advertising failed" + errorCode);
        }
    };

    public void stopAdvertising() {
        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if(advertiser == null) return;
        advertiser.stopAdvertising(advertiseCallback);
        AndroidUtils.toast("stop advertising!");
    }

    private void startServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(_context, gattServerCallback);
        if (bluetoothGattServer == null) return;

        bluetoothGattServer.clearServices();
        bluetoothGattServer.addService(GameProfile.createService());

        AndroidUtils.toast("start Gatt Server!");
    }

    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i("OpenEnding", "Connected : " + device);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i("OpenEnding", "Disconnected : " + device);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.i("BLE", "onCharacteristicReadRequest");
            String uuid = characteristic.getUuid().toString();

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);

//            UUID uuid = characteristic.getUuid();
//            switch(uuid.toString())
//            {
//                case "Test":
//                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, {0, 1, 2});
//                    break;
//                default :
//                    bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null);
//                    break;
//            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i("OpenEnding", "onCharacteristicWriteRequest");

            BluetoothGattCharacteristic ownDeviceCharacteristic = GameProfile.getService().getCharacteristic(characteristic.getUuid());
            ownDeviceCharacteristic.setValue(value);

            String message = Base64.encodeToString(Arrays.copyOfRange(value, 0, value.length), 0);
            UnityPlayer.UnitySendMessage("BluetoothLEConnection", "OnDataReceive", message);

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, (byte[])null);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, (byte[])null);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, (byte[])null);
        }

    };

    private void stopServer() {
        if(bluetoothGattServer == null) return;

        bluetoothGattServer.close();
    }

    //P -> C 송신
    private void indicate(BluetoothDevice device, byte[] data) {
        byte[] temp = {0, 1, 2};
        data = temp;

        BluetoothGattCharacteristic characteristic = bluetoothGattServer.getService(GameProfile.GAME_SERVICE).getCharacteristic(GameProfile.TEST_A);
        characteristic.setValue(data);

        bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, true);
    }

    public void startScanning() {
        if (!bluetoothAdapter.isEnabled()) return;

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(GameProfile.GAME_SERVICE))
                .build();

        scanFilters.clear();
        scanFilters.add(scanFilter);

        ScanSettings scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanResults.clear();

        mainThreadHandler.postDelayed(() -> {
            scanner.stopScan(scanCallback);
        }, 2 * 60 * 1000);

        scanner.startScan(scanFilters, scanSettings, scanCallback);
        AndroidUtils.toast("start scanning!");
        Log.i("OpenEnding", "start scanning!!!");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            for (BluetoothDevice existDevice : scanResults) {
                if (result.getDevice().getAddress().equals(existDevice.getAddress())) return;
            }

            BluetoothDevice device = result.getDevice();
            scanResults.add(device);
            device.connectGatt(_context, false, bluetoothGattCallBack);

            AndroidUtils.toast("scan target found!");
            Log.i("OpenEnding", "scan target found!");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            AndroidUtils.toast("scan fail");
            Log.i("OpenEnding", "scan fail!!!");
        }
    };

    public void stopScanning() {
        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        scanner.stopScan(scanCallback);
        AndroidUtils.toast("stop Scanning!");
        Log.i("OpenEnding", "stop scanning!!!");
    }

    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCallback bluetoothGattCallBack = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE || status != BluetoothGatt.GATT_SUCCESS) {
                AndroidUtils.toast("GATT FAILURE");
                Log.i("OpenEnding", "GATT_FAILURE!!! : " + status);
                Log.i("OpenEnding", "AND newState : " + newState);
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedGATT.put(gatt.getDevice().getAddress(), gatt);

                AndroidUtils.toast("connect");
                Log.i("OpenEnding", "connect!!!");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedGATT.remove(gatt.getDevice().getAddress());

                AndroidUtils.toast("disconnect");
                Log.i("OpenEnding", "disconnect!!!");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) {
                AndroidUtils.toast("discovery fail");
                Log.i("OpenEnding", "discovery fail!!!");
                return;
            }

            AndroidUtils.toast("service discovery success");
            Log.i("OpenEnding", "service discovery failed!!!");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            String msg = characteristic.getStringValue(0);
            AndroidUtils.toast(msg);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {

            } else {
                disconnectGattServer();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                String msg = characteristic.getStringValue(0);
                AndroidUtils.toast(msg);
            } else {
                // disconnectGattServer();
            }
        }
    };

    //C->P 송신
    public void write(String name, byte[] data) {
        BluetoothGatt gatt = connectedGATT.get(name);
        BluetoothGattService service = GameProfile.getService();
        BluetoothGattCharacteristic characteristic = service.getCharacteristic(GameProfile.TEST_A);

        characteristic.setValue(data);
        characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean isWrite = gatt.writeCharacteristic(characteristic);

        if(isWrite) {

        }
    }

    public void disconnectGattServer() {
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
    }
}
