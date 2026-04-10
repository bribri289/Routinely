package com.routinely.app.ui;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import com.routinely.app.R;
import com.routinely.app.RoutinelyApp;
import com.routinely.app.data.*;
import com.routinely.app.services.AlarmService;
import java.util.*;

public class WakeCheckActivity extends AppCompatActivity {
    int alarmId; int checksLeft; AppData db; Models.Alarm alarm;
    boolean completed = false;
    Vibrator vibrator;
    static final int NOTIF_ID = 1002;

    static String[][] QUESTIONS={
        {"What day of the week is it?","Monday","Tuesday","Wednesday","Sunday","0+dayOfWeek"},
        {"What is 8 × 7?","54","56","58","52","1"},
        {"Which month comes after March?","May","February","April","June","2"},
        {"What is 15 − 9?","7","5","6","8","2"},
        {"How many days in a week?","5","6","7","8","2"},
        {"What is 12 ÷ 4?","2","3","4","6","1"},
        {"What comes after Tuesday?","Monday","Thursday","Wednesday","Friday","2"},
        {"What is 3 × 8?","24","28","18","32","0"},
        {"How many months in a year?","10","11","12","13","2"},
        {"What is 9 + 6?","13","14","15","16","2"},
        {"What is 100 ÷ 5?","15","20","25","10","1"},
        {"Which season comes after Winter?","Summer","Autumn","Spring","Winter","2"},
        {"What is 7 × 6?","40","42","44","46","1"},
        {"How many sides does a triangle have?","2","3","4","5","1"},
        {"What is 25 − 8?","15","16","17","18","2"},
        {"What is 4 × 9?","32","36","40","38","1"},
        {"How many hours in a day?","20","22","24","26","2"},
        {"What is 18 ÷ 3?","4","5","6","7","2"},
        {"What comes after Wednesday?","Tuesday","Thursday","Friday","Saturday","1"},
        {"What is 11 + 13?","22","23","24","25","2"}
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        setContentView(R.layout.activity_wake_check);
        db=AppData.get(this);
        alarmId=getIntent().getIntExtra("alarmId",-1);
        alarm=db.findAlarm(alarmId);
        checksLeft=alarm!=null?alarm.wakeCheckCount:1;
        // Intercept back press: treat as missed and restart alarm
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                handleMissedWakeCheck();
                finish();
            }
        });
        startVibration();
        showNotification();
        nextQuestion();
    }

    void startVibration() {
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1500, 300};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            else
                vibrator.vibrate(pattern, 0);
        }
    }

    void stopVibration() {
        if (vibrator != null) { vibrator.cancel(); vibrator = null; }
    }

    /** Called when the user dismisses the wake check without completing it. */
    void handleMissedWakeCheck() {
        stopVibration();
        cancelNotification();
        restartAlarm();
    }

    void showNotification() {
        Intent open = new Intent(this, WakeCheckActivity.class);
        open.putExtra("alarmId", alarmId);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT |
            (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(this, NOTIF_ID, open, piFlags);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this, RoutinelyApp.CH_ALARM)
            .setContentTitle("🧠 Wake-Up Check")
            .setContentText("Answer the question to confirm you're awake!")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, nb.build());
    }

    void cancelNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.cancel(NOTIF_ID);
    }

    void restartAlarm() {
        Intent si = new Intent(this, AlarmService.class);
        si.putExtra("alarmId", alarmId);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(si);
        else
            startService(si);
    }

    void nextQuestion(){
        if(checksLeft<=0){
            completed = true;
            stopVibration();
            cancelNotification();
            Toast.makeText(this,"Wake-up confirmed! Stay awake! 🌟",Toast.LENGTH_LONG).show();
            finish(); return;
        }
        String[] q=QUESTIONS[(int)(Math.random()*QUESTIONS.length)];
        ((TextView)findViewById(R.id.tv_wc_question)).setText(q[0]);
        ((TextView)findViewById(R.id.tv_wc_count)).setText("Check "+(alarm!=null?(alarm.wakeCheckCount-checksLeft+1):1)+" of "+(alarm!=null?alarm.wakeCheckCount:1));
        List<String> opts=Arrays.asList(q[1],q[2],q[3],q[4]);
        final int ansIdx=Integer.parseInt(q[5]);
        final String answer=opts.get(ansIdx);
        int[] btnIds={R.id.btn_opt1,R.id.btn_opt2,R.id.btn_opt3,R.id.btn_opt4};
        for(int i=0;i<4;i++){
            Button btn=findViewById(btnIds[i]); btn.setText(opts.get(i));
            final String optVal=opts.get(i);
            btn.setOnClickListener(v->{
                if(optVal.equals(answer)){
                    checksLeft--;
                    if(checksLeft>0){Toast.makeText(this,"Correct! One more...",Toast.LENGTH_SHORT).show();nextQuestion();}
                    else nextQuestion();
                } else {
                    ((TextView)findViewById(R.id.tv_wc_feedback)).setText("Wrong! Try again.");
                    ((TextView)findViewById(R.id.tv_wc_feedback)).setTextColor(0xFFEF4444);
                }
            });
        }
        ((TextView)findViewById(R.id.tv_wc_feedback)).setText("");
    }

    @Override protected void onDestroy() {
        if (!completed) {
            handleMissedWakeCheck();
        }
        super.onDestroy();
    }
}

