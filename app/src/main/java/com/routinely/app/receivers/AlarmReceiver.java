package com.routinely.app.receivers;
import java.util.Calendar;
import android.app.*;
import android.content.*;
import android.os.Build;
import com.routinely.app.data.*;
import com.routinely.app.services.AlarmService;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent intent){
        String action=intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action)){
            AppData db=AppData.get(ctx);
            for(Models.Alarm a:db.alarms) if(a.enabled) schedule(ctx,a);
            // FIX: Reschedule habit and routine notifications on boot (were never rescheduled before)
            for(Models.Habit h:db.habits) if(h.reminderEnabled)
                com.routinely.app.receivers.HabitNotificationReceiver.schedule(ctx,h);
            for(Models.Routine r:db.routines) if(!r.archived)
                com.routinely.app.receivers.RoutineNotificationReceiver.schedule(ctx,r);
            return;
        }
        int alarmId=intent.getIntExtra("alarmId",-1);
        Intent si=new Intent(ctx,AlarmService.class); si.putExtra("alarmId",alarmId);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O) ctx.startForegroundService(si);
        else ctx.startService(si);
    }

    public static void schedule(Context ctx, Models.Alarm alarm){
        AlarmManager am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(am==null) return;
        java.util.Calendar cal=java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY,alarm.hour);
        cal.set(java.util.Calendar.MINUTE,alarm.minute);
        cal.set(java.util.Calendar.SECOND,0);
        if(cal.getTimeInMillis()<=System.currentTimeMillis()) cal.add(java.util.Calendar.DAY_OF_YEAR,1);
        PendingIntent pi=buildPI(ctx,alarm.id);
        if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
        else if(Build.VERSION.SDK_INT>=23)
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
        else
            am.setExact(AlarmManager.RTC_WAKEUP,cal.getTimeInMillis(),pi);
    }

    public static void cancel(Context ctx, int alarmId){
        AlarmManager am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(am!=null) am.cancel(buildPI(ctx,alarmId));
    }

    public static void snooze(Context ctx, Models.Alarm alarm, int minutes){
        AlarmManager am=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        if(am==null) return;
        long fireAt=System.currentTimeMillis()+(minutes*60*1000L);
        PendingIntent pi=buildPI(ctx,alarm.id);
        if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,fireAt,pi);
        else am.setExact(AlarmManager.RTC_WAKEUP,fireAt,pi);
    }

    private static PendingIntent buildPI(Context ctx, int id){
        Intent i=new Intent(ctx,AlarmReceiver.class); i.putExtra("alarmId",id);
        int flags=PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0);
        return PendingIntent.getBroadcast(ctx,id,i,flags);
    }
}
