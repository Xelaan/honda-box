package com.crewbeat.hondabox;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.*;

/** Local display only. It never sets Android or OEM volume and never captures touch. */
public final class VolumeOverlay {
    static final float WINDOW_ALPHA=.8f;
    private final Context context; private final Handler ui=new Handler(Looper.getMainLooper());
    private final WindowManager windows; private final ProbeState state=ProbeState.INSTANCE;
    private WindowManager.LayoutParams layout;private Indicator view; private boolean started, failed, wasAllowed;
    private long session=-1, changedUntil, demoUntil; private Integer lastVolume;
    private final Runnable tick=new Runnable(){@Override public void run(){ if(!started)return; render();ui.postDelayed(this,250); }};
    public VolumeOverlay(Context c){context=c;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
    private static SharedPreferences prefs(Context c){return c.getSharedPreferences("overlay",Context.MODE_PRIVATE);}
    public static boolean enabled(Context c){return prefs(c).getBoolean("enabled",true);}
    public static boolean pinned(Context c){return prefs(c).getBoolean("pinned",false);}
    public static void options(Context c,boolean enabled,boolean pinned){prefs(c).edit().putBoolean("enabled",enabled).putBoolean("pinned",pinned).apply();}
    public void start(){if(started)return;started=true;ui.post(tick);}
    public void refreshOptions(){demoUntil=0;failed=false;if(started)render();}
    public void demo(){demoUntil=SystemClock.uptimeMillis()+15000;failed=false;if(started)render();}
    private int dp(float x){return Math.round(x*context.getResources().getDisplayMetrics().density);}
    private String sourceTitle(ProbeState.Snapshot s){
        AudioMode mode=AudioMode.reported(s);String title="—";
        if(mode==AudioMode.RADIO&&s.fresh(119)){
            Integer band=s.displayNumber(115);title=RadioFrequency.value(band,s.displayNumber(119),s.honda());
            String unit=RadioFrequency.unit(band);unit=unit.equals("kHz")?I18n.t(R.string.radio_khz):unit.equals("MHz")?I18n.t(R.string.radio_mhz):unit;
            if(!title.equals("—")&&!unit.isEmpty())title+=" "+unit;
        }else if(mode==AudioMode.CD||mode==AudioMode.USB||mode==AudioMode.IPOD){
            title=s.combinedMediaText();
            if(title.equals("—")&&!s.cdNumber(101).equals("—"))title=I18n.t(R.string.ui_track)+s.cdNumber(101);
        }
        return s.source()+(title.equals("—")?I18n.t(R.string.ui_vehicle_audio):" · "+title);
    }
    private void hide(){if(view!=null){try{windows.removeViewImmediate(view);}catch(RuntimeException ignored){}view=null;layout=null;}state.overlayPosition(0);}
    private void render(){
        ProbeState.Snapshot s=state.overlaySnapshot(); long now=SystemClock.uptimeMillis();
        if(session!=s.sessionId || !s.connected){session=s.sessionId;lastVolume=null;changedUntil=0;}
        Integer volume=s.connected?s.number(132):null;
        if(volume!=null && !volume.equals(lastVolume)){lastVolume=volume;changedUntil=now+3000;}
        boolean allowed=Settings.canDrawOverlays(context); if(allowed!=wasAllowed){failed=false;wasAllowed=allowed;}
        boolean test=now<demoUntil;
        if(!allowed){hide();state.overlay(false,I18n.t(R.string.ui_overlay_permission_not_granted),"");return;}
        if(!enabled(context) && !test){hide();state.overlay(false,I18n.t(R.string.ui_indicator_off),"");return;}
        boolean show=test || pinned(context) || (volume!=null && now<changedUntil);
        if(!show){hide();state.overlay(false,s.connected?I18n.t(R.string.ui_waiting_for_a_volume_change):I18n.t(R.string.ui_waiting_for_can_connection),"");return;}
        String value=test?"17":volume==null?"—":String.valueOf(volume);
        String subtitle=test?I18n.t(R.string.ui_test_audio_unchanged):!s.connected?I18n.t(R.string.ui_no_can_connection):volume==null?I18n.t(R.string.ui_waiting_for_volume_data):sourceTitle(s);
        if(failed)return;
        try{
            if(view==null){
                view=new Indicator(context);
                int width=Math.min(dp(332),context.getResources().getDisplayMetrics().widthPixels-dp(24));
                int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
                WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,dp(88),type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
                p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;p.y=dp(24);p.alpha=WINDOW_ALPHA;
                p.setTitle("FYT Probe Volume Overlay");layout=p;windows.addView(view,p);
            }
            int gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL,x=0;
            if(layout.gravity!=gravity||layout.x!=x){layout.gravity=gravity;layout.x=x;windows.updateViewLayout(view,layout);}
            int offset=dp(s.trackOverlayVisible?126:16);
            if(layout.y!=offset){layout.y=offset;windows.updateViewLayout(view,layout);}state.overlayPosition(0);
            view.content(value,subtitle,test);state.overlay(true,test?I18n.t(R.string.ui_test_indicator_15_seconds):I18n.t(R.string.ui_indicator_visible),value+" · "+subtitle);
        }catch(RuntimeException e){hide();failed=true;state.overlay(false,I18n.t(R.string.ui_cannot_display)+e.getClass().getSimpleName(),"");state.log("OVERLAY ERROR: "+e);}
    }
    public void close(){started=false;ui.removeCallbacks(tick);hide();state.overlay(false,I18n.t(R.string.ui_stopped_with_probe),"");}
    private final class Indicator extends View{
        private final TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);private String value="—",subtitle="";private boolean demo;private ThemeSettings.Palette palette;
        Indicator(Context c){super(c);setLayerType(View.LAYER_TYPE_SOFTWARE,null);setContentDescription(I18n.t(R.string.ui_fyt_probe_vehicle_volume_indicator));}
        void content(String v,String sub,boolean test){ThemeSettings.Palette next=ThemeSettings.resolve(context).palette;if(value.equals(v)&&subtitle.equals(sub)&&demo==test&&palette==next)return;palette=next;value=v;subtitle=sub;demo=test;invalidate();}
        private void line(Canvas c,float x1,float y1,float x2,float y2){c.drawLine(dp(x1),dp(y1),dp(x2),dp(y2),p);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);ThemeSettings.Palette theme=palette==null?ThemeSettings.resolve(context).palette:palette;float w=getWidth(),h=getHeight();p.setStyle(Paint.Style.FILL);p.setColor(theme.card);
            c.drawRoundRect(new RectF(dp(1),dp(1),w-dp(1),h-dp(1)),dp(22),dp(22),p);
            p.setColor(theme.border);p.setStrokeWidth(dp(1));p.setStyle(Paint.Style.STROKE);
            c.drawRoundRect(new RectF(dp(1),dp(1),w-dp(1),h-dp(1)),dp(22),dp(22),p);
            p.setColor(demo?theme.warning:theme.accent);p.setStrokeWidth(dp(2));p.setStrokeJoin(Paint.Join.ROUND);
            Path speaker=new Path();speaker.moveTo(dp(24),dp(35));speaker.lineTo(dp(30),dp(35));speaker.lineTo(dp(38),dp(28));
            speaker.lineTo(dp(38),dp(54));speaker.lineTo(dp(30),dp(47));speaker.lineTo(dp(24),dp(47));speaker.close();c.drawPath(speaker,p);
            c.drawArc(new RectF(dp(34),dp(29),dp(51),dp(53)),-55,110,false,p);
            p.setStyle(Paint.Style.FILL);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(dp(13));
            c.drawText(demo?I18n.t(R.string.ui_overlay_test):I18n.t(R.string.ui_volume),dp(67),dp(32),p);
            p.setColor(theme.muted);p.setTextSize(dp(13));p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
            String sub=TextUtils.ellipsize(subtitle,p,Math.max(0,w-dp(150)),TextUtils.TruncateAt.END).toString();
            c.drawText(sub,dp(67),dp(54),p);
            p.setColor(theme.text);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));p.setTextSize(dp(38));p.setTextAlign(Paint.Align.RIGHT);
            c.drawText(value,w-dp(24),dp(57),p);p.setTextAlign(Paint.Align.LEFT);
            // OEM Honda volume range is0..40; the gauge follows the reported value.
            float fraction=0;try{fraction=Math.max(0,Math.min(40,Integer.parseInt(value)))/40f;}catch(NumberFormatException ignored){}
            float barLeft=dp(67),barRight=w-dp(24);
            p.setColor(theme.border);c.drawRoundRect(new RectF(barLeft,dp(65),barRight,dp(69)),dp(2),dp(2),p);
            if(fraction>0){p.setColor(demo?theme.warning:theme.accent);c.drawRoundRect(new RectF(barLeft,dp(65),barLeft+(barRight-barLeft)*fraction,dp(69)),dp(2),dp(2),p);}
        }
    }
}
