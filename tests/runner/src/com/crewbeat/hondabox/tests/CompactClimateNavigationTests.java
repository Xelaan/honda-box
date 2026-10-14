package com.crewbeat.hondabox.tests;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import com.crewbeat.hondabox.*;
import java.util.*;

/** Actual Android window placement, live toggling and fold/unfold; no vehicle commands. */
public final class CompactClimateNavigationTests extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;
    private Context context;private Activity activity;private ClimateOverlay popup;private ClimateStripOverlay compact;private int checks;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private Object field(Object object,String name)throws Exception{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private View window()throws Exception{return (View)field(compact,"view");}
    private Rect bounds()throws Exception{View view=window();int[] xy=new int[2];runOnMainSync(()->view.getLocationOnScreen(xy));return new Rect(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight());}
    private int screenHeight(){DisplayMetrics d=new DisplayMetrics();((WindowManager)context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRealMetrics(d);return d.heightPixels;}
    private void seed(){state.newSession();state.running(true);state.recording(false);state.status("test","Synthetic climate navigation geometry",true);int[] codes={1000,10,11,13,14,19,20,21,27,28},values={6291908,1,1,1,1,1,1,4,44,46};for(int i=0;i<codes.length;i++)state.event(codes[i],new int[]{values[i]},null,null);}
    private void close(){runOnMainSync(()->{if(compact!=null)compact.close();if(popup!=null)popup.close();if(activity!=null)activity.finish();});compact=null;popup=null;activity=null;SystemClock.sleep(150);}
    private void waitRender(){SystemClock.sleep(650);}
    private void toggle(boolean enabled){ClimateStripOverlay.extendToSystemNavigation(context,enabled);waitRender();}
    private void collapse()throws Exception{View v=(View)field(compact,"strip");runOnMainSync(v::performClick);waitRender();}
    private void restore(String name,Map<String,?> prefs){SharedPreferences.Editor edit=context.getSharedPreferences(name,0).edit().clear();for(Map.Entry<String,?> e:prefs.entrySet()){String k=e.getKey();Object v=e.getValue();if(v instanceof Boolean)edit.putBoolean(k,(Boolean)v);else if(v instanceof String)edit.putString(k,(String)v);else if(v instanceof Integer)edit.putInt(k,(Integer)v);else if(v instanceof Long)edit.putLong(k,(Long)v);else if(v instanceof Float)edit.putFloat(k,(Float)v);else if(v instanceof Set)edit.putStringSet(k,new HashSet<>((Set<String>)v));}edit.commit();}
    private void image(String theme)throws Exception{Bitmap b=getUiAutomation().takeScreenshot();try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(context.getExternalFilesDir(null),"compact-navigation-"+theme+"-TEST.png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}b.recycle();}
    @Override public void onStart(){context=getTargetContext();Bundle result=new Bundle();int outcome=Activity.RESULT_OK;Map<String,Map<String,?>> old=new LinkedHashMap<>();for(String n:new String[]{"theme","language","overlay","background","diagnostics"})old.put(n,context.getSharedPreferences(n,0).getAll());
        try{
            context.getSharedPreferences("background",0).edit().putBoolean("wanted",false).putBoolean("paused",true).commit();context.stopService(new Intent(context,ProbeService.class));SystemClock.sleep(400);
            context.getSharedPreferences("overlay",0).edit().remove("climateStripExtendNavigation").commit();check(!ClimateStripOverlay.extendToSystemNavigation(context),"option defaults off");
            for(String theme:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){
                ThemeSettings.mode(context,theme);I18n.language(context,"en");seed();ClimateOverlay.enabled(context,false);ClimateStripOverlay.enabled(context,true);ClimateStripOverlay.expandButton(context,true);ClimateStripOverlay.buttonAutoHide(context,false);ClimateStripOverlay.alwaysAutoHide(context,false);ClimateStripOverlay.extendToSystemNavigation(context,false);context.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",false).commit();
                activity=startActivitySync(new Intent(context,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));runOnMainSync(()->{popup=new ClimateOverlay(context);popup.start();compact=new ClimateStripOverlay(context,popup);compact.start();});waitRender();
                CheckBox option=activity.findViewById(android.R.id.content).findViewWithTag("debug-climate-navigation");check(option!=null&&!option.isChecked(),"Debug checkbox exists and matches preference");check(option.getText().toString().equals(I18n.t(R.string.debug_climate_extend_navigation)),"English title localized");
                View original=window();Rect normal=bounds();WindowManager.LayoutParams lp=(WindowManager.LayoutParams)original.getLayoutParams();int defaultFlags=lp.flags,defaultInsets=Build.VERSION.SDK_INT>=30?lp.getFitInsetsTypes():0;
                check(normal.bottom<screenHeight(),"normal open bar remains above navigation");runOnMainSync(option::performClick);waitRender();Rect extended=bounds();check(extended.bottom==screenHeight(),"extended open bar reaches physical bottom");check(extended.left==normal.left&&extended.width()==normal.width()&&extended.height()==normal.height(),"extension preserves horizontal centering and size");check(original==window()&&window().getAlpha()==1&&window().getVisibility()==View.VISIBLE,"live extension keeps mounted window visible");check((lp.flags&WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)!=0,"extended screen layout enabled");if(Build.VERSION.SDK_INT>=30){check((lp.getFitInsetsTypes()&WindowInsets.Type.navigationBars())==0,"navigation fitting removed");check((lp.getFitInsetsTypes()&WindowInsets.Type.statusBars())==(defaultInsets&WindowInsets.Type.statusBars()),"status-bar fitting retained");}
                runOnMainSync(option::performClick);waitRender();check(bounds().equals(normal),"turning off restores original frame");check(lp.flags==defaultFlags&&(Build.VERSION.SDK_INT<30||lp.getFitInsetsTypes()==defaultInsets),"turning off restores original flags and inset types");
                collapse();check(state.overlaySnapshot().climateStripCollapsed,"manual minimize folds the bar");Rect folded=bounds();check(folded.bottom<screenHeight(),"normal handle remains above navigation");toggle(true);check(bounds().bottom==screenHeight()&&bounds().width()==folded.width(),"extended folded handle reaches physical bottom without widening");collapse();check(!state.overlaySnapshot().climateStripCollapsed&&bounds().bottom==screenHeight(),"unfold keeps extended placement");
                runOnMainSync(()->{try{((View)field(compact,"strip")).performClick();}catch(Exception e){throw new RuntimeException(e);}});ClimateStripOverlay.extendToSystemNavigation(context,false);waitRender();check(state.overlaySnapshot().climateStripCollapsed&&window().getVisibility()==View.VISIBLE&&window().getAlpha()==1,"toggle during folding does not lose handle");check(bounds().bottom<screenHeight(),"toggle during folding restores normal anchor");
                toggle(true);runOnMainSync(()->compact.close());runOnMainSync(()->{compact=new ClimateStripOverlay(context,popup);compact.start();});waitRender();check(bounds().bottom==screenHeight()&&state.overlaySnapshot().climateStripCollapsed,"reopening retains option and folded state");toggle(false);collapse();image(theme);
                close();I18n.language(context,"ru");seed();activity=startActivitySync(new Intent(context,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitRender();CheckBox ru=activity.findViewById(android.R.id.content).findViewWithTag("debug-climate-navigation");check(ru!=null&&ru.getText().toString().equals(I18n.t(R.string.debug_climate_extend_navigation)),"Russian title localized");close();
                seed();activity=startActivitySync(new Intent(context,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitRender();check(activity.findViewById(android.R.id.content).findViewWithTag("debug-climate-navigation")==null,"experimental setting is Debug-only");close();
            }
            check(!state.snapshot().commandBusy,"no vehicle commands sent");result.putString("result","PASS: "+checks+" actual compact climate/navigation checks");
        }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}finally{close();state.newSession();state.running(false);for(Map.Entry<String,Map<String,?>> e:old.entrySet())restore(e.getKey(),e.getValue());}
        finish(outcome,result);
    }
}
