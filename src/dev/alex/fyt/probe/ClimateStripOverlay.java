package dev.alex.fyt.probe;
import android.animation.ValueAnimator;import android.view.animation.DecelerateInterpolator;import android.content.*;import android.graphics.PixelFormat;import android.graphics.drawable.GradientDrawable;import android.widget.FrameLayout;import android.widget.ImageButton;import android.os.*;import android.view.*;
/** Tappable bottom climate strip; the large panel always has display priority. */
public final class ClimateStripOverlay {
 private final Context context;private final ClimateOverlay popup;private final WindowManager windows;private final Handler ui=new Handler(Looper.getMainLooper());
 private FrameLayout view;private ClimateStripView strip;private ImageButton expandButton,minimizeButton;private ThemeSettings.Palette buttonPalette;private boolean buttonShown,buttonWanted,lastAlwaysAutoHide;private float buttonReveal;private ValueAnimator buttonAnimator;private long buttonHideAt;private WindowManager.LayoutParams layout;private ValueAnimator animator;private ViewTreeObserver.OnGlobalLayoutListener pendingLayout;private float fold;private boolean animationWindow,collapsed,started,failed,wasAllowed;
 private static final long AUTO_SHOW_MS=5000;
 private long demoUntil;private boolean wasDemo;
 private long lastRevision=-1;private String lastClimate;private boolean autoExpanded;private long autoHideAt;
 private final Runnable tick=new Runnable(){public void run(){if(!started)return;render();ui.postDelayed(this,200);}};
 public ClimateStripOverlay(Context c,ClimateOverlay popup){context=c;this.popup=popup;windows=(WindowManager)c.getSystemService(Context.WINDOW_SERVICE);}
 public static boolean enabled(Context c){return c.getSharedPreferences("overlay",0).getBoolean("climateStrip",true);}
 public static void enabled(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("climateStrip",on).apply();}
 public static boolean active(ProbeState.Snapshot s){return s.running&&s.connected&&s.honda()&&Integer.valueOf(1).equals(s.climatePower());}
 public static boolean alwaysAutoHide(Context c){return c.getSharedPreferences("overlay",0).getBoolean("climateStripAlwaysAutoHide",false);}
 public static void alwaysAutoHide(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripAlwaysAutoHide",on).apply();}
 // The legacy preference now enables both side buttons; keep its key for installed users.
 public static boolean expandButton(Context c){return c.getSharedPreferences("overlay",0).getBoolean("climateStripExpandButton",false);}
 public static void expandButton(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripExpandButton",on).apply();}
 public static boolean buttonAutoHide(Context c){return c.getSharedPreferences("overlay",0).getBoolean("climateStripButtonAutoHide",false);}
 public static void buttonAutoHide(Context c,boolean on){c.getSharedPreferences("overlay",0).edit().putBoolean("climateStripButtonAutoHide",on).apply();}
 private int dp(int n){return Math.round(n*context.getResources().getDisplayMetrics().density);}
 public void start(){if(started)return;started=true;ui.post(tick);}
 public void demo(){demoUntil=SystemClock.uptimeMillis()+15000;failed=false;if(started)render();}
 public void refreshOptions(){failed=false;if(started)render();}
 private void hide(){cancelPendingLayout();if(animator!=null){animator.cancel();animator=null;}if(buttonAnimator!=null){buttonAnimator.cancel();buttonAnimator=null;}buttonWanted=false;buttonReveal=0;animationWindow=false;lastClimate=null;lastRevision=-1;autoExpanded=false;autoHideAt=0;buttonHideAt=0;buttonShown=false;buttonPalette=null;strip=null;expandButton=minimizeButton=null;layout=null;if(view!=null){try{windows.removeViewImmediate(view);}catch(RuntimeException ignored){}view=null;}}
 // Android otherwise adds its own lateral move animation when a centered window changes width.
 // This framework flag has existed since API 22; use reflection because it is not in the SDK stub.
 private static void noWindowMoveAnimation(WindowManager.LayoutParams lp){lp.windowAnimations=0;try{java.lang.reflect.Field flags=WindowManager.LayoutParams.class.getField("privateFlags");flags.setInt(lp,flags.getInt(lp)|0x40);}catch(ReflectiveOperationException|RuntimeException ignored){}}
 private boolean reserveButton(){return expandButton(context)||buttonWanted||buttonAnimator!=null||buttonReveal>0;}
 private int expandedWidth(){return Math.min(dp(420),context.getResources().getDisplayMetrics().widthPixels-dp(24)-dp(reserveButton()?104:0));}
 private void cancelPendingLayout(){if(pendingLayout!=null&&view!=null&&view.getViewTreeObserver().isAlive())view.getViewTreeObserver().removeOnGlobalLayoutListener(pendingLayout);pendingLayout=null;}
 // Window size changes only at the animation boundaries. Wait for its matching
 // layout before drawing, so old content dimensions cannot appear at a new x.
 // Keep the window visible while measuring: deferred layout callbacks for an
 // INVISIBLE overlay must not strand the collapsed handle in a hidden window.
 private void resizeWindow(int width,int height,boolean forAnimation,Runnable ready){
  if(view==null||layout==null)return;cancelPendingLayout();final FrameLayout target=view;
  // Reserve equal gutters on both sides for the whole expanded state, including its fade.
  // The strip is centered by the window itself; both buttons use the symmetric gutters.
  // A button appearing/disappearing must not resize or restart a strip animation.
  buttonShown=reserveButton()&&(forAnimation||!collapsed);animationWindow=forAnimation;
  int gutter=dp(buttonShown?52:0),bottomInset=forAnimation?dp(8):0;
  strip.expandedWidth(expandedWidth());FrameLayout.LayoutParams content=new FrameLayout.LayoutParams(width,height,Gravity.BOTTOM|Gravity.LEFT);content.leftMargin=gutter;content.bottomMargin=bottomInset;strip.setLayoutParams(content);strip.setTranslationY(forAnimation?dp(8)*fold:0);
  for(int side=0;side<2;side++){ImageButton button=side==0?expandButton:minimizeButton;FrameLayout.LayoutParams buttonLayout=(FrameLayout.LayoutParams)button.getLayoutParams();buttonLayout.bottomMargin=dp(6)+bottomInset;button.setLayoutParams(buttonLayout);}setButtonsVisibility(buttonShown?(buttonWanted||buttonAnimator!=null||(forAnimation&&buttonReveal>0)?View.VISIBLE:View.INVISIBLE):View.GONE);applyButton();
  width+=gutter*2;height=Math.max(height,buttonShown?dp(52):0)+bottomInset;
  final int targetWidth=width,targetHeight=height,targetX=0,targetY=forAnimation||collapsed?0:dp(8);
  boolean same=layout.width==width&&layout.height==height&&layout.x==targetX&&layout.y==targetY&&view.getWidth()==width&&view.getHeight()==height;
  if(same){view.setAlpha(1);view.setVisibility(popup.isWindowPresent()?View.INVISIBLE:View.VISIBLE);ready.run();return;}
  view.setAlpha(0);view.setVisibility(popup.isWindowPresent()?View.INVISIBLE:View.VISIBLE);pendingLayout=()->{if(view!=target||target.getWidth()!=targetWidth||target.getHeight()!=targetHeight)return;cancelPendingLayout();target.setAlpha(1);target.setVisibility(popup.isWindowPresent()?View.INVISIBLE:View.VISIBLE);ready.run();};
  target.getViewTreeObserver().addOnGlobalLayoutListener(pendingLayout);layout.width=width;layout.height=height;layout.x=targetX;layout.y=targetY;windows.updateViewLayout(target,layout);
 }
 private void applyFold(){if(view==null||layout==null)return;strip.setTranslationY(animationWindow?dp(8)*fold:0);strip.fold(fold,collapsed);applyButton();}
 private void setButtonsVisibility(int visibility){if(expandButton!=null)expandButton.setVisibility(visibility);if(minimizeButton!=null)minimizeButton.setVisibility(visibility);}
 private void applyButton(){if(expandButton==null||minimizeButton==null)return;
  // Side space contracts with the card. During a fold, button visibility and position
  // share the strip's progress; a standalone button fade is only for an open strip.
  float d=context.getResources().getDisplayMetrics().density,group=1-fold,amount=buttonReveal*Math.max(0,1-fold*2);
  float sideTravel=(expandedWidth()-Math.min(dp(148),expandedWidth()))*.5f+dp(32);
  for(int side=0;side<2;side++){ImageButton button=side==0?expandButton:minimizeButton;
   button.setAlpha(amount);button.setScaleX((.9f+.1f*buttonReveal)*group);button.setScaleY((.9f+.1f*buttonReveal)*group);
   button.setTranslationX((side==0?1:-1)*sideTravel*fold);
   button.setTranslationY(dp(4)*(1-buttonReveal)+(20.5f*d+(animationWindow?dp(8):0))*fold);
   button.setEnabled(buttonWanted&&!collapsed);button.setImportantForAccessibility(buttonWanted&&!collapsed?View.IMPORTANT_FOR_ACCESSIBILITY_AUTO:View.IMPORTANT_FOR_ACCESSIBILITY_NO);
  }
  strip.sideButtonsHidden(!collapsed&&expandButton(context)&&!buttonWanted);
 }
 private void prepareButtonFold(){if(buttonAnimator!=null){buttonAnimator.cancel();buttonAnimator=null;}buttonWanted=!collapsed&&expandButton(context)&&(!buttonAutoHide(context)||SystemClock.uptimeMillis()<buttonHideAt);if(!collapsed)buttonReveal=buttonWanted?1:0;}

 private void showButton(boolean wanted){
  if(wanted==buttonWanted)return;buttonWanted=wanted;if(wanted&&buttonShown)setButtonsVisibility(View.VISIBLE);if(buttonAnimator!=null){buttonAnimator.cancel();buttonAnimator=null;}
  final FrameLayout target=view;ValueAnimator next=ValueAnimator.ofFloat(buttonReveal,wanted?1:0);buttonAnimator=next;next.setDuration(260);next.setInterpolator(new DecelerateInterpolator(1.5f));
  next.addUpdateListener(a->{if(view!=target||buttonAnimator!=a)return;buttonReveal=(Float)a.getAnimatedValue();applyButton();});
  next.addListener(new android.animation.AnimatorListenerAdapter(){private boolean canceled;@Override public void onAnimationCancel(android.animation.Animator a){canceled=true;}@Override public void onAnimationEnd(android.animation.Animator a){if(canceled||view!=target||buttonAnimator!=a)return;buttonAnimator=null;if(!buttonWanted)setButtonsVisibility(buttonShown?View.INVISIBLE:View.GONE);applyButton();}});applyButton();next.start();
 }
 private void settleWindow(){if(view==null||layout==null)return;resizeWindow(collapsed?Math.min(dp(148),expandedWidth()):expandedWidth(),dp(collapsed?36:52),false,()->{if(view!=null){applyFold();strip.post(()->{if(strip!=null)strip.sendAccessibilityEvent(android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);});}});}
 private void animateFold(boolean targetCollapsed){
  collapsed=targetCollapsed;cancelPendingLayout();if(animator!=null){animator.cancel();animator=null;}final FrameLayout target=view;
  prepareButtonFold();
  resizeWindow(expandedWidth(),dp(52),true,()->{if(view!=target)return;ValueAnimator next=ValueAnimator.ofFloat(fold,collapsed?1:0);animator=next;next.setDuration(260);next.setInterpolator(new DecelerateInterpolator(1.5f));
   next.addUpdateListener(a->{if(view!=target||animator!=a)return;fold=(Float)a.getAnimatedValue();try{applyFold();}catch(RuntimeException e){hide();failed=true;}});
   next.addListener(new android.animation.AnimatorListenerAdapter(){private boolean canceled;@Override public void onAnimationCancel(android.animation.Animator a){canceled=true;}@Override public void onAnimationEnd(android.animation.Animator a){if(canceled||view!=target||animator!=a)return;animator=null;if(collapsed)buttonReveal=0;try{settleWindow();}catch(RuntimeException e){hide();failed=true;}}});next.start();
  });
 }
 private void onBarTap(){
  if(view==null||popup.isWindowPresent())return;
  if(!collapsed&&expandButton(context)&&!buttonWanted){
   long now=SystemClock.uptimeMillis();buttonHideAt=now+AUTO_SHOW_MS;
   if(autoExpanded||alwaysAutoHide(context)){autoExpanded=true;autoHideAt=now+AUTO_SHOW_MS;}
   showButton(true);return;
  }
  toggle();
 }
 private void toggle(){if(view==null||popup.isWindowPresent())return;if(collapsed)buttonHideAt=SystemClock.uptimeMillis()+AUTO_SHOW_MS;autoExpanded=collapsed&&alwaysAutoHide(context);autoHideAt=autoExpanded?SystemClock.uptimeMillis()+AUTO_SHOW_MS:0;animateFold(!collapsed);if(SystemClock.uptimeMillis()>=demoUntil)context.getSharedPreferences("overlay",0).edit().putBoolean("climateStripCollapsed",collapsed).apply();ProbeState.INSTANCE.climateStrip(true,ClimateStripView.description(SystemClock.uptimeMillis()<demoUntil?ClimateOverlay.demoSnapshot():ProbeState.INSTANCE.overlaySnapshot()),collapsed);}
 private void render(){ProbeState state=ProbeState.INSTANCE;long now=SystemClock.uptimeMillis();boolean test=now<demoUntil;if(test!=wasDemo){hide();wasDemo=test;if(test)buttonHideAt=now+AUTO_SHOW_MS;}ProbeState.Snapshot s=test?ClimateOverlay.demoSnapshot():state.overlaySnapshot();boolean allowed=android.provider.Settings.canDrawOverlays(context);if(allowed!=wasAllowed){failed=false;wasAllowed=allowed;}
  if(!allowed||(!test&&(!enabled(context)||!active(s)))){hide();state.climateStrip(false,"");return;}if(failed)return;
  try{if(view==null){collapsed=!test&&context.getSharedPreferences("overlay",0).getBoolean("climateStripCollapsed",false);fold=collapsed?1:0;view=new FrameLayout(context);strip=new ClimateStripView(context);strip.setOnClickListener(v->onBarTap());view.addView(strip);expandButton=new ImageButton(context);expandButton.setImageResource(R.drawable.ic_climate_expand);expandButton.setPadding(dp(10),dp(10),dp(10),dp(10));expandButton.setContentDescription(I18n.t(R.string.climate_strip_full_panel));expandButton.setOnClickListener(v->{popup.showFromStrip(SystemClock.uptimeMillis()<demoUntil);if(popup.isWindowPresent()&&view!=null){view.setVisibility(View.INVISIBLE);ProbeState.INSTANCE.climateStrip(false,lastClimate==null?"":lastClimate,collapsed);}});FrameLayout.LayoutParams buttonLayout=new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.LEFT|Gravity.BOTTOM);buttonLayout.bottomMargin=dp(6);view.addView(expandButton,buttonLayout);minimizeButton=new ImageButton(context);minimizeButton.setImageResource(R.drawable.ic_climate_minimize);minimizeButton.setPadding(dp(10),dp(10),dp(10),dp(10));minimizeButton.setContentDescription(I18n.t(R.string.climate_strip_collapse));minimizeButton.setOnClickListener(v->{if(!collapsed)toggle();});FrameLayout.LayoutParams minimizeLayout=new FrameLayout.LayoutParams(dp(40),dp(40),Gravity.RIGHT|Gravity.BOTTOM);minimizeLayout.bottomMargin=dp(6);view.addView(minimizeButton,minimizeLayout);setButtonsVisibility(View.GONE);applyButton();lastAlwaysAutoHide=alwaysAutoHide(context);if(lastAlwaysAutoHide&&!collapsed){autoExpanded=true;autoHideAt=now+AUTO_SHOW_MS;}int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
   // Force an initial size transition: an unchanged window size can skip the layout callback while invisible.
   WindowManager.LayoutParams lp=new WindowManager.LayoutParams(1,1,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);noWindowMoveAnimation(lp);lp.gravity=Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;lp.y=dp(8);lp.setTitle("Honda Box Climate Strip");layout=lp;windows.addView(view,lp);applyFold();settleWindow();}
   String climate=ClimateStripView.description(s);
   // Establish a baseline on creation; repeated callbacks with identical values do not open it.
   boolean changed=lastClimate!=null&&(!lastClimate.equals(climate)||(lastRevision>=0&&lastRevision!=s.climateRevision));lastRevision=s.climateRevision;
   boolean always=alwaysAutoHide(context);if(always!=lastAlwaysAutoHide){lastAlwaysAutoHide=always;if(always&&!collapsed){autoExpanded=true;autoHideAt=now+AUTO_SHOW_MS;}else if(!always){autoExpanded=false;autoHideAt=0;}}
   if(changed){buttonHideAt=now+AUTO_SHOW_MS;if(collapsed||autoExpanded||always){autoExpanded=true;autoHideAt=now+AUTO_SHOW_MS;if(collapsed)animateFold(false);}}
   lastClimate=climate;strip.content(s);
   // Keep the lower window mounted, but make it invisible for the entire popup animation.
   // This also handles enabling the strip while the popup is already open.
   boolean visible=!popup.isWindowPresent();
   // Give the compact row five visible seconds after the larger popup closes.
   if(autoExpanded){if(!visible)autoHideAt=now+AUTO_SHOW_MS;else if(now>=autoHideAt){autoExpanded=false;animateFold(true);}}
   if(buttonHideAt>now&&!visible)buttonHideAt=now+AUTO_SHOW_MS;
   if(animator==null&&pendingLayout==null&&buttonShown!=(reserveButton()&&!collapsed))settleWindow();
   boolean showButton=!collapsed&&expandButton(context)&&(!buttonAutoHide(context)||now<buttonHideAt);
   if(animator==null&&pendingLayout==null)showButton(showButton);
   ThemeSettings.Palette palette=ThemeSettings.resolve(context).palette;if(buttonPalette!=palette){buttonPalette=palette;GradientDrawable background=new GradientDrawable();background.setColor((palette.card&0xffffff)|0xE6000000);background.setCornerRadius(dp(12));background.setStroke(dp(1),palette.border);expandButton.setBackground(background);expandButton.setColorFilter(palette.accent);GradientDrawable rightBackground=new GradientDrawable();rightBackground.setColor((palette.card&0xffffff)|0xE6000000);rightBackground.setCornerRadius(dp(12));rightBackground.setStroke(dp(1),palette.border);minimizeButton.setBackground(rightBackground);minimizeButton.setColorFilter(palette.accent);}expandButton.setContentDescription(I18n.t(R.string.climate_strip_full_panel));minimizeButton.setContentDescription(I18n.t(R.string.climate_strip_collapse));
   view.setAlpha(pendingLayout==null?1:0);view.setVisibility(visible?View.VISIBLE:View.INVISIBLE);state.climateStrip(visible,climate,collapsed);
  }catch(RuntimeException e){hide();failed=true;state.climateStrip(false,"");state.log("CLIMATE STRIP ERROR: "+e.getClass().getSimpleName());}
 }
 public void close(){demoUntil=0;wasDemo=false;started=false;ui.removeCallbacks(tick);hide();ProbeState.INSTANCE.climateStrip(false,"");}
}
