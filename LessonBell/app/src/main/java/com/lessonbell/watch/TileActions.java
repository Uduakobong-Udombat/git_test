package com.lessonbell.watch;

import android.content.Context;
import android.util.Log;
import java.time.LocalDate;

/** Actions run locally on the watch; no Android phone companion involved. */
public final class TileActions {
    private TileActions() {}
    public static void handle(Context context, String action) {
        if (action == null || action.isEmpty()) return;
        if ("toggle-today".equals(action)) {
            LocalDate day=LocalDate.now();
            Lessons.setSkipped(context,day,!Lessons.isSkipped(context,day));
            // Keep tile interaction responsive; alarm changes are recomputed off main thread.
            new Thread(() -> Scheduler.schedule(context.getApplicationContext()), "lessonbell-tile-alarms").start();
            TileRefresh.request(context);
        } else if ("sync".equals(action)) {
            if (!CalendarSync.isConnected(context)) {
                Lessons.prefs(context).edit().putString("google_sync_status", "Open LessonBell to connect Google").apply();
                TileRefresh.request(context);return;
            }
            Lessons.prefs(context).edit().putString("google_sync_status", "Refreshing from watch...").apply();
            new Thread(() -> {
                try {
                    String token=GoogleCalendarAuth.silentToken(context);
                    if (token == null) {
                        Lessons.prefs(context).edit().putString("google_sync_status", "Open LessonBell to renew Google access").apply();
                    } else {
                        CalendarSync.sync(context,token);
                    }
                } catch (Exception e) {
                    Log.w("LessonBell","Tile sync failed",e);
                    Lessons.prefs(context).edit().putString("google_sync_status", "Sync failed; open LessonBell to retry").apply();
                } finally { TileRefresh.request(context); }
            }, "lessonbell-tile-sync").start();
            TileRefresh.request(context);
        }
    }
}
