package com.lessonbell.watch;

import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.tiles.ResourceBuilders;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/** First watch tile: next class, countdown, skip today, sync and open. */
public final class NextLessonTileService extends TileService {
    @Override public ListenableFuture<TileBuilders.Tile> onTileRequest(RequestBuilders.TileRequest request) {
        TileActions.handle(getApplicationContext(),request.getCurrentState().getLastClickableId());
        return Futures.immediateFuture(TileLayouts.next(this));
    }
    @Override public ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(RequestBuilders.ResourcesRequest request) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder().setVersion("1").build());
    }
}
