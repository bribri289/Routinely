package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.util.*;

public class EditHabitActivity extends AppCompatActivity {
    AppData db; Models.Habit habit; boolean isNew=false;
    String[] DAYS={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_edit_habit);
        db=AppData.get(this);
        Models.Habit incoming=(Models.Habit)getIntent().getSerializableExtra("habit");
        if(incoming==null){isNew=true;habit=new Models.Habit();habit.id=db.newId();}
        else habit=incoming;
        setup();
    }

    void setup(){
        findViewById(R.id.btn_back).setOnClickListener(v->finish());
        ((TextView)findViewById(R.id.tv_title)).setText(isNew?"New Habit":"Edit Habit");
        EditText etEmoji=findViewById(R.id.et_emoji); etEmoji.setText(habit.emoji);
        EditText etName=findViewById(R.id.et_name); etName.setText(habit.name);
        EditText etCat=findViewById(R.id.et_category); etCat.setText(habit.category);

        LinearLayout daysRow=findViewById(R.id.days_row); daysRow.removeAllViews();
        for(int i=0;i<7;i++){final int idx=i;
            TextView chip=new TextView(this); chip.setText(DAYS[i]);
            chip.setPadding(24,12,24,12); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(habit.repeatDays[i]?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{habit.repeatDays[idx]=!habit.repeatDays[idx];chip.setBackground(getDrawable(habit.repeatDays[idx]?R.drawable.chip_bg_active:R.drawable.chip_bg));});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); daysRow.addView(chip);}

        TimePicker tp=findViewById(R.id.time_picker);
        tp.setIs24HourView(false); tp.setHour(habit.reminderHour); tp.setMinute(habit.reminderMinute);
        Switch swRem=findViewById(R.id.sw_reminder); swRem.setChecked(habit.reminderEnabled);

        Spinner rSpin=findViewById(R.id.spinner_routine);
        List<String> rLabels=new ArrayList<>(); rLabels.add("No routine");
        List<Integer> rIds=new ArrayList<>(); rIds.add(0);
        for(Models.Routine r:db.routines){rLabels.add(r.emoji+" "+r.name);rIds.add(r.id);}
        ArrayAdapter<String> ra=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,rLabels);
        ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); rSpin.setAdapter(ra);
        for(int i=0;i<rIds.size();i++) if(rIds.get(i)==habit.linkedRoutineId){rSpin.setSelection(i);break;}
        final List<Integer> routineIds=rIds;

        findViewById(R.id.btn_save).setOnClickListener(v->{
            String name=etName.getText().toString().trim();
            if(name.isEmpty()){etName.setError("Name required");return;}
            habit.name=name;
            habit.emoji=etEmoji.getText().toString().trim(); if(habit.emoji.isEmpty())habit.emoji="💧";
            habit.category=etCat.getText().toString().trim();
            habit.reminderHour=tp.getHour(); habit.reminderMinute=tp.getMinute();
            habit.reminderEnabled=swRem.isChecked();
            habit.linkedRoutineId=routineIds.get(rSpin.getSelectedItemPosition());
            if(isNew){
                db.habits.add(habit);
            } else {
                for(int i=0;i<db.habits.size();i++){
                    if(db.habits.get(i).id==habit.id){db.habits.set(i,habit);break;}
                }
            }
            db.save();
            Toast.makeText(this,"Habit saved!",Toast.LENGTH_SHORT).show();
            finish();
        });

        View btnDel=findViewById(R.id.btn_delete); btnDel.setVisibility(isNew?View.GONE:View.VISIBLE);
        btnDel.setOnClickListener(v->{
            new android.app.AlertDialog.Builder(this).setTitle("Delete habit?")
                .setPositiveButton("Delete",(d,w)->{
                    db.habits.removeIf(h->h.id==habit.id); db.save(); finish();
                }).setNegativeButton("Cancel",null).show();
        });
    }
}
