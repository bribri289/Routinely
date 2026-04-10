package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.receivers.RoutineNotificationReceiver;
import com.routinely.app.data.*;
import java.util.*;

public class EditRoutineActivity extends AppCompatActivity {
    AppData db; Models.Routine routine; boolean isNew=false;
    LinearLayout stepsContainer;
    Button btnStartRoutineNow;
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

        // TODO: Linked alarm feature has a known glitch — the alarm spinner selection does not
        // reliably persist and the alarm link is not always applied when the routine fires.
        // Will be addressed in a later update once the alarm scheduling engine is stabilized.
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
            // FIX: Schedule routine reminder notification — was missing, causing routine notifications to never fire
            RoutineNotificationReceiver.cancel(this, routine.id);
            RoutineNotificationReceiver.schedule(this, routine);
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

        // Start Routine button with projected end time
        btnStartRoutineNow = findViewById(R.id.btn_start_routine_now);
        updateEndTimeButton(btnStartRoutineNow);
        btnStartRoutineNow.setOnClickListener(v -> {
            collectStepFields();
            if (routine.steps.isEmpty()) {
                Toast.makeText(this, "Add steps first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, RunRoutineActivity.class);
            i.putExtra("routine", routine);
            i.putExtra("autoStart", true);
            startActivity(i);
        });
    }

    /** Calculate and display projected end time on the Start Routine button. */
    void updateEndTimeButton(Button btn) {
        int totalMins = 0;
        for (Models.RoutineStep s : routine.steps) totalMins += Math.max(0, s.durationSeconds / 60);
        java.util.Calendar end = java.util.Calendar.getInstance();
        end.add(java.util.Calendar.MINUTE, totalMins);
        int endH = end.get(java.util.Calendar.HOUR_OF_DAY);
        int endM = end.get(java.util.Calendar.MINUTE);
        String ap = endH < 12 ? "AM" : "PM";
        int hh = endH % 12; if (hh == 0) hh = 12;
        btn.setText(String.format("▶  Start Routine  •  Ends at %d:%02d %s", hh, endM, ap));
    }

    void rebuildSteps(){
        stepsContainer.removeAllViews();
        if (btnStartRoutineNow != null) updateEndTimeButton(btnStartRoutineNow);
        LayoutInflater inf=LayoutInflater.from(this);
        for(int i=0;i<routine.steps.size();i++){
            Models.RoutineStep step=routine.steps.get(i);
            View item=inf.inflate(R.layout.item_step_edit,stepsContainer,false);
            ((TextView)item.findViewById(R.id.tv_step_num)).setText(String.valueOf(i+1));

            EditText etEmoji=item.findViewById(R.id.et_step_emoji); etEmoji.setText(step.emoji);
            EditText etName=item.findViewById(R.id.et_step_name); etName.setText(step.name);
            EditText etDesc=item.findViewById(R.id.et_step_desc); etDesc.setText(step.description);

            // Duration stepper: display minutes (rounded)
            int durMins = Math.max(0, step.durationSeconds / 60);
            final int[] durHolder = {durMins};
            TextView tvDur = item.findViewById(R.id.tv_dur_value);
            tvDur.setText(String.valueOf(durHolder[0]));
            item.findViewById(R.id.btn_dur_minus).setOnClickListener(v -> {
                if (durHolder[0] > 0) { durHolder[0]--; tvDur.setText(String.valueOf(durHolder[0])); step.durationSeconds=Math.max(60,durHolder[0]*60); updateEndTimeButton(btnStartRoutineNow); }
            });
            item.findViewById(R.id.btn_dur_plus).setOnClickListener(v -> {
                durHolder[0]++; tvDur.setText(String.valueOf(durHolder[0])); step.durationSeconds=Math.max(60,durHolder[0]*60); updateEndTimeButton(btnStartRoutineNow);
            });
            // Store reference so collectStepFields can read it
            tvDur.setTag(durHolder);

            // Auto-icon: when step name loses focus, suggest icon if emoji field is default
            etName.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String cur = etEmoji.getText().toString().trim();
                    if (cur.isEmpty() || cur.equals("✅")) {
                        String suggested = autoIconForStep(etName.getText().toString());
                        etEmoji.setText(suggested);
                    }
                }
            });

            // Recurrence day chips (always visible)
            LinearLayout stepDays = item.findViewById(R.id.step_days_row);
            buildStepDayChips(stepDays, step);

            // Quick-select recurrence buttons
            TextView btnDaily = item.findViewById(R.id.btn_rec_daily);
            TextView btnWeekdays = item.findViewById(R.id.btn_rec_weekdays);
            TextView btnWeekends = item.findViewById(R.id.btn_rec_weekends);
            applyQuickSelect(step, stepDays, btnDaily, btnWeekdays, btnWeekends);
            btnDaily.setOnClickListener(v -> { java.util.Arrays.fill(step.repeatDays,true); step.recurrenceType="daily"; buildStepDayChips(stepDays,step); applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends); });
            btnWeekdays.setOnClickListener(v -> { step.repeatDays=new boolean[]{true,true,true,true,true,false,false}; step.recurrenceType="weekdays"; buildStepDayChips(stepDays,step); applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends); });
            btnWeekends.setOnClickListener(v -> { step.repeatDays=new boolean[]{false,false,false,false,false,true,true}; step.recurrenceType="weekends"; buildStepDayChips(stepDays,step); applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends); });

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

    /** Highlight the active quick-select button based on current repeatDays pattern. */
    void applyQuickSelect(Models.RoutineStep step, LinearLayout daysRow, TextView btnDaily, TextView btnWeekdays, TextView btnWeekends) {
        boolean isDaily = step.recurrenceType.equals("daily") || (allTrue(step.repeatDays));
        boolean isWeekdays = step.recurrenceType.equals("weekdays");
        boolean isWeekends = step.recurrenceType.equals("weekends");
        btnDaily.setBackground(getDrawable(isDaily ? R.drawable.chip_bg_active : R.drawable.chip_bg));
        btnWeekdays.setBackground(getDrawable(isWeekdays ? R.drawable.chip_bg_active : R.drawable.chip_bg));
        btnWeekends.setBackground(getDrawable(isWeekends ? R.drawable.chip_bg_active : R.drawable.chip_bg));
        btnDaily.setTextColor(isDaily ? 0xFFFFFFFF : 0xFF6B7280);
        btnWeekdays.setTextColor(isWeekdays ? 0xFFFFFFFF : 0xFF6B7280);
        btnWeekends.setTextColor(isWeekends ? 0xFFFFFFFF : 0xFF6B7280);
    }

    boolean allTrue(boolean[] arr) { for (boolean b : arr) if (!b) return false; return true; }

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
            // Duration stepper: read minutes from tv_dur_value tag
            TextView tvDur=item.findViewById(R.id.tv_dur_value);
            if(tvDur!=null){
                Object tag=tvDur.getTag();
                int mins=0;
                if(tag instanceof int[]) mins=Math.max(0,((int[])tag)[0]);
                else try{mins=Math.max(0,Integer.parseInt(tvDur.getText().toString()));}catch(Exception ignored){}
                step.durationSeconds=Math.max(60,mins*60);
            }
            // Recurrence type is managed by the day-chip click listeners directly on step.recurrenceType
            // If none of the quick-select matched, infer from repeatDays
            if(!step.recurrenceType.equals("daily")&&!step.recurrenceType.equals("weekdays")&&!step.recurrenceType.equals("weekends")){
                step.recurrenceType="custom_days";
            }
        }
    }

    /** Suggest an emoji icon based on common step title keywords. */
    static String autoIconForStep(String name) {
        if (name == null) return "✅";
        String lower = name.toLowerCase();
        if (lower.contains("brush") || lower.contains("teeth")) return "🪥";
        if (lower.contains("shower") || lower.contains("bath")) return "🚿";
        if (lower.contains("wash") || lower.contains("face")) return "🧼";
        if (lower.contains("skincare") || lower.contains("moistur")) return "✨";
        if (lower.contains("water") || lower.contains("drink")) return "💧";
        if (lower.contains("coffee") || lower.contains("tea")) return "☕";
        if (lower.contains("eat") || lower.contains("breakfast") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("meal") || lower.contains("food")) return "🍽️";
        if (lower.contains("exercise") || lower.contains("workout") || lower.contains("gym") || lower.contains("run") || lower.contains("jog")) return "🏋️";
        if (lower.contains("yoga") || lower.contains("stretch") || lower.contains("meditat")) return "🧘";
        if (lower.contains("walk") || lower.contains("steps")) return "🚶";
        if (lower.contains("read") || lower.contains("book") || lower.contains("study")) return "📖";
        if (lower.contains("journal") || lower.contains("write") || lower.contains("diary")) return "📝";
        if (lower.contains("plan") || lower.contains("review") || lower.contains("goal")) return "📋";
        if (lower.contains("sleep") || lower.contains("nap") || lower.contains("rest")) return "😴";
        if (lower.contains("vitamin") || lower.contains("supplement") || lower.contains("pill") || lower.contains("medicine")) return "💊";
        if (lower.contains("clean") || lower.contains("tidy") || lower.contains("organiz")) return "🧹";
        if (lower.contains("phone") || lower.contains("email") || lower.contains("message")) return "📱";
        if (lower.contains("music") || lower.contains("podcast")) return "🎵";
        if (lower.contains("gratitude") || lower.contains("affirmation")) return "🙏";
        return "✅";
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
