package com.routinely.app.ui;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;

public class RunRoutineActivity extends AppCompatActivity {
    Models.Routine routine; AppData db;
    int curStep=0; int secLeft=0; int secTotal=0;
    boolean paused=false; CountDownTimer timer;
    ProgressBar progressBar;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_run_routine);
        db=AppData.get(this);
        routine=(Models.Routine)getIntent().getSerializableExtra("routine");
        if(routine==null||routine.steps.isEmpty()){finish();return;}
        progressBar=findViewById(R.id.progress_bar);
        findViewById(R.id.btn_back).setOnClickListener(v->showExitDialog());
        startStep(0);
    }

    void startStep(int idx){
        curStep=idx;
        if(idx>=routine.steps.size()){onRoutineComplete();return;}
        Models.RoutineStep step=routine.steps.get(idx);
        // Header
        ((TextView)findViewById(R.id.tv_step_counter)).setText("Step "+(idx+1)+" of "+routine.steps.size());
        ((TextView)findViewById(R.id.tv_routine_name)).setText(routine.name);
        // Progress bar
        progressBar.setMax(routine.steps.size());
        progressBar.setProgress(idx+1);
        // Content
        ((TextView)findViewById(R.id.tv_step_emoji)).setText(step.emoji);
        ((TextView)findViewById(R.id.tv_step_name)).setText(step.name);
        ((TextView)findViewById(R.id.tv_step_description)).setText(step.description);
        // Timer
        secTotal=step.durationSeconds>0?step.durationSeconds:(step.durationMinutes>0?step.durationMinutes*60:300); secLeft=secTotal; paused=false;
        updateTimer();
        startTimer();
        // Linked habit
        View habPanel=findViewById(R.id.habit_panel);
        if(step.linkedHabitId!=0){
            Models.Habit h=db.findHabit(step.linkedHabitId);
            if(h!=null){habPanel.setVisibility(View.VISIBLE);((TextView)habPanel.findViewById(R.id.tv_habit_name)).setText(h.emoji+" "+h.name);}
            else habPanel.setVisibility(View.GONE);
        } else habPanel.setVisibility(View.GONE);
        // Buttons
        ((Button)findViewById(R.id.btn_complete)).setText("✓ Complete Step");
        findViewById(R.id.btn_complete).setOnClickListener(v->completeStep());
        findViewById(R.id.btn_skip).setOnClickListener(v->skipStep());
        Button btnPause=findViewById(R.id.btn_pause);
        btnPause.setText("⏸ Pause"); btnPause.setOnClickListener(v->togglePause());
    }

    void startTimer(){
        if(timer!=null)timer.cancel();
        timer=new CountDownTimer(secLeft*1000L,1000){
            public void onTick(long ms){secLeft=(int)(ms/1000);updateTimer();}
            public void onFinish(){secLeft=0;updateTimer();completeStep();}
        }.start();
    }

    void updateTimer(){
        int m=secLeft/60, s=secLeft%60;
        ((TextView)findViewById(R.id.tv_timer)).setText(String.format("%d:%02d",m,s));
        // Ring progress
        int prog=secTotal>0?(int)((1f-(float)secLeft/secTotal)*100):0;
        ProgressBar ring=findViewById(R.id.timer_ring); ring.setProgress(prog);
    }

    void togglePause(){
        paused=!paused;
        Button btn=findViewById(R.id.btn_pause);
        if(paused){if(timer!=null)timer.cancel();btn.setText("▶ Resume");}
        else{startTimer();btn.setText("⏸ Pause");}
    }

    void completeStep(){
        if(timer!=null)timer.cancel();
        // Mark habit done
        Models.RoutineStep step=routine.steps.get(curStep);
        if(step.linkedHabitId!=0){Models.Habit h=db.findHabit(step.linkedHabitId);if(h!=null&&!h.completedToday){h.completedToday=true;h.streak++;db.save();}}
        startStep(curStep+1);
    }

    void skipStep(){if(timer!=null)timer.cancel();startStep(curStep+1);}

    void onRoutineComplete(){
        if(timer!=null)timer.cancel();
        db.logActivity(routine.name,true);
        setContentView(R.layout.activity_routine_complete);
        ((TextView)findViewById(R.id.tv_complete_emoji)).setText(routine.emoji);
        ((TextView)findViewById(R.id.tv_complete_name)).setText(routine.name+" complete!");
        ((TextView)findViewById(R.id.tv_steps_done)).setText(routine.steps.size()+" steps completed");
        long totalMin=routine.getTotalMinutes();
        ((TextView)findViewById(R.id.tv_time_taken)).setText(totalMin+" minutes");
        findViewById(R.id.btn_done).setOnClickListener(v->finish());
    }

    void showExitDialog(){
        new android.app.AlertDialog.Builder(this)
            .setTitle("Stop routine?")
            .setMessage("Your progress will be lost.")
            .setPositiveButton("Stop",(d,w)->{if(timer!=null)timer.cancel();finish();})
            .setNegativeButton("Continue",null).show();
    }

    @Override public void onBackPressed(){showExitDialog();}
}
