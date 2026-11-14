package com.crewbeat.hondabox;
import java.util.Locale;
import org.json.JSONObject;
/** Experimental TYPE 0A clock; never sets Android time or infers AM/PM. */
public final class BsjClock {
 public static final class Sample {
  public final String payloadHex,candidate;public final long at,count;
  Sample(int[] p,long at,long count,boolean honda){
   this.at=at;this.count=count;StringBuilder b=new StringBuilder();
   for(int v:p){if(b.length()>0)b.append(' ');b.append(String.format(Locale.ROOT,"%02X",v&255));}
   payloadHex=b.toString();candidate=honda?decode(p):null;
  }
  public JSONObject json()throws Exception{return new JSONObject().put("payloadHex",payloadHex).put("lastAtMs",at).put("receivedCount",count).put("candidateTime",candidate==null?JSONObject.NULL:candidate).put("verified",false);}
 }
 public static String decode(int[] p){
  // Only the observed 01 Fx mm layout. Hours 10..12 are not established.
  if(p==null||p.length!=3||p[0]!=1||p[1]<0xF0||p[1]>0xF9||p[2]<0||p[2]>0x59||(p[2]&15)>9)return null;
  return String.format(Locale.ROOT,"%d:%02d",p[1]&15,(p[2]>>>4)*10+(p[2]&15));
 }
 private Sample latest;
 public void accept(int[] p,long at,boolean honda){latest=new Sample(p,at,latest==null?1:latest.count+1,honda);}
 public Sample latest(){return latest;}
 public void clear(){latest=null;}
 public JSONObject json()throws Exception{return new JSONObject().put("latest",latest==null?JSONObject.NULL:latest.json()).put("note","Experimental TYPE0A clock; unverified against OEM display. Header, AM/PM and hours 10..12 unknown. No system time changes. Captured without recording; reset on log reset, reconnect or profile change.");}
}
