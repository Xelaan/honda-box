package com.crewbeat.hondabox;
public final class DiagnosticRecordingHarness {
 private static int checks;private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 private static void frame(ProbeState s,int type,int... p){IpodMarkerStateHarness.frame(s,type,p);}
 public static void main(String[] args){
  ProbeState s=IpodMarkerStateHarness.state(4,5);check(!s.recording(),"recording off by default");
  for(int i=0;i<1000;i++)frame(s,5,0,0,1,2,0,1,0,9);
  check(s.snapshot().rawCount==0,"raw history disabled");check(s.analyzer.snapshot().currentFrames().isEmpty(),"decoded history disabled");
  check(s.snapshot().cdNumber(101).equals("1")&&s.snapshot().cdTime().equals("00:09"),"playback decoded without recording");check(!s.snapshot().log.contains("RAW 1019"),"HEX event log disabled");
  frame(s,6,0x32,0x22,0x22);check(s.snapshot().mediaDevice==ProbeState.MediaDevice.IPOD,"iPod detection stays live");
  frame(s,12,0,0);check(!s.unknownMessages.latest().isEmpty(),"unknown overlay state stays live");
  s.recording(true);frame(s,5,0,0,1,2,0,1,0,0x10);
  check(s.snapshot().rawCount==1,"raw capture enabled");check(s.analyzer.snapshot().currentFrames().size()==1,"decoded capture enabled");check(s.snapshot().log.contains("RAW 1019"),"verbose event log enabled");
  s.recording(false);frame(s,5,0,0,1,2,0,1,0,0x11);
  check(s.snapshot().rawCount==1&&s.analyzer.snapshot().currentFrames().size()==1,"turning off stops histories but retains capture");check(s.snapshot().cdTime().equals("00:11"),"data updates after stopping recording");
  s.clearLog();check(s.snapshot().rawCount==0&&s.analyzer.snapshot().currentFrames().isEmpty(),"reset clears old capture while off");check(s.snapshot().cdTime().equals("00:11"),"reset keeps current playback");
  System.out.println("PASS: "+checks+" diagnostic recording checks");
 }
}
