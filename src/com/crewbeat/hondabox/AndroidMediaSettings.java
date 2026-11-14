package com.crewbeat.hondabox;
import android.app.Activity;import android.content.*;import android.provider.Settings;import android.view.View;import android.widget.*;
public final class AndroidMediaSettings {
 public static boolean enabled(Context c){return c.getSharedPreferences("overlay",0).getBoolean("androidMetadata",true);}
 public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("androidMetadata",on).apply();AndroidMediaMonitor.refresh(c);}
 public static boolean access(Context c){String list=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");if(list==null)return false;ComponentName ours=new ComponentName(c,AndroidMediaListener.class);for(String part:list.split(":"))if(ours.equals(ComponentName.unflattenFromString(part)))return true;return false;}
 public static View controls(Activity a){ThemeSettings.Palette p=ThemeSettings.resolve(a).palette;LinearLayout box=new LinearLayout(a);box.setOrientation(1);
  CheckBox enabled=new CheckBox(a);enabled.setTag("android-metadata-enabled");enabled.setText(I18n.t(R.string.android_media_enabled));enabled.setTextColor(p.text);enabled.setChecked(enabled(a));enabled.setOnCheckedChangeListener((v,on)->enabled(a,on));box.addView(enabled);
  TextView note=new TextView(a);note.setText(I18n.t(R.string.android_media_notes));note.setTextColor(p.muted);note.setTextSize(12);box.addView(note);
  Button permission=ConsoleControls.button(a,I18n.t(R.string.android_media_access),p,false);permission.setTag("android-metadata-access");permission.setOnClickListener(v->{try{a.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(RuntimeException e){Toast.makeText(a,I18n.t(R.string.settings_unavailable),Toast.LENGTH_SHORT).show();}});box.addView(permission,new LinearLayout.LayoutParams(-2,-2));
  TextView status=new TextView(a);status.setTag("android-metadata-status");status.setTextColor(p.muted);status.setTextSize(12);box.addView(status);status(a,box);return box;
 }
 public static void status(Context c,View root){TextView status=root.findViewWithTag("android-metadata-status");if(status!=null)status.setText(I18n.t(access(c)?R.string.android_media_access_on:R.string.android_media_access_off));}
 private AndroidMediaSettings(){}
}
