package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.receivers.HabitNotificationReceiver;
import com.routinely.app.data.*;
import java.util.*;

public class EditHabitActivity extends AppCompatActivity {
    AppData db;
    Models.Habit habit;
    boolean isNew = false;
    String[] DAYS = {"S", "M", "T", "W", "T", "F", "S"};
    List<int[]> reminderTimes = new ArrayList<>(); // {hour, minute}
    LinearLayout remindersContainer;
    // Goal type: 0=reps, 1=measurement, 2=time
    int goalType = 0;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_edit_habit);
        db = AppData.get(this);
        Models.Habit incoming = (Models.Habit) getIntent().getSerializableExtra("habit");
        if (incoming == null) {
            isNew = true;
            habit = new Models.Habit();
            habit.id = db.newId();
            habit.reminderEnabled = false;
        } else {
            habit = incoming;
        }
        // Copy reminder times
        if (habit.reminderTimes != null) reminderTimes = new ArrayList<>(habit.reminderTimes);
        else if (habit.reminderEnabled) reminderTimes.add(new int[]{habit.reminderHour, habit.reminderMinute});
        setup();
    }

    void setup() {
        ((TextView) findViewById(R.id.tv_title)).setText(isNew ? "New Habit" : "Edit Habit");
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        EditText etEmoji = findViewById(R.id.et_emoji);
        EditText etName = findViewById(R.id.et_name);
        etEmoji.setText(habit.emoji);
        etName.setText(habit.name);

        // Auto-generate icon when name changes
        etName.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String icon = autoIconForHabit(s.toString().trim());
                if (!icon.isEmpty()) etEmoji.setText(icon);
            }
            public void afterTextChanged(android.text.Editable s) {}
        });

        // Identity template
        EditText etWhen = findViewById(R.id.et_identity_when);
        EditText etWhere = findViewById(R.id.et_identity_where);
        EditText etResult = findViewById(R.id.et_identity_result);
        if (habit.identityWhen != null) etWhen.setText(habit.identityWhen);
        if (habit.identityWhere != null) etWhere.setText(habit.identityWhere);
        if (habit.identityResult != null) etResult.setText(habit.identityResult);

        // Day toggles (S M T W T F S)
        LinearLayout daysRow = findViewById(R.id.days_row);
        daysRow.removeAllViews();
        for (int i = 0; i < 7; i++) {
            final int idx = i;
            TextView chip = new TextView(this);
            chip.setText(DAYS[i]);
            chip.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
            chip.setTextColor(0xFFFFFFFF);
            chip.setTextSize(13);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(getDrawable(habit.repeatDays[i] ? R.drawable.chip_bg_active : R.drawable.chip_bg));
            chip.setOnClickListener(v -> {
                habit.repeatDays[idx] = !habit.repeatDays[idx];
                chip.setBackground(getDrawable(habit.repeatDays[idx] ? R.drawable.chip_bg_active : R.drawable.chip_bg));
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(dpToPx(2), 0, dpToPx(2), 0);
            chip.setLayoutParams(lp);
            daysRow.addView(chip);
        }

        // Quick selects: Daily / Weekdays / Weekends
        findViewById(R.id.btn_daily).setOnClickListener(v -> {
            Arrays.fill(habit.repeatDays, true);
            refreshDayChips(daysRow);
        });
        findViewById(R.id.btn_weekdays).setOnClickListener(v -> {
            habit.repeatDays = new boolean[]{true, true, true, true, true, false, false};
            refreshDayChips(daysRow);
        });
        findViewById(R.id.btn_weekends).setOnClickListener(v -> {
            habit.repeatDays = new boolean[]{false, false, false, false, false, true, true};
            refreshDayChips(daysRow);
        });

        // Reminders
        Switch swRem = findViewById(R.id.sw_reminder);
        swRem.setChecked(habit.reminderEnabled);
        View remSection = findViewById(R.id.reminders_section);
        remSection.setVisibility(habit.reminderEnabled ? View.VISIBLE : View.GONE);
        swRem.setOnCheckedChangeListener((btn, checked) -> {
            habit.reminderEnabled = checked;
            remSection.setVisibility(checked ? View.VISIBLE : View.GONE);
        });

        remindersContainer = findViewById(R.id.reminders_list);
        rebuildRemindersList();

        findViewById(R.id.btn_add_reminder).setOnClickListener(v -> {
            showTimePickerDialog(-1, 8, 0);
        });

        // Routine dropdown
        Spinner rSpin = findViewById(R.id.spinner_routine);
        List<String> rLabels = new ArrayList<>();
        rLabels.add("No routine");
        List<Integer> rIds = new ArrayList<>();
        rIds.add(0);
        for (Models.Routine r : db.routines) { rLabels.add(r.emoji + " " + r.name); rIds.add(r.id); }
        ArrayAdapter<String> ra = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, rLabels);
        ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rSpin.setAdapter(ra);
        for (int i = 0; i < rIds.size(); i++) if (rIds.get(i) == habit.linkedRoutineId) { rSpin.setSelection(i); break; }
        final List<Integer> routineIds = rIds;

        // Goal type selector
        setupGoalType();

        // Daily target
        EditText etTarget = findViewById(R.id.et_daily_target);
        etTarget.setText(String.valueOf(Math.max(1, habit.dailyTarget)));

        // Delete button
        View btnDel = findViewById(R.id.btn_delete);
        btnDel.setVisibility(isNew ? View.GONE : View.VISIBLE);
        btnDel.setOnClickListener(v -> new android.app.AlertDialog.Builder(this)
            .setTitle("Delete habit?")
            .setPositiveButton("Delete", (d, w) -> { db.habits.removeIf(h -> h.id == habit.id); db.save(); finish(); })
            .setNegativeButton("Cancel", null).show());

        // Save
        EditText etNameFinal = etName;
        EditText etEmojiFinal = etEmoji;
        EditText etTargetFinal = etTarget;
        EditText etWhenFinal = etWhen;
        EditText etWhereFinal = etWhere;
        EditText etResultFinal = etResult;
        findViewById(R.id.btn_save).setOnClickListener(v -> {
            String name = etNameFinal.getText().toString().trim();
            if (name.isEmpty()) { etNameFinal.setError("Name required"); return; }
            habit.name = name;
            habit.emoji = etEmojiFinal.getText().toString().trim();
            if (habit.emoji.isEmpty()) habit.emoji = "\uD83D\uDCA7";
            habit.identityWhen = etWhenFinal.getText().toString().trim();
            habit.identityWhere = etWhereFinal.getText().toString().trim();
            habit.identityResult = etResultFinal.getText().toString().trim();
            habit.reminderEnabled = swRem.isChecked();
            habit.reminderTimes = new ArrayList<>(reminderTimes);
            if (!reminderTimes.isEmpty()) {
                habit.reminderHour = reminderTimes.get(0)[0];
                habit.reminderMinute = reminderTimes.get(0)[1];
            }
            habit.linkedRoutineId = routineIds.get(rSpin.getSelectedItemPosition());
            habit.goalType = goalType;
            try { habit.dailyTarget = Math.max(1, Integer.parseInt(etTargetFinal.getText().toString())); } catch (Exception ignored) {}
            if (isNew) {
                db.habits.add(habit);
            } else {
                for (int i = 0; i < db.habits.size(); i++) {
                    if (db.habits.get(i).id == habit.id) { db.habits.set(i, habit); break; }
                }
            }
            db.save();
            HabitNotificationReceiver.cancel(this, habit.id);
            if (habit.reminderEnabled) HabitNotificationReceiver.schedule(this, habit);
            Toast.makeText(this, "Habit saved!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    void setupGoalType() {
        goalType = habit.goalType;
        TextView btnReps = findViewById(R.id.btn_goal_reps);
        TextView btnMeasure = findViewById(R.id.btn_goal_measure);
        TextView btnTime = findViewById(R.id.btn_goal_time);
        View[] btns = {btnReps, btnMeasure, btnTime};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            btns[i].setOnClickListener(v -> {
                goalType = idx;
                for (int j = 0; j < 3; j++)
                    btns[j].setBackgroundResource(j == idx ? R.drawable.chip_bg_active : R.drawable.chip_bg);
                updateGoalLabel();
            });
        }
        for (int j = 0; j < 3; j++)
            btns[j].setBackgroundResource(j == goalType ? R.drawable.chip_bg_active : R.drawable.chip_bg);
        updateGoalLabel();
    }

    void updateGoalLabel() {
        TextView tvGoalLabel = findViewById(R.id.tv_goal_label);
        switch (goalType) {
            case 0: tvGoalLabel.setText("Reps (times per day)"); break;
            case 1: tvGoalLabel.setText("Measurement (e.g. liters, pages, km)"); break;
            case 2: tvGoalLabel.setText("Time (e.g. minutes, hours)"); break;
        }
    }

    void refreshDayChips(LinearLayout daysRow) {
        for (int i = 0; i < daysRow.getChildCount() && i < 7; i++) {
            View chip = daysRow.getChildAt(i);
            chip.setBackgroundResource(habit.repeatDays[i] ? R.drawable.chip_bg_active : R.drawable.chip_bg);
        }
    }

    void rebuildRemindersList() {
        remindersContainer.removeAllViews();
        for (int i = 0; i < reminderTimes.size(); i++) {
            final int idx = i;
            int[] t = reminderTimes.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dpToPx(8), 0, dpToPx(8));

            TextView tvTime = new TextView(this);
            tvTime.setText(formatTime(t[0], t[1]));
            tvTime.setTextColor(0xFF1A1A2E);
            tvTime.setTextSize(15);
            tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTime.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tvTime.setOnClickListener(v -> showTimePickerDialog(idx, t[0], t[1]));
            row.addView(tvTime);

            TextView btnRemove = new TextView(this);
            btnRemove.setText("\u2715");
            btnRemove.setTextColor(0xFFEF4444);
            btnRemove.setTextSize(16);
            btnRemove.setPadding(dpToPx(8), 0, 0, 0);
            btnRemove.setOnClickListener(v -> {
                reminderTimes.remove(idx);
                rebuildRemindersList();
            });
            row.addView(btnRemove);

            // Divider
            View div = new View(this);
            div.setBackgroundColor(0xFFE5E7EB);
            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.addView(row);
            LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            div.setLayoutParams(divLp);
            wrapper.addView(div);
            remindersContainer.addView(wrapper);
        }
    }

    void showTimePickerDialog(int replaceIdx, int initHour, int initMinute) {
        android.app.TimePickerDialog tpd = new android.app.TimePickerDialog(this, (view, hour, minute) -> {
            if (replaceIdx >= 0 && replaceIdx < reminderTimes.size()) {
                reminderTimes.set(replaceIdx, new int[]{hour, minute});
            } else {
                reminderTimes.add(new int[]{hour, minute});
            }
            rebuildRemindersList();
        }, initHour, initMinute, false);
        tpd.show();
    }

    String formatTime(int h, int m) {
        String ap = h < 12 ? "AM" : "PM";
        int hh = h % 12; if (hh == 0) hh = 12;
        return String.format(Locale.US, "%d:%02d %s", hh, m, ap);
    }

    String autoIconForHabit(String name) {
        String n = name.toLowerCase(Locale.US);
        if (n.contains("water") || n.contains("drink")) return "\uD83D\uDCA7";
        if (n.contains("run") || n.contains("jog")) return "\uD83C\uDFC3";
        if (n.contains("gym") || n.contains("workout") || n.contains("exercise")) return "\uD83C\uDFCB\uFE0F";
        if (n.contains("read") || n.contains("book")) return "\uD83D\uDCDA";
        if (n.contains("meditat") || n.contains("mindful")) return "\uD83E\uDDD8";
        if (n.contains("sleep") || n.contains("bed")) return "\uD83D\uDCA4";
        if (n.contains("diet") || n.contains("eat") || n.contains("food")) return "\uD83E\uDD57";
        if (n.contains("journal") || n.contains("write") || n.contains("diary")) return "\u270D\uFE0F";
        if (n.contains("yoga") || n.contains("stretch")) return "\uD83E\uDDD8";
        if (n.contains("walk") || n.contains("step")) return "\uD83D\uDEB6";
        if (n.contains("vitamin") || n.contains("supplement") || n.contains("pill")) return "\uD83D\uDC8A";
        if (n.contains("cod") || n.contains("program") || n.contains("learn")) return "\uD83D\uDCBB";
        if (n.contains("gratitude") || n.contains("grateful")) return "\uD83D\uDE4F";
        if (n.contains("cold") || n.contains("shower")) return "\uD83D\uDEBF";
        if (n.contains("call") || n.contains("friend") || n.contains("family")) return "\uD83D\uDCDE";
        return "";
    }

    int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
