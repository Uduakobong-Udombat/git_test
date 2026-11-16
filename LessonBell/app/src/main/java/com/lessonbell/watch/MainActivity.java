package com.lessonbell.watch;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Native watch-first UI. All controls work without a phone-installed app. */
public final class MainActivity extends Activity {
    private LinearLayout column;
    private final int WHITE=Color.rgb(239,247,252), MUTE=Color.rgb(164,187,203), CYAN=Color.rgb(113,211,252);
    private final int BG=Color.rgb(9,20,31), PANEL=Color.rgb(26,47,63);
    private static final int PERMISSIONS=2204;
    @Override public void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);display();}
    @Override protected void onResume(){super.onResume();Scheduler.schedule(this);if(column!=null)refresh();}
    private TextView text(String content,int size,int color,boolean bold) {
        TextView t=new TextView(this);t.setText(content);t.setTextColor(color);t.setTextSize(size);
        t.setGravity(Gravity.CENTER);t.setPadding(8,6,8,6);
        if(bold)t.setTypeface(null,1);return t;
    }
    private void add(String content,int size,int color,boolean bold){column.addView(text(content,size,color,bold));}
    private Button button(String content,Runnable action) {
        Button b=new Button(this);b.setText(content);b.setTextSize(12);b.setAllCaps(false);
        b.setTextColor(WHITE); b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(PANEL));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(6,3,6,3);
        column.addView(b,p);b.setOnClickListener(v->action.run());return b;
    }
    private void display(){
        getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        column=new LinearLayout(this);column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(24,23,24,45);column.setGravity(Gravity.CENTER_HORIZONTAL);
        scroll.addView(column);setContentView(scroll);refresh();
    }
    private void refresh() {
        if(column==null)return;
        column.removeAllViews();
        add("LESSONBELL",19,CYAN,true);
        add("Your timetable · watch only",11,MUTE,false);
        List<Map.Entry<LocalDate,Lessons.Lesson>> upcoming=Lessons.upcoming(this,9);
        ZonedDateTime now=ZonedDateTime.now();
        boolean seen=false;
        for(Map.Entry<LocalDate,Lessons.Lesson> e:upcoming) {
            ZonedDateTime start=e.getValue().at(e.getKey(),ZoneId.systemDefault());
            if(start.isAfter(now)) {
                long mins=Duration.between(now,start).toMinutes();
                add("NEXT CLASS",11,CYAN,true);
                add(e.getValue().shortTitle(),18,WHITE,true);
                add(e.getKey().format(DateTimeFormatter.ofPattern("EEE d MMM"))+"  "+e.getValue().start,13,WHITE,false);
                add(e.getValue().room+" · "+mins+" min away",12,MUTE,false);
                seen=true;break;
            }
        }
        if(!seen)add("No upcoming lessons in 9 days",12,MUTE,false);
        add(Scheduler.canSchedule(this)?"✓ Precise alarms enabled":"! Allow precise alarms below",11,
                Scheduler.canSchedule(this)?CYAN:Color.YELLOW,false);
        add("Alerts: 10, 5 and 2 min before every period",11,MUTE,false);
        button("Test strongest vibration",()->{Vibration.buzz(this,2);Toast.makeText(this,"5 strong pulses",Toast.LENGTH_SHORT).show();});
        if(!Scheduler.canSchedule(this))button("Allow precise alarms",()->{
            Intent intent=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(android.net.Uri.parse("package:"+getPackageName()));
            try{startActivity(intent);}catch(Exception e){Toast.makeText(this,"Find Alarms & reminders in Settings",Toast.LENGTH_LONG).show();}
        });
        if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            button("Allow notifications",()->requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},PERMISSIONS));
        add("NON-TEACHING DAYS",12,CYAN,true);
        LocalDate today=LocalDate.now(),tomorrow=today.plusDays(1);
        button((Lessons.isSkipped(this,today)?"Enable":"Skip")+" today",()->toggleDate(today));
        button((Lessons.isSkipped(this,tomorrow)?"Enable":"Skip")+" tomorrow",()->toggleDate(tomorrow));
        button("Choose a date to skip / enable",()->selectDate());
        button("Skip next 7 days",()->{
            for(int d=0;d<7;d++)Lessons.setSkipped(this,LocalDate.now().plusDays(d),true);
            Scheduler.schedule(this);refresh();
        });
        button("Enable next 7 days",()->{
            for(int d=0;d<7;d++)Lessons.setSkipped(this,LocalDate.now().plusDays(d),false);
            Scheduler.schedule(this);refresh();
        });
        add("CALENDAR",12,CYAN,true);
        boolean calendar=CalendarSync.hasPermission(this);
        add(calendar?"Watch calendar access granted":"Watch calendar access not granted",10,MUTE,false);
        button(calendar?"Sync watch calendar":"Allow calendar access",()->{
            if(!CalendarSync.hasPermission(this)) requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},PERMISSIONS);
            else {Toast.makeText(this,CalendarSync.sync(this),Toast.LENGTH_LONG).show();refresh();}
        });
        boolean auto=Lessons.prefs(this).getBoolean("auto_holidays",true);
        button("Auto-skip calendar holidays: "+(auto?"ON":"OFF"),()->{
            Lessons.prefs(this).edit().putBoolean("auto_holidays",!auto).apply();
            Scheduler.schedule(this);refresh();
        });
        add("TIMETABLE · COMING UP",12,CYAN,true);
        int count=0;
        for(Map.Entry<LocalDate,Lessons.Lesson> e:upcoming) {
            if(e.getValue().at(e.getKey(),ZoneId.systemDefault()).isBefore(now))continue;
            if(count++>=7)break;
            Lessons.Lesson l=e.getValue();
            add(e.getKey().format(DateTimeFormatter.ofPattern("EEE"))+"  "+l.start+"  "+l.shortTitle(),11,WHITE,false);
        }
        add("Offline timetable · Asia/Bangkok compatible",10,MUTE,false);
    }
    private void toggleDate(LocalDate date){Lessons.setSkipped(this,date,!Lessons.isSkipped(this,date));Scheduler.schedule(this);refresh();}
    private void selectDate(){
        LocalDate now=LocalDate.now();
        new DatePickerDialog(this,(picker,year,month,day)->{
            LocalDate date=LocalDate.of(year,month+1,day);
            boolean manually=Lessons.prefs(this).getStringSet("disabled_dates",Collections.emptySet()).contains(date.toString());
            Lessons.setSkipped(this,date,!manually);
            Scheduler.schedule(this);
            Toast.makeText(this,(manually?"Enabled ":"Skipped ")+date,Toast.LENGTH_SHORT).show();refresh();
        },now.getYear(),now.getMonthValue()-1,now.getDayOfMonth()).show();
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] grants){
        super.onRequestPermissionsResult(code,perms,grants);
        if(code==PERMISSIONS && CalendarSync.hasPermission(this)){
            Toast.makeText(this,CalendarSync.sync(this),Toast.LENGTH_LONG).show();
        }
        Scheduler.schedule(this);refresh();
    }
}
