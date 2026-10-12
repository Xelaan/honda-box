package dev.alex.fyt.probe;

import android.content.*;
import android.os.*;
import android.provider.Settings;

/** Source-driven normal Activity launches. It creates no WindowManager window. */
public final class AudioSourceController {
    private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());
    private long quietUntil;
    private boolean started,wasEnabled,quietBaseline;private long session=-1,sequence=-1,deviceRevision=-1;
    private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,200);}};
    public AudioSourceController(Context c){context=c;wasEnabled=enabled(c);}
    public static boolean enabled(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getBoolean("audioScreens",true);}
    public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().putBoolean("audioScreens",on).apply();}
    public void backgroundStart(){quietBaseline=true;quietUntil=SystemClock.elapsedRealtime()+5000;}
    public void start(){if(started)return;started=true;ui.post(tick);}
    public void refreshOptions(){if(started)render();}
    private void render(){ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();boolean on=enabled(context);
        if(!s.connected){session=-1;sequence=-1;deviceRevision=-1;return;}
        if(AudioMode.aux(s))AudioPlayerActivity.hideForAux();
        boolean first=s.sessionId!=session,changed=sequence!=s.sourceSequence||deviceRevision!=s.mediaDeviceRevision;deviceRevision=s.mediaDeviceRevision;session=s.sessionId;
        if(!on||!wasEnabled){sequence=s.sourceSequence;wasEnabled=on;return;}wasEnabled=on;
        if(!changed)return;sequence=s.sourceSequence;AudioMode mode=AudioMode.reported(s);
        if(SystemClock.elapsedRealtime()<quietUntil){if(mode!=null)quietBaseline=false;return;}
        Integer work=s.fresh(114)?s.displayNumber(114):null;if(mode!=AudioMode.RADIO&&(Integer.valueOf(9).equals(work)||Integer.valueOf(10).equals(work)))return;
        if(mode!=null&&quietBaseline){quietBaseline=false;return;}
        // Opening a radio subscription does not steal the screen. A current CD can open on connection.
        // Consume the change without navigating away from any open Honda Box screen.
        // It must not become a deferred launch when the user leaves the app.
        if(mode==null||(first&&mode==AudioMode.RADIO)||ProbeApplication.foreground()||AudioPlayerActivity.isVisible()||DebugActivity.isVisible())return;
        if(Build.VERSION.SDK_INT>=29&&!Settings.canDrawOverlays(context)){ProbeState.INSTANCE.log("AUDIO SCREEN: automatic launch needs overlay permission or visible player; launcher icons remain available.");return;}
        try{context.startActivity(AudioPlayerActivity.intent(context,mode,true));}
        catch(RuntimeException e){ProbeState.INSTANCE.log("AUDIO SCREEN ERROR: "+e);}
    }
    public void close(){started=false;ui.removeCallbacks(tick);}
}
