package com.crewbeat.hondabox;

import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;

/** Read-only source/on-off popup; optionally stays visible while verified audio power is off. */
public final class SourceOverlay {
    private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());private final WindowManager windows;
    private Indicator view;private boolean started,failed,wasAllowed,announcementPending;
    private long session=-1,shownUntil;private Integer profile;private String lastLabel="";private long powerRevision=-1;private Boolean powerNotice;
    private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,250);}};
    public SourceOverlay(Context c){context=c;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
    public static boolean enabled(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getBoolean("source",true);}
    public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().putBoolean("source",on).apply();}
    public static boolean keepVisibleWhenOff(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE).getBoolean("sourceKeepWhenOff",true);}
    public static void keepVisibleWhenOff(Context c,boolean on){c.getSharedPreferences("overlay",Context.MODE_PRIVATE).edit().putBoolean("sourceKeepWhenOff",on).apply();}
    public void start(){if(started)return;started=true;announcementPending=true;ui.post(tick);}
    /** Called on the UI thread. Missing connection/source data leaves the request pending. */
    public void announceCurrentSource(){if(!started)return;announcementPending=true;failed=false;render();}
    public void refreshOptions(){failed=false;if(started)render();}
    private int dp(float n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
    private void hide(){if(view!=null){try{windows.removeViewImmediate(view);}catch(RuntimeException ignored){}view=null;}}
    private Integer lastSource;
    private void reset(){shownUntil=0;lastLabel="";lastSource=null;powerRevision=-1;powerNotice=null;}
    private void render(){
        ProbeState state=ProbeState.INSTANCE;boolean allowed=Settings.canDrawOverlays(context);
        if(allowed!=wasAllowed){failed=false;wasAllowed=allowed;}
        if(!enabled(context)){hide();reset();state.sourceOverlay(false,I18n.t(R.string.ui_source_indicator_off),"");return;}
        if(!allowed){hide();reset();state.sourceOverlay(false,I18n.t(R.string.ui_overlay_permission_not_granted),"");return;}
        ProbeState.Snapshot s=state.overlaySnapshot();
        if(session!=s.sessionId||!java.util.Objects.equals(profile,s.number(1000))){session=s.sessionId;profile=s.number(1000);reset();}
        Integer source=s.audioSource();
        boolean inferredIpod=s.mediaDevice!=ProbeState.MediaDevice.UNKNOWN&&(source==null||source==0);if(!s.running||!s.connected||(!inferredIpod&&(source==null||source<1||source>16))){
            hide();reset();state.sourceOverlay(false,s.connected?I18n.t(R.string.ui_waiting_for_source_data):I18n.t(R.string.ui_waiting_for_can_connection),"");return;
        }
        long now=SystemClock.elapsedRealtime();String label=s.source();Integer work=s.displayNumber(114);if(AudioMode.reported(s)==AudioMode.USB&&s.fresh(114)&&(Integer.valueOf(9).equals(work)||Integer.valueOf(10).equals(work)))label+=" · "+I18n.t(R.string.media_no_data);
        // First raw state establishes a baseline; duplicate packets/volume ramps do not retrigger.
        boolean powerChanged=powerRevision>=0&&s.bsjAudioPowerRevision!=powerRevision&&s.bsjAudioEnabled!=null;
        powerRevision=s.bsjAudioPowerRevision;
        boolean sourceChanged=!label.equals(lastLabel)||!java.util.Objects.equals(source,lastSource);
        if(powerChanged||sourceChanged||announcementPending){announcementPending=false;powerNotice=powerChanged?s.bsjAudioEnabled:null;lastLabel=label;lastSource=source;shownUntil=now+3000;failed=false;}
        boolean pinnedOff=keepVisibleWhenOff(context)&&Boolean.FALSE.equals(s.bsjAudioEnabled);
        Boolean displayedPower=Boolean.FALSE.equals(s.bsjAudioEnabled)?Boolean.FALSE:powerNotice;
        String caption=I18n.t(R.string.ui_vehicle_source_132);
        String suffix=displayedPower==null?"":" · "+I18n.t(displayedPower?R.string.ui_on:R.string.ui_off).toUpperCase(java.util.Locale.ROOT);
        if(!pinnedOff&&now>=shownUntil){hide();state.sourceOverlay(false,I18n.t(R.string.ui_waiting_for_a_source_change),"");return;}
        if(failed)return;
        try{
            if(view==null){view=new Indicator(context);int width=Math.min(dp(270),context.getResources().getDisplayMetrics().widthPixels-dp(24));
                int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                WindowManager.LayoutParams lp=new WindowManager.LayoutParams(width,dp(68),type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
                lp.gravity=Gravity.TOP|Gravity.RIGHT;lp.x=dp(16);lp.y=dp(16);lp.alpha=.8f;lp.setTitle("Honda Box Source Overlay");windows.addView(view,lp);
            }
            view.content(label,caption,suffix);state.sourceOverlay(true,I18n.t(pinnedOff?R.string.source_visible_while_off:R.string.ui_source_visible_hides_after_3),label+suffix);
        }catch(RuntimeException e){hide();failed=true;state.sourceOverlay(false,I18n.t(R.string.ui_cannot_display)+e.getClass().getSimpleName(),"");state.log("SOURCE OVERLAY ERROR: "+e);}
    }
    public void close(){started=false;announcementPending=false;ui.removeCallbacks(tick);hide();reset();ProbeState.INSTANCE.sourceOverlay(false,I18n.t(R.string.ui_stopped_with_probe),"");}
    private final class Indicator extends View{
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private String label="",caption="",suffix="";private ThemeSettings.Palette palette;
        Indicator(Context c){super(c);setContentDescription(I18n.t(R.string.ui_fyt_probe_current_vehicle_audio));}
        void content(String text,String heading,String status){ThemeSettings.Palette next=ThemeSettings.resolve(context).palette;if(label.equals(text)&&caption.equals(heading)&&suffix.equals(status)&&palette==next)return;label=text;caption=heading;suffix=status;palette=next;setContentDescription(heading+" · "+text+status);invalidate();}
        @Override protected void onDraw(Canvas c){super.onDraw(c);ThemeSettings.Palette t=palette==null?ThemeSettings.resolve(context).palette:palette;
            p.setStyle(Paint.Style.FILL);p.setColor(t.card);c.drawRoundRect(new RectF(dp(1),dp(1),getWidth()-dp(1),getHeight()-dp(1)),dp(18),dp(18),p);
            p.setStyle(Paint.Style.STROKE);p.setColor(t.border);p.setStrokeWidth(dp(1));c.drawRoundRect(new RectF(dp(1),dp(1),getWidth()-dp(1),getHeight()-dp(1)),dp(18),dp(18),p);
            p.setStyle(Paint.Style.FILL);p.setColor(t.muted);p.setTextSize(dp(12));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(caption,dp(18),dp(23),p);
            p.setColor(t.text);p.setTextSize(dp(25));p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
            String value=label;float available=getWidth()-dp(36)-p.measureText(suffix);while(value.length()>0&&p.measureText(value)>available)value=value.substring(0,value.length()-1);c.drawText(value+suffix,dp(18),dp(51),p);
        }
    }
}
