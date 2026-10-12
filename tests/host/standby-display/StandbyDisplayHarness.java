package com.crewbeat.hondabox;
public final class StandbyDisplayHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        int[] text={0,48,48,50,32,75,121,103,111,32,45,32,70,105,114,101,62};
        int[] hybrid={0,0,1,1,0,2,0,0x43,111,32,45,32,70,105,114,101,62};
        BsjStandbyDisplay d=new BsjStandbyDisplay(text,123);
        check(d.layout==BsjStandbyDisplay.Layout.TEXT&&d.text.equals("002 Kygo - Fire>")&&d.track==-1,"text is not counters");
        d=new BsjStandbyDisplay(hybrid,123);
        check(d.track==2&&d.minutes==0&&d.seconds==43&&d.text.equals("o - Fire>"),"hybrid candidate and separate tail");
        check(new BsjStandbyDisplay(new int[17],1).layout==BsjStandbyDisplay.Layout.EMPTY,"zeros are empty");
        check(new BsjStandbyDisplay(new int[3],1).layout==BsjStandbyDisplay.Layout.UNKNOWN,"unsupported size");
        hybrid[7]=0x60;check(new BsjStandbyDisplay(hybrid,1).layout==BsjStandbyDisplay.Layout.UNKNOWN,"invalid seconds");hybrid[7]=0x43;
        ProbeState s=IpodMarkerStateHarness.state(2,1);
        s.recording(false);
        IpodMarkerStateHarness.frame(s,3,new int[]{0x81,8});
        IpodMarkerStateHarness.frame(s,5,new int[]{1,0,1,1,0,9,0,0x12});
        ProbeState.Snapshot before=s.snapshot();
        IpodMarkerStateHarness.frame(s,12,hybrid);ProbeState.Snapshot after=s.snapshot();
        check(after.standbyDisplay!=null&&after.standbyDisplay.track==2,"recording off still captures diagnostic");
        check(after.audioSource().equals(before.audioSource())&&after.sourceSequence==before.sourceSequence&&after.media().equals(before.media()),"diagnostic cannot update playback");
        s.clearLog();check(s.snapshot().standbyDisplay!=null,"log reset preserves current data");
        s.event(1000,new int[]{1},null,null);IpodMarkerStateHarness.frame(s,12,text);check(s.snapshot().standbyDisplay==null,"profile gate and reset");
        s=IpodMarkerStateHarness.state(2,1);IpodMarkerStateHarness.frame(s,12,text);s.newSession();check(s.snapshot().standbyDisplay==null,"session reset");
        int texts=0,media=0,other=0;
        for(String line:java.nio.file.Files.readAllLines(java.nio.file.Paths.get(args[0]))){
            String[] parts=line.split(",");int[] p=new int[parts.length];for(int i=0;i<p.length;i++)p[i]=Integer.parseInt(parts[i]);
            d=new BsjStandbyDisplay(p,1);
            if(d.layout==BsjStandbyDisplay.Layout.TEXT)texts++;
            else if(d.layout==BsjStandbyDisplay.Layout.POSSIBLE_PLAYBACK)media++;else other++;
        }
        check(texts==9&&media==23&&other==0,"32 recorded packets: text9, hybrid23; got "+texts+","+media+","+other);
        System.out.println("PASS "+checks+" checks; replayed 32 recorded packets");
    }
}
