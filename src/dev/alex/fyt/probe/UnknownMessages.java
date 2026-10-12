package dev.alex.fyt.probe;
import java.util.*;
import org.json.*;
/** Read-only, bounded diagnostics of validated BSJ packets; no guessed signal names. */
public final class UnknownMessages {
 public static final class Entry {
  public final long revision,firstAt,lastAt;public final int type,repeats;public final String reason,fields,fullHex;
  Entry(long rev,long first,long last,int type,int repeats,String reason,String fields,String full){revision=rev;firstAt=first;lastAt=last;this.type=type;this.repeats=repeats;this.reason=reason;this.fields=fields;fullHex=full;}
 }
 private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();private long revision;
 public synchronized void clear(){entries.clear();revision++;}
 public synchronized long revision(){return revision;}
 public synchronized List<Entry> latest(){ArrayList<Entry> out=new ArrayList<>(entries.values());Collections.reverse(out);return out;}
 private static String hex(int... a){StringBuilder b=new StringBuilder();for(int n:a){if(b.length()>0)b.append(' ');b.append(String.format(Locale.US,"%02X",n&255));}return b.toString();}
 /** Routine packets are not unknown just because every reserved bit is unlabelled. */
 public static String reason(int t,int[] p){
  switch(t){
   case 1:return p.length==2?null:"Key input / unexpected length";
   case 2:return p.length==6?null:"Climate / unexpected length";
   case 3:return p.length==2?null:"Audio / unexpected length";
   case 4:return p.length==5?null:"Radio / unexpected length";
   case 5:return p.length==8?null:"Media / unexpected length";
   case 6:return p.length==3?null:"CD changer / unexpected length";
   case 7:
    if(p.length<2)return "Text / unexpected length";
    switch(p[0]){case 0:case 1:case 3:case 4:case 5:case 6:case 7:case 8:return null;default:return "Text / unknown subtype";}
   case 9:
    if(p.length!=49)return "Menu / unexpected length";
    // Owner identifies header20 as the OEM SEL icon: the menu item can be selected.
    // Recognize this exact value; other headers are not inferred as bit combinations.
    return p[0]==0x20?null:"Menu / unfamiliar header";
   case 14:return p.length==13?null:"Stock vehicle fields / unexpected length";
   case 15:return p.length==2?null:"Stock notification / unexpected length";
   case 16:return p.length>=1&&p.length<=2?null:"Stock fields133/134 / unexpected length";
   case 17:return null; // Door support is out of scope by owner request; raw capture is retained.
   case 127:return p.length==16?null:"Adapter ID / unexpected length";
   default:return "Unknown type";
  }
 }
 private static String fields(int t,int[] p){return hex(p);}
 public synchronized void accept(int type,int[] p,long at){String reason=reason(type,p);if(reason==null)return;String fields=fields(type,p),key=type+"/"+p.length;Entry old=entries.remove(key);
  boolean changed=old==null||!old.fields.equals(fields)||at-old.lastAt>=8000;
  if(changed)revision++;
  int[] full=new int[p.length+4];full[0]=46;full[1]=type;full[2]=p.length;int sum=type+p.length;for(int i=0;i<p.length;i++){full[i+3]=p[i];sum+=p[i];}full[full.length-1]=(~sum)&255;
  entries.put(key,new Entry(changed?revision:old.revision,changed?at:old.firstAt,at,type,changed?1:old.repeats+1,reason,fields,hex(full)));
  while(entries.size()>32)entries.remove(entries.keySet().iterator().next());
 }
 public synchronized JSONArray json()throws Exception{JSONArray out=new JSONArray();for(Entry e:latest())out.put(new JSONObject().put("type",e.type).put("reason",e.reason).put("fields",e.fields).put("fullHex",e.fullHex).put("firstAtMs",e.firstAt).put("lastAtMs",e.lastAt).put("repeats",e.repeats));return out;}
}
