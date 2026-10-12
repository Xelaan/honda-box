package com.crewbeat.hondabox;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONObject;

/** Two bounded diagnostic counters/samples. Input comes only from checksum-valid frames. */
public final class BsjVehiclePackets {
    public static final class Sample {
        public final int type,length;
        public final long count,mappedCount,at,firstAt;
        public final String payloadHex;
        public final Map<Integer,Integer> fields;
        private Sample(int type,int[] payload,long at,Sample previous){
            this.type=type;length=payload.length;this.at=at;
            count=previous==null?1:previous.count+1;firstAt=previous==null?at:previous.firstAt;
            payloadHex=BsjAnalyzer.hex(payload);
            Map<Integer,Integer> decoded=new LinkedHashMap<>();
            if(type==0x0E){int[] values=CarInfoPacket.decode(payload);if(values!=null)for(int i=0;i<values.length;i++)decoded.put(140+i,values[i]);}
            else if(type==0x10&&(payload.length==1||payload.length==2)){decoded.put(133,(payload[0]>>>5)&7);decoded.put(134,payload[0]&31);}
            fields=Collections.unmodifiableMap(decoded);
            mappedCount=(previous==null?0:previous.mappedCount)+(fields.isEmpty()?0:1);
        }
        public JSONObject json()throws Exception{
            JSONObject decoded=new JSONObject();for(Map.Entry<Integer,Integer> e:fields.entrySet())decoded.put(String.valueOf(e.getKey()),e.getValue());
            return new JSONObject().put("type",type).put("payloadLength",length).put("receivedCount",count)
                .put("mappedCount",mappedCount).put("firstAtMs",firstAt).put("lastAtMs",at).put("payloadHex",payloadHex).put("stockFieldValues",decoded);
        }
    }
    private Sample packet0E,packet10;
    public void accept(int type,int[] payload,long at){
        if(type==0x0E)packet0E=new Sample(type,payload,at,packet0E);
        else if(type==0x10)packet10=new Sample(type,payload,at,packet10);
    }
    public Sample packet0E(){return packet0E;}
    public Sample packet10(){return packet10;}
    public void clear(){packet0E=packet10=null;}
    public JSONObject json()throws Exception{
        return new JSONObject().put("type0E",packet0E==null?JSONObject.NULL:packet0E.json())
            .put("type10",packet10==null?JSONObject.NULL:packet10.json())
            .put("note","Profile6291908; checksum-valid frames only. Latest sample plus counts per type, collected even with recording disabled. Reset on log reset, connection session or profile change. TYPE0E exact length13 maps to fields140..147; field semantics/units and D0E relationship remain unverified. TYPE10 length1/2 byte0 maps to fields133/134; a second byte is unassigned. Other lengths retained raw without guessed decoding.");
    }
}
