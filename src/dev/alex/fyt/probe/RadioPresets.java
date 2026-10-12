package dev.alex.fyt.probe;
import java.util.*;
import org.json.*;

/** Received Honda preset frequencies, isolated by band; never requests or invents stations. */
public final class RadioPresets {
    public static final class Entry {
        public final int band,slot,index;public final long at;
        Entry(int b,int s,int n,long t){band=b;slot=s;index=n;at=t;}
        JSONObject json()throws Exception{return new JSONObject().put("band",band).put("slot",slot).put("index",index).put("receivedAtMs",at);}
    }
    private final Map<String,Entry> entries=new LinkedHashMap<>();private long revision;
    public synchronized void record(int profile,int band,int slot,int index,long at){
        if(profile!=6291908||band<0||band>5||slot<1||slot>6||index<1||index>65535)return;
        String key=band+":"+slot;Entry old=entries.get(key);if(old!=null&&old.index==index)return;
        entries.put(key,new Entry(band,slot,index,at));revision++;
    }
    public synchronized long revision(){return revision;}
    public synchronized Map<String,Entry> snapshot(){return new LinkedHashMap<>(entries);}
    public synchronized JSONObject json()throws Exception{JSONArray all=new JSONArray();for(Entry e:entries.values())all.put(e.json());return new JSONObject().put("profile",6291908).put("entries",all).put("note","Only received frequencies. Band-isolated cache, may be stale if presets changed while Probe was stopped. Missing entries were never received; no scan commands.");}
    public synchronized void restore(String text){try{JSONObject root=new JSONObject(text);if(root.optInt("profile")!=6291908)return;JSONArray all=root.getJSONArray("entries");for(int i=0;i<Math.min(36,all.length());i++){JSONObject e=all.getJSONObject(i);record(6291908,e.getInt("band"),e.getInt("slot"),e.getInt("index"),e.getLong("receivedAtMs"));}}catch(Exception ignored){}}
    public synchronized void clear(){entries.clear();revision++;}
}
