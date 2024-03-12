package com.oeccsy.openending_ble;

import android.app.Activity;

import com.unity3d.player.UnityPlayer;

public class Plugin {
    protected static Plugin _instance;
    protected static Activity _context;
    public static Plugin instance() {
        if(_instance == null) {
            _instance = new Plugin();
            _context = UnityPlayer.currentActivity;
        }
        return _instance;
    }
}