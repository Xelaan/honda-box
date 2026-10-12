package com.crewbeat.hondabox;
import android.app.Activity;import android.os.*;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import android.view.*;import android.widget.*;
/** Passive trip-computer screen. Unknown values remain unknown, including callback defaults. */
public final class CarInfoActivity extends Activity {
 private final Handler ui=new Handler(Looper.getMainLooper());private ThemeSettings.Palette palette;private int language=-1;private TextView connection,explanation,raw;private boolean expanded;
 private final Runnable tick=new Runnable(){public void run(){render();ui.postDelayed(this,400);}};
 @Override public void onCreate(Bundle b){super.onCreate(b);expanded=b!=null&&b.getBoolean("rawExpanded");build();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
 private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("tnum");return v;}
 private View gap(int n){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(n)));return v;}
 private void build(){palette=ThemeSettings.resolve(this).palette;language=I18n.revision();setTitle(I18n.t(R.string.car_info_title));getWindow().setStatusBarColor(palette.bg);getWindow().setNavigationBarColor(palette.bg);getWindow().getDecorView().setSystemUiVisibility(palette.night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(palette.bg);LinearLayout root=column();root.setPadding(dp(28),dp(18),dp(28),dp(20));scroll.addView(root);setContentView(scroll);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);LinearLayout title=column();title.addView(text("HONDA  /  CAR INFO",11,palette.accent));TextView heading=text(I18n.t(R.string.car_info_title),28,palette.text);heading.setTypeface(Typeface.create("sans-serif-medium",0));title.addView(heading);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));Button back=ConsoleControls.button(this,I18n.t(R.string.ui_back),palette,false);back.setOnClickListener(v->finish());header.addView(back);root.addView(header);root.addView(gap(10));connection=text("",12,palette.muted);root.addView(connection);root.addView(gap(14));
  boolean wide=getResources().getConfiguration().screenWidthDp>=600;String[] labels={I18n.t(R.string.car_info_consumption),I18n.t(R.string.car_info_range),"Trip A","Trip B"};
  for(int row=0;row<2;row++){LinearLayout line=new LinearLayout(this);line.setOrientation(wide?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);for(int j=0;j<2;j++){LinearLayout card=column();card.setPadding(dp(20),dp(14),dp(20),dp(14));GradientDrawable bg=new GradientDrawable();bg.setColor(palette.card);bg.setCornerRadius(dp(18));card.setBackground(bg);card.addView(text(labels[row*2+j],13,palette.muted));card.addView(gap(8));TextView value=text("—",30,palette.text);value.setTypeface(Typeface.create("sans-serif-medium",0));card.addView(value);card.addView(text(I18n.t(R.string.car_info_unavailable),11,palette.muted));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(wide?0:-1,-2,wide?1:0);if(wide&&j==0)lp.rightMargin=dp(14);if(!wide)lp.bottomMargin=dp(12);line.addView(card,lp);}root.addView(line);root.addView(gap(12));}
  explanation=text(I18n.t(R.string.car_info_unconfirmed),12,palette.muted);root.addView(explanation);root.addView(gap(10));Button details=ConsoleControls.button(this,I18n.t(R.string.car_info_received_fields),palette,false);details.setOnClickListener(v->{expanded=!expanded;raw.setVisibility(expanded?View.VISIBLE:View.GONE);});root.addView(details);raw=text("",12,palette.text);raw.setTypeface(Typeface.MONOSPACE);raw.setPadding(dp(8),dp(10),dp(8),0);raw.setVisibility(expanded?View.VISIBLE:View.GONE);root.addView(raw);
 }
 public static ProbeState.Value received(ProbeState.Snapshot s,int code){
  if(!Integer.valueOf(6291908).equals(s.number(1000)))return null;
  ProbeState.Value decoded=s.bsjValues.get(code);if(decoded!=null)return decoded;
  ProbeState.Value callback=s.values.get(code);return callback!=null&&callback.ints!=null&&callback.ints.length>0&&callback.ints[0]!=0?callback:null;
 }
 private void render(){if(language!=I18n.revision()||palette!=ThemeSettings.resolve(this).palette)build();ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();connection.setText(s.connected?I18n.t(R.string.car_info_live):I18n.t(R.string.car_info_disconnected));connection.setTextColor(s.connected?palette.accent:palette.muted);
  StringBuilder lines=new StringBuilder();for(int code=140;code<=147;code++){ProbeState.Value v=received(s,code);lines.append('[').append(code).append("] ");if(v==null)lines.append(I18n.t(R.string.car_info_unavailable));else lines.append(v.ints[0]).append("  · ").append(String.format(java.util.Locale.US,"%tT",v.at)).append("  · ").append(s.bsjValues.containsKey(code)?"BSJ 0x0E":"FYT");lines.append('\n');}raw.setText(lines.toString());
 }
 @Override protected void onStart(){super.onStart();ui.removeCallbacks(tick);ui.post(tick);}
 @Override protected void onStop(){ui.removeCallbacks(tick);super.onStop();}
 @Override protected void onSaveInstanceState(Bundle b){b.putBoolean("rawExpanded",expanded);super.onSaveInstanceState(b);}
}
