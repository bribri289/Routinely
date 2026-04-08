package com.routinely.app.receivers;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.routinely.app.R;
import com.routinely.app.RoutinelyApp;
import com.routinely.app.data.MindsetData;
import com.routinely.app.ui.MainActivity;
import java.util.Calendar;

public class DailyLessonReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        Calendar cal = Calendar.getInstance();
        int idx = (cal.get(Calendar.DAY_OF_YEAR) - 1) % MindsetData.DAILY_LESSONS.length;
        String title = "☀️ Daily Lesson: " + MindsetData.DAILY_LESSONS[idx][0];
        String body = MindsetData.DAILY_LESSONS[idx][1];
        String preview = (body != null && body.contains("\n")) ? body.substring(0, body.indexOf('\n')) : body;

        Intent mainIntent = new Intent(ctx, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.putExtra("tab", 4);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, 9001, mainIntent, piFlags);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, RoutinelyApp.CH_MINDSET)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(preview)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(preview))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try { NotificationManagerCompat.from(ctx).notify(9000, nb.build()); } catch (SecurityException ignored) {}

        // Reschedule for next day
        scheduleNext(ctx);
    }

    public static void schedule(Context ctx) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1);
        PendingIntent pi = buildPI(ctx);
        if (Build.VERSION.SDK_INT >= 23)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        else
            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
    }

    private static void scheduleNext(Context ctx) { schedule(ctx); }

    private static PendingIntent buildPI(Context ctx) {
        Intent i = new Intent(ctx, DailyLessonReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(ctx, 8888, i, flags);
    }
}
