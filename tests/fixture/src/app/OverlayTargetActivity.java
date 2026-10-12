package app;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
/** Separate app beneath the overlay; emulator only. */
public class OverlayTargetActivity extends Activity {
    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(10,17,24));getWindow().setNavigationBarColor(Color.rgb(10,17,24));
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.rgb(15,23,31));
        TextView label=new TextView(this);label.setText("ДРУГОЕ ПРИЛОЖЕНИЕ\n\nПроверка фонового индикатора FYT Probe\n\nДанные на этом экране тестовые");label.setTextColor(Color.rgb(196,216,227));label.setTextSize(22);label.setGravity(Gravity.CENTER);
        root.addView(label,new FrameLayout.LayoutParams(-1,-1));Button target=new Button(this);target.setText("Касание проходит сквозь индикатор");
        int d=Math.round(getResources().getDisplayMetrics().density);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(360*d,88*d,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);p.bottomMargin=24*d;
        target.setOnClickListener(v->{int count=getSharedPreferences("touches",0).getInt("count",0)+1;getSharedPreferences("touches",0).edit().putInt("count",count).apply();});root.addView(target,p);setContentView(root);
    }
}
