package dev.alex.fyt.probe;

import android.content.Context;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** Passive OEM display. Closing this window never sends a command to the vehicle. */
public final class CarMenuOverlay {
    public static final long DURATION=5000;
    private final Context context;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private final WindowManager windows;
    private CarMenuPopupView view;
    private boolean started,failed,wasAllowed,lastEnabled,ready;
    private Integer profile;
    private long session=-1,revision,baselineAt,until,demoUntil;
    private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,200);}};
    public CarMenuOverlay(Context c){context=c;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
    public static boolean enabled(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getBoolean("carMenu",true);}
    public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().putBoolean("carMenu",on).apply();}
    public void start(){if(started)return;started=true;ui.post(tick);}
    public void refreshOptions(){demoUntil=0;failed=false;if(started)render();}
    public void demo(){demoUntil=SystemClock.uptimeMillis()+15000;failed=false;if(started)render();}
    private int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    private void hide(){if(view!=null){view.animate().cancel();try{windows.removeViewImmediate(view);}catch(RuntimeException ignored){}view=null;}}
    private void reset(ProbeState.Snapshot s){session=s.sessionId;profile=s.number(1000);revision=s.menuRevision;baselineAt=0;ready=false;until=0;}
    private void render(){
        ProbeState state=ProbeState.INSTANCE;ProbeState.Snapshot s=state.overlaySnapshot();long now=SystemClock.uptimeMillis();boolean on=enabled(context);
        if(session!=s.sessionId||!java.util.Objects.equals(profile,s.number(1000))||!s.connected||on!=lastEnabled){reset(s);lastEnabled=on;}
        if(s.connected&&s.carMenuSupported()){
            if(!ready){if(baselineAt==0)baselineAt=now;revision=s.menuRevision;if(now-baselineAt>=750)ready=true;}
            else if(revision!=s.menuRevision){
                revision=s.menuRevision;
                boolean closed=Integer.valueOf(0).equals(s.displayNumber(139))&&s.menuVisibilitySequence>=s.menuContentSequence;
                if(!s.hasMenuRows()||closed)until=0;else if(on)until=now+DURATION;
            }
        }else until=0;
        boolean allowed=Settings.canDrawOverlays(context);if(wasAllowed!=allowed){failed=false;wasAllowed=allowed;until=0;}
        boolean test=now<demoUntil;
        if(!allowed){hide();state.carMenuOverlay(false,I18n.t(R.string.ui_overlay_permission_not_granted),"");return;}
        if(!on&&!test){hide();state.carMenuOverlay(false,I18n.t(R.string.ui_car_menu_off),"");return;}
        if(!test&&(!s.connected||!s.carMenuSupported()||now>=until)){hide();state.carMenuOverlay(false,!s.connected?I18n.t(R.string.ui_waiting_for_can_connection):!s.carMenuSupported()?I18n.t(R.string.ui_menu_schema_unverified):I18n.t(R.string.ui_waiting_for_a_menu_change),"");return;}
        if(failed)return;
        try{
            if(view==null){
                view=new CarMenuPopupView(context,()->{until=0;demoUntil=0;hide();state.carMenuOverlay(false,I18n.t(R.string.ui_menu_hidden),"");},()->{if(until>0)until=SystemClock.uptimeMillis()+DURATION;});
                int screenWidth=context.getResources().getDisplayMetrics().widthPixels,screenHeight=context.getResources().getDisplayMetrics().heightPixels;
                int width=Math.min(dp(510),Math.round(screenWidth*.44f));if(screenWidth<dp(500))width=screenWidth-dp(32);
                int height=Math.min(dp(280),Math.round(screenHeight*.53f));
                int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                WindowManager.LayoutParams lp=new WindowManager.LayoutParams(width,height,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
                lp.gravity=Gravity.CENTER;lp.x=0;lp.y=0;lp.alpha=1f;lp.setTitle("FYT Probe Car Menu Overlay");
                windows.addView(view,lp);view.setAlpha(0);view.animate().alpha(1).setDuration(180).start();
            }
            ProbeState.Snapshot shown=test?demoSnapshot():s;view.content(shown,test);
            state.carMenuOverlay(true,test?I18n.t(R.string.ui_menu_test_15_seconds):I18n.t(R.string.ui_car_menu_5_seconds),shown.menu());
        }catch(RuntimeException e){hide();failed=true;state.carMenuOverlay(false,I18n.t(R.string.ui_cannot_display)+e.getClass().getSimpleName(),"");state.log("CAR MENU OVERLAY ERROR: "+e);}
    }
    private ProbeState.Snapshot demoSnapshot(){ProbeState.Snapshot s=new ProbeState.Snapshot();s.connected=true;s.values=new java.util.LinkedHashMap<>();s.values.put(1000,new ProbeState.Value(new int[]{6291908},null,null));
        String[] lines={"AUDIO SETTINGS","BASS          +1","TREBLE         C"};for(int i=0;i<3;i++)s.values.put(135+i,new ProbeState.Value(null,null,new String[]{lines[i]}));return s;}
    public void close(){started=false;ui.removeCallbacks(tick);hide();demoUntil=0;until=0;ProbeState.INSTANCE.carMenuOverlay(false,I18n.t(R.string.ui_stopped_with_probe),"");}
}
