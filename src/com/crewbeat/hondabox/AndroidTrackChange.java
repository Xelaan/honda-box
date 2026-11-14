package com.crewbeat.hondabox;
/** Settle metadata bursts; pause/resume, artist/artwork corrections do not announce again. */
public final class AndroidTrackChange {
 private String announced="",pending="";private long changedAt;
 public void reset(){announced="";pending="";}
 public String update(AndroidTrack t,long now,boolean allowed){
  if(!allowed||!t.usable()){reset();return null;}
  if(!t.playing){pending="";return null;}
  String key=t.identity();if(key.equals(announced)){pending="";return null;}
  if(!key.equals(pending)){pending=key;changedAt=now;return null;}
  if(now-changedAt<500)return null;
  announced=key;pending="";return t.display();
 }
}
