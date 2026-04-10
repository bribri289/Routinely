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
    /** Screen mode constants. Pass via EXTRA_MODE intent extra. */
    public static final int MODE_CREATE = 0;
    public static final int MODE_EDIT   = 1;
    public static final int MODE_VIEW   = 2;
    public static final String EXTRA_MODE = "mode";

    /** Reusable day-pattern arrays for recurrence quick-select. */
    static final boolean[] WEEKDAYS_PATTERN = {true,true,true,true,true,false,false};
    static final boolean[] WEEKENDS_PATTERN = {false,false,false,false,false,true,true};
    /** Max characters shown in step description preview in VIEW mode. */
    static final int DESC_PREVIEW_LEN = 50;

    AppData db; Models.Routine routine; boolean isNew=false;
    int mode = MODE_EDIT;
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
            mode=MODE_CREATE;
        } else {
            routine=incoming;
            mode=getIntent().getIntExtra(EXTRA_MODE, MODE_EDIT);
        }
        setup();
    }

    void setup(){
        // Dynamic title based on mode
        String[] titles={"New Routine","Edit Routine","Routine Summary"};
        ((TextView)findViewById(R.id.tv_title)).setText(titles[mode]);

        // Back button text
        Button btnBack=findViewById(R.id.btn_back);
        btnBack.setText(mode==MODE_VIEW?"← Back":"← Cancel");
        btnBack.setOnClickListener(v->finish());

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
            if(mode!=MODE_VIEW){
                chip.setOnClickListener(v->{routine.repeatDays[idx]=!routine.repeatDays[idx];chip.setBackground(getDrawable(routine.repeatDays[idx]?R.drawable.chip_bg_active:R.drawable.chip_bg));});
            }
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

        // ── Mode-specific behaviour ────────────────────────────────────────
        Button btnSave=findViewById(R.id.btn_save);
        if(mode==MODE_VIEW){
            // Read-only fields
            etEmoji.setFocusable(false); etEmoji.setEnabled(false);
            etName.setFocusable(false);  etName.setEnabled(false);
            etCat.setFocusable(false);   etCat.setEnabled(false);
            tp.setEnabled(false);
            almSpin.setEnabled(false);
            // Hide edit-only controls
            findViewById(R.id.btn_add_step).setVisibility(View.GONE);
            findViewById(R.id.btn_delete).setVisibility(View.GONE);
            // btn_save becomes "Edit Routine" shortcut
            btnSave.setText("Edit");
            btnSave.setOnClickListener(v->{
                Intent i=new Intent(this,EditRoutineActivity.class);
                i.putExtra("routine",routine);
                i.putExtra(EXTRA_MODE,MODE_EDIT);
                startActivity(i); finish();
            });
        } else {
            // CREATE / EDIT modes
            View btnDel=findViewById(R.id.btn_delete);
            btnDel.setVisibility(isNew?View.GONE:View.VISIBLE);
            btnDel.setOnClickListener(v->{
                new android.app.AlertDialog.Builder(this).setTitle("Delete routine?")
                    .setPositiveButton("Delete",(d,w)->{
                        db.routines.removeIf(r->r.id==routine.id); db.save(); finish();
                    }).setNegativeButton("Cancel",null).show();
            });
            // Add step
            findViewById(R.id.btn_add_step).setOnClickListener(v->{
                collectStepFields();
                Models.RoutineStep s=new Models.RoutineStep();
                s.id=db.newId(); s.name="New step"; s.emoji="✅"; s.durationSeconds=300;
                routine.steps.add(s); rebuildSteps();
            });
            // Save
            btnSave.setOnClickListener(v->{
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
                    for(int i=0;i<db.routines.size();i++){
                        if(db.routines.get(i).id==routine.id){db.routines.set(i,routine);break;}
                    }
                }
                db.save();
                RoutineNotificationReceiver.cancel(this,routine.id);
                RoutineNotificationReceiver.schedule(this,routine);
                Toast.makeText(this,"Routine saved!",Toast.LENGTH_SHORT).show();
                finish();
            });
        }

        // Start Routine button (always visible in all modes)
        btnStartRoutineNow=findViewById(R.id.btn_start_routine_now);
        updateEndTimeButton(btnStartRoutineNow);
        btnStartRoutineNow.setOnClickListener(v->{
            if(mode!=MODE_VIEW) collectStepFields();
            if(routine.steps.isEmpty()){Toast.makeText(this,"Add steps first",Toast.LENGTH_SHORT).show();return;}
            Intent i=new Intent(this,RunRoutineActivity.class);
            i.putExtra("routine",routine); i.putExtra("autoStart",true); startActivity(i);
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
        if(btnStartRoutineNow!=null) updateEndTimeButton(btnStartRoutineNow);
        if(mode==MODE_VIEW){
            rebuildStepsViewMode();
        } else {
            rebuildStepsEditMode();
        }
    }

    // ── VIEW mode: compact summary cards ────────────────────────────────────

    void rebuildStepsViewMode(){
        if(routine.steps.isEmpty()){
            TextView empty=new TextView(this); empty.setText("No steps yet.");
            empty.setTextColor(0xFF9CA3AF); empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32,32,32,32);
            stepsContainer.addView(empty); return;
        }
        float d=getResources().getDisplayMetrics().density;
        for(int i=0;i<routine.steps.size();i++){
            Models.RoutineStep step=routine.steps.get(i);
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackground(getDrawable(R.drawable.card_bg2)); card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setPadding((int)(16*d),(int)(14*d),(int)(16*d),(int)(14*d));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,0,(int)(8*d)); card.setLayoutParams(lp);
            // Icon
            TextView tvEmoji=new TextView(this); tvEmoji.setText(step.emoji); tvEmoji.setTextSize(22);
            LinearLayout.LayoutParams iconLp=new LinearLayout.LayoutParams((int)(44*d),(int)(44*d));
            iconLp.setMarginEnd((int)(12*d)); tvEmoji.setLayoutParams(iconLp);
            tvEmoji.setGravity(android.view.Gravity.CENTER);
            tvEmoji.setBackground(getDrawable(R.drawable.card_bg));
            card.addView(tvEmoji);
            // Name + duration col
            LinearLayout col=new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
            TextView tvSName=new TextView(this); tvSName.setText(step.name);
            tvSName.setTextColor(0xFFFFFFFF); tvSName.setTextSize(15);
            tvSName.setTypeface(null,android.graphics.Typeface.BOLD); col.addView(tvSName);
            int sm=step.durationSeconds/60;
            String durStr=sm>0?sm+" min":step.durationSeconds+"s";
            TextView tvDur2=new TextView(this); tvDur2.setText(durStr);
            tvDur2.setTextColor(0xFF9CA3AF); tvDur2.setTextSize(12); col.addView(tvDur2);
            // Optional description snippet
            if(step.description!=null&&!step.description.isEmpty()){
                String snip=step.description.length()>DESC_PREVIEW_LEN
                    ?step.description.substring(0,DESC_PREVIEW_LEN)+"…"
                    :step.description;
                TextView tvSnip=new TextView(this); tvSnip.setText(snip);
                tvSnip.setTextColor(0xFF6B7280); tvSnip.setTextSize(11); col.addView(tvSnip);
            }
            card.addView(col);
            // Best streak badge
            if(step.bestStreak>0){
                TextView tvStreak=new TextView(this); tvStreak.setText("🏆 "+step.bestStreak);
                tvStreak.setTextSize(11); tvStreak.setTextColor(0xFFF59E0B);
                tvStreak.setBackground(getDrawable(R.drawable.chip_bg_yellow));
                tvStreak.setPadding((int)(8*d),(int)(4*d),(int)(8*d),(int)(4*d));
                LinearLayout.LayoutParams stLp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.WRAP_CONTENT);
                stLp.setMarginStart((int)(8*d)); tvStreak.setLayoutParams(stLp);
                card.addView(tvStreak);
            }
            // Chevron
            TextView tvArrow=new TextView(this); tvArrow.setText("›");
            tvArrow.setTextColor(0xFF9CA3AF); tvArrow.setTextSize(20);
            tvArrow.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams arrowLp=new LinearLayout.LayoutParams((int)(24*d),ViewGroup.LayoutParams.WRAP_CONTENT);
            arrowLp.setMarginStart((int)(4*d)); tvArrow.setLayoutParams(arrowLp);
            card.addView(tvArrow);

            final int stepIdx=i;
            card.setOnClickListener(v->showStepEditPopup(stepIdx));
            stepsContainer.addView(card);
        }
    }

    // ── EDIT/CREATE mode: inline step editors ───────────────────────────────

    void rebuildStepsEditMode(){
        LayoutInflater inf=LayoutInflater.from(this);
        for(int i=0;i<routine.steps.size();i++){
            Models.RoutineStep step=routine.steps.get(i);
            View item=inf.inflate(R.layout.item_step_edit,stepsContainer,false);
            ((TextView)item.findViewById(R.id.tv_step_num)).setText(String.valueOf(i+1));

            EditText etEmoji=item.findViewById(R.id.et_step_emoji); etEmoji.setText(step.emoji);
            EditText etName=item.findViewById(R.id.et_step_name); etName.setText(step.name);
            EditText etDesc=item.findViewById(R.id.et_step_desc); etDesc.setText(step.description);

            // Duration stepper: display minutes (rounded)
            int durMins=Math.max(0,step.durationSeconds/60);
            final int[] durHolder={durMins};
            TextView tvDur=item.findViewById(R.id.tv_dur_value);
            tvDur.setText(String.valueOf(durHolder[0]));
            item.findViewById(R.id.btn_dur_minus).setOnClickListener(v->{
                if(durHolder[0]>0){durHolder[0]--;tvDur.setText(String.valueOf(durHolder[0]));step.durationSeconds=durHolder[0]*60;updateEndTimeButton(btnStartRoutineNow);}
            });
            item.findViewById(R.id.btn_dur_plus).setOnClickListener(v->{
                durHolder[0]++;tvDur.setText(String.valueOf(durHolder[0]));step.durationSeconds=durHolder[0]*60;updateEndTimeButton(btnStartRoutineNow);
            });
            tvDur.setTag(durHolder);

            // Auto-icon: when step name loses focus, suggest icon if emoji field is default
            etName.setOnFocusChangeListener((v,hasFocus)->{
                if(!hasFocus){
                    String cur=etEmoji.getText().toString().trim();
                    if(cur.isEmpty()||cur.equals("✅")){
                        etEmoji.setText(autoIconForStep(etName.getText().toString()));
                    }
                }
            });

            // Recurrence day chips (always visible)
            LinearLayout stepDays=item.findViewById(R.id.step_days_row);
            buildStepDayChips(stepDays,step);

            // Quick-select recurrence buttons
            TextView btnDaily=item.findViewById(R.id.btn_rec_daily);
            TextView btnWeekdays=item.findViewById(R.id.btn_rec_weekdays);
            TextView btnWeekends=item.findViewById(R.id.btn_rec_weekends);
            applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends);
            btnDaily.setOnClickListener(v->{java.util.Arrays.fill(step.repeatDays,true);step.recurrenceType="daily";buildStepDayChips(stepDays,step);applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends);});
            btnWeekdays.setOnClickListener(v->{step.repeatDays=java.util.Arrays.copyOf(WEEKDAYS_PATTERN,7);step.recurrenceType="weekdays";buildStepDayChips(stepDays,step);applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends);});
            btnWeekends.setOnClickListener(v->{step.repeatDays=java.util.Arrays.copyOf(WEEKENDS_PATTERN,7);step.recurrenceType="weekends";buildStepDayChips(stepDays,step);applyQuickSelect(step,stepDays,btnDaily,btnWeekdays,btnWeekends);});

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

            // Long-press to reorder (move up / down)
            final int stepIdx=i;
            item.setOnLongClickListener(v->{
                String[] opts={"Move Up","Move Down"};
                new android.app.AlertDialog.Builder(this).setTitle("Reorder Step")
                    .setItems(opts,(dlg,w)->{
                        collectStepFields();
                        int newIdx=stepIdx+(w==0?-1:1);
                        if(newIdx>=0&&newIdx<routine.steps.size()){
                            Models.RoutineStep s=routine.steps.remove(stepIdx);
                            routine.steps.add(newIdx,s); rebuildSteps();
                        }
                    }).show();
                return true;
            });

            item.findViewById(R.id.btn_remove_step).setOnClickListener(v->{
                collectStepFields(); routine.steps.remove(stepIdx); rebuildSteps();
            });
            stepsContainer.addView(item);
        }
    }

    // ── Step Detail Editor popup ─────────────────────────────────────────────

    /** Full step edit popup modal. Accessible from both VIEW and EDIT modes. */
    void showStepEditPopup(int stepIdx){
        if(stepIdx<0||stepIdx>=routine.steps.size()) return;
        Models.RoutineStep step=routine.steps.get(stepIdx);

        android.app.Dialog popup=new android.app.Dialog(this,R.style.FullScreenDialog);
        ScrollView sv=new ScrollView(this);
        sv.setBackgroundColor(0xFF111827);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20,48,20,32);
        root.setBackgroundColor(0xFF111827);

        // Header
        LinearLayout hdr=new LinearLayout(this); hdr.setOrientation(LinearLayout.HORIZONTAL); hdr.setGravity(android.view.Gravity.CENTER_VERTICAL);
        hdr.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView tvBack2=new TextView(this); tvBack2.setText("✕"); tvBack2.setTextColor(0xFF9CA3AF); tvBack2.setTextSize(20); tvBack2.setPadding(0,0,16,0); tvBack2.setOnClickListener(x->popup.dismiss()); hdr.addView(tvBack2);
        TextView tvTitle=new TextView(this); tvTitle.setText("Edit Step"); tvTitle.setTextColor(0xFFFFFFFF); tvTitle.setTextSize(17); tvTitle.setTypeface(null,android.graphics.Typeface.BOLD);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1)); hdr.addView(tvTitle);
        root.addView(hdr);
        addPopupSpace(root,20);

        // Icon + Name row
        addPopupLabel(root,"STEP NAME");
        LinearLayout nameRow=new LinearLayout(this); nameRow.setOrientation(LinearLayout.HORIZONTAL); nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        nameRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        float density=getResources().getDisplayMetrics().density;
        android.widget.EditText etEmoji2=makePopupEditText(step.emoji,(int)(56*density),(int)(52*density),24); etEmoji2.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams emojiLp=new LinearLayout.LayoutParams((int)(56*density),(int)(52*density)); emojiLp.setMarginEnd((int)(10*density)); etEmoji2.setLayoutParams(emojiLp);
        nameRow.addView(etEmoji2);
        android.widget.EditText etName2=makePopupEditText(step.name,0,(int)(52*density),15);
        etName2.setHint("Step name"); etName2.setHintTextColor(0xFF6B7280);
        etName2.setLayoutParams(new LinearLayout.LayoutParams(0,(int)(52*density),1));
        // Auto-icon on name focus-loss
        etName2.setOnFocusChangeListener((v,hasFocus)->{
            if(!hasFocus){
                String cur=etEmoji2.getText().toString().trim();
                if(cur.isEmpty()||cur.equals("✅")){
                    etEmoji2.setText(autoIconForStep(etName2.getText().toString()));
                }
            }
        });
        nameRow.addView(etName2);
        root.addView(nameRow);
        addPopupSpace(root,14);

        // Duration stepper
        addPopupLabel(root,"DURATION (minutes)");
        final int[] durHolder={Math.max(0,step.durationSeconds/60)};
        LinearLayout durRow=new LinearLayout(this); durRow.setOrientation(LinearLayout.HORIZONTAL); durRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        durRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        Button btnMinus=makePopupIconButton("–",R.drawable.btn_secondary_bg);
        TextView tvDurVal=new TextView(this); tvDurVal.setText(String.valueOf(durHolder[0])); tvDurVal.setTextColor(0xFFFFFFFF); tvDurVal.setTextSize(20); tvDurVal.setTypeface(null,android.graphics.Typeface.BOLD);
        tvDurVal.setGravity(android.view.Gravity.CENTER); tvDurVal.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        Button btnPlus=makePopupIconButton("+",R.drawable.btn_primary_bg);
        btnMinus.setOnClickListener(x->{if(durHolder[0]>0){durHolder[0]--;tvDurVal.setText(String.valueOf(durHolder[0]));}});
        btnPlus.setOnClickListener(x->{durHolder[0]++;tvDurVal.setText(String.valueOf(durHolder[0]));});
        durRow.addView(btnMinus); durRow.addView(tvDurVal); durRow.addView(btnPlus);
        root.addView(durRow);
        addPopupSpace(root,14);

        // Description
        addPopupLabel(root,"DESCRIPTION");
        android.widget.EditText etDesc2=makePopupEditText(step.description,0,-1,14);
        etDesc2.setHint("Add a description…"); etDesc2.setHintTextColor(0xFF6B7280); etDesc2.setMinLines(3);
        etDesc2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(etDesc2);
        addPopupSpace(root,14);

        // Recurrence
        addPopupLabel(root,"RECURRENCE");
        LinearLayout quickRow=new LinearLayout(this); quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView btnRDaily=makeRecChip("Daily"); LinearLayout.LayoutParams qLp=new LinearLayout.LayoutParams(0,(int)(36*density),1); qLp.setMarginEnd(6); btnRDaily.setLayoutParams(qLp);
        TextView btnRWeekdays=makeRecChip("Weekdays"); LinearLayout.LayoutParams qLp2=new LinearLayout.LayoutParams(0,(int)(36*density),1); qLp2.setMarginEnd(6); btnRWeekdays.setLayoutParams(qLp2);
        TextView btnRWeekends=makeRecChip("Weekends"); btnRWeekends.setLayoutParams(new LinearLayout.LayoutParams(0,(int)(36*density),1));
        quickRow.addView(btnRDaily); quickRow.addView(btnRWeekdays); quickRow.addView(btnRWeekends);
        root.addView(quickRow);
        addPopupSpace(root,8);

        // Day toggles
        LinearLayout daysRow2=new LinearLayout(this); daysRow2.setOrientation(LinearLayout.HORIZONTAL); daysRow2.setGravity(android.view.Gravity.CENTER);
        daysRow2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));
        boolean[] recDays=java.util.Arrays.copyOf(step.repeatDays,7);
        String[] dayLabels={"M","T","W","T","F","S","S"};
        int chipSz=(int)(34*density);
        TextView[] dayChips=new TextView[7];
        for(int d2=0;d2<7;d2++){final int di=d2;
            TextView chip=new TextView(this); chip.setText(dayLabels[d2]); chip.setTextSize(11); chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextColor(recDays[di]?0xFFFFFFFF:0xFF6B7280);
            chip.setBackground(getDrawable(recDays[di]?R.drawable.circle_primary:R.drawable.circle_bg3));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(chipSz,chipSz); lp.setMargins(4,4,4,4); chip.setLayoutParams(lp);
            chip.setOnClickListener(x->{recDays[di]=!recDays[di];chip.setBackground(getDrawable(recDays[di]?R.drawable.circle_primary:R.drawable.circle_bg3));chip.setTextColor(recDays[di]?0xFFFFFFFF:0xFF6B7280);});
            dayChips[d2]=chip; daysRow2.addView(chip);}
        root.addView(daysRow2);
        Runnable refreshChips=()->{for(int d2=0;d2<7;d2++){boolean on=recDays[d2];dayChips[d2].setBackground(getDrawable(on?R.drawable.circle_primary:R.drawable.circle_bg3));dayChips[d2].setTextColor(on?0xFFFFFFFF:0xFF6B7280);}};
        btnRDaily.setOnClickListener(x->{java.util.Arrays.fill(recDays,true);refreshChips.run();});
        btnRWeekdays.setOnClickListener(x->{System.arraycopy(WEEKDAYS_PATTERN,0,recDays,0,7);refreshChips.run();});
        btnRWeekends.setOnClickListener(x->{System.arraycopy(WEEKENDS_PATTERN,0,recDays,0,7);refreshChips.run();});
        addPopupSpace(root,14);

        // Linked habit
        addPopupLabel(root,"LINKED HABIT");
        android.widget.Spinner spinHabit=new android.widget.Spinner(this); spinHabit.setBackground(getDrawable(R.drawable.input_bg));
        java.util.List<String> hl=new java.util.ArrayList<>(); hl.add("No habit"); java.util.List<Integer> hi=new java.util.ArrayList<>(); hi.add(0);
        for(Models.Habit h:db.habits){hl.add(h.emoji+" "+h.name);hi.add(h.id);}
        android.widget.ArrayAdapter<String> haAdapter=new android.widget.ArrayAdapter<>(this,android.R.layout.simple_spinner_item,hl);
        haAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinHabit.setAdapter(haAdapter);
        for(int j=0;j<hi.size();j++) if(hi.get(j)==step.linkedHabitId){spinHabit.setSelection(j);break;}
        spinHabit.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(int)(48*density)));
        root.addView(spinHabit);
        addPopupSpace(root,14);

        // Best Streak display
        addPopupLabel(root,"BEST STREAK");
        TextView tvStreak=new TextView(this);
        tvStreak.setText("🏆 "+step.bestStreak+" day"+(step.bestStreak!=1?"s":""));
        tvStreak.setTextColor(0xFFF59E0B); tvStreak.setTextSize(16); tvStreak.setTypeface(null,android.graphics.Typeface.BOLD);
        tvStreak.setPadding(0,4,0,4); root.addView(tvStreak);
        addPopupSpace(root,24);

        // Save button
        Button btnSave2=new Button(this); btnSave2.setText("Save Step");
        btnSave2.setTextColor(0xFFFFFFFF); btnSave2.setTextSize(15); btnSave2.setTypeface(null,android.graphics.Typeface.BOLD);
        btnSave2.setBackground(getDrawable(R.drawable.btn_primary_bg));
        LinearLayout.LayoutParams saveLp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(int)(52*density));
        saveLp.setMargins(0,0,0,8); btnSave2.setLayoutParams(saveLp);
        btnSave2.setOnClickListener(x->{
            String nm=etName2.getText().toString().trim(); if(!nm.isEmpty()) step.name=nm;
            String em=etEmoji2.getText().toString().trim(); if(!em.isEmpty()) step.emoji=em;
            step.description=etDesc2.getText().toString();
            step.durationSeconds=Math.max(0,durHolder[0]*60);
            System.arraycopy(recDays,0,step.repeatDays,0,7);
            boolean allOn=true; for(boolean bb:recDays) if(!bb){allOn=false;break;}
            boolean wk=recDays[0]&&recDays[1]&&recDays[2]&&recDays[3]&&recDays[4]&&!recDays[5]&&!recDays[6];
            boolean we=!recDays[0]&&!recDays[1]&&!recDays[2]&&!recDays[3]&&!recDays[4]&&recDays[5]&&recDays[6];
            step.recurrenceType=allOn?"daily":wk?"weekdays":we?"weekends":"custom_days";
            step.linkedHabitId=hi.get(spinHabit.getSelectedItemPosition());
            // Persist to db
            for(int idx2=0;idx2<db.routines.size();idx2++){if(db.routines.get(idx2).id==routine.id){db.routines.set(idx2,routine);break;}}
            db.save();
            popup.dismiss();
            rebuildSteps();
        });
        root.addView(btnSave2);

        // Delete step button
        Button btnDelStep=new Button(this); btnDelStep.setText("🗑  Delete Step");
        btnDelStep.setTextColor(0xFFEF4444); btnDelStep.setTextSize(14);
        btnDelStep.setBackground(getDrawable(R.drawable.btn_danger_bg));
        LinearLayout.LayoutParams delLp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(int)(48*density));
        delLp.setMargins(0,0,0,16); btnDelStep.setLayoutParams(delLp);
        btnDelStep.setOnClickListener(x->{
            new android.app.AlertDialog.Builder(this).setTitle("Delete this step?")
                .setPositiveButton("Delete",(dlg,w)->{
                    routine.steps.remove(stepIdx);
                    for(int idx2=0;idx2<db.routines.size();idx2++){if(db.routines.get(idx2).id==routine.id){db.routines.set(idx2,routine);break;}}
                    db.save(); popup.dismiss(); rebuildSteps();
                }).setNegativeButton("Cancel",null).show();
        });
        root.addView(btnDelStep);

        sv.addView(root);
        popup.setContentView(sv);
        popup.show();
    }

    // ── Popup UI helper builders ─────────────────────────────────────────────

    android.widget.EditText makePopupEditText(String text,int widthPx,int heightPx,int textSizeSp){
        android.widget.EditText et=new android.widget.EditText(this);
        et.setText(text); et.setTextColor(0xFFFFFFFF); et.setTextSize(textSizeSp);
        et.setBackground(getDrawable(R.drawable.input_bg)); et.setPadding(20,12,20,12);
        int w=widthPx>0?widthPx:LinearLayout.LayoutParams.MATCH_PARENT;
        int h=heightPx>0?heightPx:LinearLayout.LayoutParams.WRAP_CONTENT;
        et.setLayoutParams(new LinearLayout.LayoutParams(w,h));
        return et;
    }

    Button makePopupIconButton(String text,int bgRes){
        Button btn=new Button(this); btn.setText(text); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(20);
        btn.setBackground(getDrawable(bgRes)); btn.setPadding(0,0,0,0);
        float d=getResources().getDisplayMetrics().density;
        btn.setLayoutParams(new LinearLayout.LayoutParams((int)(44*d),(int)(44*d)));
        return btn;
    }

    TextView makeRecChip(String label){
        TextView tv=new TextView(this); tv.setText(label); tv.setTextSize(12);
        tv.setGravity(android.view.Gravity.CENTER); tv.setBackground(getDrawable(R.drawable.chip_bg));
        tv.setTextColor(0xFF9CA3AF); return tv;
    }

    void addPopupLabel(LinearLayout root,String text){
        TextView tv=new TextView(this); tv.setText(text); tv.setTextColor(0xFF6B7280); tv.setTextSize(10);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,6); tv.setLayoutParams(lp); root.addView(tv);
    }

    void addPopupSpace(LinearLayout root,int dpHeight){
        View v=new View(this); float d=getResources().getDisplayMetrics().density;
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(int)(dpHeight*d)));
        root.addView(v);
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
                step.durationSeconds=Math.max(0,mins*60);
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
