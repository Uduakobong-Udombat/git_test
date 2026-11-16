package com.lessonbell.watch;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/** Permission-based, watch-local Google Identity authorization. No Google password or OAuth token is stored by LessonBell. */
public final class GoogleCalendarAuth {
    private GoogleCalendarAuth() {}
    private static final String SCOPE="https://www.googleapis.com/auth/calendar.readonly";
    static final int JOB_ID=0x1E550B;
    public static AuthorizationRequest request() {
        return AuthorizationRequest.builder().setRequestedScopes(Collections.singletonList(new Scope(SCOPE))).build();
    }
    /** Never launches consent UI from a background job. Returns null if interactive consent is necessary. */
    public static String silentToken(Context context) throws Exception {
        AuthorizationResult result=Tasks.await(Identity.getAuthorizationClient(context).authorize(request()),25,TimeUnit.SECONDS);
        if(result.hasResolution())return null;
        String token=result.getAccessToken();
        if(token==null || token.isEmpty())return null;
        return token;
    }
    public static void enableBackground(Context context) {
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler==null)return;
        if(!Lessons.prefs(context).getBoolean("google_connected",false))return;
        JobInfo job=new JobInfo.Builder(JOB_ID,new ComponentName(context,CalendarSyncJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(60L*60*1000) // best-effort, Android may defer during battery saving
                .setPersisted(true).build();
        scheduler.schedule(job);
    }
    public static void disableBackground(Context context) {
        JobScheduler scheduler=(JobScheduler)context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler!=null)scheduler.cancel(JOB_ID);
    }
}
