package dev.alex.fyt.probe;

import java.util.*;
import org.json.*;

/** Bounded, source-isolated history of received OEM menu text. Never implies an accepted selection. */
public final class CarMenuMemory {
    private static final int MAX_PER_SOURCE=8,MAX_ENTRIES=128;
    private static final class Entry {
        final int source;final String text;final long at;
        Entry(int source,String text,long at){this.source=source;this.text=text;this.at=at;}
    }
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();private long revision;
    public synchronized void record(int profile,Integer source,String[] rows,long at){
        if(profile!=6291908||source==null||source<1||source>16)return;
        StringBuilder b=new StringBuilder();for(String row:rows){String t=MediaText.display(row);if(t.equals("—")||t.isEmpty())continue;if(b.length()>0)b.append(" · ");b.append(t.substring(0,Math.min(128,t.length())));}
        String text=b.toString();if(text.isEmpty())return;String key=source+":"+text;Entry old=entries.get(key);
        // A repeated unchanged menu does not churn persistent storage.
        if(old!=null)return;entries.put(key,new Entry(source,text,at));revision++;
        int n=0;for(Entry e:entries.values())if(e.source==source)n++;
        if(n>MAX_PER_SOURCE){Iterator<Map.Entry<String,Entry>> it=entries.entrySet().iterator();while(it.hasNext())if(it.next().getValue().source==source){it.remove();break;}}
        while(entries.size()>MAX_ENTRIES)entries.remove(entries.keySet().iterator().next());
    }
    public synchronized String summary(Integer source){if(source==null)return "";StringBuilder b=new StringBuilder();for(Entry e:entries.values())if(e.source==source){if(b.length()>0)b.append('\n');b.append(e.text);}return b.toString();}
    public synchronized long revision(){return revision;}
    public synchronized JSONObject json()throws Exception {JSONArray all=new JSONArray();for(Entry e:entries.values())all.put(new JSONObject().put("source",e.source).put("text",e.text).put("receivedAtMs",e.at));return new JSONObject().put("profile",6291908).put("entries",all).put("maxPerSource",MAX_PER_SOURCE).put("note","Previously received menu text, keyed by reported CAN source. Cached pages are not live playback selections. USB and iPod may share a source; no device identity is restored from cache.");}
    public synchronized void restore(String text){try{JSONObject j=new JSONObject(text);if(j.optInt("profile")!=6291908)return;JSONArray all=j.getJSONArray("entries");for(int i=0;i<Math.min(MAX_ENTRIES,all.length());i++){JSONObject e=all.getJSONObject(i);record(6291908,e.getInt("source"),new String[]{e.getString("text")},e.getLong("receivedAtMs"));}}catch(Exception ignored){}}
    public synchronized void clear(){entries.clear();revision++;}
}
