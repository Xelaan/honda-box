package com.crewbeat.hondabox;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.*;

/** Visible foreground capture and explicit user pause; no wake locks or hidden relaunch loops. */
public final class BackgroundSettings {
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences("background",0);}
    public static boolean headless(Context c){return prefs(c).getBoolean("headless",true);}
    public static boolean autoStart(Context c){return prefs(c).getBoolean("autoStart",true);}
    public static void autoStart(Context c,boolean on){prefs(c).edit().putBoolean("autoStart",on).apply();}
    public static boolean paused(Context c){return prefs(c).getBoolean("paused",false);}
    public static boolean wanted(Context c){return prefs(c).getBoolean("wanted",false)&&!paused(c);}
    public static void activate(Context c){prefs(c).edit().putBoolean("paused",false).putBoolean("wanted",true).apply();}
    public static void pause(Context c){prefs(c).edit().putBoolean("paused",true).putBoolean("wanted",false).apply();}
    public static void stop(Context c){pause(c);c.stopService(new Intent(c,ProbeService.class));}
    public static void open(Context c){activate(c);if(ProbeState.INSTANCE.overlaySnapshot().running)return;Intent i=new Intent(c,ProbeService.class);if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
    public static boolean mayAutoStart(Context c){return autoStart(c)&&!paused(c);}
    public static boolean batteryExempt(Context c){return ((PowerManager)c.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(c.getPackageName());}
    public static android.view.View controls(Activity a){ThemeSettings.Palette p=ThemeSettings.resolve(a).palette;LinearLayout box=new LinearLayout(a);box.setOrientation(1);TextView title=new TextView(a);title.setText(I18n.t(R.string.background_operation));title.setTextSize(14);title.setTextColor(p.accent);box.addView(title);CheckBox boot=new CheckBox(a);boot.setText(I18n.t(R.string.start_after_boot));boot.setTextColor(p.text);boot.setChecked(autoStart(a));boot.setOnCheckedChangeListener((v,on)->autoStart(a,on));box.addView(boot);
        Button battery=ConsoleControls.button(a,I18n.t(R.string.battery_settings),p,false);battery.setOnClickListener(v->{try{a.startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+a.getPackageName())));}catch(RuntimeException error){try{a.startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));}catch(RuntimeException ignored){Toast.makeText(a,I18n.t(R.string.settings_unavailable),Toast.LENGTH_LONG).show();}}});box.addView(battery);
        Button system=ConsoleControls.button(a,I18n.t(R.string.dudu_settings),p,false);system.setOnClickListener(v->{try{a.startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(RuntimeException e){Toast.makeText(a,I18n.t(R.string.settings_unavailable),Toast.LENGTH_LONG).show();}});box.addView(system);
        TextView help=new TextView(a);help.setText(I18n.t(R.string.keep_alive_help));help.setTextSize(12);help.setTextColor(p.muted);help.setPadding(0,8,0,8);box.addView(help);return box;
    }
    private BackgroundSettings(){}
}
