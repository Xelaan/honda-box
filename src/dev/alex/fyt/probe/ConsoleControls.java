package dev.alex.fyt.probe;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

/** Shared flat controls: native button elevation must not cast clipped shadows. */
public final class ConsoleControls {
    private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
    public static Button button(Context c,String label,ThemeSettings.Palette p,boolean selected){
        Button b=new Button(c);b.setText(label);b.setAllCaps(false);b.setTextSize(14);
        b.setTextColor(selected?p.accentText:p.text);
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));
        b.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));
        b.setStateListAnimator(null);b.setElevation(0);b.setTranslationZ(0);
        GradientDrawable shape=new GradientDrawable();shape.setColor(selected?p.accent:p.button);shape.setCornerRadius(dp(c,16));
        int ripple=(p.accent&0xffffff)|0x28000000;
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple),shape,null));return b;
    }
    public static void verticalSpacing(View root){
        if(!(root instanceof ViewGroup))return;ViewGroup group=(ViewGroup)root;
        for(int i=0;i<group.getChildCount();i++){
            View child=group.getChildAt(i);
            if(group instanceof LinearLayout&&((LinearLayout)group).getOrientation()==LinearLayout.VERTICAL&&child instanceof Button&&!(child instanceof CompoundButton)){
                LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)child.getLayoutParams();
                lp.topMargin=Math.max(lp.topMargin,dp(root.getContext(),6));lp.bottomMargin=Math.max(lp.bottomMargin,dp(root.getContext(),6));child.setLayoutParams(lp);
            }
            if(child instanceof CheckBox){
                ThemeSettings.Palette p=ThemeSettings.resolve(child.getContext()).palette;
                ((CheckBox)child).setButtonTintList(new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{p.accent,p.muted}));
            }
            verticalSpacing(child);
        }
    }
    private ConsoleControls(){}
}
