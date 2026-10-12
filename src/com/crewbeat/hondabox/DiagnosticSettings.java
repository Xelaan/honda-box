package com.crewbeat.hondabox;
import android.content.Context;
/** Diagnostic history is opt-in; decoding and current vehicle state always remain live. */
public final class DiagnosticSettings {
    public static boolean enabled(Context c){return c.getSharedPreferences("diagnostics",0).getBoolean("record",false);}
    public static void enabled(Context c,boolean on){c.getSharedPreferences("diagnostics",0).edit().putBoolean("record",on).apply();ProbeState.INSTANCE.recording(on);}
    private DiagnosticSettings(){}
}
