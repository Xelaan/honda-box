package com.crewbeat.hondabox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.*;

/** Optional app-side Accord protocol interpretation; never rewrites CAN profile1000. */
public final class AccordCompatibility {
    public static final int PROFILE=6291908;
    private static final String KEY="processAsHondaAccord";
    public static boolean active(Integer actual,boolean override){return actual!=null&&actual>0&&(actual==PROFILE||override);}
    public static boolean enabled(Context c){return c.getSharedPreferences("overlay",0).getBoolean(KEY,false);}
    public static void enabled(Context c,boolean on){
        c.getSharedPreferences("overlay",0).edit().putBoolean(KEY,on).apply();
        if(ProbeState.INSTANCE.overlaySnapshot().running)
            c.startService(new Intent(c,ProbeService.class).setAction(ProbeService.COMPATIBILITY));
        else ProbeState.INSTANCE.processAsHondaAccord(on);
    }
    public static View controls(Activity activity){
        ThemeSettings.Palette p=ThemeSettings.resolve(activity).palette;
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);
        CheckBox toggle=new CheckBox(activity);toggle.setTag("process-as-honda-accord");toggle.setText(I18n.t(R.string.accord_compatibility_toggle));toggle.setTextColor(p.text);toggle.setChecked(enabled(activity));
        toggle.setOnCheckedChangeListener((v,on)->enabled(activity,on));box.addView(toggle);
        TextView note=new TextView(activity);note.setText(I18n.t(R.string.accord_compatibility_notes));note.setTextSize(12);note.setTextColor(p.muted);box.addView(note);
        return box;
    }
}
