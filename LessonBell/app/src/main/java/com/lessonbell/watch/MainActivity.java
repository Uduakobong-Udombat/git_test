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
import org.json.JSONArray;
import org.json.JSONObject;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.common.api.ApiException;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Native watch-first UI. All controls work without a phone-installed app. */
public final class MainActivity extends Activity {
    private LinearLayout column;
    private final int WHITE=Color.rgb(239,247,252), MUTE=Color.rgb(164,187,203), CYAN=Color.rgb(113,211,252);
    private final int BG=Color.rgb(9,20,31), PANEL=Color.rgb(26,47,63);
    private static final int PERMISSIONS=2204;
    private static final int GOOGLE_CONSENT=2205;
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
        add("GOOGLE CALENDAR",12,CYAN,true);
        boolean connected=CalendarSync.isConnected(this);
        add(connected?"Google authorization completed":"Not connected · your offline timetable works",10,
                connected?CYAN:MUTE,false);
        long last=Lessons.prefs(this).getLong("last_calendar_sync",0);
        if(last>0)add("Last sync: "+java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT,
                java.text.DateFormat.SHORT).format(new Date(last)),10,MUTE,false);
        add(Lessons.prefs(this).getString("google_sync_status","Use Connect to grant read-only access"),10,MUTE,false);
        button(connected?"Sync Google now":"Connect Google Calendar",()->connectGoogle());
        if(connected){
            button("Choose Google calendars",()->selectCalendars());
            boolean authoritative=Lessons.prefs(this).getBoolean("calendar_authoritative",true);
            button("Calendar replaces matching classes: "+(authoritative?"ON":"OFF"),()->{
                Lessons.prefs(this).edit().putBoolean("calendar_authoritative",!authoritative).apply();
                Scheduler.schedule(this);refresh();
            });
        }
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
    private void connectGoogle() {
        Toast.makeText(this,"Requesting Google Calendar permission…",Toast.LENGTH_SHORT).show();
        Identity.getAuthorizationClient(this).authorize(GoogleCalendarAuth.request())
                .addOnSuccessListener(result->{
                    if(result.hasResolution()){
                        try {startIntentSenderForResult(result.getPendingIntent().getIntentSender(),
                                GOOGLE_CONSENT,null,0,0,0);}
                        catch(Exception e){showCalendarStatus("Cannot open Google consent: "+e.getClass().getSimpleName());}
                    } else {syncToken(result.getAccessToken());}
                })
                .addOnFailureListener(e->showCalendarStatus("Google authorization unavailable: "+e.getClass().getSimpleName()));
    }
    private void syncToken(String token) {
        if(token==null||token.isEmpty()) {showCalendarStatus("Google did not supply an access token");return;}
        Lessons.prefs(this).edit().putBoolean("google_connected",true).apply();
        GoogleCalendarAuth.enableBackground(this);
        showCalendarStatus("Downloading Google calendars…");
        new Thread(()->{
            try {
                String status=CalendarSync.sync(getApplicationContext(),token);
                runOnUiThread(()->showCalendarStatus(status));
            }catch(Exception e){
                String error="Google sync failed: "+e.getMessage();
                Lessons.prefs(this).edit().putString("google_sync_status",error).apply();
                runOnUiThread(()->showCalendarStatus(error));
            }
        },"lessonbell-foreground-sync").start();
    }
    private void showCalendarStatus(String s) {
        Toast.makeText(this,s,Toast.LENGTH_LONG).show();
        if(!isFinishing())refresh();
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=GOOGLE_CONSENT)return;
        if(resultCode!=RESULT_OK || data==null){showCalendarStatus("Google authorization cancelled");return;}
        try {
            AuthorizationResult auth=Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data);
            syncToken(auth.getAccessToken());
        }catch(ApiException e){showCalendarStatus("Google authorization failed ("+e.getStatusCode()+")");}
    }
    private void selectCalendars(){
        try {
            JSONArray array=new JSONArray(Lessons.prefs(this).getString("google_calendars_json","[]"));
            if(array.length()==0){showCalendarStatus("Connect and sync first");return;}
            List<String> ids=new ArrayList<>();List<String> names=new ArrayList<>();
            for(int i=0;i<array.length();i++) {
                JSONObject item=array.optJSONObject(i);if(item==null)continue;
                ids.add(item.optString("id"));
                names.add(item.optString("summaryOverride",item.optString("summary","Calendar")));
            }
            Set<String> enabled=new HashSet<>(Lessons.prefs(this).getStringSet("selected_google_calendars",Collections.emptySet()));
            boolean[] checked=new boolean[ids.size()];
            for(int i=0;i<ids.size();i++)checked[i]=enabled.contains(ids.get(i));
            new AlertDialog.Builder(this).setTitle("Google calendars to read")
                    .setMultiChoiceItems(names.toArray(new String[0]),checked,(dialog,which,on)->checked[which]=on)
                    .setPositiveButton("Save",(dialog,which)->{
                        Set<String> selection=new HashSet<>();
                        for(int i=0;i<ids.size();i++)if(checked[i])selection.add(ids.get(i));
                        Lessons.prefs(this).edit().putStringSet("selected_google_calendars",selection)
                                .putBoolean("google_calendar_selection_set",true).apply();
                        connectGoogle();
                    }).setNegativeButton("Cancel",null).show();
        }catch(Exception e){showCalendarStatus("Could not display calendars");}
    }
    @Override public void onRequestPermissionsResult(int code,String[] perms,int[] grants){
        super.onRequestPermissionsResult(code,perms,grants);
        Scheduler.schedule(this);refresh();
    }
}
