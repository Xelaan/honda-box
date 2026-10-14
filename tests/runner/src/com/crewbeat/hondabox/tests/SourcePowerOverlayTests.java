package com.crewbeat.hondabox.tests;
import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import com.crewbeat.hondabox.*;
/** Real overlay-window lifecycle on emulator only; no CAN service or commands. */
public final class SourcePowerOverlayTests extends Instrumentation {
 private int checks;private SourceOverlay overlay;private final ProbeState state=ProbeState.INSTANCE;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private void frame(int b,int volume){int[] raw={46,3,2,b,volume,255-((3+2+b+volume)&255)};state.event(1019,raw,null,null);}
 private void render(){runOnMainSync(()->overlay.refreshOptions());}
 private ProbeService freshServiceWake(Context c)throws Exception{
  ProbeService service=new ProbeService();
  java.lang.reflect.Method attach=android.content.ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(service,c);
  for(String name:new String[]{"active","sourceOverlay"}){java.lang.reflect.Field f=ProbeService.class.getDeclaredField(name);f.setAccessible(true);f.set(service,name.equals("active")?Boolean.TRUE:overlay);}
  java.lang.reflect.Field module=ProbeService.class.getDeclaredField("module");module.setAccessible(true);module.set(service,new android.os.Binder());
  java.lang.reflect.Field screens=ProbeService.class.getDeclaredField("audioScreens");screens.setAccessible(true);screens.set(service,new AudioSourceController(c));
  java.lang.reflect.Method wake=ProbeService.class.getDeclaredMethod("wakeUp");wake.setAccessible(true);wake.invoke(service);waitForIdleSync();return service;
 }
 @Override public void onStart(){Bundle result=new Bundle();int outcome=0;Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("overlay",0);boolean source=prefs.getBoolean("source",true),keep=prefs.getBoolean("sourceKeepWhenOff",true);boolean hadKeep=prefs.contains("sourceKeepWhenOff");
  SharedPreferences background=c.getSharedPreferences("background",0);java.util.Map<String,?> savedBackground=new java.util.HashMap<>(background.getAll());boolean hadWanted=background.contains("wanted"),hadPaused=background.contains("paused"),wanted=background.getBoolean("wanted",false),paused=background.getBoolean("paused",false);
  try{
   BackgroundRecovery.enabled(c,false);c.stopService(new android.content.Intent(c,ProbeService.class));SystemClock.sleep(300);
   SourceOverlay.enabled(c,true);SourceOverlay.keepVisibleWhenOff(c,true);state.newSession();state.running(true);state.event(1000,new int[]{6291908},null,null);state.status("test","live",true);frame(0x01,0);
   runOnMainSync(()->{overlay=new SourceOverlay(c);overlay.start();overlay.refreshOptions();});
   check(state.snapshot().sourceOverlayVisible&&state.snapshot().sourceOverlayText.contains("CD"),"off baseline visible");
   String offStatus=state.snapshot().sourceOverlayStatus,offText=state.snapshot().sourceOverlayText;
   SystemClock.sleep(3300);render();check(state.snapshot().sourceOverlayVisible&&state.snapshot().sourceOverlayStatus.equals(offStatus),"off stays beyond timeout");
   frame(0x01,0);render();check(state.snapshot().sourceOverlayText.equals(offText),"duplicate keeps off caption");
   SourceOverlay.keepVisibleWhenOff(c,false);render();check(!state.snapshot().sourceOverlayVisible,"disabling pin restores expired timeout");
   SourceOverlay.keepVisibleWhenOff(c,true);render();check(state.snapshot().sourceOverlayVisible,"enabling pin while already off restores overlay");
   frame(0x81,1);render();check(state.snapshot().sourceOverlayVisible&&!state.snapshot().sourceOverlayText.equals(offText),"on announcement replaces off");
   SystemClock.sleep(3300);frame(0x81,2);render();check(!state.snapshot().sourceOverlayVisible,"on timeout not restarted by volume");
   runOnMainSync(()->overlay.start());render();check(!state.snapshot().sourceOverlayVisible,"repeated service start does not retrigger announcement");
   BackgroundSettings.activate(c);long session=state.snapshot().sessionId;frame(0x81,2);freshServiceWake(c);
   check(state.snapshot().sourceOverlayVisible&&state.snapshot().sourceOverlayText.equals("CD"),"fresh service wake announces unchanged source with normal caption");
   check(state.snapshot().sessionId==session&&state.snapshot().connected,"fresh wake preserves live subscriptions");
   ProbeService pending=freshServiceWake(c);android.os.Handler timeoutHandler=new android.os.Handler(android.os.Looper.getMainLooper());
   java.lang.reflect.Field binding=ProbeService.class.getDeclaredField("binding");binding.setAccessible(true);binding.set(pending,new android.content.ServiceConnection(){public void onServiceConnected(android.content.ComponentName n,android.os.IBinder b){}public void onServiceDisconnected(android.content.ComponentName n){}});
   java.lang.reflect.Field module=ProbeService.class.getDeclaredField("module");module.setAccessible(true);module.set(pending,null);java.lang.reflect.Field worker=ProbeService.class.getDeclaredField("worker");worker.setAccessible(true);worker.set(pending,timeoutHandler);
   java.lang.reflect.Field lastWake=ProbeService.class.getDeclaredField("lastWakeAt");lastWake.setAccessible(true);lastWake.setLong(pending,-1);java.lang.reflect.Method wake=ProbeService.class.getDeclaredMethod("wakeUp");wake.setAccessible(true);wake.invoke(pending);
   java.lang.reflect.Field watchdog=ProbeService.class.getDeclaredField("watchdog");watchdog.setAccessible(true);Runnable timeout=(Runnable)watchdog.get(pending);boolean rearmed=timeoutHandler.hasCallbacks(timeout);timeoutHandler.removeCallbacks(timeout);check(rearmed,"wake re-arms cancelled timeout for an in-flight bind");waitForIdleSync();
   SystemClock.sleep(3300);render();check(!state.snapshot().sourceOverlayVisible,"wake announcement uses ordinary timeout");
   state.newSession();state.running(true);runOnMainSync(()->overlay.announceCurrentSource());check(!state.snapshot().sourceOverlayVisible,"wake waits for reconnect");
   state.status("test","connected without source",true);state.event(1000,new int[]{6291908},null,null);render();check(!state.snapshot().sourceOverlayVisible,"connected without source never shows placeholder");
   state.event(98,new int[]{2},null,null);render();check(state.snapshot().sourceOverlayVisible&&state.snapshot().sourceOverlayText.equals("CD"),"delayed source completes pending announcement");
   frame(0x01,0);render();check(state.snapshot().sourceOverlayVisible,"next off pins again");
   SourceOverlay.enabled(c,false);runOnMainSync(()->overlay.announceCurrentSource());render();check(!state.snapshot().sourceOverlayVisible,"master switch suppresses wake and hides pinned off");
   SourceOverlay.enabled(c,true);render();check(state.snapshot().sourceOverlayVisible,"master enable restores pinned off");
   state.status("test","disconnected",false);render();check(!state.snapshot().sourceOverlayVisible,"disconnection hides pinned off");
   runOnMainSync(()->{overlay.close();overlay.announceCurrentSource();});state.status("test","late data",true);frame(0x81,2);render();check(!state.snapshot().sourceOverlayVisible,"closed overlay ignores late wake and source data");
   result.putString("result","PASS: "+checks+" real source overlay checks");
  }catch(Throwable e){result.putString("failure",android.util.Log.getStackTraceString(e));outcome=1;}
  finally{if(overlay!=null)runOnMainSync(()->overlay.close());state.newSession();state.running(false);SourceOverlay.enabled(c,source);if(hadKeep)SourceOverlay.keepVisibleWhenOff(c,keep);else prefs.edit().remove("sourceKeepWhenOff").commit();SharedPreferences.Editor edit=background.edit().clear();for(java.util.Map.Entry<String,?> entry:savedBackground.entrySet()){Object value=entry.getValue();if(value instanceof Boolean)edit.putBoolean(entry.getKey(),(Boolean)value);else if(value instanceof Long)edit.putLong(entry.getKey(),(Long)value);else if(value instanceof String)edit.putString(entry.getKey(),(String)value);}edit.commit();((android.app.job.JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(BackgroundRecovery.JOB_ID);}
  finish(outcome,result);
 }
}
