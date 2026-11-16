package com.lessonbell.watch;

import android.content.Context;
import androidx.wear.protolayout.ActionBuilders;
import androidx.wear.protolayout.ColorBuilders;
import androidx.wear.protolayout.DimensionBuilders;
import androidx.wear.protolayout.LayoutElementBuilders;
import androidx.wear.protolayout.ModifiersBuilders;
import androidx.wear.protolayout.TimelineBuilders;
import androidx.wear.tiles.TileBuilders;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Expanded, colorful round-screen Tiles. The background fills the whole Tile. */
public final class TileLayouts {
    private TileLayouts() {}

    // High-contrast color system: ink background, warm clock, fresh mint, cyan and violet.
    private static final int INK=0xff0a1329, WHITE=0xfff7fbff, MUTED=0xffa9bfd0,
            CYAN=0xff69d9ff, MINT=0xffa7f5b7, AMBER=0xffffc16e,
            LAVENDER=0xffc6b3ff, CORAL=0xffff9f91,
            MINT_BG=0xff153c38, SKIP_BG=0xff643641,
            SYNC_BG=0xff184c67, OPEN_BG=0xff493867,
            ROW_BG=0xff1a2b45;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private static LayoutElementBuilders.Text label(String value, int size, int color, boolean bold) {
        return new LayoutElementBuilders.Text.Builder()
            .setText(value)
            .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                .setSize(DimensionBuilders.sp(size))
                .setColor(ColorBuilders.argb(color))
                .setWeight(bold ? LayoutElementBuilders.FONT_WEIGHT_BOLD : LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                .build())
            .setMaxLines(1).build();
    }

    private static LayoutElementBuilders.Spacer gap(float dp) {
        return new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(dp)).build();
    }

    private static ModifiersBuilders.Background fill(int color, float radiusDp) {
        return new ModifiersBuilders.Background.Builder()
            .setColor(ColorBuilders.argb(color))
            .setCorner(new ModifiersBuilders.Corner.Builder()
                .setRadius(DimensionBuilders.dp(radiusDp)).build())
            .build();
    }

    private static ModifiersBuilders.Clickable launch(Context context, String id) {
        ActionBuilders.AndroidActivity target=new ActionBuilders.AndroidActivity.Builder()
            .setPackageName(context.getPackageName())
            .setClassName(MainActivity.class.getName()).build();
        return new ModifiersBuilders.Clickable.Builder().setId(id)
            .setOnClick(new ActionBuilders.LaunchAction.Builder().setAndroidActivity(target).build()).build();
    }

    private static ModifiersBuilders.Clickable load(String id) {
        return new ModifiersBuilders.Clickable.Builder().setId(id)
            .setOnClick(new ActionBuilders.LoadAction.Builder().build()).build();
    }

    private static LayoutElementBuilders.Box control(Context context, String text, String action,
                                                      int background, int foreground, boolean open) {
        return new LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(56)).setHeight(DimensionBuilders.dp(34))
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                .setBackground(fill(background, 13))
                .setClickable(open ? launch(context,action) : load(action)).build())
            .addContent(label(text,12,foreground,true)).build();
    }

    private static LayoutElementBuilders.Row actions(Context context, boolean skipped) {
        return new LayoutElementBuilders.Row.Builder()
            .setWidth(DimensionBuilders.wrap()).setHeight(DimensionBuilders.wrap())
            .addContent(control(context,skipped ? "Enable" : "Skip", "toggle-today",SKIP_BG,WHITE,false))
            .addContent(new LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(6)).build())
            .addContent(control(context,"Sync","sync",SYNC_BG,CYAN,false))
            .addContent(new LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(6)).build())
            .addContent(control(context,"Open","open",OPEN_BG,LAVENDER,true))
            .build();
    }

    private static LayoutElementBuilders.Box countdown(String text) {
        return new LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(176)).setHeight(DimensionBuilders.dp(33))
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                .setBackground(fill(MINT_BG,17)).build())
            .addContent(label(text,17,MINT,true)).build();
    }

    // Tiles must fill their viewport, not wrap a tiny content column.
    private static TileBuilders.Tile render(LayoutElementBuilders.Column content) {
        LayoutElementBuilders.Box background=new LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.expand())
            .setHeight(DimensionBuilders.expand())
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                .setBackground(fill(INK,120)).build())
            .addContent(content).build();
        return new TileBuilders.Tile.Builder().setResourcesVersion("1")
            .setFreshnessIntervalMillis(60_000L)
            .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(background)).build();
    }

    public static TileBuilders.Tile next(Context context) {
        LocalDate today=LocalDate.now();
        ZonedDateTime now=ZonedDateTime.now();
        Map.Entry<LocalDate,Lessons.Lesson> upcoming=null;
        for(Map.Entry<LocalDate,Lessons.Lesson> e:Lessons.upcoming(context,9)) {
            if(e.getValue().at(e.getKey(),ZoneId.systemDefault()).isAfter(now)) {upcoming=e;break;}
        }
        boolean skipped=Lessons.isSkipped(context,today);
        LayoutElementBuilders.Column.Builder c=new LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.dp(184)).setHeight(DimensionBuilders.wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(label("✦  NEXT LESSON",12,CYAN,true))
            .addContent(gap(8));
        if(upcoming == null) {
            c.addContent(label("All clear",23,WHITE,true)).addContent(gap(8))
             .addContent(label("No upcoming classes",14,MUTED,false))
             .addContent(gap(10)).addContent(countdown("You're up to date"));
        } else {
            Lessons.Lesson lesson=upcoming.getValue();
            ZonedDateTime at=lesson.at(upcoming.getKey(),ZoneId.systemDefault());
            long minutes=Math.max(1,(Duration.between(now,at).getSeconds()+59)/60);
            String remaining=minutes<60 ? "IN "+minutes+" MIN" : "IN "+(minutes/60)+"H "+(minutes%60)+"M";
            c.addContent(label(lesson.group,21,WHITE,true))
             .addContent(gap(1))
             .addContent(label(lesson.start.format(TIME),39,AMBER,true))
             .addContent(label(upcoming.getKey().format(DAY),12,MUTED,false))
             .addContent(gap(3))
             .addContent(label(lesson.room+"  •  "+lesson.period,13,LAVENDER,true))
             .addContent(gap(10))
             .addContent(countdown(remaining));
        }
        if(skipped) c.addContent(gap(3)).addContent(label("TODAY'S ALERTS PAUSED",10,CORAL,true));
        c.addContent(gap(skipped?9:13)).addContent(actions(context,skipped));
        return render(c.build());
    }

    private static LayoutElementBuilders.Box lessonRow(Lessons.Lesson lesson,int accent) {
        LayoutElementBuilders.Row row=new LayoutElementBuilders.Row.Builder()
            .setWidth(DimensionBuilders.wrap()).setHeight(DimensionBuilders.wrap())
            .addContent(label(lesson.start.format(TIME),12,accent,true))
            .addContent(new LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(8)).build())
            .addContent(label(lesson.group+" "+lesson.period,12,WHITE,true)).build();
        return new LayoutElementBuilders.Box.Builder()
            .setWidth(DimensionBuilders.dp(183)).setHeight(DimensionBuilders.dp(27))
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                .setBackground(fill(ROW_BG,9)).build())
            .addContent(row).build();
    }

    public static TileBuilders.Tile today(Context context) {
        LocalDate date=LocalDate.now();
        boolean skipped=Lessons.isSkipped(context,date);
        List<Lessons.Lesson> lessons=Lessons.forDate(context,date);
        LayoutElementBuilders.Column.Builder c=new LayoutElementBuilders.Column.Builder()
            .setWidth(DimensionBuilders.dp(185)).setHeight(DimensionBuilders.wrap())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .addContent(label("✦  TODAY'S LESSONS",12,CYAN,true))
            .addContent(gap(2))
            .addContent(label(date.format(DAY),12,MUTED,false))
            .addContent(gap(10));
        if(skipped) {
            c.addContent(label("Alerts paused",18,CORAL,true))
             .addContent(gap(6)).addContent(label("Enable to resume today",11,MUTED,false));
        } else if(lessons.isEmpty()) {
            c.addContent(label("No classes today",19,MINT,true))
             .addContent(gap(8)).addContent(label("Enjoy the free time",12,MUTED,false));
        } else {
            int[] palette={CYAN,AMBER,LAVENDER,MINT,CORAL};
            int count=Math.min(5,lessons.size());
            for(int i=0;i<count;i++){
                c.addContent(lessonRow(lessons.get(i),palette[i%palette.length]));
                if(i<count-1)c.addContent(gap(3));
            }
            if(lessons.size()>5)c.addContent(gap(3)).addContent(label("+"+(lessons.size()-5)+" more in the app",11,MUTED,false));
        }
        c.addContent(gap(12)).addContent(actions(context,skipped));
        return render(c.build());
    }
}
