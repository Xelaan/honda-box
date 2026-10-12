package com.crewbeat.hondabox;

import java.util.Locale;

/** Tentative interpretation of the latest validated 0x0C frame. Used as a source-scoped display fallback behind normal playback. */
public final class BsjStandbyDisplay {
    public enum Layout { EMPTY, TEXT, POSSIBLE_PLAYBACK, UNKNOWN }
    public final long at;
    public final String raw, text;
    public final Layout layout;
    public final int firstByte, track, minutes, seconds;

    public BsjStandbyDisplay(int[] payload, long receivedAt) {
        at=receivedAt;
        StringBuilder hex=new StringBuilder();
        for(int b:payload){if(hex.length()>0)hex.append(' ');hex.append(String.format(Locale.ROOT,"%02X",b&255));}
        raw=hex.toString();firstByte=payload.length==0?-1:payload[0];
        Layout candidate=Layout.UNKNOWN;String possibleText="";int t=-1,m=-1,s=-1;
        if(payload.length==17){
            boolean empty=true;for(int b:payload)if(b!=0)empty=false;
            if(empty)candidate=Layout.EMPTY;
            else if(payload[0]==0){
                possibleText=ascii(payload,1);
                if(!possibleText.isEmpty())candidate=Layout.TEXT;
                // Observed prefix only. ASCII TITLE bytes must not be treated as counters.
                else if(payload[1]==0 && payload[2]==1 && (payload[3]==1||payload[3]==2)
                    && bcd(payload[4])>=0 && bcd(payload[5])>=0
                    && bcd(payload[6])>=0 && bcd(payload[7])>=0 && bcd(payload[7])<60){
                    candidate=Layout.POSSIBLE_PLAYBACK;
                    t=bcd(payload[4])*100+bcd(payload[5]);m=bcd(payload[6]);s=bcd(payload[7]);
                    possibleText=ascii(payload,8);
                }
            }
        }
        layout=candidate;text=possibleText;track=t;minutes=m;seconds=s;
    }
    private static int bcd(int v){return v>=0&&v<=255&&(v&15)<=9&&(v>>>4)<=9?(v>>>4)*10+(v&15):-1;}
    private static String ascii(int[] p,int start){
        StringBuilder out=new StringBuilder();boolean ended=false;
        for(int i=start;i<p.length;i++){
            int b=p[i];if(b==0){ended=true;continue;}
            if(ended||b<32||b>126)return "";
            out.append((char)b);
        }
        return out.toString().trim();
    }
}
