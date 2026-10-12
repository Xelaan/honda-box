package com.crewbeat.hondabox.tests;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.crewbeat.hondabox.*;
import java.io.*;
import java.util.*;

/** Emulator-only documentation capture. Synthetic data, actual UI, no vehicle commands. */
public final class DocumentationScreenshots extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;
    private Context context;
    private Activity activity;
    private ClimateOverlay climate;
    private ClimateStripOverlay compact;
    private VolumeOverlay volume;
    private SourceOverlay source;
    private TrackOverlay track;
    private CarMenuOverlay menu;
    private int images;
    private String theme;

    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void event(int code,int value){state.event(code,new int[]{value},null,null);}
    private void frame(int type,int... payload){
        int[] bytes=new int[payload.length+4];bytes[0]=46;bytes[1]=type;bytes[2]=payload.length;
        int sum=type+payload.length;
        for(int i=0;i<payload.length;i++){bytes[i+3]=payload[i];sum+=payload[i];}
        bytes[bytes.length-1]=255-(sum&255);state.event(1019,bytes,null,null);
    }
    private void text(int field,String value){
        int[] payload=new int[value.length()+1];payload[0]=field;
        for(int i=0;i<value.length();i++)payload[i+1]=value.charAt(i);frame(7,payload);
    }
    private void seed(String mode){
        state.newSession();state.running(true);state.recording(false);
        state.status("Connected","Synthetic documentation data",true);event(1000,6291908);
        int[] codes={10,11,13,14,18,19,20,21,27,28,12,16,17,33,36,49};
        int[] values={1,1,1,1,0,1,1,4,44,46,1,0,0,1,0,1};
        for(int i=0;i<codes.length;i++)event(codes[i],values[i]);
        event(132,18);
        if(mode.equals("radio")){
            event(98,1);frame(3,0x80,18);event(115,2);
            int[] presets={1,55,241,275,343,409};
            for(int i=0;i<6;i++)event(120+i,presets[i]);
            frame(4,2,0,241,3,0x81);
        }else if(mode.equals("ipod")){
            event(98,2);frame(3,0x81,18);frame(6,50,34,34);event(102,0);
            text(7,"Dreams - Fleetwood Mac");
        }else if(mode.equals("usb")){
            event(98,4);frame(3,0x83,18);frame(5,0,0,1,2,0,0x12,0x01,0x24);
            text(7,"Midnight City - M83");
        }else{
            event(98,2);frame(3,0x81,18);frame(6,1,1,1);
            frame(5,2,0,1,1,0,0x15,0x02,0x34);text(1,"Hotel California");
        }
    }
    private void main(){activity=startActivitySync(new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
    private void player(AudioMode mode){activity=startActivitySync(AudioPlayerActivity.intent(context,mode,false));}
    private Object field(Object owner,Class<?> type,String name)throws Exception{
        java.lang.reflect.Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);
    }
    private void capture(String feature)throws Exception{
        SystemClock.sleep(600);
        Bitmap bitmap=getUiAutomation().takeScreenshot();
        if(bitmap==null||bitmap.getWidth()!=1280||bitmap.getHeight()!=480)throw new AssertionError("Expected 1280x480 emulator");
        File folder=new File(context.getExternalFilesDir(null),"documentation");folder.mkdirs();
        try(OutputStream out=new FileOutputStream(new File(folder,feature+"-"+theme+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}
        bitmap.recycle();images++;
        Bundle progress=new Bundle();progress.putString("screenshot",feature+"-"+theme);sendStatus(0,progress);
    }
    private void close(){
        runOnMainSync(()->{
            if(compact!=null)compact.close();if(climate!=null)climate.close();
            if(volume!=null)volume.close();if(source!=null)source.close();
            if(track!=null)track.close();if(menu!=null)menu.close();
            if(activity!=null)activity.finish();
        });
        compact=null;climate=null;volume=null;source=null;track=null;menu=null;activity=null;
        SystemClock.sleep(200);
    }
    private void scrollTo(View value)throws Exception{
        ScrollView scroll=(ScrollView)field(activity,SettingsScreenActivity.class,"scroll");
        runOnMainSync(()->{Rect r=new Rect();value.getDrawingRect(r);scroll.offsetDescendantRectToMyCoords(value,r);scroll.scrollTo(0,Math.max(0,r.top-20));});
    }
    private View findText(View root,String text){
        if(root instanceof TextView&&((TextView)root).getText().toString().equals(text))return root;
        if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++){View v=findText(((ViewGroup)root).getChildAt(i),text);if(v!=null)return v;}
        return null;
    }
    private void menuRow(String label){
        int[] payload=new int[49];payload[0]=0x20;
        for(int i=1;i<49;i++)payload[i]=32;
        for(int i=0;i<Math.min(16,label.length());i++)payload[17+i]=label.charAt(i);
        frame(9,payload);
    }
    private void gallery()throws Exception{
        seed("ipod");main();capture("selector");close();
        seed("radio");player(AudioMode.RADIO);capture("radio");close();
        seed("radio");event(115,4);frame(4,4,0,9,0,1);player(AudioMode.RADIO);capture("radio-am");close();
        seed("cd");player(AudioMode.CD);capture("cd");close();
        seed("usb");player(AudioMode.USB);capture("usb");close();
        seed("ipod");player(AudioMode.USB);capture("ipod");close();
        seed("radio");main();runOnMainSync(()->{climate=new ClimateOverlay(context);climate.start();climate.showFromStrip(false);});capture("climate-full");close();
        seed("cd");player(AudioMode.CD);
        context.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();
        runOnMainSync(()->{climate=new ClimateOverlay(context);climate.start();compact=new ClimateStripOverlay(context,climate);compact.start();compact.refreshOptions();});
        capture("climate-compact");
        runOnMainSync(()->{try{((View)field(compact,ClimateStripOverlay.class,"strip")).performClick();}catch(Exception e){throw new RuntimeException(e);}});
        capture("climate-folded");close();
        seed("cd");main();runOnMainSync(()->{volume=new VolumeOverlay(context);volume.start();});capture("volume-overlay");close();
        seed("cd");main();runOnMainSync(()->{track=new TrackOverlay(context);track.start();});SystemClock.sleep(500);
        frame(5,2,0,1,1,0,0x16,0,0);text(1,"Take It Easy");SystemClock.sleep(900);capture("track-overlay");
        if(!state.overlaySnapshot().trackOverlayVisible)throw new AssertionError("Track card not visible");close();
        seed("radio");main();runOnMainSync(()->{track=new TrackOverlay(context);track.start();});SystemClock.sleep(500);
        frame(4,2,1,19,4,0x81);SystemClock.sleep(900);capture("station-overlay");close();
        seed("cd");event(98,3);frame(3,0x02,18);main();capture("selector-off");runOnMainSync(()->{source=new SourceOverlay(context);source.start();source.announceCurrentSource();});capture("source-off");close();
        seed("cd");player(AudioMode.CD);runOnMainSync(()->{menu=new CarMenuOverlay(context);menu.start();});SystemClock.sleep(1100);
        frame(3,0xC1,18);menuRow("BASS +1");menuRow("TREBLE CENTER");menuRow("FADER CENTER");menuRow("SVC MID");capture("oem-menu");
        if(!state.overlaySnapshot().carMenuOverlayVisible)throw new AssertionError("OEM menu not visible");close();
        seed("usb");player(AudioMode.USB);runOnMainSync(()->activity.findViewById(android.R.id.content).findViewWithTag("player-controls-info").performClick());capture("player-help");close();
        ClimateOverlay.enabled(context,true);seed("radio");activity=startActivitySync(new Intent(context,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));capture("settings");
        View heading=findText(activity.findViewById(android.R.id.content),I18n.t(R.string.settings_large_climate));
        if(heading!=null){scrollTo(heading);capture("climate-settings");}close();
        seed("radio");activity=startActivitySync(new Intent(context,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        scrollTo(activity.findViewById(android.R.id.content).findViewWithTag("debug-radio-fields"));capture("debug-radio");close();ClimateOverlay.enabled(context,false);
    }
    private void restore(String name,Map<String,?> values){
        SharedPreferences.Editor editor=context.getSharedPreferences(name,0).edit().clear();
        for(Map.Entry<String,?> entry:values.entrySet()){
            String key=entry.getKey();Object v=entry.getValue();
            if(v instanceof Boolean)editor.putBoolean(key,(Boolean)v);else if(v instanceof String)editor.putString(key,(String)v);
            else if(v instanceof Integer)editor.putInt(key,(Integer)v);else if(v instanceof Long)editor.putLong(key,(Long)v);
            else if(v instanceof Float)editor.putFloat(key,(Float)v);
            else if(v instanceof Set)editor.putStringSet(key,new HashSet<>((Set<String>)v));
        }editor.commit();
    }
    @Override public void onStart(){
        context=getTargetContext();Bundle result=new Bundle();int outcome=Activity.RESULT_OK;
        Map<String,Map<String,?>> old=new LinkedHashMap<>();
        for(String name:new String[]{"theme","language","overlay","background","diagnostics"})old.put(name,context.getSharedPreferences(name,0).getAll());
        try{
            if(!Build.FINGERPRINT.contains("generic")&&!Build.FINGERPRINT.contains("emulator")&&!Build.HARDWARE.equals("ranchu")&&!Build.HARDWARE.equals("goldfish"))throw new AssertionError("Emulator only");
            context.stopService(new Intent(context,ProbeService.class));SystemClock.sleep(300);
            I18n.language(context,"en");ThemeSettings.accent(context,AccentColors.DEFAULT);DiagnosticSettings.enabled(context,false);
            ClimateOverlay.enabled(context,false);ClimateStripOverlay.enabled(context,true);
            ClimateStripOverlay.expandButton(context,true);ClimateStripOverlay.buttonAutoHide(context,false);ClimateStripOverlay.alwaysAutoHide(context,false);
            VolumeOverlay.options(context,true,false);TrackOverlay.enabled(context,true);SourceOverlay.enabled(context,true);
            SourceOverlay.keepVisibleWhenOff(context,true);CarMenuOverlay.enabled(context,true);
            for(String appearance:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){theme=appearance;ThemeSettings.mode(context,appearance);gallery();}
            result.putString("result","Captured "+images+" synthetic feature screenshots, no vehicle commands");
        }catch(Throwable error){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(error));}
        finally{close();state.newSession();state.running(false);for(Map.Entry<String,Map<String,?>> entry:old.entrySet())restore(entry.getKey(),entry.getValue());}
        finish(outcome,result);
    }
}
