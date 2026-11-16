package com.lessonbell.watch;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CalendarContract;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.*;
import java.util.*;

/** Optional on-device CalendarContract import. No phone companion or API key. */
public final class CalendarSync {
    private CalendarSync() {}
    public static boolean hasPermission(Context c) {
        return c.checkSelfPermission(Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED;
    }
    private static boolean isHoliday(String title,String calendar,boolean allDay) {
        String t=(title==null?"":title).toLowerCase(Locale.ROOT);
        String cal=(calendar==null?"":calendar).toLowerCase(Locale.ROOT);
        return (allDay && (cal.contains("holiday")||cal.contains("holidays")||cal.contains("ngày lễ") ||
                cal.contains("school closure"))) ||
                t.contains("public holiday")||t.contains("school holiday")||t.contains("school closed")||
                t.contains("no school")||t.contains("nghỉ lễ")||t.contains("nghỉ học");
    }
    private static boolean isLesson(String title) {
        String t=(title==null?"":title).toLowerCase(Locale.ROOT);
        return t.contains("computer science") || t.contains("igcse cs") || t.contains("a level cs") ||
                t.contains("cs-12") || t.contains("cs-10") || t.contains("cs-9") ||
                (t.contains("lesson") && (t.contains("grade")||t.contains("year")));
    }
    public static String sync(Context c) {
        if(!hasPermission(c))return "Calendar permission needed";
        long begin=System.currentTimeMillis()-3600000L;
        long end=begin+28L*24*3600000L;
        String[] cols={CalendarContract.Instances.EVENT_ID,CalendarContract.Instances.TITLE,
                CalendarContract.Instances.EVENT_LOCATION,CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.CALENDAR_DISPLAY_NAME};
        JSONArray lessons=new JSONArray();
        Set<String> holidays=new HashSet<>(),seen=new HashSet<>();
        int imports=0;
        try(Cursor cursor=CalendarContract.Instances.query(c.getContentResolver(),cols,begin,end)) {
            if(cursor==null)return "No watch calendar provider";
            ZoneId zone=ZoneId.systemDefault();
            while(cursor.moveToNext()) {
                String id=cursor.getString(0),title=cursor.getString(1),location=cursor.getString(2);
                long start=cursor.getLong(3),finish=cursor.getLong(4);
                boolean allDay=cursor.getInt(5)!=0;
                String calendar=cursor.getString(6);
                if(isHoliday(title,calendar,allDay)) {
                    LocalDate date=allDay?Instant.ofEpochMilli(start).atZone(ZoneOffset.UTC).toLocalDate()
                            :Instant.ofEpochMilli(start).atZone(zone).toLocalDate();
                    holidays.add(date.toString());
                }
                if(!allDay && finish>start && isLesson(title)) {
                    ZonedDateTime at=Instant.ofEpochMilli(start).atZone(zone);
                    String key=id+"-"+start;
                    if(!seen.add(key))continue;
                    JSONObject row=new JSONObject();
                    row.put("id",key);row.put("date",at.toLocalDate().toString());
                    row.put("title",title==null?"Calendar lesson":title);
                    row.put("room",location==null?"Calendar":location);
                    row.put("start",at.toLocalTime().withSecond(0).withNano(0).toString());
                    row.put("end",Instant.ofEpochMilli(finish).atZone(zone).toLocalTime().withSecond(0).withNano(0).toString());
                    lessons.put(row);imports++;
                }
            }
            Lessons.prefs(c).edit().putString("calendar_json",lessons.toString())
                    .putStringSet("holiday_dates",holidays)
                    .putLong("last_calendar_sync",System.currentTimeMillis()).apply();
            Scheduler.schedule(c);
            return imports+" lesson events, "+holidays.size()+" holidays imported";
        }catch(Exception e) {
            Log.w("LessonBell","Calendar provider unavailable",e);
            return "Calendar not available on this watch";
        }
    }
}
