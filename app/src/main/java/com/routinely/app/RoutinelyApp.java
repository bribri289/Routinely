package com.routinely.app;
import android.app.*;
import android.os.Build;
public class RoutinelyApp extends Application {
    public static final String CH_ALARM="routinely_alarms", CH_HABIT="routinely_habits";
    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            NotificationChannel ac = new NotificationChannel(CH_ALARM,"Routinely Alarms",NotificationManager.IMPORTANCE_HIGH);
            ac.setBypassDnd(true); ac.enableVibration(true); ac.setShowBadge(true);
            nm.createNotificationChannel(ac);
            nm.createNotificationChannel(new NotificationChannel(CH_HABIT,"Habit Reminders",NotificationManager.IMPORTANCE_DEFAULT));
        }
    }
}
