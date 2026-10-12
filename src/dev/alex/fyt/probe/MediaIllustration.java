package dev.alex.fyt.probe;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.io.InputStream;
/** Shared, once-decoded decorative assets for the home screen and players. */
final class MediaIllustration extends View {
 private static Bitmap atlas,ipod,darkAtlas,darkIpod;
 private int index=-1;private final boolean anchored;private boolean dark;private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 MediaIllustration(Context c,int index,boolean anchored){super(c);this.anchored=anchored;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);source(index);}
 void source(int value){if(index==value)return;index=value;if(index==3){if(ipod==null)ipod=load(getContext(),"home/dudu-ipod-v1.png");}else if(atlas==null)atlas=load(getContext(),"home/dudu-media-sprites-v2.png");night(dark);}
 private static Bitmap load(Context c,String path){try(InputStream stream=c.getAssets().open(path)){BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;return BitmapFactory.decodeStream(stream,null,options);}catch(java.io.IOException ignored){return null;}}
 void night(boolean value){dark=value;if(value){if(index==3){if(darkIpod==null)darkIpod=load(getContext(),"home/dudu-ipod-dark-v1.png");}else if(darkAtlas==null)darkAtlas=load(getContext(),"home/dudu-media-sprites-dark-v1.png");}invalidate();}
 private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override protected void onDraw(Canvas canvas){Bitmap image=index==3?(dark&&darkIpod!=null?darkIpod:ipod):(dark&&darkAtlas!=null?darkAtlas:atlas);if(image==null)return;int cell=index==3?image.getWidth():image.getWidth()/3;Rect source=new Rect(index==3?0:index*cell,0,index==3?image.getWidth():(index+1)*cell,image.getHeight());float width,height,left,top;
  if(anchored){width=height=Math.min(getWidth()*.62f,getHeight()*.95f);left=getWidth()-width-dp(6);top=getHeight()-height-dp(2);}else{float ratio=source.width()/(float)source.height();width=Math.min(getWidth(),getHeight()*ratio);height=width/ratio;left=(getWidth()-width)/2;top=(getHeight()-height)/2;}
  paint.setAlpha(dark?235:145);canvas.drawBitmap(image,source,new RectF(left,top,left+width,top+height),paint);
 }
}
