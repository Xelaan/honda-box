package dev.alex.fyt.probe;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;

/** A scrollable list of reconstructed choices with the received OEM item highlighted. Read-only. */
public final class CarMenuPopupView extends LinearLayout {
    private final TextView heading,footer;private final Button close;private final ScrollView scroll;private final LinearLayout list;
    private String lastKey="";
    public CarMenuPopupView(Context context,Runnable dismiss){this(context,dismiss,()->{});}
    public CarMenuPopupView(Context context,Runnable dismiss,Runnable interaction){
        super(context);setOrientation(VERTICAL);setPadding(dp(18),dp(8),dp(12),dp(10));setElevation(dp(10));
        LinearLayout header=new LinearLayout(context);header.setGravity(Gravity.CENTER_VERTICAL);
        heading=new TextView(context);heading.setTextSize(13);heading.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));heading.setLetterSpacing(.06f);heading.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(heading,new LinearLayout.LayoutParams(0,dp(40),1));close=new Button(context);close.setText("×");close.setTextSize(25);close.setPadding(0,0,0,0);close.setMinWidth(0);close.setMinimumWidth(0);close.setContentDescription(I18n.t(R.string.ui_hide_car_menu));close.setOnClickListener(v->dismiss.run());header.addView(close,new LinearLayout.LayoutParams(dp(40),dp(40)));addView(header);
        scroll=new ScrollView(context);scroll.setFillViewport(false);scroll.setClipToPadding(false);scroll.setOnTouchListener((v,e)->{interaction.run();return false;});list=new LinearLayout(context);list.setOrientation(VERTICAL);scroll.addView(list);addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        footer=new TextView(context);footer.setTextSize(10);footer.setSingleLine(true);footer.setEllipsize(TextUtils.TruncateAt.END);footer.setPadding(0,dp(6),0,0);addView(footer);setClickable(true);
    }
    private int dp(float value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private GradientDrawable background(int color,int stroke,int radius){GradientDrawable b=new GradientDrawable();b.setColor(color);b.setCornerRadius(dp(radius));if(stroke!=0)b.setStroke(dp(1),stroke);return b;}
    public void content(ProbeState.Snapshot s,boolean test){ThemeSettings.Palette t=ThemeSettings.resolve(getContext()).palette;MenuListModel model=MenuListModel.from(s);
        setBackground(background(t.card,t.border,22));heading.setText((test?"TEST · ":"")+model.heading(s));heading.setTextColor(test?t.warning:t.accent);footer.setText(I18n.t(R.string.ui_use_the_vehicle_controls));footer.setTextColor(t.muted);close.setContentDescription(I18n.t(R.string.ui_hide_car_menu));close.setBackground(background(t.button,0,20));close.setTextColor(t.muted);
        String key=model.heading(s)+model.items.toString()+model.selected+t.card+I18n.revision();
        if(!key.equals(lastKey)){lastKey=key;list.removeAllViews();for(int i=0;i<model.items.size();i++){boolean selected=i==model.selected;TextView row=new TextView(getContext());row.setText(model.items.get(i));row.setTextSize(17);row.setTypeface(Typeface.create(selected?"sans-serif-medium":"sans-serif",0));row.setTextColor(selected?(t.night?t.bg:Color.WHITE):t.text);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),0,dp(8),0);row.setSingleLine(true);row.setEllipsize(TextUtils.TruncateAt.END);row.setSelected(selected);if(selected)row.setBackground(background(t.accent,0,10));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(36));lp.bottomMargin=dp(3);list.addView(row,lp);}if(model.selected>=0)scroll.post(()->scroll.smoothScrollTo(0,dp(Math.max(0,model.selected-1)*39)));}
        setContentDescription((test?"TEST · ":"")+I18n.t(R.string.ui_car_menu)+" · "+model.heading(s));
    }
}
