package com.crewbeat.hondabox;

public enum AudioMode {
    RADIO("Radio",1,R.drawable.ic_radio,AudioCommand.RADIO),CD("CD",2,R.drawable.ic_cd,AudioCommand.CD),
    USB("USB",4,R.drawable.ic_usb,AudioCommand.USB),IPOD("iPod",-1,R.drawable.ic_ipod,null);
    public final String title;public final int source,icon;public final AudioCommand select;
    AudioMode(String name,int code,int drawable,AudioCommand command){title=name;source=code;icon=drawable;select=command;}
    public static AudioMode reported(ProbeState.Snapshot s){if(!s.connected||!s.accord())return null;
        Integer n=s.audioSource();if((n==null||n==0)&&s.mediaDevice!=ProbeState.MediaDevice.UNKNOWN)return USB;if(n==null)return null;return n==1?RADIO:n==2?(s.mediaDevice!=ProbeState.MediaDevice.UNKNOWN?USB:CD):(n==3||n==4)&&n!=s.auxSource?USB:null;
    }
    public static boolean aux(ProbeState.Snapshot s){return s.connected&&s.accord()&&s.auxSource>=3&&Integer.valueOf(s.auxSource).equals(s.audioSource());}
}
