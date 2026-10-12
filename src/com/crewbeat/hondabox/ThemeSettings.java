package com.crewbeat.hondabox;

import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import java.text.SimpleDateFormat;
import java.util.*;

/** One persistent setting for the panel, diagnostics, and volume window. */
public final class ThemeSettings {
    public static final String AUTO="auto", DAY="day", NIGHT="night", SYSTEM="system";
    public static final class Palette {
        public final boolean night; public final int bg,card,text,muted,accent,accentText,button,border,warning;
        Palette(boolean dark){this(dark,null);}
        Palette(boolean dark,Integer custom){night=dark;bg=c(dark?"#101319":"#EEF1F5");card=c(dark?"#191E26":"#FFFFFF");text=c(dark?"#D9DEE8":"#303846");
            muted=c(dark?"#8B95A6":"#737F90");accent=custom==null?c(dark?"#8FAFF7":"#426DC3"):AccentColors.tone(custom,dark);accentText=AccentColors.foreground(accent);button=c(dark?"#262D38":"#E7ECF4");border=c(dark?"#333B48":"#E2E7ED");warning=c(dark?"#D2B587":"#896329");}
        private int c(String value){return Color.parseColor(value);}
    }
    public static final Palette LIGHT=new Palette(false), DARK=new Palette(true);
    public static final class State {
        public final Palette palette;public final String mode,description;public final SolarTimes.Result solar;
        State(Palette p,String m,String text,SolarTimes.Result s){palette=p;mode=m;description=text;solar=s;}
    }
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("theme",Context.MODE_PRIVATE);}
    public static int accent(Context c){return prefs(c).getInt("accent",AccentColors.DEFAULT)|0xFF000000;}
    public static void accent(Context c,int colour){colour|=0xFF000000;SharedPreferences.Editor edit=prefs(c).edit();if(colour==AccentColors.DEFAULT)edit.remove("accent");else edit.putInt("accent",colour);edit.apply();invalidate();}
    private static int accentKey=AccentColors.DEFAULT;private static Palette accentLight=LIGHT,accentDark=DARK;
    private static Palette palette(Context c,boolean dark){int base=accent(c);if(base==AccentColors.DEFAULT)return dark?DARK:LIGHT;if(base!=accentKey){accentKey=base;accentLight=new Palette(false,base);accentDark=new Palette(true,base);}return dark?accentDark:accentLight;}
    public static String mode(Context c){return prefs(c).getString("mode",AUTO);}
    public static void mode(Context c,String mode){if(!AUTO.equals(mode)&&!DAY.equals(mode)&&!NIGHT.equals(mode)&&!SYSTEM.equals(mode))throw new IllegalArgumentException("theme");prefs(c).edit().putString("mode",mode).apply();invalidate();}
    public static boolean hasLocation(Context c){return prefs(c).contains("lat")&&prefs(c).contains("lon");}
    public static boolean gps(Context c){return prefs(c).getBoolean("gps",false);}
    public static void gps(Context c,boolean active){prefs(c).edit().putBoolean("gps",active).apply();invalidate();}
    public static double latitude(Context c){return Double.longBitsToDouble(prefs(c).getLong("lat",0));}
    public static double longitude(Context c){return Double.longBitsToDouble(prefs(c).getLong("lon",0));}
    public static void location(Context c,double lat,double lon,boolean gps){
        if(!SolarTimes.valid(lat,lon))throw new IllegalArgumentException(I18n.t(R.string.ui_coordinates_out_of_range));
        prefs(c).edit().putLong("lat",Double.doubleToLongBits(lat)).putLong("lon",Double.doubleToLongBits(lon)).putBoolean("gps",gps).apply();invalidate();
    }
    private static State cached;private static long cachedAt,expires;private static String cacheZone;private static int cacheSystem,cacheLanguage;
    private static synchronized void invalidate(){cached=null;}
    public static synchronized void languageChanged(){cached=null;}
    public static State resolve(Context c){return resolve(c,System.currentTimeMillis(),TimeZone.getDefault());}
    public static synchronized State resolve(Context c,long now,TimeZone zone){
        int system=c.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK;
        String zoneKey=zone.getID()+":"+zone.getOffset(now);
        if(cached!=null&&now>=cachedAt&&now<expires&&zoneKey.equals(cacheZone)&&system==cacheSystem&&cacheLanguage==I18n.revision())return cached;
        String m=mode(c);SolarTimes.Result solar=null;boolean dark;String text;
        if(DAY.equals(m)){dark=false;text=I18n.t(R.string.ui_day_selected_manually);}
        else if(NIGHT.equals(m)){dark=true;text=I18n.t(R.string.ui_night_selected_manually);}
        else if(SYSTEM.equals(m)){dark=system==Configuration.UI_MODE_NIGHT_YES;text=I18n.t(R.string.theme_system_description);}
        else if(hasLocation(c)) {
            solar=SolarTimes.calculate(now,latitude(c),longitude(c),zone);dark=!solar.day;
            SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.getDefault());f.setTimeZone(zone);
            String next=solar.next==0?I18n.t(R.string.ui_no_sunrise_sunset_near_this):(solar.day?I18n.t(R.string.ui_sunset):I18n.t(R.string.ui_sunrise))+f.format(new Date(solar.next));
            text=I18n.t(R.string.ui_auto_309)+(dark?I18n.t(R.string.ui_night_318):I18n.t(R.string.ui_day_315))+" · "+next;
        } else {dark=system==Configuration.UI_MODE_NIGHT_YES;text=I18n.t(R.string.ui_auto_android_theme_set_a);}
        cached=new State(palette(c,dark),m,text,solar);cachedAt=now;expires=now+60000;
        if(solar!=null&&solar.next>now)expires=Math.min(expires,solar.next);
        cacheZone=zoneKey;cacheSystem=system;cacheLanguage=I18n.revision();return cached;
    }
    private ThemeSettings(){}
}
