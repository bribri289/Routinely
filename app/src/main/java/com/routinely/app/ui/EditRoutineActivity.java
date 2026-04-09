package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.util.*;

public class EditRoutineActivity extends AppCompatActivity {
    AppData db; Models.Routine routine; boolean isNew=false;
    LinearLayout stepsContainer;
    String[] DAYS={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_edit_routine);
        db=AppData.get(this);
        Models.Routine incoming=(Models.Routine)getIntent().getSerializableExtra("routine");
        if(incoming==null){
            isNew=true;
            routine=new Models.Routine();
            routine.id=db.newId();
        } else {
            // Work on a deep copy to avoid reference issues
            routine=incoming;
        }
        setup();
    }

    void setup(){
        findViewById(R.id.btn_back).setOnClickListener(v->finish());
        ((TextView)findViewById(R.id.tv_title)).setText(isNew?"New Routine":"Edit Routine");

        EditText etEmoji=findViewById(R.id.et_emoji); etEmoji.setText(routine.emoji);
        EditText etName=findViewById(R.id.et_name); etName.setText(routine.name);
        EditText etCat=findViewById(R.id.et_category); etCat.setText(routine.category);

        TimePicker tp=findViewById(R.id.time_picker);
        tp.setIs24HourView(false); tp.setHour(routine.startHour); tp.setMinute(routine.startMinute);

        // Day chips
        LinearLayout daysRow=findViewById(R.id.days_row); daysRow.removeAllViews();
        for(int i=0;i<7;i++){final int idx=i;
            TextView chip=new TextView(this); chip.setText(DAYS[i]);
            chip.setPadding(24,12,24,12); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(routine.repeatDays[i]?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{routine.repeatDays[idx]=!routine.repeatDays[idx];chip.setBackground(getDrawable(routine.repeatDays[idx]?R.drawable.chip_bg_active:R.drawable.chip_bg));});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); daysRow.addView(chip);}

        // Alarm link
        Spinner almSpin=findViewById(R.id.spinner_alarm);
        List<String> almLabels=new ArrayList<>(); almLabels.add("No linked alarm");
        List<Integer> almIds=new ArrayList<>(); almIds.add(0);
        for(Models.Alarm a:db.alarms){almLabels.add(a.getTimeString()+" — "+a.label);almIds.add(a.id);}
        ArrayAdapter<String> aa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,almLabels);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); almSpin.setAdapter(aa);
        for(int i=0;i<almIds.size();i++) if(almIds.get(i)==routine.linkedAlarmId){almSpin.setSelection(i);break;}
        final List<Integer> alarmIds=almIds;

        stepsContainer=findViewById(R.id.steps_container);
        rebuildSteps();
        findViewById(R.id.btn_add_step).setOnClickListener(v->{
            collectStepFields(); // save edits in existing steps before adding
            Models.RoutineStep s=new Models.RoutineStep();
            s.id=db.newId(); s.name="New step"; s.emoji="✅"; s.durationSeconds=300;
            routine.steps.add(s); rebuildSteps();
        });

        // Save
        findViewById(R.id.btn_save).setOnClickListener(v->{
            // Read all step fields before saving
            collectStepFields();
            String name=etName.getText().toString().trim();
            if(name.isEmpty()){etName.setError("Name required");return;}
            routine.name=name;
            routine.emoji=etEmoji.getText().toString().trim(); if(routine.emoji.isEmpty())routine.emoji="🌅";
            routine.category=etCat.getText().toString().trim();
            routine.startHour=tp.getHour(); routine.startMinute=tp.getMinute();
            routine.linkedAlarmId=alarmIds.get(almSpin.getSelectedItemPosition());
            if(isNew){
                db.routines.add(routine);
            } else {
                // Update in place
                for(int i=0;i<db.routines.size();i++){
                    if(db.routines.get(i).id==routine.id){db.routines.set(i,routine);break;}
                }
            }
            db.save();
            Toast.makeText(this,"Routine saved!",Toast.LENGTH_SHORT).show();
            finish();
        });

        View btnDel=findViewById(R.id.btn_delete); btnDel.setVisibility(isNew?View.GONE:View.VISIBLE);
        btnDel.setOnClickListener(v->{
            new android.app.AlertDialog.Builder(this).setTitle("Delete routine?")
                .setPositiveButton("Delete",(d,w)->{
                    db.routines.removeIf(r->r.id==routine.id); db.save(); finish();
                }).setNegativeButton("Cancel",null).show();
        });
    }

    void rebuildSteps(){
        stepsContainer.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(this);
        for(int i=0;i<routine.steps.size();i++){
            Models.RoutineStep step=routine.steps.get(i);
            View item=inf.inflate(R.layout.item_step_edit,stepsContainer,false);
            ((TextView)item.findViewById(R.id.tv_step_num)).setText(String.valueOf(i+1));

            EditText etEmoji=item.findViewById(R.id.et_step_emoji); etEmoji.setText(step.emoji);
            EditText etName=item.findViewById(R.id.et_step_name); etName.setText(step.name);
            EditText etDesc=item.findViewById(R.id.et_step_desc); etDesc.setText(step.description);

            // Duration: hours, minutes, seconds
            int h=step.durationSeconds/3600;
            int m=(step.durationSeconds%3600)/60;
            int s=step.durationSeconds%60;
            EditText etH=item.findViewById(R.id.et_step_hours); etH.setText(String.valueOf(h));
            EditText etM=item.findViewById(R.id.et_step_mins); etM.setText(String.valueOf(m));
            EditText etS=item.findViewById(R.id.et_step_secs); etS.setText(String.valueOf(s));

            // Recurrence
            Spinner recSpin=item.findViewById(R.id.spinner_recurrence);
            String[] recTypes={"Daily","Weekdays","Weekends","Every N days","Every N weeks","Every N months","Custom days"};
            ArrayAdapter<String> ra=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,recTypes);
            ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); recSpin.setAdapter(ra);
            int recIdx=recurrenceIndex(step.recurrenceType);
            recSpin.setSelection(recIdx);

            // Custom interval
            EditText etInterval=item.findViewById(R.id.et_step_interval); etInterval.setText(String.valueOf(step.customInterval));

            // Day chips for custom
            LinearLayout stepDays=item.findViewById(R.id.step_days_row);
            buildStepDayChips(stepDays, step);

            recSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> p,View v,int pos,long id){
                    stepDays.setVisibility(pos==6?View.VISIBLE:View.GONE);
                    etInterval.setVisibility((pos==3||pos==4||pos==5)?View.VISIBLE:View.GONE);
                }
                public void onNothingSelected(AdapterView<?> p){}
            });
            stepDays.setVisibility(recIdx==6?View.VISIBLE:View.GONE);
            etInterval.setVisibility((recIdx==3||recIdx==4||recIdx==5)?View.VISIBLE:View.GONE);

            // Habit link
            Spinner hs=item.findViewById(R.id.spinner_habit);
            List<String> hl=new ArrayList<>(); hl.add("No habit"); List<Integer> hi=new ArrayList<>(); hi.add(0);
            for(Models.Habit habit:db.habits){hl.add(habit.emoji+" "+habit.name);hi.add(habit.id);}
            ArrayAdapter<String> ha=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,hl);
            ha.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); hs.setAdapter(ha);
            for(int j=0;j<hi.size();j++) if(hi.get(j)==step.linkedHabitId){hs.setSelection(j);break;}
            final List<Integer> habitIds=hi;
            hs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
                public void onItemSelected(AdapterView<?> p,View v,int pos,long id){step.linkedHabitId=habitIds.get(pos);}
                public void onNothingSelected(AdapterView<?> p){}
            });

            final int stepIdx=i;
            item.findViewById(R.id.btn_remove_step).setOnClickListener(v->{
                collectStepFields(); routine.steps.remove(stepIdx); rebuildSteps();
            });
            stepsContainer.addView(item);
        }
    }

    void buildStepDayChips(LinearLayout row, Models.RoutineStep step){
        row.removeAllViews();
        String[] DAYS2={"M","T","W","T","F","S","S"};
        int sizeDp=32;
        float density=getResources().getDisplayMetrics().density;
        int sizePx=(int)(sizeDp*density);
        for(int i=0;i<7;i++){final int idx=i;
            TextView chip=new TextView(this); chip.setText(DAYS2[i]);
            chip.setTextColor(step.repeatDays[idx]?0xFFFFFFFF:0xFF6B7280); chip.setTextSize(11);
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setBackground(getDrawable(step.repeatDays[idx]?R.drawable.circle_primary:R.drawable.circle_bg3));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(sizePx,sizePx); lp.setMargins(4,4,4,4); chip.setLayoutParams(lp);
            chip.setOnClickListener(v->{step.repeatDays[idx]=!step.repeatDays[idx];chip.setBackground(getDrawable(step.repeatDays[idx]?R.drawable.circle_primary:R.drawable.circle_bg3));chip.setTextColor(step.repeatDays[idx]?0xFFFFFFFF:0xFF6B7280);});
            row.addView(chip);}
    }

    void collectStepFields(){
        for(int i=0;i<stepsContainer.getChildCount()&&i<routine.steps.size();i++){
            View item=stepsContainer.getChildAt(i); Models.RoutineStep step=routine.steps.get(i);
            EditText n=item.findViewById(R.id.et_step_name); if(n!=null&&!n.getText().toString().isEmpty())step.name=n.getText().toString();
            EditText d=item.findViewById(R.id.et_step_desc); if(d!=null)step.description=d.getText().toString();
            EditText em=item.findViewById(R.id.et_step_emoji); if(em!=null&&!em.getText().toString().isEmpty())step.emoji=em.getText().toString();
            EditText etH=item.findViewById(R.id.et_step_hours);
            EditText etM=item.findViewById(R.id.et_step_mins);
            EditText etS=item.findViewById(R.id.et_step_secs);
            try{
                int h=etH!=null?Integer.parseInt(etH.getText().toString()):0;
                int m=etM!=null?Integer.parseInt(etM.getText().toString()):0;
                int s=etS!=null?Integer.parseInt(etS.getText().toString()):0;
                step.durationSeconds=h*3600+m*60+s;
                if(step.durationSeconds<=0)step.durationSeconds=60;
            }catch(Exception ignored){}
            Spinner recSpin=item.findViewById(R.id.spinner_recurrence);
            if(recSpin!=null){
                String[] types={"daily","weekdays","weekends","every_n_days","every_n_weeks","every_n_months","custom_days"};
                int pos=recSpin.getSelectedItemPosition();
                step.recurrenceType=pos<types.length?types[pos]:"daily";
            }
            EditText etInt=item.findViewById(R.id.et_step_interval);
            try{if(etInt!=null)step.customInterval=Math.max(1,Integer.parseInt(etInt.getText().toString()));}catch(Exception ignored){}
        }
    }

    int recurrenceIndex(String type){
        switch(type==null?"daily":type){
            case "weekdays":return 1; case "weekends":return 2;
            case "every_n_days":return 3; case "every_n_weeks":return 4;
            case "every_n_months":return 5; case "custom_days":return 6;
            default:return 0;
        }
    }
}
