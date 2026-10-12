package dev.alex.fyt.probe;
/** Real model/checksum-valid frames; no service, UI or hardware commands. */
public final class MediaLoadingHarness {
 private static int checks;
 private static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
 private static void packet(ProbeState s,int type,int... p){IpodMarkerStateHarness.frame(s,type,p);}
 private static void text(ProbeState s,String title){int[] p=new int[title.length()+1];p[0]=7;for(int i=0;i<title.length();i++)p[i+1]=title.charAt(i);packet(s,7,p);}
 private static void playback(ProbeState s,boolean usb,int state,int track,int second){packet(s,5,usb?0:1,0,state,usb?2:1,0,track,0,second);}
 public static void main(String[] args){
  for(boolean usb:new boolean[]{false,true}){
   ProbeState s=IpodMarkerStateHarness.state(usb?4:2,5);
   playback(s,usb,0,1,0);check(s.snapshot().mediaLoading()&&s.overlaySnapshot().mediaLoading(),"new track starts loading in both snapshots");
   check(s.overlaySnapshot().combinedMediaText().equals("—"),"loading is UI state, never a song title");
   playback(s,usb,0,1,0x12);check(s.overlaySnapshot().cdTime().equals("00:12"),"loading leaves playback counters intact");
   playback(s,usb,1,1,0x13);check(!s.overlaySnapshot().mediaLoading(),"text-ready state ends loading even before text arrives");
   text(s,"Babe I'm Gonna>");text(s,"Gonna Leave You");check(s.overlaySnapshot().combinedMediaText().equals("Gonna Leave You"),"matching text packets replace rather than concatenate");
   packet(s,3,usb?0x83:0x81,12);playback(s,usb,0,1,0x14);
   check(!s.overlaySnapshot().mediaLoading()&&s.overlaySnapshot().combinedMediaText().equals("Gonna Leave You"),"same track loading flag preserves title during volume");
   check(s.overlaySnapshot().cdTime().equals("00:14"),"same track flag preserves timer");
   playback(s,usb,0,2,0);check(s.overlaySnapshot().mediaLoading()&&s.overlaySnapshot().combinedMediaText().equals("—"),"real next track waits without previous title");
   text(s,"Different song");check(!s.overlaySnapshot().mediaLoading()&&s.overlaySnapshot().combinedMediaText().equals("Different song"),"actual text wins even before state byte updates");
   playback(s,usb,0,3,0);playback(s,usb,1,3,1);text(s,"NO INFO");check(!s.overlaySnapshot().mediaLoading()&&s.overlaySnapshot().combinedMediaText().equals("NO INFO"),"NO INFO is a complete received display value");
   playback(s,usb,2,4,0);check(!s.overlaySnapshot().mediaLoading(),"unknown state is not guessed to be loading");
   playback(s,usb,0,5,0);s.status("test","disconnected",false);check(!s.overlaySnapshot().mediaLoading(),"disconnection disables loading");
   s.status("test","live",true);packet(s,5,0,9,0,0,0,0,0,0);check(!s.overlaySnapshot().mediaLoading(),"NO DATA clears loading state");
   playback(s,usb,0,1,0);packet(s,3,0x82,12);check(!s.overlaySnapshot().mediaLoading(),"AUX source switch clears loading");
   s.newSession();check(!s.overlaySnapshot().mediaLoading(),"session reset clears loading");
  }
  ProbeState s=IpodMarkerStateHarness.state(2,5);playback(s,false,0,1,0);packet(s,6,0x32,0x22,0x22);
  check(s.overlaySnapshot().mediaDevice==ProbeState.MediaDevice.IPOD&&!s.overlaySnapshot().mediaLoading(),"iPod inference does not inherit CD loading state");
  s=IpodMarkerStateHarness.state(2,5);playback(s,false,0,0x15,0);ProbeState.Snapshot a=s.overlaySnapshot();long start=a.mediaTrackStartedNanos;
  check(a.cdNumber(101).equals("15")&&a.cdTrackIntro(start),"BCD track15 starts two-second intro on receipt");
  check(a.cdTrackIntro(start+1999999999L)&&!a.cdTrackIntro(start+2000000000L),"intro ends exactly at two seconds");
  playback(s,false,0,0x15,1);packet(s,3,0x81,12);playback(s,false,1,0x15,2);
  check(s.overlaySnapshot().mediaTrackStartedNanos==start&&!s.overlaySnapshot().cdTrackIntro(start+3000000000L),"timer packets, readiness and volume do not restart intro");
  playback(s,false,0,0x16,0);a=s.overlaySnapshot();check(a.mediaTrackStartedNanos!=start&&a.cdTrackIntro(a.mediaTrackStartedNanos),"new track gets its own intro");
  long next=a.mediaTrackStartedNanos;packet(s,5,2,0,0,1,0,0x16,0,0);a=s.overlaySnapshot();check(a.mediaTrackStartedNanos!=next&&a.cdTrackIntro(a.mediaTrackStartedNanos),"same track number on another disc starts new intro");
  s.status("test","disconnected",false);check(!s.overlaySnapshot().cdTrackIntro(a.mediaTrackStartedNanos),"disconnection suppresses intro");
  s.newSession();check(s.overlaySnapshot().mediaTrackStartedNanos==0,"new session clears intro clock");
  System.out.println("PASS: "+checks+" replacement/loading model checks");
 }
}
