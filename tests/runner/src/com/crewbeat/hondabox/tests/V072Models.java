package com.crewbeat.hondabox.tests;
import com.crewbeat.hondabox.*;
import org.json.*;

final class V072Models {
    int checks;void check(String label,boolean value){if(!value)throw new AssertionError(label);checks++;}
    int run()throws Exception{
        ProbeState s=new ProbeState();s.newSession();s.running(true);s.status("test","",true);s.event(1000,new int[]{6291908},null,null);s.event(115,new int[]{2},null,null);
        for(int slot=1;slot<=6;slot++)s.event(119+slot,new int[]{320+slot},null,null);
        s.event(98,new int[]{2},null,null);s.event(98,new int[]{1},null,null);check("six received presets survive source change",s.snapshot().preset(1)==321&&s.snapshot().preset(6)==326&&!s.snapshot().fresh(120));
        s.event(115,new int[]{1},null,null);check("bands do not share presets",s.snapshot().preset(1)==null);s.event(120,new int[]{321},null,null);check("identical retained native callback not reassigned to new band",s.snapshot().preset(1)==null);
        s.event(120,new int[]{400},null,null);check("changed native preset belongs to band",s.snapshot().preset(1)==400);s.event(115,new int[]{2},null,null);check("return restores original bank",s.snapshot().preset(1)==321);
        RadioPresets restored=new RadioPresets();restored.restore(s.radioPresets.json().toString());check("persistent cache round trip",restored.snapshot().get("2:6").index==326&&restored.snapshot().get("1:1").index==400);
        int count=restored.snapshot().size();restored.record(999,2,1,50,20);restored.record(6291908,6,1,50,20);restored.record(6291908,2,7,50,20);restored.record(6291908,2,1,0,20);check("invalid preset inputs rejected",restored.snapshot().size()==count&&restored.snapshot().get("2:1").index==321);
        restored.restore("bad json");check("corrupt preferences safe",restored.snapshot().size()==count);
        BsjAnalyzer a=new BsjAnalyzer();a.begin(1,6291908,1000);check("initial raw no data",!a.snapshot().rawLive(1001)&&a.snapshot().rawAge(1001)==-1);long empty=a.mark("ENGINE_IDLE",1100);a.frame(0x0c,new int[17],1200);check("late frames do not validate empty-at-marker capture",a.snapshot().window(empty).captureQuality.equals("INVALID_NO_RAW_DATA")&&!a.snapshot().window(empty).usable(7000));
        check("fresh valid raw",a.snapshot().rawLive(2200)&&!a.snapshot().rawLive(2201));long good=a.mark("GEAR_P",1500);check("live marker valid",a.snapshot().window(good).captureQuality.equals("RAW_LIVE")&&a.snapshot().window(good).usable(6500));
        long stale=a.mark("GEAR_D",2500);check("stale marker invalid",a.snapshot().window(stale).rawAgeAtMarker==1300&&!a.snapshot().window(stale).usable(8000));
        int[] id=new int[16];byte[] ascii="XC-GBJR-0722V704".getBytes("US-ASCII");for(int i=0;i<16;i++)id[i]=ascii[i];a.frame(0x7f,id,2600);check("adapter identity received",a.snapshot().adapterId.equals("XC-GBJR-0722V704"));id[0]=0;a.frame(0x7f,id,2700);check("invalid identity cannot replace confirmed ID",a.snapshot().adapterId.equals("XC-GBJR-0722V704"));
        JSONObject root=new JSONObject();BsjAnalyzer.export(root,a.snapshot(),8000);check("raw diagnostics exported",root.getJSONObject("bsjAnalyzer").getString("rawStatus").equals("NO DATA")&&root.getJSONArray("experimentMarkers").getJSONObject(0).getString("captureQuality").equals("INVALID_NO_RAW_DATA"));
        a.begin(2,6291908,9000);check("no live data inherited by reconnect",a.snapshot().lastFrameAt==-1&&a.snapshot().adapterId.isEmpty());a.frame(3,new int[]{128,5},9200);a.clear(9300);check("clear resets analyzer freshness",!a.snapshot().rawLive(9301)&&a.snapshot().segmentFrames==0);
        check("unknown families remain unknown",BsjAnalyzer.known(0x0c).equals("Unknown")&&BsjAnalyzer.known(0x11).equals("Unknown"));return checks;
    }
}
