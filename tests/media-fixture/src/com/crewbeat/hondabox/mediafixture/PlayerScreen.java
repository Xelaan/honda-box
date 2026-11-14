package com.crewbeat.hondabox.mediafixture;
public final class PlayerScreen extends android.app.Activity {
 @Override public void onCreate(android.os.Bundle b){super.onCreate(b);android.widget.TextView t=new android.widget.TextView(this);t.setText("TEST Android Player · opened from track overlay");t.setTextSize(28);setContentView(t);}
}
