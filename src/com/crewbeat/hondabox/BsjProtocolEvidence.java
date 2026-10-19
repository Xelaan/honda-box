package com.crewbeat.hondabox;

import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Connection-level evidence, independent of diagnostic recording. At most 256 immutable samples. */
public final class BsjProtocolEvidence {
    public static final class Sample {
        public final int type,length;
        public final long count,at,firstAt;
        private Sample(int type,int length,long at,Sample old){
            this.type=type;this.length=length;this.at=at;
            count=old==null?1:old.count+1;firstAt=old==null?at:old.firstAt;
        }
        public JSONObject json()throws Exception{
            return new JSONObject().put("type",type).put("lastPayloadLength",length)
                .put("receivedCount",count).put("firstAtMs",firstAt).put("lastAtMs",at);
        }
    }
    private final Map<Integer,Sample> samples=new LinkedHashMap<>();
    public void accept(int type,int[] payload,long at){
        if(type<0||type>255||payload==null)return;
        samples.put(type,new Sample(type,payload.length,at,samples.get(type)));
    }
    public Map<Integer,Sample> snapshot(){return new LinkedHashMap<>(samples);}
    public void clear(){samples.clear();}
    public JSONObject json()throws Exception{
        JSONObject types=new JSONObject();for(Map.Entry<Integer,Sample> e:samples.entrySet())types.put(String.valueOf(e.getKey()),e.getValue().json());
        return new JSONObject().put("types",types).put("note","Checksum-valid BSJ packets received since this connection/profile began, even with recording off. Log reset retains these counters; session/profile change clears them. Counts prove receipt only, not successful decoding or hardware support. FYT initial/default callbacks and absence of packets do not establish support. No packet history or payload retained here.");
    }
}
