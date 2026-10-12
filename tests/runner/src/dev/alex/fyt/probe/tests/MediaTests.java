package dev.alex.fyt.probe.tests;
import dev.alex.fyt.probe.*;
import org.json.*;
import android.content.res.AssetManager;
import java.io.*;
import java.nio.charset.*;

final class MediaTests {
    private int checks;private void check(String s,boolean ok){if(!ok)throw new AssertionError(s);checks++;}
    private int[] bytes(String s,Charset c){byte[] b=s.getBytes(c);int[] a=new int[b.length+2];a[0]=1;for(int i=0;i<b.length;i++)a[i+1]=b[i]&255;return a;}
    private int[] packet(int type,int... p){int[] a=new int[p.length+4];a[0]=46;a[1]=type;a[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){a[i+3]=p[i];sum+=p[i];}a[a.length-1]=(~sum)&255;return a;}
    private void raw(ProbeState s,int type,int... p){s.event(1019,packet(type,p),null,null);}
    int run(AssetManager assets)throws Exception {
        MediaName pair=MediaName.parse("Led Zeppelin — Babe I'm Gonna Leave You.mp3");check("clear delimiter separates",pair!=null&&pair.artist.equals("Led Zeppelin")&&pair.title.equals("Babe I'm Gonna Leave You"));
        check("ordinary internal hyphen not separator",MediaName.parse("AC-DC Thunderstruck")==null);check("ambiguous multiple separators retained",MediaName.parse("Artist - Track - Live")==null);check("truncated display not split",MediaName.parse("Artist - Song>")==null);check("missing separator not guessed",MediaName.parse("Led Zeppelin")==null);
        check("UTF8 Cyrillic",BsjText.decode(bytes("Песня — Шопен",StandardCharsets.UTF_8),1).text.equals("Песня — Шопен"));
        check("CP1251 Cyrillic",BsjText.decode(bytes("Русский исполнитель",Charset.forName("windows-1251")),1).text.equals("Русский исполнитель"));
        check("null terminator",BsjText.decode(new int[]{5,65,0,66,0},1).text.equals("A"));check("padding stripped",BsjText.decode(new int[]{5,65,32,32,0},1).text.equals("A"));
        BsjText.Result lost=BsjText.decode(new int[]{0,183,183,32,65,0},1);check("B7 loss explicit",lost.lostCharacters&&lost.text.equals("□□ A")&&!lost.text.contains("�"));
        check("unicode middle dot is preserved",!BsjText.decode(bytes("Трек · Artist",StandardCharsets.UTF_8),1).lostCharacters);
        ProbeState s=new ProbeState();s.newSession();s.running(true);s.status("test","",true);s.event(1000,new int[]{6291908},null,null);raw(s,3,0x81,7);
        int[] title=bytes("Название трека",StandardCharsets.UTF_8);raw(s,7,title);check("decoded title used",s.snapshot().cdTitle().equals("Название трека"));
        s.event(103,null,null,new String[]{"broken stock value"});check("validated raw title preferred",s.snapshot().cdTitle().equals("Название трека"));check("stock lastValues untouched",new JSONObject(s.exportJson()).getJSONObject("lastValues").getJSONObject("103").getJSONArray("strings").getString(0).equals("broken stock value"));
        int[] artist=bytes("Исполнитель",StandardCharsets.UTF_8);artist[0]=3;raw(s,7,artist);check("decoded artist used",s.snapshot().text(105).equals("Исполнитель"));
        raw(s,3,0x83,7);check("new source removes old metadata",s.snapshot().cdTitle().equals("—")&&!s.snapshot().fresh(105));
        int[] filename=bytes("01 Angry Stupid.mp3",StandardCharsets.UTF_8);filename[0]=5;raw(s,7,filename);check("filename becomes title fallback",s.snapshot().cdTitle().equals("01 Angry Stupid.mp3"));
        int[] folder=bytes("music",StandardCharsets.UTF_8);folder[0]=4;raw(s,7,folder);check("folder distinct",s.snapshot().mediaFolder().equals("music")&&s.snapshot().text(104).equals("—"));
        raw(s,7,new int[]{5,0});check("explicit empty filename clears without known playing track",s.snapshot().cdTitle().equals("—"));
        int[] album=bytes("Led Zeppelin",StandardCharsets.UTF_8);album[0]=0;raw(s,7,album);check("display-only text shown",s.snapshot().mediaPrimary().equals("Led Zeppelin")&&s.snapshot().displayTextOnly());
        int[] second=bytes("Babe I'm Gonna",StandardCharsets.UTF_8);second[0]=0;raw(s,7,second);check("rotating text kept without splitting",s.snapshot().recentDisplayTexts().contains("Led Zeppelin")&&s.snapshot().recentDisplayTexts().contains("Babe I'm Gonna"));
        check("artist not invented from album",s.snapshot().text(105).equals("—"));raw(s,3,0x82,7);check("code3 defaults to AUX after external mode",AudioMode.reported(s.snapshot())==null&&AudioMode.aux(s.snapshot()));
        s.auxSource(3);check("explicit AUX mapping hides only chosen source",AudioMode.aux(s.snapshot())&&AudioMode.reported(s.snapshot())==null);s.auxSource(4);check("reassignment leaves code3 external",AudioMode.reported(s.snapshot())==AudioMode.USB);
        s.status("offline","",false);check("disconnected metadata hidden",s.snapshot().cdTitle().equals("—"));
        s.status("test","",true);s.auxSource(5);raw(s,3,0x84,7);check("calibrated uncommon AUX label and route agree",s.snapshot().source().equals("AUX")&&AudioMode.aux(s.snapshot()));
        s.auxSource(-1);raw(s,3,0x83,7);int[] pairFile=bytes("Led Zeppelin - Dazed and Confused.mp3",StandardCharsets.UTF_8);pairFile[0]=5;raw(s,7,pairFile);check("filename split is visible fallback",s.snapshot().cdTitle().equals("Dazed and Confused")&&s.snapshot().cdArtist().equals("Led Zeppelin")&&s.snapshot().splitInferred());
        raw(s,7,title);raw(s,7,artist);check("actual fields override inferred pair",s.snapshot().cdTitle().equals("Название трека")&&s.snapshot().cdArtist().equals("Исполнитель")&&!s.snapshot().splitInferred());
        String value;try(InputStream in=assets.open("device-logs-v071.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);value=new String(out.toByteArray(),StandardCharsets.UTF_8);}
        JSONArray cases=new JSONArray(value);boolean observedFilename=false,observedLed=false,observedLost=false;int transitions=0;
        for(int i=0;i<cases.length();i++){ProbeState replay=new ProbeState();long session=-1;Integer before=null;JSONArray chunks=cases.getJSONObject(i).getJSONArray("chunks");
            for(int j=0;j<chunks.length();j++){JSONObject c=chunks.getJSONObject(j);if(session!=c.getLong("session")){session=c.getLong("session");replay.newSession();replay.running(true);replay.status("Replay","",true);replay.event(1000,new int[]{6291908},null,null);before=null;}
                String[] parts=c.getString("hex").split(" ");int[] a=new int[parts.length];for(int k=0;k<a.length;k++)a[k]=Integer.parseInt(parts[k],16);replay.event(1019,a,null,null);
                ProbeState.Snapshot snap=replay.snapshot();observedFilename|=!snap.mediaFilename().equals("—");observedLed|=snap.mediaPrimary().startsWith("Led Zeppelin");observedLost|=snap.bsjTextLost;
                if(Integer.valueOf(2).equals(before)&&Integer.valueOf(3).equals(snap.audioSource())){check("recorded CD to AUX has no player",AudioMode.reported(snap)==null&&AudioMode.aux(snap));transitions++;}before=snap.audioSource();}
            JSONObject exported=new JSONObject(replay.exportJson());check("real log text exported "+i,exported.has("bsjMediaText"));}
        check("actual CD/file-name text recovered",observedFilename);check("actual Led Zeppelin text recovered",observedLed);check("actual unsupported Cyrillic loss identified",observedLost);check("multiple real wheel transitions: "+transitions,transitions>=3);return checks;
    }
}
