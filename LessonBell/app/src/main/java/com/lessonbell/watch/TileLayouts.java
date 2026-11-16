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

/** Small round-friendly layouts shared by the two independent Wear OS tiles. */
public final class TileLayouts {
    private TileLayouts() {}
    private static final int BLUE=0xff79d5ff, WHITE=0xffeff7fc, MUTED=0xffb3c8d6,
            GREEN=0xff9eebbd, ORANGE=0xffffd28c;
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY=DateTimeFormatter.ofPattern("EEE d MMM",Locale.ENGLISH);

    static LayoutElementBuilders.Text line(String value,int fontSize,int color,boolean bold) {
        LayoutElementBuilders.FontStyle style=new LayoutElementBuilders.FontStyle.Builder()
                .setSize(DimensionBuilders.sp(fontSize))
                .setColor(ColorBuilders.argb(color))
                .setWeight(bold ? LayoutElementBuilders.FONT_WEIGHT_BOLD : LayoutElementBuilders.FONT_WEIGHT_NORMAL)
                .build();
        return new LayoutElementBuilders.Text.Builder().setText(value)
                .setFontStyle(style).setMaxLines(1).build();
    }
    static LayoutElementBuilders.Spacer space(float dp) {
        return new LayoutElementBuilders.Spacer.Builder().setHeight(DimensionBuilders.dp(dp)).build();
    }
    private static ModifiersBuilders.Clickable launch(Context c, String id) {
        ActionBuilders.AndroidActivity activity=new ActionBuilders.AndroidActivity.Builder()
                .setPackageName(c.getPackageName()).setClassName(MainActivity.class.getName()).build();
        return new ModifiersBuilders.Clickable.Builder().setId(id)
                .setOnClick(new ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity).build()).build();
    }
    private static ModifiersBuilders.Clickable load(String id) {
        return new ModifiersBuilders.Clickable.Builder().setId(id)
                .setOnClick(new ActionBuilders.LoadAction.Builder().build()).build();
    }
    private static LayoutElementBuilders.Text button(String value, String action, boolean open, Context c) {
        return new LayoutElementBuilders.Text.Builder()
                .setText(value)
                .setFontStyle(new LayoutElementBuilders.FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(12)).setWeight(LayoutElementBuilders.FONT_WEIGHT_BOLD)
                        .setColor(ColorBuilders.argb(BLUE)).build())
                .setModifiers(new ModifiersBuilders.Modifiers.Builder()
                        .setClickable(open?launch(c,action):load(action)).build())
                .setMaxLines(1).build();
    }
    static LayoutElementBuilders.Row controls(Context c, boolean skipped) {
        return new LayoutElementBuilders.Row.Builder()
                .setWidth(DimensionBuilders.wrap()).setHeight(DimensionBuilders.wrap())
                .addContent(button(skipped?"Enable":"Skip", "toggle-today",false,c))
                .addContent(new LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12)).build())
                .addContent(button("Sync","sync",false,c))
                .addContent(new LayoutElementBuilders.Spacer.Builder().setWidth(DimensionBuilders.dp(12)).build())
                .addContent(button("Open","open",true,c))
                .build();
    }
    static TileBuilders.Tile tile(LayoutElementBuilders.LayoutElement root, long freshnessMs) {
        return new TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(freshnessMs)
                .setTileTimeline(TimelineBuilders.Timeline.fromLayoutElement(root))
                .build();
    }
    public static TileBuilders.Tile next(Context context) {
        LocalDate today=LocalDate.now();
        ZonedDateTime now=ZonedDateTime.now();
        Map.Entry<LocalDate,Lessons.Lesson> upcoming=null;
        for (Map.Entry<LocalDate,Lessons.Lesson> item: Lessons.upcoming(context,9)) {
            if (item.getValue().at(item.getKey(),ZoneId.systemDefault()).isAfter(now)) {
                upcoming=item;break;
            }
        }
        boolean skipped=Lessons.isSkipped(context,today);
        LayoutElementBuilders.Column.Builder col=new LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.wrap()).setHeight(DimensionBuilders.wrap())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(line("LESSONBELL",12,BLUE,true)).addContent(space(6));
        if(upcoming==null){
            col.addContent(line("No upcoming lessons",14,WHITE,true));
            col.addContent(line("Check timetable",11,MUTED,false));
        } else {
            ZonedDateTime at=upcoming.getValue().at(upcoming.getKey(),ZoneId.systemDefault());
            long minutes=Math.max(1,(Duration.between(now,at).getSeconds()+59)/60);
            col.addContent(line(upcoming.getValue().group,18,WHITE,true));
            col.addContent(line(upcoming.getValue().start.format(TIME),25,WHITE,true));
            col.addContent(line(upcoming.getKey().format(DAY)+" · "+upcoming.getValue().room,11,MUTED,false));
            col.addContent(space(4));
            col.addContent(line(minutes<60?"In "+minutes+" min":"In "+(minutes/60)+"h "+(minutes%60)+"m",13,GREEN,true));
        }
        if(skipped){col.addContent(space(3));col.addContent(line("Today skipped",10,ORANGE,true));}
        col.addContent(space(11)).addContent(controls(context,skipped));
        return tile(col.build(),60_000L);
    }
    public static TileBuilders.Tile today(Context context) {
        LocalDate today=LocalDate.now();
        boolean skipped=Lessons.isSkipped(context,today);
        List<Lessons.Lesson> lessons=Lessons.forDate(context,today);
        LayoutElementBuilders.Column.Builder col=new LayoutElementBuilders.Column.Builder()
                .setWidth(DimensionBuilders.wrap()).setHeight(DimensionBuilders.wrap())
                .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
                .addContent(line("TODAY · "+today.format(DAY).toUpperCase(Locale.ENGLISH),11,BLUE,true))
                .addContent(space(5));
        if(skipped) col.addContent(line("Alerts disabled today",12,ORANGE,true));
        else if(lessons.isEmpty()) col.addContent(line("No lessons scheduled",13,WHITE,true));
        else {
            int count=0;
            for (Lessons.Lesson lesson:lessons) {
                if(count++>=4) break;
                col.addContent(line(lesson.start.format(TIME)+"  "+lesson.group+" "+lesson.period,12,WHITE,count==1));
                col.addContent(space(3));
            }
            if(lessons.size()>4)col.addContent(line("+"+(lessons.size()-4)+" more",10,MUTED,false));
        }
        col.addContent(space(9)).addContent(controls(context,skipped));
        return tile(col.build(),60_000L);
    }
}
