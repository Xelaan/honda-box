import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Packages existing generated artwork into padded Android splash resources. */
public class PrepareSplashIcons {
 public static void main(String[] args)throws Exception {
  Path root=Paths.get(args[0]);
  for(boolean dark:new boolean[]{false,true}){
   BufferedImage atlas=ImageIO.read(root.resolve("assets/home/dudu-media-sprites-"+(dark?"dark-v1":"v2")+".png").toFile());
   for(int i=0;i<4;i++){
    String name=new String[]{"radio","cd","usb","ipod"}[i];
    BufferedImage source=i==3?ImageIO.read(root.resolve("assets/home/dudu-ipod-"+(dark?"dark-v1":"v1")+".png").toFile()):atlas.getSubimage(i*(atlas.getWidth()/3),0,atlas.getWidth()/3,atlas.getHeight());
    BufferedImage icon=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);Graphics2D g=icon.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    float scale=340f/Math.max(source.getWidth(),source.getHeight());int w=Math.round(source.getWidth()*scale),h=Math.round(source.getHeight()*scale);g.drawImage(source,(512-w)/2,(512-h)/2,w,h,null);g.dispose();
    Path folder=root.resolve("res/drawable"+(dark?"-night":"")+"-nodpi");Files.createDirectories(folder);ImageIO.write(icon,"png",folder.resolve("splash_"+name+".png").toFile());
    if(i<3){Path dest=root.resolve("launchers/"+new String[]{"Radio","CD","USB"}[i]+"/res/drawable"+(dark?"-night":"")+"-nodpi");Files.createDirectories(dest);Files.copy(folder.resolve("splash_"+name+".png"),dest.resolve("splash_icon.png"),StandardCopyOption.REPLACE_EXISTING);}
   }
  }
 }
}
