package com.crewbeat.hondabox;

import android.content.Context;
import android.graphics.*;
import java.util.HashMap;

/** Transparent assets with optical scaling based on painted area, rather than image width. */
public final class ClimateIcons {
    private static final HashMap<Integer,Glyph> cache=new HashMap<>();
    private static final class Glyph {
        final Bitmap image;final Rect bounds;final float inkArea;
        Glyph(Context c,int resource){BitmapFactory.Options o=new BitmapFactory.Options();o.inSampleSize=4;
            image=BitmapFactory.decodeResource(c.getResources(),resource,o);
            int width=image.getWidth(),height=image.getHeight(),left=width,top=height,right=-1,bottom=-1;
            int[] pixels=new int[width*height];image.getPixels(pixels,0,width,0,0,width,height);
            float ink=0;
            for(int y=0;y<height;y++)for(int x=0;x<width;x++){int alpha=pixels[y*width+x]>>>24;if(alpha>24){ink+=alpha/255f;left=Math.min(left,x);top=Math.min(top,y);right=Math.max(right,x);bottom=Math.max(bottom,y);}}
            inkArea=Math.max(1,ink);
            bounds=right<0?new Rect(0,0,width,height):new Rect(Math.max(0,left-2),Math.max(0,top-2),Math.min(width,right+3),Math.min(height,bottom+3));
        }
    }
    public static float draw(Context context,Canvas canvas,Paint paint,int code,float x,float y,float size,int color){
        return draw(context,canvas,paint,code,x,y,size,size*1.65f,color);
    }
    public static float draw(Context context,Canvas canvas,Paint paint,int code,float x,float y,float size,float maxWidth,int color){
        int res=code==119?R.drawable.climate_v2_face_feet:code==120?R.drawable.climate_v2_feet_windshield:code==12?R.drawable.climate_v2_recirculation:code==16?R.drawable.climate_rear:code==17?R.drawable.climate_v2_windshield:code==18?R.drawable.climate_air_windshield:code==19?R.drawable.climate_v2_face:code==20?R.drawable.climate_v2_feet:R.drawable.climate_v3_fan;
        Glyph glyph; synchronized(cache){glyph=cache.get(res);if(glyph==null){glyph=new Glyph(context,res);cache.put(res,glyph);}}
        // Wide car/window symbols need more width to match the visual weight of a seated person.
        float ratio=code==21?Math.min(size/glyph.bounds.width(),size/glyph.bounds.height()):(float)Math.sqrt(size*size*.32f/glyph.inkArea);
        ratio=Math.min(ratio,Math.min(maxWidth/glyph.bounds.width(),size*(code==21?1f:.92f)/glyph.bounds.height()));
        float w=glyph.bounds.width()*ratio,h=glyph.bounds.height()*ratio;
        paint.setStyle(Paint.Style.FILL);paint.setAlpha(255);paint.setColorFilter(new PorterDuffColorFilter(color,PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(glyph.image,glyph.bounds,new RectF(x-w/2,y-h/2,x+w/2,y+h/2),paint);paint.setColorFilter(null);return w;
    }
    private ClimateIcons(){}
}
