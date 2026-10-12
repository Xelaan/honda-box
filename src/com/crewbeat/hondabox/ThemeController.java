package com.crewbeat.hondabox;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.location.*;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Location is requested only from a visible screen, by an explicit user action.
 * The last fix is saved privately for offline solar switching; no background-location permission. */
public final class ThemeController implements LocationListener {
    public static final int REQUEST=88;
    private final Activity activity;private final Runnable changed;
    private boolean visible,listening;private String gpsStatus="";private TextView info;
    private LocationManager manager;
    public ThemeController(Activity a,Runnable c){activity=a;changed=c;manager=(LocationManager)a.getSystemService(Context.LOCATION_SERVICE);}
    private int dp(int n){return Math.round(n*activity.getResources().getDisplayMetrics().density);}
    public View controls(){
        LinearLayout column=new LinearLayout(activity);column.setOrientation(LinearLayout.VERTICAL);
        LinearLayout row=new LinearLayout(activity);ThemeSettings.State state=ThemeSettings.resolve(activity);ThemeSettings.Palette p=state.palette;
        String[] modes={ThemeSettings.DAY,ThemeSettings.NIGHT,ThemeSettings.AUTO,ThemeSettings.SYSTEM},names={I18n.t(R.string.ui_day),I18n.t(R.string.ui_night),I18n.t(R.string.ui_auto),I18n.t(R.string.theme_system)};
        for(int i=0;i<modes.length;i++){
            final String mode=modes[i];Button b=button(names[i],mode.equals(state.mode),p);
            b.setContentDescription(I18n.t(R.string.ui_theme)+names[i]);b.setOnClickListener(v->{ThemeSettings.mode(activity,mode);changed.run();});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(48));lp.setMargins(0,0,dp(8),0);row.addView(b,lp);
        }
        column.addView(row);
        info=new TextView(activity);info.setTextColor(p.muted);info.setTextSize(11);info.setPadding(0,dp(5),0,0);column.addView(info);TextView criterion=new TextView(activity);criterion.setText(I18n.t(R.string.theme_auto_criterion));criterion.setTextColor(p.muted);criterion.setTextSize(11);criterion.setPadding(0,dp(5),0,0);column.addView(criterion);
        if(ThemeSettings.AUTO.equals(state.mode)){
            Button location=button(I18n.t(R.string.ui_location),false,p);location.setContentDescription(I18n.t(R.string.ui_location_for_sunrise_and_sunset));location.setOnClickListener(v->showLocation());
            LinearLayout.LayoutParams locationParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,dp(48));locationParams.topMargin=dp(8);column.addView(location,locationParams);
        }
        column.addView(new AccentController(activity,changed).controls());update();return column;
    }
    private Button button(String name,boolean active,ThemeSettings.Palette p){return ConsoleControls.button(activity,name,p,active);}
    public void update(){if(info!=null)info.setText(ThemeSettings.resolve(activity).description+(gpsStatus.isEmpty()?"":" · "+gpsStatus));}
    public void showLocation(){
        ThemeSettings.Palette p=ThemeSettings.resolve(activity).palette;
        LinearLayout box=new LinearLayout(activity);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(22),dp(8),dp(22),dp(8));
        TextView explanation=new TextView(activity);explanation.setText(I18n.t(R.string.ui_enter_coordinates_or_obtain_them));explanation.setTextSize(14);explanation.setTextColor(p.text);box.addView(explanation);
        EditText lat=new EditText(activity),lon=new EditText(activity);lat.setHint(I18n.t(R.string.ui_latitude_90_90));lon.setHint(I18n.t(R.string.ui_longitude_180_180));
        int type=InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL|InputType.TYPE_NUMBER_FLAG_SIGNED;lat.setInputType(type);lon.setInputType(type);lat.setTextColor(p.text);lon.setTextColor(p.text);lat.setHintTextColor(p.muted);lon.setHintTextColor(p.muted);lat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(p.accent));lon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(p.accent));
        if(ThemeSettings.hasLocation(activity)){lat.setText(String.format(Locale.US,"%.5f",ThemeSettings.latitude(activity)));lon.setText(String.format(Locale.US,"%.5f",ThemeSettings.longitude(activity)));}
        box.addView(lat);box.addView(lon);
        TextView origin=new TextView(activity);origin.setTextColor(p.muted);origin.setText(ThemeSettings.gps(activity)?I18n.t(R.string.ui_source_gps_last_saved_location):I18n.t(R.string.ui_source_manual));box.addView(origin);
        AlertDialog dialog=new AlertDialog.Builder(activity, p.night?AlertDialog.THEME_DEVICE_DEFAULT_DARK:AlertDialog.THEME_DEVICE_DEFAULT_LIGHT)
            .setTitle(I18n.t(R.string.ui_theme_location)).setView(box).setPositiveButton(I18n.t(R.string.ui_save),null).setNeutralButton(I18n.t(R.string.ui_get_gps),null).setNegativeButton(I18n.t(R.string.ui_cancel),null).create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{
                double a=Double.parseDouble(lat.getText().toString().trim().replace(',','.')),o=Double.parseDouble(lon.getText().toString().trim().replace(',','.'));
                if(!SolarTimes.valid(a,o))throw new IllegalArgumentException();stopGps();gpsStatus="";ThemeSettings.location(activity,a,o,false);dialog.dismiss();changed.run();
            }catch(RuntimeException e){lat.setError(I18n.t(R.string.ui_enter_numbers_latitude_90_90));}});
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{dialog.dismiss();requestGps();});
        });dialog.show();
    }
    private boolean permission(){return activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED||activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;}
    private void requestGps(){ThemeSettings.gps(activity,true);if(!permission())activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST);else startGps();}
    public void permissionResult(int request){if(request!=REQUEST)return;if(permission())startGps();else{ThemeSettings.gps(activity,false);gpsStatus=I18n.t(R.string.ui_gps_permission_not_granted);update();}}
    public void start(){visible=true;if(ThemeSettings.gps(activity)&&permission())startGps();}
    public void stop(){visible=false;stopGps();}
    private void stopGps(){if(manager!=null&&listening)manager.removeUpdates(this);listening=false;}
    private void startGps(){
        if(!visible||manager==null||!permission())return;stopGps();gpsStatus=I18n.t(R.string.ui_gps_waiting_for_coordinates);
        Location newest=null;
        for(String provider:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER})try{
            if(!manager.isProviderEnabled(provider))continue;
            Location l=manager.getLastKnownLocation(provider);if(l!=null&&(newest==null||l.getTime()>newest.getTime()))newest=l;
            manager.requestLocationUpdates(provider,60000,500,this,Looper.getMainLooper());listening=true;
        }catch(SecurityException|IllegalArgumentException ignored){}
        if(newest!=null&&System.currentTimeMillis()-newest.getTime()>=0&&System.currentTimeMillis()-newest.getTime()<86400000)onLocationChanged(newest);
        if(!listening)gpsStatus=I18n.t(R.string.ui_gps_unavailable_enter_coordinates_manually);update();
    }
    @Override public void onLocationChanged(Location location){if(!visible||!ThemeSettings.gps(activity)||!SolarTimes.valid(location.getLatitude(),location.getLongitude()))return;
        ThemeSettings.location(activity,location.getLatitude(),location.getLongitude(),true);gpsStatus=I18n.t(R.string.ui_gps_location_saved);changed.run();}
    @Override public void onProviderEnabled(String provider){update();}
    @Override public void onProviderDisabled(String provider){gpsStatus=I18n.t(R.string.ui_gps_off_saved_location);update();}
    @Override public void onStatusChanged(String provider,int status,Bundle extras){}
}
