package com.crewbeat.hondabox;

/** Diagnostics, recording and explicit test commands; entered from Settings. */
public final class DebugActivity extends SettingsScreenActivity {
    private static volatile boolean visible;
    public static boolean isVisible(){return visible;}
    @Override protected void onStart(){visible=true;super.onStart();}
    @Override protected void onStop(){visible=false;super.onStop();}
    @Override protected boolean debugScreen(){return true;}
}
