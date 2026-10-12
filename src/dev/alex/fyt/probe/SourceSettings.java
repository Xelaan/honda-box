package dev.alex.fyt.probe;

import android.content.Context;

/** Default inferred from owner MODE order and recorded external4 -> AUX3 -> Radio1; manual override retained. */
public final class SourceSettings {
    public static final int DEFAULT_AUX=3;
    public static String cleanAuxName(String name){String value=name==null?"":name.replaceAll("[\\p{Cntrl}]"," ").replaceAll("\\s+"," ").trim();int count=value.codePointCount(0,value.length());if(count>32)value=value.substring(0,value.offsetByCodePoints(0,32));return value.isEmpty()?"AUX":value;}
    public static String auxName(Context c){return cleanAuxName(c.getSharedPreferences("overlay",0).getString("auxDisplayName","AUX"));}
    public static String auxCustomName(Context c){return c.getSharedPreferences("overlay",0).getString("auxCustomName","");}
    public static void auxName(Context c,String name){String value=cleanAuxName(name);android.content.SharedPreferences.Editor prefs=c.getSharedPreferences("overlay",0).edit().putString("auxDisplayName",value);if(!value.equals("AUX")&&!value.equals("Android")&&!value.equals("Head Unit"))prefs.putString("auxCustomName",value);prefs.apply();ProbeState.INSTANCE.auxName(value);}

    public static boolean hideOnAux(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getBoolean("hidePlayerOnAux",true);}
    public static void hideOnAux(Context c,boolean hide){c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().putBoolean("hidePlayerOnAux",hide).apply();}
    public static int aux(Context c){int n=c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getInt("confirmedAuxSource",DEFAULT_AUX);return n==-1?DEFAULT_AUX:n;}
    public static void aux(Context c,int source){if(source!=-1&&(source<3||source>16))throw new IllegalArgumentException("AUX source");
        android.content.SharedPreferences.Editor edit=c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit();if(source==-1)edit.remove("confirmedAuxSource");else edit.putInt("confirmedAuxSource",source);edit.apply();ProbeState.INSTANCE.auxSource(source==-1?DEFAULT_AUX:source);}
}
