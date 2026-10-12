package dev.alex.fyt.probe.tests;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.io.*;
final class IconPreview0818Tests {
 private Bitmap render(Drawable d){Bitmap b=Bitmap.createBitmap(144,144,Bitmap.Config.ARGB_8888);d.setBounds(0,0,144,144);d.draw(new Canvas(b));return b;}
 int run(Context c)throws Exception{
  String[] packages={"dev.alex.fyt.launcher.box","dev.alex.fyt.probe","dev.alex.fyt.launcher.radio","dev.alex.fyt.launcher.cd","dev.alex.hondabox.mediausb"};
  String[] labels={"Honda Box","Box Service","Radio","CD","USB · iPod"};Bitmap[] icons=new Bitmap[5];
  for(int i=0;i<icons.length;i++)icons[i]=render(c.getPackageManager().getApplicationIcon(packages[i]));
  for(int i=0;i<icons.length;i++)for(int j=i+1;j<icons.length;j++)if(icons[i].sameAs(icons[j]))throw new AssertionError("Distinct source/service icons: "+labels[i]+" vs "+labels[j]);
  Bitmap preview=Bitmap.createBitmap(1000,244,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(preview);canvas.drawColor(Color.rgb(244,246,250));
  Paint text=new Paint(Paint.ANTI_ALIAS_FLAG);text.setColor(Color.rgb(37,47,66));text.setTextSize(20);text.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));text.setTextAlign(Paint.Align.CENTER);
  for(int i=0;i<icons.length;i++){canvas.drawBitmap(icons[i],i*200+28,28,null);canvas.drawText(labels[i],i*200+100,210,text);}
  try(OutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"icons-current.png"))){if(!preview.compress(Bitmap.CompressFormat.PNG,100,out))throw new AssertionError("PNG export failed");}
  for(Bitmap icon:icons)icon.recycle();preview.recycle();return 11;
 }
}
