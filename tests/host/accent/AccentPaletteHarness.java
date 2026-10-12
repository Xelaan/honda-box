package dev.alex.fyt.probe;
public final class AccentPaletteHarness {
 public static void main(String[] args){int checks=0;
  if(AccentColors.tone(AccentColors.DEFAULT,false)!=0xFF426DC3||AccentColors.tone(AccentColors.DEFAULT,true)!=0xFF8FAFF7)throw new AssertionError("Original palette changed");
  for(int base:AccentColors.PRESETS)for(boolean night:new boolean[]{false,true}){int tone=AccentColors.tone(base,night);if(AccentColors.contrast(tone,night?0xFF191E26:0xFFFFFFFF)<4.5)throw new AssertionError("Accent contrast");if(AccentColors.contrast(tone,AccentColors.foreground(tone))<4.5)throw new AssertionError("Label contrast");checks+=2;System.out.printf("%06X %s -> %06X%n",base&0xFFFFFF,night?"night":"day",tone&0xFFFFFF);}
  for(int r=0;r<=255;r+=17)for(int g=0;g<=255;g+=17)for(int b=0;b<=255;b+=17)for(boolean night:new boolean[]{false,true}){int tone=AccentColors.tone(0xFF000000|(r<<16)|(g<<8)|b,night);if(AccentColors.contrast(tone,night?0xFF191E26:0xFFFFFFFF)<4.5||AccentColors.contrast(tone,AccentColors.foreground(tone))<4.5)throw new AssertionError("Custom contrast");checks+=2;}
  System.out.println(checks+" checks PASS");
 }
}
