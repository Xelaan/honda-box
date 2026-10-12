package dev.alex.fyt.probe;

import android.app.Activity;
import android.content.*;
import android.os.*;
import android.net.Uri;
import android.widget.Toast;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Shared SAF export; JSON computation and file writing run off the UI thread. */
final class JsonExport {
    private static final int REQUEST=21;private final Activity activity;
    private final Handler ui=new Handler(Looper.getMainLooper());private boolean busy;private String report;
    JsonExport(Activity activity){this.activity=activity;}
    private void toast(String text){Toast.makeText(activity,text,Toast.LENGTH_LONG).show();}
    void start(){if(busy){toast(I18n.t(R.string.ui_preparing_log));return;}busy=true;toast(I18n.t(R.string.ui_preparing_log_77));
        new Thread(()->{try{final String value=ProbeState.INSTANCE.exportJson();ui.post(()->{
            busy=false;if(activity.isFinishing()||activity.isDestroyed())return;report=value;
            try{activity.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/json").putExtra(Intent.EXTRA_TITLE,"fyt-probe-"+System.currentTimeMillis()+".json"),REQUEST);}
            catch(ActivityNotFoundException e){((ClipboardManager)activity.getSystemService(Activity.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("FYT Probe JSON",report));toast(I18n.t(R.string.ui_saving_unavailable_json_copied_to));}
        });}catch(Exception e){ui.post(()->{busy=false;toast(I18n.t(R.string.ui_preparation_error)+e.getMessage());});}},"ProbeExportPrepare").start();
    }
    void result(int request,int result,Intent data){if(request!=REQUEST)return;if(result!=Activity.RESULT_OK||data==null||data.getData()==null){report=null;return;}
        final Uri uri=data.getData();final String saved=report;report=null;busy=true;
        new Thread(()->{try{String value=saved!=null?saved:ProbeState.INSTANCE.exportJson();
            try(OutputStream stream=activity.getContentResolver().openOutputStream(uri,"wt")){if(stream==null)throw new java.io.IOException(I18n.t(R.string.ui_cannot_access_the_file));stream.write(value.getBytes(StandardCharsets.UTF_8));}
            ui.post(()->{busy=false;toast(I18n.t(R.string.ui_log_saved));});
        }catch(Exception e){ui.post(()->{busy=false;toast(I18n.t(R.string.ui_save_error)+e.getMessage());});}},"ProbeExportWrite").start();
    }
}
