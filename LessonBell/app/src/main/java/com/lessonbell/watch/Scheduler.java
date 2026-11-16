package com.lessonbell.watch;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import java.time.*;
import java.util.*;

public final class Scheduler {
    private Scheduler() {}
    public static final int[] LEADS={10,5,2};
    private static final int DAYS_AHEAD=15;
    public static boolean canSchedule(Context c) {
        AlarmManager manager=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        return manager != null && (Build.VERSION.SDK_INT<31 || manager.canScheduleExactAlarms());
    }
    private static PendingIntent intent(Context c, int code, LocalDate day, Lessons.Lesson l, int minutes) {
        Intent i=new Intent(c,AlertReceiver.class);
        i.setAction("com.lessonbell.watch.ALERT");
        i.putExtra("lesson",l.shortTitle());
        i.putExtra("details",l.description());
        i.putExtra("date",day.toString());
        i.putExtra("start",l.start.toString());
        i.putExtra("minutes",minutes);
        return PendingIntent.getBroadcast(c,code,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static int requestCode(LocalDate day, Lessons.Lesson l, int minutes) {
        return (day.toString()+"|"+l.id+"|"+minutes).hashCode();
    }
    public static synchronized void schedule(Context c) {
        AlarmManager a=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(a==null)return;
        LocalDate now=LocalDate.now();
        if(!canSchedule(c)) { Log.w("LessonBell","Exact alarm access not granted");return; }
        // Cancel previously registered days first so date exclusions and changed calendar events apply.
        // Persist exact alarm IDs because calendar events may disappear on refresh.
        Set<String> old=Lessons.prefs(c).getStringSet("scheduled",Collections.emptySet());
        for(String entry:old) {
            try {
                String[] bits=entry.split("\\|",4);
                Intent i=new Intent(c,AlertReceiver.class).setAction("com.lessonbell.watch.ALERT");
                PendingIntent pi=PendingIntent.getBroadcast(c,Integer.parseInt(bits[0]),i,
                        PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);
                if(pi!=null){a.cancel(pi);pi.cancel();}
            } catch(Exception ignored) { }
        }
        Set<String> current=new HashSet<>();
        long currentMillis=System.currentTimeMillis();
        ZoneId zone=ZoneId.systemDefault();
        for(int n=0;n<DAYS_AHEAD;n++) {
            LocalDate date=now.plusDays(n);
            for(Lessons.Lesson l:Lessons.forDate(c,date)) {
                for(int m:LEADS) {
                    ZonedDateTime when=l.at(date,zone).minusMinutes(m);
                    long millis=when.toInstant().toEpochMilli();
                    if(millis<=currentMillis+1000)continue;
                    int code=requestCode(date,l,m);
                    PendingIntent p=intent(c,code,date,l,m);
                    // AlarmClock is exact and allowed to wake in idle mode; it displays the next-alarm indicator.
                    PendingIntent show=PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),
                            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                    a.setAlarmClock(new AlarmManager.AlarmClockInfo(millis,show),p);
                    current.add(code+"|"+date+"|"+l.id+"|"+m);
                }
            }
        }
        Lessons.prefs(c).edit().putStringSet("scheduled",current).apply();
        Log.i("LessonBell","Scheduled "+current.size()+" alarms");
    }
}
