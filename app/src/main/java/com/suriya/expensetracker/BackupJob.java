package com.suriya.expensetracker;
import android.app.job.JobParameters;
import android.app.job.JobService;
/** Android may defer this job; opening the app also checks for missed backups. */
public final class BackupJob extends JobService {
    @Override public boolean onStartJob(JobParameters params){new Thread(()->{try{BackupManager.write(getApplicationContext(),false);}catch(Exception ignored){}finally{jobFinished(params,false);}}).start();return true;}
    @Override public boolean onStopJob(JobParameters params){return true;}
}
