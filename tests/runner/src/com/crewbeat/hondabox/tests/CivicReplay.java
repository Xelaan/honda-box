package com.crewbeat.hondabox.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.PixelFormat;import com.crewbeat.hondabox.*;import org.json.*;
/** Emulator-only, timestamped replay of numeric events; no FYT service or hardware commands. */
public final class CivicReplay extends Instrumentation {
 private boolean verifyCarInfo;private volatile boolean paused,restart;private ClimateOverlay popup;private ClimateStripOverlay strip;private LinearLayout controls;private TextView progress;private WindowManager wm;private Activity screen;private final ProbeState state=ProbeState.INSTANCE;private int checks;
 @Override public void onCreate(Bundle b){super.onCreate(b);verifyCarInfo=b!=null&&"true".equals(b.getString("verifyCarInfo"));start();}
 private void main(Runnable r){runOnMainSync(r);}
 private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 private void value(int code,int n){state.event(code,new int[]{n},null,null);}
 private void reset(){state.newSession();state.running(true);state.status("Replay","Recorded Civic climate; no live car connection",true);value(1000,15729093);value(28,-1);value(27,-1);value(11,0);value(13,0);value(21,0);value(18,0);value(19,0);value(20,0);}
 private void setup(Context c)throws Exception{
  BackgroundSettings.stop(c);SystemClock.sleep(300);I18n.init(c);I18n.language(c,"ru");ThemeSettings.mode(c,ThemeSettings.DAY);reset();
  if(verifyCarInfo){
  screen=startActivitySync(new Intent(c,CarInfoActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));SystemClock.sleep(600);
  check(CarInfoActivity.civicDistance(state.snapshot())==null,"missing distance unknown");value(98,0);check(CarInfoActivity.civicDistance(state.snapshot())==null,"startup zero not invented mileage");
  value(98,123456);SystemClock.sleep(600);check(CarInfoActivity.civicDistance(state.snapshot())==123456,"distance98 decoded");
  main(()->{TextView v=screen.findViewById(android.R.id.content).findViewWithTag("civic-distance-value");check(v!=null&&v.getText().toString().replaceAll("[^0-9]","").equals("123456"),"CarInfo visible distance updates");});
  value(98,123457);SystemClock.sleep(600);main(()->{TextView v=screen.findViewById(android.R.id.content).findViewWithTag("civic-distance-value");check(v.getText().toString().replaceAll("[^0-9]","").equals("123457"),"CarInfo refreshes distance");});
  value(1000,6291908);check(CarInfoActivity.civicDistance(state.snapshot())==null,"Accord source98 is not distance");reset();main(()->screen.finish());
  }
  ClimateOverlay.enabled(c,true);ClimateStripOverlay.enabled(c,true);ClimateStripOverlay.expandButton(c,true);ClimateStripOverlay.alwaysAutoHide(c,false);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();
  main(()->{popup=new ClimateOverlay(c);strip=new ClimateStripOverlay(c,popup);popup.start();strip.start();wm=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);controls=new LinearLayout(c);controls.setPadding(12,4,12,4);controls.setGravity(Gravity.CENTER_VERTICAL);controls.setBackgroundColor(0xE62B3544);progress=new TextView(c);progress.setTextColor(0xFFFFFFFF);progress.setTextSize(14);controls.addView(progress);
   Button pause=new Button(c);pause.setText("Пауза");pause.setOnClickListener(v->{paused=!paused;pause.setText(paused?"Продолжить":"Пауза");});controls.addView(pause);
   Button again=new Button(c);again.setText("Сначала");again.setOnClickListener(v->restart=true);controls.addView(again);
   Button theme=new Button(c);theme.setText("День / ночь");theme.setOnClickListener(v->{ThemeSettings.mode(c,ThemeSettings.resolve(c).palette.night?ThemeSettings.DAY:ThemeSettings.NIGHT);popup.refreshOptions();strip.refreshOptions();});controls.addView(theme);
   WindowManager.LayoutParams lp=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;lp.y=4;wm.addView(controls,lp);
  });
 }
 @Override public void onStart(){Bundle result=new Bundle();if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){result.putString("failure","Emulator only");finish(1,result);return;}int code=0;
  try{Context c=getTargetContext();setup(c);java.io.InputStream in=getContext().getAssets().open("civic-climate-realtime.json");java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);in.close();JSONObject data=new JSONObject(out.toString("UTF-8"));JSONArray events=data.getJSONArray("events");long duration=data.getLong("durationMs");
   Bundle ready=new Bundle();ready.putString("replay","READY: 1280x800, 1x timing, repeated; CarInfo checks="+checks);sendStatus(0,ready);
   for(int cycle=0;cycle<20;cycle++){reset();int index=0;long elapsed=0,last=SystemClock.uptimeMillis(),draw=0;restart=false;
    while(elapsed<duration+3000&&!restart){long now=SystemClock.uptimeMillis();if(!paused)elapsed+=now-last;last=now;
     while(index<events.length()&&events.getJSONObject(index).getLong("at")<=elapsed){JSONObject e=events.getJSONObject(index++);JSONArray a=e.getJSONArray("ints");int[] ints=new int[a.length()];for(int j=0;j<ints.length;j++)ints[j]=a.getInt(j);state.event(e.getInt("code"),ints,null,null);}
     if(now-draw>200){draw=now;String label=String.format(java.util.Locale.US,"ЛОГ CIVIC · 1× · %02d:%02d / %02d:%02d  ",elapsed/60000,elapsed/1000%60,duration/60000,duration/1000%60);main(()->progress.setText(label));}SystemClock.sleep(20);
    }
   }
   result.putString("result","Replay finished; "+checks+" CarInfo checks passed");
  }catch(Throwable e){code=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{main(()->{if(strip!=null)strip.close();if(popup!=null)popup.close();if(controls!=null&&wm!=null)wm.removeView(controls);});state.running(false);state.status("Replay finished","",false);}
  finish(code,result);
 }
}
