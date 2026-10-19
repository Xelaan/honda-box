package com.crewbeat.hondabox;

import java.util.Locale;
import java.util.Map;

/** Received transport evidence, with no guessed capabilities or source transitions. */
public final class BsjDataInfo {
    private static final int[] TYPES={1,2,3,4,5,6,7,9,12,14,16,17,127};
    private static final int[] LABELS={R.string.debug_data_keys,R.string.debug_data_climate,R.string.debug_data_source_volume,
        R.string.debug_data_radio,R.string.debug_data_playback,R.string.debug_data_discs,R.string.debug_data_titles,
        R.string.debug_data_menu,R.string.debug_data_standby,R.string.debug_data_vehicle,R.string.debug_data_audio,
        R.string.debug_data_status,R.string.debug_data_identity};
    private static boolean listed(int type){for(int t:TYPES)if(t==type)return true;return false;}
    private static void sample(StringBuilder b,int type,String label,BsjProtocolEvidence.Sample sample,long now){
        b.append(String.format(Locale.US,"%02X",type)).append(" · ").append(label).append(": ");
        if(sample==null)b.append(I18n.t(R.string.debug_data_not_received));
        else b.append(sample.count).append(" × · len=").append(sample.length).append(" · ")
            .append(Math.max(0,(now-sample.at)/1000)).append(I18n.t(R.string.debug_data_seconds_ago));
        b.append('\n');
    }
    private static String transport(ProbeState.Snapshot s,int field){
        ProbeState.Value raw=s.bsjValues.get(field);Integer value=s.displayNumber(field);
        if(value==null)return I18n.t(R.string.debug_data_not_received);
        return (raw!=null?"BSJ 03":"FYT ["+field+"]")+" → "+value;
    }
    public static String describe(ProbeState.Snapshot s){
        StringBuilder b=new StringBuilder(I18n.t(s.connected?R.string.debug_data_live:R.string.debug_data_history)).append('\n');
        b.append(I18n.t(R.string.debug_data_source_volume)).append(": ")
            .append(transport(s,98)).append(" / ").append(transport(s,132)).append('\n');
        long now=System.currentTimeMillis();
        for(int i=0;i<TYPES.length;i++)sample(b,TYPES[i],I18n.t(LABELS[i]),s.protocolEvidence.get(TYPES[i]),now);
        for(Map.Entry<Integer,BsjProtocolEvidence.Sample> e:s.protocolEvidence.entrySet())if(!listed(e.getKey()))sample(b,e.getKey(),I18n.t(R.string.debug_data_unassigned),e.getValue(),now);
        return b.toString().trim();
    }
    private BsjDataInfo(){}
}
