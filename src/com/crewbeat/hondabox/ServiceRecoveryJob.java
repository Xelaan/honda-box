package com.crewbeat.hondabox;
import android.app.job.*;
/** Short periodic liveness check. Android chooses the execution time. */
public final class ServiceRecoveryJob extends JobService {
    @Override public boolean onStartJob(JobParameters params){BackgroundRecovery.check(this);return false;}
    @Override public boolean onStopJob(JobParameters params){return false;}
}
