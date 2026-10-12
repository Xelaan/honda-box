package com.crewbeat.hondabox.tests;
import com.crewbeat.hondabox.*;
import android.content.res.AssetManager;
import java.io.*;
import org.json.*;
final class CleanMedia0810Tests {
 private int checks;private void check(String name,boolean ok){if(!ok)throw new AssertionError(name);checks++;}
 private ProbeState model(int source){ProbeState s=new ProbeState();s.newSession();s.status("test","",true);s.event(1000,new int[]{6291908},null,null);s.event(98,new int[]{source},null,null);return s;}
 private void raw(ProbeState s,int type,int... p){int[] a=new int[p.length+4];a[0]=46;a[1]=type;a[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){a[i+3]=p[i];sum+=p[i];}a[a.length-1]=(~sum)&255;s.event(1019,a,null,null);}
 private void text(ProbeState s,int kind,String text){int[] a=new int[text.length()+1];a[0]=kind;for(int i=0;i<text.length();i++)a[i+1]=text.charAt(i);raw(s,7,a);}
 int run(AssetManager assets)throws Exception {
  String input;try(InputStream in=assets.open("cd-ipod-clean-v0810.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[4096];for(int n;(n=in.read(a))!=-1;)b.write(a,0,n);input=b.toString("UTF-8");}
  JSONArray chunks=new JSONObject(input).getJSONArray("chunks");ProbeState replay=model(0);
  for(int i=0;i<chunks.length();i++){String[] hex=chunks.getJSONObject(i).getString("hex").split(" ");int[] a=new int[hex.length];for(int k=0;k<a.length;k++)a[k]=Integer.parseInt(hex[k],16);replay.event(1019,a,null,null);}
  check("all 35 split raw packets reassembled",replay.snapshot().bsjFramesAccepted==35&&replay.snapshot().bsjFramesRejected==0);
  check("new subtype8 clean log text received",replay.snapshot().combinedMediaText().equals("lissa"));
  check("source packet absence stays unknown",replay.snapshot().audioSource()==0&&AudioMode.reported(replay.snapshot())==null);
  check("subtype8 does not falsely classify iPod",replay.snapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN);
  check("new text fields exported",new JSONObject(replay.exportJson()).getJSONObject("bsjMediaText").getJSONObject("displayFieldsBySubtype").getJSONObject("8").getJSONArray("strings").getString(0).equals("lissa"));
  ProbeState s=model(2);raw(s,5,1,0,0,1,0,5,0,33);text(s,8,"Known CD artist");
  check("CD gets subtype8 too",s.snapshot().combinedMediaText().equals("Known CD artist")&&AudioMode.reported(s.snapshot())==AudioMode.CD);
  raw(s,5,0,0,0,0,0,0,0,0);check("volume blank retains CD text and track",s.snapshot().combinedMediaText().equals("Known CD artist")&&s.snapshot().cdNumber(101).equals("5"));
  text(s,7,"Babe I'm Gonna>");text(s,7,"Gonna Leave You");text(s,6,"Led Zeppelin");check("only selected field shown, other scrolling field cached",s.snapshot().combinedMediaText().equals("Led Zeppelin")&&s.snapshot().bsjExtraTexts.get(7).strings[0].equals("Babe I'm Gonna Leave You"));
  text(s,8,"NO INFO");check("NO INFO retains same-track value",s.snapshot().combinedMediaText().contains("Known CD artist")&&!s.snapshot().combinedMediaText().contains("NO INFO"));
  raw(s,5,1,0,0,1,0,6,0,0);check("new track clears all extra fields",s.snapshot().combinedMediaText().equals("—"));
  raw(s,7,8,183,183,32,65);check("lost Russian uses boxes",s.snapshot().combinedMediaText().equals("□□ A")&&s.snapshot().bsjTextLost);
  raw(s,3,0x83,7);check("new source clears extra fields",s.snapshot().combinedMediaText().equals("—"));
  text(s,8,"New USB artist");raw(s,5,0,9,0,0,0,0,0,0);check("No Data clears extra fields",s.snapshot().combinedMediaText().equals("—"));
  ProbeState other=model(2);other.event(1000,new int[]{1234},null,null);text(other,8,"Wrong profile");check("different profile unaffected",other.snapshot().bsjExtraTexts.isEmpty());
  ProbeState title=model(0);String titleInput;
  try(InputStream in=assets.open("title-cycle-v0810.json")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[4096];for(int n;(n=in.read(a))!=-1;)b.write(a,0,n);titleInput=b.toString("UTF-8");}
  JSONArray titleChunks=new JSONObject(titleInput).getJSONArray("chunks");
  for(int i=0;i<titleChunks.length();i++){String[] hex=titleChunks.getJSONObject(i).getString("hex").split(" ");int[] a=new int[hex.length];for(int k=0;k<a.length;k++)a[k]=Integer.parseInt(hex[k],16);title.event(1019,a,null,null);}
  ProbeState.Snapshot ts=title.snapshot();String combined=ts.combinedMediaText();
  check("TITLE log all packets replayed",ts.bsjFramesAccepted==58&&ts.bsjFramesRejected==0);
  check("TITLE artist cached",ts.bsjExtraTexts.get(8).strings[0].equals("Tim McMorris"));
  check("TITLE partial song cached",ts.bsjExtraTexts.get(7).strings[0].equals("It's a Beautifu…"));
  check("TITLE selected empty album shown alone",combined.equals("NO INFO")&&ts.currentTitleField()==6);
  check("TITLE USB recognized but raw source preserved",ts.audioSource()==0&&ts.mediaDevice==ProbeState.MediaDevice.USB&&AudioMode.reported(ts)==AudioMode.USB&&ts.source().equals("USB"));
  check("TITLE subtype7 independent from subtype8",ts.bsjExtraTexts.get(7).strings[0].startsWith("It's a Beautifu")&&ts.bsjExtraTexts.get(8).strings[0].equals("Tim McMorris"));
  text(title,7,"Beautiful Day");check("TITLE scrolling continuation stays in song field",title.snapshot().combinedMediaText().contains("It's a Beautiful Day"));
  ProbeState usb=model(2);raw(usb,5,0,0x70,1,2,0,1,1,0);check("USB playback overrides stale CD routing",AudioMode.reported(usb.snapshot())==AudioMode.USB&&usb.snapshot().audioSource()==2);
  raw(usb,5,1,0,1,1,0,3,0,10);check("CD playback clears USB inference",AudioMode.reported(usb.snapshot())==AudioMode.CD);
  ProbeState aux=model(3);raw(aux,5,0,0x70,1,2,0,1,1,0);check("inactive USB frame does not override AUX",AudioMode.aux(aux.snapshot())&&aux.snapshot().mediaDevice==ProbeState.MediaDevice.UNKNOWN);
  return checks;
 }
}
