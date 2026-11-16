package com.lessonbell.watch;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.*;
import java.util.*;

/** Built-in periods come from the supplied weekly timetable, not a guessed calendar. */
public final class Lessons {
    private Lessons() { }
    public static final class Lesson {
        public final String id, subject, group, room;
        public final DayOfWeek day;
        public final LocalTime start, end;
        public final LocalDate date; // null for weekly timetable; populated for calendar events
        public final String period;
        public Lesson(String id, DayOfWeek day, String period, String subject, String group, String room,
                      LocalTime start, LocalTime end, LocalDate date) {
            this.id=id;this.day=day;this.period=period;this.subject=subject;this.group=group;
            this.room=room;this.start=start;this.end=end;this.date=date;
        }
        public ZonedDateTime at(LocalDate d, ZoneId zone) { return d.atTime(start).atZone(zone); }
        public String shortTitle() { return group + " • " + period; }
        public String description() { return subject + " · " + room; }
    }
    private static Lesson p(DayOfWeek d, String period, String subj, String group, String room, String start, String end) {
        return new Lesson(d.name()+"-"+period, d,period,subj,group,room,LocalTime.parse(start),LocalTime.parse(end),null);
    }
    public static List<Lesson> weekly() {
        List<Lesson> v = new ArrayList<>();
        v.add(p(DayOfWeek.MONDAY,"P1","A Level CS-12-01","A Level","ICT04 (A204)","08:00","08:45"));
        v.add(p(DayOfWeek.MONDAY,"P2","A Level CS-12-01","A Level","ICT04 (A204)","08:50","09:35"));
        v.add(p(DayOfWeek.MONDAY,"P3","IGCSE CS-10B","Grade 10B","ICT06 (A206)","09:55","10:40"));
        v.add(p(DayOfWeek.MONDAY,"P4","IGCSE CS-10B","Grade 10B","ICT06 (A206)","10:45","11:30"));
        v.add(p(DayOfWeek.TUESDAY,"P1","IGCSE CS-9B-01","Grade 9B","ICT06 (A206)","08:00","08:45"));
        v.add(p(DayOfWeek.TUESDAY,"P2","IGCSE CS-9B-01","Grade 9B","ICT06 (A206)","08:50","09:35"));
        v.add(p(DayOfWeek.TUESDAY,"P6","A Level CS-12-01","A Level","ICT04 (A204)","13:35","14:20"));
        v.add(p(DayOfWeek.TUESDAY,"P7","A Level CS-12-01","A Level","ICT04 (A204)","14:40","15:25"));
        v.add(p(DayOfWeek.WEDNESDAY,"P1","A Level CS-12-01","A Level","ICT04 (A204)","08:00","08:45"));
        v.add(p(DayOfWeek.WEDNESDAY,"P2","A Level CS-12-01","A Level","ICT04 (A204)","08:50","09:35"));
        v.add(p(DayOfWeek.WEDNESDAY,"P3","A Level CS-12-01","A Level","ICT04 (A204)","09:55","10:40"));
        v.add(p(DayOfWeek.THURSDAY,"P4","IGCSE CS-10B","Grade 10B","ICT06 (A206)","10:45","11:30"));
        v.add(p(DayOfWeek.THURSDAY,"P5S","IGCSE CS-10B","Grade 10B","ICT06 (A206)","11:35","12:20"));
        v.add(p(DayOfWeek.THURSDAY,"P6","IGCSE CS-9B-01","Grade 9B","ICT06 (A206)","13:35","14:20"));
        v.add(p(DayOfWeek.THURSDAY,"P7","IGCSE CS-9B-01","Grade 9B","ICT06 (A206)","14:40","15:25"));
        return v;
    }
    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("lessonbell_v1",Context.MODE_PRIVATE);
    }
    public static boolean isSkipped(Context c, LocalDate date) {
        SharedPreferences sp=prefs(c);
        if(sp.getStringSet("forced_enabled_dates",Collections.emptySet()).contains(date.toString()))return false;
        return sp.getStringSet("disabled_dates",Collections.emptySet()).contains(date.toString()) ||
                (sp.getBoolean("auto_holidays",true) &&
                        sp.getStringSet("holiday_dates",Collections.emptySet()).contains(date.toString()));
    }
    public static void setSkipped(Context c, LocalDate date, boolean skip) {
        SharedPreferences sp=prefs(c);
        Set<String> dates=new HashSet<>(sp.getStringSet("disabled_dates",Collections.emptySet()));
        Set<String> forced=new HashSet<>(sp.getStringSet("forced_enabled_dates",Collections.emptySet()));
        if(skip) { dates.add(date.toString()); forced.remove(date.toString()); }
        else { dates.remove(date.toString()); forced.add(date.toString()); }
        sp.edit().putStringSet("disabled_dates",dates).putStringSet("forced_enabled_dates",forced).apply();
    }
    public static List<Lesson> calendar(Context c) {
        ArrayList<Lesson> v=new ArrayList<>();
        try {
            JSONArray a=new JSONArray(prefs(c).getString("calendar_json","[]"));
            for(int i=0;i<a.length();i++) {
                JSONObject x=a.getJSONObject(i);
                LocalDate date=LocalDate.parse(x.getString("date"));
                v.add(new Lesson("cal-"+x.getString("id"),date.getDayOfWeek(),"Calendar",x.getString("title"),
                        x.optString("group",x.getString("title")),x.optString("room","Calendar"),LocalTime.parse(x.getString("start")),
                        LocalTime.parse(x.getString("end")),date));
            }
        } catch(Exception ignored) { /* Damaged cache cannot disrupt offline lessons. */ }
        return v;
    }
    public static List<Lesson> forDate(Context c, LocalDate date) {
        List<Lesson> out=new ArrayList<>();
        if(isSkipped(c,date))return out;
        List<Lesson> matchingCalendar=new ArrayList<>();
        for(Lesson x:calendar(c))if(date.equals(x.date))matchingCalendar.add(x);
        Set<String> overrideGroups=new HashSet<>();
        // A calendar event that identifies a class replaces that class's preloaded periods for the day.
        // This avoids stale alerts when a class is moved or a double period is a single event.
        if(prefs(c).getBoolean("calendar_authoritative",true))
            for(Lesson x:matchingCalendar)if(!"Calendar".equals(x.group))overrideGroups.add(x.group);
        for(Lesson x:weekly()) if(x.day==date.getDayOfWeek()&&!overrideGroups.contains(x.group))out.add(x);
        for(Lesson x:matchingCalendar) {
            boolean duplicate=false;
            for(Lesson y:out) if(Math.abs(Duration.between(y.start,x.start).toMinutes())<=2) { duplicate=true;break; }
            if(!duplicate)out.add(x);
        }
        out.sort(Comparator.comparing(l->l.start));
        return out;
    }
    public static List<Map.Entry<LocalDate, Lesson>> upcoming(Context c, int days) {
        LocalDate today=LocalDate.now();
        List<Map.Entry<LocalDate, Lesson>> result=new ArrayList<>();
        for(int i=0;i<days;i++) {
            LocalDate date=today.plusDays(i);
            for(Lesson l:forDate(c,date))result.add(new AbstractMap.SimpleImmutableEntry<>(date,l));
        }
        return result;
    }
}
