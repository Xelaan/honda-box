package com.crewbeat.hondabox;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.*;
import android.widget.*;

/** Offline setup help. Platform settings are opened only after an explicit button tap. */
public final class BackgroundSetupGuide {
    private final Activity activity;private AlertDialog dialog;
    public BackgroundSetupGuide(Activity activity){this.activity=activity;}
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
    private TextView label(int resource,int size,int color){TextView text=new TextView(activity);text.setText(I18n.t(resource));text.setTextSize(size);text.setTextColor(color);text.setLineSpacing(dp(3),1);return text;}
    public View controls(){
        ThemeSettings.Palette p=ThemeSettings.resolve(activity).palette;LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.addView(label(R.string.setup_guides,12,p.accent));
        LinearLayout buttons=new LinearLayout(activity);buttons.setGravity(Gravity.START);buttons.setOrientation(activity.getResources().getConfiguration().screenWidthDp<600?LinearLayout.VERTICAL:LinearLayout.HORIZONTAL);
        int[] labels={R.string.setup_fyt_title,R.string.setup_dudu_title,R.string.setup_check_title};String[] tags={"setup-guide-fyt","setup-guide-dudu","setup-guide-check"};
        for(int i=0;i<labels.length;i++){final int page=i;Button button=ConsoleControls.button(activity,I18n.t(labels[i]),p,false);button.setTag(tags[i]);button.setOnClickListener(v->show(page));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(48));lp.topMargin=dp(8);lp.rightMargin=dp(8);buttons.addView(button,lp);}box.addView(buttons);return box;
    }
    private void section(LinearLayout body,int heading,int text,ThemeSettings.Palette p){
        TextView title=label(heading,14,p.text);title.setTypeface(Typeface.create("sans-serif-medium",0));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(body.getChildCount()==0?0:18);lp.bottomMargin=dp(7);body.addView(title,lp);TextView detail=label(text,14,p.muted);detail.setTextIsSelectable(true);body.addView(detail,new LinearLayout.LayoutParams(-1,-2));
    }
    private void show(int page){
        close();ThemeSettings.Palette p=ThemeSettings.resolve(activity).palette;
        LinearLayout body=new LinearLayout(activity);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(12),dp(22),dp(18));
        if(page==0){section(body,R.string.setup_choose_title,R.string.setup_choose_body,p);section(body,R.string.setup_fyt_steps_title,R.string.setup_fyt_steps,p);section(body,R.string.setup_android_title,R.string.setup_android_steps,p);section(body,R.string.setup_missing_title,R.string.setup_fyt_missing,p);}
        else if(page==1){section(body,R.string.setup_choose_title,R.string.setup_choose_body,p);section(body,R.string.setup_dudu_os_title,R.string.setup_dudu_os_steps,p);section(body,R.string.setup_dudu_launcher_title,R.string.setup_dudu_launcher_steps,p);section(body,R.string.setup_android_title,R.string.setup_android_steps,p);}
        else{section(body,R.string.setup_check_boot_title,R.string.setup_check_boot,p);section(body,R.string.setup_check_sleep_title,R.string.setup_check_sleep,p);section(body,R.string.setup_stop_title,R.string.setup_stop_body,p);section(body,R.string.setup_troubleshoot_title,R.string.setup_troubleshoot,p);}
        ScrollView scroll=new ScrollView(activity);scroll.setTag("setup-guide-content");scroll.setFillViewport(false);scroll.addView(body);
        int title=page==0?R.string.setup_fyt_title:page==1?R.string.setup_dudu_title:R.string.setup_check_title;
        dialog=new AlertDialog.Builder(activity,p.night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setTitle(I18n.t(title)).setView(scroll).setPositiveButton(I18n.t(R.string.player_title_help_close),(d,w)->{})
            .setNeutralButton(I18n.t(page==0?R.string.setup_open_fyt:R.string.setup_open_system),(d,w)->openSettings(page==0)).create();
        dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(p.accent);dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(p.accent);
        Window window=dialog.getWindow();if(window!=null){GradientDrawable bg=new GradientDrawable();bg.setColor(p.card);bg.setCornerRadius(dp(20));window.setBackgroundDrawable(bg);View decor=activity.getWindow().getDecorView();int width=decor.getWidth()>0?decor.getWidth():activity.getResources().getDisplayMetrics().widthPixels,height=decor.getHeight()>0?decor.getHeight():activity.getResources().getDisplayMetrics().heightPixels;window.setLayout(Math.min(dp(720),width-dp(40)),Math.min(dp(620),height-dp(48)));}
    }
    private void openSettings(boolean fyt){
        Intent intent=fyt?activity.getPackageManager().getLaunchIntentForPackage("com.syu.settings"):null;
        if(fyt&&intent==null)Toast.makeText(activity,I18n.t(R.string.setup_fyt_fallback),Toast.LENGTH_LONG).show();
        try{activity.startActivity(intent==null?new Intent(Settings.ACTION_SETTINGS):intent);}
        catch(RuntimeException e){Toast.makeText(activity,I18n.t(R.string.settings_unavailable),Toast.LENGTH_LONG).show();}
    }
    public void close(){if(dialog!=null){dialog.dismiss();dialog=null;}}
}
