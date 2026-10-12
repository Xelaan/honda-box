package dev.alex.fyt.probe;
import android.animation.ArgbEvaluator;import android.content.Context;import android.graphics.*;import android.view.View;
/** Six items in a single compact climate row. */
public final class ClimateStripView extends View {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private ProbeState.Snapshot state;private float fold;private boolean collapsed,sideButtonsHidden;private int expandedWidthPx;
 public ClimateStripView(Context c){super(c);}
 public void expandedWidth(int pixels){expandedWidthPx=pixels;}
 public void content(ProbeState.Snapshot s){state=s;accessibility();invalidate();}
 public void fold(float progress,boolean target){fold=Math.max(0,Math.min(1,progress));collapsed=target;accessibility();invalidate();}
 public void sideButtonsHidden(boolean hidden){if(sideButtonsHidden==hidden)return;sideButtonsHidden=hidden;accessibility();}
 private void accessibility(){setContentDescription(I18n.t(collapsed?R.string.climate_strip_expand:sideButtonsHidden?R.string.climate_strip_show_buttons:R.string.climate_strip_collapse)+(state==null?"":" · "+description(state)));}
 public static String description(ProbeState.Snapshot s){return s.temperature(27)+" · airflow="+ClimateAirflow.mask(s)+" · fan="+s.raw(21)+" · A/C="+s.flag(11)+" · "+s.temperature(28);}
 private void text(Canvas c,String v,float x,float y,float size,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));Paint.FontMetrics f=p.getFontMetrics();c.drawText(v,x,y-(f.ascent+f.descent)/2,p);}
 @Override protected void onDraw(Canvas c){super.onDraw(c);if(state==null)return;ThemeSettings.Palette t=ThemeSettings.resolve(getContext()).palette;float d=getResources().getDisplayMetrics().density;c.save();c.scale(d,d);float windowWidth=getWidth()/d,h=getHeight()/d;
  float expanded=expandedWidthPx>0?expandedWidthPx/d:Math.min(420,(getResources().getDisplayMetrics().widthPixels/d)-24);
  float w=expanded+(Math.min(148,expanded)-expanded)*fold,y=h/2;
  c.translate((windowWidth-w)/2,0);
  // The folded handle sits at the bottom of its touch area, not the row centre.
  float pad=1+15*fold,cardHeight=(h-2)*(1-fold)+5*fold;
  float backgroundY=y+(h-5.5f-y)*fold;
  p.setStyle(Paint.Style.FILL);p.setColor((Integer)new ArgbEvaluator().evaluate(fold,t.card,t.text));p.setAlpha(Math.round(255*(.90f-.35f*fold)));c.drawRoundRect(new RectF(pad,backgroundY-cardHeight/2,w-pad,backgroundY+cardHeight/2),16-13.5f*fold,16-13.5f*fold,p);p.setAlpha(255);
  // A faint cool-to-warm tint appears only as the climate row folds away.
  if(fold>0){p.setShader(new LinearGradient(pad,0,w-pad,0,new int[]{0x303C8DDE,0x103C8DDE,0x10E76A70,0x30E76A70},new float[]{0,.42f,.58f,1},Shader.TileMode.CLAMP));p.setAlpha(Math.round(255*fold));c.drawRoundRect(new RectF(pad,backgroundY-cardHeight/2,w-pad,backgroundY+cardHeight/2),16-13.5f*fold,16-13.5f*fold,p);p.setShader(null);p.setAlpha(255);}
  if(fold<.01f){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(t.border);c.drawRoundRect(new RectF(1,1,w-1,h-1),16,16,p);}p.setStyle(Paint.Style.FILL);
  float contentAlpha=Math.max(0,1-fold*2);if(contentAlpha==0){c.restore();return;}
  c.saveLayerAlpha(0,0,w,h,Math.round(255*contentAlpha));c.translate((w-expanded)/2,0);w=expanded;
  float size=w<360?21:25;text(c,state.temperature(27),w*.13f,y,size,t.text);
  int glyph=ClimateAirflow.glyph(state);if(glyph>=0)ClimateIcons.draw(getContext(),c,p,glyph,w*.335f,y,28,w*.12f,t.accent);else text(c,"—",w*.335f,y,18,t.muted);
  ClimateIcons.draw(getContext(),c,p,21,w*.48f,y,25,t.accent);Integer fan=state.number(21);text(c,fan==null||fan<0||fan>7?"—":String.valueOf(fan),w*.565f,y,21,t.text);
  Integer ac=state.number(11);boolean acOn=Integer.valueOf(1).equals(ac);p.setStyle(Paint.Style.FILL);p.setColor(acOn?t.accent:t.border);c.drawRoundRect(new RectF(w*.69f-23,y-14,w*.69f+23,y+14),8,8,p);text(c,ac==null?"A/C ?":"A/C",w*.69f,y,12,acOn?t.accentText:t.muted);
  text(c,state.temperature(28),w*.87f,y,size,t.text);c.restore();c.restore();
 }
}
