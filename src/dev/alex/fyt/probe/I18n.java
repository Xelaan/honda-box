package dev.alex.fyt.probe;

import android.app.Activity;
import android.content.*;
import android.content.res.*;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

/** App-only language selection; device locale, raw packets and OEM metadata are untouched. */
public final class I18n {
    private static Context app;private static volatile Context localized;private static String key="";private static int revision;
    private static List<String[]> pairs;
    public static synchronized void init(Context c){app=c.getApplicationContext();resolve();}
    public static String language(Context c){return c.getSharedPreferences("language",0).getString("mode","system");}
    public static synchronized void language(Context c,String mode){if(!mode.equals("system")&&!mode.equals("ru")&&!mode.equals("en"))throw new IllegalArgumentException("language");c.getSharedPreferences("language",0).edit().putString("mode",mode).apply();if(app==null)app=c.getApplicationContext();resolve();}
    private static synchronized void resolve(){if(app==null)return;String mode=language(app);String system=(android.os.Build.VERSION.SDK_INT>=24?Resources.getSystem().getConfiguration().getLocales().get(0):Resources.getSystem().getConfiguration().locale).getLanguage();String next=mode.equals("system")?(system.equals("ru")?"ru":"en"):mode;if(next.equals(key)&&localized!=null)return;
        Configuration config=new Configuration(app.getResources().getConfiguration());config.setLocale(new Locale(next));localized=app.createConfigurationContext(config);key=next;revision++;
    }
    public static int revision(){resolve();return revision;}
    public static String t(int id){resolve();Context c=localized;return c==null?"…":c.getString(id);}
    public static synchronized String stored(String text){if(text==null)return "";resolve();if(app==null)return text;
        if(pairs==null){pairs=new ArrayList<>();Configuration ru=new Configuration(app.getResources().getConfiguration());ru.setLocale(new Locale("ru"));Configuration en=new Configuration(ru);en.setLocale(Locale.ENGLISH);Context a=app.createConfigurationContext(ru),b=app.createConfigurationContext(en);for(int id:I18nCatalog.IDS)pairs.add(new String[]{a.getString(id),b.getString(id)});}
        boolean english=key.equals("en");List<String[]> ordered=new ArrayList<>(pairs);int from=english?0:1,to=english?1:0;ordered.sort((a,b)->Integer.compare(b[from].length(),a[from].length()));StringBuilder result=new StringBuilder();
        for(int pos=0;pos<text.length();){String[] found=null;for(String[] pair:ordered){String s=pair[from];if(s.isEmpty()||!text.startsWith(s,pos))continue;int end=pos+s.length();if(Character.isLetter(s.charAt(0))&&pos>0&&Character.isLetter(text.charAt(pos-1)))continue;if(Character.isLetter(s.charAt(s.length()-1))&&end<text.length()&&Character.isLetter(text.charAt(end)))continue;found=pair;break;}if(found==null)result.append(text.charAt(pos++));else{result.append(found[to]);pos+=found[from].length();}}
        return result.toString();
    }
    public static android.view.View controls(Activity a,Runnable changed){LinearLayout box=new LinearLayout(a);box.setOrientation(1);ThemeSettings.Palette p=ThemeSettings.resolve(a).palette;TextView title=new TextView(a);title.setText(t(R.string.language));title.setTextColor(p.muted);title.setTextSize(12);box.addView(title);LinearLayout row=new LinearLayout(a);String[] modes={"system","ru","en"},labels={t(R.string.system_language),"Русский","English"};int height=Math.round(48*a.getResources().getDisplayMetrics().density),gap=Math.round(8*a.getResources().getDisplayMetrics().density);
        for(int i=0;i<modes.length;i++){final String mode=modes[i];Button b=ConsoleControls.button(a,labels[i],p,mode.equals(language(a)));b.setOnClickListener(v->{language(a,mode);changed.run();if(ProbeState.INSTANCE.overlaySnapshot().running)try{a.startService(new Intent(a,ProbeService.class).setAction(ProbeService.OVERLAY));}catch(RuntimeException ignored){}});LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,height);lp.setMargins(0,0,i<2?gap:0,0);row.addView(b,lp);}box.addView(row);return box;
    }
    private I18n(){}
}
