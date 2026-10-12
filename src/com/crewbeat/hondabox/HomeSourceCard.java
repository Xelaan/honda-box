package com.crewbeat.hondabox;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.io.InputStream;

/** DUDU-like source tile; labels remain native and the illustration is decorative. */
final class HomeSourceCard extends FrameLayout {
    private final TextView title,value,selectedLabel;
    private final MediaIllustration art;
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    HomeSourceCard(Context context,int index,String name){super(context);
        art=new MediaIllustration(context,index,true);addView(art,new LayoutParams(-1,-1));
        title=label(name,27);title.setTypeface(Typeface.create("sans-serif",0));LayoutParams t=new LayoutParams(-2,-2,Gravity.TOP|Gravity.START);t.leftMargin=dp(24);t.topMargin=dp(24);addView(title,t);
        value=label("",23);value.setMaxWidth(dp(190));value.setSingleLine(true);value.setEllipsize(android.text.TextUtils.TruncateAt.END);LayoutParams v=new LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);v.leftMargin=dp(24);v.bottomMargin=dp(24);addView(value,v);
        selectedLabel=label("",12);selectedLabel.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));LayoutParams active=new LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);active.leftMargin=dp(24);active.bottomMargin=dp(60);addView(selectedLabel,active);
        setClickable(true);setFocusable(true);setOnTouchListener((view,event)->{int action=event.getActionMasked();if(action==MotionEvent.ACTION_DOWN)view.animate().scaleX(.985f).scaleY(.985f).setDuration(120).start();else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)view.animate().scaleX(1).scaleY(1).setDuration(180).start();return false;});
    }
    private TextView label(String text,int size){TextView v=new TextView(getContext());v.setText(text);v.setTextSize(size);v.setFontFeatureSettings("tnum");return v;}
    void illustration(int index){art.source(index);}
    void bind(ThemeSettings.Palette p,boolean selected,String info){setSelected(selected);title.setTextColor(p.text);value.setTextColor(p.text);value.setText(info);selectedLabel.setText(I18n.t(R.string.home_active));selectedLabel.setTextColor(p.accent);selectedLabel.setVisibility(selected?VISIBLE:INVISIBLE);art.night(p.night);
        GradientDrawable background=new GradientDrawable();background.setCornerRadius(dp(12));background.setColor(selected?blend(p.card,p.accent,p.night?.10f:.085f):p.card);if(selected)background.setStroke(dp(2),blend(p.card,p.accent,p.night?.22f:.24f));setBackground(new RippleDrawable(ColorStateList.valueOf((p.accent&0xFFFFFF)|0x20000000),background,null));
    }
    private int blend(int a,int b,float n){return Color.rgb(Math.round(Color.red(a)*(1-n)+Color.red(b)*n),Math.round(Color.green(a)*(1-n)+Color.green(b)*n),Math.round(Color.blue(a)*(1-n)+Color.blue(b)*n));}
}
