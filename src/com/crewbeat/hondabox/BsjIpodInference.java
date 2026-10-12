package com.crewbeat.hondabox;
/** Exact marker06 selects iPod from non-USB sources; USB replacement requires live-text confirmation. */
public final class BsjIpodInference {
    private long firstCd=-1,lastPlayback=-1,changerAt=-1,menuAt=-1,markerAt=-1,standbyAt=-1;
    private int cdPackets;private boolean cdFamily,changerIdle,inferred,marker,usbPlayback,markerPending,markerText,standbyTitle;
    public void reset(){firstCd=lastPlayback=changerAt=menuAt=markerAt=standbyAt=-1;cdPackets=0;cdFamily=changerIdle=inferred=marker=usbPlayback=markerPending=markerText=standbyTitle=false;}
    public boolean fromMarker(){return marker;}
    public boolean pendingMarker(){return markerPending;}
    public boolean fromStandbyTitle(){return standbyTitle;}
    private boolean confirmMarker(long at){if(!markerPending||!markerText||at-markerAt<3000)return false;markerPending=false;inferred=true;return true;}
    public boolean accept(int type,int[] p,long at,Integer source){return accept(type,p,at,source,null);}
    public boolean accept(int type,int[] p,long at,Integer source,Integer band){
        if(type==127){reset();return false;}
        if(type==9){menuAt=at;return false;}
        if(type==6&&p.length==3){
            changerAt=at;changerIdle=true;
            for(int b:p)for(int n:new int[]{(b>>>4)&15,b&15})if(n!=2&&n!=3)changerIdle=false;
            if(!inferred&&!markerPending&&!usbPlayback&&p[0]==0x32&&p[1]==0x22&&p[2]==0x22&&source!=null&&source>=0&&source<=4){
                marker=true;markerPending=Integer.valueOf(4).equals(source);markerText=standbyTitle=false;markerAt=at;inferred=!markerPending;return true;
            }
            return confirmMarker(at);
        }
        if(type==5&&p.length==8){
            int work=p[1]&15,track=(p[4]<<8)|p[5];
            if(work==9||work==10){reset();return false;}
            // Both devices send work11 while connecting. Only the later empty
            // TITLE heartbeat without playback counters identifies paused iPod.
            if(p[0]==0&&p[1]==11&&p[2]==0&&p[3]==0&&track==0&&p[6]==0&&p[7]==0)standbyAt=at;
            if(work==0&&track>0&&(p[3]==1||p[3]==2)){
                boolean cd=p[0]>0&&p[3]==1;
                if(!cd){firstCd=-1;cdPackets=0;}
                else{if(firstCd<0)firstCd=at;cdPackets++;}
                cdFamily=cd;usbPlayback=!cd&&p[0]==0&&p[3]==2;lastPlayback=at;inferred=marker=markerPending=markerText=standbyTitle=false;markerAt=standbyAt=-1;
            }
            return false;
        }
        if(type!=7)return confirmMarker(at);
        if(p.length<2||inferred)return false;
        int kind=p[0];if(kind!=1&&kind!=4&&kind!=5&&kind!=6&&kind!=7&&kind!=8)return false;
        StringBuilder b=new StringBuilder();for(int i=1;i<p.length&&p[i]!=0;i++)b.append((char)p[i]);
        String text=b.toString().trim();
        if(text.isEmpty()){
            boolean emptyTitle=kind==7&&p.length==31;for(int i=1;i<p.length;i++)if(p[i]!=0)emptyTitle=false;
            if(markerPending&&emptyTitle&&standbyAt>=0&&at-standbyAt>=3000&&at-standbyAt<=15000&&at-markerAt>=3000){markerText=standbyTitle=true;return confirmMarker(at);}
            return false;
        }
        if(text.equalsIgnoreCase("NO INFO")||text.equalsIgnoreCase("NO DATA"))return false;
        if(markerPending){markerText=true;return confirmMarker(at);}
        if(!Integer.valueOf(2).equals(source))return false;
        // Require prior live playback, a recent non-active changer packet, silence
        // of that playback family and a new text packet. A menu or dead link is insufficient.
        if(!cdFamily||cdPackets<3||lastPlayback-firstCd<500||at-lastPlayback<4000||at-lastPlayback>30000
            ||!changerIdle||at-changerAt>15000||(menuAt>=0&&at-menuAt<4000))return false;
        inferred=true;marker=false;return true;
    }
}
