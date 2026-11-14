package com.crewbeat.hondabox;
/** Immutable media-session text; never persisted or copied into the CAN log. */
public final class AndroidTrack {
 public static final AndroidTrack EMPTY=new AndroidTrack("","","","","",false);
 public final String packageName,appName,mediaId,title,artist;public final boolean playing;
 public AndroidTrack(String pkg,String app,String id,String title,String artist,boolean playing){packageName=clean(pkg);appName=clean(app);mediaId=clean(id);this.title=clean(title);this.artist=clean(artist);this.playing=playing;}
 private static String clean(String s){return s==null?"":s.replaceAll("\\s+"," ").trim();}
 public boolean usable(){return !packageName.isEmpty()&&!title.isEmpty();}
 public String identity(){return packageName+"\n"+(mediaId.isEmpty()?"title:"+title.toLowerCase(java.util.Locale.ROOT):"id:"+mediaId);}
 public String display(){return artist.isEmpty()?title:title+" · "+artist;}
}
