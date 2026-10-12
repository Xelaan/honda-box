package com.crewbeat.hondabox.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import java.util.concurrent.Callable;import com.crewbeat.hondabox.*;
/** Real gear hit-testing and settings Activity launch on the local emulator only. */
public final class ClimateSettingsButtonTests extends Instrumentation {
 private ClimateOverlay popup;private Activity settings;private int checks;private final ProbeState state=ProbeState.INSTANCE;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private Object field(String name)throws Exception{java.lang.reflect.Field f=ClimateOverlay.class.getDeclaredField(name);f.setAccessible(true);return f.get(popup);}
 private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private void await(String label,Callable<Boolean> test)throws Exception{long end=SystemClock.uptimeMillis()+4000;while(SystemClock.uptimeMillis()<end){if(test.call())return;SystemClock.sleep(25);}throw new AssertionError(label);}
 private void touch(View target)throws Exception{int[] p=new int[2];runOnMainSync(()->target.getLocationOnScreen(p));float x=p[0]+target.getWidth()/2f,y=p[1]+target.getHeight()/2f;long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,x,y,0);down.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(down,true),"physical down reaches overlay");down.recycle();MotionEvent up=MotionEvent.obtain(now,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(up,true),"physical up reaches overlay");up.recycle();}
 private void image(Context c,String name)throws Exception{waitForIdleSync();SystemClock.sleep(250);android.graphics.Bitmap b=getUiAutomation().takeScreenshot();try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),name))){b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
 @Override public void onStart(){Bundle result=new Bundle();int outcome=Activity.RESULT_OK;Context c=getTargetContext();boolean enabled=ClimateOverlay.enabled(c);String language=I18n.language(c),theme=ThemeSettings.mode(c);
  try{
   c.stopService(new Intent(c,ProbeService.class));SystemClock.sleep(300);ClimateOverlay.enabled(c,false);state.newSession();state.running(true);state.status("test","local climate",true);
   int[] codes={1000,21,27,28,11,13,18,19,20},values={6291908,4,44,46,1,1,0,1,1};for(int i=0;i<codes.length;i++)state.event(codes[i],new int[]{values[i]},null,null);
   for(String mode:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){
    I18n.language(c,"en");ThemeSettings.mode(c,mode);runOnMainSync(()->{popup=new ClimateOverlay(c);popup.start();popup.showFromStrip(false);});
    await("full panel finishes slide",()->popup.isWindowPresent()&&Math.abs(((View)field("windowView")).getTranslationY())<.01f);
    ImageButton gear=(ImageButton)field("settingsButton");FrameLayout root=(FrameLayout)field("windowView");View canvas=(View)field("view");float density=c.getResources().getDisplayMetrics().density;
    check(gear.isEnabled()&&gear.getParent()==root&&canvas.getParent()==root,"gear is an accessible sibling inside the full panel");
    check(gear.getContentDescription().toString().equals("Open Honda Box settings"),"English gear label");
    check(Math.abs(root.getWidth()-gear.getRight()-14*density)<=1&&Math.abs(gear.getTop()-18*density)<=1,"gear sits at top right with margin");
    check(gear.getWidth()==Math.round(48*density)&&gear.getHeight()==Math.round(48*density),"48dp touch target");
    check(canvas.getTranslationY()==0&&root.getTranslationY()==0,"panel and gear share the completed slide");
    check(gear.getDrawable()!=null&&gear.getColorFilter()!=null&&gear.getColorFilter().equals(new android.graphics.PorterDuffColorFilter(ThemeSettings.resolve(c).palette.muted,android.graphics.PorterDuff.Mode.SRC_ATOP))&&gear.getBackground()==null,"settings uses the inactive power tint without a button surface");
    image(c,"climate-full-settings-"+mode+"-TEST.png");
    I18n.language(c,"ru");runOnMainSync(()->popup.refreshOptions());check(gear.getContentDescription().toString().equals("Открыть настройки Honda Box"),"Russian gear label updates live");
    ActivityMonitor monitor=addMonitor(SettingsActivity.class.getName(),null,false);touch(gear);settings=monitor.waitForActivityWithTimeout(4000);removeMonitor(monitor);
    check(settings instanceof SettingsActivity,"gear opens Honda Box preferences");
    await("gear dismisses popup",()->!popup.isWindowPresent()&&!state.overlaySnapshot().climateOverlayVisible);
    check(field("settingsButton")==null,"dismissal removes gear with its panel");
    runOnMainSync(()->settings.finish());waitForIdleSync();settings=null;runOnMainSync(()->popup.close());
   }
   runOnMainSync(()->{popup=new ClimateOverlay(c);popup.start();popup.showFromStrip(false);});await("ordinary tap panel ready",()->popup.isWindowPresent()&&Math.abs(((View)field("windowView")).getTranslationY())<.01f);
   touch((View)field("view"));await("ordinary panel tap still dismisses",()->!popup.isWindowPresent());check(field("settingsButton")==null,"ordinary dismissal also removes gear");
   check(!state.snapshot().commandBusy,"UI navigation starts no CAN command");result.putString("result","PASS: "+checks+" full climate settings button checks");
  }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{if(settings!=null)runOnMainSync(()->settings.finish());if(popup!=null)runOnMainSync(()->popup.close());state.newSession();state.running(false);ClimateOverlay.enabled(c,enabled);I18n.language(c,language);ThemeSettings.mode(c,theme);}
  finish(outcome,result);
 }
}
