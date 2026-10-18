package com.crewbeat.hondabox.tests;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.crewbeat.hondabox.*;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Emulator-only Android media-key delivery, muted playback and settings; no vehicle commands. */
public final class AuxPlaybackIntegrationTests extends Instrumentation {
    private final ProbeState state=ProbeState.INSTANCE;private Context context;private AuxPlaybackController controller;
    private MediaSession media;private AudioTrack audio;private Activity activity;private int checks;private volatile int pauses,plays;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    private void check(boolean ok,String text){if(!ok)throw new AssertionError(text);checks++;}
    private void main(Runnable task){Throwable[] error={null};runOnMainSync(()->{try{task.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError("UI action",error[0]);}
    private void await(BooleanSupplier test,String text){long end=SystemClock.uptimeMillis()+4000;while(!test.getAsBoolean()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);check(test.getAsBoolean(),text);}
    private void refresh(){main(()->{try{java.lang.reflect.Method method=AuxPlaybackController.class.getDeclaredMethod("refresh");method.setAccessible(true);method.invoke(controller);}catch(Exception e){throw new RuntimeException(e);}});}
    private boolean playing(){return ((AudioManager)context.getSystemService(Context.AUDIO_SERVICE)).isMusicActive();}
    private void playback(boolean play){main(()->{if(play)audio.play();else audio.pause();media.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).setState(play?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,0,1).build());});await(()->playing()==play,"Android playback state "+play);}
    private void source(int source){int b=0x80|source-1;state.event(1019,new int[]{46,3,2,b,7,(~(3+2+b+7))&255},null,null);refresh();}
    private void baseline(boolean play){main(()->controller.reset());source(3);playback(play);pauses=plays=0;refresh();}
    private void tap(float x,float y){long now=SystemClock.uptimeMillis();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"settings option tap delivered");event.recycle();}}
    private void restore(SharedPreferences p,Map<String,?> values){SharedPreferences.Editor e=p.edit().clear();for(Map.Entry<String,?> item:values.entrySet()){String k=item.getKey();Object v=item.getValue();if(v instanceof Boolean)e.putBoolean(k,(Boolean)v);else if(v instanceof String)e.putString(k,(String)v);else if(v instanceof Long)e.putLong(k,(Long)v);else if(v instanceof Integer)e.putInt(k,(Integer)v);}e.commit();}
    @Override public void onStart(){context=getTargetContext();Bundle result=new Bundle();int outcome=0;Map<String,?> old=null,oldBg=null;SharedPreferences prefs=context.getSharedPreferences("overlay",0),bg=context.getSharedPreferences("background",0);String lang=I18n.language(context),theme=ThemeSettings.mode(context);
        try{
            check(Build.HARDWARE.contains("ranchu")||Build.HARDWARE.contains("goldfish"),"disposable emulator required");old=new HashMap<>(prefs.getAll());oldBg=new HashMap<>(bg.getAll());
            main(()->BackgroundSettings.stop(context));SystemClock.sleep(300);prefs.edit().remove("auxPauseAndroid").remove("auxReturnPlayback").commit();
            check(AuxPlaybackSettings.pauseOnLeave(context),"pause defaults on");check(AuxPlaybackSettings.onReturn(context)==AuxPlaybackPolicy.ReturnAction.IF_AUTO_PAUSED,"return defaults to resume own pause");
            for(String language:new String[]{"en","ru"})for(String appearance:new String[]{ThemeSettings.DAY,ThemeSettings.NIGHT}){
                I18n.language(context,language);ThemeSettings.mode(context,appearance);activity=startActivitySync(new Intent(context,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
                CheckBox pause=activity.findViewById(android.R.id.content).findViewWithTag("aux-pause-android");Button returning=activity.findViewById(android.R.id.content).findViewWithTag("aux-return-android");
                check(pause!=null&&returning!=null,"both AUX controls present "+language+appearance);check(returning.getWidth()<((View)returning.getParent()).getWidth(),"return setting button is compact");
                main(()->pause.performClick());check(!AuxPlaybackSettings.pauseOnLeave(context),"pause toggle persists off");main(()->pause.performClick());check(AuxPlaybackSettings.pauseOnLeave(context),"pause toggle persists on");
                for(int choice=0;choice<3;choice++){
                    final int index=choice;main(()->returning.performClick());waitForIdleSync();
                    int[] labels={R.string.aux_return_always,R.string.aux_return_if_paused,R.string.aux_return_nothing};await(()->getUiAutomation().getRootInActiveWindow()!=null,"return options dialog visible");android.view.accessibility.AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
                    for(int id:labels)check(!root.findAccessibilityNodeInfosByText(I18n.t(id)).isEmpty(),"return choice shown");
                    android.view.accessibility.AccessibilityNodeInfo option=root.findAccessibilityNodeInfosByText(I18n.t(labels[index])).get(0);android.graphics.Rect bounds=new android.graphics.Rect();option.getBoundsInScreen(bounds);check(!bounds.isEmpty(),"return choice has visible bounds");tap(bounds.centerX(),bounds.centerY());waitForIdleSync();await(()->AuxPlaybackSettings.onReturn(context).ordinal()==index,"return choice persists "+choice);
                }
                main(()->activity.finish());waitForIdleSync();activity=null;AuxPlaybackSettings.onReturn(context,AuxPlaybackPolicy.ReturnAction.IF_AUTO_PAUSED);
            }
            main(()->{
                audio=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(8000).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setTransferMode(AudioTrack.MODE_STATIC).setBufferSizeInBytes(16000).build();audio.setVolume(0);audio.write(new short[8000],0,8000);audio.setLoopPoints(0,8000,-1);
                media=new MediaSession(context,"Honda AUX integration test");media.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS|MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);media.setCallback(new MediaSession.Callback(){@Override public void onPause(){pauses++;audio.pause();media.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).setState(PlaybackState.STATE_PAUSED,0,1).build());}@Override public void onPlay(){plays++;audio.play();media.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE).setState(PlaybackState.STATE_PLAYING,0,1).build());}},new Handler(Looper.getMainLooper()));media.setActive(true);
            });
            state.newSession();state.running(true);state.status("test","synthetic AUX transition",true);state.event(1000,new int[]{6291908},null,null);state.auxSource(3);BackgroundRecovery.notePower(context,true);AudioSourceController.enabled(context,false);state.event(98,new int[]{3},null,null);
            main(()->{controller=new AuxPlaybackController(context);controller.start();});SystemClock.sleep(250);
            baseline(true);source(1);await(()->pauses==1&&!playing(),"Android PAUSE delivered on AUX to radio");refresh();source(2);source(4);check(pauses==1,"CD/USB changes do not repeat Android PAUSE");source(3);await(()->plays==1&&playing(),"default Android PLAY delivered on return");source(3);check(plays==1,"same AUX source does not repeat PLAY");
            baseline(false);source(2);source(3);SystemClock.sleep(350);check(pauses==0&&plays==0&&!playing(),"user-paused playback stays paused");
            baseline(true);AuxPlaybackSettings.pauseOnLeave(context,false);source(2);SystemClock.sleep(350);check(pauses==0&&playing(),"disabled pause setting leaves Android playing");AuxPlaybackSettings.pauseOnLeave(context,true);
            baseline(false);AuxPlaybackSettings.onReturn(context,AuxPlaybackPolicy.ReturnAction.ALWAYS_PLAY);source(2);source(3);await(()->plays==1&&playing(),"always play starts previously paused Android media");
            baseline(true);AuxPlaybackSettings.onReturn(context,AuxPlaybackPolicy.ReturnAction.NOTHING);source(4);await(()->pauses==1&&!playing(),"do-nothing still permits configured pause");refresh();source(3);SystemClock.sleep(350);check(plays==0&&!playing(),"do-nothing does not resume on AUX");
            AuxPlaybackSettings.onReturn(context,AuxPlaybackPolicy.ReturnAction.IF_AUTO_PAUSED);baseline(true);source(2);await(()->pauses==1&&!playing(),"manual override scenario auto-pauses");refresh();playback(true);refresh();playback(false);source(3);SystemClock.sleep(350);check(plays==0&&!playing(),"independent resume then manual pause cancels default resume");
            baseline(true);source(2);await(()->pauses==1&&!playing(),"reconnect scenario auto-pauses");refresh();state.newSession();state.running(true);state.status("test","reconnect",true);state.event(1000,new int[]{6291908},null,null);source(3);SystemClock.sleep(350);check(plays==0&&!playing(),"reconnect does not auto-play");
            result.putString("result","PASS: "+checks+" AUX settings/media checks; real Android media session, muted playback and no CAN writes");
        }catch(Throwable error){outcome=1;result.putString("failure",android.util.Log.getStackTraceString(error));}
        finally{if(old!=null){if(controller!=null)main(()->controller.close());if(activity!=null)main(()->activity.finish());main(()->{if(media!=null)media.release();if(audio!=null)audio.release();});state.newSession();state.running(false);restore(prefs,old);restore(bg,oldBg);I18n.language(context,lang);ThemeSettings.mode(context,theme);((android.app.job.JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE)).cancel(BackgroundRecovery.JOB_ID);}}
        finish(outcome,result);
    }
}
