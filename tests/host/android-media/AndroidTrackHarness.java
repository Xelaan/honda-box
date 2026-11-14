package com.crewbeat.hondabox;
/** Android-free announcement identity/debounce checks. */
public final class AndroidTrackHarness {
 private static int count;private static void check(boolean b,String label){count++;if(!b)throw new AssertionError(label);}
 private static AndroidTrack track(String id,String title,String artist,boolean play){return new AndroidTrack("player","Player",id,title,artist,play);}
 public static void main(String[] args){AndroidTrackChange d=new AndroidTrackChange();AndroidTrack a=track("a","Song","",true);
  check(d.update(a,0,true)==null,"settle first metadata");check(d.update(a,499,true)==null,"debounce");check("Song".equals(d.update(a,500,true)),"announce first playing track");check(d.update(a,900,true)==null,"no duplicate");
  check(d.update(track("a","Song","Artist",true),1000,true)==null,"artist arrival not new track");check(d.update(track("a","Corrected","Artist",true),1100,true)==null,"stable ID correction not new track");
  check(d.update(track("a","Corrected","Artist",false),1200,true)==null,"pause no event");check(d.update(a,2000,true)==null,"resume no duplicate");
  AndroidTrack b=track("b","Other","Artist",true);check(d.update(b,2100,true)==null,"next waits");check(d.update(a,2200,true)==null,"transient replacement cancelled");check(d.update(b,2400,true)==null,"new stable replacement starts");check("Other · Artist".equals(d.update(b,3000,true)),"next track text");
  check(d.update(b,3100,false)==null,"OEM route suppresses");check(d.update(b,4000,true)==null,"return settles");check(d.update(b,4500,true)!=null,"current song on return to AUX");
  d.reset();AndroidTrack noId=track("","No ID","",true);d.update(noId,0,true);check(d.update(noId,500,true)!=null,"title fallback without ID");check(d.update(track("","NO ID","Late artist",true),900,true)==null,"title case and late artist no duplicate");
  check(d.update(AndroidTrack.EMPTY,1000,true)==null,"destroy/empty clears");check(!track("x","  ","Artist",true).usable(),"artist alone not fabricated title");
  AndroidTrack unicode=track("ru","  Песня\n  здесь  ","Исполнитель",true);check(unicode.display().equals("Песня здесь · Исполнитель"),"Unicode and whitespace");
  System.out.println("PASS: "+count+" Android announcement checks");
 }
}
