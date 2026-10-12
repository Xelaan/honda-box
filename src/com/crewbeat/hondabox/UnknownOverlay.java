package com.crewbeat.hondabox;
import android.content.*;import android.os.*;import android.view.*;import android.widget.*;import android.graphics.*;import android.graphics.drawable.GradientDrawable;
import java.text.SimpleDateFormat;import java.util.*;
/** Opt-in, noninteractive diagnostics; repeats do not keep the popup on screen. */
public final class UnknownOverlay {
 private final Context context;private final Handler ui=new Handler(Looper.getMainLooper());private final WindowManager windows;
 private TextView view;private boolean started,wasEnabled,failed;private long baseline,shownRevision,until,session=-1;
 private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,200);}};
 public UnknownOverlay(Context c){context=c;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
 public static boolean enabled(Context c){return c.getSharedPreferences("overlay",0).getBoolean("unknownBsj",false);}
 public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("unknownBsj",on).apply();}
 private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
 public void start(){if(started)return;started=true;ui.post(tick);}
 public void refreshOptions(){failed=false;if(started)render();}
 private void hide(){if(view!=null){try{windows.removeViewImmediate(view);}catch(RuntimeException ignored){}view=null;}}
 private void render(){ProbeState state=ProbeState.INSTANCE;ProbeState.Snapshot s=state.overlaySnapshot();long rev=state.unknownMessages.revision();boolean on=enabled(context);
  if(!on||!s.running||!s.connected||!android.provider.Settings.canDrawOverlays(context)){hide();wasEnabled=false;baseline=rev;return;}
  if(!wasEnabled||session!=s.sessionId){hide();wasEnabled=true;session=s.sessionId;baseline=rev;shownRevision=rev;until=0;failed=false;return;}
  if(state.unknownMessages.latest().isEmpty()){hide();shownRevision=rev;until=0;return;}
  long now=SystemClock.elapsedRealtime();if(rev!=shownRevision){shownRevision=rev;until=now+8000;failed=false;}
  if(now>=until||failed){hide();return;}
  StringBuilder text=new StringBuilder(I18n.t(R.string.unknown_overlay_title));SimpleDateFormat time=new SimpleDateFormat("HH:mm:ss.SSS",Locale.US);int n=0;
  for(UnknownMessages.Entry e:state.unknownMessages.latest()){if(e.revision<=baseline)continue;if(n++==3)break;text.append("\n").append(time.format(new Date(e.firstAt))).append(String.format(Locale.US,"  0x%02X  ×%d",e.type,e.repeats)).append("  ").append(e.reason).append("\n").append(e.fields);}
  try{ThemeSettings.Palette t=ThemeSettings.resolve(context).palette;if(view==null){view=new TextView(context);view.setTextSize(12);view.setTypeface(Typeface.MONOSPACE);view.setPadding(dp(14),dp(10),dp(14),dp(10));view.setMaxLines(10);
   int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
   WindowManager.LayoutParams lp=new WindowManager.LayoutParams(Math.min(dp(580),context.getResources().getDisplayMetrics().widthPixels-dp(32)),WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.TOP|Gravity.LEFT;lp.x=dp(16);lp.y=dp(96);lp.setTitle("Honda Box Unknown BSJ");windows.addView(view,lp);}
   GradientDrawable bg=new GradientDrawable();bg.setColor(t.card);bg.setCornerRadius(dp(14));bg.setStroke(dp(1),t.accent);view.setBackground(bg);view.setTextColor(t.text);view.setText(text.toString());
  }catch(RuntimeException e){hide();failed=true;state.log("UNKNOWN OVERLAY ERROR: "+e.getClass().getSimpleName());}
 }
 public void close(){started=false;ui.removeCallbacks(tick);hide();wasEnabled=false;}
}
