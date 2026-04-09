package com.routinely.app.receivers;

import android.app.*;
import android.content.*;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.routinely.app.R;
import com.routinely.app.RoutinelyApp;
import com.routinely.app.data.AppData;
import com.routinely.app.data.Models;
import com.routinely.app.ui.MainActivity;
import java.util.Calendar;

/**
 * BroadcastReceiver that fires routine reminder notifications.
 * Scheduled via AlarmManager when a routine is saved.
 * This mirrors the DailyLessonReceiver pattern which works correctly.
 *
 * FIX: Previously routine notifications never fired because no receiver existed
 * and no scheduling logic was called from EditRoutineActivity.
 */
public class RoutineNotificationReceiver extends BroadcastReceiver {

    private static final String EXTRA_ROUTINE_ID = "routineId";
    // Dedicated notification channel for routine reminders
    public static final String CH_ROUTINE = "routinely_routines";
    /**
     * Maps Calendar.DAY_OF_WEEK (1=Sun…7=Sat) to our repeatDays index (0=Mon…6=Sun).
     * Example: Calendar.SUNDAY=1 → index 6, Calendar.MONDAY=2 → index 0.
     */
    private static final int[] DOW_TO_INDEX = {6, 0, 1, 2, 3, 4, 5};

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int routineId = intent.getIntExtra(EXTRA_ROUTINE_ID, -1);
        AppData db = AppData.get(ctx);
        Models.Routine routine = db.findRoutine(routineId);
        if (routine == null || routine.archived) return;

        // Only fire if today is a repeat day
        Calendar now = Calendar.getInstance();
        int dow = now.get(Calendar.DAY_OF_WEEK); // 1=Sun … 7=Sat
        int idx = DOW_TO_INDEX[dow - 1];
        if (!routine.repeatDays[idx]) {
            // Not scheduled today — reschedule for next occurrence
            scheduleNext(ctx, routine);
            return;
        }

        // Build and post the notification
        Intent mainIntent = new Intent(ctx, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.putExtra("tab", 1); // Routines tab
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, routineId + 30000, mainIntent, piFlags);

        String title = routine.emoji + " " + routine.name + " — Starting now!";
        int totalMin = routine.getTotalMinutes();
        String body = "Your " + totalMin + "-minute routine is ready. " + routine.steps.size() + " steps. Let's go!";

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CH_ROUTINE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH);

        try { NotificationManagerCompat.from(ctx).notify(routineId + 30000, nb.build()); }
        catch (SecurityException ignored) {}

        // Reschedule for next day at the same time
        scheduleNext(ctx, routine);
    }

    /**
     * Schedule a routine notification at the routine's start time.
     * Call this after saving a routine.
     */
    public static void schedule(Context ctx, Models.Routine routine) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, routine.startHour);
        cal.set(Calendar.MINUTE, routine.startMinute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        // If the time has already passed today, schedule for tomorrow
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        PendingIntent pi = buildPI(ctx, routine.id);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    /** Cancel pending routine reminder alarm. */
    public static void cancel(Context ctx, int routineId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(buildPI(ctx, routineId));
    }

    private static void scheduleNext(Context ctx, Models.Routine routine) {
        // Reschedule for same time next day
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, routine.startHour);
        cal.set(Calendar.MINUTE, routine.startMinute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        PendingIntent pi = buildPI(ctx, routine.id);
        if (Build.VERSION.SDK_INT >= 23) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
        }
    }

    private static PendingIntent buildPI(Context ctx, int routineId) {
        Intent i = new Intent(ctx, RoutineNotificationReceiver.class);
        i.putExtra(EXTRA_ROUTINE_ID, routineId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(ctx, routineId + 30000, i, flags);
    }
}
