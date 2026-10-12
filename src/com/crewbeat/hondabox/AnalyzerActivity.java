package com.crewbeat.hondabox;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Native passive experiment viewer. Uses the existing service and never sends a vehicle command. */
public class AnalyzerActivity extends Activity {
    private final Handler ui=new Handler(Looper.getMainLooper());private HandlerThread thread;private Handler worker;
    private ThemeSettings.Palette palette;private JsonExport exporter;private boolean visible,busy;
    private TextView status,report,watch,compareTitle;private LinearLayout markers,selectors;private Spinner group,left,right;
    private CheckBox unknown;private EditText offset;private final List<Button> marks=new ArrayList<>();
    private List<String> groupKeys=new ArrayList<>();private List<Long> windowIds=new ArrayList<>();
    private String selectedGroup="",mode="markers";private long aId=-1,bId=-1;private int byteOffset,revision;
    private int languageRevision=-1;private boolean unknownOnly=true;private String candidateKey="",candidateText="";
    private final Runnable refresh=new Runnable(){public void run(){if(!visible)return;load();ui.postDelayed(this,1000);}};
    @Override public void onCreate(Bundle saved){super.onCreate(saved);exporter=new JsonExport(this);thread=new HandlerThread("BsjAnalyzerUI");thread.start();worker=new Handler(thread.getLooper());
        if(saved!=null){mode=saved.getString("mode","markers");selectedGroup=saved.getString("group","");aId=saved.getLong("a",-1);bId=saved.getLong("b",-1);byteOffset=saved.getInt("offset",0);unknownOnly=saved.getBoolean("unknown",true);}build();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private LinearLayout column(){LinearLayout v=new LinearLayout(this);v.setOrientation(1);return v;}
    private LinearLayout row(){LinearLayout v=new LinearLayout(this);v.setOrientation(0);return v;}
    private TextView text(String value,int size){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(palette.text);v.setPadding(0,dp(6),0,dp(6));return v;}
    private Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(palette.text);b.setAllCaps(false);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable();bg.setColor(palette.card);bg.setCornerRadius(dp(8));bg.setStroke(dp(1),(palette.muted&0xffffff)|0x40000000);b.setBackground(bg);b.setMinHeight(dp(40));b.setOnClickListener(v->action.run());return b;}
    private void build(){candidateKey="";languageRevision=I18n.revision();revision++;palette=ThemeSettings.resolve(this).palette;getWindow().setStatusBarColor(palette.bg);getWindow().setNavigationBarColor(palette.bg);
        getWindow().getDecorView().setSystemUiVisibility(palette.night?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(palette.bg);LinearLayout root=column();root.setPadding(dp(24),dp(16),dp(24),dp(24));scroll.addView(root);setContentView(scroll);
        root.addView(text("BSJ Experiment Analyzer",26));root.addView(text(I18n.t(R.string.ui_read_only_markers_are_your),13));
        LinearLayout actions=row();actions.addView(button(I18n.t(R.string.ui_connect),()->{Intent i=new Intent(this,ProbeService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);}));
        actions.addView(button(I18n.t(R.string.ui_stop),()->BackgroundSettings.stop(this)));actions.addView(button(I18n.t(R.string.ui_export_json_v2),()->exporter.start()));actions.addView(button(I18n.t(R.string.ui_back),this::finish));root.addView(actions);
        status=text(I18n.t(R.string.ui_waiting_for_frames),13);root.addView(status);LinearLayout tabs=row();for(String tab:new String[]{"markers","frames","compare","Watch"})tabs.addView(button(tabLabel(tab),()->{mode=tab;revision++;showMode();load();}),new LinearLayout.LayoutParams(0,-2,1));root.addView(tabs);
        markers=column();marks.clear();for(int i=0;i<BsjAnalyzer.LABELS.length;i+=3){LinearLayout line=row();for(int j=i;j<Math.min(i+3,BsjAnalyzer.LABELS.length);j++){final String label=BsjAnalyzer.LABELS[j];Button b=button(label,()->{
            long id=ProbeState.INSTANCE.markExperiment(label);Toast.makeText(this,id<0?I18n.t(R.string.ui_an_active_connection_and_profile):label+I18n.t(R.string.ui_window_completes_in_5_seconds),Toast.LENGTH_SHORT).show();revision++;load();});b.setTextSize(12);marks.add(b);line.addView(b,new LinearLayout.LayoutParams(0,-2,1));}markers.addView(line);}root.addView(markers);
        selectors=column();group=new Spinner(this);group.setBackgroundTintList(android.content.res.ColorStateList.valueOf(palette.muted));selectors.addView(text(I18n.t(R.string.ui_group_type_payload_length),12));selectors.addView(group);group.setOnItemSelectedListener(select(()->{
            int n=group.getSelectedItemPosition();if(n>=0&&n<groupKeys.size()&&!selectedGroup.equals(groupKeys.get(n))){selectedGroup=groupKeys.get(n);revision++;load();}}));
        unknown=new CheckBox(this);unknown.setText(I18n.t(R.string.ui_frames_unknown_types_only));unknown.setTextColor(palette.text);unknown.setButtonTintList(android.content.res.ColorStateList.valueOf(palette.accent));unknown.setChecked(unknownOnly);unknown.setOnCheckedChangeListener((v,on)->{unknownOnly=on;revision++;load();});selectors.addView(unknown);
        left=new Spinner(this);right=new Spinner(this);compareTitle=text(I18n.t(R.string.ui_compare_window_a_window_b),12);selectors.addView(compareTitle);selectors.addView(left);selectors.addView(right);
        left.setOnItemSelectedListener(select(()->{int n=left.getSelectedItemPosition();if(n>=0&&n<windowIds.size()&&aId!=windowIds.get(n)){aId=windowIds.get(n);revision++;load();}}));
        right.setOnItemSelectedListener(select(()->{int n=right.getSelectedItemPosition();if(n>=0&&n<windowIds.size()&&bId!=windowIds.get(n)){bId=windowIds.get(n);revision++;load();}}));
        offset=new EditText(this);offset.setTextColor(palette.text);offset.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);offset.setHint(I18n.t(R.string.ui_watch_byte_offset_starting_at));offset.setHintTextColor(palette.muted);offset.setText(String.valueOf(byteOffset));
        offset.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int n){}public void onTextChanged(CharSequence s,int a,int before,int count){try{byteOffset=Integer.parseInt(s.toString());}catch(Exception e){byteOffset=-1;}revision++;load();}public void afterTextChanged(Editable e){}});selectors.addView(offset);root.addView(selectors);
        watch=text("",30);watch.setTypeface(Typeface.MONOSPACE);root.addView(watch);report=text("",13);report.setTypeface(Typeface.MONOSPACE);report.setTextIsSelectable(true);root.addView(report);
        root.addView(button(I18n.t(R.string.ui_clear_analysis),()->new AlertDialog.Builder(this).setTitle(I18n.t(R.string.ui_clear_frames_and_experiments)).setMessage(I18n.t(R.string.ui_unsaved_analysis_windows_will_be))
            .setNegativeButton(I18n.t(R.string.ui_cancel),null).setPositiveButton(I18n.t(R.string.ui_clear),(d,w)->{ProbeState.INSTANCE.analyzer.clear(System.currentTimeMillis());selectedGroup="";aId=bId=-1;candidateKey="";revision++;load();}).show()));
        groupKeys=new ArrayList<>();windowIds=new ArrayList<>();showMode();
    }
    private AdapterView.OnItemSelectedListener select(Runnable action){return new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int position,long id){action.run();}public void onNothingSelected(AdapterView<?> p){}};}
    private String tabLabel(String tab){return tab.equals("markers")?I18n.t(R.string.ui_markers):tab.equals("frames")?I18n.t(R.string.ui_frames_16):tab.equals("compare")?I18n.t(R.string.ui_compare):"Watch";}
    private void showMode(){markers.setVisibility(mode.equals("markers")?View.VISIBLE:View.GONE);selectors.setVisibility(mode.equals("markers")?View.GONE:View.VISIBLE);
        compareTitle.setVisibility(mode.equals("compare")?View.VISIBLE:View.GONE);unknown.setVisibility(mode.equals("frames")?View.VISIBLE:View.GONE);left.setVisibility(mode.equals("compare")?View.VISIBLE:View.GONE);right.setVisibility(left.getVisibility());offset.setVisibility(mode.equals("Watch")?View.VISIBLE:View.GONE);watch.setVisibility(offset.getVisibility());}
    private ArrayAdapter<String> adapter(List<String> values){return new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,values){
        @Override public View getView(int n,View old,ViewGroup parent){TextView v=(TextView)super.getView(n,old,parent);v.setTextColor(palette.text);return v;}
        @Override public View getDropDownView(int n,View old,ViewGroup parent){TextView v=(TextView)super.getDropDownView(n,old,parent);v.setTextColor(palette.text);v.setBackgroundColor(palette.card);v.setPadding(dp(12),dp(12),dp(12),dp(12));return v;}};}
    private void options(BsjAnalyzer.Snapshot s,LinkedHashMap<String,BsjAnalyzer.Stats> stats){List<String> keys=new ArrayList<>(stats.keySet());if(!keys.equals(groupKeys)){
        groupKeys=keys;if(!keys.contains(selectedGroup))selectedGroup=keys.isEmpty()?"":keys.get(0);group.setAdapter(adapter(keys));group.setSelection(Math.max(0,keys.indexOf(selectedGroup)));revision++;}
        List<Long> ids=new ArrayList<>();List<String> titles=new ArrayList<>();for(BsjAnalyzer.Window w:s.windows){ids.add(w.id);titles.add("#"+w.id+" "+w.label+" · "+String.format(Locale.US,"%tT",w.at));}
        if(!ids.equals(windowIds)){windowIds=ids;if(!ids.contains(aId))aId=ids.isEmpty()?-1:ids.get(0);if(!ids.contains(bId))bId=ids.isEmpty()?-1:ids.get(ids.size()-1);
            left.setAdapter(adapter(titles));right.setAdapter(adapter(titles));left.setSelection(Math.max(0,ids.indexOf(aId)));right.setSelection(Math.max(0,ids.indexOf(bId)));revision++;}
    }
    private void load(){if(busy||!visible||report==null)return;if(languageRevision!=I18n.revision()||palette!=ThemeSettings.resolve(this).palette)build();busy=true;
        final int tag=revision,off=byteOffset;final long a=aId,b=bId,now=System.currentTimeMillis();final String tab=mode,key=selectedGroup;final boolean onlyUnknown=unknownOnly;
        final ProbeState.Snapshot live=ProbeState.INSTANCE.overlaySnapshot();final BsjAnalyzer.Snapshot s=ProbeState.INSTANCE.analyzer.snapshot();
        worker.post(()->{try{LinkedHashMap<String,BsjAnalyzer.Stats> stats=BsjAnalyzer.statistics(s.currentFrames());String value=describe(tab,key,a,b,off,onlyUnknown,s,stats,now);String watched=watchValue(key,off,stats);
            ui.post(()->{busy=false;if(!visible||isDestroyed())return;if(tag!=revision){load();return;}options(s,stats);status.setText((s.active&&live.connected?I18n.t(R.string.ui_connected):I18n.t(R.string.ui_stopped_historical_data))+I18n.t(R.string.ui_profile)+s.profile+"\n"+s.liveText(System.currentTimeMillis())+I18n.t(R.string.ui_dropped)+s.droppedFrames+I18n.t(R.string.ui_pending_windows)+s.pending(now));
                for(Button mark:marks)mark.setEnabled(live.running&&live.connected&&s.active&&s.profile==6291908);report.setText(value);watch.setText(watched);});
        }catch(Exception e){ui.post(()->{busy=false;if(visible)report.setText(I18n.t(R.string.ui_analysis_error)+e.getMessage());});}});
    }
    private String describe(String tab,String key,long a,long b,int off,boolean onlyUnknown,BsjAnalyzer.Snapshot s,LinkedHashMap<String,BsjAnalyzer.Stats> stats,long now)throws Exception {
        StringBuilder text=new StringBuilder();if(tab.equals("markers")){
            text.append(I18n.t(R.string.ui_windows_span_2_5_seconds));
            for(BsjAnalyzer.Window w:s.windows)text.append(windowTitle(w,now)).append('\n');if(s.windows.isEmpty())text.append(I18n.t(R.string.ui_no_experiments_yet));return text.toString();}
        if(tab.equals("Watch")){BsjAnalyzer.Stats group=stats.get(key);text.append("Offset ").append(off).append(" · u8/s8, BE16/sBE16, LE16/sLE16\n");
            if(group==null)text.append(I18n.t(R.string.ui_no_frames_of_this_group));else text.append(s.active?I18n.t(R.string.ui_last_received_frame):I18n.t(R.string.ui_historical_frame_capture_stopped)).append(" · ").append(String.format(Locale.US,"%tT",group.lastAt)).append(I18n.t(R.string.ui_age)).append(Math.max(0,now-group.lastAt)/1000.0).append(I18n.t(R.string.ui_s_n)).append(group.last.fullHex());return text.toString();}
        if(tab.equals("frames")){int shown=0,total=0;for(BsjAnalyzer.Stats group:stats.values()){if(onlyUnknown&&!BsjAnalyzer.known(group.type).equals("Unknown"))continue;total++;
            if(shown++<100)text.append(group.last.key()).append(" · ").append(BsjAnalyzer.known(group.type)).append(" · n=").append(group.count).append(" · ").append(group.rate()==null?"rate —":String.format(Locale.US,"%.2f Hz",group.rate())).append('\n');}
            if(total==0)text.append(onlyUnknown?I18n.t(R.string.ui_no_unknown_types_received_yet):I18n.t(R.string.ui_no_frames_yet_n));if(total>100)text.append(I18n.t(R.string.ui_showing_the_first_100_groups));
            BsjAnalyzer.Stats selected=stats.get(key);if(selected!=null){text.append(I18n.t(R.string.ui_nselected)).append(key).append(" · payload:\n").append(selected.last.payloadHex()).append("\nBYTE  min max avg unique\n");for(int i=0;i<selected.length;i++)text.append(String.format(Locale.US,"%3d %4d %3d %7.2f %3d%s\n",i,selected.min(i),selected.max(i),selected.avg(i),selected.unique(i),selected.unique(i)>1?" *":""));}return text.toString();}
        BsjAnalyzer.Window wa=s.window(a),wb=s.window(b);if(wa==null||wb==null)text.append(I18n.t(R.string.ui_save_two_experiment_markers_first));else {
            text.append("A ").append(windowTitle(wa,now)).append("\nB ").append(windowTitle(wb,now)).append('\n');if(wa.segment!=wb.segment)text.append(I18n.t(R.string.ui_different_connections_segments_historical_comparison));
            BsjAnalyzer.Stats sa=BsjAnalyzer.statistics(wa.frames).get(key),sb=BsjAnalyzer.statistics(wb.frames).get(key);text.append("\n").append(key).append(" BYTE | A min/max/avg/unique | B\n");
            int length=sa!=null?sa.length:sb!=null?sb.length:0;for(int i=0;i<length;i++)text.append(i).append(" | ").append(byteStats(sa,i)).append(" | ").append(byteStats(sb,i)).append(sa!=null&&sb!=null&&(sa.min(i)!=sb.min(i)||sa.max(i)!=sb.max(i)||Math.abs(sa.avg(i)-sb.avg(i))>.01)?" *":"").append('\n');if(length==0)text.append(I18n.t(R.string.ui_this_group_is_absent_from));}
        String signature=s.segment+":"+s.windows.size();for(BsjAnalyzer.Window w:s.windows)signature+=":"+w.id+":"+w.complete(now)+":"+w.dropped+":"+w.interruptedAt;
        if(!signature.equals(candidateKey)){JSONArray found=SignalCandidates.find(s,now);StringBuilder c=new StringBuilder();for(int i=0;i<found.length();i++){JSONObject j=found.getJSONObject(i);c.append(j.getString("signal")).append("? TYPE ").append(String.format(Locale.US,"%02X",j.getInt("type"))).append(" LEN ").append(j.getInt("length")).append(" BYTE ").append(j.getInt("offset")).append(" ").append(j.getString("encoding")).append(" × ").append(j.opt("scale")).append('\n');}
            candidateKey=signature;candidateText=c.length()==0?I18n.t(R.string.ui_no_matching_candidates_at_least):c.toString();}
        return text.append(I18n.t(R.string.ui_ncandidates_unverified_n)).append(candidateText).append(I18n.t(R.string.ui_heuristic_search_is_limited_to)).toString();
    }
    private static String byteStats(BsjAnalyzer.Stats s,int i){return s==null?"—":String.format(Locale.US,"%d/%d/%.2f/%d",s.min(i),s.max(i),s.avg(i),s.unique(i));}
    private static String windowTitle(BsjAnalyzer.Window w,long now){return "#"+w.id+" "+w.label+" · "+(w.captureQuality.equals("RAW_LIVE")?"":w.captureQuality+" · ")+(w.interruptedAt!=0?I18n.t(R.string.ui_interrupted):w.complete(now)?I18n.t(R.string.ui_complete):I18n.t(R.string.ui_remaining)+Math.max(0,(w.end()-now+999)/1000)+I18n.t(R.string.ui_s))+" · "+w.frames.size()+I18n.t(R.string.ui_frames)+(w.dropped>0?I18n.t(R.string.ui_dropped)+w.dropped:"")+(w.startedAt>w.start()?I18n.t(R.string.ui_incomplete_2_s_pre_roll):"");}
    private static String watchValue(String key,int off,Map<String,BsjAnalyzer.Stats> stats){BsjAnalyzer.Stats s=stats.get(key);StringBuilder b=new StringBuilder();for(String e:SignalCandidates.ENCODINGS){Integer value=BsjAnalyzer.decode(s==null?null:s.last,off,e);b.append(e).append("  ").append(value==null?"—":value).append('\n');}return b.toString();}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);exporter.result(request,result,data);}
    @Override protected void onStart(){super.onStart();visible=true;ui.post(refresh);}
    @Override protected void onStop(){visible=false;ui.removeCallbacks(refresh);super.onStop();}
    @Override protected void onDestroy(){thread.quitSafely();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);out.putString("mode",mode);out.putString("group",selectedGroup);out.putLong("a",aId);out.putLong("b",bId);out.putInt("offset",byteOffset);out.putBoolean("unknown",unknownOnly);}
}
