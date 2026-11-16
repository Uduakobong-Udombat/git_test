package com.lessonbell.watch;

import androidx.wear.tiles.TileService;
import androidx.wear.tiles.TileBuilders;
import androidx.wear.tiles.RequestBuilders;
import androidx.wear.protolayout.ResourceBuilders;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/** Second tile: today's full schedule (up to four periods on the round screen). */
public final class TodayTileService extends TileService {
    @Override public ListenableFuture<TileBuilders.Tile> onTileRequest(RequestBuilders.TileRequest request) {
        TileActions.handle(getApplicationContext(),request.getCurrentState().getLastClickableId());
        return Futures.immediateFuture(TileLayouts.today(this));
    }
    @Override public ListenableFuture<ResourceBuilders.Resources> onTileResourcesRequest(RequestBuilders.ResourcesRequest request) {
        return Futures.immediateFuture(new ResourceBuilders.Resources.Builder().setVersion("1").build());
    }
}
