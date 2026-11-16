package com.lessonbell.watch;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

/** Periodic, network-gated refresh. Does not prompt for Google authorization in the background. */
public final class CalendarSyncJob extends JobService {
    private volatile boolean stopped;
    @Override public boolean onStartJob(JobParameters params) {
        stopped=false;
        new Thread(()->{
            boolean retry=false;
            try {
                String token=GoogleCalendarAuth.silentToken(getApplicationContext());
                if(token==null) {
                    Lessons.prefs(this).edit().putString("google_sync_status","Reconnect Google Calendar on the watch").apply();
                } else if(!stopped) {
                    CalendarSync.sync(this,token);
                }
            }catch(Exception e){
                retry=true;
                Lessons.prefs(this).edit().putString("google_sync_status","Sync delayed: check Wi-Fi and Google access").apply();
                Log.w("LessonBell","Calendar sync retry needed: "+e.getClass().getSimpleName());
            }finally{jobFinished(params,retry && !stopped);}
        },"lessonbell-google-sync").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {stopped=true;return true;}
}
