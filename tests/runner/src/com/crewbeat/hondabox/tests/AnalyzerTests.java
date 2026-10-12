package com.crewbeat.hondabox.tests;
import com.crewbeat.hondabox.*;
import org.json.*;
import java.util.*;
import java.io.*;
import android.content.res.AssetManager;

/** Protocol/model tests use invented packet types and controlled observations, never real signal claims. */
final class AnalyzerTests {
    private int checks;
    private void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
    private int[] packet(int type,int... payload){int[] a=new int[payload.length+4];a[0]=46;a[1]=type;a[2]=payload.length;int sum=type+payload.length;for(int i=0;i<payload.length;i++){a[i+3]=payload[i];sum+=payload[i];}a[a.length-1]=(~sum)&255;return a;}
    private boolean signal(JSONArray a,String name,int type,int offset,String encoding)throws Exception{for(int i=0;i<a.length();i++){JSONObject j=a.getJSONObject(i);if(j.getString("signal").equals(name)&&j.getInt("type")==type&&j.getInt("offset")==offset&&j.getString("encoding").equals(encoding))return true;}return false;}
    private long experiments(BsjAnalyzer a,String[] labels,int[] values,int type,int layout,long start){for(int i=0;i<labels.length;i++){long at=start+i*10000;int v0=values[i],hi0=(v0>>>8)&255,lo0=v0&255;int[] before=layout==0?new int[]{hi0,lo0,70}:layout==1?new int[]{3,hi0,lo0,90}:layout==2?new int[]{lo0,hi0,5}:new int[]{v0&255,12};a.frame(type,before,at-100);a.mark(labels[i],at);
        for(int k=0;k<4;k++){int v=values[i],hi=(v>>>8)&255,lo=v&255;int[] p=layout==0?new int[]{hi,lo,70}:layout==1?new int[]{3,hi,lo,90}:layout==2?new int[]{lo,hi,5}:new int[]{v&255,12};a.frame(type,p,at+100+k*100);}}
        return start+(labels.length-1)*10000+5001;}
    int run(AssetManager assets)throws Exception {
        List<int[]> parsed=new ArrayList<>();List<Long> times=new ArrayList<>();BsjFrames parser=new BsjFrames((type,p,at)->{parsed.add(p);times.add(at);});int[] f=packet(0x45,255,128,0,46);
        parser.accept(new int[]{8,9,46},1000);parser.accept(Arrays.copyOfRange(f,1,5),3500);check("no partial packet",parsed.isEmpty());parser.accept(Arrays.copyOfRange(f,5,f.length),7000);
        check("fragment survives long pauses",parsed.size()==1&&parsed.get(0)[0]==255);check("completion timestamp",times.get(0)==7000);
        int[] bad=packet(0x33,7);bad[bad.length-1]^=1;int[] both=new int[bad.length+f.length];System.arraycopy(bad,0,both,0,bad.length);System.arraycopy(f,0,both,bad.length,f.length);parser.accept(both,7200);
        check("checksum resync",parser.rejected==1&&parsed.size()==2);parser.accept(packet(0x7c),8000);check("zero length packet valid",parsed.get(2).length==0);
        parser.accept(new int[]{0},8500);check("sentinel excluded",parsed.size()==3);parser.reset();check("parser session reset",parser.accepted==0&&parser.rejected==0);
        BsjAnalyzer a=new BsjAnalyzer();a.frame(0x40,new int[]{1},50);check("inactive capture ignored",a.snapshot().frames.isEmpty());a.begin(7,6291908,1000);
        a.frame(0x40,new int[]{1,255},1999);a.frame(0x40,new int[]{2,128},2000);a.frame(0x40,new int[]{3,0},4000);long marker=a.mark("ENGINE_IDLE",4000);
        a.frame(0x40,new int[]{4,10},9000);a.frame(0x40,new int[]{5,12},9001);BsjAnalyzer.Window w=a.snapshot().window(marker);
        check("inclusive window bounds",w.frames.size()==3&&w.frames.get(0).at==2000&&w.frames.get(2).at==9000);check("pending window",!w.complete(8999));check("window complete at boundary",w.complete(9000)&&w.usable(9000));
        check("invalid label rejected",a.mark("WILD",9100)==-1);check("unsigned serialization",a.snapshot().frames.get(0).json().getJSONArray("payload").getInt(1)==255);
        BsjAnalyzer.Frame u=a.snapshot().frames.get(0);check("u8",BsjAnalyzer.decode(u,1,"u8")==255);check("s8",BsjAnalyzer.decode(u,1,"s8")==-1);check("BE16",BsjAnalyzer.decode(u,0,"BE16")==511);check("LE16",BsjAnalyzer.decode(u,0,"LE16")==65281);check("sLE16",BsjAnalyzer.decode(u,0,"sLE16")==-255);
        check("bounds missing word",BsjAnalyzer.decode(u,1,"BE16")==null);check("invalid offset",BsjAnalyzer.decode(u,-1,"u8")==null);check("unknown encoding",BsjAnalyzer.decode(u,0,"XYZ")==null);check("null encoding",BsjAnalyzer.decode(u,0,null)==null);
        int[] mutated=u.payload();mutated[0]=66;check("payload immutable",u.byteAt(0)==1);check("full hex",u.fullHex().equals(BsjAnalyzer.hex(packet(0x40,1,255))));
        BsjAnalyzer.Stats stats=BsjAnalyzer.statistics(w.frames).get("40/2");check("byte min/max",stats.min(0)==2&&stats.max(0)==4);check("byte average",stats.avg(0)==3);check("unique",stats.unique(1)==3);check("rate",Math.abs(stats.rate()-2.0/7)<.000001);check("changed offsets",stats.json().getJSONArray("changedOffsets").length()==2);
        long interrupted=a.mark("BRAKE_ON",10000);a.end(11000);check("disconnect interrupts pending",a.snapshot().window(interrupted).interruptedAt==11000&&!a.snapshot().window(interrupted).usable(20000));check("completed stays complete",a.snapshot().window(marker).usable(20000));
        a.begin(8,6291908,12000);a.frame(0x40,new int[]{99,1},12001);check("new capture excludes old statistics",a.snapshot().currentFrames().size()==1);a.profile(1,12002);check("unsupported profile marker rejected",a.mark("GEAR_D",12003)==-1);check("unknown frames still captured",a.snapshot().frames.size()==6);
        a.clear(12004);check("clear removes frames/markers",a.snapshot().frames.isEmpty()&&a.snapshot().windows.isEmpty());a.frame(5,null,12005);check("null payload ignored",a.snapshot().frames.isEmpty());
        BsjAnalyzer burst=new BsjAnalyzer();burst.begin(1,6291908,1000);for(int i=0;i<5000;i++)burst.frame(0x3f,new int[]{1},3000);long burstMark=burst.mark("ENGINE_IDLE",4000);
        check("evicted pre-roll reported incomplete",!burst.snapshot().window(burstMark).json(9000).getBoolean("preRollComplete"));
        a.begin(9,6291908,20000);int[] longData=new int[255];long overflow=a.mark("ENGINE_OFF",23000);for(int i=0;i<5000;i++)a.frame(0x70,longData,23000+i%4000);
        check("history count bounded",a.snapshot().frames.size()<=4096);check("history bytes bounded",a.snapshot().retainedBytes<=1048576);check("eviction counted",a.snapshot().droppedFrames>0);check("saved window retains evicted oldest",a.snapshot().window(overflow).frames.get(0).sequence==1);
        check("window truncation recorded",a.snapshot().window(overflow).dropped>0&&!a.snapshot().window(overflow).usable(30000));for(int i=0;i<70;i++)a.mark("GEAR_P",40000+i*10000);check("marker bound and loss",a.snapshot().windows.size()==64&&a.snapshot().droppedMarkers==7);
        JSONObject exported=new JSONObject();BsjAnalyzer.export(exported,a.snapshot(),800000);check("export version",exported.getInt("exportVersion")==2);for(String section:new String[]{"parsedBsjFrames","experimentMarkers","experimentWindows","candidateSignals","frameStatistics"})check("export "+section,exported.has(section));
        a=new BsjAnalyzer();a.begin(11,6291908,100000);long now=experiments(a,new String[]{"RPM_1500","RPM_2000","RPM_3000"},new int[]{1500,2000,3000},0x44,1,103000);
        JSONArray candidates=SignalCandidates.find(a.snapshot(),now);check("synthetic RPM BE16 discovered",signal(candidates,"RPM",0x44,1,"BE16"));check("candidate not verified",candidates.getJSONObject(0).getString("status").equals("candidate_unverified"));
        check("incomplete experiment excluded",SignalCandidates.find(a.snapshot(),now-2).length()==0);a.mark("RPM_3000",now+1);check("pending repeat supersedes old candidate",SignalCandidates.find(a.snapshot(),now+2).length()==0);
        a=new BsjAnalyzer();a.begin(12,6291908,200000);now=experiments(a,new String[]{"STEER_CENTER","STEER_LEFT_90","STEER_RIGHT_90"},new int[]{0,-90,90},0x52,0,203000);check("signed steering discovered",signal(SignalCandidates.find(a.snapshot(),now),"Steering",0x52,0,"sBE16"));
        a=new BsjAnalyzer();a.begin(13,6291908,300000);now=experiments(a,new String[]{"GEAR_P","GEAR_R","GEAR_N","GEAR_D","GEAR_S"},new int[]{2,4,6,8,10},0x61,3,303000);check("discrete gear discovered",signal(SignalCandidates.find(a.snapshot(),now),"Gear",0x61,0,"u8"));
        a=new BsjAnalyzer();a.begin(14,6291908,400000);now=experiments(a,new String[]{"SPEED_0","SPEED_10","SPEED_20","SPEED_30"},new int[]{0,100,200,300},0x73,2,403000);check("scaled little endian speed discovered",signal(SignalCandidates.find(a.snapshot(),now),"Speed",0x73,0,"LE16"));
        a.begin(15,6291908,now+1000);check("candidates isolated by connection",SignalCandidates.find(a.snapshot(),now+2000).length()==0);
        for(int[] control:new int[][]{{4,4,4},{3000,2000,1500},{1500,9999,3000}}){a=new BsjAnalyzer();a.begin(16,6291908,500000);now=experiments(a,new String[]{"RPM_1500","RPM_2000","RPM_3000"},control,0x44,0,503000);check("false RPM control rejected",SignalCandidates.find(a.snapshot(),now).length()==0);}
        // Replay actual recorded stream: no invented RPM markers and no hardcoded unknown packet types.
        String raw;try(InputStream in=assets.open("recorded-bsj-v06.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);raw=new String(out.toByteArray(),java.nio.charset.StandardCharsets.UTF_8);}
        JSONObject recorded=new JSONObject(raw);BsjAnalyzer replay=new BsjAnalyzer();replay.begin(17,6291908,0);BsjFrames stream=new BsjFrames((type,p,at)->replay.frame(type,p,at));JSONArray chunks=recorded.getJSONArray("chunks");
        for(int i=0;i<chunks.length();i++){JSONObject c=chunks.getJSONObject(i);String[] parts=c.getString("hex").split(" ");int[] bytes=new int[parts.length];for(int j=0;j<bytes.length;j++)bytes[j]=Integer.parseInt(parts[j],16);stream.accept(bytes,c.getLong("atMs"));}
        check("real log chunks",chunks.length()==1379);check("real valid frames",stream.accepted==1232&&replay.snapshot().frames.size()==1232);check("real groups",BsjAnalyzer.statistics(replay.snapshot().frames).size()==7);check("no unmarked RPM inference",SignalCandidates.find(replay.snapshot(),Long.MAX_VALUE).length()==0);
        check("original stream not mutated",recorded.getJSONArray("chunks").length()==1379);return checks;
    }
}
