package com.oeccsy.openending_ble;

import android.app.Activity;

import com.unity3d.player.UnityPlayer;

public class Plugin {
    protected static Activity _context;
    public Plugin() {
            _context = UnityPlayer.currentActivity;
    }
}