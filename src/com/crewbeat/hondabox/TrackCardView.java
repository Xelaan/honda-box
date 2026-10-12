package com.crewbeat.hondabox;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.text.*;
import android.view.View;

/** Compact read-only media notification, using the shared day/night palette. */
public final class TrackCardView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String title,label;private final Drawable icon;
    private final long deadline;private StaticLayout lines;private int textWidth;
    private ThemeSettings.Palette palette;private Shader tileShader,iconShader;private final PorterDuffXfermode iconBlend=new PorterDuffXfermode(PorterDuff.Mode.SRC_IN);
    public TrackCardView(Context context,String title,String label,int resource,long deadline){super(context);this.title=title.replace('\n',' ').replaceAll("\\s+"," ").trim();this.label=label;this.deadline=deadline;icon=context.getDrawable(resource).mutate();setLayerType(LAYER_TYPE_SOFTWARE,null);setContentDescription(label+": "+this.title);}
    private static int mix(int base,int tint,float amount){return Color.rgb(Math.round(Color.red(base)*(1-amount)+Color.red(tint)*amount),Math.round(Color.green(base)*(1-amount)+Color.green(tint)*amount),Math.round(Color.blue(base)*(1-amount)+Color.blue(tint)*amount));}
    private float dp(float n){return n*getResources().getDisplayMetrics().density;}
    @Override protected void onDraw(Canvas c){super.onDraw(c);ThemeSettings.Palette next=ThemeSettings.resolve(getContext()).palette;
        if(palette!=next){palette=next;lines=null;
            tileShader=new LinearGradient(0,dp(16),0,dp(68),palette.card,mix(palette.card,palette.accent,palette.night?.06f:.035f),Shader.TileMode.CLAMP);
            iconShader=new LinearGradient(dp(26),dp(18),dp(74),dp(66),mix(palette.accent,Color.WHITE,palette.night?.12f:.46f),mix(palette.accent,palette.night?Color.BLACK:Color.WHITE,palette.night?.08f:.12f),Shader.TileMode.CLAMP);
        }float w=getWidth(),h=getHeight();RectF card=new RectF(dp(8),dp(6),w-dp(8),h-dp(10));
        paint.setStyle(Paint.Style.FILL);paint.setColor(palette.card);paint.setShadowLayer(dp(6),0,dp(2),palette.night?0x50000000:0x200F203A);c.drawRoundRect(card,dp(20),dp(20),paint);paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1));paint.setColor(palette.border);c.drawRoundRect(card,dp(20),dp(20),paint);paint.setStyle(Paint.Style.FILL);
        RectF tile=new RectF(dp(24),dp(16),dp(76),dp(68));paint.setShader(tileShader);c.drawRoundRect(tile,dp(14),dp(14),paint);paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.2f));paint.setColor(palette.border);c.drawRoundRect(tile,dp(14),dp(14),paint);paint.setStyle(Paint.Style.FILL);
        int iconLayer=c.saveLayer(tile,null);icon.setTint(Color.WHITE);icon.setBounds((int)dp(26),(int)dp(18),(int)dp(74),(int)dp(66));icon.draw(c);
        paint.setColor(Color.WHITE);paint.setShader(iconShader);paint.setXfermode(iconBlend);c.drawRect(tile,paint);paint.setXfermode(null);paint.setShader(null);c.restoreToCount(iconLayer);
        paint.setColor(palette.accent);paint.setTypeface(Typeface.create("sans-serif-medium",0));paint.setTextSize(dp(12));String caption=label;while(caption.length()>0&&paint.measureText(caption)>w-dp(124))caption=caption.substring(0,caption.length()-1);c.drawText(caption,dp(92),dp(29),paint);
        int width=Math.max(1,(int)(w-dp(122)));if(lines==null||width!=textWidth){textWidth=width;titlePaint.setColor(palette.text);titlePaint.setTextSize(dp(20));titlePaint.setTypeface(Typeface.create("sans-serif-medium",0));lines=StaticLayout.Builder.obtain(title,0,title.length(),titlePaint,width).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).setIncludePad(false).setLineSpacing(dp(2),1).build();}
        c.save();c.translate(dp(92),dp(36));lines.draw(c);c.restore();
        float left=dp(92),right=w-dp(30),y=h-dp(19);paint.setColor(palette.button);c.drawRoundRect(new RectF(left,y,right,y+dp(2)),dp(1),dp(1),paint);float remaining=Math.max(0,Math.min(1,(deadline-SystemClock.uptimeMillis())/4000f));paint.setColor(palette.accent);c.drawRoundRect(new RectF(left,y,left+(right-left)*remaining,y+dp(2)),dp(1),dp(1),paint);
        if(remaining>0)postInvalidateOnAnimation();
    }
}
