package com.routinely.app.ui;
import android.content.Intent;
import android.net.Uri;
import android.os.*;
import java.util.Calendar;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import com.routinely.app.services.AlarmService;

public class AlarmRingActivity extends AppCompatActivity {
    int alarmId; AppData db; Models.Alarm alarm;
    Handler tickHandler=new Handler(Looper.getMainLooper());
    Runnable tickRunnable;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );
        setContentView(R.layout.activity_alarm_ring);
        db=AppData.get(this);
        alarmId=getIntent().getIntExtra("alarmId",-1);
        alarm=db.findAlarm(alarmId);
        if(alarm==null){finish();return;}
        setupWallpaper();
        setupClock();
        setupMissionChips();
        setupButtons();
    }

    void setupWallpaper(){
        ImageView img=findViewById(R.id.alarm_wallpaper);
        VideoView vid=findViewById(R.id.alarm_wallpaper_video);
        if(alarm.wallpaperUri!=null&&!alarm.wallpaperUri.isEmpty()){
            Uri uri=Uri.parse(alarm.wallpaperUri);
            if(alarm.wallpaperIsVideo){
                vid.setVisibility(View.VISIBLE); img.setVisibility(View.GONE);
                vid.setVideoURI(uri);
                vid.setOnPreparedListener(mp->{mp.setLooping(true);vid.start();});
            } else {
                img.setVisibility(View.VISIBLE); vid.setVisibility(View.GONE);
                img.setImageURI(uri);
            }
        }
    }

    void setupClock(){
        updateClock();
        ((TextView)findViewById(R.id.tv_alarm_label)).setText(alarm.label);
        tickRunnable=new Runnable(){public void run(){updateClock();tickHandler.postDelayed(this,30000);}};
        tickHandler.post(tickRunnable);
    }

    void updateClock(){
        java.util.Calendar cal=java.util.Calendar.getInstance();
        int h=cal.get(java.util.Calendar.HOUR_OF_DAY);
        int m=cal.get(java.util.Calendar.MINUTE);
        String ap=h<12?"AM":"PM"; int h12=h%12; if(h12==0)h12=12;
        ((TextView)findViewById(R.id.tv_alarm_time)).setText(String.format("%d:%02d",h12,m));
        ((TextView)findViewById(R.id.tv_alarm_ampm)).setText(ap);
        String[] days={"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        ((TextView)findViewById(R.id.tv_alarm_date)).setText(
            days[cal.get(java.util.Calendar.DAY_OF_WEEK)-1]+", "+
            months[cal.get(java.util.Calendar.MONTH)]+" "+
            cal.get(java.util.Calendar.DATE));
    }

    void setupMissionChips(){
        LinearLayout chipsRow=findViewById(R.id.mission_chips_row);
        chipsRow.removeAllViews();
        if(alarm.missions.isEmpty()){
            TextView tv=new TextView(this);
            tv.setText("Tap DISMISS to wake up");
            tv.setTextColor(0xCCFFFFFF); tv.setTextSize(14);
            tv.setPadding(0,8,0,8);
            chipsRow.addView(tv);
        } else {
            for(Models.Mission m:alarm.missions){
                LinearLayout chip=new LinearLayout(this);
                chip.setOrientation(LinearLayout.HORIZONTAL);
                chip.setBackground(getDrawable(R.drawable.card_bg2));
                chip.setPadding(20,12,20,12);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0,0,10,0); chip.setLayoutParams(lp);
                TextView em=new TextView(this); em.setText(m.getEmoji()); em.setTextSize(18);
                chip.addView(em);
                TextView nm=new TextView(this); nm.setText("  "+m.getDisplayName());
                nm.setTextColor(0xEEFFFFFF); nm.setTextSize(13);
                chip.addView(nm);
                chipsRow.addView(chip);
            }
        }
        // Linked routine
        LinearLayout llRoutine=findViewById(R.id.ll_linked_routine);
        if(alarm.linkedRoutineId!=0){
            Models.Routine r=db.findRoutine(alarm.linkedRoutineId);
            if(r!=null){
                ((TextView)findViewById(R.id.tv_linked_routine)).setText(r.emoji+" "+r.name);
                llRoutine.setVisibility(View.VISIBLE);
            }
        }
    }

    void setupButtons(){
        // Dismiss
        findViewById(R.id.btn_dismiss).setOnClickListener(v->{
            stopAlarmService();
            if(!alarm.missions.isEmpty()){
                Intent i=new Intent(this,MissionActivity.class);
                i.putExtra("missions",(java.io.Serializable)alarm.missions);
                i.putExtra("alarmId",alarmId);
                i.putExtra("preview",false);
                startActivity(i);
            } else {
                onMissionsComplete();
            }
            finish();
        });

        // Snooze
        Button btnSnooze=findViewById(R.id.btn_snooze);
        if(alarm.preventSnooze){
            btnSnooze.setVisibility(View.GONE);
        } else {
            int remaining=alarm.maxSnoozes-alarm.snoozeCount;
            btnSnooze.setText("Snooze 5 min ("+remaining+" left)");
            btnSnooze.setEnabled(remaining>0);
            if(remaining<=0) btnSnooze.setAlpha(0.4f);
            btnSnooze.setOnClickListener(v->{
                alarm.snoozeCount++; db.save();
                com.routinely.app.receivers.AlarmReceiver.snooze(this,alarm,5);
                stopAlarmService(); finish();
                Toast.makeText(this,"Snoozed — alarm in 5 minutes",Toast.LENGTH_SHORT).show();
            });
        }
    }

    void stopAlarmService(){
        stopService(new Intent(this,AlarmService.class));
    }

    void onMissionsComplete(){
        if(alarm.linkedRoutineId!=0){
            Models.Routine r=db.findRoutine(alarm.linkedRoutineId);
            if(r!=null){
                Intent i=new Intent(this,RunRoutineActivity.class);
                i.putExtra("routine",r); startActivity(i);
            }
        }
        if(alarm.wakeCheckEnabled){
            new Handler().postDelayed(()->{
                Intent i=new Intent(this,WakeCheckActivity.class);
                i.putExtra("alarmId",alarmId); startActivity(i);
            },alarm.wakeCheckDelay*60*1000L);
        }
        alarm.snoozeCount=0; db.save();
    }

    @Override protected void onDestroy(){
        tickHandler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }
    @Override public void onBackPressed(){} // Block back button like Alarmy
}
