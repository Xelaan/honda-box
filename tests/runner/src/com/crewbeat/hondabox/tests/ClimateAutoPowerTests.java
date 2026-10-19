package com.crewbeat.hondabox.tests;

import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.provider.Settings;
import com.crewbeat.hondabox.*;import java.util.*;import java.util.function.BooleanSupplier;

/** AUTO without a fan counter, original stuck-power OFF, and actual compact windows. No CAN writes. */
public final class ClimateAutoPowerTests extends Instrumentation {
 private int checks;private ClimateOverlay popup;private ClimateStripOverlay strip;private final ProbeState state=ProbeState.INSTANCE;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private void check(boolean ok,String why){if(!ok)throw new AssertionError(why);checks++;}
 private void main(Runnable task){Throwable[] error={null};runOnMainSync(()->{try{task.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError("UI action",error[0]);}
 private void await(BooleanSupplier condition,String why){long end=SystemClock.uptimeMillis()+3500;while(!condition.getAsBoolean()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);check(condition.getAsBoolean(),why);}
 private void value(ProbeState model,int code,int value){model.event(code,new int[]{value},null,null);}
 private ProbeState model(int profile,boolean override,int power,Integer auto,int fan,int left,int right){ProbeState model=new ProbeState();model.processAsHondaAccord(override);model.running(true);model.status("test","synthetic climate",true);value(model,1000,profile);apply(model,power,auto,fan,left,right);return model;}
 private void apply(ProbeState model,int power,Integer auto,int fan,int left,int right){value(model,10,power);if(auto!=null)value(model,13,auto);value(model,21,fan);value(model,27,left);value(model,28,right);}
 private Object field(Object object,String name){try{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new RuntimeException(e);}}
 private void models(){
  for(int profile:new int[]{6291908,3604932,123456})for(boolean override:new boolean[]{false,true}){
   boolean accord=profile==6291908||override;
   ProbeState model=model(profile,override,1,1,0,32,42);ProbeState.Snapshot s=model.snapshot();
   check(s.climateFanAutomatic()==accord,"AUTO fan detection is profile gated");check(s.climatePower()==1,"powered AUTO is ON");check(s.number(21)==0&&s.number(10)==1&&s.number(13)==1,"raw AUTO data untouched");check(s.fanLabel().equals(accord?"AUTO":"0"),"AUTO display without fabricated fan level");check(s.fanSegments()==0,"AUTO does not invent numeric segments");
   for(int fan:new int[]{1,3,7}){value(model,21,fan);s=model.snapshot();check(!s.climateFanAutomatic()&&s.fanLabel().equals(String.valueOf(fan)),"AUTO with reported speed retains actual number");}
   if(!accord)continue;
   for(Integer auto:new Integer[]{null,0}){s=model(profile,override,1,auto,0,32,42).snapshot();check(s.climatePower()==0&&!s.climateFanAutomatic(),"no AUTO inference without flag");}
   for(int temperature:new int[]{0,-1}){s=model(profile,override,1,1,0,temperature,temperature).snapshot();check(s.climatePower()==0&&!s.climateFanAutomatic(),"stale AUTO/ON with unset temperatures still OFF");}
   for(int temperature:new int[]{20,32,-2,-3}){s=model(profile,override,1,1,0,temperature,0).snapshot();check(s.climatePower()==1&&s.climateFanAutomatic(),"one valid set temperature confirms AUTO");}
   s=model(profile,override,0,1,0,32,42).snapshot();check(s.climatePower()==0&&!s.climateFanAutomatic(),"explicit OFF cannot be promoted solely by AUTO");
   s=model(profile,override,1,0,0,0,0).snapshot();check(s.climatePower()==0&&!ClimateStripOverlay.active(s),"original car stuck ON remains OFF");
   model=model(profile,override,1,1,0,32,42);model.status("offline","",false);check(!model.snapshot().climateFanAutomatic()&&model.snapshot().fanLabel().equals("—"),"no stale AUTO display disconnected");
  }
 }
 private void restore(SharedPreferences prefs,Map<String,?> old){SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> item:old.entrySet()){Object v=item.getValue();if(v instanceof Boolean)e.putBoolean(item.getKey(),(Boolean)v);else if(v instanceof String)e.putString(item.getKey(),(String)v);else if(v instanceof Long)e.putLong(item.getKey(),(Long)v);else if(v instanceof Integer)e.putInt(item.getKey(),(Integer)v);else if(v instanceof Float)e.putFloat(item.getKey(),(Float)v);}e.commit();}
 private void windows(Context c)throws Exception{
  check(Settings.canDrawOverlays(c),"emulator overlay permission");
  ClimateStripOverlay.enabled(c,true);ClimateStripOverlay.expandButton(c,false);ClimateStripOverlay.alwaysAutoHide(c,false);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();
  for(String theme:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){
   main(()->ThemeSettings.mode(c,theme));state.newSession();state.running(true);state.status("test","synthetic AUTO replay",true);value(state,1000,6291908);apply(state,0,0,0,0,0);
   main(()->{popup=new ClimateOverlay(c);strip=new ClimateStripOverlay(c,popup);strip.start();strip.refreshOptions();});check(!state.overlaySnapshot().climateStripVisible,"actual OFF window hidden");
   // Second-car powered AUTO, then manual fan3, then AUTO again, then true OFF.
   apply(state,1,1,0,20,20);main(()->strip.refreshOptions());await(()->state.overlaySnapshot().climateStripVisible,"AUTO creates actual compact window");
   await(()->{View view=(View)field(strip,"view");return view!=null&&view.getWidth()>1&&view.getAlpha()==1&&view.getVisibility()==View.VISIBLE;},"AUTO compact window measured and visible");
   Object original=field(strip,"view");check(state.overlaySnapshot().climateStripText.contains("fan=AUTO"),"actual compact text says AUTO");
   apply(state,1,0,3,32,42);main(()->strip.refreshOptions());check(field(strip,"view")==original&&state.overlaySnapshot().climateStripVisible,"manual speed keeps same window");check(state.overlaySnapshot().climateStripText.contains("fan=3"),"manual numeric fan shown");
   apply(state,1,1,0,32,42);main(()->strip.refreshOptions());SystemClock.sleep(500);check(field(strip,"view")==original&&state.overlaySnapshot().climateStripVisible,"manual to AUTO does not remove window");check(state.overlaySnapshot().climateStripText.contains("fan=AUTO"),"manual to AUTO updates fan label");
   ClimatePopupView[] large={null};main(()->{large[0]=new ClimatePopupView(c);large[0].content(state.snapshot(),false);});java.lang.reflect.Method flag=ClimatePopupView.class.getDeclaredMethod("flag",int.class);flag.setAccessible(true);check(I18n.t(R.string.ui_on).equals(flag.invoke(large[0],10)),"full panel power agrees with compact AUTO");check(large[0].getContentDescription().toString().contains("AUTO"),"full panel AUTO fan accessibility");
   apply(state,0,0,0,0,0);main(()->strip.refreshOptions());check(!state.overlaySnapshot().climateStripVisible&&field(strip,"view")==null,"real OFF removes compact window");
   apply(state,1,0,0,0,0);main(()->strip.refreshOptions());check(!state.overlaySnapshot().climateStripVisible&&field(strip,"view")==null,"original stuck-power OFF stays hidden");
   main(()->{strip.close();popup.close();});strip=null;popup=null;
  }
 }
 @Override public void onStart(){Bundle out=new Bundle();int code=0;Context c=getTargetContext();if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){out.putString("failure","Emulator only");finish(1,out);return;}
  SharedPreferences overlay=c.getSharedPreferences("overlay",0),bg=c.getSharedPreferences("background",0),theme=c.getSharedPreferences("theme",0);Map<String,?> a=new HashMap<>(overlay.getAll()),b=new HashMap<>(bg.getAll()),t=new HashMap<>(theme.getAll());
  try{BackgroundSettings.stop(c);SystemClock.sleep(350);models();windows(c);out.putString("result","PASS: "+checks+" AUTO/stuck-power and actual compact/full panel checks in both themes; zero CAN writes");}
  catch(Throwable e){code=1;out.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{main(()->{if(strip!=null)strip.close();if(popup!=null)popup.close();});state.running(false);state.newSession();restore(overlay,a);restore(bg,b);restore(theme,t);}
  finish(code,out);
 }
}
