package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.routinely.app.R;
import com.routinely.app.data.*;

public class RoutinesFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b){
        View v=inf.inflate(R.layout.fragment_routines,c,false);
        AppData db=AppData.get(requireContext());
        buildList(v,db);
        v.findViewById(R.id.btn_new_routine).setOnClickListener(x->
            startActivity(new Intent(getActivity(),EditRoutineActivity.class)));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildList(v,AppData.get(requireContext()));}

    void buildList(View v, AppData db){
        LinearLayout list=v.findViewById(R.id.routines_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        // Filter out archived
        java.util.List<Models.Routine> visible=new java.util.ArrayList<>();
        for(Models.Routine r:db.routines) if(!r.archived) visible.add(r);
        if(visible.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No routines yet\nTap + to build your first routine");
            empty.setTextColor(0xFF6B7280); empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32,80,32,80);
            list.addView(empty); return;
        }
        for(Models.Routine r:visible){
            View item=inf.inflate(R.layout.item_routine,list,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(r.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(r.name);
            // Time range: start to start+duration
            int totalMins=r.getTotalMinutes();
            int endH=r.startHour+(r.startMinute+totalMins)/60;
            int endM=(r.startMinute+totalMins)%60;
            String startStr=fmtTime(r.startHour,r.startMinute);
            String endStr=fmtTime(endH%24,endM);
            String dur=totalMins>=60?(totalMins/60)+"h "+(totalMins%60)+"m":totalMins+"m";
            ((TextView)item.findViewById(R.id.tv_time_range)).setText(startStr+" – "+endStr+" ("+dur+")");
            ((TextView)item.findViewById(R.id.tv_days)).setText(r.getDaysString());
            ((TextView)item.findViewById(R.id.tv_category)).setText(r.category);
            // Hide category box per new design requirement
            item.findViewById(R.id.tv_category).setVisibility(View.GONE);
            ((TextView)item.findViewById(R.id.tv_duration)).setText(r.steps.size()+" steps");
            // Linked alarm
            TextView tvAlarm=item.findViewById(R.id.tv_linked_alarm);
            if(r.linkedAlarmId!=0){
                Models.Alarm a=db.findAlarm(r.linkedAlarmId);
                if(a!=null){tvAlarm.setText("⏰ "+a.getTimeString());tvAlarm.setVisibility(View.VISIBLE);}
                else tvAlarm.setVisibility(View.GONE);
            } else tvAlarm.setVisibility(View.GONE);
            // Linked habits
            int habitCount=0;
            for(Models.RoutineStep s:r.steps) if(s.linkedHabitId!=0) habitCount++;
            TextView tvHabits=item.findViewById(R.id.tv_habit_count);
            if(habitCount>0){tvHabits.setText("🌱 "+habitCount+" habits");tvHabits.setVisibility(View.VISIBLE);}
            else tvHabits.setVisibility(View.GONE);
            // Play button → start routine immediately (no preview screen)
            item.findViewById(R.id.btn_play).setOnClickListener(x->{
                if(r.steps.isEmpty()){Toast.makeText(getContext(),"Add steps first",Toast.LENGTH_SHORT).show();return;}
                Intent i=new Intent(getActivity(),RunRoutineActivity.class);
                i.putExtra("routine",r);
                i.putExtra("autoStart",true);
                startActivity(i);
            });
            // Overflow ⋮
            item.findViewById(R.id.btn_overflow).setOnClickListener(x->showOverflow(r,v,db));
            // Tap card → show step summary dialog; from there user can edit
            item.setOnClickListener(x->showStepSummary(r));
            list.addView(item);
        }
    }

    String fmtTime(int h,int m){
        String ap=h<12?"am":"pm"; int hh=h%12; if(hh==0)hh=12;
        return String.format("%d:%02d%s",hh,m,ap);
    }

    /** Show a step summary card list. Tapping a step opens the edit screen. */
    void showStepSummary(Models.Routine r) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), R.style.FullScreenDialog);
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF111827);

        // Header row
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(20, 48, 20, 12);
        header.setBackgroundColor(0xFF111827);
        TextView tvClose = new TextView(getContext()); tvClose.setText("✕");
        tvClose.setTextColor(0xFF9CA3AF); tvClose.setTextSize(20); tvClose.setPadding(8, 8, 8, 8);
        tvClose.setOnClickListener(x -> dialog.dismiss());
        header.addView(tvClose);
        LinearLayout titleCol = new LinearLayout(getContext()); titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleCol.setPadding(12, 0, 0, 0);
        TextView tvName = new TextView(getContext()); tvName.setText(r.emoji + "  " + r.name);
        tvName.setTextColor(0xFFFFFFFF); tvName.setTextSize(17); tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        titleCol.addView(tvName);
        TextView tvSub = new TextView(getContext()); tvSub.setText(r.steps.size() + " steps · " + r.getTotalMinutes() + " min");
        tvSub.setTextColor(0xFF9CA3AF); tvSub.setTextSize(12); tvSub.setPadding(0, 2, 0, 0);
        titleCol.addView(tvSub);
        header.addView(titleCol);
        // Edit button
        Button btnEdit = new Button(getContext()); btnEdit.setText("✏️ Edit");
        btnEdit.setTextColor(0xFFFFFFFF); btnEdit.setTextSize(13);
        btnEdit.setBackgroundResource(R.drawable.chip_bg);
        btnEdit.setOnClickListener(x -> { dialog.dismiss(); Intent i = new Intent(getActivity(), EditRoutineActivity.class); i.putExtra("routine", r); startActivity(i); });
        header.addView(btnEdit);
        root.addView(header);

        // Divider
        View div = new View(getContext()); div.setBackgroundColor(0xFF1F2937);
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        root.addView(div);

        // Steps list (wrapped in a scrollable container that updates when steps change)
        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout stepsList = new LinearLayout(getContext());
        stepsList.setOrientation(LinearLayout.VERTICAL);
        stepsList.setPadding(16, 8, 16, 80);

        buildSummaryStepCards(r, stepsList, dialog, tvSub);

        scroll.addView(stepsList);
        root.addView(scroll);

        // Bottom: Start Routine button
        Button btnStart = new Button(getContext()); btnStart.setText("▶  Start Routine");
        btnStart.setTextColor(0xFFFFFFFF); btnStart.setTextSize(15);
        btnStart.setBackgroundResource(R.drawable.btn_primary_bg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56 * (int)getResources().getDisplayMetrics().density);
        btnLp.setMargins(16, 8, 16, 24); btnStart.setLayoutParams(btnLp);
        btnStart.setOnClickListener(x -> {
            dialog.dismiss();
            if (r.steps.isEmpty()) { Toast.makeText(getContext(), "Add steps first", Toast.LENGTH_SHORT).show(); return; }
            Intent i = new Intent(getActivity(), RunRoutineActivity.class);
            i.putExtra("routine", r); i.putExtra("autoStart", true); startActivity(i);
        });
        root.addView(btnStart);

        dialog.setContentView(root);
        dialog.show();
    }

    /** Build (or rebuild) the step cards inside the summary list. */
    void buildSummaryStepCards(Models.Routine r, LinearLayout stepsList, android.app.Dialog parentDialog, TextView tvSub) {
        stepsList.removeAllViews();
        if (r.steps.isEmpty()) {
            TextView empty = new TextView(getContext()); empty.setText("No steps yet. Tap Edit to add steps.");
            empty.setTextColor(0xFF6B7280); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32, 64, 32, 64);
            stepsList.addView(empty);
            return;
        }
        for (int i = 0; i < r.steps.size(); i++) {
            Models.RoutineStep step = r.steps.get(i);
            LinearLayout card = new LinearLayout(getContext()); card.setOrientation(LinearLayout.HORIZONTAL);
            card.setBackgroundResource(R.drawable.card_bg); card.setGravity(android.view.Gravity.CENTER_VERTICAL);
            card.setPadding(16, 14, 16, 14);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 8); card.setLayoutParams(lp);

            // Icon
            TextView tvEmoji = new TextView(getContext()); tvEmoji.setText(step.emoji);
            tvEmoji.setTextSize(22); tvEmoji.setPadding(0, 0, 14, 0); card.addView(tvEmoji);
            // Name + duration
            LinearLayout col = new LinearLayout(getContext()); col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView tvStepName = new TextView(getContext()); tvStepName.setText(step.name);
            tvStepName.setTextColor(0xFFFFFFFF); tvStepName.setTextSize(15); col.addView(tvStepName);
            int sm = step.durationSeconds / 60;
            String durStr = sm > 0 ? sm + " min" : step.durationSeconds + "s";
            TextView tvDur = new TextView(getContext()); tvDur.setText(durStr);
            tvDur.setTextColor(0xFF9CA3AF); tvDur.setTextSize(12); col.addView(tvDur);
            card.addView(col);
            // Step number badge
            TextView tvNum = new TextView(getContext()); tvNum.setText(String.valueOf(i + 1));
            tvNum.setTextColor(0xFF6B7280); tvNum.setTextSize(12); card.addView(tvNum);

            // Tapping a step card opens the step edit popup
            final int stepIdx = i;
            card.setOnClickListener(x -> showStepEditPopup(r, stepIdx, parentDialog, stepsList, tvSub));
            stepsList.addView(card);
        }
    }

    /** Full step edit popup modal. */
    void showStepEditPopup(Models.Routine r, int stepIdx, android.app.Dialog summaryDialog, LinearLayout stepsList, TextView tvSub) {
        if (stepIdx < 0 || stepIdx >= r.steps.size()) return;
        Models.RoutineStep step = r.steps.get(stepIdx);
        AppData db = AppData.get(requireContext());

        android.app.Dialog popup = new android.app.Dialog(requireContext(), R.style.FullScreenDialog);
        ScrollView sv = new ScrollView(getContext());
        sv.setBackgroundColor(0xFF111827);
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 48, 20, 32);
        root.setBackgroundColor(0xFF111827);

        // Header
        LinearLayout hdr = new LinearLayout(getContext()); hdr.setOrientation(LinearLayout.HORIZONTAL); hdr.setGravity(android.view.Gravity.CENTER_VERTICAL);
        hdr.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView tvBack = new TextView(getContext()); tvBack.setText("✕"); tvBack.setTextColor(0xFF9CA3AF); tvBack.setTextSize(20); tvBack.setPadding(0,0,16,0); tvBack.setOnClickListener(x -> popup.dismiss()); hdr.addView(tvBack);
        TextView tvTitle = new TextView(getContext()); tvTitle.setText("Edit Step"); tvTitle.setTextColor(0xFFFFFFFF); tvTitle.setTextSize(17); tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); tvTitle.setLayoutParams(titleLp); hdr.addView(tvTitle);
        root.addView(hdr);
        addPopupSpace(root, 20);

        // Icon + Name row
        addPopupLabel(root, "STEP NAME");
        LinearLayout nameRow = new LinearLayout(getContext()); nameRow.setOrientation(LinearLayout.HORIZONTAL); nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        nameRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        android.widget.EditText etEmoji = makeEditText(step.emoji, 56, 52, 24); etEmoji.setGravity(android.view.Gravity.CENTER);
        float densityEm = getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams emojiLp = new LinearLayout.LayoutParams((int)(56*densityEm), (int)(52*densityEm)); emojiLp.setMarginEnd((int)(10*densityEm)); etEmoji.setLayoutParams(emojiLp);
        nameRow.addView(etEmoji);
        android.widget.EditText etName = makeEditText(step.name, 0, 52, 15);
        etName.setHint("Step name"); etName.setHintTextColor(0xFF6B7280);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0, (int)(52*densityEm), 1); etName.setLayoutParams(nameLp);
        // Auto-icon on name change
        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                String cur = etEmoji.getText().toString().trim();
                if (cur.isEmpty() || cur.equals("✅")) {
                    etEmoji.setText(EditRoutineActivity.autoIconForStep(etName.getText().toString()));
                }
            }
        });
        nameRow.addView(etName);
        root.addView(nameRow);
        addPopupSpace(root, 14);

        // Duration stepper
        addPopupLabel(root, "DURATION (minutes)");
        final int[] durHolder = {Math.max(0, step.durationSeconds / 60)};
        LinearLayout durRow = new LinearLayout(getContext()); durRow.setOrientation(LinearLayout.HORIZONTAL); durRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        durRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        Button btnMinus = makePopupButton("–", R.drawable.btn_secondary_bg);
        TextView tvDurVal = new TextView(getContext()); tvDurVal.setText(String.valueOf(durHolder[0])); tvDurVal.setTextColor(0xFFFFFFFF); tvDurVal.setTextSize(20); tvDurVal.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDurVal.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams durValLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1); tvDurVal.setLayoutParams(durValLp);
        Button btnPlus = makePopupButton("+", R.drawable.btn_primary_bg);
        btnMinus.setOnClickListener(x -> { if (durHolder[0] > 0) { durHolder[0]--; tvDurVal.setText(String.valueOf(durHolder[0])); } });
        btnPlus.setOnClickListener(x -> { durHolder[0]++; tvDurVal.setText(String.valueOf(durHolder[0])); });
        durRow.addView(btnMinus); durRow.addView(tvDurVal); durRow.addView(btnPlus);
        root.addView(durRow);
        addPopupSpace(root, 14);

        // Description
        addPopupLabel(root, "DESCRIPTION");
        android.widget.EditText etDesc = makeEditText(step.description, 0, -1, 14); etDesc.setHint("Add a description..."); etDesc.setHintTextColor(0xFF6B7280); etDesc.setMinLines(3);
        etDesc.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(etDesc);
        addPopupSpace(root, 14);

        // Recurrence
        addPopupLabel(root, "RECURRENCE");
        // Quick-select
        LinearLayout quickRow = new LinearLayout(getContext()); quickRow.setOrientation(LinearLayout.HORIZONTAL);
        quickRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView btnRDaily = makeRecChip("Daily"); LinearLayout.LayoutParams qLp = new LinearLayout.LayoutParams(0,36,1); qLp.setMarginEnd(6); btnRDaily.setLayoutParams(qLp);
        TextView btnRWeekdays = makeRecChip("Weekdays"); LinearLayout.LayoutParams qLp2 = new LinearLayout.LayoutParams(0,36,1); qLp2.setMarginEnd(6); btnRWeekdays.setLayoutParams(qLp2);
        TextView btnRWeekends = makeRecChip("Weekends"); btnRWeekends.setLayoutParams(new LinearLayout.LayoutParams(0,36,1));
        quickRow.addView(btnRDaily); quickRow.addView(btnRWeekdays); quickRow.addView(btnRWeekends);
        root.addView(quickRow);
        addPopupSpace(root, 8);

        // Day toggles
        LinearLayout daysRow = new LinearLayout(getContext()); daysRow.setOrientation(LinearLayout.HORIZONTAL); daysRow.setGravity(android.view.Gravity.CENTER);
        daysRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        boolean[] recDays = java.util.Arrays.copyOf(step.repeatDays, 7);
        String[] dayLabels = {"M","T","W","T","F","S","S"};
        float density = getResources().getDisplayMetrics().density;
        int chipSz = (int)(34*density);
        TextView[] dayChips = new TextView[7];
        for (int d = 0; d < 7; d++) {
            final int di = d;
            TextView chip = new TextView(getContext()); chip.setText(dayLabels[d]); chip.setTextSize(11); chip.setGravity(android.view.Gravity.CENTER);
            chip.setTextColor(recDays[di]?0xFFFFFFFF:0xFF6B7280);
            chip.setBackground(requireContext().getDrawable(recDays[di]?R.drawable.circle_primary:R.drawable.circle_bg3));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(chipSz, chipSz); lp.setMargins(4,4,4,4); chip.setLayoutParams(lp);
            chip.setOnClickListener(x -> { recDays[di]=!recDays[di]; chip.setBackground(requireContext().getDrawable(recDays[di]?R.drawable.circle_primary:R.drawable.circle_bg3)); chip.setTextColor(recDays[di]?0xFFFFFFFF:0xFF6B7280); });
            dayChips[d] = chip; daysRow.addView(chip);
        }
        root.addView(daysRow);
        // Quick-select logic
        Runnable refreshChips = () -> {
            for (int d = 0; d < 7; d++) { boolean on = recDays[d]; dayChips[d].setBackground(requireContext().getDrawable(on?R.drawable.circle_primary:R.drawable.circle_bg3)); dayChips[d].setTextColor(on?0xFFFFFFFF:0xFF6B7280); }
        };
        btnRDaily.setOnClickListener(x -> { java.util.Arrays.fill(recDays,true); refreshChips.run(); });
        btnRWeekdays.setOnClickListener(x -> { boolean[] wk={true,true,true,true,true,false,false}; System.arraycopy(wk,0,recDays,0,7); refreshChips.run(); });
        btnRWeekends.setOnClickListener(x -> { boolean[] we={false,false,false,false,false,true,true}; System.arraycopy(we,0,recDays,0,7); refreshChips.run(); });
        addPopupSpace(root, 14);

        // Linked habit
        addPopupLabel(root, "LINKED HABIT");
        android.widget.Spinner spinHabit = new android.widget.Spinner(getContext()); spinHabit.setBackground(requireContext().getDrawable(R.drawable.input_bg));
        java.util.List<String> hl = new java.util.ArrayList<>(); hl.add("No habit"); java.util.List<Integer> hi = new java.util.ArrayList<>(); hi.add(0);
        for (Models.Habit h : db.habits) { hl.add(h.emoji+" "+h.name); hi.add(h.id); }
        android.widget.ArrayAdapter<String> haAdapter = new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, hl);
        haAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinHabit.setAdapter(haAdapter);
        for (int j=0;j<hi.size();j++) if(hi.get(j)==step.linkedHabitId){ spinHabit.setSelection(j); break; }
        LinearLayout.LayoutParams spinLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(48*density));
        spinHabit.setLayoutParams(spinLp); root.addView(spinHabit);
        addPopupSpace(root, 14);

        // Best Streak display
        addPopupLabel(root, "BEST STREAK");
        TextView tvStreak = new TextView(getContext());
        tvStreak.setText("🏆 " + step.bestStreak + " day" + (step.bestStreak != 1 ? "s" : ""));
        tvStreak.setTextColor(0xFFF59E0B); tvStreak.setTextSize(16); tvStreak.setTypeface(null, android.graphics.Typeface.BOLD);
        tvStreak.setPadding(0, 4, 0, 4); root.addView(tvStreak);
        addPopupSpace(root, 24);

        // Save button
        Button btnSave = new Button(getContext()); btnSave.setText("Save Step");
        btnSave.setTextColor(0xFFFFFFFF); btnSave.setTextSize(15); btnSave.setTypeface(null, android.graphics.Typeface.BOLD);
        btnSave.setBackground(requireContext().getDrawable(R.drawable.btn_primary_bg));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(52*density)); saveLp.setMargins(0,0,0,16); btnSave.setLayoutParams(saveLp);
        btnSave.setOnClickListener(x -> {
            String nm = etName.getText().toString().trim(); if (!nm.isEmpty()) step.name = nm;
            String em = etEmoji.getText().toString().trim(); if (!em.isEmpty()) step.emoji = em;
            step.description = etDesc.getText().toString();
            step.durationSeconds = Math.max(60, durHolder[0] * 60);
            System.arraycopy(recDays, 0, step.repeatDays, 0, 7);
            // Infer recurrenceType from pattern
            boolean allOn = true; for (boolean b : recDays) if (!b) { allOn = false; break; }
            boolean wk = recDays[0]&&recDays[1]&&recDays[2]&&recDays[3]&&recDays[4]&&!recDays[5]&&!recDays[6];
            boolean we = !recDays[0]&&!recDays[1]&&!recDays[2]&&!recDays[3]&&!recDays[4]&&recDays[5]&&recDays[6];
            step.recurrenceType = allOn?"daily":wk?"weekdays":we?"weekends":"custom_days";
            int selHabit = spinHabit.getSelectedItemPosition(); step.linkedHabitId = hi.get(selHabit);
            // Save to db
            for (int idx2=0;idx2<db.routines.size();idx2++) { if(db.routines.get(idx2).id==r.id){ db.routines.set(idx2,r); break; } }
            db.save();
            popup.dismiss();
            // Refresh summary list
            tvSub.setText(r.steps.size() + " steps · " + r.getTotalMinutes() + " min");
            buildSummaryStepCards(r, stepsList, summaryDialog, tvSub);
        });
        root.addView(btnSave);

        sv.addView(root);
        popup.setContentView(sv);
        popup.show();
    }

    // ── Helper builders for programmatic popup UI ──────────────────────────

    android.widget.EditText makeEditText(String text, int widthDp, int heightDp, int textSizeSp) {
        android.widget.EditText et = new android.widget.EditText(getContext());
        et.setText(text); et.setTextColor(0xFFFFFFFF); et.setTextSize(textSizeSp);
        et.setBackground(requireContext().getDrawable(R.drawable.input_bg));
        et.setPadding(20, 12, 20, 12);
        float d = getResources().getDisplayMetrics().density;
        int w = widthDp > 0 ? (int)(widthDp*d) : LinearLayout.LayoutParams.MATCH_PARENT;
        int h = heightDp > 0 ? (int)(heightDp*d) : LinearLayout.LayoutParams.WRAP_CONTENT;
        et.setLayoutParams(new LinearLayout.LayoutParams(w, h));
        return et;
    }

    Button makePopupButton(String text, int bgRes) {
        Button btn = new Button(getContext()); btn.setText(text); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(20);
        btn.setBackground(requireContext().getDrawable(bgRes)); btn.setPadding(0,0,0,0);
        float d = getResources().getDisplayMetrics().density;
        btn.setLayoutParams(new LinearLayout.LayoutParams((int)(44*d),(int)(44*d)));
        return btn;
    }

    TextView makeRecChip(String label) {
        TextView tv = new TextView(getContext()); tv.setText(label); tv.setTextSize(12);
        tv.setGravity(android.view.Gravity.CENTER); tv.setBackground(requireContext().getDrawable(R.drawable.chip_bg));
        tv.setTextColor(0xFF9CA3AF); return tv;
    }

    void addPopupLabel(LinearLayout root, String text) {
        TextView tv = new TextView(getContext()); tv.setText(text); tv.setTextColor(0xFF6B7280); tv.setTextSize(10);
        tv.setLetterSpacing(0.08f); tv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,0,0,6); tv.setLayoutParams(lp); root.addView(tv);
    }

    void addPopupSpace(LinearLayout root, int dpHeight) {
        View v = new View(getContext()); float d = getResources().getDisplayMetrics().density;
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(dpHeight*d)));
        root.addView(v);
    }

    void showOverflow(Models.Routine r, View fragmentView, AppData db){
        BottomSheetDialog sheet=new BottomSheetDialog(requireContext());
        LinearLayout layout=new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.card_bg);
        layout.setPadding(0,16,0,32);

        // Settings
        addSheetRow(layout,"⚙️  Settings",false,null,()->{ sheet.dismiss(); Intent i=new Intent(getActivity(),EditRoutineActivity.class); i.putExtra("routine",r); startActivity(i); });
        // Duplicate
        addSheetRow(layout,"📋  Duplicate",false,null,()->{ sheet.dismiss(); duplicateRoutine(r,db); buildList(fragmentView,db); });
        // Archive
        addSheetRow(layout,"🗄  Archive",false,null,()->{ sheet.dismiss(); r.archived=true; db.save(); buildList(fragmentView,db); });
        // Edit Task
        addSheetRow(layout,"✏️  Edit Task",false,null,()->{ sheet.dismiss(); Intent i=new Intent(getActivity(),EditRoutineActivity.class); i.putExtra("routine",r); startActivity(i); });
        // Share
        addSheetRow(layout,"↗️  Share Your Routine",false,null,()->{ sheet.dismiss(); shareRoutine(r); });
        // Show Suggestions toggle
        addSheetRow(layout,"🔍  Show Suggestions",true,r.showSuggestions,()->{
            r.showSuggestions=!r.showSuggestions; db.save();
        });
        // Delete
        addSheetRow(layout,"🗑  Delete",true,null,()->{ sheet.dismiss();
            new android.app.AlertDialog.Builder(getContext()).setTitle("Delete routine?")
                .setPositiveButton("Delete",(d,w)->{db.routines.removeIf(rt->rt.id==r.id);db.save();buildList(fragmentView,db);})
                .setNegativeButton("Cancel",null).show();
        });

        sheet.setContentView(layout); sheet.show();
    }

    interface Action { void run(); }

    void addSheetRow(LinearLayout layout, String label, boolean withToggle, Boolean toggleState, Action action){
        LinearLayout row=new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding(28,24,28,24);
        TextView tv=new TextView(getContext()); tv.setText(label);
        boolean isDelete=label.contains("Delete");
        tv.setTextColor(isDelete?0xFFEF4444:0xFFFFFFFF); tv.setTextSize(15);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
        row.addView(tv);
        if(withToggle&&toggleState!=null){
            Switch sw=new Switch(getContext()); sw.setChecked(toggleState);
            sw.setThumbTintList(android.content.res.ColorStateList.valueOf(0xFFF97316));
            sw.setTrackTintList(android.content.res.ColorStateList.valueOf(0xFF3F3F46));
            sw.setOnCheckedChangeListener((b,c)->action.run());
            row.addView(sw);
        } else if(!withToggle||toggleState==null){
            row.setOnClickListener(v->action.run());
        }
        layout.addView(row);
    }

    void duplicateRoutine(Models.Routine src, AppData db){
        Models.Routine c=new Models.Routine();
        c.id=db.newId(); c.name=src.name+" (Copy)"; c.emoji=src.emoji;
        c.category=src.category; c.startHour=src.startHour; c.startMinute=src.startMinute;
        c.repeatDays=java.util.Arrays.copyOf(src.repeatDays,7);
        for(Models.RoutineStep s:src.steps){
            Models.RoutineStep sc=new Models.RoutineStep();
            sc.id=db.newId(); sc.name=s.name; sc.emoji=s.emoji; sc.description=s.description;
            sc.durationSeconds=s.durationSeconds; c.steps.add(sc);
        }
        db.routines.add(c); db.save();
    }

    void shareRoutine(Models.Routine r){
        StringBuilder sb=new StringBuilder();
        sb.append(r.emoji).append(" ").append(r.name).append("\n");
        sb.append("⏰ ").append(r.getTimeString()).append(" | ").append(r.getDaysString()).append("\n\n");
        for(int i=0;i<r.steps.size();i++) sb.append((i+1)+". "+r.steps.get(i).emoji+" "+r.steps.get(i).name+"\n");
        Intent share=new Intent(Intent.ACTION_SEND); share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT,sb.toString());
        startActivity(Intent.createChooser(share,"Share Routine"));
    }
}
