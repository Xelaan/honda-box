package com.crewbeat.hondabox;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only diagnostic channels. These codes belong to their module, not CAN DATA[]. */
public final class AudioRouteCapture {
    public static final int MAIN = 0, IPOD = 5;
    private static final int[] MAIN_CODES = {0,168};
    private static final int[] IPOD_CODES = {0,3,8,9,10,11,12,14,15,17,18};
    private static final int MAX_EVENTS = 1024, MAX_ARRAY = 32, MAX_STRINGS = 4, MAX_TEXT = 256;
    private final Map<Integer,JSONObject> modules = new LinkedHashMap<>();
    private final Map<String,JSONObject> values = new LinkedHashMap<>();
    private final ArrayDeque<JSONObject> events = new ArrayDeque<>();
    private long session, count, sequence, dropped;
    private boolean live,recording;
    public synchronized void recording(boolean on){recording=on;}

    public static int[] codes(int module) {
        return module==MAIN ? MAIN_CODES.clone() : module==IPOD ? IPOD_CODES.clone() : new int[0];
    }
    private static boolean accepts(int module,int code) {
        for(int allowed:codes(module)) if(allowed==code) return true;
        return false;
    }
    private static String name(int module) { return module==MAIN?"MAIN":module==IPOD?"IPOD":"unknown"; }
    public synchronized void reset(long nextSession) {
        session=nextSession; live=false; count=0; modules.clear(); values.clear();
    }
    public synchronized void status(int module,String status) {
        if(codes(module).length==0) return;
        try {
            modules.put(module,new JSONObject().put("module",module).put("name",name(module))
                .put("requestedCodes",new JSONArray(codes(module))).put("status",trim(status,300)));
        } catch(Exception ignored) { }
    }
    public synchronized void clearHistory(){events.clear();sequence=dropped=count=0;}
    public synchronized void start() { live=true; }
    public synchronized void stop() { live=false; }
    private static String trim(String value,int length) { return value==null?null:value.substring(0,Math.min(value.length(),length)); }
    public synchronized void event(int module,int code,int[] ints,float[] floats,String[] strings) {
        if(!live||!accepts(module,code)) return;
        try {
            JSONObject value=new JSONObject().put("atMs",System.currentTimeMillis()).put("session",session)
                .put("sequence",++sequence).put("module",module).put("name",name(module)).put("code",code);
            value.put("ints",ints==null?JSONObject.NULL:new JSONArray(Arrays.copyOf(ints,Math.min(ints.length,MAX_ARRAY))));
            JSONArray f=new JSONArray(); if(floats!=null) for(int i=0;i<Math.min(floats.length,MAX_ARRAY);i++)
                f.put(Float.isNaN(floats[i])||Float.isInfinite(floats[i])?JSONObject.NULL:floats[i]);
            value.put("floats",floats==null?JSONObject.NULL:f);
            JSONArray s=new JSONArray(); boolean textTrimmed=false;
            if(strings!=null) for(int i=0;i<Math.min(strings.length,MAX_STRINGS);i++) {
                textTrimmed|=strings[i]!=null&&strings[i].length()>MAX_TEXT;
                s.put(strings[i]==null?JSONObject.NULL:trim(strings[i],MAX_TEXT));
            }
            value.put("strings",strings==null?JSONObject.NULL:s)
                .put("originalInts",ints==null?0:ints.length).put("originalFloats",floats==null?0:floats.length)
                .put("originalStrings",strings==null?0:strings.length)
                .put("truncated",(ints!=null&&ints.length>MAX_ARRAY)||(floats!=null&&floats.length>MAX_ARRAY)
                    ||(strings!=null&&strings.length>MAX_STRINGS)||textTrimmed);
            values.put(module+":"+code,value); if(recording){events.addLast(value); count++;}
            while(events.size()>MAX_EVENTS) { events.removeFirst(); dropped++; }
        } catch(Exception ignored) { }
    }
    private Integer number(int module,int code) {
        JSONObject v=values.get(module+":"+code); JSONArray a=v==null?null:v.optJSONArray("ints");
        return a==null||a.length()==0||a.isNull(0)?null:a.optInt(0);
    }
    public synchronized String summary() {

        if(!live) return I18n.t(R.string.ui_audio_route_diagnostics_stopped_data);
        Integer app=number(MAIN,0), device=number(IPOD,0), exists=number(IPOD,15);
        String channel=app==null?I18n.t(R.string.ui_no_main_response):app==7?"iPod [7]":app==13?I18n.t(R.string.ui_vehicle_audio_13):I18n.t(R.string.ui_code_301)+app;
        return I18n.t(R.string.ui_head_unit_channel)+channel+" · iPod state: "+(device==null?"—":device)
            +I18n.t(R.string.ui_device)+(exists==null?"—":exists)+" · callbacks: "+count;
    }
    public synchronized Integer mainScreenOn(){return live?number(MAIN,168):null;}
    public synchronized Integer mainChannel(){return live?number(MAIN,0):null;}
    private JSONObject context() throws Exception {
        JSONObject last=new JSONObject(); for(Map.Entry<String,JSONObject> v:values.entrySet()) last.put(v.getKey(),v.getValue());
        return new JSONObject().put("live",live).put("session",session).put("callbacksThisSession",count).put("lastValues",last);
    }
    public synchronized JSONObject markerContext() throws Exception { return new JSONObject(context().toString()); }
    public synchronized JSONObject json() throws Exception {
        JSONArray state=new JSONArray(); for(JSONObject m:modules.values()) state.put(m);
        return context().put("modules",state).put("receivedEvents",sequence).put("droppedEvents",dropped)
            .put("maxEvents",MAX_EVENTS).put("maxArrayValues",MAX_ARRAY).put("maxStrings",MAX_STRINGS).put("maxStringChars",MAX_TEXT)
            .put("note","Read-only MAIN module0/code0 and IPOD module5 legacy fields. Namespaces are separate from CAN. MAIN7 denotes the head-unit iPod channel, not an Accord BSJ subtype; MAIN13 covers OEM audio. Missing callbacks are unknown, not disconnected. Initial values and inactive media can be stale. No automatic USB/iPod classification is inferred. Events retain session IDs across reconnects.")
            .put("events",new JSONArray(events));
    }
}
