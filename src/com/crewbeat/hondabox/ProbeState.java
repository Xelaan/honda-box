package com.crewbeat.hondabox;

import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ProbeState {
    public static final ProbeState INSTANCE = new ProbeState();
    public final AudioRouteCapture audioRoutes = new AudioRouteCapture();
    public final CarMenuMemory carMenuMemory = new CarMenuMemory();
    public enum MediaDevice {UNKNOWN,USB,IPOD}
    public final RadioPresets radioPresets = new RadioPresets();
    public static final class Value {
        public final int[] ints; public final float[] floats; public final String[] strings; public final long at; public long sequence;
        Value(int[] i, float[] f, String[] s) {
            ints = i == null ? null : i.clone(); floats = f == null ? null : f.clone();
            strings = s == null ? null : s.clone(); at = System.currentTimeMillis();
        }
        JSONObject json() throws Exception {
            JSONObject j = new JSONObject().put("receivedAtMs", at);
            j.put("ints", ints == null ? JSONObject.NULL : new JSONArray(ints));
            j.put("floats", floats == null ? JSONObject.NULL : new JSONArray(floats));
            j.put("strings", strings == null ? JSONObject.NULL : new JSONArray(strings)); return j;
        }
    }
    public static final class Snapshot {
        public boolean running, connected, commandBusy, dualAvailable;public int auxSource=SourceSettings.DEFAULT_AUX;public String auxName="AUX";
        public java.util.List<Value> bsjDisplayTexts=new java.util.ArrayList<>();
        public Map<Integer,Value> bsjExtraTexts=new LinkedHashMap<>();
        public Value bsjFilename,bsjFolder,bsjTitleField;public boolean bsjTextLost;public String bsjTextEncoding;
        public Boolean bsjAudioEnabled;public long bsjAudioPowerRevision;
        public BsjStandbyDisplay standbyDisplay;
        public BsjStandbyDisplay standbyText,standbyCounters;
        public Value bsjMediaTextState;
        public long mediaTrackStartedNanos;
        public BsjVehiclePackets.Sample vehiclePacket0E,vehiclePacket10;
        public Integer bsjSource;public long bsjFramesAccepted,bsjFramesRejected;public Map<Integer,Value> bsjValues=new LinkedHashMap<>();
        public Map<String,RadioPresets.Entry> presets=new LinkedHashMap<>();public Integer mainChannel;
        public Integer preset(int slot){Integer band=displayNumber(115);RadioPresets.Entry e=band==null?null:presets.get(band+":"+slot);return !connected||!Integer.valueOf(6291908).equals(number(1000))||e==null?null:e.index;}
        public Map<String,String> menuValues=new LinkedHashMap<>();
        public String receivedPlayMode="";
        public MediaDevice manualMediaDeviceHint=MediaDevice.UNKNOWN;public MediaDevice mediaDevice=MediaDevice.UNKNOWN;public long mediaDeviceRevision;public String mediaDeviceEvidence="",cachedMenu="";
        public java.util.List<String> browseItems=new java.util.ArrayList<>();
        public long menuRevision,menuContentSequence,menuVisibilitySequence;public boolean carMenuOverlayVisible;public String carMenuOverlayStatus,carMenuOverlayText;
        public long mediaSourceSequence=-1;public long climateRevision,optionalClimateSeen,sourceSequence;public boolean audioPlayerVisible;public String audioPlayerStatus,audioPlayerMode;public boolean climateStripCollapsed;public boolean climateStripVisible;public String climateStripText;public boolean climateOverlayVisible;public int climateOverlayHeight,overlayBottomPx;public String climateOverlayStatus,climateOverlayText; public String status, detail, backend, commandStatus;
        public long count, lastEvent, sessionId, rawCount, rawDropped;
        public boolean trackOverlayVisible;public boolean sourceOverlayVisible; public String sourceOverlayStatus,sourceOverlayText; public boolean overlayVisible; public String overlayStatus, overlayText; public Map<Integer, Value> values; public String log;
        public Integer number(int code) { Value v = values.get(code); return v == null || v.ints == null || v.ints.length == 0 ? null : v.ints[0]; }
        public String text(int code) {
            Value v = code>=103&&code<=105?reading(code):values.get(code);
            if (v == null || v.strings == null || v.strings.length == 0 || v.strings[0] == null) return "—";
            String t = MediaText.display(v.strings[0]);
            return t.isEmpty() ? "—" : t;
        }
        public boolean honda() { Integer p = number(1000); return p != null && (p == 6291908 || p == 3604932 || p == 6357444 || p == 12321220 || p == 9175479); }
        public Integer rawAudioSource(){return bsjSource!=null&&Integer.valueOf(6291908).equals(number(1000))?bsjSource:number(98);}
        public Integer audioSource(){Integer n=rawAudioSource();if(Integer.valueOf(6291908).equals(number(1000))){
            if(java.util.Objects.equals(n,auxSource))return n;
            if(mediaDevice==MediaDevice.IPOD&&mediaDeviceEvidence.startsWith("Inferred from BSJ marker06")&&(!Integer.valueOf(2).equals(n)&&!Integer.valueOf(4).equals(n)))return auxSource==2?4:2;
            if(Integer.valueOf(1).equals(n)&&mediaDevice==MediaDevice.USB&&(mediaDeviceEvidence.startsWith("Inferred from owner-confirmed USB playback05")||mediaDeviceEvidence.startsWith("Inferred from BSJ marker06 pending")))return 4;
        }return n;}
        public Value reading(int code){Value v=bsjValues.get(code);if(v!=null&&code>=135&&code<=138&&v.sequence<=sourceSequence){Value logical=values.get(code);if(logical!=null&&logical.sequence>sourceSequence)return logical;}return v==null?values.get(code):v;}
        public Integer displayNumber(int code){Value v=reading(code);return v==null||v.ints==null||v.ints.length==0?null:v.ints[0];}
        private String ipodSource(){return mediaDeviceEvidence.startsWith("Inferred from BSJ")?"iPod":"iPod · "+I18n.t(R.string.media_by_menu);}
        public String source() {
            Integer n = audioSource(); if (!connected) return "—";if(n==null&&mediaDevice==MediaDevice.USB)return "USB";if(n==null)return mediaDevice==MediaDevice.IPOD?ipodSource():"—";
            if (!honda()) return I18n.t(R.string.ui_code) + n;
            if (Integer.valueOf(6291908).equals(number(1000))&&n==auxSource) return auxName;
            switch (n) {
                case 0: if(mediaDevice==MediaDevice.USB)return "USB";return mediaDevice==MediaDevice.IPOD?ipodSource():I18n.t(R.string.ui_not_reported);
                case 1: return number(115) == null ? "Radio" : radioBand();
                case 2: if(mediaDevice==MediaDevice.USB)return "USB";return mediaDevice==MediaDevice.IPOD?ipodSource():"CD"; case 3: case 4:return n==auxSource?auxName:mediaDevice==MediaDevice.IPOD?ipodSource():"USB"; default: return I18n.t(R.string.ui_code) + n;
            }
        }
        public String radioBand() {
            Integer band = displayNumber(115); if (!connected || band == null) return "—";
            if (!honda()) return "raw " + band;
            switch (band) {
                case 0: return "FM"; case 1: return "FM1"; case 2: return "FM2"; case 3: return "AM";
                // Confirmed by the owner's physical band cycle for this Honda profile.
                case 4: return number(1000) == 6291908 ? "AM1" : "MW";
                case 5: return number(1000) == 6291908 ? "AM2" : "LW";
                default: return I18n.t(R.string.ui_code) + band;
            }
        }
        public String raw(int code) { Integer n = number(code); return !connected || n == null ? "—" : String.valueOf(n); }
        public String flag(int code) {
            Integer n = code==10?climatePower():number(code); if (!connected || n == null) return "—";
            return n == 0 ? I18n.t(R.string.ui_off) : n == 1 ? I18n.t(R.string.ui_on) : "raw " + n;
        }
        public String climate() {
            if (!connected) return "—";
            if (!honda()) return "raw [10,11,13,18,19,20]: " + raw(10) + " / " + raw(11) + " / " + raw(13) + " / " + raw(18) + " / " + raw(19) + " / " + raw(20);
            return I18n.t(R.string.ui_power) + flag(10) + "    A/C: " + flag(11) + "    AUTO: " + flag(13)
                + I18n.t(R.string.ui_nwindshield) + flag(18) + I18n.t(R.string.ui_face) + flag(19) + I18n.t(R.string.ui_feet) + flag(20);
        }
        public String media() {
            if (!connected) return "—";
            Integer min = number(99), sec = number(100);
            String time = min == null || sec == null ? "—" : String.format(Locale.US, "%02d:%02d", min, sec);
            String bcd = min == null || sec == null || bcd(min) == null || bcd(sec) == null || bcd(sec) > 59
                ? I18n.t(R.string.ui_invalid_bcd) : String.format(Locale.US, "%02d:%02d", bcd(min), bcd(sec));
            Integer source = audioSource();
            if (honda() && source != null && source == 4) time = bcd + " (BCD USB)";
            else if (honda() && source != null && source == 2) time += " (CD)";
            else time = "DEC " + time + " / BCD " + bcd + I18n.t(R.string.ui_source_unknown);
            Integer track = number(101);
            String trackText = raw(101);
            if (honda() && source != null && source == 4 && track != null)
                trackText = bcd(track) == null ? "raw " + track : bcd(track) + " (BCD USB, raw " + track + ")";
            else if (honda() && (source == null || (source != 2 && source != 4)) && track != null && bcd(track) != null)
                trackText += " / BCD " + bcd(track);
            return I18n.t(R.string.ui_disc_113) + raw(113) + I18n.t(R.string.ui_track_101) + trackText + I18n.t(R.string.ui_time_99_100) + time
                + I18n.t(R.string.ui_ndisc_mode_114) + raw(114) + "    Repeat [102]: " + raw(102)
                + I18n.t(R.string.ui_ntitle) + text(103) + I18n.t(R.string.ui_album_270) + text(104) + I18n.t(R.string.ui_artist_272) + text(105)
                + I18n.t(R.string.ui_nslots_106_111) + raw(106) + " / " + raw(107) + " / " + raw(108) + " / " + raw(109) + " / " + raw(110) + " / " + raw(111)
                + I18n.t(R.string.ui_nlast_media_event) + lastFor(99, 114) + I18n.t(R.string.ui_these_fields_may_persist_after);
        }
        private Integer bcd(int n) { return n < 0 || n > 255 || (n & 15) > 9 || (n >> 4) > 9 ? null : (n >> 4) * 10 + (n & 15); }
        private String lastFor(int from, int to) {
            long at = 0; for (int code = from; code <= to; code++) { Value v = values.get(code); if (v != null) at = Math.max(at, v.at); }
            return at == 0 ? "—" : String.format(Locale.getDefault(), "%tT", at);
        }
        public String allFields() {
            if (!connected) return I18n.t(R.string.ui_current_values_unavailable_296);
            StringBuilder b = new StringBuilder();
            for (Map.Entry<Integer, Value> e : new java.util.TreeMap<>(values).entrySet()) {
                Value v = e.getValue(); b.append('[').append(e.getKey()).append("] ");
                try { b.append(v.json().toString()); } catch (Exception ignored) { b.append('?'); }
                b.append('\n');
            }
            return b.toString();
        }
        public String temperature(int code) {
            Integer n = number(code); if (!connected || n == null) return "—";
            if (!honda()) return "raw " + n;
            if (n == 0 || n == -1) return "—"; if (n == -2) return "LOW"; if (n == -3) return "HI";
            return String.format(Locale.US, "%.1f°", n == 20 ? 18f : n / 2f);
        }
        public boolean optionalKnown(int code){return connected&&(optionalClimateSeen&(1L<<code))!=0;}
        public Integer climatePower(){
            Integer fan=number(21);
            if(connected&&Integer.valueOf(6291908).equals(number(1000))&&fan!=null&&fan>=0&&fan<=7)return fan==0?0:1;
            return number(10);
        }
        public int fanSegments(){Integer fan=number(21);return !connected||fan==null||fan<0||fan>7?0:fan;}
        public static Integer packedBcd(int n){if(n<0||n>0xffff)return null;int result=0,place=1;do{int digit=n&15;if(digit>9)return null;result+=digit*place;place*=10;n>>>=4;}while(n!=0);return result;}
        private long mediaEpoch(){return mediaSourceSequence<0?sourceSequence:mediaSourceSequence;}
        public boolean fresh(int code){Value v=reading(code);return connected&&v!=null&&v.sequence>(code>=99&&code<=114?mediaEpoch():sourceSequence);}
        private String normalCdNumber(int code){Value v=reading(code);Integer n=displayNumber(code);if(!fresh(code)||n==null||n<1)return "—";if(code==101&&bsjValues.containsKey(code))n=packedBcd(n);return n==null?"—":String.valueOf(n);}
        private String normalCdTime(){Integer min=displayNumber(99),sec=displayNumber(100);if(normalCdNumber(101).equals("—")||!fresh(99)||!fresh(100)||min==null||sec==null)return "—";if(bsjValues.containsKey(99)){min=packedBcd(min);sec=packedBcd(sec);}return min==null||sec==null||min<0||sec<0||sec>59?"—":String.format(Locale.US,"%02d:%02d",min,sec);}
        private boolean standbyCountersUsable(){
            if(!connected||standbyCounters==null||standbyCounters.track<1)return false;
            String track=normalCdNumber(101);
            return track.equals("—")||track.equals(String.valueOf(standbyCounters.track));
        }
        public boolean usingStandbyTrack(){return normalCdNumber(101).equals("—")&&standbyCountersUsable();}
        public String cdNumber(int code){String normal=normalCdNumber(code);return code==101&&usingStandbyTrack()?String.valueOf(standbyCounters.track):normal;}
        public String cdTime(){
            String normal=normalCdTime();
            return normal.equals("—")&&standbyCountersUsable()?String.format(Locale.US,"%02d:%02d",standbyCounters.minutes,standbyCounters.seconds):normal;
        }
        public boolean usingStandbyTime(){return normalCdTime().equals("—")&&standbyCountersUsable();}
        private String standbyDisplayText(){
            if(!connected||standbyText==null)return "—";
            String text=standbyText.text;
            // Never show the trailing bytes of a numeric layout as a song name.
            // A numbered text prefix must agree with the current track when known.
            if(text.matches("^\\d{3} .+")){
                String track=cdNumber(101);
                if(!track.equals("—")&&Integer.parseInt(text.substring(0,3))!=Integer.parseInt(track))return "—";
            }
            return text.isEmpty()?"—":MediaText.display(text);
        }
        public boolean usingStandbyText(){return normalCombinedMediaText().equals("—")&&!standbyDisplayText().equals("—");}
        public String combinedMediaText(){String normal=normalCombinedMediaText();return normal.equals("—")?standbyDisplayText():normal;}
        /** Owner/manual-supported LOADING candidate; never replaces a received current title. */
        public boolean mediaLoading(){return connected&&bsjMediaTextState!=null&&bsjMediaTextState.sequence>mediaEpoch()&&bsjMediaTextState.ints[0]==0&&combinedMediaText().equals("—");}
        public boolean cdTrackIntro(long nowNanos){long age=nowNanos-mediaTrackStartedNanos;return connected&&mediaTrackStartedNanos!=0&&!cdNumber(101).equals("—")&&age>=0&&age<2000000000L;}
        private String mediaValue(Value v){if(!connected||v==null||v.sequence<=mediaEpoch()||v.strings==null||v.strings.length==0)return "—";String text=MediaText.display(v.strings[0]);return text.isEmpty()?"—":text;}
        public Integer currentTitleField(){return !connected||bsjTitleField==null||bsjTitleField.sequence<=mediaEpoch()?null:bsjTitleField.ints[0];}
        public String mediaFilename(){return mediaValue(bsjFilename);}
        public String mediaFolder(){return mediaValue(bsjFolder);}
        public String cdTitle(){String title=fresh(103)?text(103):"—";if(!title.equals("—"))return title;String filename=mediaFilename();MediaName split=MediaName.parse(filename);return split==null?filename:split.title;}
        private MediaName mediaSplit(){MediaName file=MediaName.parse(mediaFilename());return file!=null?file:displayTextOnly()?MediaName.parse(text(104)):null;}
        public String cdArtist(){String artist=fresh(105)?text(105):"—";if(!artist.equals("—"))return artist;MediaName split=mediaSplit();return split==null?"—":split.artist;}
        public boolean splitInferred(){return mediaSplit()!=null&&(!(fresh(103)&&!text(103).equals("—"))||!(fresh(105)&&!text(105).equals("—")));}
        /** Show only the OEM display field currently selected through TITLE. */
        private String normalCombinedMediaText(){
            Integer kind=currentTitleField();Value selected=null;
            if(kind!=null){switch(kind){
                case 0:selected=bsjValues.get(104);break;
                case 1:selected=bsjValues.get(103);break;
                case 3:selected=bsjValues.get(105);break;
                case 4:selected=bsjFolder;break;
                case 5:selected=bsjFilename;break;
                case 6:case 7:case 8:selected=bsjExtraTexts.get(kind);break;
                default:return "—";
            }
                Value work=bsjValues.get(114);
                if(work!=null&&work.sequence>mediaEpoch()&&(work.ints[0]==9||work.ints[0]==10)&&(selected==null||selected.sequence<=work.sequence))return "—";
                return mediaValue(selected);
            }
            // Legacy callbacks without a raw TITLE selector still show one primary field.
            String title=cdTitle();return !title.equals("—")?title:fresh(104)?text(104):"—";
        }
        public String playMode(){if(!receivedPlayMode.isEmpty())return receivedPlayMode;if(!fresh(102))return "";Integer n=displayNumber(102);String[] modes={"NORMAL PLAY","Repeat one","Repeat all discs","Random","Scan","Scan disc","Repeat folder","Random folder","Scan folder"};return n!=null&&n>=0&&n<modes.length&&(mediaDevice!=MediaDevice.IPOD||n==0||n==1||n==3)?modes[n]:"";}
        public String mediaPrimary(){String title=cdTitle();if(!title.equals("—"))return title;String display=fresh(104)?text(104):"—";MediaName split=MediaName.parse(display);return split==null?display:split.title;}
        public String recentDisplayTexts(){if(fresh(104))return text(104);StringBuilder b=new StringBuilder();for(Value v:bsjDisplayTexts){if(v.sequence<=mediaEpoch())continue;String value=mediaValue(v);if(value.equals("—"))continue;if(b.length()>0)b.append(" / ");b.append(value);}return b.length()==0?"—":b.toString();}
        public boolean displayTextOnly(){return cdTitle().equals("—")&&fresh(104)&&!text(104).equals("—");}
        public boolean dualInferred(){return connected&&!dualAvailable&&Integer.valueOf(6291908).equals(number(1000))&&!temperature(27).equals("—")&&!temperature(28).equals("—");}
        public String dual(){Integer v=number(14);if(!connected)return "—";if(dualAvailable&&v!=null)return v==0?I18n.t(R.string.ui_off):v==1?I18n.t(R.string.ui_on):"?";return dualInferred()?(temperature(27).equals(temperature(28))?I18n.t(R.string.ui_off):I18n.t(R.string.ui_on)):"—";}
        public String volume() { Integer n = number(132); return !connected || n == null ? "—" : String.valueOf(n); }
        public boolean carMenuSupported(){Integer p=number(1000);return p!=null&&(p==6291908||p==3604932||p==6357444||p==12321220);}
        public String menuRow(int index){if(!connected||index<0||index>2)return "";Value v=reading(135+index);return sourceSequence>0&&v!=null&&v.sequence<=sourceSequence?"":cleanMenuText(v);}
        public boolean hasMenuRows(){return !menuRow(0).isEmpty()||!menuRow(1).isEmpty()||!menuRow(2).isEmpty();}
        public String menu() {return !connected?"—":(menuRow(0).isEmpty()?"—":menuRow(0))+"  /  "+(menuRow(1).isEmpty()?"—":menuRow(1))+"  /  "+(menuRow(2).isEmpty()?"—":menuRow(2));}
    }
    private boolean running, connected,recording;
    public synchronized boolean recording(){return recording;}
    public synchronized void recording(boolean on){if(recording==on)return;recording=on;audioRoutes.recording(on);if(on&&connected){Value p=values.get(1000);analyzer.begin(sessionId,p==null||p.ints==null||p.ints.length==0?-1:p.ints[0],System.currentTimeMillis());}else if(!on)analyzer.end(System.currentTimeMillis());log(on?"DIAGNOSTIC RECORDING: ON":"DIAGNOSTIC RECORDING: OFF");}private int auxSource=SourceSettings.DEFAULT_AUX;private String auxName="AUX";
    public synchronized void auxName(String name){auxName=SourceSettings.cleanAuxName(name);}
    public synchronized void auxSource(int source){if(source==-1)source=SourceSettings.DEFAULT_AUX;if(auxSource!=source){auxSource=source;sourceSequence=count;}}
    private String status = I18n.t(R.string.ui_ready_to_check), detail = I18n.t(R.string.ui_connect_on_a_fyt_dudu), backend = I18n.t(R.string.ui_com_syu_ms_not_checked);
    private long mediaSourceSequence=-1,volumeChangedAt;private Integer mediaPlaybackSource;
    // The display may keep a CD carrier while USB is selected. Keep that source context
    // through NO DATA so unplugging a flash drive cannot enter the immediate CD path.
    private boolean usbSourceContext;
    private long count, lastEvent, sessionId,climateRevision,optionalClimateSeen,sourceSequence;private boolean dualAvailable;
    private final ArrayDeque<Value> bsjDisplayTexts=new ArrayDeque<>();
    private final Map<Integer,Value> bsjExtraTexts=new LinkedHashMap<>();
    private Value bsjFilename,bsjFolder,bsjTitleField,bsjMediaTextState;private long mediaTrackStartedNanos;private boolean bsjTextLost;private String bsjTextEncoding="";
    private Boolean bsjAudioEnabled;private long bsjAudioPowerRevision;
    private BsjStandbyDisplay standbyDisplay;
    private final BsjVehiclePackets vehiclePackets=new BsjVehiclePackets();
    private static final class StandbyMedia {
        BsjStandbyDisplay text,counters;
    }
    // Bounded source/device cache. Normal playback always has priority over standby data.
    private final Map<String,StandbyMedia> standbyMedia=new LinkedHashMap<>();
    private static String standbyKey(Snapshot s){
        if(!s.connected||!Integer.valueOf(6291908).equals(s.number(1000)))return null;
        AudioMode mode=AudioMode.reported(s);
        return mode==AudioMode.CD?"CD":mode==AudioMode.USB?(s.mediaDevice==MediaDevice.IPOD?"IPOD":"USB"):null;
    }
    private void captureStandby(BsjStandbyDisplay display){
        String key=standbyKey(overlaySnapshot());if(key==null)return;
        if(display.layout!=BsjStandbyDisplay.Layout.TEXT&&display.layout!=BsjStandbyDisplay.Layout.POSSIBLE_PLAYBACK)return;
        StandbyMedia cache=standbyMedia.get(key);if(cache==null){cache=new StandbyMedia();standbyMedia.put(key,cache);}
        if(display.layout==BsjStandbyDisplay.Layout.TEXT)cache.text=display;
        else if(display.track>0)cache.counters=display;
        while(standbyMedia.size()>3)standbyMedia.remove(standbyMedia.keySet().iterator().next());
    }
    private void copyStandby(Snapshot s){
        s.bsjMediaTextState=bsjMediaTextState;
        s.mediaTrackStartedNanos=mediaTrackStartedNanos;
        StandbyMedia cache=standbyMedia.get(standbyKey(s));
        if(cache!=null){s.standbyText=cache.text;s.standbyCounters=cache.counters;}
    }
    private Integer bsjSource;private final Map<Integer,Value> bsjValues=new LinkedHashMap<>();
    public final UnknownMessages unknownMessages=new UnknownMessages();
    public final BsjAnalyzer analyzer=new BsjAnalyzer();
    private final BsjIpodInference ipodInference=new BsjIpodInference();
    private final BsjFrames bsj=new BsjFrames((command,payload,at)->receiveBsjFrame(command,payload,at));
    private void receiveBsjFrame(int command,int[] payload,long at){
        if(recording)analyzer.frame(command,payload,at);unknownMessages.accept(command,payload,at);
        Value profile=values.get(1000);
        if(profile!=null&&profile.ints!=null&&profile.ints.length>0&&profile.ints[0]==6291908)vehiclePackets.accept(command,payload,at);
        if(command==0x0C&&profile!=null&&profile.ints!=null&&profile.ints.length>0&&profile.ints[0]==6291908)
            {standbyDisplay=new BsjStandbyDisplay(payload,at);captureStandby(standbyDisplay);}
        Value bandValue=bsjValues.get(115);if(bandValue==null)bandValue=values.get(115);
        Integer band=bandValue==null||bandValue.ints==null||bandValue.ints.length==0?null:bandValue.ints[0];
        Integer candidateSource=effectiveSource();boolean aux=java.util.Objects.equals(candidateSource,auxSource);
        if(!aux&&(usbSourceContext||mediaDevice==MediaDevice.USB))candidateSource=4;
        // Power/standby traffic can contain marker06 while AUX remains selected.
        // AUX is authoritative until a real source transition; it must not launch media.
        if(aux){ipodInference.reset();usbSourceContext=false;if(mediaDevice!=MediaDevice.UNKNOWN)resetMediaDevice();}
        if(profile!=null&&profile.ints!=null&&profile.ints.length>0&&profile.ints[0]==6291908&&!aux&&ipodInference.accept(command,payload,at,candidateSource,band)&&mediaDevice!=MediaDevice.IPOD){
            boolean pending=ipodInference.pendingMarker();
            boolean confirming=mediaDevice==MediaDevice.USB&&mediaDeviceEvidence.startsWith("Inferred from BSJ marker06 pending");
            if(!confirming){resetMediaText();for(int code=99;code<=114;code++)bsjValues.remove(code);mediaSourceSequence=count;mediaPlaybackSource=null;}
            mediaDevice=pending?MediaDevice.USB:MediaDevice.IPOD;if(pending)usbSourceContext=true;mediaDeviceRevision++;
            mediaDeviceEvidence=pending?"Inferred from BSJ marker06 pending device: default USB until title evidence without playback counters confirms iPod":ipodInference.fromMarker()?(confirming?(ipodInference.fromStandbyTitle()?"Inferred from BSJ marker06: paused iPod confirmed by work11 and empty TITLE heartbeat without playback counters":"Inferred from BSJ marker06: USB replacement confirmed by live text without playback counters"):"Inferred from BSJ marker06: immediate iPod selection from a non-USB source"):"Inferred from BSJ stream transition: CD playback05 stopped, changer06 has only states2/3, live text07 continued";
            log("MEDIA DEVICE: "+mediaDevice+" · "+mediaDeviceEvidence);
        }
        bsjFrame(command,payload);
    }
    private Integer effectiveSource(){if(bsjSource!=null)return bsjSource;Value v=values.get(98);return v==null||v.ints==null||v.ints.length==0?null:v.ints[0];}
    private void sourceTransition(Integer next,long sequence){
        Integer prior=effectiveSource();Value track=bsjValues.get(101);
        boolean confirmation=next!=null&&next>0&&(prior==null||prior==0)&&java.util.Objects.equals(mediaPlaybackSource,next)&&track!=null&&track.sequence>(mediaSourceSequence<0?sourceSequence:mediaSourceSequence);
        boolean markerConfirmation=(mediaDevice==MediaDevice.IPOD||mediaDevice==MediaDevice.USB)&&mediaDeviceEvidence.startsWith("Inferred from BSJ marker06")
            &&(prior==null||prior>=0&&prior<=4)&&!java.util.Objects.equals(next,auxSource)&&(Integer.valueOf(2).equals(next)||Integer.valueOf(4).equals(next));
        sourceSequence=sequence;
        if(!confirmation&&!markerConfirmation){usbSourceContext=Integer.valueOf(4).equals(next)&&next!=auxSource;mediaSourceSequence=sequence;mediaPlaybackSource=null;resetMediaText();resetMediaDevice();}
        else log("MEDIA: source confirmation retains compatible playback/device; source="+next);
    }
    private boolean recentVolumeChange(){long age=System.currentTimeMillis()-volumeChangedAt;return volumeChangedAt>0&&age>=0&&age<=3000;}
    private boolean retainMediaDuringVolume(){
        Snapshot current=overlaySnapshot();Integer work=current.displayNumber(114);AudioMode mode=AudioMode.reported(current);
        return recentVolumeChange()&&(mode==AudioMode.CD||mode==AudioMode.USB)&&current.connected&&current.fresh(101)
            &&!current.cdNumber(101).equals("—")&&!Integer.valueOf(9).equals(work)&&!Integer.valueOf(10).equals(work);
    }
    private void bsjValue(int code,int n){
        if(code==132){Value previous=bsjValues.get(code);if(previous==null)previous=values.get(code);if(previous==null||previous.ints==null||previous.ints.length==0||previous.ints[0]!=n)volumeChangedAt=System.currentTimeMillis();}
        Value v=new Value(new int[]{n},null,null);v.sequence=count+1;bsjValues.put(code,v);}
    private static String cleanMenuText(Value v){if(v==null||v.strings==null||v.strings.length==0||v.strings[0]==null)return "";String t=v.strings[0].replaceAll("[\\p{Cntrl}]"," ").trim();return t.length()>128?t.substring(0,128):t;}
    private long menuRevision,menuContentSequence,menuVisibilitySequence;
    private String menuRowsKey(){StringBuilder b=new StringBuilder();for(int code=135;code<=137;code++){Value v=bsjValues.get(code);if(v==null||v.sequence<=sourceSequence)v=values.get(code);b.append(sourceSequence>0&&v!=null&&v.sequence<=sourceSequence?"":cleanMenuText(v)).append('\u0000');}return b.toString();}
    private Integer menuSignal(){Value v=bsjValues.get(139);if(v==null)v=values.get(139);return v==null||v.ints==null||v.ints.length==0?null:v.ints[0];}
    private void trackMenu(String rowsBefore,Integer showBefore,boolean visibilityEvent){
        boolean rows=!rowsBefore.equals(menuRowsKey()),show=!java.util.Objects.equals(showBefore,menuSignal());
        // A repeated explicit close still closes rows received since the previous close.
        // Match stock DataPack: later text may show the menu again; zero is an event, not a permanent gate.
        boolean close=visibilityEvent&&Integer.valueOf(0).equals(menuSignal())&&(rows||menuContentSequence>menuVisibilitySequence);
        if(rows||show||close){menuRevision++;if(rows)menuContentSequence=menuRevision;if(show||close)menuVisibilitySequence=menuRevision;}
    }
    private MediaDevice manualMediaDeviceHint=MediaDevice.UNKNOWN;
    public synchronized void manualMediaDeviceHint(MediaDevice device){manualMediaDeviceHint=device==null?MediaDevice.UNKNOWN:device;}
    private MediaDevice mediaDevice=MediaDevice.UNKNOWN;private long mediaDeviceRevision;private String mediaDeviceEvidence="";
    private final java.util.Set<String> ipodMenuCategories=new java.util.HashSet<>();
    private void resetMediaDevice(){ipodInference.reset();manualMediaDeviceHint=MediaDevice.UNKNOWN;ipodMenuCategories.clear();if(mediaDevice!=MediaDevice.UNKNOWN){mediaDevice=MediaDevice.UNKNOWN;mediaDeviceRevision++;}mediaDeviceEvidence="";}
    private final Map<Integer,String> playModeLabels=new LinkedHashMap<>();
    private final MenuValues menuValues=new MenuValues();
    private final MenuBrowser menuBrowser=new MenuBrowser();
    private void receivedMenu(String[] rows){Integer source=effectiveSource();Value profile=values.get(1000);if(profile==null||profile.ints==null||profile.ints.length==0||profile.ints[0]!=6291908)return;menuValues.accept(source,rows);menuBrowser.accept(source,rows);for(String row:rows){String label=MenuListModel.label(row);if(!Integer.valueOf(1).equals(source)&&label.matches("(?:NORMAL PLAY|REPEAT .+|RANDOM .+|SCAN .+|SHUFFLE .+)"))playModeLabels.put(source,row.trim());}
        carMenuMemory.record(6291908,source,rows,System.currentTimeMillis());if(source!=null&&((source!=0&&source!=2&&source!=4)||source==auxSource))return;
        MediaDevice next=mediaDevice;String evidence=mediaDeviceEvidence;
        for(String row:rows){String label=row.trim().toLowerCase(java.util.Locale.ROOT).replaceFirst("^\\d{1,3}\\s*:\\s*", "").replaceFirst("\\s*[>…]\\s*$", "");if(label.equals("ipod")||label.equals("usb ipod")||label.equals("shuffle off")||label.equals("shuffle all")||label.equals("shuffle album")){next=MediaDevice.IPOD;evidence="iPod-specific menu label: "+label;}
            if(label.equals("playlists")||label.equals("artists")||label.equals("albums")||label.equals("songs"))ipodMenuCategories.add(label);
            if(Integer.valueOf(4).equals(source)&&(label.equals("usb")||label.equals("folder")||label.equals("folders"))){next=MediaDevice.USB;evidence="Folder navigation menu";ipodMenuCategories.clear();}}
        if(ipodMenuCategories.size()>=2||ipodMenuCategories.contains("artists")||ipodMenuCategories.contains("playlists")||ipodMenuCategories.contains("albums")){next=MediaDevice.IPOD;evidence="Inferred from current-source Honda iPod menu categories: "+new java.util.TreeSet<>(ipodMenuCategories);}
        if(next==MediaDevice.USB)usbSourceContext=true;
        if(next!=mediaDevice){mediaDevice=next;mediaDeviceEvidence=evidence;mediaDeviceRevision++;log("MEDIA DEVICE: "+next+" · "+evidence);}else if(!evidence.isEmpty())mediaDeviceEvidence=evidence;
    }
    public synchronized void captureLogicalMenu(){if(!connected||!Integer.valueOf(1).equals(menuSignal()))return;String[] rows=new String[3];for(int i=0;i<3;i++){Value raw=bsjValues.get(135+i);if(raw!=null&&raw.sequence>sourceSequence)return;Value v=values.get(135+i);if(v==null||v.sequence<=sourceSequence)return;rows[i]=cleanMenuText(v);}receivedMenu(rows);}
    private final Map<Integer,MediaText> mediaParts=new LinkedHashMap<>();
    private void resetMediaText(){mediaParts.clear();bsjExtraTexts.clear();bsjDisplayTexts.clear();bsjFilename=bsjFolder=bsjTitleField=bsjMediaTextState=null;mediaTrackStartedNanos=0;bsjTextLost=false;bsjTextEncoding="";for(int code=103;code<=105;code++)bsjValues.remove(code);}
    private Value bsjTextValue(String text){Value v=new Value(null,null,new String[]{text});v.sequence=count+1;return v;}
    private void bsjFrame(int command,int[] payload){
        Value profile=values.get(1000);if(profile==null||profile.ints==null||profile.ints.length==0||profile.ints[0]!=6291908)return;
        String rowsBefore=menuRowsKey();Integer showBefore=menuSignal();
        if(command==3&&payload.length==2){boolean enabled=(payload[0]&0x80)!=0;if(bsjAudioEnabled!=null&&bsjAudioEnabled!=enabled)bsjAudioPowerRevision++;bsjAudioEnabled=enabled;bsjValue(139,(payload[0]>>>6)&1);bsjValue(132,payload[1]&127);int next=(payload[0]&15)+1;if(!java.util.Objects.equals(effectiveSource(),next)){sourceTransition(next,count);log("BSJ source: "+next+" (validated command03)");}bsjSource=next;bsjValue(98,next);}
        else if(command==4&&payload.length==5){
            int incomingFrequency=(payload[1]<<8)|payload[2];Snapshot current=overlaySnapshot();
            if(recentVolumeChange()&&AudioMode.reported(current)==AudioMode.RADIO&&current.fresh(119)&&(incomingFrequency==0||incomingFrequency==0xFFFF))return;
            if(Integer.valueOf(1).equals(effectiveSource()))usbSourceContext=false;
            if(Integer.valueOf(1).equals(effectiveSource())&&((mediaDevice==MediaDevice.IPOD&&mediaDeviceEvidence.startsWith("Inferred from BSJ marker06"))||(mediaDevice==MediaDevice.USB&&(mediaDeviceEvidence.startsWith("Inferred from owner-confirmed USB playback05")||mediaDeviceEvidence.startsWith("Inferred from BSJ marker06 pending"))))){resetMediaDevice();resetMediaText();mediaSourceSequence=count;log("MEDIA: live radio04 cancels marker-inferred iPod");}int band=payload[0],slot=payload[3];Value old=bsjValues.get(115);if(Integer.valueOf(1).equals(effectiveSource())&&old!=null&&old.ints[0]!=band)sourceSequence=count;
            int frequency=(payload[1]<<8)|payload[2];bsjValue(115,band);bsjValue(116,slot);bsjValue(117,payload[4]&15);bsjValue(118,(payload[4]>>>7)&1);bsjValue(119,frequency);if(slot>=1&&slot<=6){bsjValue(119+slot,frequency);radioPresets.record(6291908,band,slot,frequency,System.currentTimeMillis());}
        }else if(command==5&&payload.length==8){
            // Honda sends an empty display frame while volume/menu occupies the OEM LCD.
            // Explicit work states 9/10 still invalidate playback below.
            boolean blank=true;for(int n:payload)if(n!=0){blank=false;break;}if(blank)return;
            Integer nextTrack=Snapshot.packedBcd((payload[4]<<8)|payload[5]);Value prior=bsjValues.get(101);Integer oldTrack=prior==null?null:Snapshot.packedBcd(prior.ints[0]);Value priorDisc=bsjValues.get(113);
            boolean volumeDisplay=retainMediaDuringVolume()&&(payload[1]&15)==0;
            boolean compatibleFamily=payload[3]==0||mediaPlaybackSource==null||payload[3]==(mediaPlaybackSource==2?1:2);
            if(volumeDisplay&&compatibleFamily&&(nextTrack==null||nextTrack==0))return;
            boolean missingDisc=volumeDisplay&&payload[3]!=2&&payload[0]==0&&priorDisc!=null&&priorDisc.ints[0]>0;
            boolean missingTime=volumeDisplay&&compatibleFamily&&java.util.Objects.equals(nextTrack,oldTrack)&&payload[6]==0&&payload[7]==0&&(payload[0]==0||payload[3]==0);
            boolean newDisc=priorDisc!=null&&payload[0]>0&&priorDisc.ints[0]>0&&priorDisc.ints[0]!=payload[0];if(newDisc||(nextTrack!=null&&nextTrack>0&&oldTrack!=null&&oldTrack>0&&!nextTrack.equals(oldTrack))){standbyMedia.remove(standbyKey(overlaySnapshot()));resetMediaText();}if((payload[1]&15)==9||(payload[1]&15)==10){resetMediaText();resetMediaDevice();mediaPlaybackSource=null;}else if(nextTrack!=null&&nextTrack>0&&(payload[1]&15)==0){
                // A reconnect packet may have a counter before its display family is known.
                // Explicit branches avoid unboxing null in a nested numeric conditional.
                if(payload[0]>0&&payload[3]==1){mediaPlaybackSource=2;usbSourceContext=false;}
                else if(payload[0]==0&&payload[3]==2){mediaPlaybackSource=4;usbSourceContext=true;}
                else mediaPlaybackSource=null;
            }
            if(!missingDisc)bsjValue(113,payload[0]);bsjValue(114,payload[1]&15);bsjValue(102,payload[1]>>>4);bsjValue(101,(payload[4]<<8)|payload[5]);if(!missingTime){bsjValue(99,payload[6]);bsjValue(100,payload[7]);}
            Integer source=effectiveSource();boolean inferredIpod=(mediaDevice==MediaDevice.IPOD||mediaDevice==MediaDevice.USB)&&mediaDeviceEvidence.startsWith("Inferred from BSJ");boolean eligible=source==null||source==0||source==2||source==4||(source==1&&inferredIpod);
            if(eligible&&(source==null||source!=auxSource)&&payload[0]==0&&payload[3]==2&&nextTrack!=null&&nextTrack>0&&(payload[1]&15)==0&&(mediaDevice!=MediaDevice.IPOD||mediaDeviceEvidence.startsWith("Inferred from BSJ"))){
                if(mediaDevice!=MediaDevice.USB){mediaDevice=MediaDevice.USB;mediaDeviceRevision++;}
                mediaDeviceEvidence="Inferred from owner-confirmed USB playback05: disc0, display family2, positive BCD track";
            }else if(payload[0]>0&&payload[3]==1&&((mediaDevice==MediaDevice.USB&&(mediaDeviceEvidence.startsWith("Inferred from owner-confirmed USB playback05")||mediaDeviceEvidence.startsWith("Inferred from BSJ marker06 pending")))||(mediaDevice==MediaDevice.IPOD&&mediaDeviceEvidence.startsWith("Inferred from BSJ")&&nextTrack!=null&&nextTrack>0&&(payload[1]&15)==0))){resetMediaDevice();resetMediaText();mediaSourceSequence=count;}
            if((payload[3]==1||payload[3]==2)&&nextTrack!=null&&nextTrack>0&&(payload[1]&15)==0){
                if(mediaTrackStartedNanos==0||newDisc||!java.util.Objects.equals(nextTrack,oldTrack))mediaTrackStartedNanos=System.nanoTime();
                bsjMediaTextState=new Value(new int[]{payload[2]},null,null);bsjMediaTextState.sequence=count+1;
            }
        }
        else if(command==7&&payload.length>=2){int kind=payload[0];
            if(retainMediaDuringVolume()){String incoming=BsjText.decode(payload,1).text;Snapshot current=overlaySnapshot();if((incoming.isEmpty()||incoming.equalsIgnoreCase("NO INFO"))&&!current.combinedMediaText().equals("—"))return;}
            bsjTitleField=new Value(new int[]{kind},null,null);bsjTitleField.sequence=count+1;if(kind==0||kind==1||kind==3||kind==4||kind==5||kind==6||kind==7||kind==8){BsjText.Result decoded=BsjText.decode(payload,1);Value work=bsjValues.get(114),track=bsjValues.get(101);if((decoded.text.isEmpty()||decoded.text.equalsIgnoreCase("NO INFO"))&&mediaParts.containsKey(kind)&&(work==null||(work.ints[0]!=9&&work.ints[0]!=10))){log("MEDIA TEXT: empty display retained while the same track is playing (TITLE/off)");return;}MediaText parts=mediaParts.get(kind);if(parts==null){parts=new MediaText();mediaParts.put(kind,parts);}Value v=bsjTextValue(parts.accept(decoded.text));
            if(kind==0){bsjValues.put(104,v);if(!decoded.text.isEmpty()){java.util.Iterator<Value> old=bsjDisplayTexts.iterator();while(old.hasNext())if(old.next().strings[0].equals(decoded.text))old.remove();bsjDisplayTexts.addLast(v);while(bsjDisplayTexts.size()>3)bsjDisplayTexts.removeFirst();}}else if(kind==1)bsjValues.put(103,v);else if(kind==3)bsjValues.put(105,v);else if(kind==4)bsjFolder=v;else if(kind==5)bsjFilename=v;else bsjExtraTexts.put(kind,v);
            bsjTextLost=decoded.lostCharacters;bsjTextEncoding=decoded.encoding;}}
        else if(command==14){int[] car=CarInfoPacket.decode(payload);if(car!=null)for(int i=0;i<car.length;i++)bsjValue(140+i,car[i]);}
        else if(command==9&&payload.length==49){bsjValue(138,payload[0]);String[] rows=new String[3];for(int i=0;i<3;i++){int[] text=new int[16];for(int k=0;k<16;k++)text[k]=payload[1+i*16+k];rows[i]=BsjText.decode(text,0).text;Value v=new Value(null,null,new String[]{rows[i]});v.sequence=count+1;bsjValues.put(135+i,v);}receivedMenu(rows);}
        trackMenu(rowsBefore,showBefore,command==3&&payload.length==2);
    }
    private boolean audioPlayerVisible;private String audioPlayerStatus=I18n.t(R.string.ui_audio_player_closed),audioPlayerMode="";
    public synchronized void audioPlayer(boolean visible,String status,String text){audioPlayerVisible=visible;audioPlayerStatus=status;audioPlayerMode=text;}
    private boolean carMenuOverlayVisible;private String carMenuOverlayStatus=I18n.t(R.string.ui_car_menu_off),carMenuOverlayText="";
    public synchronized void carMenuOverlay(boolean visible,String status,String text){carMenuOverlayVisible=visible;carMenuOverlayStatus=status;carMenuOverlayText=text;}
    private boolean climateStripCollapsed;private boolean climateStripVisible;private String climateStripText="";
    public synchronized void climateStrip(boolean visible,String text,boolean collapsed){climateStripCollapsed=collapsed;climateStrip(visible,text);}
    public synchronized void climateStrip(boolean visible,String text){climateStripVisible=visible;climateStripText=text;}
    private boolean climateOverlayVisible;private int climateOverlayHeight,overlayBottomPx;private String climateOverlayStatus=I18n.t(R.string.ui_climate_indicator_off),climateOverlayText="";
    public synchronized void climateOverlay(boolean visible,String status,String text,int height){climateOverlayVisible=visible;climateOverlayStatus=status;climateOverlayText=text;climateOverlayHeight=height;}
    public synchronized void overlayPosition(int offset){overlayBottomPx=offset;}
    private final ArrayDeque<JSONObject> rawChunks = new ArrayDeque<>();
    private int rawBytes; private long rawCount, rawDropped;
    private boolean trackOverlayVisible;
    public synchronized void trackOverlay(boolean visible){trackOverlayVisible=visible;}
    private boolean sourceOverlayVisible; private String sourceOverlayStatus=I18n.t(R.string.ui_source_indicator_off),sourceOverlayText="";
    public synchronized void sourceOverlay(boolean visible,String status,String text){sourceOverlayVisible=visible;sourceOverlayStatus=status;sourceOverlayText=text;}
    private boolean overlayVisible; private String overlayStatus = I18n.t(R.string.ui_indicator_off), overlayText = "";
    public synchronized void overlay(boolean visible, String status, String text) { overlayVisible=visible; overlayStatus=status; overlayText=text; }
    private final Map<Integer, Value> values = new LinkedHashMap<>();
    private final ArrayDeque<String> logs = new ArrayDeque<>();
    private final ArrayDeque<JSONObject> markers = new ArrayDeque<>();
    private final ArrayDeque<JSONObject> commands = new ArrayDeque<>();
    private long captureId=1,captureStartedAt=System.currentTimeMillis();
    private long commandId;
    private boolean commandBusy;
    private String commandStatus = I18n.t(R.string.ui_commands_are_sent_only_when);
    public synchronized void running(boolean value) { running = value;if(!value)analyzer.end(System.currentTimeMillis()); }
    public synchronized void backend(String text) { backend = text; }
    public synchronized void status(String title, String text, boolean live) { status = title; detail = text;if(recording&&live&&!connected){Value p=values.get(1000);analyzer.begin(sessionId,p==null||p.ints==null||p.ints.length==0?-1:p.ints[0],System.currentTimeMillis());}if(!live)analyzer.end(System.currentTimeMillis()); connected = live; log(title + ": " + text); }
    public synchronized void newSession() { analyzer.end(System.currentTimeMillis());unknownMessages.clear();menuBrowser.clear();menuValues.clear();playModeLabels.clear();sessionId++; audioRoutes.reset(sessionId); values.clear();bsj.reset();vehiclePackets.clear();standbyDisplay=null;standbyMedia.clear();bsjAudioEnabled=null;bsjAudioPowerRevision=0;bsjSource=null;bsjValues.clear();usbSourceContext=false;resetMediaText();resetMediaDevice();menuRevision=0;menuContentSequence=0;menuVisibilitySequence=0;climateRevision=0;optionalClimateSeen=0;sourceSequence=0;mediaSourceSequence=-1;mediaPlaybackSource=null;volumeChangedAt=0;dualAvailable=false; count = 0; lastEvent = 0; connected = false; }
    /** Start a fresh recording without altering current decoded state, IPC or freshness epochs. */
    public synchronized void clearLog(){
        captureId++;captureStartedAt=System.currentTimeMillis();
        logs.clear();markers.clear();commands.clear();rawChunks.clear();rawBytes=0;rawCount=rawDropped=0;
        bsj.reset();vehiclePackets.clear();analyzer.clear(captureStartedAt);unknownMessages.clear();audioRoutes.clearHistory();
        log("LOG RESET: new capture "+captureId+"; FYT connection and current values retained");
    }
    public synchronized void mark(String label) {
        try {
            JSONObject current = new JSONObject();
            for (Map.Entry<Integer, Value> e : values.entrySet()) current.put(String.valueOf(e.getKey()), e.getValue().json());
            markers.addLast(new JSONObject().put("atMs", System.currentTimeMillis()).put("label", label)
                .put("origin", "manual observation; not a measured CAN source").put("connected", connected).put("values", current)
                .put("audioRoutes",audioRoutes.markerContext()));
            while (markers.size() > 100) markers.removeFirst();
            log("USER MARK: " + label);
        } catch (Exception e) { log("Marker error: " + e); }
    }
    private JSONObject valuesJson() throws Exception {
        JSONObject j = new JSONObject();
        for (Map.Entry<Integer, Value> e : values.entrySet()) j.put(String.valueOf(e.getKey()), e.getValue().json());
        return j;
    }
    public synchronized long beginCommand(String label) {
        long id = ++commandId; commandBusy = true; commandStatus = label + I18n.t(R.string.ui_sending);
        try {
            commands.addLast(new JSONObject().put("id",id).put("atMs",System.currentTimeMillis()).put("action",label)
                .put("before",valuesJson()).put("audioRoutesBefore",audioRoutes.markerContext()).put("steps",new JSONArray()).put("result","pending"));
            while (commands.size() > 100) commands.removeFirst();
        } catch (Exception e) { log("Command record error: " + e); }
        log("COMMAND " + id + ": " + label); return id;
    }
    public synchronized void commandOptions(long id,String transport,String route,int holdMs,boolean debug){
        try{JSONObject record=commandRecord(id);if(record!=null)record.put("audioOptions",new JSONObject().put("transport",transport).put("route",route).put("holdMs",holdMs).put("debugOnly",debug));}
        catch(Exception e){log("Command options record error: "+e);}
    }
    private JSONObject commandRecord(long id) { for (JSONObject j : commands) if (j.optLong("id") == id) return j; return null; }
    public synchronized void commandStep(long id, int module, int code, int[] args, String result) {
        try {
            JSONObject j = commandRecord(id);
            if (j != null) j.getJSONArray("steps").put(new JSONObject().put("atMs",System.currentTimeMillis())
                .put("module",module).put("cmd",code).put("ints",new JSONArray(args)).put("transport",result)
                .put("mainChannelAtStep",audioRoutes.mainChannel()==null?JSONObject.NULL:audioRoutes.mainChannel()));
        } catch (Exception e) { log("Command step record error: " + e); }
        log("COMMAND " + id + " module=" + module + " cmd=" + code + " args=" + java.util.Arrays.toString(args) + " " + result);
    }
    public synchronized void finishCommand(long id, String result) {
        commandBusy = false; commandStatus = result;
        try { JSONObject j = commandRecord(id); if (j != null) j.put("result",result).put("finishedAtMs",System.currentTimeMillis()).put("after",valuesJson()).put("audioRoutesAfter",audioRoutes.markerContext()).put("observedSource",effectiveSource()); }
        catch (Exception e) { log("Command result record error: " + e); }
        log("COMMAND " + id + ": " + result);
    }
    public synchronized void commandRejected(String reason) { commandStatus = reason; log("COMMAND REJECTED: " + reason); }
    public synchronized void event(int code, int[] ints, float[] floats, String[] strings) {
        String rowsBefore=menuRowsKey();Integer showBefore=menuSignal();
        Value previous=values.get(code);Value v = new Value(ints, floats, strings);v.sequence=count+1;
        boolean optional=code==12||code==14||code==16||code==17||code==33||code==36||code==49;
        boolean climate=optional||code==10||code==11||code==13||code==18||code==19||code==20||code==21||code==27||code==28;
        Integer before=previous==null||previous.ints==null||previous.ints.length==0?null:previous.ints[0];
        Integer after=ints==null||ints.length==0?null:ints[0];
        if(code==132&&after!=null&&!java.util.Objects.equals(before,after))volumeChangedAt=v.at;
        if(code==1000&&!java.util.Objects.equals(before,after)){bsj.reset();vehiclePackets.clear();standbyDisplay=null;standbyMedia.clear();bsjAudioEnabled=null;bsjAudioPowerRevision=0;bsjSource=null;bsjValues.clear();mediaSourceSequence=v.sequence;mediaPlaybackSource=null;usbSourceContext=false;resetMediaText();resetMediaDevice();menuValues.clear();menuBrowser.clear();playModeLabels.clear();if(recording&&connected)analyzer.profile(after==null?-1:after,v.at);}
        if(code==98&&bsjSource==null&&!java.util.Objects.equals(before,after))sourceTransition(after,v.sequence);
        if(code==115&&bsjSource==null&&Integer.valueOf(1).equals(effectiveSource())&&previous!=null&&!java.util.Objects.equals(before,after))sourceSequence=v.sequence;
        if(code==101&&after!=null&&after>0&&!bsjValues.containsKey(101)&&(mediaTrackStartedNanos==0||!java.util.Objects.equals(before,after)))mediaTrackStartedNanos=System.nanoTime();
        if(optional&&after!=null&&after>0)optionalClimateSeen|=1L<<code;
        if(climate&&(previous!=null||(optional&&after!=null&&after>0))&&!java.util.Objects.equals(before,after))climateRevision++;
        // Generic FYT DUAL=14 is not filled by the local Honda decoder. A default zero is not evidence of support.
        if(code==14&&Integer.valueOf(1).equals(after)&&values.get(1000)!=null&&values.get(1000).ints!=null&&values.get(1000).ints.length>0&&values.get(1000).ints[0]==6291908)dualAvailable=true;
        values.put(code, v);if(code>=135&&code<=139)trackMenu(rowsBefore,showBefore,code==139&&ints!=null&&ints.length>0); count++; lastEvent = v.at;
        if(code>=120&&code<=125&&after!=null&&(previous==null||!java.util.Objects.equals(before,after))){Value p=values.get(1000),band=values.get(115),rawBand=bsjValues.get(115);if(p!=null&&p.ints!=null&&band!=null&&band.ints!=null&&(rawBand==null||rawBand.ints[0]==band.ints[0]))radioPresets.record(p.ints[0],band.ints[0],code-119,after,v.at);}
        status = I18n.t(R.string.ui_data_received); detail = I18n.t(R.string.ui_capture_is_running_change_source);
        if (code == 1019) {
            // MainServer sends [0] as the initial subscription value. It is not a captured packet.
            if (ints != null && ints.length > 0 && !(ints.length == 1 && ints[0] == 0)) {
                if(recording){
                int length = Math.min(ints.length,8192); StringBuilder hex = new StringBuilder(length*3);
                final String digits="0123456789ABCDEF";
                for (int k=0;k<length;k++) { int b=ints[k]&255;if(k>0)hex.append(' ');hex.append(digits.charAt(b>>>4)).append(digits.charAt(b&15)); }
                rawCount++;
                try {
                    rawChunks.addLast(new JSONObject().put("atMs",v.at).put("sequence",rawCount).put("session",sessionId)
                        .put("event",1019).put("hex",hex.toString()).put("capturedBytes",length).put("originalInts",ints.length)
                        .put("truncated",length!=ints.length)); rawBytes+=length;
                    while(rawChunks.size()>4096 || rawBytes>1048576) { rawBytes-=rawChunks.removeFirst().optInt("capturedBytes");rawDropped++; }
                } catch(Exception e) { log("Raw record error: " + e); }
                log("RAW 1019 #" + rawCount + " · " + ints.length + I18n.t(R.string.ui_bytes) + hex.substring(0,Math.min(hex.length(),180)));
                }
                bsj.accept(ints,v.at);
            }
        } else if(recording)try { log("event " + code + " " + v.json().toString()); } catch (Exception e) { log("event " + code); }
    }
    public synchronized void log(String text) {
        String entry = String.format(Locale.US, "%tT", System.currentTimeMillis()) + "  " + text;
        if (entry.length() > 3000) entry = entry.substring(0, 3000) + "…";
        logs.addLast(entry); while (logs.size() > 1600) logs.removeFirst();
    }
    public synchronized Snapshot overlaySnapshot() {
        Snapshot s=new Snapshot();s.auxSource=auxSource;s.auxName=auxName;s.running=running;s.connected=connected;s.commandBusy=commandBusy;s.sessionId=sessionId;s.status=status;s.count=count;s.lastEvent=lastEvent;s.overlayVisible=overlayVisible;s.trackOverlayVisible=trackOverlayVisible;s.sourceOverlayVisible=sourceOverlayVisible;s.overlayBottomPx=overlayBottomPx;
        s.bsjExtraTexts=new LinkedHashMap<>(bsjExtraTexts);s.bsjDisplayTexts=new java.util.ArrayList<>(bsjDisplayTexts);s.bsjFilename=bsjFilename;s.bsjFolder=bsjFolder;s.bsjTitleField=bsjTitleField;s.bsjTextLost=bsjTextLost;s.bsjTextEncoding=bsjTextEncoding;s.bsjAudioEnabled=bsjAudioEnabled;s.bsjAudioPowerRevision=bsjAudioPowerRevision;s.bsjSource=bsjSource;s.bsjValues=new LinkedHashMap<>(bsjValues);s.bsjFramesAccepted=bsj.accepted;s.bsjFramesRejected=bsj.rejected;s.sourceSequence=sourceSequence;s.mediaSourceSequence=mediaSourceSequence;s.audioPlayerVisible=audioPlayerVisible;s.audioPlayerStatus=audioPlayerStatus;s.audioPlayerMode=audioPlayerMode;s.optionalClimateSeen=optionalClimateSeen;
        s.menuValues=menuValues.snapshot();s.receivedPlayMode=playModeLabels.containsKey(effectiveSource())?playModeLabels.get(effectiveSource()):"";s.manualMediaDeviceHint=manualMediaDeviceHint;s.mediaDevice=mediaDevice;s.mediaDeviceRevision=mediaDeviceRevision;s.mediaDeviceEvidence=mediaDeviceEvidence;s.cachedMenu=values.get(1000)!=null&&values.get(1000).ints!=null&&values.get(1000).ints[0]==6291908?carMenuMemory.summary(effectiveSource()):"";s.menuRevision=menuRevision;s.menuContentSequence=menuContentSequence;s.menuVisibilitySequence=menuVisibilitySequence;s.carMenuOverlayVisible=carMenuOverlayVisible;s.carMenuOverlayStatus=carMenuOverlayStatus;s.carMenuOverlayText=carMenuOverlayText;
        s.climateStripCollapsed=climateStripCollapsed;s.climateStripVisible=climateStripVisible;s.climateStripText=climateStripText;s.climateRevision=climateRevision;s.dualAvailable=dualAvailable;s.climateOverlayVisible=climateOverlayVisible;s.climateOverlayHeight=climateOverlayHeight;
        s.values=new LinkedHashMap<>(values);s.presets=radioPresets.snapshot();s.mainChannel=audioRoutes.mainChannel();s.browseItems=menuBrowser.snapshot(new String[]{s.menuRow(0),s.menuRow(1),s.menuRow(2)});copyStandby(s);return s;
    }
    public synchronized Snapshot snapshot() {
        Snapshot s = new Snapshot(); s.auxSource=auxSource;s.auxName=auxName;s.running = running; s.connected = connected; s.status = status;
        s.detail = detail; s.backend = backend; s.count = count; s.lastEvent = lastEvent;
        s.sessionId=sessionId; s.rawCount=rawCount; s.rawDropped=rawDropped;
        s.standbyDisplay=standbyDisplay;s.vehiclePacket0E=vehiclePackets.packet0E();s.vehiclePacket10=vehiclePackets.packet10();
        s.bsjExtraTexts=new LinkedHashMap<>(bsjExtraTexts);s.bsjDisplayTexts=new java.util.ArrayList<>(bsjDisplayTexts);s.bsjFilename=bsjFilename;s.bsjFolder=bsjFolder;s.bsjTitleField=bsjTitleField;s.bsjTextLost=bsjTextLost;s.bsjTextEncoding=bsjTextEncoding;s.bsjAudioEnabled=bsjAudioEnabled;s.bsjAudioPowerRevision=bsjAudioPowerRevision;s.bsjSource=bsjSource;s.bsjValues=new LinkedHashMap<>(bsjValues);s.bsjFramesAccepted=bsj.accepted;s.bsjFramesRejected=bsj.rejected;s.sourceSequence=sourceSequence;s.mediaSourceSequence=mediaSourceSequence;s.audioPlayerVisible=audioPlayerVisible;s.audioPlayerStatus=audioPlayerStatus;s.audioPlayerMode=audioPlayerMode;s.optionalClimateSeen=optionalClimateSeen;
        s.menuValues=menuValues.snapshot();s.receivedPlayMode=playModeLabels.containsKey(effectiveSource())?playModeLabels.get(effectiveSource()):"";s.manualMediaDeviceHint=manualMediaDeviceHint;s.mediaDevice=mediaDevice;s.mediaDeviceRevision=mediaDeviceRevision;s.mediaDeviceEvidence=mediaDeviceEvidence;s.cachedMenu=values.get(1000)!=null&&values.get(1000).ints!=null&&values.get(1000).ints[0]==6291908?carMenuMemory.summary(effectiveSource()):"";s.menuRevision=menuRevision;s.menuContentSequence=menuContentSequence;s.menuVisibilitySequence=menuVisibilitySequence;s.carMenuOverlayVisible=carMenuOverlayVisible;s.carMenuOverlayStatus=carMenuOverlayStatus;s.carMenuOverlayText=carMenuOverlayText;
        s.climateStripCollapsed=climateStripCollapsed;s.climateStripVisible=climateStripVisible;s.climateStripText=climateStripText;s.climateRevision=climateRevision;s.dualAvailable=dualAvailable;s.climateOverlayVisible=climateOverlayVisible;s.climateOverlayHeight=climateOverlayHeight;s.climateOverlayStatus=climateOverlayStatus;s.climateOverlayText=climateOverlayText;s.overlayBottomPx=overlayBottomPx;
        s.trackOverlayVisible=trackOverlayVisible;s.sourceOverlayVisible=sourceOverlayVisible;s.sourceOverlayStatus=sourceOverlayStatus;s.sourceOverlayText=sourceOverlayText;
        s.overlayVisible=overlayVisible; s.overlayStatus=overlayStatus; s.overlayText=overlayText;
        s.commandBusy = commandBusy; s.commandStatus = commandStatus;
        s.values = new LinkedHashMap<>(values);s.presets=radioPresets.snapshot();s.mainChannel=audioRoutes.mainChannel();s.browseItems=menuBrowser.snapshot(new String[]{s.menuRow(0),s.menuRow(1),s.menuRow(2)});
        StringBuilder b = new StringBuilder(); int skip = Math.max(0, logs.size() - 40), i = 0;
        for (String l : logs) if (i++ >= skip) b.append(l).append('\n'); s.log = b.toString(); copyStandby(s);return s;
    }
    public synchronized long markExperiment(String label){return running&&connected?analyzer.mark(label,System.currentTimeMillis()):-1;}
    private JSONArray displayTextsJson()throws Exception{JSONArray out=new JSONArray();for(Value v:bsjDisplayTexts)out.put(v.json());return out;}
    public String exportJson() throws Exception {
        String base;BsjAnalyzer.Snapshot analysis;
        synchronized(this){base=exportBaseJson();analysis=analyzer.snapshot();}
        JSONObject root=new JSONObject(base);BsjAnalyzer.export(root,analysis,root.getLong("exportedAtMs"));return root.toString(2);
    }
    private String exportBaseJson() throws Exception {
        JSONObject root = new JSONObject().put("app", "Honda Box 0.8.33").put("exportedAtMs", System.currentTimeMillis())
            .put("android", Build.VERSION.RELEASE).put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL)
            .put("backend", backend).put("status", status).put("detail", detail).put("connected", connected)
            .put("callbacksThisSession", count).put("lastCallbackAtMs", lastEvent)
            .put("note", "CAN subscriptions 0..147,1000,1037,1019,1049 plus user-triggered stock audio commands for profile6291908. CAN1049 is retained as its complete callback arrays; subtype0 is the stock normalized steering value, not verified physical degrees. cmd is one-way; IPC transport does not confirm hardware action. Source98 is retained raw; validated BSJ command03 is preferred for the displayed source. BSJ media uses BCD according to the recorded hardware log. Radio and media fields may be retained when inactive. For profile6291908, source4 is USB/iPod and source3 defaults to AUX following the owner-confirmed MODE order; explicit owner calibration overrides AUX. Markers manual. Values stale if disconnected.");
        root.put("capture",new JSONObject().put("recordingEnabled",recording).put("id",captureId).put("startedAtMs",captureStartedAt).put("note","Event histories begin at the last log reset. Current decoded values, menu/preset caches and route lastValues are retained as context and may predate this recording; see their timestamps."));
        JSONObject data = new JSONObject(); for (Map.Entry<Integer, Value> e : values.entrySet()) data.put(String.valueOf(e.getKey()), e.getValue().json());
        root.put("rawBsj",new JSONObject().put("event",1019).put("receivedChunks",rawCount).put("droppedChunks",rawDropped)
            .put("retainedBytes",rawBytes).put("maxChunks",4096).put("maxBytes",1048576).put("maxChunkBytes",8192)
            .put("note","MCU/CAN protocol chunks, not proven original vehicle CAN frames. [0] subscription sentinel excluded. Signed ints normalized &255. Chunks may split packets. Oldest chunks discarded at limits.")
            .put("chunks",new JSONArray(rawChunks)));
        JSONObject decoded=new JSONObject();for(Map.Entry<Integer,Value> e:bsjValues.entrySet())decoded.put(String.valueOf(e.getKey()),e.getValue().json());
        root.put("bsjDecoded",new JSONObject().put("audioBit7",bsjAudioEnabled==null?JSONObject.NULL:bsjAudioEnabled).put("audioBit7Revision",bsjAudioPowerRevision).put("audioBit7Note","Verified by owner on 2026-10-12, profile6291908: command03 byte0 bit7 indicates car audio on/off (1 on, 0 off).").put("source",bsjSource==null?JSONObject.NULL:bsjSource).put("acceptedFrames",bsj.accepted).put("checksumFailures",bsj.rejected).put("values",decoded).put("note","Profile6291908 only. Length and checksum validated. CAN lastValues are untouched. DUAL fallback compares valid temperatures; power fallback uses fan0..7. Recirculation and defrost remain unconfirmed."));
        JSONObject extraTexts=new JSONObject();for(Map.Entry<Integer,Value> e:bsjExtraTexts.entrySet())extraTexts.put(String.valueOf(e.getKey()),e.getValue().json());
        root.getJSONObject("bsjDecoded").put("menuHeaderNote","Known by owner clarification on 2026-10-12, Honda/BSJ V704 profile6291908: TYPE09 payload[0]=0x20 (field138=32) represents the OEM SEL indicator: the item can be selected by pressing the car control. It is not a button-press event. Other header values remain unknown; no bitmask is inferred.");
        root.put("bsjMediaText",new JSONObject().put("displayStateByte2",bsjMediaTextState==null?JSONObject.NULL:bsjMediaTextState.json()).put("selectedSubtype",bsjTitleField==null?JSONObject.NULL:bsjTitleField.ints[0]).put("displayFieldsBySubtype",extraTexts).put("filename",bsjFilename==null?JSONObject.NULL:bsjFilename.json()).put("folder",bsjFolder==null?JSONObject.NULL:bsjFolder.json())
            .put("recentDisplayTexts",displayTextsJson()).put("lastEncoding",bsjTextEncoding).put("lastTextHasLostCharacters",bsjTextLost).put("note","TYPE07: 0 album/display text,1 title,3 artist,4 generic display text (folder/device semantics unconfirmed),5 filename,6/7/8 additional OEM display fields (kept separate, device identity not inferred). Filename used as title fallback. B7 substitution means the original letters were not transmitted; fallback windows-1251 is heuristic. CAN lastValues remain original."));
        JSONObject standbyCache=new JSONObject();
        for(Map.Entry<String,StandbyMedia> e:standbyMedia.entrySet()){
            StandbyMedia cache=e.getValue();JSONObject item=new JSONObject();
            if(cache.text!=null)item.put("text",cache.text.text).put("textReceivedAtMs",cache.text.at).put("textPayloadHex",cache.text.raw);
            if(cache.counters!=null)item.put("track",cache.counters.track).put("minutes",cache.counters.minutes).put("seconds",cache.counters.seconds).put("countersReceivedAtMs",cache.counters.at).put("countersPayloadHex",cache.counters.raw);
            standbyCache.put(e.getKey(),item);
        }
        root.put("bsjVehiclePacketWatch",vehiclePackets.json());
        root.put("bsjStandbyDisplayCache",new JSONObject().put("byPlayer",standbyCache).put("note","TYPE0C observed layouts used only to fill missing display data. Retained through audio off/on; normal TYPE05/07 take priority. Numeric text tail is never a title. Byte0 meaning remains unconfirmed; this packet never selects a source or device."));
        root.put("audioPlayer",new JSONObject().put("manualDisplayDevice",manualMediaDeviceHint.name()).put("confirmedAuxSource",auxSource).put("auxDisplayName",auxName).put("visible",audioPlayerVisible).put("status",audioPlayerStatus).put("text",audioPlayerMode).put("dataAfterSourceSequence",sourceSequence).put("mediaAfterSourceSequence",mediaSourceSequence));
        root.put("audioRoutes",audioRoutes.json());root.put("carMenuMemory",carMenuMemory.json());root.put("mediaDevice",new JSONObject().put("type",mediaDevice.name()).put("evidence",mediaDeviceEvidence).put("inferredFromMenu",mediaDeviceEvidence.contains("menu")).put("inferredFromPlayback",mediaDeviceEvidence.startsWith("Inferred from owner-confirmed USB playback05")).put("inferredFromStream",mediaDeviceEvidence.startsWith("Inferred from BSJ stream transition")).put("inferredFromMarker",mediaDeviceEvidence.startsWith("Inferred from BSJ marker06")));
        root.put("radioPresetCache",radioPresets.json());
        root.put("carMenuOverlay",new JSONObject().put("visible",carMenuOverlayVisible).put("status",carMenuOverlayStatus).put("text",carMenuOverlayText).put("changesThisSession",menuRevision).put("note","Three OEM lines135..137; raw BSJ09 length49 preferred for profile6291908. Show/hide139 from BSJ03 bit6. Type138 is retained but selection is not inferred. Display only; no vehicle settings commands."));
        root.put("climateStrip",new JSONObject().put("collapsed",climateStripCollapsed).put("visible",climateStripVisible).put("text",climateStripText));
        root.put("climateOverlay",new JSONObject().put("visible",climateOverlayVisible).put("status",climateOverlayStatus).put("text",climateOverlayText).put("heightPx",climateOverlayHeight).put("changesThisSession",climateRevision).put("dualFieldObservedActive",dualAvailable));
        root.put("sourceOverlay",new JSONObject().put("visible",sourceOverlayVisible).put("status",sourceOverlayStatus).put("text",sourceOverlayText));
        root.put("overlay",new JSONObject().put("visible",overlayVisible).put("status",overlayStatus).put("text",overlayText).put("bottomOffsetPx",overlayBottomPx));
        root.put("unidentifiedBsj",unknownMessages.json());
        return root.put("lastValues", data).put("markers", new JSONArray(markers)).put("commands",new JSONArray(commands)).put("log", new JSONArray(logs)).toString(2);
    }
}
