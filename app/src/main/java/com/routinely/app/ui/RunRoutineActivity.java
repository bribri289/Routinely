package com.routinely.app.ui;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;

public class RunRoutineActivity extends AppCompatActivity {
    Models.Routine routine; AppData db;
    int curStep=0; int secLeft=0; int secTotal=0;
    boolean paused=false; boolean running=false;
    CountDownTimer timer;
    ProgressBar progressBar;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_run_routine);
        db=AppData.get(this);
        routine=(Models.Routine)getIntent().getSerializableExtra("routine");
        if(routine==null){finish();return;}
        progressBar=findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        setupHeader();
        buildPreviewList();
        // Start button switches to run mode
        findViewById(R.id.btn_start_run).setOnClickListener(v->startRun());
        // FAB: add step inline
        findViewById(R.id.fab_add_step).setOnClickListener(v->{
            if(!running){ promptAddStep(); }
        });
        // Back
        findViewById(R.id.btn_back).setOnClickListener(v->{
            if(running) showExitDialog(); else finish();
        });
        // Menu ⋯
        findViewById(R.id.btn_menu).setOnClickListener(v->showMenu());
    }

    void setupHeader(){
        ((TextView)findViewById(R.id.tv_routine_name)).setText(routine.emoji+" "+routine.name);
        int totalMins=routine.getTotalMinutes();
        String ap=routine.startHour<12?"am":"pm";
        int hh=routine.startHour%12; if(hh==0)hh=12;
        ((TextView)findViewById(R.id.tv_routine_subtitle)).setText(
            String.format("%d:%02d%s (0s)",hh,routine.startMinute,ap));
    }

    void buildPreviewList(){
        LinearLayout list=findViewById(R.id.steps_list); list.removeAllViews();
        TextView empty=findViewById(R.id.tv_empty);
        if(routine.steps.isEmpty()){
            empty.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
            findViewById(R.id.btn_start_run).setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
            LayoutInflater inf=LayoutInflater.from(this);
            for(int i=0;i<routine.steps.size();i++){
                Models.RoutineStep s=routine.steps.get(i);
                View row=inf.inflate(R.layout.item_run_step,list,false);
                ((TextView)row.findViewById(R.id.tv_step_num)).setText(String.valueOf(i+1));
                ((TextView)row.findViewById(R.id.tv_step_emoji)).setText(s.emoji);
                ((TextView)row.findViewById(R.id.tv_step_name)).setText(s.name);
                int m=s.durationSeconds/60; int sec=s.durationSeconds%60;
                String dur=m>0?m+"m":(sec>0?sec+"s":"–");
                ((TextView)row.findViewById(R.id.tv_step_dur)).setText(dur);
                // Divider
                if(i<routine.steps.size()-1){
                    View div=new View(this);
                    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1);
                    lp.setMargins(0,0,0,0); div.setLayoutParams(lp);
                    div.setBackgroundColor(0xFF1F2937);
                    list.addView(row); list.addView(div);
                } else {
                    list.addView(row);
                }
            }
        }
    }

    void promptAddStep(){
        android.app.AlertDialog.Builder b=new android.app.AlertDialog.Builder(this);
        b.setTitle("Add Step");
        EditText et=new EditText(this); et.setHint("Step name"); et.setPadding(48,24,48,24);
        b.setView(et);
        b.setPositiveButton("Add",(d,w)->{
            String name=et.getText().toString().trim();
            if(name.isEmpty()) return;
            Models.RoutineStep s=new Models.RoutineStep();
            s.id=db.newId(); s.name=name; s.emoji="✅"; s.durationSeconds=300;
            routine.steps.add(s);
            // Update in db
            for(int i=0;i<db.routines.size();i++){
                if(db.routines.get(i).id==routine.id){db.routines.set(i,routine);break;}
            }
            db.save(); buildPreviewList();
        });
        b.setNegativeButton("Cancel",null); b.show();
    }

    void showMenu(){
        String[] opts={"Settings","Duplicate","Archive","Edit Task","Share","Delete"};
        new android.app.AlertDialog.Builder(this).setItems(opts,(d,w)->{
            switch(w){
                case 0: case 3:{ Intent i=new Intent(this,EditRoutineActivity.class); i.putExtra("routine",routine); startActivity(i); break; }
                case 5:{ new android.app.AlertDialog.Builder(this).setTitle("Delete routine?")
                    .setPositiveButton("Delete",(dd,ww)->{db.routines.removeIf(r->r.id==routine.id);db.save();finish();})
                    .setNegativeButton("Cancel",null).show(); break; }
                case 2:{ routine.archived=true; for(int i=0;i<db.routines.size();i++){if(db.routines.get(i).id==routine.id){db.routines.set(i,routine);break;}} db.save(); finish(); break; }
            }
        }).show();
    }

    void startRun(){
        if(routine.steps.isEmpty()) return;
        running=true;
        // Hide preview, show run panel
        findViewById(R.id.preview_scroll).setVisibility(View.GONE);
        findViewById(R.id.run_panel).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_start_run).setVisibility(View.GONE);
        findViewById(R.id.btn_complete).setVisibility(View.VISIBLE);
        findViewById(R.id.skip_pause_row).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_add_step).setVisibility(View.GONE);
        startStep(0);
    }

    void startStep(int idx){
        curStep=idx;
        if(idx>=routine.steps.size()){onRoutineComplete();return;}
        Models.RoutineStep step=routine.steps.get(idx);
        progressBar.setMax(routine.steps.size());
        progressBar.setProgress(idx+1);
        ((TextView)findViewById(R.id.tv_step_emoji)).setText(step.emoji);
        ((TextView)findViewById(R.id.tv_step_name)).setText(step.name);
        ((TextView)findViewById(R.id.tv_step_description)).setText(step.description);
        secTotal=step.durationSeconds>0?step.durationSeconds:(step.durationMinutes>0?step.durationMinutes*60:300);
        secLeft=secTotal; paused=false;
        updateTimer(); startTimer();
        View habPanel=findViewById(R.id.habit_panel);
        if(step.linkedHabitId!=0){
            Models.Habit h=db.findHabit(step.linkedHabitId);
            if(h!=null){habPanel.setVisibility(View.VISIBLE);((TextView)habPanel.findViewById(R.id.tv_habit_name)).setText(h.emoji+" "+h.name);}
            else habPanel.setVisibility(View.GONE);
        } else habPanel.setVisibility(View.GONE);
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
            .setTitle("Stop routine?").setMessage("Your progress will be lost.")
            .setPositiveButton("Stop",(d,w)->{if(timer!=null)timer.cancel();finish();})
            .setNegativeButton("Continue",null).show();
    }

    @Override public void onBackPressed(){
        if(running) showExitDialog(); else finish();
    }
}
