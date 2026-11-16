package com.lessonbell.watch;

import android.content.Context;
import androidx.wear.tiles.TileService;

/** Refresh both Tiles after any cached timetable, alarm or skip-date change. */
public final class TileRefresh {
    private TileRefresh() {}
    public static void request(Context context) {
        try {
            TileService.getUpdater(context).requestUpdate(NextLessonTileService.class);
            TileService.getUpdater(context).requestUpdate(TodayTileService.class);
        } catch (RuntimeException ignored) { /* Tiles remain optional on unsupported launchers. */ }
    }
}
