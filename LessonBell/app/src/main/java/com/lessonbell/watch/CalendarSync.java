package com.lessonbell.watch;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Uses Google Calendar REST API v3 directly; all requests are authorized by watch-side Google Play Services. */
public final class CalendarSync {
    private CalendarSync() {}
    private static final String API="https://www.googleapis.com/calendar/v3/";
    private static final int MAX_CALENDARS=80,MAX_PAGES=8;
    public static boolean isConnected(Context c) {return Lessons.prefs(c).getBoolean("google_connected",false);}
    private static String q(String str) {return URLEncoder.encode(str,StandardCharsets.UTF_8);}
    private static JSONObject get(String url,String accessToken) throws IOException {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
        connection.setRequestProperty("Authorization","Bearer "+accessToken);
        connection.setRequestProperty("Accept","application/json");
        try {
            int code=connection.getResponseCode();
            if(code!=200)throw new IOException("Google Calendar HTTP "+code);
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=connection.getInputStream()){
                byte[] buffer=new byte[8192];int n;int bytes=0;
                while((n=in.read(buffer))>=0) {bytes+=n;if(bytes>2_000_000)throw new IOException("Calendar response too large");out.write(buffer,0,n);}
            }
            try {return new JSONObject(out.toString("UTF-8"));}
            catch(Exception e) {throw new IOException("Unexpected Calendar response",e);}
        }finally{connection.disconnect();}
    }
    public static boolean isHoliday(String title,String calendar,boolean allDay) {
        String t=(title==null?"":title).toLowerCase(Locale.ROOT);
        String cal=(calendar==null?"":calendar).toLowerCase(Locale.ROOT);
        return (allDay && (cal.contains("holiday")||cal.contains("holidays")||cal.contains("ngày lễ")||cal.contains("school closure"))) ||
                t.contains("public holiday")||t.contains("school holiday")||t.contains("school closed")||
                t.contains("no school")||t.contains("nghỉ lễ")||t.contains("nghỉ học")||t.contains("teacher training day");
    }
    public static boolean isLesson(String title) {
        String t=(title==null?"":title).toLowerCase(Locale.ROOT);
        return t.contains("computer science") || t.contains("igcse cs") || t.contains("a level cs") ||
                t.contains("cs-12") || t.contains("cs-10") || t.contains("cs-9") ||
                (t.contains("lesson") && (t.contains("grade")||t.contains("year")));
    }
    /** Returns a stable class identity used to replace stale default periods on a day when a matching calendar class exists. */
    public static String classGroup(String title) {
        String t=(title==null?"":title).toLowerCase(Locale.ROOT);
        if(t.contains("10b")||t.contains("cs-10"))return "Grade 10B";
        if(t.contains("9b")||t.contains("cs-9"))return "Grade 9B";
        if(t.contains("a level")||t.contains("12-01")||t.contains("cs-12"))return "A Level";
        return "Calendar";
    }
    private static JSONArray listCalendars(String token) throws IOException {
        JSONArray all=new JSONArray();String page="";
        for(int round=0;round<MAX_PAGES;round++) {
            JSONObject resp=get(API+"users/me/calendarList?maxResults=250&showHidden=false"+
                    (page.isEmpty()?"":"&pageToken="+q(page)),token);
            JSONArray items=resp.optJSONArray("items");
            if(items!=null)for(int i=0;i<items.length();i++) {
                JSONObject cal=items.optJSONObject(i);
                if(cal!=null && !"none".equals(cal.optString("accessRole")))all.put(cal);
                if(all.length()>MAX_CALENDARS)throw new IOException("Too many calendars; limit exceeded");
            }
            page=resp.optString("nextPageToken","");
            if(page.isEmpty())return all;
        }
        throw new IOException("Calendar list truncated; please retry");
    }
    private static void storeHolidayRange(Set<String> dates, LocalDate first,LocalDate endExclusive) {
        for(LocalDate day=first;day.isBefore(endExclusive)&&day.isBefore(first.plusDays(40));day=day.plusDays(1))dates.add(day.toString());
    }
    private static void processEvent(JSONObject event,String calendarId,String calendarName,
                                     JSONArray lessons,Set<String> holidays,Set<String> seen,ZoneId zone) {
        if("cancelled".equals(event.optString("status")))return;
        JSONObject start=event.optJSONObject("start"),end=event.optJSONObject("end");
        if(start==null||end==null)return;
        String title=event.optString("summary","");
        String startDate=start.optString("date","");
        boolean allDay=!startDate.isEmpty();
        try {
            if(isHoliday(title,calendarName,allDay)) {
                if(allDay) {
                    LocalDate first=LocalDate.parse(startDate);
                    LocalDate last=LocalDate.parse(end.optString("date",first.plusDays(1).toString()));
                    storeHolidayRange(holidays,first,last);
                } else {
                    LocalDate date=OffsetDateTime.parse(start.getString("dateTime")).atZoneSameInstant(zone).toLocalDate();
                    holidays.add(date.toString());
                }
            }
            if(allDay||!isLesson(title))return;
            ZonedDateTime a=OffsetDateTime.parse(start.getString("dateTime")).atZoneSameInstant(zone);
            ZonedDateTime b=OffsetDateTime.parse(end.getString("dateTime")).atZoneSameInstant(zone);
            if(!b.isAfter(a)||!a.toLocalDate().equals(b.toLocalDate()))return;
            String key=calendarId+":"+event.optString("id",title)+":"+a.toInstant().toEpochMilli();
            if(!seen.add(key))return;
            JSONObject row=new JSONObject();
            row.put("id",key);row.put("date",a.toLocalDate().toString());
            row.put("title",title);row.put("group",classGroup(title));
            row.put("room",event.optString("location","Calendar"));
            row.put("start",a.toLocalTime().withSecond(0).withNano(0).toString());
            row.put("end",b.toLocalTime().withSecond(0).withNano(0).toString());
            lessons.put(row);
        }catch(Exception ignored){ /* Malformed events cannot invalidate other calendar entries. */ }
    }
    /** Atomic update: on HTTP failure do not overwrite a previously successful offline cache. */
    public static String sync(Context context,String token) throws IOException {
        if(token==null||token.isEmpty())throw new IOException("Google authorization needed");
        JSONArray calendars=listCalendars(token);
        SharedPreferences prefs=Lessons.prefs(context);
        Set<String> selected=prefs.getStringSet("selected_google_calendars",Collections.emptySet());
        boolean selectionSet=prefs.getBoolean("google_calendar_selection_set",false);
        Set<String> initial=new HashSet<>();
        JSONArray lessonEvents=new JSONArray(); Set<String> holidayDates=new HashSet<>(),seen=new HashSet<>();
        ZoneId zone=ZoneId.systemDefault();
        String timeMin=OffsetDateTime.now(zone).minusHours(3).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String timeMax=ZonedDateTime.now(zone).plusDays(29).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        for(int i=0;i<calendars.length();i++) {
            JSONObject cal=calendars.optJSONObject(i);if(cal==null)continue;
            String id=cal.optString("id","");if(id.isEmpty())continue;
            initial.add(id);
            if(selectionSet&&!selected.contains(id))continue;
            String name=cal.optString("summaryOverride",cal.optString("summary","Calendar"));
            String page="";
            for(int round=0;round<MAX_PAGES;round++) {
                String url=API+"calendars/"+q(id)+"/events?maxResults=250&singleEvents=true&orderBy=startTime"+
                        "&timeMin="+q(timeMin)+"&timeMax="+q(timeMax)+
                        (page.isEmpty()?"":"&pageToken="+q(page));
                JSONObject resp=get(url,token);
                JSONArray events=resp.optJSONArray("items");
                if(events!=null)for(int j=0;j<events.length();j++) {
                    JSONObject e=events.optJSONObject(j);
                    if(e!=null)processEvent(e,id,name,lessonEvents,holidayDates,seen,zone);
                }
                page=resp.optString("nextPageToken","");
                if(page.isEmpty())break;
                if(round==MAX_PAGES-1)throw new IOException("Too many events; sync incomplete");
            }
        }
        SharedPreferences.Editor edit=prefs.edit()
                .putString("calendar_json",lessonEvents.toString())
                .putStringSet("holiday_dates",holidayDates)
                .putString("google_calendars_json",calendars.toString())
                .putLong("last_calendar_sync",System.currentTimeMillis())
                .putBoolean("google_connected",true)
                .putString("google_sync_status",lessonEvents.length()+" lessons, "+holidayDates.size()+" holidays synced");
        if(!selectionSet)edit.putStringSet("selected_google_calendars",initial).putBoolean("google_calendar_selection_set",true);
        edit.apply();
        Scheduler.schedule(context);
        GoogleCalendarAuth.enableBackground(context);
        return lessonEvents.length()+" lessons, "+holidayDates.size()+" holidays synced";
    }
}
