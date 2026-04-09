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
 * BroadcastReceiver that fires habit reminder notifications.
 * Scheduled via AlarmManager when a habit is saved with reminders enabled.
 * This mirrors the DailyLessonReceiver pattern which works correctly.
 *
 * FIX: Previously habit notifications never fired because no receiver existed
 * and no scheduling logic was called from EditHabitActivity.
 */
public class HabitNotificationReceiver extends BroadcastReceiver {

    private static final String EXTRA_HABIT_ID = "habitId";

    @Override
    public void onReceive(Context ctx, Intent intent) {
        int habitId = intent.getIntExtra(EXTRA_HABIT_ID, -1);
        AppData db = AppData.get(ctx);
        Models.Habit habit = db.findHabit(habitId);
        if (habit == null || !habit.reminderEnabled) return;

        // Build and post the notification
        Intent mainIntent = new Intent(ctx, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mainIntent.putExtra("tab", 2); // Habits tab
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(ctx, habitId + 20000, mainIntent, piFlags);

        String title = habit.emoji + " Time for: " + habit.name;
        String body = "Stay consistent — your streak is at " + habit.streak + " days. Keep it going!";

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, RoutinelyApp.CH_HABIT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try { NotificationManagerCompat.from(ctx).notify(habitId + 20000, nb.build()); }
        catch (SecurityException ignored) {}

        // Reschedule for next occurrence
        scheduleNext(ctx, habit);
    }

    /**
     * Schedule a habit reminder. Call this after saving a habit with reminder enabled.
     * Uses AlarmManager with setExactAndAllowWhileIdle so it fires reliably.
     * Each reminder time gets a unique PendingIntent request code to avoid overwriting.
     */
    public static void schedule(Context ctx, Models.Habit habit) {
        if (!habit.reminderEnabled) return;
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        java.util.List<int[]> times = habit.getEffectiveReminderTimes();
        for (int t = 0; t < times.size(); t++) {
            int[] time = times.get(t);
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, time[0]);
            cal.set(Calendar.MINUTE, time[1]);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            // Use unique request code per reminder time index to prevent overwrites
            PendingIntent pi = buildPI(ctx, habit.id, t);
            if (Build.VERSION.SDK_INT >= 23) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }

    /** Cancel all pending reminder alarms for a habit (up to a reasonable maximum). */
    public static void cancel(Context ctx, int habitId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        // Cancel alarms for reminder-time indices 0–9 (covers all practical reminder counts)
        for (int t = 0; t < 10; t++) {
            am.cancel(buildPI(ctx, habitId, t));
        }
    }

    private static void scheduleNext(Context ctx, Models.Habit habit) {
        schedule(ctx, habit);
    }

    /**
     * Build a PendingIntent unique per habit + reminder time index.
     * Request code = habitId * 10 + timeIndex + 10000 to avoid collisions.
     */
    private static PendingIntent buildPI(Context ctx, int habitId, int timeIndex) {
        Intent i = new Intent(ctx, HabitNotificationReceiver.class);
        i.putExtra(EXTRA_HABIT_ID, habitId);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        return PendingIntent.getBroadcast(ctx, habitId * 10 + timeIndex + 10000, i, flags);
    }
}
