package com.crewbeat.hondabox;

/** Known audio operations only; diagnostic options are frozen for each manual action. */
public final class AudioCommandOptions {
    public enum Transport { PROFILE, RAW_1008 }
    public enum Route { CURRENT, OEM, DELAY_700, CONFIRMED_13, CAN_ONLY }
    public static final int[] HOLDS={60,180,400,800};
    public static final AudioCommandOptions STANDARD=new AudioCommandOptions(Transport.PROFILE,Route.CURRENT,180,false);
    public final Transport transport;public final Route route;public final int holdMs;public final boolean debug;
    public AudioCommandOptions(Transport transport,Route route,int holdMs,boolean debug){
        boolean valid=false;for(int h:HOLDS)if(h==holdMs)valid=true;
        if(transport==null||route==null||!valid)throw new IllegalArgumentException("Unsupported audio test options");
        this.transport=transport;this.route=route;this.holdMs=holdMs;this.debug=debug;
    }
    public static AudioCommandOptions debug(String transport,String route,int hold){
        return new AudioCommandOptions(Transport.valueOf(transport),Route.valueOf(route),hold,true);
    }
    public boolean setsMain(AudioCommand action){return route!=Route.CAN_ONLY&&(route!=Route.CURRENT||action.selectsSource());}
    public boolean waitsForMain(AudioCommand action){return setsMain(action)&&(route==Route.CURRENT||route==Route.CONFIRMED_13);}
    public int delayMs(AudioCommand action){return !setsMain(action)?0:route==Route.DELAY_700?700:waitsForMain(action)?350:0;}
    public int command(AudioCommand action){return transport==Transport.RAW_1008?1008:action.selectsSource()?1:0;}
    public int[] args(AudioCommand action,boolean pressed){
        if(transport==Transport.RAW_1008)return action.selectsSource()?new int[]{0x83,1,action.mode}:new int[]{0x82,2,action.key,pressed?1:0};
        return action.selectsSource()?new int[]{action.mode}:new int[]{action.key,pressed?1:0};
    }
    public int transportLabel(){return transport==Transport.PROFILE?R.string.debug_audio_transport_profile:R.string.debug_audio_transport_raw;}
    public int routeLabel(){switch(route){case OEM:return R.string.debug_audio_route_oem;case DELAY_700:return R.string.debug_audio_route_delay;case CONFIRMED_13:return R.string.debug_audio_route_confirmed;case CAN_ONLY:return R.string.debug_audio_route_can;default:return R.string.debug_audio_route_current;}}
    public String description(){return I18n.t(transportLabel())+" · "+I18n.t(routeLabel())+" · "+holdMs+" ms";}
}
