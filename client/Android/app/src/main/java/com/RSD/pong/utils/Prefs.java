package com.RSD.pong.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private final SharedPreferences prefs;

    public Prefs(Context ctx) {
        prefs = ctx.getSharedPreferences("app_settings", Context.MODE_PRIVATE);
    }

    public void setServerIp(String ip) {
        prefs.edit().putString("server_ip", ip).apply();
    }

    public String getServerIp() {
        return prefs.getString("server_ip", null);
    }
}
