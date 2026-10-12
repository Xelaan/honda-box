package dev.alex.fyt.probe;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Bounded passive analysis of checksum-validated BSJ packets. No IPC or command access. */
public final class BsjAnalyzer {
    public static final int MAX_FRAMES=4096,MAX_BYTES=1048576,MAX_MARKERS=64;
    private static final int MAX_WINDOW_FRAMES=1024,MAX_WINDOW_BYTES=131072,MAX_WINDOWS_BYTES=1048576;
    public static final String[] LABELS={"ENGINE_OFF","ENGINE_IDLE","RPM_1500","RPM_2000","RPM_3000",
        "STEER_CENTER","STEER_LEFT_90","STEER_RIGHT_90","GEAR_P","GEAR_R","GEAR_N","GEAR_D","GEAR_S",
        "SPEED_0","SPEED_10","SPEED_20","SPEED_30","BRAKE_ON","BRAKE_OFF"};
    public static final class Frame {
        public final long at,sequence,session,segment;public final int profile,type;private final int[] data;
        Frame(long at,long sequence,long session,long segment,int profile,int type,int[] payload){
            this.at=at;this.sequence=sequence;this.session=session;this.segment=segment;this.profile=profile;this.type=type;data=payload.clone();
            for(int i=0;i<data.length;i++)data[i]&=255;
        }
        public int length(){return data.length;}
        public int byteAt(int offset){return data[offset];}
        public int[] payload(){return data.clone();}
        public String key(){return String.format(Locale.US,"%02X/%d",type,length());}
        int bytes(){return length()+4;}
        public String payloadHex(){return hex(data);}
        public String fullHex(){int[] all=new int[length()+4];all[0]=46;all[1]=type;all[2]=length();int sum=type+length();
            for(int i=0;i<length();i++){all[i+3]=data[i];sum+=data[i];}all[all.length-1]=(~sum)&255;return hex(all);}
        public JSONObject json()throws Exception{return new JSONObject().put("timestampMs",at).put("sequence",sequence).put("session",session)
            .put("captureSegment",segment).put("profile",profile).put("type",type).put("typeHex",String.format(Locale.US,"%02X",type))
            .put("length",length()).put("payload",new JSONArray(data)).put("fullHex",fullHex()).put("checksumValid",true);}
    }
    private static final class Marker {
        long id,at,session,segment,startedAt,interruptedAt,rawAgeAtMarker;int profile,bytes,dropped;String label,captureQuality;
        final ArrayList<Frame> frames=new ArrayList<>();
    }
    public static final class Window {
        public final long id,at,session,segment,startedAt,interruptedAt,rawAgeAtMarker;public final int profile,dropped;public final String label,captureQuality;
        public final List<Frame> frames;
        Window(Marker m){id=m.id;at=m.at;session=m.session;segment=m.segment;startedAt=m.startedAt;interruptedAt=m.interruptedAt;
            profile=m.profile;dropped=m.dropped;label=m.label;rawAgeAtMarker=m.rawAgeAtMarker;captureQuality=m.captureQuality;frames=Collections.unmodifiableList(new ArrayList<>(m.frames));}
        public long start(){return at-2000;}
        public long end(){return at+5000;}
        public boolean complete(long now){return interruptedAt==0&&now>=end();}
        public boolean usable(long now){return complete(now)&&dropped==0&&"RAW_LIVE".equals(captureQuality);}
        public JSONObject markerJson()throws Exception{return new JSONObject().put("id",id).put("label",label).put("timestampMs",at)
            .put("session",session).put("captureSegment",segment).put("profile",profile).put("captureQuality",captureQuality).put("rawAgeAtMarkerMs",rawAgeAtMarker<0?JSONObject.NULL:rawAgeAtMarker).put("origin","manual observation; not measured vehicle data");}
        public JSONObject json(long now)throws Exception{JSONArray packets=new JSONArray();for(Frame f:frames)packets.put(f.json());
            return markerJson().put("fromMs",start()).put("untilMs",end()).put("complete",complete(now))
                .put("preRollComplete",startedAt<=start()).put("interruptedAtMs",interruptedAt==0?JSONObject.NULL:interruptedAt)
                .put("droppedFrames",dropped).put("frames",packets);}
    }
    public static final class Snapshot {
        public final List<Frame> frames;public final List<Window> windows;public final long session,segment,received,droppedFrames,droppedMarkers;
        public final int profile,retainedBytes;public final boolean active;
        public final long lastFrameAt,segmentFrames,startedAt;public final String adapterId;
        Snapshot(BsjAnalyzer a){frames=Collections.unmodifiableList(new ArrayList<>(a.history));ArrayList<Window> list=new ArrayList<>();for(Marker m:a.markers)list.add(new Window(m));
            windows=Collections.unmodifiableList(list);session=a.session;segment=a.segment;profile=a.profile;active=a.active;received=a.sequence;
            droppedFrames=a.droppedFrames;droppedMarkers=a.droppedMarkers;retainedBytes=a.bytes;lastFrameAt=a.lastFrameAt;segmentFrames=a.segmentFrames;startedAt=a.startedAt;adapterId=a.adapterId;}
        public long rawAge(long now){return lastFrameAt<0?-1:Math.max(0,now-lastFrameAt);}
        public boolean rawLive(long now){return active&&rawAge(now)>=0&&rawAge(now)<=1000;}
        public double rate(long now){long since=Math.max(startedAt,now-3000),duration=Math.max(1,now-since);int count=0;for(Frame f:frames)if(f.segment==segment&&f.at>=since&&f.at<=now)count++;return count*1000.0/duration;}
        public String liveText(long now){long age=rawAge(now);return "BSJ RAW ● "+(rawLive(now)?"LIVE":"NO DATA")+" · last frame "+(age<0?"—":age+" ms")+" · "+String.format(Locale.US,"%.1f fps",rate(now))+" · parsed "+segmentFrames+(adapterId.isEmpty()?"":" · "+adapterId);}
        public Window window(long id){for(Window w:windows)if(w.id==id)return w;return null;}
        public List<Frame> currentFrames(){ArrayList<Frame> list=new ArrayList<>();for(Frame f:frames)if(f.segment==segment)list.add(f);return list;}
        public int pending(long now){int count=0;for(Window w:windows)if(w.interruptedAt==0&&now<w.end())count++;return count;}
    }
    public static final class Stats {
        public final int type,length;public long count,firstAt,lastAt;public Frame last;
        private final int[] min,max;private final double[] sums;private final BitSet[] unique;
        Stats(Frame f){type=f.type;length=f.length();min=new int[length];max=new int[length];sums=new double[length];unique=new BitSet[length];Arrays.fill(min,255);for(int i=0;i<length;i++)unique[i]=new BitSet(256);}
        void add(Frame f){if(count++==0)firstAt=f.at;lastAt=f.at;last=f;for(int i=0;i<length;i++){int n=f.byteAt(i);min[i]=Math.min(min[i],n);max[i]=Math.max(max[i],n);sums[i]+=n;unique[i].set(n);}}
        public int min(int i){return min[i];}public int max(int i){return max[i];}public double avg(int i){return sums[i]/count;}public int unique(int i){return unique[i].cardinality();}
        public Double rate(){return count<2||lastAt<=firstAt?null:(count-1)*1000.0/(lastAt-firstAt);}
        public JSONObject json()throws Exception{JSONArray b=new JSONArray(),changed=new JSONArray();for(int i=0;i<length;i++){
            b.put(new JSONObject().put("offset",i).put("min",min[i]).put("max",max[i]).put("avg",avg(i)).put("unique",unique(i)));if(unique(i)>1)changed.put(i);}
            return new JSONObject().put("type",type).put("typeHex",String.format(Locale.US,"%02X",type)).put("length",length).put("count",count)
                .put("firstAtMs",firstAt).put("lastAtMs",lastAt).put("framesPerSecond",rate()==null?JSONObject.NULL:rate())
                .put("knownFamily",known(type)).put("lastPayload",new JSONArray(last.data)).put("bytes",b).put("changedOffsets",changed);}
    }
    private final ArrayDeque<Frame> history=new ArrayDeque<>();private final ArrayDeque<Marker> markers=new ArrayDeque<>();
    private long session,segment,startedAt,sequence,markerId,droppedFrames,droppedMarkers,lastEvictedAt;private int profile=-1,bytes,windowBytes;private boolean active;
    private long lastFrameAt=-1,segmentFrames;private String adapterId="";
    public synchronized void begin(long session,int profile,long now){interrupt(now);this.session=session;this.profile=profile;segment++;startedAt=now;lastEvictedAt=0;lastFrameAt=-1;segmentFrames=0;adapterId="";active=true;}
    public synchronized void profile(int profile,long now){if(this.profile!=profile){begin(session,profile,now);}}
    private void interrupt(long now){for(Marker m:markers)if(m.interruptedAt==0&&now<m.at+5000)m.interruptedAt=now;}
    public synchronized void end(long now){interrupt(now);active=false;}
    public synchronized void clear(long now){history.clear();markers.clear();bytes=windowBytes=0;sequence=markerId=droppedFrames=droppedMarkers=0;segment++;startedAt=now;lastEvictedAt=0;lastFrameAt=-1;segmentFrames=0;adapterId="";}
    private void add(Marker m,Frame f){if(m.frames.size()>=MAX_WINDOW_FRAMES||m.bytes+f.bytes()>MAX_WINDOW_BYTES||windowBytes+f.bytes()>MAX_WINDOWS_BYTES){m.dropped++;return;}
        m.frames.add(f);m.bytes+=f.bytes();windowBytes+=f.bytes();}
    /** Called exclusively after streaming parser checksum validation. */
    public synchronized void frame(int type,int[] payload,long at){if(!active||type<0||type>255||payload==null||payload.length>255)return;
        Frame f=new Frame(at,++sequence,session,segment,profile,type,payload);history.addLast(f);bytes+=f.bytes();
        lastFrameAt=at;segmentFrames++;if(type==0x7f&&payload.length==16){StringBuilder id=new StringBuilder();boolean printable=true;for(int n:payload){int value=n&255;if(value<32||value>126){printable=false;break;}id.append((char)value);}if(printable)adapterId=id.toString();}
        for(Marker m:markers)if(m.segment==segment&&m.interruptedAt==0&&at>=m.at-2000&&at<=m.at+5000)add(m,f);
        while(history.size()>MAX_FRAMES||bytes>MAX_BYTES){Frame old=history.removeFirst();bytes-=old.bytes();if(old.segment==segment)lastEvictedAt=Math.max(lastEvictedAt,old.at);droppedFrames++;}
    }
    public synchronized long mark(String label,long at){if(!active||profile!=6291908||!Arrays.asList(LABELS).contains(label))return -1;
        while(markers.size()>=MAX_MARKERS){Marker old=markers.removeFirst();windowBytes-=old.bytes;droppedMarkers++;}
        Marker m=new Marker();m.id=++markerId;m.at=at;m.session=session;m.segment=segment;m.profile=profile;m.label=label;m.startedAt=Math.max(startedAt,lastEvictedAt==0?startedAt:lastEvictedAt+1);
        m.rawAgeAtMarker=lastFrameAt<0?-1:Math.max(0,at-lastFrameAt);m.captureQuality=m.rawAgeAtMarker>=0&&m.rawAgeAtMarker<=1000?"RAW_LIVE":"INVALID_NO_RAW_DATA";
        for(Frame f:history)if(f.segment==segment&&f.at>=at-2000&&f.at<=at)add(m,f);
        markers.addLast(m);return m.id;
    }
    public synchronized Snapshot snapshot(){return new Snapshot(this);}
    public static String hex(int[] values){StringBuilder b=new StringBuilder(values.length*3);for(int n:values){if(b.length()>0)b.append(' ');int value=n&255;String digits="0123456789ABCDEF";b.append(digits.charAt(value>>>4)).append(digits.charAt(value&15));}return b.toString();}
    public static String known(int type){switch(type){case 1:return "Key input";case 14:return "Stock vehicle fields";case 15:return "Stock notification";case 16:return "Stock fields133/134";case 17:return "Door status";case 2:return "Climate";case 3:return "Audio/volume";case 4:return "Radio";case 5:return "Media";case 6:return "CD changer";case 7:return "Media text";case 9:return "OEM menu";case 0x7f:return "Adapter identification";default:return "Unknown";}}
    public static LinkedHashMap<String,Stats> statistics(List<Frame> frames){LinkedHashMap<String,Stats> result=new LinkedHashMap<>();for(Frame f:frames){Stats s=result.get(f.key());
        if(s==null){s=new Stats(f);result.put(f.key(),s);}s.add(f);}return result;}
    public static Integer decode(Frame f,int offset,String encoding){if(f==null||encoding==null||offset<0||offset>=f.length())return null;int a=f.byteAt(offset);if(encoding.equals("u8"))return a;if(encoding.equals("s8"))return a>=128?a-256:a;
        if(offset+1>=f.length())return null;int b=f.byteAt(offset+1),n;
        if(encoding.equals("BE16")||encoding.equals("sBE16"))n=(a<<8)|b;else if(encoding.equals("LE16")||encoding.equals("sLE16"))n=(b<<8)|a;else return null;
        return encoding.startsWith("s")&&n>=32768?n-65536:n;}
    public static void export(JSONObject root,Snapshot s,long now)throws Exception{
        JSONArray frames=new JSONArray(),marks=new JSONArray(),windows=new JSONArray(),stats=new JSONArray();for(Frame f:s.frames)frames.put(f.json());for(Window w:s.windows){marks.put(w.markerJson());windows.put(w.json(now));}
        for(Stats group:statistics(s.currentFrames()).values())stats.put(group.json());
        root.put("exportVersion",2).put("parsedBsjFrames",frames).put("experimentMarkers",marks).put("experimentWindows",windows)
            .put("frameStatistics",stats).put("candidateSignals",SignalCandidates.find(s,now))
            .put("bsjAnalyzer",new JSONObject().put("active",s.active).put("currentSession",s.session).put("currentSegment",s.segment).put("profile",s.profile)
                .put("receivedFrames",s.received).put("droppedFrames",s.droppedFrames).put("droppedMarkers",s.droppedMarkers).put("retainedBytes",s.retainedBytes)
                .put("rawStatus",s.rawLive(now)?"LIVE":"NO DATA").put("lastValidFrameAtMs",s.lastFrameAt<0?JSONObject.NULL:s.lastFrameAt).put("lastValidFrameAgeMs",s.rawAge(now)<0?JSONObject.NULL:s.rawAge(now)).put("recentFramesPerSecond",s.rate(now)).put("adapterId",s.adapterId.isEmpty()?JSONObject.NULL:s.adapterId)
                .put("maxCandidateFieldChecksTotal",SignalCandidates.MAX_FIELDS).put("maxCandidateResults",SignalCandidates.MAX_RESULTS).put("maxFrames",MAX_FRAMES).put("maxBytes",MAX_BYTES).put("maxMarkers",MAX_MARKERS).put("maxWindowFrames",MAX_WINDOW_FRAMES)
                .put("maxWindowBytes",MAX_WINDOW_BYTES).put("maxAllWindowBytes",MAX_WINDOWS_BYTES).put("pendingWindows",s.pending(now))
                .put("note","Only checksum-valid BSJ/MCU protocol frames, not vehicle CAN-ID frames. Timestamp is receipt of the completing chunk. Windows include -2s/+5s, retain their own frame references, and disclose truncation/interruption/preroll. Statistics describe retained current-segment frames. Candidate heuristics use complete, untruncated post-marker data in one capture segment; all signals remain unverified. Known message families do not exclude unknown bytes. Candidate search examines at most 4096 fields across available signal sets and retains at most 40 matches. No analyzer commands or CAN profile changes."));
    }
}
