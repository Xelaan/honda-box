package com.crewbeat.hondabox.tests;

import android.app.*;import android.content.*;import android.os.*;import android.widget.*;
import com.crewbeat.hondabox.*;import org.json.*;import java.util.*;import java.util.function.BooleanSupplier;

/** Raw-only, logical-only, stale FYT callbacks and actual volume/player/debug windows. Emulator only. */
public final class BsjFallbackTests extends Instrumentation {
 private int checks;private String replay;private Activity activity;private VolumeOverlay overlay;private final ProbeState live=ProbeState.INSTANCE;
 @Override public void onCreate(Bundle args){super.onCreate(args);replay=args==null?null:args.getString("replay");start();}
 private void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
 private void main(Runnable task){Throwable[] error={null};runOnMainSync(()->{try{task.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError("UI action",error[0]);}
 private void await(BooleanSupplier ok,String why){long end=SystemClock.uptimeMillis()+3500;while(!ok.getAsBoolean()&&SystemClock.uptimeMillis()<end)SystemClock.sleep(25);check(ok.getAsBoolean(),why);}
 private Object field(Object object,String name){try{java.lang.reflect.Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(Exception e){throw new RuntimeException(e);}}
 private void render(Object view){main(()->{try{java.lang.reflect.Method m=(view instanceof Activity?SettingsScreenActivity.class:VolumeOverlay.class).getDeclaredMethod("render");m.setAccessible(true);m.invoke(view);}catch(Exception e){throw new RuntimeException(e);}});}
 private void value(ProbeState s,int code,int n){s.event(code,new int[]{n},null,null);}
 private int[] packet(int type,int... p){int[] raw=new int[p.length+4];raw[0]=46;raw[1]=type;raw[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){raw[i+3]=p[i];sum+=p[i];}raw[raw.length-1]=(~sum)&255;return raw;}
 private void frame(ProbeState s,int type,int... p){s.event(1019,packet(type,p),null,null);}
 private void text(ProbeState s,String t){int[] p=new int[t.length()+1];p[0]=1;for(int i=0;i<t.length();i++)p[i+1]=t.charAt(i);frame(s,7,p);}
 private void prepare(ProbeState s,int profile,boolean override){s.processAsHondaAccord(override);s.newSession();s.running(true);s.status("test","synthetic transport",true);s.recording(false);value(s,1000,profile);}
 private void model()throws Exception{
  for(int profile:new int[]{6291908,123456}){
   ProbeState s=new ProbeState();prepare(s,profile,profile!=6291908);value(s,98,0);value(s,132,27);
   for(int volume:new int[]{24,25,26,27,28,32,39,40,31,27}){frame(s,3,0x82,0x80|volume);ProbeState.Snapshot a=s.snapshot();check(a.volumeNumber()==volume&&a.volume().equals(String.valueOf(volume)),"raw volume wins stale FYT");check(a.number(132)==27&&a.number(98)==0,"original FYT values untouched");check(a.audioSource()==3&&AudioMode.reported(a)==null,"raw AUX wins startup zero");}
   value(s,132,0);check(s.snapshot().volume().equals("27"),"later default callback cannot erase raw volume");
   frame(s,3,0x80,12);frame(s,4,2,0,251,3,0x81);ProbeState.Snapshot a=s.snapshot();
   check(a.source().equals("FM2")&&a.radioBand().equals("FM2"),"raw-only radio band in source label");check(a.displayNumber(116)==3&&a.displayNumber(118)==1&&a.preset(3)==251,"raw radio selection/stereo/preset");
   frame(s,3,0x81,14);frame(s,6,0x10,0x23,0x45);frame(s,5,2,0x10,1,1,0,0x15,0x01,0x23);text(s,"CD title");a=s.snapshot();
   int[] slots={1,0,2,3,4,5};for(int i=0;i<6;i++)check(a.displayNumber(106+i)==slots[i],"raw CD slot nibble "+i);
   check(a.cdNumber(113).equals("2")&&a.cdNumber(101).equals("15")&&a.cdTime().equals("01:23"),"raw CD selection/BCD track/time");check(a.combinedMediaText().equals("CD title")&&a.playMode().equals("Repeat one"),"raw CD title/play mode");
   frame(s,3,0x81,30);frame(s,5,0,0,0,0,0,0,0,0);check(s.snapshot().cdNumber(113).equals("2")&&s.snapshot().combinedMediaText().equals("CD title"),"CD retained through volume blanking");
   frame(s,3,0x83,15);frame(s,5,0,0,1,2,0,0x12,0,0x23);text(s,"USB title");a=s.snapshot();check(a.mediaDevice==ProbeState.MediaDevice.USB&&AudioMode.reported(a)==AudioMode.USB,"explicit USB playback remains USB");check(a.cdNumber(101).equals("12")&&a.cdTime().equals("00:23")&&a.combinedMediaText().equals("USB title"),"raw USB track/time/title");
   frame(s,3,0xC3,15);int[] menu=new int[49];menu[0]=0x20;String row="PLAY MODE";for(int i=0;i<row.length();i++)menu[17+i]=row.charAt(i);frame(s,9,menu);check(s.snapshot().menuRow(1).equals(row)&&s.snapshot().displayNumber(139)==1,"raw menu content and visibility");frame(s,3,0x83,15);check(s.snapshot().displayNumber(139)==0,"raw explicit menu close");
   frame(s,3,0x81,15);frame(s,6,0x32,0x22,0x22);check(s.snapshot().mediaDevice==ProbeState.MediaDevice.IPOD,"verified marker still selects iPod from CD");
   frame(s,3,0x82,15);long epoch=s.snapshot().sourceSequence;frame(s,6,0x32,0x22,0x22);text(s,"Stray title");for(int t:new int[]{10,11,123})frame(s,t,1,0xF2,0x39);a=s.snapshot();check(a.audioSource()==3&&a.mediaDevice==ProbeState.MediaDevice.UNKNOWN&&a.sourceSequence==epoch,"AUX unaffected by marker/text/unknown types");
   long total=a.protocolEvidence.get(3).count;int[] bad=packet(3,0x81,40);bad[bad.length-1]^=1;s.event(1019,bad,null,null);check(s.snapshot().protocolEvidence.get(3).count==total&&s.snapshot().volumeNumber()==15,"bad checksum neither counted nor applied");frame(s,3,0x81);check(s.snapshot().audioSource()==3&&s.snapshot().volumeNumber()==15,"wrong length does not change source/volume");
   frame(s,6,0x10,0x23);check(s.snapshot().displayNumber(106)==3,"wrong slot length leaves previous state");
   Map<Integer,BsjProtocolEvidence.Sample> before=s.snapshot().protocolEvidence;check(before.get(3).length==1&&before.get(6).length==2,"receipt evidence retains unexpected lengths without decoding");s.clearLog();check(s.snapshot().protocolEvidence.get(3).count==total+1,"log reset retains connection evidence");check(!s.recording()&&s.snapshot().rawCount==0,"receipt evidence needs no recorded history");check(new JSONObject(s.exportJson()).getJSONObject("bsjProtocolEvidence").getJSONObject("types").has("3"),"receipt evidence exported");
   s.event(1000,new int[]{profile+1},null,null);check(s.snapshot().protocolEvidence.isEmpty()&&s.snapshot().bsjValues.isEmpty(),"profile isolates raw values and receipt evidence");s.newSession();check(s.snapshot().protocolEvidence.isEmpty(),"reconnect clears receipt evidence");
  }
  ProbeState s=new ProbeState();prepare(s,6291908,false);value(s,98,1);value(s,115,2);value(s,132,19);check(s.snapshot().source().equals("FM2")&&s.snapshot().volumeNumber()==19,"logical-only radio and volume retained");value(s,98,2);value(s,113,4);value(s,101,15);value(s,99,1);value(s,100,23);s.event(103,null,null,new String[]{"Logical CD"});check(s.snapshot().cdNumber(113).equals("4")&&s.snapshot().cdNumber(101).equals("15")&&s.snapshot().combinedMediaText().equals("Logical CD"),"logical-only CD fields retained");check(s.snapshot().protocolEvidence.isEmpty(),"logical callbacks never fabricated as raw packets");s.status("test","offline",false);check(s.snapshot().volumeNumber()==null&&s.snapshot().volume().equals("—"),"disconnected volume is not live");
  prepare(s,123456,false);value(s,98,1);value(s,132,9);frame(s,3,0x82,40);frame(s,6,0x10,0x23,0x45);check(s.snapshot().volumeNumber()==9&&s.snapshot().audioSource()==1&&s.snapshot().displayNumber(106)==null,"other profile not decoded as Accord");check(s.snapshot().protocolEvidence.containsKey(3),"receipt evidence independent of interpretation");
 }
 private void recorded()throws Exception{
  if(replay==null)return;String data;try(java.io.InputStream in=getContext().getAssets().open(replay)){java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);data=out.toString("UTF-8");}
  JSONObject capture=new JSONObject(data);JSONArray chunks=capture.getJSONArray("chunks");ProbeState s=new ProbeState();prepare(s,6291908,true);value(s,98,0);value(s,132,27);
  final int[] expected={27,0};BsjFrames verify=new BsjFrames((type,p)->{if(type==3&&p.length==2){expected[0]=p[1]&127;expected[1]++;}});
  for(int i=0;i<chunks.length();i++){String[] bytes=chunks.getString(i).split(" ");int[] raw=new int[bytes.length];for(int k=0;k<raw.length;k++)raw[k]=Integer.parseInt(bytes[k],16);verify.accept(raw,0);s.event(1019,raw,null,null);if(expected[1]>0){check(s.snapshot().volumeNumber()==expected[0],"captured volume at chunk "+i);check(s.snapshot().audioSource()==3&&s.snapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN,"captured AUX source remains stable");}}
  check(expected[1]==36&&s.snapshot().bsjFramesAccepted==246,"complete second-car raw stream replayed");for(int type:new int[]{4,5,6,7,9,127})check(!s.snapshot().protocolEvidence.containsKey(type),"second-car absent type "+type+" not invented");check(s.snapshot().protocolEvidence.get(2).count==24&&s.snapshot().volumeNumber()==27,"captured climate count and final volume");
 }
 private void windows(Context c)throws Exception{
  prepare(live,6291908,false);value(live,98,0);value(live,132,27);frame(live,3,0x81,24);frame(live,5,2,0,1,1,0,0x15,0,0x23);text(live,"Synthetic CD");VolumeOverlay.options(c,true,false);
  main(()->{overlay=new VolumeOverlay(c);overlay.start();});await(()->live.snapshot().overlayVisible,"raw volume opens actual overlay");
  activity=startActivitySync(AudioPlayerActivity.intent(c,AudioMode.CD,false));await(()->((TextView)field(activity,"volume")).getText().toString().endsWith("24"),"actual player reads raw volume");
  frame(live,3,0x81,40);render(overlay);await(()->((TextView)field(activity,"volume")).getText().toString().endsWith("40"),"actual player follows raw volume despite logical27");check(live.snapshot().overlayText.startsWith("40 · "),"actual volume overlay follows raw40");check(live.snapshot().cdNumber(113).equals("2")&&live.snapshot().combinedMediaText().equals("Synthetic CD"),"volume UI leaves CD disc/title intact");main(()->activity.finish());activity=null;main(()->overlay.close());overlay=null;
  activity=startActivitySync(new Intent(c,DebugActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  for(String lang:new String[]{"ru","en"}){main(()->I18n.language(c,lang));render(activity);TextView info=activity.findViewById(android.R.id.content).findViewWithTag("debug-protocol-evidence");check(info!=null&&info.isTextSelectable(),"selectable receipt Debug view "+lang);String label=info.getText().toString();check(label.contains("BSJ 03 → 40")&&label.contains("BSJ 03 → 2"),"debug source/volume show actual transport "+lang);check(label.contains("04 · ")&&label.contains(I18n.t(R.string.debug_data_not_received)),"Debug distinguishes absent radio "+lang);}
  live.status("test","offline",false);render(activity);TextView info=activity.findViewById(android.R.id.content).findViewWithTag("debug-protocol-evidence");check(info.getText().toString().startsWith(I18n.t(R.string.debug_data_history)),"disconnected evidence labelled historical");main(()->activity.finish());activity=null;
 }
 private void restore(SharedPreferences prefs,Map<String,?> saved){SharedPreferences.Editor e=prefs.edit().clear();for(Map.Entry<String,?> item:saved.entrySet()){Object v=item.getValue();if(v instanceof Boolean)e.putBoolean(item.getKey(),(Boolean)v);else if(v instanceof String)e.putString(item.getKey(),(String)v);else if(v instanceof Integer)e.putInt(item.getKey(),(Integer)v);else if(v instanceof Long)e.putLong(item.getKey(),(Long)v);else if(v instanceof Float)e.putFloat(item.getKey(),(Float)v);}e.commit();}
 @Override public void onStart(){Bundle out=new Bundle();int code=0;Context c=getTargetContext();if(!Build.HARDWARE.contains("ranchu")&&!Build.HARDWARE.contains("goldfish")){out.putString("failure","Emulator required");finish(1,out);return;}
  String[] names={"background","overlay","theme","language","radioPresets","carMenuMemory"};Map<String,Map<String,?>> saved=new LinkedHashMap<>();for(String name:names)saved.put(name,new HashMap<>(c.getSharedPreferences(name,0).getAll()));boolean recording=live.recording();
  try{main(()->BackgroundSettings.stop(c));SystemClock.sleep(350);model();recorded();windows(c);out.putString("result","PASS: "+checks+" BSJ/FYT fallback, source isolation, receipt diagnostics and actual player/volume checks"+(replay==null?"":"; second-car raw capture replayed"));}
  catch(Throwable e){code=1;out.putString("failure",android.util.Log.getStackTraceString(e));}
  finally{main(()->{if(activity!=null)activity.finish();if(overlay!=null)overlay.close();});live.running(false);live.processAsHondaAccord(false);live.newSession();live.recording(recording);for(String name:names)restore(c.getSharedPreferences(name,0),saved.get(name));live.processAsHondaAccord(AccordCompatibility.enabled(c));I18n.init(c);}
  finish(code,out);
 }
}
