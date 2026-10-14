package com.crewbeat.hondabox;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Shared styling and lifecycle for the settings and diagnostic screens. */
public abstract class SettingsScreenActivity extends Activity {
    protected abstract boolean debugScreen();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private int BG, CARD, TEXT, MUTED, ACCENT;
    private int languageRevision=-1; private ThemeSettings.Palette palette; private ThemeController themes; private ScrollView scroll;
    private TextView status, detail, source, rawSource, volume, left, right, fan, climateFlags, radio, radioTextInfo, media, fields, commandStatus, profile, stats, menu, backend, log, audioRouteInfo, standbyInfo, vehiclePacket0EInfo, vehiclePacket10Info, steeringInfo;
    private final java.util.ArrayList<Button> commandButtons = new java.util.ArrayList<>();
    private Button connect, stop;
    private Spinner audioTransport,audioRoute,audioHold;
    private TextView overlayInfo, rawInfo, permissionInfo; private CheckBox overlayEnabled, overlayPinned, sourceOverlayEnabled, climateOverlayEnabled;private TextView climateOverlayInfo,carMenuOverlayInfo;
    private BackgroundSetupGuide setupGuide;
    private JsonExport jsonExport; private StockOverlaySettings stockOverlays;
    private final Runnable refresh = new Runnable() { @Override public void run() { render(); ui.postDelayed(this, 500); } };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);ProbeState.INSTANCE.auxName(SourceSettings.auxName(this)); stockOverlays=new StockOverlaySettings(this); jsonExport=new JsonExport(this);themes=new ThemeController(this,()->{buildUi();render();}); buildUi();
    }
    private void buildUi() {
        if(setupGuide!=null){setupGuide.close();setupGuide=null;}
        languageRevision=I18n.revision(); int previousScroll=scroll==null?0:scroll.getScrollY(); boolean expanded=fields!=null&&fields.getVisibility()==View.VISIBLE;
        commandButtons.clear(); palette=ThemeSettings.resolve(this).palette; BG=palette.bg;CARD=palette.card;TEXT=palette.text;MUTED=palette.muted;ACCENT=palette.accent;
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(palette.night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.setFillViewport(true);
        LinearLayout root = column(); root.setPadding(dp(28),dp(22),dp(28),dp(24)); scroll.addView(root); setContentView(scroll);
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout title = column(); title.addView(label(debugScreen()?"HONDA  /  DEBUG":"HONDA  /  SETTINGS", 11, ACCENT));
        title.addView(label(I18n.t(debugScreen()?R.string.debug_title:R.string.ui_settings), 27, TEXT)); header.addView(title, new LinearLayout.LayoutParams(0,-2,1));
        TextView version = label("BOX  0.8.33", 12, MUTED); header.addView(version);header.addView(spaceX(16));Button back=button(I18n.t(R.string.ui_back),false);back.setTag("settings-back");back.setOnClickListener(v->finish());header.addView(back,new LinearLayout.LayoutParams(-2,dp(48)));root.addView(header);
        root.addView(space(18));
        if(!debugScreen()){
            LinearLayout permissions=card();permissions.addView(label(I18n.t(R.string.settings_overlay_permission_title),14,ACCENT));
            permissionInfo=label("",12,MUTED);permissions.addView(space(8));permissions.addView(permissionInfo);
        Button permission=button(I18n.t(R.string.ui_allow_display_over_other_apps),false);
        permission.setOnClickListener(v->{ try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))); }
            catch(ActivityNotFoundException e) { Toast.makeText(this,I18n.t(R.string.ui_open_android_settings_apps_fyt),Toast.LENGTH_LONG).show(); } });
            permissions.addView(permission);permissions.addView(space(8));
            permissions.addView(label(I18n.t(R.string.settings_overlay_permission_notes),12,MUTED));
            LinearLayout general=card();general.addView(label(I18n.t(R.string.settings_general),14,ACCENT));general.addView(space(10));
            general.addView(I18n.controls(this,()->{buildUi();render();}));general.addView(space(16));
            general.addView(label(I18n.t(R.string.settings_appearance),12,MUTED));general.addView(space(6));general.addView(themes.controls());
            root.addView(general);root.addView(space(16));
            root.addView(permissions);root.addView(space(16));
            LinearLayout background=card();background.addView(BackgroundSettings.controls(this));background.addView(space(12));setupGuide=new BackgroundSetupGuide(this);background.addView(setupGuide.controls());root.addView(background);root.addView(space(16));
        }
        if(debugScreen()){
        root.addView(label(I18n.t(R.string.debug_help),12,MUTED));root.addView(space(10));
        LinearLayout experiments=card();experiments.addView(label(I18n.t(R.string.debug_overlay_experiments),14,ACCENT));
        CheckBox extendClimate=new CheckBox(this);extendClimate.setTag("debug-climate-navigation");extendClimate.setText(I18n.t(R.string.debug_climate_extend_navigation));extendClimate.setTextColor(TEXT);extendClimate.setChecked(ClimateStripOverlay.extendToSystemNavigation(this));experiments.addView(extendClimate);
        extendClimate.setOnCheckedChangeListener((v,on)->ClimateStripOverlay.extendToSystemNavigation(this,on));
        experiments.addView(label(I18n.t(R.string.debug_climate_extend_navigation_notes),12,MUTED));root.addView(experiments);root.addView(space(14));
        CheckBox recording=new CheckBox(this);recording.setText(I18n.t(R.string.diagnostics_record_toggle));recording.setTextColor(TEXT);recording.setChecked(DiagnosticSettings.enabled(this));root.addView(recording);
        recording.setOnCheckedChangeListener((v,on)->{DiagnosticSettings.enabled(this,on);render();});root.addView(label(I18n.t(R.string.diagnostics_record_notes),12,MUTED));root.addView(space(10));
        Button analyzer=button("BSJ Experiment Analyzer",false);analyzer.setOnClickListener(v->startActivity(new Intent(this,AnalyzerActivity.class)));root.addView(analyzer);
        root.addView(space(14));
        LinearLayout banner = card(); status = label("",19,ACCENT); detail = label("",13,MUTED);
        banner.addView(status); banner.addView(space(5)); banner.addView(detail);
        LinearLayout buttons = row(); buttons.setPadding(0,dp(10),0,0);
        connect = button(I18n.t(R.string.ui_connect),true); connect.setOnClickListener(v -> startProbe());
        stop = button(I18n.t(R.string.ui_stop),false); stop.setOnClickListener(v -> BackgroundSettings.stop(this));
        Button export = button(I18n.t(R.string.ui_save_log),false); export.setOnClickListener(v -> export());
        buttons.addView(connect); buttons.addView(spaceX(8)); buttons.addView(stop);
        if (getResources().getConfiguration().screenWidthDp >= 480) { buttons.addView(spaceX(8)); buttons.addView(export); banner.addView(buttons); }
        else { banner.addView(buttons); banner.addView(space(8)); banner.addView(export); }
        banner.addView(space(8));Button resetLog=button(I18n.t(R.string.log_reset_button),false);resetLog.setOnClickListener(v->{ProbeState.INSTANCE.clearLog();render();Toast.makeText(this,I18n.t(R.string.log_reset_done),Toast.LENGTH_SHORT).show();});banner.addView(resetLog);
        CheckBox unknownPopup=new CheckBox(this);unknownPopup.setText(I18n.t(R.string.unknown_overlay_toggle));unknownPopup.setTextColor(TEXT);unknownPopup.setChecked(UnknownOverlay.enabled(this));banner.addView(unknownPopup);
        unknownPopup.setOnCheckedChangeListener((v,on)->{UnknownOverlay.enabled(this,on);overlayAction(false);});
        banner.addView(label(I18n.t(R.string.unknown_overlay_help),11,MUTED));
        root.addView(banner); root.addView(space(14));
        }
        if(!debugScreen()){
        LinearLayout overlayCard=card(); overlayCard.addView(label(I18n.t(R.string.settings_audio_overlays),14,ACCENT));
        overlayCard.addView(space(10));overlayCard.addView(label(I18n.t(R.string.ui_volume),12,MUTED));
        overlayEnabled=new CheckBox(this); overlayEnabled.setText(I18n.t(R.string.ui_show_on_volume_changes)); overlayEnabled.setTextColor(TEXT);
        overlayEnabled.setChecked(VolumeOverlay.enabled(this)); overlayCard.addView(overlayEnabled);
        overlayPinned=new CheckBox(this); overlayPinned.setText(I18n.t(R.string.settings_volume_pinned)); overlayPinned.setTextColor(TEXT);
        overlayPinned.setChecked(VolumeOverlay.pinned(this)); overlayCard.addView(overlayPinned);
        overlayCard.addView(space(14));overlayCard.addView(label(I18n.t(R.string.settings_track),12,MUTED));
        CheckBox trackPopup=new CheckBox(this);trackPopup.setText(I18n.t(R.string.track_popup));trackPopup.setTextColor(TEXT);trackPopup.setChecked(TrackOverlay.enabled(this));trackPopup.setOnCheckedChangeListener((v,on)->TrackOverlay.enabled(this,on));overlayCard.addView(trackPopup);
        Button trackDemo=button(I18n.t(R.string.track_demo_button),false);trackDemo.setOnClickListener(v->{
            if(!Settings.canDrawOverlays(this)){Toast.makeText(this,I18n.t(R.string.ui_overlay_permission_not_granted),Toast.LENGTH_LONG).show();return;}
            if(!ProbeState.INSTANCE.overlaySnapshot().running){Toast.makeText(this,I18n.t(R.string.ui_connect_first),Toast.LENGTH_SHORT).show();return;}
            startService(new Intent(this,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("trackDemo",true));
        });overlayCard.addView(trackDemo);
        overlayCard.addView(space(14));overlayCard.addView(label(I18n.t(R.string.ui_audio_source),12,MUTED));
        sourceOverlayEnabled=new CheckBox(this);sourceOverlayEnabled.setText(I18n.t(R.string.ui_show_source_on_change_3));sourceOverlayEnabled.setTextColor(TEXT);sourceOverlayEnabled.setChecked(SourceOverlay.enabled(this));overlayCard.addView(sourceOverlayEnabled);
        overlayCard.addView(label(I18n.t(R.string.source_popup_notes),12,MUTED));
        CheckBox sourceKeepOff=new CheckBox(this);sourceKeepOff.setText(I18n.t(R.string.source_keep_off));sourceKeepOff.setTextColor(TEXT);sourceKeepOff.setChecked(SourceOverlay.keepVisibleWhenOff(this));sourceKeepOff.setEnabled(sourceOverlayEnabled.isChecked());overlayCard.addView(sourceKeepOff);
        sourceKeepOff.setOnCheckedChangeListener((toggle,checked)->{SourceOverlay.keepVisibleWhenOff(this,checked);overlayAction(false);});
        overlayCard.addView(label(I18n.t(R.string.source_keep_off_notes),12,MUTED));
        sourceOverlayEnabled.setOnCheckedChangeListener((toggle,checked)->{SourceOverlay.enabled(this,checked);sourceKeepOff.setEnabled(checked);overlayAction(false);});
        overlayEnabled.setOnCheckedChangeListener((toggle,checked)->{ VolumeOverlay.options(this,checked,overlayPinned.isChecked()); overlayAction(false); });
        overlayPinned.setOnCheckedChangeListener((toggle,checked)->{ VolumeOverlay.options(this,overlayEnabled.isChecked(),checked); overlayAction(false); });
        Button demo=button(I18n.t(R.string.settings_volume_demo),false); demo.setOnClickListener(v->overlayAction(true));
        overlayCard.addView(demo,5);overlayCard.addView(space(14));

        overlayInfo=label("",12,MUTED);overlayCard.addView(space(8));overlayCard.addView(overlayInfo);
        overlayCard.addView(space(5));overlayCard.addView(label(I18n.t(R.string.ui_after_granting_permission_enable_the),11,MUTED));

        LinearLayout climateWindowCard=card();climateWindowCard.addView(label(I18n.t(R.string.settings_large_climate),14,ACCENT));
        climateOverlayEnabled=new CheckBox(this);climateOverlayEnabled.setText(I18n.t(R.string.ui_show_climate_on_value_changes));climateOverlayEnabled.setTextColor(TEXT);climateOverlayEnabled.setChecked(ClimateOverlay.enabled(this));climateWindowCard.addView(climateOverlayEnabled);
        climateOverlayEnabled.setOnCheckedChangeListener((toggle,checked)->{ClimateOverlay.enabled(this,checked);overlayAction(false);});
        climateWindowCard.addView(label(I18n.t(R.string.settings_popup_climate_notes),12,MUTED));
        Button climateDemo=button(I18n.t(R.string.settings_large_climate_demo),false);climateDemo.setOnClickListener(v->{
            if(!ProbeState.INSTANCE.snapshot().running){Toast.makeText(this,I18n.t(R.string.ui_connect_first),Toast.LENGTH_SHORT).show();return;}
            try{startService(new Intent(this,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("climateDemo",true));}catch(RuntimeException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
        });climateWindowCard.addView(climateDemo);climateWindowCard.addView(space(8));climateOverlayInfo=label("",12,MUTED);climateWindowCard.addView(climateOverlayInfo);
        LinearLayout compactClimateCard=card();compactClimateCard.addView(label(I18n.t(R.string.settings_compact_climate),14,ACCENT));
        CheckBox stripEnabled=new CheckBox(this);stripEnabled.setText(I18n.t(R.string.climate_strip_toggle));stripEnabled.setTextColor(TEXT);stripEnabled.setChecked(ClimateStripOverlay.enabled(this));compactClimateCard.addView(stripEnabled);
        compactClimateCard.addView(label(I18n.t(R.string.settings_compact_climate_notes),12,MUTED));
        CheckBox stripAutoHide=new CheckBox(this);stripAutoHide.setText(I18n.t(R.string.climate_strip_always_hide));stripAutoHide.setTextColor(TEXT);stripAutoHide.setChecked(ClimateStripOverlay.alwaysAutoHide(this));stripAutoHide.setEnabled(stripEnabled.isChecked());compactClimateCard.addView(stripAutoHide);stripAutoHide.setOnCheckedChangeListener((v,on)->{ClimateStripOverlay.alwaysAutoHide(this,on);overlayAction(false);});compactClimateCard.addView(label(I18n.t(R.string.climate_strip_always_hide_notes),12,MUTED));
        CheckBox stripButton=new CheckBox(this);stripButton.setText(I18n.t(R.string.climate_strip_button));stripButton.setTextColor(TEXT);stripButton.setChecked(ClimateStripOverlay.expandButton(this));stripButton.setEnabled(stripEnabled.isChecked());compactClimateCard.addView(stripButton);compactClimateCard.addView(label(I18n.t(R.string.climate_strip_button_notes),12,MUTED));
        CheckBox stripButtonHide=new CheckBox(this);stripButtonHide.setText(I18n.t(R.string.climate_strip_button_hide));stripButtonHide.setTextColor(TEXT);stripButtonHide.setChecked(ClimateStripOverlay.buttonAutoHide(this));stripButtonHide.setEnabled(stripEnabled.isChecked()&&stripButton.isChecked());compactClimateCard.addView(stripButtonHide);stripButtonHide.setOnCheckedChangeListener((v,on)->{ClimateStripOverlay.buttonAutoHide(this,on);overlayAction(false);});stripButton.setOnCheckedChangeListener((v,on)->{ClimateStripOverlay.expandButton(this,on);stripButtonHide.setEnabled(stripEnabled.isChecked()&&on);overlayAction(false);});compactClimateCard.addView(label(I18n.t(R.string.climate_strip_button_hide_notes),12,MUTED));
        stripEnabled.setOnCheckedChangeListener((v,on)->{ClimateStripOverlay.enabled(this,on);stripAutoHide.setEnabled(on);stripButton.setEnabled(on);stripButtonHide.setEnabled(on&&stripButton.isChecked());overlayAction(false);});
        Button stripDemo=button(I18n.t(R.string.settings_strip_demo),false);stripDemo.setOnClickListener(v->{
            if(!Settings.canDrawOverlays(this)){Toast.makeText(this,I18n.t(R.string.ui_overlay_permission_not_granted),Toast.LENGTH_LONG).show();return;}
            if(!ProbeState.INSTANCE.overlaySnapshot().running){Toast.makeText(this,I18n.t(R.string.ui_connect_first),Toast.LENGTH_SHORT).show();return;}
            startService(new Intent(this,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("climateStripDemo",true));
        });compactClimateCard.addView(stripDemo);
        compactClimateCard.addView(space(5));compactClimateCard.addView(label(I18n.t(R.string.settings_climate_preview_notes),12,MUTED));
        LinearLayout menuCard=card();menuCard.addView(label(I18n.t(R.string.settings_car_menu),14,ACCENT));
        CheckBox carMenuEnabled=new CheckBox(this);carMenuEnabled.setText(I18n.t(R.string.ui_show_the_vehicle_menu));carMenuEnabled.setTextColor(TEXT);carMenuEnabled.setChecked(CarMenuOverlay.enabled(this));menuCard.addView(carMenuEnabled);
        carMenuEnabled.setOnCheckedChangeListener((toggle,checked)->{CarMenuOverlay.enabled(this,checked);overlayAction(false);});
        Button menuDemo=button(I18n.t(R.string.ui_menu_test_15_seconds),false);menuDemo.setOnClickListener(v->{
            if(!ProbeState.INSTANCE.snapshot().running){Toast.makeText(this,I18n.t(R.string.ui_connect_first),Toast.LENGTH_SHORT).show();return;}
            try{startService(new Intent(this,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("carMenuDemo",true));}catch(RuntimeException e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}
        });menuCard.addView(menuDemo);carMenuOverlayInfo=label("",12,MUTED);menuCard.addView(space(8));menuCard.addView(carMenuOverlayInfo);
        menuCard.addView(space(5));menuCard.addView(label(I18n.t(R.string.ui_three_lines_from_the_vehicle),11,MUTED));
        LinearLayout cdCard=card();cdCard.addView(label(I18n.t(R.string.settings_players),14,ACCENT));
        CheckBox cdEnabled=new CheckBox(this);cdEnabled.setText(I18n.t(R.string.ui_open_player_on_source_change));cdEnabled.setTextColor(TEXT);cdEnabled.setChecked(AudioSourceController.enabled(this));cdCard.addView(cdEnabled);
        cdEnabled.setOnCheckedChangeListener((toggle,checked)->{AudioSourceController.enabled(this,checked);overlayAction(false);});
        CheckBox hideAux=new CheckBox(this);hideAux.setText(I18n.t(R.string.hide_player_on_aux));hideAux.setTextColor(TEXT);hideAux.setChecked(SourceSettings.hideOnAux(this));cdCard.addView(hideAux);
        hideAux.setOnCheckedChangeListener((toggle,checked)->{SourceSettings.hideOnAux(this,checked);overlayAction(false);});
        cdCard.addView(space(12));cdCard.addView(label(I18n.t(R.string.aux_name_title),13,TEXT));
        LinearLayout auxNameRow=row();auxNameRow.setGravity(Gravity.CENTER_VERTICAL);EditText auxName=new EditText(this);auxName.setSingleLine(true);auxName.setTextColor(TEXT);auxName.setHintTextColor(MUTED);auxName.setHint("AUX");auxName.setText(SourceSettings.auxName(this).equals("AUX")||SourceSettings.auxName(this).equals("Android")||SourceSettings.auxName(this).equals("Head Unit")?SourceSettings.auxCustomName(this):SourceSettings.auxName(this));auxName.setTextSize(14);auxName.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(32)});auxName.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS);auxName.setContentDescription(I18n.t(R.string.aux_name_title));
        auxNameRow.addView(auxName,new LinearLayout.LayoutParams(dp(260),dp(48)));auxNameRow.addView(spaceX(10));Button saveAuxName=button(I18n.t(R.string.ui_save),false);auxNameRow.addView(saveAuxName);
        saveAuxName.setOnClickListener(v->{SourceSettings.auxName(this,auxName.getText().toString());auxName.setText(SourceSettings.auxName(this));auxName.clearFocus();android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(auxName.getWindowToken(),0);Toast.makeText(this,I18n.t(R.string.aux_name_saved),Toast.LENGTH_SHORT).show();render();});String[] auxNames={"AUX","Android","Head Unit",I18n.t(R.string.aux_name_custom)};RadioGroup auxChoices=new RadioGroup(this);auxChoices.setOrientation(LinearLayout.HORIZONTAL);RadioButton[] auxOptions=new RadioButton[4];String currentAuxName=SourceSettings.auxName(this);int selectedAux=3;
        for(int i=0;i<auxNames.length;i++){RadioButton option=new RadioButton(this);option.setId(View.generateViewId());option.setText(auxNames[i]);option.setTextColor(TEXT);option.setButtonTintList(android.content.res.ColorStateList.valueOf(ACCENT));RadioGroup.LayoutParams optionSize=new RadioGroup.LayoutParams(-2,dp(48));optionSize.rightMargin=dp(12);auxChoices.addView(option,optionSize);auxOptions[i]=option;if(i<3&&currentAuxName.equals(auxNames[i]))selectedAux=i;}
        auxChoices.check(auxOptions[selectedAux].getId());auxNameRow.setVisibility(selectedAux==3?View.VISIBLE:View.GONE);cdCard.addView(auxChoices);
        auxChoices.setOnCheckedChangeListener((group,id)->{for(int i=0;i<auxOptions.length;i++)if(auxOptions[i].getId()==id){boolean custom=i==3;auxNameRow.setVisibility(custom?View.VISIBLE:View.GONE);if(!custom){SourceSettings.auxName(this,auxNames[i]);auxName.clearFocus();android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(keyboard!=null)keyboard.hideSoftInputFromWindow(auxName.getWindowToken(),0);render();}break;}});
        cdCard.addView(auxNameRow);cdCard.addView(label(I18n.t(R.string.aux_name_notes),12,MUTED));
        root.addView(label(I18n.t(R.string.settings_climate),16,ACCENT));root.addView(space(6));root.addView(label(I18n.t(R.string.settings_climate_choices_help),12,MUTED));root.addView(space(12));
        root.addView(climateWindowCard);root.addView(space(16));root.addView(compactClimateCard);root.addView(space(16));
        root.addView(cdCard);root.addView(space(16));
        root.addView(overlayCard);root.addView(space(16));
        root.addView(menuCard);root.addView(space(16));
        root.addView(stockOverlays.controls());root.addView(space(16));
        Button debug=button(I18n.t(R.string.debug_open),false);debug.setOnClickListener(v->startActivity(new Intent(this,DebugActivity.class)));root.addView(debug);
        }
        if(debugScreen()){
        LinearLayout playerCard=card();playerCard.addView(label(I18n.t(R.string.ui_vehicle_audio_players),11,ACCENT));
        playerCard.addView(label(I18n.t(R.string.ui_install_the_four_radio_cd),12,MUTED));LinearLayout players=row();for(AudioMode m:AudioMode.values()){Button player=button(m.title,false);player.setOnClickListener(v->startActivity(AudioPlayerActivity.intent(this,m,false)));players.addView(player);players.addView(spaceX(8));}playerCard.addView(space(8));playerCard.addView(players);root.addView(playerCard);root.addView(space(14));
        boolean wide = getResources().getConfiguration().screenWidthDp >= 700;
        LinearLayout panels = wide ? row() : column();
        LinearLayout audio = card(); audio.addView(label(I18n.t(R.string.ui_audio_source),11,MUTED));
        LinearLayout numbers = row(); numbers.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout sourceBox = column(); source = label("—",44,TEXT); sourceBox.addView(source);
        rawSource = label(I18n.t(R.string.ui_waiting_for_can_event),11,MUTED); sourceBox.addView(rawSource); numbers.addView(sourceBox,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout volumeBox = column(); volumeBox.addView(label(I18n.t(R.string.ui_volume),10,MUTED)); volume = label("—",36,TEXT); volumeBox.addView(volume); numbers.addView(volumeBox);
        audio.addView(numbers); audio.addView(space(12)); menu = label(I18n.t(R.string.ui_menu_234),12,MUTED); audio.addView(menu);
        LinearLayout climate = card(); climate.addView(label(I18n.t(R.string.ui_climate_two_zones),11,MUTED));
        LinearLayout zones = row(); left = label("—",38,TEXT); right = label("—",38,TEXT);
        LinearLayout zl=column(), zr=column(); zl.addView(left); zl.addView(label(I18n.t(R.string.ui_left),10,MUTED)); zr.addView(right); zr.addView(label(I18n.t(R.string.ui_right),10,MUTED));
        zones.addView(zl,new LinearLayout.LayoutParams(0,-2,1)); zones.addView(zr,new LinearLayout.LayoutParams(0,-2,1)); climate.addView(zones);
        climate.addView(space(12)); fan = label(I18n.t(R.string.ui_fan_214),12,MUTED); climate.addView(fan);
        climate.addView(space(8)); climateFlags = label("—",12,MUTED); climate.addView(climateFlags);
        if (wide) { panels.addView(audio,new LinearLayout.LayoutParams(0,-2,1.35f)); panels.addView(spaceX(14)); panels.addView(climate,new LinearLayout.LayoutParams(0,-2,1)); }
        else { panels.addView(audio); panels.addView(space(14)); panels.addView(climate); }
        root.addView(panels); root.addView(space(14));
        LinearLayout commands = card();commands.setTag("debug-audio-commands"); commands.addView(label(I18n.t(R.string.ui_audio_command_check),11,ACCENT)); commands.addView(space(10));
        android.content.SharedPreferences audioPrefs=getSharedPreferences("audioDebug",MODE_PRIVATE);
        audioTransport=audioChoice(commands,I18n.t(R.string.debug_audio_transport),new String[]{I18n.t(R.string.debug_audio_transport_profile),I18n.t(R.string.debug_audio_transport_raw)},audioPrefs.getInt("transport",0),"transport");
        AudioCommandOptions.Route[] routes=AudioCommandOptions.Route.values();String[] routeLabels=new String[routes.length];
        for(int i=0;i<routes.length;i++)routeLabels[i]=I18n.t(new AudioCommandOptions(AudioCommandOptions.Transport.PROFILE,routes[i],180,true).routeLabel());
        audioRoute=audioChoice(commands,I18n.t(R.string.debug_audio_sequence),routeLabels,audioPrefs.getInt("route",0),"route");
        String[] holds=new String[AudioCommandOptions.HOLDS.length];for(int i=0;i<holds.length;i++)holds[i]=AudioCommandOptions.HOLDS[i]+" ms";
        audioHold=audioChoice(commands,I18n.t(R.string.debug_audio_hold),holds,audioPrefs.getInt("hold",1),"hold");
        commands.addView(label(I18n.t(R.string.debug_audio_options_notes),11,MUTED));commands.addView(space(10));
        LinearLayout sources = row(); addCommand(sources,AudioCommand.RADIO); addCommand(sources,AudioCommand.CD); addCommand(sources,AudioCommand.USB); commands.addView(sources);
        commands.addView(space(8)); LinearLayout tracks = row(); addCommand(tracks,AudioCommand.PREVIOUS); addCommand(tracks,AudioCommand.NEXT); commands.addView(tracks);
        commands.addView(space(8)); commandStatus = label("",12,MUTED); commands.addView(commandStatus);
        commands.addView(space(6)); commands.addView(label(I18n.t(R.string.ui_am_fm_selects_the_radio),11,MUTED));
        Button stockAudio=button(I18n.t(R.string.ui_oem_radio_cd_usb_selection),false);stockAudio.setOnClickListener(v->{try{Intent stock=getPackageManager().getLaunchIntentForPackage("com.syu.canbus");if(stock==null)throw new android.content.ActivityNotFoundException();startActivity(stock);}catch(RuntimeException e){Toast.makeText(this,I18n.t(R.string.ui_oem_screen_unavailable)+e.getClass().getSimpleName(),Toast.LENGTH_LONG).show();ProbeState.INSTANCE.log("STOCK AUDIO SCREEN ERROR: "+e);}});commands.addView(space(8));commands.addView(stockAudio);
        root.addView(commands); root.addView(space(14));
        LinearLayout mediaCard = card(); mediaCard.addView(label(I18n.t(R.string.ui_radio_cd_usb_data),11,ACCENT));
        mediaCard.addView(space(10));mediaCard.addView(label(I18n.t(R.string.debug_radio_fields_title),12,ACCENT));mediaCard.addView(label(I18n.t(R.string.debug_radio_fields_notes),12,MUTED));
        radio = label("—",13,TEXT);radio.setTag("debug-radio-fields");radio.setTextIsSelectable(true);mediaCard.addView(space(8));mediaCard.addView(radio);
        mediaCard.addView(space(14));mediaCard.addView(label(I18n.t(R.string.debug_radio_text_title),12,ACCENT));mediaCard.addView(label(I18n.t(R.string.debug_radio_text_notes),12,MUTED));
        radioTextInfo=label("",12,MUTED);radioTextInfo.setTag("debug-radio-text");radioTextInfo.setTextIsSelectable(true);mediaCard.addView(space(8));mediaCard.addView(radioTextInfo);
        media = label("—",13,MUTED); mediaCard.addView(space(8)); mediaCard.addView(media);
        audioRouteInfo=label("",12,MUTED);mediaCard.addView(space(8));mediaCard.addView(audioRouteInfo);
        mediaCard.addView(label(I18n.t(R.string.ui_the_head_unit_channel_is),11,MUTED));
        mediaCard.addView(space(10)); Button marker = button(I18n.t(R.string.ui_mark_the_selected_source),false);
        marker.setOnClickListener(v -> markSource()); mediaCard.addView(marker);
        root.addView(mediaCard); root.addView(space(14));
        LinearLayout standbyCard=card();
        standbyCard.addView(label(I18n.t(R.string.debug_standby_title),11,ACCENT));
        standbyCard.addView(space(8));standbyCard.addView(label(I18n.t(R.string.debug_standby_notes),12,MUTED));
        standbyInfo=label("",13,TEXT);standbyInfo.setTextIsSelectable(true);
        standbyCard.addView(space(10));standbyCard.addView(standbyInfo);
        standbyCard.addView(space(14));standbyCard.addView(label(I18n.t(R.string.debug_standby_hypotheses_title),11,ACCENT));
        int[] standbyHypotheses={R.string.debug_standby_hypothesis_cache,R.string.debug_standby_hypothesis_buffer,
            R.string.debug_standby_hypothesis_family,R.string.debug_standby_hypothesis_source};
        for(int hypothesis:standbyHypotheses){standbyCard.addView(space(8));standbyCard.addView(label(I18n.t(hypothesis),12,MUTED));}
        root.addView(standbyCard);root.addView(space(14));
        LinearLayout vehicleCard=card();
        vehicleCard.addView(label(I18n.t(R.string.debug_vehicle_packets_title),11,ACCENT));
        vehicleCard.addView(space(8));vehicleCard.addView(label(I18n.t(R.string.debug_vehicle_packets_notes),12,MUTED));
        vehicleCard.addView(space(10));vehicleCard.addView(label(I18n.t(R.string.debug_vehicle_0e_notes),12,MUTED));
        vehiclePacket0EInfo=label("",13,TEXT);vehiclePacket0EInfo.setTextIsSelectable(true);
        vehicleCard.addView(space(8));vehicleCard.addView(vehiclePacket0EInfo);
        vehicleCard.addView(space(14));vehicleCard.addView(label(I18n.t(R.string.debug_vehicle_10_notes),12,MUTED));
        vehiclePacket10Info=label("",13,TEXT);vehiclePacket10Info.setTextIsSelectable(true);
        vehicleCard.addView(space(8));vehicleCard.addView(vehiclePacket10Info);
        root.addView(vehicleCard);root.addView(space(14));
        LinearLayout steeringCard=card();
        steeringCard.addView(label(I18n.t(R.string.debug_steering_title),11,ACCENT));
        steeringCard.addView(space(8));steeringCard.addView(label(I18n.t(R.string.debug_steering_notes),12,MUTED));
        steeringInfo=label("",13,TEXT);steeringInfo.setTextIsSelectable(true);
        steeringCard.addView(space(10));steeringCard.addView(steeringInfo);
        root.addView(steeringCard);root.addView(space(14));
        profile=label("",12,MUTED); stats=label("",12,MUTED); backend=label("",11,MUTED);
        rawInfo=label("",12,ACCENT);root.addView(rawInfo);root.addView(space(5));
        root.addView(profile); root.addView(space(5)); root.addView(stats); root.addView(space(5)); root.addView(backend);
        root.addView(space(10)); Button showFields = button(I18n.t(R.string.ui_all_received_can_fields),false); root.addView(showFields);
        fields = label("",11,MUTED); fields.setTypeface(Typeface.MONOSPACE); fields.setTextIsSelectable(true); fields.setVisibility(expanded?View.VISIBLE:View.GONE); root.addView(fields);
        showFields.setOnClickListener(v -> { fields.setVisibility(fields.getVisibility() == View.GONE ? View.VISIBLE : View.GONE); render(); });
        root.addView(space(18)); root.addView(label(I18n.t(R.string.ui_events_last_40),11,ACCENT)); root.addView(space(8));
        log=label("",11,MUTED); log.setTypeface(Typeface.MONOSPACE); log.setTextIsSelectable(true); log.setPadding(dp(14),dp(12),dp(14),dp(12)); log.setBackground(shape(CARD,12)); root.addView(log);
        root.addView(space(12)); root.addView(label(I18n.t(R.string.ui_source_98_0_means_the),11,MUTED));
        }
        compactButtons(root);ConsoleControls.verticalSpacing(root);
        scroll.post(()->scroll.scrollTo(0,previousScroll));
    }
    private void overlayAction(boolean demo) {
        if (!ProbeState.INSTANCE.snapshot().running) {
            Toast.makeText(this,I18n.t(R.string.ui_connect_first),Toast.LENGTH_SHORT).show(); return;
        }
        try { startService(new Intent(this,ProbeService.class).setAction(ProbeService.OVERLAY).putExtra("demo",demo)); }
        catch(RuntimeException e) { Toast.makeText(this,I18n.t(R.string.ui_indicator)+e.getMessage(),Toast.LENGTH_LONG).show(); }
    }
    private void startProbe() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 9);
        Intent intent=new Intent(this,ProbeService.class);
        try { if (Build.VERSION.SDK_INT>=26) startForegroundService(intent); else startService(intent); }
        catch (RuntimeException e) { ProbeState.INSTANCE.status(I18n.t(R.string.ui_could_not_start),e.toString(),false); }
    }
    private void render() {
        if(languageRevision!=I18n.revision()||palette!=ThemeSettings.resolve(this).palette)buildUi(); themes.update();
        ProbeState.Snapshot s=ProbeState.INSTANCE.snapshot();
        if(!debugScreen()){
        boolean allowed=Settings.canDrawOverlays(this);
        permissionInfo.setText(I18n.t(allowed?R.string.ui_permission_granted:R.string.ui_permission_not_granted));permissionInfo.setTextColor(allowed?ACCENT:palette.warning);
        climateOverlayInfo.setText(I18n.stored(s.climateOverlayStatus));carMenuOverlayInfo.setText(I18n.stored(s.carMenuOverlayStatus));
        overlayInfo.setText(I18n.stored(s.overlayStatus)+I18n.t(R.string.ui_nsource)+I18n.stored(s.sourceOverlayStatus));
            return;
        }
        rawInfo.setText((ProbeState.INSTANCE.recording()?"":I18n.t(R.string.diagnostics_record_off)+"\n")+"BSJ raw [1019]: "+s.rawCount+I18n.t(R.string.ui_chunks_evicted)+s.rawDropped+I18n.t(R.string.ui_these_are_can_adapter_protocol));
        status.setText(I18n.stored(s.status)); status.setTextColor(s.connected && s.count>0 ? ACCENT : palette.warning);
        detail.setText(I18n.stored(s.detail)); connect.setEnabled(!s.running); connect.setAlpha(s.running ? .45f : 1f); stop.setEnabled(s.running); stop.setAlpha(s.running?1f:.45f);
        source.setText(s.source()); volume.setText(s.volume()); left.setText(s.temperature(27)); right.setText(s.temperature(28));
        rawSource.setText(I18n.t(R.string.ui_source_98) + value(s,98) + I18n.t(R.string.ui_band_115) + value(s,115));
        menu.setText(I18n.t(R.string.ui_menu) + s.menu()); fan.setText(I18n.t(R.string.ui_fan_21) + value(s,21)); climateFlags.setText(s.climate()+"\nDUAL: "+s.dual()
            +"\n"+I18n.t(R.string.debug_climate_field16)+s.raw(16)+"\n"+I18n.t(R.string.debug_climate_field16_notes));
        radio.setText(RadioDebugInfo.fields(s));radioTextInfo.setText(RadioDebugInfo.textChannels(s));
        standbyInfo.setText(standbyDescription(s.standbyDisplay));
        vehiclePacket0EInfo.setText(vehiclePacketDescription(0x0E,s.vehiclePacket0E));
        vehiclePacket10Info.setText(vehiclePacketDescription(0x10,s.vehiclePacket10));
        steeringInfo.setText(steeringDescription(s));
        media.setText(s.media()); audioRouteInfo.setText(ProbeState.INSTANCE.audioRoutes.summary()); if (fields.getVisibility() == View.VISIBLE) fields.setText(s.allFields());
        boolean canCommand = s.running && s.connected && !s.commandBusy && s.number(1000) != null && s.number(1000) == 6291908;
        for (Button b : commandButtons) { b.setEnabled(canCommand); b.setAlpha(canCommand ? 1f : .45f); }
        commandStatus.setText(I18n.stored(s.commandStatus));
        Integer p=s.number(1000);
        profile.setText(I18n.t(R.string.ui_profile_252) + (p==null?I18n.t(R.string.ui_not_received_yet):p) + (p==null?"":s.honda()?I18n.t(R.string.ui_honda_compatible_schema):I18n.t(R.string.ui_unknown_schema_raw_values)));
        String age=s.lastEvent==0?"—":String.format(Locale.getDefault(),"%tT",s.lastEvent);
        stats.setText("Callbacks: " + s.count + I18n.t(R.string.ui_last_event) + age + (s.connected?"":I18n.t(R.string.ui_current_values_unavailable)));
        backend.setText(I18n.stored(s.backend)); log.setText(s.log.isEmpty()?I18n.t(R.string.ui_connection_results_and_received_events):s.log);
    }
    private String steeringDescription(ProbeState.Snapshot s){
        ProbeState.Value value=s.values.get(1049);
        if(value==null)return I18n.t(R.string.debug_steering_waiting);
        StringBuilder out=new StringBuilder(I18n.t(R.string.debug_standby_received))
            .append(String.format(Locale.getDefault(),"%tF %tT",value.at,value.at))
            .append(" · ").append(Math.max(0,(System.currentTimeMillis()-value.at)/1000)).append(I18n.t(R.string.debug_standby_age))
            .append("\nints = ").append(java.util.Arrays.toString(value.ints))
            .append("\nfloats = ").append(java.util.Arrays.toString(value.floats))
            .append("\nstrings = ").append(java.util.Arrays.toString(value.strings));
        if(value.ints!=null&&value.ints.length>=2&&value.ints[0]==0)
            out.append("\n").append(I18n.t(R.string.debug_steering_normalized)).append(value.ints[1]);
        else out.append("\n").append(I18n.t(R.string.debug_steering_unassigned));
        if(!s.connected)out.append("\n").append(I18n.t(R.string.debug_steering_disconnected));
        return out.toString();
    }
    private String vehiclePacketDescription(int type,BsjVehiclePackets.Sample sample){
        String heading=String.format(Locale.ROOT,"TYPE 0x%02X",type);
        if(sample==null)return heading+" · "+I18n.t(R.string.debug_vehicle_not_received);
        StringBuilder out=new StringBuilder(heading).append(" · ").append(I18n.t(R.string.debug_vehicle_received))
            .append(sample.count).append(" · ").append(I18n.t(R.string.debug_vehicle_mapped)).append(sample.mappedCount)
            .append("\nLEN ").append(sample.length).append(" · ").append(I18n.t(R.string.debug_standby_received))
            .append(String.format(Locale.getDefault(),"%tF %tT",sample.at,sample.at))
            .append(" · ").append(Math.max(0,(System.currentTimeMillis()-sample.at)/1000)).append(I18n.t(R.string.debug_standby_age))
            .append("\n").append(I18n.t(R.string.debug_standby_raw)).append(sample.payloadHex);
        if(sample.fields.isEmpty())out.append("\n").append(I18n.t(R.string.debug_vehicle_unexpected));
        else{
            out.append("\n").append(I18n.t(R.string.debug_vehicle_fields));
            int index=0;for(java.util.Map.Entry<Integer,Integer> e:sample.fields.entrySet()){
                if(index>0)out.append(index%4==0?"\n":" · ");
                out.append('[').append(e.getKey()).append("]=").append(e.getValue());index++;
            }
            if(type==0x10&&sample.length==2)out.append("\n").append(I18n.t(R.string.debug_vehicle_extra_byte));
        }
        return out.toString();
    }
    private String standbyDescription(BsjStandbyDisplay d){
        if(d==null)return I18n.t(R.string.debug_standby_waiting);
        StringBuilder out=new StringBuilder(I18n.t(R.string.debug_standby_received))
            .append(String.format(Locale.getDefault(),"%tF %tT",d.at,d.at))
            .append(" · ").append(Math.max(0,(System.currentTimeMillis()-d.at)/1000))
            .append(I18n.t(R.string.debug_standby_age));
        out.append("\n").append(I18n.t(R.string.debug_standby_raw)).append(d.raw);
        out.append("\n").append(I18n.t(R.string.debug_standby_byte0))
            .append(d.firstByte<0?"—":String.format(Locale.ROOT,"%02X",d.firstByte));
        switch(d.layout){
            case EMPTY:out.append("\n").append(I18n.t(R.string.debug_standby_empty));break;
            case TEXT:out.append("\n").append(I18n.t(R.string.debug_standby_text)).append(d.text);break;
            case POSSIBLE_PLAYBACK:
                out.append("\n").append(I18n.t(R.string.debug_standby_playback))
                    .append(String.format(Locale.ROOT,"%03d · %d:%02d",d.track,d.minutes,d.seconds));
                String[] bytes=d.raw.split(" ");
                if(bytes.length>=4){
                    out.append("\n").append(I18n.t(R.string.debug_standby_control_prefix)).append(bytes[1]).append(' ').append(bytes[2]).append(' ').append(bytes[3]);
                    out.append("\n").append(I18n.t(R.string.debug_standby_family_candidate)).append(bytes[3]);
                }
                if(!d.text.isEmpty())out.append("\n").append(I18n.t(R.string.debug_standby_tail)).append(d.text);
                break;
            default:out.append("\n").append(I18n.t(R.string.debug_standby_unknown));break;
        }
        return out.toString();
    }
    private String value(ProbeState.Snapshot s,int code) { Integer n=s.number(code); return !s.connected||n==null?"—":String.valueOf(n); }
    private void markSource() {
        String[] labels = {"FM1", "FM2", "AM1", "AM2", "CD", "AUX", "USB", "iPod", I18n.t(R.string.ui_audio_off)};
        new AlertDialog.Builder(this).setTitle(I18n.t(R.string.ui_which_source_is_selected_using))
            .setItems(labels, (dialog, which) -> {
                ProbeState.INSTANCE.mark(labels[which]);
                Toast.makeText(this,I18n.t(R.string.ui_marker) + labels[which] + I18n.t(R.string.ui_added_to_the_log),Toast.LENGTH_SHORT).show();
            }).setNegativeButton(I18n.t(R.string.ui_cancel),null).show();
    }
    private Spinner audioChoice(LinearLayout container,String caption,String[] choices,int selected,String preference){
        LinearLayout line=row();line.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading=label(caption,12,MUTED);line.addView(heading,new LinearLayout.LayoutParams(dp(125),-2));
        Spinner picker=new Spinner(this);picker.setTag("debug-audio-"+preference);picker.setContentDescription(caption);
        android.widget.ArrayAdapter<String> adapter=new android.widget.ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,choices){
            @Override public View getView(int pos,View convert,android.view.ViewGroup parent){TextView v=(TextView)super.getView(pos,convert,parent);v.setTextColor(TEXT);v.setTextSize(13);return v;}
            @Override public View getDropDownView(int pos,View convert,android.view.ViewGroup parent){TextView v=(TextView)super.getDropDownView(pos,convert,parent);v.setTextColor(TEXT);v.setTextSize(13);v.setPadding(dp(14),dp(12),dp(14),dp(12));return v;}
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);picker.setAdapter(adapter);
        picker.setPopupBackgroundDrawable(shape(CARD,10));picker.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        picker.setSelection(Math.max(0,Math.min(selected,choices.length-1)));
        picker.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(AdapterView<?> parent,View view,int position,long id){getSharedPreferences("audioDebug",MODE_PRIVATE).edit().putInt(preference,position).apply();}
            @Override public void onNothingSelected(AdapterView<?> parent){}
        });
        line.addView(picker,new LinearLayout.LayoutParams(-2,dp(44)));container.addView(line);return picker;
    }
    private void addCommand(LinearLayout row, AudioCommand action) {
        Button b = button(action.title(),false); commandButtons.add(b);
        b.setOnClickListener(v -> {
            try { startService(new Intent(this,ProbeService.class).setAction(ProbeService.COMMAND).putExtra("command",action.name())
                .putExtra("audioDebug",true)
                .putExtra("audioTransport",AudioCommandOptions.Transport.values()[audioTransport.getSelectedItemPosition()].name())
                .putExtra("audioRoute",AudioCommandOptions.Route.values()[audioRoute.getSelectedItemPosition()].name())
                .putExtra("audioHoldMs",AudioCommandOptions.HOLDS[audioHold.getSelectedItemPosition()])); }
            catch (RuntimeException e) { ProbeState.INSTANCE.commandRejected(I18n.t(R.string.ui_could_not_send_command) + e); }
        });
        if (row.getChildCount() > 0) row.addView(spaceX(8)); row.addView(b,new LinearLayout.LayoutParams(0,-2,1));
    }
    private void export() {jsonExport.start();}
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);jsonExport.result(request,result,data);
    }
    @Override protected void onStart() { super.onStart();if(!debugScreen()){stockOverlays.start();themes.start();}ui.removeCallbacks(refresh);ui.post(refresh); }
    @Override protected void onStop() { ui.removeCallbacks(refresh);themes.stop();super.onStop(); }
    @Override protected void onDestroy(){if(setupGuide!=null)setupGuide.close();stockOverlays.close();super.onDestroy();}
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);themes.permissionResult(code);}
    private void compactButtons(View view){
        if(!(view instanceof ViewGroup))return;ViewGroup parent=(ViewGroup)view;
        for(int i=0;i<parent.getChildCount();i++){View child=parent.getChildAt(i);
            if(child instanceof Button&&!(child instanceof CompoundButton)){
                ((Button)child).setMaxWidth(dp(360));
                if(parent instanceof LinearLayout&&((LinearLayout)parent).getOrientation()==LinearLayout.VERTICAL){
                    LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)child.getLayoutParams();lp.width=ViewGroup.LayoutParams.WRAP_CONTENT;child.setLayoutParams(lp);
                }
            }
            compactButtons(child);
        }
    }
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private LinearLayout card() { LinearLayout l=column(); l.setPadding(dp(20),dp(16),dp(20),dp(16)); l.setBackground(shape(CARD,18)); return l; }
    private TextView label(String text,int size,int color) { TextView v=new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setFontFeatureSettings("tnum"); return v; }
    private View space(int h) { View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(h))); return v; }
    private View spaceX(int w) { View v=new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(w),1)); return v; }
    private GradientDrawable shape(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private Button button(String text,boolean primary) { return ConsoleControls.button(this,text,palette,primary); }
}
