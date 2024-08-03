package com.oeccsy.openending_ble;

import android.app.Activity;
import android.content.Context;

import com.unity3d.player.UnityPlayer;

public class Plugin {
    protected static Context _context;
    protected static Activity _activity;

    public Plugin() {
        _context = UnityPlayer.currentActivity.getApplicationContext();
        _activity = UnityPlayer.currentActivity;
    }
}