package com.crewbeat.hondabox.tests;

import android.app.*;
import android.content.*;
import android.os.*;
import com.crewbeat.hondabox.*;
import java.util.*;

/** Real activity lifecycle and controller; launches captured locally, no vehicle writes. */
public final class AudioForegroundTests extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;
    private AudioSourceController screens;private Activity activity;private int checks;
    private final List<Intent> launches=new ArrayList<>();
    private void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private void source(int value){runOnMainSync(()->{state.event(98,new int[]{value},null,null);screens.refreshOptions();});}
    private void closeActivity(){if(activity!=null){runOnMainSync(()->activity.finish());waitForIdleSync();activity=null;}long end=SystemClock.uptimeMillis()+3000;while((ProbeApplication.foreground()||DebugActivity.isVisible()||AudioPlayerActivity.isVisible())&&SystemClock.uptimeMillis()<end)SystemClock.sleep(20);}
    private void refresh(){runOnMainSync(()->screens.refreshOptions());}
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Context c=getTargetContext();Bundle result=new Bundle();int outcome=Activity.RESULT_OK;
        SharedPreferences background=c.getSharedPreferences("background",0);Map<String,?> saved=new HashMap<>(background.getAll());boolean enabled=AudioSourceController.enabled(c);
        try{
            c.stopService(new Intent(c,ProbeService.class));SystemClock.sleep(300);state.newSession();state.running(true);state.status("test","local source data",true);state.event(1000,new int[]{6291908},null,null);state.event(98,new int[]{1},null,null);AudioSourceController.enabled(c,true);
            Context capture=new ContextWrapper(c){@Override public void startActivity(Intent i){launches.add(new Intent(i));}};
            runOnMainSync(()->{screens=new AudioSourceController(capture);screens.start();screens.refreshOptions();});
            for(Class<?> screen:new Class<?>[]{MainActivity.class,SettingsActivity.class,DebugActivity.class,CarInfoActivity.class}){
                activity=startActivitySync(new Intent(c,screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
                check(ProbeApplication.foreground(),screen.getSimpleName()+" is foreground");int before=launches.size();
                for(int code:new int[]{2,4,1})source(code);
                check(launches.size()==before,screen.getSimpleName()+" stays open through CD/USB/radio changes");
                source(2);closeActivity();check(!ProbeApplication.foreground(),"foreground lifecycle clears after "+screen.getSimpleName());
                refresh();check(launches.size()==before,"no queued CD launch after leaving "+screen.getSimpleName());
                source(4);check(launches.size()==before+1,"new background source change still launches player after "+screen.getSimpleName());
                Intent launch=launches.get(launches.size()-1);check(launch.getBooleanExtra("automatic",false)&&"USB".equals(launch.getStringExtra("mode")),"background launch keeps shared player intent");source(1);
            }
            activity=startActivitySync(AudioPlayerActivity.intent(c,AudioMode.CD,false));waitForIdleSync();check(AudioPlayerActivity.isVisible(),"manual player opening still works");int before=launches.size();source(4);check(launches.size()==before,"existing player is reused without another launch");
            result.putString("result","PASS: "+checks+" foreground navigation checks; local source events only");
        }catch(Throwable e){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(e));}
        finally{if(screens!=null)runOnMainSync(()->screens.close());closeActivity();c.stopService(new Intent(c,ProbeService.class));state.newSession();state.running(false);AudioSourceController.enabled(c,enabled);SharedPreferences.Editor edit=background.edit().clear();for(Map.Entry<String,?> e:saved.entrySet()){Object value=e.getValue();if(value instanceof Boolean)edit.putBoolean(e.getKey(),(Boolean)value);else if(value instanceof String)edit.putString(e.getKey(),(String)value);else if(value instanceof Integer)edit.putInt(e.getKey(),(Integer)value);else if(value instanceof Long)edit.putLong(e.getKey(),(Long)value);}edit.commit();}
        finish(outcome,result);
    }
}
