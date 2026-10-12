package com.crewbeat.hondabox;
/** Actual model: cached 0C display survives power cycles and fills gaps without becoming a source. */
public final class StandbyPlaybackHarness {
    static int checks;
    static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
    static void frame(ProbeState s,int type,int... p){IpodMarkerStateHarness.frame(s,type,p);}
    static int[] text(String t){int[] p=new int[17];for(int i=0;i<Math.min(16,t.length());i++)p[i+1]=t.charAt(i);return p;}
    static int[] hybrid(int track,int seconds){return new int[]{0,0,1,1,0,track,0,seconds,111,32,45,32,70,105,114,101,62};}
    public static void main(String[] args)throws Exception{
        ProbeState s=IpodMarkerStateHarness.state(2,1);s.recording(false);
        frame(s,3,1,0);long source=s.snapshot().sourceSequence;
        frame(s,12,text("002 Kygo - Fire>"));frame(s,12,hybrid(2,0x43));
        ProbeState.Snapshot a=s.overlaySnapshot();
        check(a.combinedMediaText().equals("002 Kygo - Fire…"),"separate text survives numeric packet: " + a.combinedMediaText()+" key "+a.source()+" cache "+(a.standbyText==null?"null":a.standbyText.text));
        check(a.cdNumber(101).equals("2")&&a.cdTime().equals("00:43"),"cached counters fill missing display");
        check(a.cdNumber(113).equals("—")&&a.currentTitleField()==null,"disc and TITLE selector not invented");
        check(a.audioSource()==2&&a.sourceSequence==source&&!a.fresh(101),"does not alter source or freshness");
        frame(s,3,0x81,8);a=s.snapshot();
        check(a.usingStandbyText()&&a.combinedMediaText().equals("002 Kygo - Fire…"),"power-on preserves cached text");
        check(a.cdTime().equals("00:43"),"power-on preserves timer until new data");
        frame(s,12,new int[17]);check(s.snapshot().combinedMediaText().equals("002 Kygo - Fire…"),"empty standby packet retains values");
        frame(s,5,1,0,1,1,0,2,0,0x59);a=s.snapshot();
        check(a.cdTime().equals("00:59")&&!a.usingStandbyTime(),"normal counter takes priority");
        frame(s,12,hybrid(2,0x10));check(s.snapshot().cdTime().equals("00:59"),"older standby timer cannot rewind normal timer");
        frame(s,7,1,78,101,119,32,116,105,116,108,101);a=s.snapshot();
        check(a.combinedMediaText().equals("New title")&&!a.usingStandbyText(),"new normal title supersedes cache");
        frame(s,12,text("002 Old title>"));check(s.snapshot().combinedMediaText().equals("New title"),"standby text cannot replace normal title");
        frame(s,5,1,0,1,1,0,3,0,0x01);a=s.snapshot();
        check(a.combinedMediaText().equals("—")&&a.standbyCounters==null,"new normal track supersedes old cache");
        frame(s,12,text("002 Old title>"));frame(s,12,hybrid(2,0x43));a=s.snapshot();
        check(a.combinedMediaText().equals("—")&&a.cdNumber(101).equals("3")&&a.cdTime().equals("00:01"),"wrong-track standby content suppressed");
        frame(s,3,0x82,8);frame(s,12,text("AUX stray text"));check(s.snapshot().combinedMediaText().equals("—"),"AUX ignores media fallback");
        frame(s,3,0x80,8);frame(s,12,text("Radio stray text"));check(s.snapshot().combinedMediaText().equals("—"),"radio ignores media fallback");
        s=IpodMarkerStateHarness.state(2,1);frame(s,12,text("CD remembered"));
        frame(s,3,0x83,8);frame(s,12,text("USB remembered"));check(s.snapshot().combinedMediaText().equals("USB remembered"),"USB has separate cache");
        frame(s,3,0x81,8);check(s.snapshot().combinedMediaText().equals("CD remembered"),"return to CD keeps CD cache");
        frame(s,6,0x32,0x22,0x22);check(s.snapshot().combinedMediaText().equals("—"),"inferred iPod cannot use CD cache");
        frame(s,12,text("iPod remembered"));check(s.snapshot().combinedMediaText().equals("iPod remembered"),"iPod has own cache");
        frame(s,3,0x83,8);frame(s,5,0,0,1,2,0,1,0,1);check(s.snapshot().mediaDevice==ProbeState.MediaDevice.USB&&s.snapshot().combinedMediaText().equals("USB remembered"),"USB cannot use iPod cache");
        s.newSession();check(s.snapshot().standbyText==null,"new connection resets cache");
        int replayed=0;
        for(String path:args){
            s=IpodMarkerStateHarness.state(2,1);
            for(String line:java.nio.file.Files.readAllLines(java.nio.file.Paths.get(path))){
                String[] f=line.split(",");int type=Integer.parseInt(f[1]);int[] p=new int[f.length-3];for(int i=0;i<p.length;i++)p[i]=Integer.parseInt(f[i+3]);
                a=s.snapshot();String title=a.combinedMediaText(),timer=a.cdTime();Integer sourceBefore=a.audioSource();
                frame(s,type,p);
                if(type==12){ProbeState.Snapshot b=s.snapshot();check(java.util.Objects.equals(sourceBefore,b.audioSource()),"recorded standby does not switch source");
                    if(!title.equals("—"))check(title.equals(b.combinedMediaText()),"recorded standby keeps known text");
                    if(!timer.equals("—"))check(timer.equals(b.cdTime()),"recorded standby does not rewind timer");replayed++;}
            }
        }
        System.out.println("PASS "+checks+" checks; replayed "+replayed+" standby packets");
    }
}
