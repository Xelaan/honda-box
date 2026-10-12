package com.crewbeat.hondabox;
/** Opaque accent colours; theme tones keep accents readable against the card. */
public final class AccentColors {
 public static final int DEFAULT=0xFF426DC3;
 // Blue retains the original Honda Box pair. Other presets have deliberately
 // softer night tones rather than mechanically whitening the daytime colour.
 public static final int[] PRESETS={DEFAULT,0xFF0070E0,0xFFD63C49,0xFF007F86,0xFF237A57,0xFFA96512,0xFF6656C8};
 private static final int[] NIGHT_TONES={0xFF8FAFF7,0xFF70B7FF,0xFFFF7782,0xFF64D8DE,0xFF72D7AC,0xFFF3BB65,0xFFB5A6FF};
 private static double channel(int n){double v=n/255.0;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
 public static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
 public static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
 public static int foreground(int colour){return contrast(colour,0xFF000000)>=contrast(colour,0xFFFFFFFF)?0xFF000000:0xFFFFFFFF;}
 public static int tone(int base,boolean night){base|=0xFF000000;if(base==DEFAULT)return night?NIGHT_TONES[0]:DEFAULT;if(night)for(int i=1;i<PRESETS.length;i++)if(base==PRESETS[i]){base=NIGHT_TONES[i];break;}int card=night?0xFF191E26:0xFFFFFFFF;if(contrast(base,card)>=4.5)return base;int target=night?255:0;for(int step=1;step<=100;step++){double t=step/100.0;int r=(int)Math.round(((base>>16)&255)*(1-t)+target*t),g=(int)Math.round(((base>>8)&255)*(1-t)+target*t),b=(int)Math.round((base&255)*(1-t)+target*t);int c=0xFF000000|(r<<16)|(g<<8)|b;if(contrast(c,card)>=4.5)return c;}return night?0xFFFFFFFF:0xFF000000;}
 private AccentColors(){}
}
