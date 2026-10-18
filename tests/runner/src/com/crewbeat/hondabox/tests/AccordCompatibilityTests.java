package com.crewbeat.hondabox.tests;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.crewbeat.hondabox.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Emulator-only model equivalence, persistent checkbox and real read-only reconnect. */
public final class AccordCompatibilityTests extends Instrumentation {
    private int checks;private Context c;private Activity activity;
    private final ProbeState live=ProbeState.INSTANCE;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;}
    private void main(Runnable action){Throwable[] error={null};runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError("UI action",error[0]);}
    private void await(BooleanSupplier test,String text){long end=SystemClock.uptimeMillis()+7000;while(!test.getAsBoolean()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(30);check(test.getAsBoolean(),text);}
    private ProbeState model(int profile,boolean override){ProbeState s=new ProbeState();s.processAsHondaAccord(override);s.running(true);s.status("test","local replay",true);s.event(1000,new int[]{profile},null,null);return s;}
    private int[] packet(int type,int... payload){int[] raw=new int[payload.length+4];raw[0]=46;raw[1]=type;raw[2]=payload.length;int sum=type+payload.length;for(int i=0;i<payload.length;i++){raw[3+i]=payload[i];sum+=payload[i];}raw[raw.length-1]=(~sum)&255;return raw;}
    private void frame(ProbeState s,int type,int... payload){s.event(1019,packet(type,payload),null,null);}
    private void menu(ProbeState s,String text){int[] p=new int[49];p[0]=32;for(int i=0;i<Math.min(16,text.length());i++)p[17+i]=text.charAt(i);frame(s,9,p);}
    private void replay(ProbeState s){
        s.event(10,new int[]{0},null,null);s.event(21,new int[]{4},null,null);s.event(27,new int[]{44},null,null);s.event(28,new int[]{46},null,null);
        s.event(98,new int[]{3},null,null);frame(s,3,0x80,10);frame(s,4,4,0,100,2,0x81);
    }
    private void models()throws Exception{
        for(int profile:new int[]{3604932,6357444,12321220,9175479,123456}){
            ProbeState off=model(profile,false);replay(off);check(off.snapshot().bsjSource==null,"override off does not decode raw source");check(off.snapshot().rawAudioSource()==3,"logical source retained by default");check(off.snapshot().climatePower()==0,"no automatic cross-profile power fallback");check(!off.snapshot().dualInferred(),"no automatic cross-profile DUAL inference");
            ProbeState expected=model(6291908,false),actual=model(profile,true);replay(expected);replay(actual);
            ProbeState.Snapshot a=actual.snapshot(),n=expected.snapshot();check(a.accord()&&a.honda(),"explicit Accord schema active");check(a.rawAudioSource().equals(n.rawAudioSource())&&a.source().equals(n.source()),"raw source and radio bands match native Accord");check(AudioMode.reported(a)==AudioMode.RADIO,"player recognition enabled");check(a.preset(2).equals(n.preset(2)),"radio preset cache enabled");check(a.dual().equals(n.dual())&&a.dualInferred(),"DUAL fallback enabled");check(a.number(1000)==profile&&a.number(98)==3,"actual profile and normalized source untouched");
            JSONObject exported=new JSONObject(actual.exportJson());check(exported.getJSONObject("compatibility").getInt("actualProfile")==profile,"export retains actual profile");check(exported.getJSONObject("compatibility").getBoolean("processAsHondaAccord"),"export labels override");check(actual.radioPresets.json().getInt("profile")==profile,"preset cache retains actual identity");
            frame(actual,3,0x81,11);frame(actual,5,1,0,0,0x15,0,0x22,0,0);frame(actual,7,1,'T','r','a','c','k');
            check(AudioMode.reported(actual.snapshot())==AudioMode.CD,"CD screen recognized");check(!actual.snapshot().combinedMediaText().equals("—"),"raw playback text decoded");String title=actual.snapshot().combinedMediaText();frame(actual,3,0x81,12);frame(actual,7,1);check(actual.snapshot().combinedMediaText().equals(title),"volume blank retains playback title");
            menu(actual,"Artists");check(actual.snapshot().mediaDevice==ProbeState.MediaDevice.IPOD,"iPod menu inference enabled");check(!actual.snapshot().cachedMenu.isEmpty(),"menu cache available");check(actual.carMenuMemory.json().getInt("profile")==profile,"menu cache retains actual identity");
            actual.newSession();actual.status("test","",true);actual.event(1000,new int[]{profile},null,null);frame(actual,3,0x81,11);frame(actual,6,0x32,0x22,0x22);check(actual.snapshot().mediaDevice==ProbeState.MediaDevice.IPOD,"marker06 CD to iPod inference enabled");
            frame(actual,0x0E,new int[13]);check(actual.snapshot().vehiclePacket0E!=null,"experimental Accord packet capture enabled");
            long session=actual.snapshot().sessionId;actual.processAsHondaAccord(false);a=actual.snapshot();check(a.sessionId>session&&!a.connected,"toggle rebaselines session");check(a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&a.bsjSource==null&&a.bsjValues.isEmpty(),"toggle clears inferred state");actual.status("test","",true);actual.event(1000,new int[]{profile},null,null);check(!actual.snapshot().accord()&&actual.snapshot().cachedMenu.isEmpty(),"disable restores original handling");check(actual.radioPresets.snapshot().isEmpty(),"disable hides Accord cache");
            actual.processAsHondaAccord(true);actual.status("test","",true);actual.event(1000,new int[]{profile},null,null);frame(actual,3,0x80,10);frame(actual,4,2,0,99,1,1);actual.event(1000,new int[]{profile+1},null,null);check(actual.radioPresets.snapshot().isEmpty()&&actual.carMenuMemory.summary(1).isEmpty(),"profile change isolates caches");
        }
        check(model(6291908,false).snapshot().accord()&&model(6291908,true).snapshot().accord(),"native profile unaffected by switch");
        ProbeState unknown=new ProbeState();unknown.processAsHondaAccord(true);check(!unknown.snapshot().accord(),"missing profile does not invent schema");for(int p:new int[]{0,-1})check(!model(p,true).snapshot().accord(),"invalid profile does not invent schema");
    }
    private String fixture(Intent i)throws Exception{CountDownLatch latch=new CountDownLatch(1);String[] result={null};i.setComponent(new ComponentName("com.syu.ms","app.ControlReceiver"));c.sendOrderedBroadcast(i,null,new BroadcastReceiver(){@Override public void onReceive(Context x,Intent y){result[0]=getResultData();latch.countDown();}},new Handler(Looper.getMainLooper()),0,null,null);check(latch.await(4,TimeUnit.SECONDS),"fixture responded");return result[0];}
    private void uiAndService()throws Exception{
        c.getSharedPreferences("overlay",0).edit().remove("processAsHondaAccord").commit();live.processAsHondaAccord(false);check(!AccordCompatibility.enabled(c),"setting defaults OFF");
        for(String language:new String[]{"en","ru"})for(String theme:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){
            main(()->{I18n.language(c,language);ThemeSettings.mode(c,theme);});activity=startActivitySync(new Intent(c,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            main(()->{CheckBox box=activity.findViewById(android.R.id.content).findViewWithTag("process-as-honda-accord");check(box!=null&&!box.isChecked(),"checkbox in normal settings");check(box.getText().toString().equals(I18n.t(R.string.accord_compatibility_toggle)),"localized label");box.performClick();check(AccordCompatibility.enabled(c),"checkbox persists ON");check(!live.overlaySnapshot().running,"setting does not autostart stopped service");box.performClick();check(!AccordCompatibility.enabled(c),"checkbox persists OFF");activity.finish();});activity=null;
        }
        fixture(new Intent().putExtra("resetCommands",true));fixture(new Intent().putExtra("silent",false).putExtra("profile",3604932).putExtra("source",1).putExtra("raw",new int[]{0}));
        main(()->BackgroundSettings.open(c));await(()->live.overlaySnapshot().connected&&Integer.valueOf(3604932).equals(live.overlaySnapshot().number(1000)),"fixture connected using real non-Accord profile");check(!live.overlaySnapshot().accord(),"service starts with compatibility OFF");long session=live.overlaySnapshot().sessionId;
        main(()->AccordCompatibility.enabled(c,true));await(()->live.overlaySnapshot().connected&&live.overlaySnapshot().sessionId>session&&Integer.valueOf(3604932).equals(live.overlaySnapshot().number(1000))&&live.overlaySnapshot().accord(),"setting applies live and reconnects read-only");
        check(AccordCompatibility.enabled(c),"preference survives reconnect");check(live.overlaySnapshot().number(1000)==3604932,"head-unit profile unchanged");
        main(()->AccordCompatibility.enabled(c,false));await(()->live.overlaySnapshot().connected&&Integer.valueOf(3604932).equals(live.overlaySnapshot().number(1000))&&!live.overlaySnapshot().accord(),"disabling live restores default processing");
        check("[]".equals(fixture(new Intent().putExtra("getCommands",true))),"toggling sends zero vehicle commands");
    }
    private void restore(String name,Map<String,?> old){SharedPreferences.Editor e=c.getSharedPreferences(name,0).edit().clear();for(Map.Entry<String,?> item:old.entrySet()){Object v=item.getValue();if(v instanceof String)e.putString(item.getKey(),(String)v);else if(v instanceof Boolean)e.putBoolean(item.getKey(),(Boolean)v);else if(v instanceof Integer)e.putInt(item.getKey(),(Integer)v);else if(v instanceof Long)e.putLong(item.getKey(),(Long)v);else if(v instanceof Float)e.putFloat(item.getKey(),(Float)v);}e.commit();}
    @Override public void onStart(){Bundle out=new Bundle();int result=0;c=getTargetContext();if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){out.putString("failure","Emulator required");finish(1,out);return;}
        String[] names={"overlay","background","language","theme","radioPresets","carMenuMemory"};Map<String,Map<String,?>> saved=new LinkedHashMap<>();for(String name:names)saved.put(name,new HashMap<>(c.getSharedPreferences(name,0).getAll()));
        try{main(()->BackgroundSettings.stop(c));SystemClock.sleep(350);live.running(false);models();
            SharedPreferences.Editor e=c.getSharedPreferences("overlay",0).edit();for(String key:new String[]{"volume","source","track","climate","climateStrip","carMenu","unknown","audioScreens","auxPauseAndroid"})e.putBoolean(key,false);e.commit();uiAndService();out.putString("result","PASS: "+checks+" Accord override model, settings RU/EN day/night and live reconnect checks; zero vehicle commands");
        }catch(Throwable e){result=1;out.putString("failure",android.util.Log.getStackTraceString(e));}
        finally{if(activity!=null)main(()->activity.finish());main(()->BackgroundSettings.stop(c));SystemClock.sleep(350);try{fixture(new Intent().putExtra("profile",6291908).putExtra("source",1).putExtra("raw",new int[]{0}));}catch(Exception ignored){}for(String name:names)restore(name,saved.get(name));live.running(false);live.processAsHondaAccord(AccordCompatibility.enabled(c));live.newSession();I18n.init(c);}
        finish(result,out);
    }
}
