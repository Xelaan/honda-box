package dev.alex.fyt.probe;

import java.util.*;

/** Offline NOAA/Meeus solar calculation. Conventional sunrise uses zenith 90.833 degrees.
 * Times are UTC instants; the timezone is used only to choose and label the local date.
 * No fixed sunrise hours, network requests, or geographic assumptions. */
public final class SolarTimes {
    private static final double RAD=Math.PI/180;
    private static final long DAY=86400000L;
    public static final class Result {
        public final boolean day; public final long sunrise,sunset,next;
        Result(boolean d,long r,long s,long n){day=d;sunrise=r;sunset=s;next=n;}
    }
    private static double[] sun(double epoch) {
        double t=(epoch/DAY+2440587.5-2451545)/36525;
        double l=(280.46646+t*(36000.76983+t*.0003032))%360;
        double m=357.52911+t*(35999.05029-.0001537*t), e=.016708634-t*(.000042037+.0000001267*t);
        double c=Math.sin(m*RAD)*(1.914602-t*(.004817+.000014*t))+Math.sin(2*m*RAD)*(.019993-.000101*t)+Math.sin(3*m*RAD)*.000289;
        double omega=125.04-1934.136*t, longitude=l+c-.00569-.00478*Math.sin(omega*RAD);
        double ob=23+(26+(21.448-t*(46.815+t*(.00059-t*.001813)))/60)/60+.00256*Math.cos(omega*RAD);
        double decl=Math.asin(Math.sin(ob*RAD)*Math.sin(longitude*RAD));
        double y=Math.pow(Math.tan(ob*RAD/2),2);
        double eq=4/RAD*(y*Math.sin(2*l*RAD)-2*e*Math.sin(m*RAD)+4*e*y*Math.sin(m*RAD)*Math.cos(2*l*RAD)-.5*y*y*Math.sin(4*l*RAD)-1.25*e*e*Math.sin(2*m*RAD));
        return new double[]{decl,eq};
    }
    private static long event(long midnight,double lat,double lon,boolean rise){
        double minutes=720-4*lon+(rise?-360:360);
        for(int i=0;i<4;i++) {
            double[] s=sun(midnight+minutes*60000);
            double cos=(Math.cos(90.833*RAD)-Math.sin(lat*RAD)*Math.sin(s[0]))/(Math.cos(lat*RAD)*Math.cos(s[0]));
            if(!finite(cos)||cos>1||cos< -1)return 0;
            double angle=Math.acos(cos)/RAD;
            minutes=720-4*lon-s[1]+(rise?-4:4)*angle;
        }
        return midnight+Math.round(minutes*60000);
    }
    private static boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
    public static boolean valid(double lat,double lon){return finite(lat)&&finite(lon)&&Math.abs(lat)<=90&&Math.abs(lon)<=180;}
    public static Result calculate(long now,double lat,double lon,TimeZone zone) {
        if(!valid(lat,lon))throw new IllegalArgumentException("Invalid coordinates");
        Calendar local=Calendar.getInstance(zone);local.setTimeInMillis(now);
        Calendar utc=Calendar.getInstance(TimeZone.getTimeZone("UTC"));utc.clear();
        utc.set(local.get(Calendar.YEAR),local.get(Calendar.MONTH),local.get(Calendar.DAY_OF_MONTH));
        long date=utc.getTimeInMillis(),latest=Long.MIN_VALUE,next=Long.MAX_VALUE,rise=0,set=0;boolean day=false;
        for(int delta=-2;delta<=2;delta++)for(boolean rising:new boolean[]{true,false}) {
            long at=event(date+delta*DAY,lat,lon,rising);if(at==0)continue;
            if(at<=now&&at>latest){latest=at;day=rising;}
            if(at>now&&at<next)next=at;
            Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(at);
            if(c.get(Calendar.YEAR)==local.get(Calendar.YEAR)&&c.get(Calendar.DAY_OF_YEAR)==local.get(Calendar.DAY_OF_YEAR)) {
                if(rising)rise=at;else set=at;
            }
        }
        if(latest==Long.MIN_VALUE) {
            // Polar days/nights, including dates adjacent to their first/last event.
            double[] s=sun(now);double utcMinutes=((now%DAY)+DAY)%DAY/60000.0;
            double hour=(utcMinutes+s[1]+4*lon)/4-180;
            double altitude=Math.asin(Math.sin(lat*RAD)*Math.sin(s[0])+Math.cos(lat*RAD)*Math.cos(s[0])*Math.cos(hour*RAD))/RAD;
            day=altitude>=-.833;
        }
        return new Result(day,rise,set,next==Long.MAX_VALUE?0:next);
    }
    private SolarTimes(){}
}
