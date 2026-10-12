package com.crewbeat.hondabox.tests;
import android.app.*;import android.content.*;import android.os.*;import android.view.*;import android.widget.*;
import java.util.*;import java.util.concurrent.*;import com.crewbeat.hondabox.*;
/** Real emulator overlay windows and click listeners; no FYT fixture or command transport. */
public final class ClimateSideButtonsTests extends Instrumentation {
 private ClimateStripOverlay panel;private ClimateOverlay popup;private int checks;private final ProbeState state=ProbeState.INSTANCE;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private Object field(String name)throws Exception{java.lang.reflect.Field f=ClimateStripOverlay.class.getDeclaredField(name);f.setAccessible(true);return f.get(panel);}
 private void set(String name,Object value)throws Exception{java.lang.reflect.Field f=ClimateStripOverlay.class.getDeclaredField(name);f.setAccessible(true);f.set(panel,value);}
 private void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 private void await(String label,Callable<Boolean> test)throws Exception{long end=SystemClock.uptimeMillis()+4000;while(SystemClock.uptimeMillis()<end){if(test.call())return;SystemClock.sleep(25);}throw new AssertionError(label);}
 private boolean settled()throws Exception{return field("pendingLayout")==null&&field("animator")==null&&field("buttonAnimator")==null;}
 private boolean shown()throws Exception{return ((ImageButton)field("expandButton")).isEnabled()&&((ImageButton)field("minimizeButton")).isEnabled();}
 private void tap(String name)throws Exception{View target=(View)field(name);runOnMainSync(()->target.performClick());}
 private void saveImage(Context c,String name)throws Exception{waitForIdleSync();SystemClock.sleep(250);android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(c.getExternalFilesDir(null),name))){bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();}
 private void configure(Context c,boolean autoHide){ClimateStripOverlay.enabled(c,true);ClimateStripOverlay.expandButton(c,true);ClimateStripOverlay.buttonAutoHide(c,autoHide);ClimateStripOverlay.alwaysAutoHide(c,false);ClimateOverlay.enabled(c,false);c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();}
 private void start(Context c){runOnMainSync(()->{popup=new ClimateOverlay(c);popup.start();panel=new ClimateStripOverlay(c,popup);panel.start();panel.refreshOptions();});}
 private void stop(){runOnMainSync(()->{panel.close();popup.close();});}
 @Override public void onStart(){Bundle result=new Bundle();int outcome=Activity.RESULT_OK;Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("overlay",0);Map<String,?> old=prefs.getAll();String language=I18n.language(c),theme=ThemeSettings.mode(c);
  try{
   c.stopService(new Intent(c,ProbeService.class));SystemClock.sleep(300);I18n.language(c,"en");
   state.newSession();state.running(true);state.event(1000,new int[]{6291908},null,null);state.status("test","local climate values",true);
   int[] codes={21,27,28,11,19},values={4,44,46,1,1};for(int i=0;i<codes.length;i++)state.event(codes[i],new int[]{values[i]},null,null);
   for(String mode:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){
    ThemeSettings.mode(c,mode);configure(c,true);start(c);await("initial open bar settles",()->settled()&&state.overlaySnapshot().climateStripVisible);
    check(!shown()&&!state.overlaySnapshot().climateStripCollapsed,"auto-hide buttons start hidden, bar open");
    ClimateStripView strip=(ClimateStripView)field("strip");check(strip.getContentDescription().toString().startsWith("Show climate side buttons"),"hidden-button accessibility action describes reveal");
    FrameLayout root=(FrameLayout)field("view");WindowManager.LayoutParams lp=(WindowManager.LayoutParams)field("layout");int width=lp.width,height=lp.height;
    tap("strip");await("first bar tap reveals both",()->settled()&&shown());check(!state.overlaySnapshot().climateStripCollapsed,"first tap does not collapse bar");
    check(lp.width==width&&lp.height==height&&lp.x==0,"revealing buttons leaves window geometry fixed");
    ImageButton left=(ImageButton)field("expandButton"),right=(ImageButton)field("minimizeButton");
    check(left.getParent()==root&&right.getParent()==root&&strip.getParent()==root,"both buttons are siblings of the bar in one window");
    check(left.getRight()<=strip.getLeft()&&right.getLeft()>=strip.getRight(),"buttons sit outside the card on opposite sides");
    check(Math.abs((left.getLeft()+left.getRight()+right.getLeft()+right.getRight())/4f-root.getWidth()/2f)<1,"button positions are symmetric");
    check(left.getAlpha()==right.getAlpha()&&left.getScaleX()==right.getScaleX(),"buttons share reveal progress");
    check(left.getBackground()!=right.getBackground()&&left.getColorFilter().equals(right.getColorFilter()),"both buttons have themed independent backgrounds and matching accent");
    saveImage(c,"mini-climate-two-buttons-"+mode+"-TEST.png");
    tap("strip");await("second bar tap collapses",()->settled()&&state.overlaySnapshot().climateStripCollapsed);check(lp.width==Math.round(148*c.getResources().getDisplayMetrics().density)&&!shown(),"fold leaves only the bottom handle");
    tap("strip");await("handle tap opens bar and buttons",()->settled()&&!state.overlaySnapshot().climateStripCollapsed&&shown());
    tap("minimizeButton");await("right button minimizes bar",()->settled()&&state.overlaySnapshot().climateStripCollapsed);
    check(c.getSharedPreferences("overlay",0).getBoolean("climateStripCollapsed",false),"minimize persists fold state");
    tap("strip");await("handle restores after minimize",()->settled()&&shown());
    runOnMainSync(()->{try{set("buttonHideAt",SystemClock.uptimeMillis()-1);}catch(Exception e){throw new RuntimeException(e);}panel.refreshOptions();});
    await("both buttons fade while bar stays open",()->settled()&&!shown());check(!state.overlaySnapshot().climateStripCollapsed&&state.overlaySnapshot().climateStripVisible,"button timeout never hides bar");
    check(left.getVisibility()==View.INVISIBLE&&right.getVisibility()==View.INVISIBLE&&lp.width==width,"hidden buttons preserve gutters and center");
    check(left.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO&&right.getImportantForAccessibility()==View.IMPORTANT_FOR_ACCESSIBILITY_NO,"hidden buttons are not accessible controls");
    tap("strip");await("tap hidden controls reappears",()->settled()&&shown());check(!state.overlaySnapshot().climateStripCollapsed,"reveal tap again keeps open bar");
    tap("expandButton");await("left button opens full popup",()->popup.isWindowPresent()&&!state.overlaySnapshot().climateStripVisible);
    check(((View)field("view")).getVisibility()==View.INVISIBLE,"full popup hides the whole compact window");
    stop();
   }
   ThemeSettings.mode(c,ThemeSettings.DAY);configure(c,false);start(c);await("persistent controls visible",()->settled()&&shown());
   check(!ClimateStripOverlay.buttonAutoHide(c),"existing side-button auto-hide preference preserved");
   ClimateStripOverlay.expandButton(c,false);runOnMainSync(()->panel.refreshOptions());await("turning side-buttons off hides both",()->settled()&&!shown());
   tap("strip");await("buttons-disabled bar retains collapse behavior",()->settled()&&state.overlaySnapshot().climateStripCollapsed);
   tap("strip");await("buttons-disabled handle retains expansion",()->settled()&&!state.overlaySnapshot().climateStripCollapsed);check(!shown(),"disabled setting shows neither button");
   state.event(21,new int[]{0},null,null);await("climate off removes window",()->!state.overlaySnapshot().climateStripVisible);
   check(field("view")==null,"climate off removes buttons with bar");stop();
   result.putString("result","PASS: "+checks+" actual compact climate side-button checks");
  }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{if(panel!=null&&popup!=null)stop();state.newSession();state.running(false);SharedPreferences.Editor edit=prefs.edit().clear();for(Map.Entry<String,?> e:old.entrySet()){Object v=e.getValue();if(v instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)v);else if(v instanceof Integer)edit.putInt(e.getKey(),(Integer)v);else if(v instanceof String)edit.putString(e.getKey(),(String)v);else if(v instanceof Long)edit.putLong(e.getKey(),(Long)v);else if(v instanceof Float)edit.putFloat(e.getKey(),(Float)v);}edit.commit();I18n.language(c,language);ThemeSettings.mode(c,theme);}
  finish(outcome,result);
 }
}
