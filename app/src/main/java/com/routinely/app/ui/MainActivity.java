package com.routinely.app.ui;
import android.app.AlarmManager;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.*;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;
import com.routinely.app.data.AppData;
import com.routinely.app.data.Models;
import com.routinely.app.receivers.DailyLessonReceiver;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private int curTab = 0;
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        AppData.get(this);
        setupNav();
        switchTab(0);
        requestPerms();
        DailyLessonReceiver.schedule(this);
        handleIntent(getIntent());
    }
    @Override protected void onNewIntent(Intent i) { super.onNewIntent(i); handleIntent(i); }
    @Override protected void onResume() { super.onResume(); switchTab(curTab); }

    void handleIntent(Intent intent) {
        if (intent==null) return;
        int tab = intent.getIntExtra("tab", -1);
        if (tab >= 0 && tab <= 4) { switchTab(tab); return; }
        Uri d = intent.getData(); if (d==null) return;
        String rid = d.getQueryParameter("start"); if(rid==null) rid=d.getQueryParameter("routine");
        if (rid!=null) { try { int id=Integer.parseInt(rid); Models.Routine r=AppData.get(this).findRoutine(id); if(r!=null){Intent i=new Intent(this,RunRoutineActivity.class);i.putExtra("routine",r);startActivity(i);} } catch(Exception ignored){} }
    }

    void setupNav() {
        int[] ids = {R.id.nav_today,R.id.nav_routines,R.id.nav_habits,R.id.nav_alarm,R.id.nav_mindset};
        for (int i=0;i<ids.length;i++) { final int idx=i; findViewById(ids[i]).setOnClickListener(v->switchTab(idx)); }
    }

    public void switchTab(int idx) {
        curTab = idx;
        Fragment f;
        switch(idx) { 
            case 1:f=new RoutinesFragment();break; 
            case 2:f=new HabitsFragment();break; 
            case 3:f=new AlarmFragment();break; 
            case 4:{
                HabitsFragment hf=new HabitsFragment();
                android.os.Bundle args=new android.os.Bundle(); args.putInt("section",2); hf.setArguments(args);
                f=hf;break;
            }
            default:f=new TodayFragment();break; 
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,f).commit();
        int[] navIds={R.id.nav_today,R.id.nav_routines,R.id.nav_habits,R.id.nav_alarm,R.id.nav_mindset};
        int primary=getColor(R.color.primary); int muted=getColor(R.color.text_muted);
        for(int i=0;i<navIds.length;i++){
            LinearLayout nav=(LinearLayout)findViewById(navIds[i]);
            boolean active=i==idx;
            for(int c=0;c<nav.getChildCount();c++){View ch=nav.getChildAt(c);if(ch instanceof TextView)((TextView)ch).setTextColor(active?primary:muted);}
        }
    }

    void requestPerms() {
        List<String> p=new ArrayList<>();
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.POST_NOTIFICATIONS);
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.CAMERA);
        if(Build.VERSION.SDK_INT>=29&&ContextCompat.checkSelfPermission(this,Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED) p.add(Manifest.permission.ACTIVITY_RECOGNITION);
        if(!p.isEmpty()) ActivityCompat.requestPermissions(this,p.toArray(new String[0]),100);
        if(Build.VERSION.SDK_INT>=31){android.app.AlarmManager am=(android.app.AlarmManager)getSystemService(ALARM_SERVICE);if(am!=null&&!am.canScheduleExactAlarms())startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:"+getPackageName())));}
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);if(pm!=null&&!pm.isIgnoringBatteryOptimizations(getPackageName()))startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,Uri.parse("package:"+getPackageName())));
        if(Build.VERSION.SDK_INT>=23&&!Settings.canDrawOverlays(this))startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
    }
}
