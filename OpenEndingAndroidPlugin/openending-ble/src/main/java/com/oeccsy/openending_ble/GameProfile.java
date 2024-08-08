package com.oeccsy.openending_ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

public class GameProfile {
    public static UUID GAME_SERVICE = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb");
    public static UUID GAME_DATA = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb");

    public static UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static BluetoothGattService ownService;

    public static BluetoothGattService getService() {
        if(ownService == null) return createService();

        return ownService;
    }

    public static BluetoothGattService createService()
    {
        ownService = new BluetoothGattService(GAME_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic testA = new BluetoothGattCharacteristic(
                GAME_DATA,
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_INDICATE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);

        BluetoothGattDescriptor configDescriptor = new BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIG,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);

        testA.addDescriptor(configDescriptor);

        ownService.addCharacteristic(testA);

        return ownService;
    }
}
