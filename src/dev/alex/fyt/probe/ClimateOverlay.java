package dev.alex.fyt.probe;

import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.animation.DecelerateInterpolator;
import java.util.LinkedHashMap;

/** Read-only bottom window. Only actual changes after the connection baseline open it. */
public final class ClimateOverlay {
    public static final long DURATION=5000;
    private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());private final WindowManager windows;
    private final TemperatureIndicators temperatures=new TemperatureIndicators();
    private FrameLayout windowView;private ImageButton settingsButton;private ThemeSettings.Palette settingsPalette;private int transition;private boolean closing;private ClimatePopupView view;private boolean started,failed,wasAllowed,lastEnabled,ready;
    private long session=-1,revision,baselineAt,until,demoUntil,manualUntil;private boolean manualDemo;private int height;
    private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,200);}};
    public ClimateOverlay(Context c){context=c;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
    public static boolean enabled(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getBoolean("climate",true);}
    public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().putBoolean("climate",on).apply();}
    public void start(){if(started)return;started=true;ui.post(tick);}
    public void refreshOptions(){failed=false;if(started)render();}
    public void demo(){demoUntil=SystemClock.uptimeMillis()+15000;until=0;failed=false;if(started)render();}
    public void showFromStrip(boolean sample){ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();if(!sample&&(!s.connected||!s.honda()))return;manualDemo=sample;manualUntil=SystemClock.uptimeMillis()+DURATION;failed=false;if(started)render();}
    private int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    private void hide(){transition++;closing=false;if(windowView!=null){windowView.animate().cancel();try{windows.removeViewImmediate(windowView);}catch(RuntimeException ignored){}}windowView=null;view=null;settingsButton=null;settingsPalette=null;height=0;}
    private void hideAnimated(){if(view==null||closing)return;closing=true;final ClimatePopupView target=view;final int token=++transition;
        windowView.animate().cancel();windowView.animate().translationY(height).setDuration(220).setInterpolator(new DecelerateInterpolator()).withEndAction(()->{if(token==transition&&view==target)hide();}).start();
    }
    private void openSettings(){
        try{
            context.startActivity(new Intent(context,SettingsActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP));
            until=demoUntil=manualUntil=0;manualDemo=false;render();
        }catch(RuntimeException e){ProbeState.INSTANCE.log("CLIMATE SETTINGS: "+e);Toast.makeText(context,I18n.t(R.string.settings_unavailable),Toast.LENGTH_SHORT).show();}
    }
    private void updateSettingsButton(){
        ThemeSettings.Palette palette=ThemeSettings.resolve(context).palette;
        if(settingsPalette!=palette){settingsPalette=palette;settingsButton.setBackground(null);settingsButton.setColorFilter(palette.muted);}
        settingsButton.setContentDescription(I18n.t(R.string.climate_open_settings));
    }
    private void reset(ProbeState.Snapshot s){session=s.sessionId;revision=s.climateRevision;baselineAt=0;ready=false;until=0;}
    private void render(){
        ProbeState state=ProbeState.INSTANCE;ProbeState.Snapshot s=state.overlaySnapshot();long now=SystemClock.uptimeMillis();boolean on=enabled(context);
        if(session!=s.sessionId||!s.connected||on!=lastEnabled){reset(s);lastEnabled=on;}
        if(s.connected&&s.honda()){
            if(!ready){if(baselineAt==0&&s.number(21)!=null)baselineAt=now;
                revision=s.climateRevision;if(baselineAt>0&&now-baselineAt>=750)ready=true;
            }else if(revision!=s.climateRevision){revision=s.climateRevision;if(on)until=now+DURATION;}
        }else until=0;
        boolean allowed=Settings.canDrawOverlays(context);if(wasAllowed!=allowed){failed=false;wasAllowed=allowed;}
        boolean manual=now<manualUntil;boolean test=now<demoUntil||(manual&&manualDemo);temperatures.observe(s,ready&&on&&!test,now);
        if(!allowed){hide();state.climateOverlay(false,I18n.t(R.string.ui_overlay_permission_not_granted),"",0);return;}
        if(!on&&!test&&!manual){hideAnimated();state.climateOverlay(false,I18n.t(R.string.ui_climate_indicator_off),"",0);return;}
        if(!test&&(!s.connected||!s.honda()||(!manual&&now>=until))){if(s.connected&&s.honda())hideAnimated();else hide();state.climateOverlay(false,!s.connected?I18n.t(R.string.ui_waiting_for_can_connection):!s.honda()?I18n.t(R.string.ui_climate_schema_unverified):I18n.t(R.string.ui_waiting_for_a_climate_change),"",0);return;}
        if(failed)return;
        try{
            if(view==null){view=new ClimatePopupView(context,temperatures);view.setOnTouchListener((v,event)->{if(event.getAction()==MotionEvent.ACTION_UP){until=0;demoUntil=0;manualUntil=0;render();}return true;});
                height=Math.min(dp(238),Math.round(context.getResources().getDisplayMetrics().heightPixels*.46f));
                int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,height,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
                lp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;lp.y=0;lp.alpha=1f;lp.setTitle("FYT Probe Climate Overlay");windowView=new FrameLayout(context);windowView.addView(view,new FrameLayout.LayoutParams(-1,-1));
                settingsButton=new ImageButton(context);settingsButton.setImageResource(R.drawable.ic_climate_settings);settingsButton.setPadding(dp(8),dp(8),dp(8),dp(8));settingsButton.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);settingsButton.setTag("climate-settings");settingsButton.setOnClickListener(v->openSettings());
                FrameLayout.LayoutParams settingsLayout=new FrameLayout.LayoutParams(dp(48),dp(48),Gravity.TOP|Gravity.RIGHT);settingsLayout.topMargin=dp(18);settingsLayout.rightMargin=dp(14);windowView.addView(settingsButton,settingsLayout);updateSettingsButton();
                windows.addView(windowView,lp);windowView.setTranslationY(height);windowView.animate().translationY(0).setDuration(280).setInterpolator(new DecelerateInterpolator()).start();
            }
            if(closing){closing=false;transition++;windowView.animate().cancel();windowView.animate().translationY(0).setDuration(180).setInterpolator(new DecelerateInterpolator()).start();}
            ProbeState.Snapshot shown=test?demoSnapshot():s;view.content(shown,test);updateSettingsButton();
            state.climateOverlay(true,test?I18n.t(R.string.ui_climate_test_15_seconds):I18n.t(R.string.ui_climate_change_5_seconds),(test?I18n.t(R.string.ui_test):"")+shown.temperature(27)+" / "+shown.temperature(28)+" · DUAL "+shown.dual(),height);
        }catch(RuntimeException e){hide();failed=true;state.climateOverlay(false,I18n.t(R.string.ui_cannot_display)+e.getClass().getSimpleName(),"",0);state.log("CLIMATE OVERLAY ERROR: "+e);}
    }
    static ProbeState.Snapshot demoSnapshot(){ProbeState.Snapshot s=new ProbeState.Snapshot();s.connected=true;s.dualAvailable=true;s.optionalClimateSeen=(1L<<12)|(1L<<14)|(1L<<16)|(1L<<17)|(1L<<33)|(1L<<36)|(1L<<49);s.values=new LinkedHashMap<>();
        int[] codes={1000,10,11,13,14,18,19,20,21,27,28,12,16,17,33,36,49},data={6291908,1,1,1,1,0,1,1,4,44,46,1,0,0,1,0,1};
        for(int i=0;i<codes.length;i++)s.values.put(codes[i],new ProbeState.Value(new int[]{data[i]},null,null));return s;
    }
    public boolean isWindowPresent(){return windowView!=null;}
    public void close(){manualUntil=0;manualDemo=false;started=false;ui.removeCallbacks(tick);hide();ProbeState.INSTANCE.climateOverlay(false,I18n.t(R.string.ui_stopped_with_probe),"",0);}
}
