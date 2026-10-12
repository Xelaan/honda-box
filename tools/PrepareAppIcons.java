import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Packages generated foregrounds with consistent Android adaptive-icon margins. */
public class PrepareAppIcons {
 public static void main(String[] args)throws Exception {
  Path root=Paths.get(args[0]);BufferedImage atlas=ImageIO.read(root.resolve("assets/home/honda-app-family-v3.png").toFile());
  for(int i=0;i<5;i++){
   BufferedImage source=atlas.getSubimage((i%3)*(atlas.getWidth()/3),(i/3)*(atlas.getHeight()/2),atlas.getWidth()/3,atlas.getHeight()/2);BufferedImage icon=new BufferedImage(512,512,BufferedImage.TYPE_INT_ARGB);Graphics2D g=icon.createGraphics();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);g.drawImage(source,116,116,280,280,null);g.dispose();
   Path folder=root.resolve("res/drawable-nodpi");Files.createDirectories(folder);Path file=folder.resolve(new String[]{"box_ui_foreground.png","box_service_foreground.png","app_radio_foreground.png","app_cd_foreground.png","app_usb_foreground.png"}[i]);ImageIO.write(icon,"png",file.toFile());
   if(i>=2){Path target=root.resolve("launchers/"+new String[]{"Radio","CD","USB"}[i-2]+"/res/drawable-nodpi");Files.createDirectories(target);Files.copy(file,target.resolve("media_foreground.png"),StandardCopyOption.REPLACE_EXISTING);}
   if(i==0){Path target=root.resolve("launchers/Box/res/drawable-nodpi");Files.createDirectories(target);Files.copy(file,target.resolve("box_ui_foreground.png"),StandardCopyOption.REPLACE_EXISTING);}
  }
 }
}
