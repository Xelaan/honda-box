package dev.alex.fyt.probe;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.os.SystemClock;

/** Bottom-only readout, inspired by Tesla's climate surface. It sends no commands. */
public final class ClimatePopupView extends View {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Bitmap dayCabin,nightCabin;
    private ProbeState.Snapshot state;private boolean demo;private final TemperatureIndicators temperatures;private final boolean ownsTemperatures;
    public ClimatePopupView(Context c){this(c,new TemperatureIndicators(),true);}
    public ClimatePopupView(Context c,TemperatureIndicators feedback){this(c,feedback,false);}
    private ClimatePopupView(Context c,TemperatureIndicators feedback,boolean own){super(c);temperatures=feedback;ownsTemperatures=own;BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=2;
        dayCabin=BitmapFactory.decodeResource(getResources(),R.drawable.cabin_day,o);nightCabin=BitmapFactory.decodeResource(getResources(),R.drawable.cabin_night,o);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void content(ProbeState.Snapshot s,boolean test){state=s;demo=test;if(ownsTemperatures&&!test)temperatures.observe(s,true,SystemClock.uptimeMillis());setContentDescription((test?I18n.t(R.string.ui_test_306):"")+I18n.t(R.string.ui_climate)+s.temperature(27)+" / "+s.temperature(28)+", DUAL "+s.dual()+", "+s.climate()+I18n.t(R.string.ui_fan)+s.raw(21));invalidate();}
    private String flag(int code){if(state==null||!state.connected||state.number(code)==null||((code==12||code==16||code==17||code==33||code==36||code==49)&&!state.optionalKnown(code)))return "—";Integer n=code==10?state.climatePower():state.number(code);return n==0?I18n.t(R.string.ui_off):n==1?I18n.t(R.string.ui_on):"?";}
    private boolean active(int code){return I18n.t(R.string.ui_on).equals(flag(code));}
    private void text(Canvas c,String s,float x,float y,float size,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextAlign(Paint.Align.CENTER);p.setTextSize(size);p.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));c.drawText(s,x,y,p);}
    private void label(Canvas c,String s,int code,float x,float y,ThemeSettings.Palette t){String f=flag(code);text(c,s+(f.equals("—")?" —":f.equals("?")?" ?":""),x,y,getWidth()/getResources().getDisplayMetrics().density>=700?19:17,active(code)?t.accent:t.muted);}
    @Override protected void onDraw(Canvas c){super.onDraw(c);ThemeSettings.Palette t=ThemeSettings.resolve(getContext()).palette;
        float density=getResources().getDisplayMetrics().density;c.save();c.scale(density,density);float w=getWidth()/density,h=getHeight()/density;boolean wide=w>=700;
        p.setStyle(Paint.Style.FILL);p.setColor(t.card);c.drawRect(0,0,w,h,p);p.setColor(t.border);c.drawRect(0,0,w,1,p);
        p.setColor(t.border);c.drawRoundRect(new RectF(w/2-24,7,w/2+24,10),2,2,p);
        // Cover the full panel height with a broad cabin image; all readings are drawn above it.
        Bitmap cabin=t.night?nightCabin:dayCabin;float cabinWidth=w*(wide?.70f:.80f),imageLeft=(w-cabinWidth)/2,imageRight=(w+cabinWidth)/2;
        float cabinOffset=wide?32:28;
        // Shift the cabin composition down while retaining a full-height cover crop.
        float scale=Math.max(cabinWidth/cabin.getWidth(),(h+2*cabinOffset)/cabin.getHeight());int cropWidth=Math.min(cabin.getWidth(),Math.round(cabinWidth/scale)),cropHeight=Math.min(cabin.getHeight(),Math.round(h/scale));
        int cropX=(cabin.getWidth()-cropWidth)/2,cropY=Math.max(0,(cabin.getHeight()-cropHeight)/2-Math.round(cabinOffset/scale));
        p.setAlpha(t.night?180:205);c.drawBitmap(cabin,new Rect(cropX,cropY,cropX+cropWidth,cropY+cropHeight),new RectF(imageLeft,0,imageRight,h),p);p.setAlpha(255);
        float fade=wide?70:30;
        p.setShader(new LinearGradient(imageLeft,0,imageLeft+fade,0,new int[]{t.card,t.card&0xffffff},null,Shader.TileMode.CLAMP));c.drawRect(imageLeft,0,imageLeft+fade,h,p);
        p.setShader(new LinearGradient(imageRight-fade,0,imageRight,0,new int[]{t.card&0xffffff,t.card},null,Shader.TileMode.CLAMP));c.drawRect(imageRight-fade,0,imageRight,h,p);p.setShader(null);
        float top=42,icon=wide?34:28;
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setStrokeCap(Paint.Cap.ROUND);p.setColor(active(10)?t.accent:t.muted);float power=wide?40:26;
        c.drawArc(new RectF(power-13,top-13,power+13,top+13),-45,270,false,p);c.drawLine(power,top-17,power,top-2,p);p.setStyle(Paint.Style.FILL);
        if(flag(10).equals("—"))text(c,"—",power+21,top+5,10,t.muted);
        label(c,"AUTO",13,wide?110:77,top+5,t);label(c,"A/C",11,wide?182:132,top+5,t);
        // Five complete airflow modes, matching the owner's conventional automotive reference.
        // Honda's windshield-only mode covers front defrost; no duplicate CAN17 indicator.
        int[] masks={2,6,4,5,1},glyphs={19,119,20,120,17};
        int mask=ClimateAirflow.mask(state);boolean known=mask>=0;
        float airflowWidth=Math.min(icon*1.65f,w*(wide?.065f:.115f)-12);
        for(int i=0;i<5;i++){float x=w*.5f+(i-2)*w*(wide?.065f:.115f);float glyphScale=i==3?1.06f:1f;float inkWidth=ClimateIcons.draw(getContext(),c,p,glyphs[i],x,wide?top:90,icon*glyphScale,airflowWidth*glyphScale,known&&mask==masks[i]?t.accent:t.muted);if(!known)text(c,"—",x+inkWidth/2+6,(wide?top:90)+5,10,t.muted);}
        // Unsupported recirculation and rear-defrost indicators are hidden.
        if(wide&&active(49))text(c,"AUTO",w*.5f,top+31,12,t.accent);
        String dual=state==null?"—":state.dual();text(c,"DUAL"+(dual.equals("—")?" —":dual.equals("?")?" ?":""),w-(wide?112:94),top+5,wide?19:14,dual.equals(I18n.t(R.string.ui_on))?t.accent:t.muted);
        long now=SystemClock.uptimeMillis();
        temperatures.draw(c,p,state==null?"—":state.temperature(27),0,w*.12f,h*.67f,wide?50:38,t,now);
        temperatures.draw(c,p,state==null?"—":state.temperature(28),1,w*.88f,h*.67f,wide?50:38,t,now);
        if(!demo&&temperatures.animating(now))postInvalidateOnAnimation();
        int level=state!=null&&state.connected&&state.number(21)!=null?state.number(21):-1;
        float fanY=h-25,barLeft=w*.43f,barRight=w*.60f;
        ClimateIcons.draw(getContext(),c,p,21,w*.38f,fanY,wide?30:26,level>0?t.accent:t.muted);
        int segments=state==null?0:state.fanSegments();float gap=wide?7:3,segment=(barRight-barLeft-6*gap)/7;
        for(int i=0;i<7;i++){p.setColor(i<segments?t.accent:t.border);c.drawRoundRect(new RectF(barLeft+i*(segment+gap),fanY-2,barLeft+i*(segment+gap)+segment,fanY+2),2,2,p);}
        text(c,level<0?"—":String.valueOf(level),w*.65f,fanY+6,18,t.muted);
        if(active(33))text(c,"AUTO",w*.72f,fanY+6,14,t.accent);
        if(demo)text(c,I18n.t(R.string.ui_test_vehicle_values_unchanged),w/2,wide?86:116,11,t.warning);
        c.restore();
    }
}
