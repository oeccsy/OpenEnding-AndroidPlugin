package com.oeccsy.openending_ble;

import static android.content.Context.BLUETOOTH_SERVICE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Base64;
import android.util.Log;

import com.unity3d.player.UnityPlayer;

import java.util.Arrays;

public class Peripheral extends Plugin {

    private static Peripheral _instance;

    public static Peripheral getInstance() {
        if (_instance == null) {
            _instance = new Peripheral();
        }
        return _instance;
    }

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private Handler peripheralThreadHandler;

    private BluetoothGattServer bluetoothGattServer;
    private BluetoothDevice centralDevice;

    public void initBluetoothSystem() {
        bluetoothManager = (BluetoothManager) _context.getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        peripheralThreadHandler = new Handler();

        Log.i("OpenEnding", "Init BluetoothLE System");
    }

    public void startAdvertising(String deviceName) {
        if(!bluetoothAdapter.isEnabled()) return;
        bluetoothAdapter.setName(deviceName);

        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .setConnectable(true)
                .setTimeout(0)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(GameProfile.GAME_SERVICE))
                //.addServiceData(new ParcelUuid(GameProfile.GAME_SERVICE), new byte[] {(byte)deviceNum})
                .build();

        peripheralThreadHandler.postDelayed(() -> {
            stopAdvertising();
        }, 30 * 1000);

        advertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback);

        Log.i("OpenEnding", "Peripheral Start Advertise");
        AndroidUtils.toast("Advertise 시작");
    }

    public void stopAdvertising() {
        BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if(advertiser == null) return;
        advertiser.stopAdvertising(advertiseCallback);

        Log.i("OpenEnding", "Peripheral Stop Advertise");
    }

    private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.i("OpenEnding", "Peripheral Start Success");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.i("OpenEnding", "Peripheral Start Fail : " + errorCode);
        }
    };

    private void startGattServer() {
        bluetoothGattServer = bluetoothManager.openGattServer(_context, gattServerCallback);
        if (bluetoothGattServer == null) return;

        bluetoothGattServer.clearServices();
        bluetoothGattServer.addService(GameProfile.getService());

        Log.i("OpenEnding", "Peripheral Start GattServer");
    }

    private void stopGattServer() {
        if(bluetoothGattServer == null) return;

        bluetoothGattServer.close();

        Log.i("OpenEnding", "Peripheral Stop GattServer");
    }

    private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.i("OpenEnding", "GATT Failure, status : " + status);
                AndroidUtils.toast("GATT FAILURE");
                return;
            }

            switch(newState) {
                case BluetoothProfile.STATE_CONNECTED :
                    centralDevice = device;
                    UnityPlayer.UnitySendMessage("AndroidConnection", "OnDeviceConnected", device.getName());

                    AndroidUtils.toast(device.getName() + "연결 완료");
                    Log.i("OpenEnding", "Connect : " + device.getAddress());
                    break;
                case BluetoothProfile.STATE_DISCONNECTED :
                    centralDevice = null;
                    UnityPlayer.UnitySendMessage("AndroidConnection", "OnDeviceDisconnected", device.getName());

                    AndroidUtils.toast(device.getName() + "연결 끊김");
                    Log.i("OpenEnding", "Disconnect : " + device.getAddress());
                    break;
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Log.i("OpenEnding", "onCharacteristicReadRequest");
            String uuid = characteristic.getUuid().toString();

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        //C->P 수신
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.i("OpenEnding", "onCharacteristicWriteRequest");

            BluetoothGattCharacteristic ownDeviceCharacteristic = GameProfile.getService().getCharacteristic(characteristic.getUuid());
            ownDeviceCharacteristic.setValue(value);

            String encodedData = Base64.encodeToString(Arrays.copyOfRange(value, 0, value.length), 0);
            UnityPlayer.UnitySendMessage("AndroidConnection", "OnDataReceive", encodedData);

            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            bluetoothGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
        }
    };

    //P -> C 송신
    private void indicate(byte[] data) {
        BluetoothDevice device = centralDevice;

        BluetoothGattCharacteristic characteristic = bluetoothGattServer.getService(GameProfile.GAME_SERVICE).getCharacteristic(GameProfile.CHARACTERISTIC_GAME_DATA);
        characteristic.setValue(data);

        bluetoothGattServer.notifyCharacteristicChanged(device, characteristic, true);

        Log.i("OpenEnding", "indicate");
    }
}