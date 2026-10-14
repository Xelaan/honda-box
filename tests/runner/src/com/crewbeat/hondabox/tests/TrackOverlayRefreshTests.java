package com.crewbeat.hondabox.tests;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import android.view.View;
import com.crewbeat.hondabox.*;
import java.util.*;

/** Real track-card refresh and lifetime checks; synthetic packets, no CAN commands. */
public final class TrackOverlayRefreshTests extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;private TrackOverlay overlay;private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private Object field(Object value,String name)throws Exception{java.lang.reflect.Field f=value.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(value);}
    private void render(){runOnMainSync(()->{try{java.lang.reflect.Method m=TrackOverlay.class.getDeclaredMethod("render");m.setAccessible(true);m.invoke(overlay);}catch(Exception e){throw new RuntimeException(e);}});}
    private void frame(int type,int... p){int[] raw=new int[p.length+4];raw[0]=46;raw[1]=type;raw[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){raw[i+3]=p[i];sum+=p[i];}raw[raw.length-1]=(~sum)&255;state.event(1019,raw,null,null);}
    private void text(int kind,String value){int[] p=new int[value.length()+1];p[0]=kind;for(int i=0;i<value.length();i++)p[i+1]=value.charAt(i);frame(7,p);}
    private void track(int source,int n){frame(5,source==2?1:0,0,1,source==2?1:2,0,n,0,0);}
    @Override public void onStart(){Bundle result=new Bundle();int outcome=0;Context c=getTargetContext();
        SharedPreferences bg=c.getSharedPreferences("background",0),prefs=c.getSharedPreferences("overlay",0);Map<String,?> saved=null;
        boolean old=TrackOverlay.enabled(c),had=prefs.contains("tracks");String language=I18n.language(c);
        try{
            check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"disposable emulator required");
            saved=new HashMap<>(bg.getAll());BackgroundSettings.stop(c);SystemClock.sleep(300);TrackOverlay.enabled(c,true);I18n.language(c,"en");
            for(int source:new int[]{2,4,-1}){
                String name=source==2?"CD":source==4?"USB":"iPod";
                state.newSession();state.status("test","local replay",true);state.event(1000,new int[]{6291908},null,null);state.event(98,new int[]{source==-1?2:source},null,null);
                if(source==-1){frame(6,0x32,0x22,0x22);text(7,"First song");check(state.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD,"iPod detected");}else{track(source,1);text(1,"First song");}
                runOnMainSync(()->{overlay=new TrackOverlay(c);overlay.start();});SystemClock.sleep(300);render();
                if(source==-1){text(7,"Second song");render();SystemClock.sleep(1450);}else{track(source,2);render();SystemClock.sleep(2050);}
                render();View card=(View)field(overlay,"view");check(card!=null,name+" announcement opens");
                long deadline=(Long)field(overlay,"until");String original=(String)field(card,"title");
                if(source!=-1)check(original.equals("Track 2"),name+" fallback before late TITLE");
                text(source==-1?5:1,"Updated title");render();
                check(field(overlay,"view")==card,name+" reuses existing card");check(field(card,"title").equals("Updated title"),name+" selected title refreshed");
                check(card.getContentDescription().toString().endsWith(": Updated title"),name+" accessibility caption refreshed");
                check((Long)field(overlay,"until")==deadline&&(Long)field(card,"deadline")==deadline,name+" deadline preserved");
                text(3,"Artist selected via TITLE");render();check(field(card,"title").equals("Artist selected via TITLE"),name+" field rotation refreshes card");
                check(field(overlay,"view")==card&&(Long)field(overlay,"until")==deadline,name+" field rotation does not reannounce");
                if(source!=-1){track(source,3);text(3,"Different track");render();check(field(card,"title").equals("Artist selected via TITLE"),name+" different track cannot overwrite previous card");}
                else{long wait=deadline-SystemClock.uptimeMillis()+250;if(wait>0)SystemClock.sleep(wait);render();check(field(overlay,"view")==null,name+" expires on original deadline");text(3,"Later artist");render();check(field(overlay,"view")==null,name+" hidden card stays hidden on TITLE");}
                runOnMainSync(()->overlay.close());overlay=null;
            }
            result.putString("result","PASS: "+checks+" real track-overlay refresh checks");
        }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
        finally{if(saved!=null){if(overlay!=null)runOnMainSync(()->overlay.close());state.newSession();state.running(false);I18n.language(c,language);if(had)TrackOverlay.enabled(c,old);else prefs.edit().remove("tracks").commit();SharedPreferences.Editor edit=bg.edit().clear();for(Map.Entry<String,?> e:saved.entrySet()){Object v=e.getValue();if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof Long)edit.putLong(e.getKey(),(Long)v);else if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);else if(v instanceof String)edit.putString(e.getKey(),(String)v);}edit.commit();}}
        finish(outcome,result);
    }
}
