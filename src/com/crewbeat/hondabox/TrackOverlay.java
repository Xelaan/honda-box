package com.crewbeat.hondabox;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.TextView;
/** New-track/station card; tapping opens the current source without sending commands. */
public final class TrackOverlay {
    private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());private final WindowManager windows;
    private final TrackChange changes=new TrackChange();private TrackCardView view;private long until,demoUntil;private boolean demoPending;private boolean started;
    public TrackOverlay(Context c){context=c;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
    public static boolean enabled(Context c){return c.getSharedPreferences("overlay",0).getBoolean("tracks",true);}
    public static void enabled(Context c,boolean value){c.getSharedPreferences("overlay",0).edit().putBoolean("tracks",value).apply();}
    private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,200);}};
    public void start(){if(!started){started=true;ui.post(tick);}}
    public void demo(){demoUntil=SystemClock.uptimeMillis()+4000;demoPending=true;if(started)render();}
    private void hide(){ProbeState.INSTANCE.trackOverlay(false);if(view!=null){view.animate().cancel();try{windows.removeViewImmediate(view);}catch(RuntimeException ignored){}view=null;}}
    private void openSource(){
        ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();AudioMode mode=AudioMode.reported(s);
        demoUntil=0;demoPending=false;hide();
        if(mode==null)return;
        try{context.startActivity(AudioPlayerActivity.intent(context,mode,false));}
        catch(RuntimeException e){ProbeState.INSTANCE.log("TRACK OPEN SOURCE ERROR: "+e);}
    }
    private void render(){ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();long now=SystemClock.uptimeMillis();boolean foreground=AudioPlayerActivity.isVisible();String title=changes.update(s,now,foreground);
        boolean demo=now<demoUntil;
        if(!Settings.canDrawOverlays(context)||(!demo&&(!s.connected||foreground||!enabled(context)))){hide();return;}
        if(demo){title=demoPending?I18n.t(R.string.track_demo_title):null;demoPending=false;}
        if(title!=null){hide();until=demo?demoUntil:now+4000;
            AudioMode mode=AudioMode.reported(s);int icon=s.mediaDevice==ProbeState.MediaDevice.IPOD?R.drawable.track_icon_ipod:mode==AudioMode.RADIO?R.drawable.track_icon_radio:mode==AudioMode.USB?R.drawable.track_icon_usb:R.drawable.track_icon_cd;
            String label=demo?I18n.t(R.string.track_demo_label):I18n.t(R.string.track_now_playing)+"  ·  "+s.source();view=new TrackCardView(context,title,label,icon,until);
            view.setOnClickListener(v->openSource());
            WindowManager.LayoutParams lp=new WindowManager.LayoutParams(Math.min(dp(600),context.getResources().getDisplayMetrics().widthPixels-dp(32)),dp(88),Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;lp.y=dp(6);lp.alpha=VolumeOverlay.WINDOW_ALPHA;lp.setTitle("Honda Box New Track");try{windows.addView(view,lp);ProbeState.INSTANCE.trackOverlay(true);view.setAlpha(0);view.setTranslationY(-dp(8));view.animate().alpha(1).translationY(0).setDuration(180).start();}catch(RuntimeException e){view=null;ProbeState.INSTANCE.log("TRACK OVERLAY: "+e);}}
        if(now>=until||(!demo&&s.carMenuOverlayVisible))hide();
    }
    public void close(){started=false;demoUntil=0;demoPending=false;ui.removeCallbacks(tick);hide();}
}
