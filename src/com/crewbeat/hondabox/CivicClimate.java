package com.crewbeat.hondabox;
import java.util.Arrays;
import org.json.JSONObject;
/** LZ/HZ Civic 2006-2011 climate only. No Accord source/key semantics. */
public final class CivicClimate {
 public static boolean supports(Integer profile){return profile!=null&&(profile==15729093||profile==15794629);}
 public static final class Sample {
  private final int[] payload;public final long at;
  private Sample(int[] p,long at){payload=p.clone();this.at=at;}
  public Integer field(int code){switch(code){
   case 11:return (payload[0]>>>6)&1;case 13:return (payload[0]>>>3)&1;
   case 18:return (payload[1]>>>7)&1;case 19:return (payload[1]>>>6)&1;case 20:return (payload[1]>>>5)&1;
   case 21:return (payload[1]&15)<=7?payload[1]&15:null;
   case 37:return payload[4]&1;
   default:return null;
  }}
  public Integer power(){
   // No power bit in the observed profile. AUTO/airflow speed or a displayed temperature mean active.
   if(field(13)==1||(field(21)!=null&&field(21)>0)||payload[2]!=255||payload[3]!=255)return 1;
   return field(21)==null?null:0;
  }
  public boolean same(Sample other){return other!=null&&Arrays.equals(payload,other.payload);}
  public JSONObject json()throws Exception{return new JSONObject().put("type",40).put("payloadHex",BsjAnalyzer.hex(payload)).put("receivedAtMs",at).put("inferredPower",power());}
 }
 public static Sample decode(int type,int[] p,long at){if(type!=0x28||p==null||p.length!=6)return null;for(int b:p)if(b<0||b>255)return null;return new Sample(p,at);}
 private CivicClimate(){}
}
