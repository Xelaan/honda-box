package dev.alex.fyt.probe;
import java.util.HashMap;
/** Desktop timing tests for the production TrackChange class; no Android APK is built. */
public final class TrackChangeHarness {
 static int checks;
 static void expect(String name,String actual,String expected){checks++;if(!java.util.Objects.equals(actual,expected))throw new AssertionError(name+": "+actual+" != "+expected);}
 static ProbeState.Snapshot media(){return new ProbeState.Snapshot();}
 public static void main(String[] args){
  TrackChange d=new TrackChange();ProbeState.Snapshot s=media();
  expect("startup quiet",d.update(s,0,false),null);
  s.track="2";s.text="—";expect("counter waits for title",d.update(s,1000,false),null);
  expect("counter still waits",d.update(s,1600,false),null);
  expect("missing title uses track number",d.update(s,2800,false),"Track 2");
  expect("no duplicate",d.update(s,4000,false),null);
  s.track="3";s.text="—";expect("next counter queued",d.update(s,5000,false),null);
  s.text="Third song";expect("late title before fallback",d.update(s,5700,false),"Third song");
  s.kind=6;s.text="Album";expect("TITLE album rotation quiet",d.update(s,7000,false),null);
  s.kind=7;s.text="Third song";expect("TITLE return quiet",d.update(s,8000,false),null);
  s.text="Fourth song";expect("same counter new song queued",d.update(s,9000,false),null);
  expect("text debounce",d.update(s,10000,false),null);
  expect("stable changed song detected",d.update(s,10300,false),"Fourth song");
  s.track="4";expect("late counter queued",d.update(s,11000,false),null);
  expect("late counter does not duplicate text event",d.update(s,11700,false),null);
  s.mode=AudioMode.USB;s.track="—";s.text="First iPod song";
  expect("source switch waits",d.update(s,12000,false),null);
  expect("source switch with text only",d.update(s,13300,false),"First iPod song");
  s.text="Second song…";expect("text-only track queued",d.update(s,14000,false),null);
  s.text="Second song complete";expect("scroll settles",d.update(s,14800,false),null);
  expect("one event for joined scroll",d.update(s,16100,false),"Second song complete");
  s.text="Second song complete extended";expect("prefix continuation quiet",d.update(s,18000,false),null);
  expect("prefix continuation stays quiet",d.update(s,22000,false),null);
  s.text="Third iPod song";expect("visible player suppresses event",d.update(s,23000,true),null);
  expect("background does not repeat viewed change",d.update(s,25000,false),null);
  s.mode=null;expect("AUX does not announce stale text",d.update(s,26000,false),null);
  s.mode=AudioMode.RADIO;s.n.put(115,1);s.n.put(119,241);s.fresh=false;
  expect("radio waits for fresh frequency",d.update(s,27000,false),null);
  s.fresh=true;expect("radio source transition queued",d.update(s,28000,false),null);
  expect("radio source transition announced",d.update(s,28700,false),"FM1 · 99.50 MHz");
  s.n.put(119,251);expect("station change queued",d.update(s,30000,false),null);
  s.n.put(119,261);expect("seek debounce resets",d.update(s,30400,false),null);
  expect("seek still settling",d.update(s,30900,false),null);
  expect("only final station announced",d.update(s,31100,false),"FM1 · 100.50 MHz");
  s.n.put(115,4);s.n.put(119,9);expect("AM change queued",d.update(s,32000,false),null);
  expect("AM station with kHz",d.update(s,32700,false),"AM1 · 603 kHz");
  s.n.put(116,3);s.n.put(119,10);expect("saved AM station queued",d.update(s,33000,false),null);
  expect("saved AM station number before frequency",d.update(s,33700,false),"AM1 · #3 · 612 kHz");
  s.n.put(115,1);s.n.put(116,6);s.n.put(119,241);expect("saved FM station queued",d.update(s,34000,false),null);
  expect("saved FM station number before frequency",d.update(s,34700,false),"FM1 · #6 · 99.50 MHz");
  s.n.put(116,0);s.n.put(119,251);expect("manual frequency queued",d.update(s,35000,false),null);
  expect("manual frequency has no slot",d.update(s,35700,false),"FM1 · 100.00 MHz");
  s.n.put(116,7);s.n.put(119,261);expect("invalid preset queued",d.update(s,36000,false),null);
  expect("invalid preset is not shown",d.update(s,36700,false),"FM1 · 100.50 MHz");
  s.n.put(116,2);s.stalePreset=true;s.n.put(119,271);expect("stale preset queued",d.update(s,37000,false),null);
  expect("stale preset is not shown",d.update(s,37700,false),"FM1 · 101.00 MHz");s.stalePreset=false;
  s.connected=false;expect("disconnect quiet",d.update(s,38000,false),null);
  s.connected=true;s.sessionId++;expect("reconnect baseline quiet",d.update(s,39000,false),null);
  expect("reconnect no delayed repeat",d.update(s,41000,false),null);
  TrackChange stale=new TrackChange();ProbeState.Snapshot old=media();expect("stale test startup quiet",stale.update(old,0,false),null);
  old.track="2";old.trackValue.sequence=200;expect("new counter with old title queued",stale.update(old,1000,false),null);
  expect("old track title not announced",stale.update(old,1700,false),null);
  expect("old title falls back to new number",stale.update(old,2900,false),"Track 2");
  TrackChange cachedDetector=new TrackChange();ProbeState.Snapshot cached=media();cached.cached=true;cached.track="9";expect("cached standby baseline quiet",cachedDetector.update(cached,0,false),null);cached.track="10";expect("cached standby change quiet",cachedDetector.update(cached,2000,false),null);expect("cached standby has no delayed announcement",cachedDetector.update(cached,4000,false),null);
  TrackChange ipod=new TrackChange();ProbeState.Snapshot p=media();p.mode=AudioMode.USB;p.mediaDevice=ProbeState.MediaDevice.IPOD;p.track="—";p.kind=5;p.text="Song name";
  expect("generic iPod text establishes quiet baseline",ipod.update(p,0,false),null);
  p.text="Artist name";expect("generic iPod artist change ignored",ipod.update(p,1000,false),null);expect("generic iPod artist remains quiet",ipod.update(p,2500,false),null);
  p.text="Album name";expect("generic iPod album change ignored",ipod.update(p,3000,false),null);expect("generic iPod album remains quiet",ipod.update(p,4500,false),null);
  p.text="Song name";expect("return to generic iPod title ignored",ipod.update(p,5000,false),null);expect("generic iPod title remains quiet",ipod.update(p,6500,false),null);
  p.kind=7;expect("track-specific iPod field quiet baseline",ipod.update(p,7000,false),null);p.kind=8;p.text="Artist name";expect("iPod artist field quiet",ipod.update(p,8000,false),null);p.kind=6;p.text="Album name";expect("iPod album field quiet",ipod.update(p,9000,false),null);p.kind=7;p.text="Song name";expect("same iPod title returns quietly",ipod.update(p,10000,false),null);
  p.text="New song";expect("track-specific new iPod title settles",ipod.update(p,11000,false),null);p.kind=8;p.text="New artist";expect("TITLE switch during settling does not show artist",ipod.update(p,12300,false),null);expect("artist remains quiet after settling",ipod.update(p,12500,false),null);p.kind=7;p.text="New song";expect("pending announcement keeps the selected track title",ipod.update(p,12700,false),"New song");
  TrackChange folder=new TrackChange();ProbeState.Snapshot f=media();f.mode=AudioMode.USB;f.track="—";f.kind=4;f.text="Folder A";expect("folder baseline quiet",folder.update(f,0,false),null);f.text="Folder B";expect("folder alone is not a track change",folder.update(f,1000,false),null);expect("folder alone has no delayed announcement",folder.update(f,2400,false),null);
  f.kind=5;f.text="first.mp3";expect("USB filename quiet baseline",folder.update(f,3000,false),null);f.text="second.mp3";expect("USB filename change still settles",folder.update(f,4000,false),null);expect("USB filename change still announces",folder.update(f,5300,false),"second.mp3");
  TrackChange live=new TrackChange();ProbeState.Snapshot heartbeat=media();expect("live counter baseline quiet",live.update(heartbeat,0,false),null);heartbeat.text="Alternate TITLE text";heartbeat.trackValue.sequence++;expect("same active counter suppresses text candidate",live.update(heartbeat,3000,false),null);heartbeat.trackValue.sequence++;expect("counter heartbeat still suppresses changed text",live.update(heartbeat,4500,false),null);heartbeat.trackValue.sequence++;expect("continued timer packets keep same track quiet",live.update(heartbeat,6000,false),null);
  heartbeat.track="2";heartbeat.trackValue.sequence++;heartbeat.titleValue.sequence=heartbeat.trackValue.sequence;heartbeat.text="Actual next song";expect("new live counter still queues",live.update(heartbeat,7000,false),null);expect("new live counter still announces",live.update(heartbeat,7700,false),"Actual next song");
  System.out.println("PASS: "+checks+" track/station timing checks");
 }
}
// Minimal collaborators expose the same Snapshot surface used by the production detector.
class ProbeState {
 enum MediaDevice {UNKNOWN,USB,IPOD}
 static class Value {long sequence=100;}
 static class Snapshot {
  Value bsjFolder,bsjFilename;HashMap<Integer,Value> bsjExtraTexts=new HashMap<>();
  Value trackValue=new Value(),titleValue=new Value();
  boolean connected=true,fresh=true,stalePreset,cached;long sessionId=1;AudioMode mode=AudioMode.CD;
  MediaDevice mediaDevice=MediaDevice.UNKNOWN;
  String track="1",disc="1",text="First song";Integer kind=7;
  HashMap<Integer,Integer> n=new HashMap<>();
  Integer number(int code){return code==1000?6291908:n.get(code);}
  Integer displayNumber(int code){return n.get(code);}
  boolean fresh(int code){return fresh&&!(code==116&&stalePreset);}
  String cdNumber(int code){return code==101?track:disc;}
  String combinedMediaText(){return text;}
  boolean usingStandbyText(){return cached;}
  boolean usingStandbyTrack(){return cached;}
  Integer currentTitleField(){if(kind!=null)bsjExtraTexts.put(kind,titleValue);return kind;}
  Value reading(int code){return code==101?trackValue:titleValue;}
  String text(int code){return text;}
  String radioBand(){return n.get(115)==4?"AM1":"FM1";}
 }
}
enum AudioMode {RADIO,CD,USB,IPOD;static AudioMode reported(ProbeState.Snapshot s){return s.mode;}}
class I18n {static String t(int id){return id==1?"kHz":id==2?"MHz":"Track ";}}
class R {static class string {static final int radio_khz=1,radio_mhz=2,ui_track=3;}}
