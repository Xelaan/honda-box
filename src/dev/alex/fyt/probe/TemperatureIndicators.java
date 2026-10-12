package dev.alex.fyt.probe;

import android.graphics.*;

/** Decorative feedback for reported temperatures; never sends climate commands. */
public final class TemperatureIndicators {
    private static final long DURATION=900;
    private final Float[] previous=new Float[2];
    private final long[] changedAt=new long[2];
    private final int[] direction=new int[2];
    private long session=Long.MIN_VALUE;private Integer profile;
    private Float value(ProbeState.Snapshot s,int code){
        Integer n=s.number(code);
        if(!s.connected||!s.honda()||n==null||n==0||n==-1)return null;
        if(n==-2)return Float.NEGATIVE_INFINITY;if(n==-3)return Float.POSITIVE_INFINITY;
        return n>0?(n==20?18f:n/2f):null;
    }
    public void observe(ProbeState.Snapshot s,boolean animate,long now){
        Integer nextProfile=s.number(1000);
        boolean baseline=session!=s.sessionId||!java.util.Objects.equals(profile,nextProfile);
        session=s.sessionId;profile=nextProfile;
        for(int zone=0;zone<2;zone++){
            Float next=value(s,27+zone),old=previous[zone];
            if(baseline||!animate||next==null||old==null){direction[zone]=0;changedAt[zone]=0;}
            else if(Float.compare(next,old)!=0){direction[zone]=Float.compare(next,old);changedAt[zone]=now;}
            previous[zone]=next;
        }
    }
    private float strength(int zone,long now){
        long age=now-changedAt[zone];if(direction[zone]==0||age<0||age>=DURATION)return 0;
        return age<80?age/80f:(float)Math.pow(1f-(age-80f)/(DURATION-80f),2);
    }
    public boolean animating(long now){return (direction[0]!=0&&now>=changedAt[0]&&now-changedAt[0]<DURATION)||(direction[1]!=0&&now>=changedAt[1]&&now-changedAt[1]<DURATION);}
    private int mix(int a,int b,float amount){
        return Color.rgb(Math.round(Color.red(a)+(Color.red(b)-Color.red(a))*amount),
            Math.round(Color.green(a)+(Color.green(b)-Color.green(a))*amount),
            Math.round(Color.blue(a)+(Color.blue(b)-Color.blue(a))*amount));
    }
    private void chevron(Canvas c,Paint p,float x,float y,float halfWidth,float bend,boolean up,int color,float pulse,boolean known,boolean night){
        int rest=known?(night?95:65):(night?45:30);
        p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeWidth(1.5f+.5f*pulse);p.setColor(color);p.setAlpha(Math.round(rest+(210-rest)*pulse));
        Path path=new Path();float sign=up?-1:1;
        path.moveTo(x-halfWidth,y);path.lineTo(x-4,y+sign*bend);
        path.quadTo(x,y+sign*(bend+2),x+4,y+sign*bend);path.lineTo(x+halfWidth,y);c.drawPath(path,p);
        p.setAlpha(255);p.setStyle(Paint.Style.FILL);
    }
    public void draw(Canvas c,Paint p,String text,int zone,float x,float baseline,float size,ThemeSettings.Palette t,long now){
        float pulse=strength(zone,now);int warm=Color.parseColor(t.night?"#E99AA5":"#D46E81"),cool=Color.parseColor(t.night?"#91B5F0":"#5A80C8");
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        p.setColor(mix(t.text,direction[zone]>0?warm:cool,pulse*.2f));c.drawText(text,x,baseline,p);
        Paint.FontMetrics metrics=p.getFontMetrics();float width=size>=45?18:15,bend=5;
        chevron(c,p,x,baseline+metrics.ascent-15,width,bend,true,warm,direction[zone]>0?pulse:0,!text.equals("—")&&!text.startsWith("raw"),t.night);
        chevron(c,p,x,baseline+metrics.descent+16,width,bend,false,cool,direction[zone]<0?pulse:0,!text.equals("—")&&!text.startsWith("raw"),t.night);
    }
}
