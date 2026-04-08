package com.routinely.app;
import android.app.*;
import android.os.Build;
import androidx.appcompat.app.AppCompatDelegate;
public class RoutinelyApp extends Application {
    public static final String CH_ALARM="routinely_alarms", CH_HABIT="routinely_habits", CH_MINDSET="routinely_mindset";
    @Override public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel ac = new NotificationChannel(CH_ALARM,"Routinely Alarms",NotificationManager.IMPORTANCE_HIGH);
            ac.setBypassDnd(true); ac.enableVibration(true); ac.setShowBadge(true);
            nm.createNotificationChannel(ac);
            nm.createNotificationChannel(new NotificationChannel(CH_HABIT,"Habit Reminders",NotificationManager.IMPORTANCE_DEFAULT));
            nm.createNotificationChannel(new NotificationChannel(CH_MINDSET,"Daily Lessons",NotificationManager.IMPORTANCE_DEFAULT));
        }
    }
}
