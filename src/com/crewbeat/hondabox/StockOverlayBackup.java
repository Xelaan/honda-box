package com.crewbeat.hondabox;

import android.content.SharedPreferences;

/** Durable backup precedes the shell write, and remains available if verification fails. */
public final class StockOverlayBackup {
    private final SharedPreferences prefs;
    public StockOverlayBackup(SharedPreferences prefs){this.prefs=prefs;}
    public String original(){String mode=prefs.getString("original",null);return StockOverlayShellService.validMode(mode)?mode:null;}
    public void save(String mode){if(!StockOverlayShellService.validMode(mode))throw new IllegalArgumentException("mode");if(original()==null&&!prefs.edit().putString("original",mode).commit())throw new IllegalStateException("Cannot save original AppOp");}
    public void restored(){if(!prefs.edit().remove("original").commit())throw new IllegalStateException("Cannot clear AppOp backup");}
}
