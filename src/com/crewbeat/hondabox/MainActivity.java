package com.crewbeat.hondabox;

import android.app.Activity;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;

/** Main audio selector. Only a deliberate source tap sends an OEM selection command. */
public final class MainActivity extends Activity {
    private final Handler ui=new Handler(Looper.getMainLooper());
    private ThemeSettings.Palette palette;private int languageRevision=-1;private TextView status,source,audioOffChip;private View audioOffGap;
    private final HomeSourceCard[] cards=new HomeSourceCard[3];private View connectionDot;
    private final AudioMode[] modes={AudioMode.RADIO,AudioMode.CD,AudioMode.USB};private Button connect;
    private String cardState="";
    private final Runnable refresh=new Runnable(){public void run(){render();ui.postDelayed(this,300);}};
    @Override public void onCreate(Bundle b){super.onCreate(b);buildUi();startReading();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(0);return v;}
    private TextView text(String value,int size,int color){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setFontFeatureSettings("tnum");return v;}
    private GradientDrawable shape(int color,int stroke){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(22));if(stroke!=0)bg.setStroke(dp(2),stroke);return bg;}
    private int blend(int background,int foreground,float amount){int r=Math.round(android.graphics.Color.red(background)*(1-amount)+android.graphics.Color.red(foreground)*amount),g=Math.round(android.graphics.Color.green(background)*(1-amount)+android.graphics.Color.green(foreground)*amount),b=Math.round(android.graphics.Color.blue(background)*(1-amount)+android.graphics.Color.blue(foreground)*amount);return android.graphics.Color.rgb(r,g,b);}
    private View gap(int width,int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(dp(width),dp(height)));return v;}
    private Button button(String label){return ConsoleControls.button(this,label,palette,false);}
    private void buildUi(){languageRevision=I18n.revision();palette=ThemeSettings.resolve(this).palette;cardState="";setTitle("Honda Box");int homeBackground=palette.bg;getWindow().setStatusBarColor(homeBackground);getWindow().setNavigationBarColor(homeBackground);getWindow().getDecorView().setSystemUiVisibility(palette.night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        boolean wide=getResources().getConfiguration().screenWidthDp>=600;boolean shortScreen=getResources().getConfiguration().screenHeightDp<380;
        LinearLayout root=column();root.setPadding(dp(wide?32:20),dp(20),dp(wide?32:20),dp(14));root.setBackgroundColor(homeBackground);root.setClipChildren(false);root.setClipToPadding(false);
        if(wide)setContentView(root);else{ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(root);setContentView(scroll);}
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);LinearLayout title=column();TextView brand=text("Honda Box",shortScreen?25:30,palette.text);brand.setLetterSpacing(-.025f);brand.setTypeface(Typeface.create("sans-serif-medium",0));title.addView(brand);TextView heading=text(I18n.t(R.string.choose_source),13,palette.muted);heading.setPadding(0,dp(3),0,0);title.addView(heading);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        source=text("",13,palette.text);source.setSingleLine(true);source.setEllipsize(android.text.TextUtils.TruncateAt.END);source.setMaxWidth(dp(260));source.setPadding(dp(16),dp(12),dp(16),dp(12));source.setBackground(shape(palette.card,0));
        LinearLayout sourceChips=row();sourceChips.setGravity(Gravity.CENTER_VERTICAL);sourceChips.addView(source,new LinearLayout.LayoutParams(-2,dp(44)));
        audioOffGap=gap(10,1);audioOffGap.setVisibility(View.GONE);sourceChips.addView(audioOffGap);
        audioOffChip=text(I18n.t(R.string.ui_off).toUpperCase(java.util.Locale.ROOT),13,palette.text);audioOffChip.setSingleLine(true);audioOffChip.setGravity(Gravity.CENTER);audioOffChip.setPadding(dp(16),dp(12),dp(16),dp(12));audioOffChip.setBackground(shape(palette.card,0));audioOffChip.setContentDescription(I18n.t(R.string.source_turned_off));audioOffChip.setVisibility(View.GONE);sourceChips.addView(audioOffChip,new LinearLayout.LayoutParams(-2,dp(44)));
        if(wide){header.addView(sourceChips,new LinearLayout.LayoutParams(-2,dp(44)));header.addView(gap(12,1));}
        Button settings=button(I18n.t(R.string.ui_settings));settings.setId(R.id.home_settings);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(settings,new LinearLayout.LayoutParams(-2,dp(48)));root.addView(header);if(!wide){root.addView(gap(1,12));root.addView(sourceChips,new LinearLayout.LayoutParams(-2,dp(44)));}root.addView(gap(1,24));
        LinearLayout choices=wide?row():column();int[] ids={R.id.home_radio,R.id.home_cd,R.id.home_usb};String[] labels={"Radio","CD","USB / iPod"};
        for(int i=0;i<3;i++){final AudioMode mode=modes[i];HomeSourceCard card=new HomeSourceCard(this,i,labels[i]);cards[i]=card;card.setId(ids[i]);card.setContentDescription(I18n.t(R.string.home_select)+" · "+labels[i]);card.setOnClickListener(v->select(mode));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(wide?0:-1,wide?-1:dp(230),wide?1:0);if(i<2){if(wide)lp.rightMargin=dp(20);else lp.bottomMargin=dp(12);}choices.addView(card,lp);}
        root.addView(choices,new LinearLayout.LayoutParams(-1,wide?0:-2,wide?1:0));root.addView(gap(1,22));
        LinearLayout footer=row();footer.setGravity(Gravity.CENTER_VERTICAL);connectionDot=new View(this);footer.addView(connectionDot,new LinearLayout.LayoutParams(dp(6),dp(6)));footer.addView(gap(8,1));status=text("",12,palette.muted);status.setSingleLine(true);status.setEllipsize(android.text.TextUtils.TruncateAt.END);footer.addView(status,new LinearLayout.LayoutParams(0,-2,1));
        Button carInfo=button(I18n.t(R.string.car_info_button));carInfo.setId(R.id.home_car_info);carInfo.setOnClickListener(v->startActivity(new Intent(this,CarInfoActivity.class)));footer.addView(carInfo,new LinearLayout.LayoutParams(-2,dp(48)));connect=button(I18n.t(R.string.ui_connect));connect.setOnClickListener(v->startReading());LinearLayout.LayoutParams connectLayout=new LinearLayout.LayoutParams(-2,dp(48));connectLayout.leftMargin=dp(10);footer.addView(connect,connectLayout);root.addView(footer);
    }
    private void startReading(){try{BackgroundSettings.open(this);}catch(RuntimeException e){ProbeState.INSTANCE.status(I18n.t(R.string.ui_could_not_start),e.toString(),false);}}

    private void select(AudioMode mode){ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();if(!s.running||!s.connected||s.commandBusy||!s.accord())return;
        try{startService(new Intent(this,ProbeService.class).setAction(ProbeService.COMMAND).putExtra("command",mode.select.name()));startActivity(AudioPlayerActivity.intent(this,mode,false));}catch(RuntimeException e){ProbeState.INSTANCE.commandRejected(I18n.t(R.string.ui_could_not_send_command)+e.getMessage());}
    }
    private void render(){if(languageRevision!=I18n.revision()||palette!=ThemeSettings.resolve(this).palette)buildUi();ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();boolean ready=s.running&&s.connected&&!s.commandBusy&&s.accord();AudioMode active=AudioMode.reported(s);String key=ready+":"+(active==null?"":active.name())+":"+s.source()+":"+s.mediaDevice;
        if(!key.equals(cardState)){cardState=key;for(int i=0;i<cards.length;i++){boolean selected=modes[i]==active;cards[i].setEnabled(ready);cards[i].setAlpha(ready?1f:.6f);String info=i==0?(selected?s.source():"AM / FM"):i==1?I18n.t(R.string.home_cd_value):(selected?(s.mediaDevice==ProbeState.MediaDevice.IPOD?"iPod":"USB"):I18n.t(R.string.home_usb_value));cards[i].illustration(i==2&&selected&&s.mediaDevice==ProbeState.MediaDevice.IPOD?3:i);cards[i].bind(palette,selected,info);}}
        boolean audioOff=s.connected&&s.accord()&&Boolean.FALSE.equals(s.bsjAudioEnabled);
        String currentSource=I18n.t(R.string.home_current)+" · "+s.source();
        source.setText(currentSource);source.setContentDescription(currentSource);audioOffChip.setVisibility(audioOff?View.VISIBLE:View.GONE);audioOffGap.setVisibility(audioOff?View.VISIBLE:View.GONE);connectionDot.setBackground(shape(s.connected?palette.accent:palette.warning,0));status.setText(s.connected&&!s.accord()?I18n.t(R.string.audio_unavailable):I18n.stored(s.status));status.setTextColor(s.connected?palette.muted:palette.warning);connect.setVisibility(s.running?View.GONE:View.VISIBLE);
    }
    @Override protected void onStart(){super.onStart();ui.removeCallbacks(refresh);ui.post(refresh);}
    @Override protected void onStop(){ui.removeCallbacks(refresh);super.onStop();}
}
