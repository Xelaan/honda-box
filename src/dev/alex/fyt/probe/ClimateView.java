package dev.alex.fyt.probe;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** DUDU-inspired climate display. Read-only until climate commands are verified. */
public final class ClimateView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final ThemeSettings.Palette palette;private final Bitmap cabin;
    private ProbeState.Snapshot state;
    public ClimateView(Context c,ThemeSettings.Palette p){super(c);palette=p;
        BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
        cabin=BitmapFactory.decodeResource(getResources(),p.night?R.drawable.cabin_night:R.drawable.cabin_day,options);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
    }
    public void update(ProbeState.Snapshot s){state=s;setContentDescription(I18n.t(R.string.ui_climate_driver)+s.temperature(27)+I18n.t(R.string.ui_passenger)+s.temperature(28)+I18n.t(R.string.ui_fan)+s.raw(21)+", "+s.climate()+I18n.t(R.string.ui_display_only_use_the_vehicle));invalidate();}
    private boolean active(int code){return state!=null&&state.connected&&state.honda()&&Integer.valueOf(1).equals(code==10?state.climatePower():state.number(code));}
    private String value(int code){if(state==null||!state.connected||!state.honda()||state.number(code)==null)return "—";Integer n=code==10?state.climatePower():state.number(code);return n==0?I18n.t(R.string.ui_off):n==1?I18n.t(R.string.ui_on):"?";}
    private void text(Canvas c,String s,float x,float y,float size,int color){paint.setStyle(Paint.Style.FILL);paint.setColor(color);paint.setTextSize(size);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);c.drawText(s,x,y,paint);}
    private void chip(Canvas c,String label,int code,float x,float y,float width){
        boolean on=active(code);paint.setStyle(Paint.Style.FILL);paint.setColor(on?palette.accent:palette.button);
        c.drawRoundRect(new RectF(x-width/2,y-16,x+width/2,y+16),7,7,paint);
        text(c,label+(value(code).equals("—")?" —":""),x,y+5,13,on?palette.accentText:palette.text);
    }
    private void airflow(Canvas c,int code,float x,float y,float width){
        boolean on=active(code);paint.setStyle(Paint.Style.FILL);paint.setColor(on?palette.accent:palette.button);
        c.drawRoundRect(new RectF(x-width/2,y-16,x+width/2,y+16),7,7,paint);
        ClimateIcons.draw(getContext(),c,paint,code,x,y,23,on?palette.accentText:palette.text);
        String reading=value(code);if(reading.equals("—")||reading.equals("?"))text(c,reading,x+width/2-7,y+4,10,palette.muted);
    }
    private void fan(Canvas c,float x,float y){ClimateIcons.draw(getContext(),c,paint,21,x,y,18,palette.muted);}
    @Override protected void onDraw(Canvas actual){super.onDraw(actual);float scale=getResources().getDisplayMetrics().density;actual.save();actual.scale(scale,scale);
        float w=getWidth()/scale,h=getHeight()/scale;
        paint.setColor(palette.card);paint.setStyle(Paint.Style.FILL);actual.drawRoundRect(new RectF(0,0,w,h),18,18,paint);
        actual.save();Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),18,18,Path.Direction.CW);actual.clipPath(clip);
        if(cabin!=null){paint.setAlpha(palette.night?155:185);float imageWidth=Math.min(w*.78f,(h-30)*3);actual.drawBitmap(cabin,null,new RectF((w-imageWidth)/2,24,(w+imageWidth)/2,h-8),paint);paint.setAlpha(255);}
        // Fade the illustration under small labels, retaining legible text in both palettes.
        paint.setShader(new LinearGradient(0,h-80,0,h,new int[]{palette.card&0x00ffffff,palette.card},null,Shader.TileMode.CLAMP));actual.drawRect(0,h-80,w,h,paint);paint.setShader(null);
        float barLeft=w*.32f,barRight=w*.68f,top=30;
        chip(actual,"",10,w*.24f,top,58);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f);paint.setColor(active(10)?palette.accentText:palette.text);float power=w*.24f;
        actual.drawArc(new RectF(power-7,top-7,power+7,top+7),-45,270,false,paint);actual.drawLine(power,top-9,power,top-1,paint);paint.setStyle(Paint.Style.FILL);
        chip(actual,"A/C",11,w*.76f,top,58);
        int level=state!=null&&state.connected&&state.honda()&&state.number(21)!=null?state.number(21):-1;
        // Hardware recording confirms levels0..7; one segment per speed.
        int segments=state==null?0:state.fanSegments();
        paint.setColor(palette.button);actual.drawRoundRect(new RectF(barLeft-16,top-12,barRight+16,top+12),6,6,paint);
        float gap=5,segment=(barRight-barLeft-6*gap)/7;
        for(int i=0;i<7;i++){paint.setColor(i<segments?palette.accent:palette.border);actual.drawRect(barLeft+i*(segment+gap),top-2,barLeft+i*(segment+gap)+segment,top+2,paint);}
        fan(actual,w/2-9,top+28);text(actual,level<0?"—":String.valueOf(level),w/2+10,top+32,11,palette.muted);
        text(actual,state==null?"—":state.temperature(27),w*.115f,h*.56f,Math.min(38,w*.09f),palette.text);
        text(actual,state==null?"—":state.temperature(28),w*.885f,h*.56f,Math.min(38,w*.09f),palette.text);

        float bottom=h-39;
        airflow(actual,18,w*.16f,bottom,Math.min(80,w*.19f));airflow(actual,19,w*.37f,bottom,Math.min(74,w*.18f));
        chip(actual,"AUTO",13,w*.58f,bottom,Math.min(74,w*.18f));airflow(actual,20,w*.8f,bottom,Math.min(74,w*.18f));
        text(actual,I18n.t(R.string.ui_readings_use_the_vehicle_controls),w/2,h-9,10,palette.muted);
        actual.restore();actual.restore();
    }
}
