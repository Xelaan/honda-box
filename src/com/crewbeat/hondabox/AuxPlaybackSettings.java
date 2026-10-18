package com.crewbeat.hondabox;

import android.app.*;
import android.content.*;
import android.view.View;
import android.widget.*;

/** Android playback controls for the configured AUX source; no extra permissions. */
public final class AuxPlaybackSettings {
    public static boolean pauseOnLeave(Context c){return c.getSharedPreferences("overlay",0).getBoolean("auxPauseAndroid",true);}
    public static void pauseOnLeave(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("auxPauseAndroid",on).apply();}
    public static AuxPlaybackPolicy.ReturnAction onReturn(Context c){
        String value=c.getSharedPreferences("overlay",0).getString("auxReturnPlayback",AuxPlaybackPolicy.ReturnAction.IF_AUTO_PAUSED.name());
        try{return AuxPlaybackPolicy.ReturnAction.valueOf(value);}catch(IllegalArgumentException|NullPointerException ignored){return AuxPlaybackPolicy.ReturnAction.IF_AUTO_PAUSED;}
    }
    public static void onReturn(Context c,AuxPlaybackPolicy.ReturnAction action){c.getSharedPreferences("overlay",0).edit().putString("auxReturnPlayback",action.name()).apply();}
    public static View controls(Activity activity){
        ThemeSettings.Palette p=ThemeSettings.resolve(activity).palette;LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);
        TextView title=new TextView(activity);title.setText(I18n.t(R.string.aux_playback_title));title.setTextSize(13);title.setTextColor(p.text);box.addView(title);
        CheckBox pause=new CheckBox(activity);pause.setTag("aux-pause-android");pause.setText(I18n.t(R.string.aux_pause_android));pause.setTextColor(p.text);pause.setChecked(pauseOnLeave(activity));pause.setOnCheckedChangeListener((v,on)->pauseOnLeave(activity,on));box.addView(pause);
        TextView returning=new TextView(activity);returning.setText(I18n.t(R.string.aux_return_playback));returning.setTextSize(12);returning.setTextColor(p.muted);box.addView(returning);
        String[] choices={I18n.t(R.string.aux_return_always),I18n.t(R.string.aux_return_if_paused),I18n.t(R.string.aux_return_nothing)};
        Button choice=ConsoleControls.button(activity,choices[onReturn(activity).ordinal()],p,false);choice.setTag("aux-return-android");choice.setOnClickListener(v->{
            AlertDialog dialog=new AlertDialog.Builder(activity,p.night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
                .setTitle(I18n.t(R.string.aux_return_playback)).setSingleChoiceItems(choices,onReturn(activity).ordinal(),(d,index)->{onReturn(activity,AuxPlaybackPolicy.ReturnAction.values()[index]);choice.setText(choices[index]);d.dismiss();}).setNegativeButton(I18n.t(R.string.player_title_help_close),null).create();dialog.show();
        });box.addView(choice,new LinearLayout.LayoutParams(-2,-2));
        TextView note=new TextView(activity);note.setText(I18n.t(R.string.aux_playback_notes));note.setTextSize(12);note.setTextColor(p.muted);box.addView(note);return box;
    }
    private AuxPlaybackSettings(){}
}
