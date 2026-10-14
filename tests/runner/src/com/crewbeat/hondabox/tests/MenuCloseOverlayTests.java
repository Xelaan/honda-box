package com.crewbeat.hondabox.tests;

import android.app.Instrumentation;
import android.content.*;
import android.os.*;
import com.crewbeat.hondabox.*;
import java.util.*;

/** Exercises the real menu window with locally replayed BSJ packets, without CAN writes. */
public final class MenuCloseOverlayTests extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;
    private CarMenuOverlay overlay;
    private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean ok,String label){if(!ok)throw new AssertionError(label);checks++;}
    private void frame(int type,int... payload){
        int[] raw=new int[payload.length+4];raw[0]=46;raw[1]=type;raw[2]=payload.length;
        int sum=type+payload.length;for(int i=0;i<payload.length;i++){raw[i+3]=payload[i];sum+=payload[i];}
        raw[raw.length-1]=(~sum)&255;state.event(1019,raw,null,null);
    }
    private void menu(String row){int[] p=new int[49];p[0]=32;for(int i=0;i<Math.min(16,row.length());i++)p[17+i]=row.charAt(i);frame(9,p);}
    private void render(){runOnMainSync(()->overlay.refreshOptions());}
    private void visible(boolean expected,String label)throws Exception{
        render();check(state.overlaySnapshot().carMenuOverlayVisible==expected,label+" exported visibility");
        java.lang.reflect.Field field=CarMenuOverlay.class.getDeclaredField("view");field.setAccessible(true);
        check((field.get(overlay)!=null)==expected,label+" actual overlay window");
    }
    @Override public void onStart(){Bundle result=new Bundle();int outcome=0;Context c=getTargetContext();
        Map<String,?> background=null;SharedPreferences prefs=c.getSharedPreferences("background",0);
        SharedPreferences settings=c.getSharedPreferences("overlay",0);boolean hadMenu=settings.contains("carMenu"),oldMenu=CarMenuOverlay.enabled(c);
        try{
            check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"disposable emulator required");
            background=new HashMap<>(prefs.getAll());BackgroundSettings.stop(c);SystemClock.sleep(300);CarMenuOverlay.enabled(c,true);
            for(int scenario=0;scenario<4;scenario++){
                int source=scenario==0||scenario==2?2:4;boolean ipod=scenario>=2;
                String name=ipod?"iPod on source"+source:source==2?"CD":"USB";
                state.newSession();state.status("test","local menu replay",true);state.event(1000,new int[]{6291908},null,null);
                frame(3,0xC0|source-1,7);
                runOnMainSync(()->{overlay=new CarMenuOverlay(c);overlay.start();});SystemClock.sleep(1000);render();
                menu(ipod?"Artists":"03:Track 03");visible(true,name+" menu opens");
                if(ipod)check(state.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD,name+" inferred device");
                frame(3,0x80|source-1,7);visible(false,name+" explicit close");
                frame(5,source==2?1:0,0,0,source==2?1:2,0,3,0,0);
                frame(7,0,'A','l','b','u','m');frame(7,1,'T','r','a','c','k');
                visible(false,name+" playback and TITLE do not reopen menu");
                menu("04:Next track");visible(true,name+" new menu content reopens");
                frame(3,0x80|source-1,7);visible(false,name+" repeated close after new content");
                SystemClock.sleep(300);visible(false,name+" stays closed on polling");
                runOnMainSync(()->overlay.close());overlay=null;
            }
            result.putString("result","PASS: "+checks+" real menu overlay checks; CD, USB and iPod on both source carriers; no vehicle commands");
        }catch(Throwable e){result.putString("failure",android.util.Log.getStackTraceString(e));outcome=1;}
        finally{if(background!=null){if(overlay!=null)runOnMainSync(()->overlay.close());state.newSession();state.running(false);
            if(hadMenu)CarMenuOverlay.enabled(c,oldMenu);else settings.edit().remove("carMenu").commit();
            SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> e:background.entrySet()){Object v=e.getValue();if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof Long)edit.putLong(e.getKey(),(Long)v);else if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);else if(v instanceof String)edit.putString(e.getKey(),(String)v);}edit.commit();
        }}
        finish(outcome,result);
    }
}
