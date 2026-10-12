package com.crewbeat.hondabox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.graphics.*;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.Locale;

/** Radio/CD and a shared USB/iPod Activity. Legacy iPod launches also use automatic device detection. */
public final class AudioPlayerActivity extends Activity {
    private static WeakReference<AudioPlayerActivity> current=new WeakReference<>(null);private static boolean visible;
    private final Handler ui=new Handler(Looper.getMainLooper());private AudioMode mode;private ProbeState.MediaDevice shownDevice=ProbeState.MediaDevice.UNKNOWN;private int languageRevision=-1;private ThemeSettings.Palette palette;
    private TextView titleField,heading,sourceInfo,primary,secondary,details,elapsed,volume;private TextView[] presets;private View[] presetCards;
    private LinearLayout titleRow;private ImageButton titleInfo,playerInfo;private AlertDialog titleHelp,playerHelp;
    private TextView select;private Button previous,next;private GradientDrawable sourceIndicator;private long session=-1,sequence=-1,deviceRevision=-1;
    private final Runnable refresh=new Runnable(){public void run(){render();if(!isFinishing())ui.postDelayed(this,200);}};
    public static boolean isVisible(){return visible;}
    public static void hideForAux(){AudioPlayerActivity a=current.get();if(a!=null&&!a.isFinishing()&&SourceSettings.hideOnAux(a)){ProbeState.INSTANCE.log("AUDIO PLAYER: hidden for owner-calibrated AUX source");a.finishAndRemoveTask();}}
    public static Intent intent(Context c,AudioMode m,boolean automatic){return new Intent(c,AudioPlayerActivity.class).putExtra("mode",m.name()).putExtra("automatic",automatic).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);}
    private AudioMode requested(Intent i){String name=i.getStringExtra("mode");if(name!=null)try{return normalize(AudioMode.valueOf(name));}catch(IllegalArgumentException ignored){}
        String alias=i.getComponent()==null?"":i.getComponent().getClassName();return alias.endsWith(".Radio")?AudioMode.RADIO:alias.endsWith(".USB")?normalize(AudioMode.USB):alias.endsWith(".IPod")?normalize(AudioMode.IPOD):AudioMode.CD;}
    private String selectionPrompt(){return mode==AudioMode.RADIO?I18n.t(R.string.player_select_radio):I18n.t(R.string.player_select)+screenTitle();}
    private AudioMode normalize(AudioMode requested){return requested==AudioMode.IPOD?AudioMode.USB:requested;}
    private String screenTitle(){return mode==AudioMode.USB?(shownDevice==ProbeState.MediaDevice.IPOD?"iPod":"USB"):mode.title;}
    @Override protected void onCreate(Bundle b){super.onCreate(b);current=new WeakReference<>(this);mode=requested(getIntent());if(b!=null)mode=normalize(AudioMode.valueOf(b.getString("mode",mode.name())));
        ProbeState.INSTANCE.manualMediaDeviceHint(ProbeState.MediaDevice.UNKNOWN);
        ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();session=s.sessionId;sequence=s.sourceSequence;deviceRevision=s.mediaDeviceRevision;shownDevice=s.mediaDevice;buildUi();
        if(!s.running)try{Intent start=new Intent(this,ProbeService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(start);else startService(start);}catch(RuntimeException e){ProbeState.INSTANCE.log("PLAYER SERVICE ERROR: "+e);}
    }
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);mode=requested(i);ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();session=s.sessionId;sequence=s.sourceSequence;deviceRevision=s.mediaDeviceRevision;shownDevice=s.mediaDevice;buildUi();render();}
    @Override protected void onSaveInstanceState(Bundle b){b.putString("mode",mode.name());super.onSaveInstanceState(b);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.HORIZONTAL);return v;}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(LinearLayout.VERTICAL);return v;}
    private View gap(int width,int height){View v=new View(this);v.setLayoutParams(new LinearLayout.LayoutParams(dp(width),dp(height)));return v;}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setFontFeatureSettings("tnum");t.setSingleLine(true);t.setEllipsize(android.text.TextUtils.TruncateAt.END);return t;}
    private GradientDrawable shape(int color,int radius){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(dp(radius));return bg;}
    private GradientDrawable illustrationBackground(){
        int tint=palette.accent;
        float amount=palette.night?.12f:.09f;
        int edge=Color.rgb(Math.round(Color.red(palette.card)*(1-amount)+Color.red(tint)*amount),Math.round(Color.green(palette.card)*(1-amount)+Color.green(tint)*amount),Math.round(Color.blue(palette.card)*(1-amount)+Color.blue(tint)*amount));
        GradientDrawable background=new GradientDrawable(GradientDrawable.Orientation.BL_TR,new int[]{palette.card,palette.card,edge});background.setCornerRadius(dp(12));return background;
    }
    private Button button(String label){return ConsoleControls.button(this,label,palette,false);}
    private ImageButton infoButton(String tag,int description,View.OnClickListener click){ImageButton b=new ImageButton(this);b.setTag(tag);b.setImageResource(R.drawable.ic_title_info);b.setColorFilter(palette.accent);b.setScaleType(ImageView.ScaleType.FIT_CENTER);b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setBackground(new RippleDrawable(ColorStateList.valueOf((palette.accent&0xffffff)|0x20000000),null,null));b.setContentDescription(I18n.t(description));b.setOnClickListener(click);return b;}
    private void dismissHelp(){if(titleHelp!=null){titleHelp.dismiss();titleHelp=null;}if(playerHelp!=null){playerHelp.dismiss();playerHelp=null;}}
    private AlertDialog showHelp(String title,String message,int width){
        AlertDialog dialog=new AlertDialog.Builder(this,palette.night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setTitle(title).setMessage(message).setPositiveButton(I18n.t(R.string.player_title_help_close),null).create();
        dialog.show();dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(palette.accent);
        TextView text=dialog.findViewById(android.R.id.message);if(text!=null)text.setTextColor(palette.text);
        Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(shape(palette.card,20));window.setLayout(Math.min(dp(width),getResources().getDisplayMetrics().widthPixels-dp(48)),WindowManager.LayoutParams.WRAP_CONTENT);}
        return dialog;
    }
    private void showTitleHelp(){
        if(mode==AudioMode.RADIO)return;
        dismissHelp();titleHelp=showHelp(screenTitle()+" · TITLE",I18n.t(mode==AudioMode.CD?R.string.player_title_help_cd:shownDevice==ProbeState.MediaDevice.IPOD?R.string.player_title_help_ipod:R.string.player_title_help_usb),560);
    }
    private void showPlayerHelp(){
        int source=mode==AudioMode.RADIO?R.string.player_controls_radio:mode==AudioMode.CD?R.string.player_controls_cd:shownDevice==ProbeState.MediaDevice.IPOD?R.string.player_controls_ipod:R.string.player_controls_usb;
        String message=I18n.t(R.string.player_controls_source)+"\n\n"+I18n.t(source)+"\n\n"+I18n.t(R.string.player_controls_settings);
        dismissHelp();playerHelp=showHelp(screenTitle()+" · "+I18n.t(R.string.player_controls_help_title),message,640);
    }
    private void send(AudioCommand command){ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();if(command==null||!s.running||!s.connected||s.commandBusy||!Integer.valueOf(6291908).equals(s.number(1000)))return;
        if(!command.selectsSource()&&(AudioMode.reported(s)!=mode||s.audioSource()==null||s.audioSource()<1))return;
        startService(new Intent(this,ProbeService.class).setAction(ProbeService.COMMAND).putExtra("command",command.name()));}
    private void buildUi(){dismissHelp();languageRevision=I18n.revision();palette=ThemeSettings.resolve(this).palette;setTitle(screenTitle());getWindow().setStatusBarColor(palette.bg);getWindow().setNavigationBarColor(palette.bg);
        getWindow().getDecorView().setSystemUiVisibility(palette.night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        boolean wide=getResources().getConfiguration().screenWidthDp>=650;
        LinearLayout root=column();root.setPadding(dp(28),dp(18),dp(28),dp(14));root.setBackgroundColor(palette.bg);setContentView(root);
        LinearLayout header=row();header.setGravity(Gravity.CENTER_VERTICAL);heading=text(screenTitle(),28,palette.text);heading.setTypeface(Typeface.create("sans-serif-medium",0));heading.setContentDescription(I18n.t(R.string.ui_audio_player)+screenTitle());header.addView(heading,new LinearLayout.LayoutParams(0,dp(48),1));
        volume=text(I18n.t(R.string.ui_volume_112),14,palette.muted);volume.setGravity(Gravity.CENTER_VERTICAL);
        android.graphics.drawable.Drawable volumeIcon=getDrawable(R.drawable.ic_volume_speaker).mutate();volumeIcon.setTint(palette.accent);volumeIcon.setBounds(0,0,dp(24),dp(24));volume.setCompoundDrawablesRelative(volumeIcon,null,null,null);volume.setCompoundDrawablePadding(dp(8));header.addView(volume);header.addView(gap(18,1));
        Button settings=button(I18n.t(R.string.ui_settings));settings.setTextSize(13);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));header.addView(settings,new LinearLayout.LayoutParams(-2,dp(48)));root.addView(header);root.addView(gap(1,12));
        LinearLayout body=wide?row():column();body.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout illustration=column();illustration.setPadding(dp(18),dp(12),dp(18),dp(14));illustration.setBackground(illustrationBackground());MediaIllustration art=new MediaIllustration(this,mode==AudioMode.RADIO?0:mode==AudioMode.CD?1:shownDevice==ProbeState.MediaDevice.IPOD?3:2,false);art.night(palette.night);illustration.addView(art,new LinearLayout.LayoutParams(-1,0,1));sourceInfo=text(I18n.t(R.string.ui_waiting_for_source),12,palette.muted);illustration.addView(sourceInfo);body.addView(illustration,new LinearLayout.LayoutParams(wide?dp(300):-1,wide?-1:dp(170)));if(wide)body.addView(gap(18,1));else body.addView(gap(1,12));
        FrameLayout informationPanel=new FrameLayout(this);informationPanel.setBackground(shape(palette.card,12));
        LinearLayout information=column();information.setPadding(dp(24),dp(18),dp(24),dp(18));
        LinearLayout metadata=column();metadata.setGravity(Gravity.CENTER_VERTICAL);metadata.setPadding(0,0,dp(32),0);
        titleRow=row();titleRow.setGravity(Gravity.CENTER_VERTICAL);titleRow.setVisibility(mode==AudioMode.RADIO?View.GONE:View.VISIBLE);
        titleField=text(mode==AudioMode.USB&&shownDevice==ProbeState.MediaDevice.IPOD?"TITLE":"TITLE"+waitingDots(),12,palette.accent);titleField.setIncludeFontPadding(false);titleField.setMaxWidth(dp(300));
        // Reserve all three dots so the adjacent info button stays still.
        if(!(mode==AudioMode.USB&&shownDevice==ProbeState.MediaDevice.IPOD))titleField.setMinWidth((int)Math.ceil(titleField.getPaint().measureText("TITLE...")));
        titleRow.addView(titleField,new LinearLayout.LayoutParams(-2,-2));
        titleInfo=infoButton("player-title-info",R.string.player_title_help_description,v->showTitleHelp());titleInfo.setVisibility(mode==AudioMode.RADIO?View.GONE:View.VISIBLE);titleRow.addView(titleInfo,new LinearLayout.LayoutParams(dp(44),dp(44)));metadata.addView(titleRow);metadata.addView(gap(1,6));
        primary=text(mode==AudioMode.RADIO?"—":waitingDots(),mode==AudioMode.RADIO?54:32,palette.text);primary.setIncludeFontPadding(false);if(mode!=AudioMode.RADIO){primary.setSingleLine(false);primary.setEllipsize(null);}primary.setTypeface(Typeface.create("sans-serif",0));metadata.addView(primary);secondary=text("—",16,palette.muted);secondary.setVisibility(View.GONE);metadata.addView(secondary);
        elapsed=text("—",24,palette.text);details=text("—",13,palette.muted);elapsed.setIncludeFontPadding(false);details.setIncludeFontPadding(false);details.setSingleLine(false);details.setEllipsize(null);presets=null;LinearLayout choiceGrid=column();
        if(mode==AudioMode.RADIO||mode==AudioMode.CD){if(mode==AudioMode.RADIO)elapsed.setVisibility(View.GONE);presets=new TextView[6];presetCards=new View[6];LinearLayout grid=choiceGrid;grid.addView(gap(1,10));for(int rowIndex=0;rowIndex<2;rowIndex++){LinearLayout stationRow=row();for(int column=0;column<3;column++){int i=rowIndex*3+column;TextView station=text(mode==AudioMode.CD?I18n.t(R.string.ui_disc)+(i+1):(i+1)+"  —",14,palette.muted);station.setSingleLine(false);station.setMaxLines(2);station.setGravity(Gravity.CENTER);FrameLayout tile=new FrameLayout(this);tile.setBackground(shape(mode==AudioMode.CD?palette.button:palette.bg,10));if(mode==AudioMode.CD){LinearLayout discRow=row();discRow.setGravity(Gravity.CENTER);discRow.addView(new DiscGlyph(this,tile),new LinearLayout.LayoutParams(dp(22),dp(22)));discRow.addView(gap(8,1));discRow.addView(station);tile.addView(discRow,new FrameLayout.LayoutParams(-1,-1));}else tile.addView(station,new FrameLayout.LayoutParams(-1,-1));presetCards[i]=tile;LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(46),1);lp.rightMargin=dp(column<2?8:0);stationRow.addView(tile,lp);presets[i]=station;}grid.addView(stationRow);if(rowIndex==0)grid.addView(gap(1,8));}}
        // Center title, timer and playback details as one group instead of spreading
        // them between the middle and bottom of the card. Long titles scroll with
        // their context, while the station/disc grid keeps its own space below.
        LinearLayout facts=column();facts.setGravity(Gravity.CENTER_VERTICAL);if(mode==AudioMode.RADIO)elapsed.setVisibility(View.GONE);facts.addView(elapsed,new LinearLayout.LayoutParams(-1,-2));if(mode!=AudioMode.RADIO)facts.addView(gap(1,4));facts.addView(details,new LinearLayout.LayoutParams(-1,-2));metadata.addView(gap(1,mode==AudioMode.RADIO?6:10));metadata.addView(facts,new LinearLayout.LayoutParams(-1,-2));
        ScrollView metadataScroll=new ScrollView(this);metadataScroll.setVerticalScrollBarEnabled(false);metadataScroll.setFillViewport(true);metadataScroll.setClipToPadding(false);metadataScroll.addView(metadata,new ScrollView.LayoutParams(-1,-2));information.addView(metadataScroll,new LinearLayout.LayoutParams(-1,0,1));if(mode==AudioMode.CD||mode==AudioMode.RADIO)information.addView(choiceGrid,new LinearLayout.LayoutParams(-1,-2));informationPanel.addView(information,new FrameLayout.LayoutParams(-1,-1));playerInfo=infoButton("player-controls-info",R.string.player_controls_help_description,v->showPlayerHelp());FrameLayout.LayoutParams helpPosition=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.END);helpPosition.topMargin=dp(8);helpPosition.rightMargin=dp(8);informationPanel.addView(playerInfo,helpPosition);body.addView(informationPanel,new LinearLayout.LayoutParams(wide?0:-1,wide?-1:dp(mode==AudioMode.USB?250:340),wide?1:0));
        if(wide)root.addView(body,new LinearLayout.LayoutParams(-1,0,1));else{ScrollView bodyScroll=new ScrollView(this);bodyScroll.addView(body);root.addView(bodyScroll,new LinearLayout.LayoutParams(-1,0,1));}root.addView(gap(1,16));
        LinearLayout controls=row();controls.setBaselineAligned(false);controls.setGravity(Gravity.CENTER_VERTICAL);
        select=text(selectionPrompt(),12,palette.muted);select.setSingleLine(false);select.setMaxLines(2);select.setGravity(Gravity.CENTER_VERTICAL|Gravity.START);
        sourceIndicator=new GradientDrawable();sourceIndicator.setShape(GradientDrawable.OVAL);sourceIndicator.setColor(palette.muted);sourceIndicator.setBounds(0,0,dp(6),dp(6));select.setCompoundDrawablesRelative(sourceIndicator,null,null,null);select.setCompoundDrawablePadding(dp(8));select.setOnClickListener(v->{if(AudioMode.reported(ProbeState.INSTANCE.overlaySnapshot())!=mode)send(mode.select);});
        previous=button(mode==AudioMode.RADIO?I18n.t(R.string.ui_previous):I18n.t(R.string.ui_previous_126));next=button(mode==AudioMode.RADIO?I18n.t(R.string.ui_next):I18n.t(R.string.ui_next_119));previous.setOnClickListener(v->send(AudioCommand.PREVIOUS));next.setOnClickListener(v->send(AudioCommand.NEXT));
        FrameLayout climateSpace=new FrameLayout(this);climateSpace.setTag("player-climate-space");climateSpace.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        // A quiet source label, clear center space and smaller transport controls.
        int sourceWidth=wide?Math.max(0,Math.min(dp(300),getResources().getDisplayMetrics().widthPixels-dp(400))):0;
        controls.addView(select,new LinearLayout.LayoutParams(sourceWidth,dp(48),wide?0:1));
        controls.addView(climateSpace,new LinearLayout.LayoutParams(0,dp(48),wide?1:0));
        controls.addView(gap(14,1));controls.addView(previous,new LinearLayout.LayoutParams(dp(wide?160:125),dp(48)));controls.addView(gap(10,1));controls.addView(next,new LinearLayout.LayoutParams(dp(wide?160:125),dp(48)));root.addView(controls);
    }
    private String frequency(ProbeState.Snapshot s,int code){return s.fresh(code)?RadioFrequency.value(s.displayNumber(115),s.displayNumber(code),Integer.valueOf(6291908).equals(s.number(1000))):"—";}
    private String presetFrequency(ProbeState.Snapshot s,int slot){return RadioFrequency.value(s.displayNumber(115),s.preset(slot),Integer.valueOf(6291908).equals(s.number(1000)));}
    // Reuse the visible-player refresh; no additional timer or background work.
    private String waitingDots(){int phase=(int)((SystemClock.uptimeMillis()/600)%3);return phase==0?".":phase==1?"..":"...";}
    private String loadingText(){return I18n.t(R.string.media_loading)+waitingDots();}
    private void value(TextView view,CharSequence next){if(!view.getText().toString().contentEquals(next))view.setText(next);}
    private String frequencyUnit(Integer band){String unit=RadioFrequency.unit(band);return unit.equals("kHz")?I18n.t(R.string.radio_khz):unit.equals("MHz")?I18n.t(R.string.radio_mhz):"";}
    private CharSequence tunedFrequency(ProbeState.Snapshot s){String f=frequency(s,119),unit=frequencyUnit(s.displayNumber(115));if(f.equals("—")||unit.isEmpty())return f;SpannableString label=new SpannableString(f+" "+unit);label.setSpan(new RelativeSizeSpan(.38f),f.length()+1,label.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);return label;}
    private String number(ProbeState.Snapshot s,int code){String raw=s.cdNumber(code);if((code==101&&s.usingStandbyTrack())||s.bsjValues.containsKey(code)||mode!=AudioMode.USB||raw.equals("—"))return raw;Integer n=s.number(code);return n==null||n>255||(n&15)>9||(n>>4)>9?"—":String.valueOf((n>>4)*10+(n&15));}
    private Integer bcd(Integer n){return n==null||n<0||n>255||(n&15)>9||(n>>4)>9?null:(n>>4)*10+(n&15);}
    private String time(ProbeState.Snapshot s){if(s.usingStandbyTime()||mode==AudioMode.CD||s.bsjValues.containsKey(99))return s.cdTime();if(!s.fresh(99)||!s.fresh(100)||number(s,101).equals("—"))return "—";Integer min=bcd(s.number(99)),sec=bcd(s.number(100));return min==null||sec==null||sec>59?"—":String.format(Locale.US,"%02d:%02d",min,sec);}
    private String radioStatus(ProbeState.Snapshot s){Integer st=s.displayNumber(118),status=s.displayNumber(117);String stereo=st==null?"Stereo —":st==0?"Stereo Off":st==1?"Stereo ON":"Stereo —";String[] names={"—","Normal","TUNE+","TUNE−","SEEK+","SEEK−","SCAN","Auto Search"};return stereo+" · "+(status!=null&&status>=1&&status<=7?names[status]:"—");}
    private String metadata(ProbeState.Snapshot s,int code){return s.fresh(code)?s.text(code):"—";}
    private void render(){ProbeState.Snapshot s=ProbeState.INSTANCE.overlaySnapshot();if(AudioMode.aux(s)&&SourceSettings.hideOnAux(this)){hideForAux();return;}
        if(session!=s.sessionId){session=s.sessionId;sequence=s.sourceSequence;}
        else if(sequence!=s.sourceSequence||deviceRevision!=s.mediaDeviceRevision){deviceRevision=s.mediaDeviceRevision;boolean baseline=sequence==0;sequence=s.sourceSequence;AudioMode reported=AudioMode.reported(s);if((!baseline||s.bsjSource!=null)&&reported!=null&&reported!=mode){mode=reported;buildUi();}}
        ProbeState.MediaDevice displayDevice=s.mediaDevice;if(shownDevice!=displayDevice){shownDevice=displayDevice;buildUi();}if(languageRevision!=I18n.revision()||palette!=ThemeSettings.resolve(this).palette)buildUi();AudioMode actual=AudioMode.reported(s);boolean active=actual==mode,valid=s.running&&s.connected&&Integer.valueOf(6291908).equals(s.number(1000));
        sourceInfo.setText(!s.connected?I18n.t(R.string.ui_waiting_for_can_connection_117):active?I18n.t(R.string.ui_vehicle_source)+s.source():I18n.t(R.string.ui_current_source)+s.source());
        volume.setText(I18n.t(R.string.ui_volume_111)+s.volume());select.setEnabled(!active&&valid&&!s.commandBusy&&mode.select!=null);select.setSelected(active);sourceIndicator.setColor(active?palette.accent:palette.muted);
        String selection=active?screenTitle()+I18n.t(R.string.player_selected):selectionPrompt();
        String controlsNote=I18n.t(R.string.player_vehicle_controls);SpannableString selectionLabel=new SpannableString(active?selection:selection+"\n"+controlsNote);if(!active)selectionLabel.setSpan(new RelativeSizeSpan(.78f),selection.length()+1,selectionLabel.length(),Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);select.setText(selectionLabel);select.setContentDescription(active?selection:selection+". "+I18n.t(R.string.player_vehicle_controls));boolean knownTransport=s.audioSource()!=null&&s.audioSource()>0;previous.setEnabled(valid&&active&&knownTransport&&!s.commandBusy);next.setEnabled(valid&&active&&knownTransport&&!s.commandBusy);
        titleRow.setVisibility(mode==AudioMode.RADIO?View.GONE:View.VISIBLE);
        if(mode==AudioMode.RADIO){sourceInfo.setVisibility(active?View.GONE:View.VISIBLE);String bank=s.radioBand(),waveband=bank.equals("AM1")?" · MW":bank.equals("AM2")?" · LW":"";String title=screenTitle()+(active&&s.fresh(115)&&!bank.equals("—")?" · "+bank+waveband:"");if(!heading.getText().toString().equals(title)){heading.setText(title);heading.setContentDescription(I18n.t(R.string.ui_audio_player)+title);setTitle(title);}secondary.setVisibility(View.GONE);value(primary,active?tunedFrequency(s):"—");Integer band=s.displayNumber(115),selected=s.fresh(116)?s.displayNumber(116):null;secondary.setText(active?s.radioBand():I18n.t(R.string.ui_frequency_not_received));details.setText(active?radioStatus(s):"—");for(int i=0;i<6;i++){String f=active?presetFrequency(s,i+1):"—";boolean chosen=active&&selected!=null&&selected==i+1;value(presets[i],(i+1)+"  "+f+(f.equals("—")?"":" "+frequencyUnit(band)));highlightTile(i,chosen);presets[i].scrollTo(0,0);}}

        else{boolean readable=valid&&(active||(mode==AudioMode.USB&&actual==AudioMode.CD)||s.audioSource()==null||Integer.valueOf(0).equals(s.audioSource()));String title=readable?s.combinedMediaText():"—",track=readable?number(s,101):"—";
            boolean ipod=mode==AudioMode.USB&&shownDevice==ProbeState.MediaDevice.IPOD;
            String shownTitle=title;boolean trackNumberShown=false;
            if(title.equals("—")||title.trim().isEmpty()){
                if(readable&&!ipod&&!track.equals("—")){trackNumberShown=s.cdTrackIntro(System.nanoTime());shownTitle=trackNumberShown?I18n.t(R.string.ui_track)+track:loadingText();}
                else if(readable&&s.mediaLoading())shownTitle=loadingText();
                else shownTitle=waitingDots();
            }
            value(primary,shownTitle);
            Integer field=readable?s.currentTitleField():null;
            // OEM CD fields 0/1/3 and external-media fields 6/7/8 share labels.
            if(ipod)value(titleField,"TITLE");
            else if(trackNumberShown)value(titleField,"TITLE · "+I18n.t(R.string.media_track_number));
            else if(readable&&s.usingStandbyText())value(titleField,"TITLE · "+I18n.t(R.string.standby_display_field));
            else if(field!=null){int label;switch(field){case 3:case 8:label=R.string.usb_title_artist;break;case 0:case 6:label=R.string.usb_title_album;break;case 1:case 7:label=R.string.usb_title_track;break;case 4:label=R.string.usb_title_folder;break;case 5:label=R.string.usb_title_filename;break;default:label=R.string.usb_title_unknown;}
                value(titleField,"TITLE · "+I18n.t(label)+(label==R.string.usb_title_unknown?" "+field:""));}
            else value(titleField,"TITLE"+waitingDots());
            secondary.setVisibility(View.GONE);elapsed.setText(readable?(ipod?I18n.t(R.string.media_playing):time(s)):"—");
            String context=ipod?"":mode==AudioMode.CD?I18n.t(R.string.ui_disc)+(readable?s.cdNumber(113):"—")+" · "+I18n.t(R.string.ui_track)+track:I18n.t(R.string.ui_track)+track;
            // TYPE07 kind4 carries generic OEM display text, not a verified folder field.
            if(readable&&!s.playMode().isEmpty())context+=(context.isEmpty()?"":" · ")+s.playMode();details.setText(context);details.setVisibility(context.isEmpty()?View.GONE:View.VISIBLE);if(mode==AudioMode.CD){Integer disc=readable&&s.fresh(113)?s.displayNumber(113):null;for(int i=0;i<6;i++)highlightTile(i,readable&&disc!=null&&disc==i+1);}}
        ProbeState.INSTANCE.audioPlayer(true,sourceInfo.getText().toString(),mode.name());
    }
    private void highlightTile(int i,boolean chosen){if(!Boolean.valueOf(chosen).equals(presets[i].getTag())){presets[i].setSelected(chosen);presetCards[i].setSelected(chosen);presets[i].setTextColor(chosen?palette.accentText:palette.muted);presetCards[i].setBackground(shape(chosen?palette.accent:mode==AudioMode.CD?palette.button:palette.bg,10));presets[i].setTag(chosen);presetCards[i].invalidate();if(presetCards[i] instanceof ViewGroup){View row=((ViewGroup)presetCards[i]).getChildAt(0);if(row instanceof ViewGroup)((ViewGroup)row).getChildAt(0).invalidate();}}}
    private final class DiscGlyph extends View{
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final View tile;
        DiscGlyph(Context c,View tile){super(c);this.tile=tile;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas c){float side=Math.min(getWidth(),getHeight());c.save();c.translate((getWidth()-side)/2,(getHeight()-side)/2);c.scale(side/24f,side/24f);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.4f);paint.setColor(tile.isSelected()?palette.accentText:palette.muted);c.drawCircle(12,12,10,paint);c.drawCircle(12,12,2.6f,paint);c.drawArc(new RectF(5,5,19,19),210,35,false,paint);c.drawArc(new RectF(5,5,19,19),30,35,false,paint);c.restore();}
    }
    @Override protected void onStart(){super.onStart();visible=true;ui.removeCallbacks(refresh);ui.post(refresh);}
    @Override protected void onStop(){visible=false;ui.removeCallbacks(refresh);ProbeState.INSTANCE.audioPlayer(false,I18n.t(R.string.ui_audio_player_hidden),mode.name());super.onStop();}
    @Override protected void onDestroy(){dismissHelp();if(current.get()==this)current.clear();super.onDestroy();}
}
