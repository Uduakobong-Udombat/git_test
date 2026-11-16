package com.lessonbell.watch;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.graphics.Color;

public class AlertReceiver extends BroadcastReceiver {
    static final String CHANNEL="lesson_urgent_v1";
    @Override public void onReceive(Context c, Intent i) {
        if(!"com.lessonbell.watch.ALERT".equals(i.getAction()))return;
        String date=i.getStringExtra("date");
        if(date==null)return;
        try { if(Lessons.isSkipped(c,java.time.LocalDate.parse(date)))return; }
        catch(Exception e) { return; }
        int minutes=i.getIntExtra("minutes",5);
        String title=i.getStringExtra("lesson");
        String details=i.getStringExtra("details");
        Vibration.buzz(c,minutes);
        NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm!=null) {
            NotificationChannel ch=new NotificationChannel(CHANNEL,"Lesson reminders",NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("Strong watch lesson alerts");
            // Vibration is driven explicitly to avoid double-vibration from the channel.
            ch.enableVibration(false); ch.setSound(null,null);ch.enableLights(true);ch.setLightColor(Color.CYAN);
            nm.createNotificationChannel(ch);
            Intent open=new Intent(c,MainActivity.class);
            PendingIntent content=PendingIntent.getActivity(c,0,open,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification n=new Notification.Builder(c,CHANNEL)
                    .setSmallIcon(com.lessonbell.watch.R.drawable.ic_app)
                    .setContentTitle((title==null?"Lesson":title)+" in "+minutes+" min")
                    .setContentText(details==null?"Upcoming lesson":details)
                    .setCategory(Notification.CATEGORY_REMINDER)
                    .setContentIntent(content).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PUBLIC)
                    .build();
            try { nm.notify((date+title+minutes).hashCode(),n); } catch(SecurityException ignored) { }
        }
        // Replenish future reminders after alarms fire, even if app isn't launched for weeks.
        Scheduler.schedule(c);
    }
}
