package com.routinely.app.ui;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.*;
import android.view.animation.ScaleAnimation;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HabitsFragment extends Fragment {
    int curTab = 0;
    int sectionIdx = 0;
    // Date-row calendar state: selectedDate as "yyyy-MM-dd"
    String selectedDate;
    boolean selectedIsToday;

    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_habits, c, false);
        AppData db = AppData.get(requireContext());
        if (getArguments() != null) sectionIdx = getArguments().getInt("section", 0);
        // Init selected date to today
        selectedDate = todayStr();
        selectedIsToday = true;
        setupSectionTabs(v, db);
        applySection(v, db);
        setupTabs(v, db);
        buildDateRow(v, db);
        buildHabits(v, db);
        v.findViewById(R.id.btn_new_habit).setOnClickListener(x ->
            startActivity(new Intent(getActivity(), EditHabitActivity.class)));
        return v;
    }

    @Override public void onResume() {
        super.onResume();
        View v = getView();
        if (v != null) {
            AppData db = AppData.get(requireContext());
            buildHabits(v, db);
            buildDateRow(v, db);
            if (sectionIdx == 1) buildProgress(v, db);
        }
    }

    // ── Section tabs ──────────────────────────────────────────────────────────

    void setupSectionTabs(View v, AppData db) {
        int[] tabIds = {R.id.section_tab_habits, R.id.section_tab_progress, R.id.section_tab_mindset};
        for (int i = 0; i < tabIds.length; i++) {
            final int idx = i;
            v.findViewById(tabIds[i]).setOnClickListener(x -> {
                sectionIdx = idx;
                applySection(v, db);
                if (idx == 1) buildProgress(v, AppData.get(requireContext()));
                if (idx == 2) loadMindsetSection(v, AppData.get(requireContext()));
            });
        }
    }

    void applySection(View v, AppData db) {
        int[] tabIds = {R.id.section_tab_habits, R.id.section_tab_progress, R.id.section_tab_mindset};
        for (int j = 0; j < tabIds.length; j++) {
            v.findViewById(tabIds[j]).setBackgroundResource(j == sectionIdx ? R.drawable.chip_bg_active : R.drawable.chip_bg);
            ((TextView) v.findViewById(tabIds[j])).setTextColor(j == sectionIdx ? 0xFFFFFFFF : 0xFF9CA3AF);
        }
        v.findViewById(R.id.habits_section).setVisibility(sectionIdx == 0 ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.progress_section).setVisibility(sectionIdx == 1 ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.mindset_section).setVisibility(sectionIdx == 2 ? View.VISIBLE : View.GONE);
        v.findViewById(R.id.btn_new_habit).setVisibility(sectionIdx == 0 ? View.VISIBLE : View.GONE);
        if (sectionIdx == 2) loadMindsetSection(v, db);
    }

    // ── Habit sub-tabs (All / Remaining / Streaks) ────────────────────────────

    void setupTabs(View v, AppData db) {
        int[] tabIds = {R.id.tab_all, R.id.tab_today, R.id.tab_streaks};
        for (int i = 0; i < tabIds.length; i++) {
            final int idx = i;
            v.findViewById(tabIds[i]).setOnClickListener(x -> {
                curTab = idx;
                for (int j = 0; j < tabIds.length; j++)
                    v.findViewById(tabIds[j]).setBackgroundResource(j == idx ? R.drawable.chip_bg_active : R.drawable.chip_bg);
                buildHabits(v, AppData.get(requireContext()));
            });
        }
        v.findViewById(tabIds[0]).setBackgroundResource(R.drawable.chip_bg_active);
    }

    // ── Date-row calendar ─────────────────────────────────────────────────────

    void buildDateRow(View v, AppData db) {
        LinearLayout row = v.findViewById(R.id.date_row);
        row.removeAllViews();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -14); // start 14 days in the past
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (int i = 0; i < 29; i++) { // 14 past + today + 14 future
            final String dateStr = sdf.format(cal.getTime());
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH);
            boolean isSelected = dateStr.equals(selectedDate);
            boolean isToday = dateStr.equals(todayStr());

            LinearLayout cell = new LinearLayout(getContext());
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(android.view.Gravity.CENTER);
            int cellW = dpToPx(60);
            LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(cellW, ViewGroup.LayoutParams.WRAP_CONTENT);
            cellLp.setMargins(dpToPx(3), 0, dpToPx(3), 0);
            cell.setLayoutParams(cellLp);
            cell.setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(8));

            if (isSelected) {
                cell.setBackgroundResource(R.drawable.chip_bg_active);
            } else if (isToday) {
                cell.setBackgroundResource(R.drawable.chip_bg);
            } else {
                cell.setBackgroundColor(0x00000000);
            }

            TextView tvDay = new TextView(getContext());
            tvDay.setText(dayNames[dayOfWeek]);
            tvDay.setTextSize(11);
            tvDay.setGravity(android.view.Gravity.CENTER);
            tvDay.setTextColor(isSelected ? 0xFFFFFFFF : (isToday ? 0xFF6755C8 : 0xFF9CA3AF));

            TextView tvNum = new TextView(getContext());
            tvNum.setText(String.valueOf(dayOfMonth));
            tvNum.setTextSize(isSelected ? 18 : 15);
            tvNum.setGravity(android.view.Gravity.CENTER);
            tvNum.setTextColor(isSelected ? 0xFFFFFFFF : (isToday ? 0xFF6755C8 : 0xFF1A1A2E));
            if (isSelected || isToday) tvNum.setTypeface(null, Typeface.BOLD);

            // Dot indicator if any habit was completed on this date
            View dot = new View(getContext());
            int dotColor = isHabitDoneOnDate(db, dateStr) ? (isSelected ? 0xFFFFFFFF : 0xFF10B981) : 0x00000000;
            dot.setBackgroundColor(dotColor);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dpToPx(5), dpToPx(5));
            dotLp.setMargins(0, dpToPx(3), 0, 0);
            dot.setLayoutParams(dotLp);

            cell.addView(tvDay);
            cell.addView(tvNum);
            cell.addView(dot);

            cell.setOnClickListener(x -> {
                selectedDate = dateStr;
                selectedIsToday = dateStr.equals(todayStr());
                buildDateRow(v, AppData.get(requireContext()));
                buildHabits(v, AppData.get(requireContext()));
            });
            row.addView(cell);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        // Auto-scroll to today (position 14)
        HorizontalScrollView hsv = v.findViewById(R.id.date_row_scroll);
        hsv.post(() -> {
            int todayPos = 14;
            int scrollTo = Math.max(0, (dpToPx(60) + dpToPx(6)) * todayPos - dpToPx(120));
            hsv.smoothScrollTo(scrollTo, 0);
        });
    }

    boolean isHabitDoneOnDate(AppData db, String dateStr) {
        boolean isToday = dateStr.equals(todayStr());
        for (Models.Habit h : db.habits) {
            if (isToday && h.completedToday) return true;
            if (!isToday && h.logs != null) {
                for (Models.HabitLog log : h.logs) {
                    if (dateStr.equals(log.date) && log.count > 0) return true;
                }
            }
        }
        return false;
    }

    // ── Habit list ────────────────────────────────────────────────────────────

    void buildHabits(View v, AppData db) {
        LinearLayout list = v.findViewById(R.id.habits_list);
        list.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(getContext());

        // Update today's stats (always reflect today regardless of selected date)
        int bestStreak = 0, done = 0, total = db.habits.size();
        for (Models.Habit h : db.habits) {
            bestStreak = Math.max(bestStreak, h.streak);
            if (h.completedToday) done++;
        }
        ((TextView) v.findViewById(R.id.tv_score)).setText(total > 0 ? (done * 100 / total) + "%" : "0%");
        ((TextView) v.findViewById(R.id.tv_streak)).setText(bestStreak + " day streak");
        ((TextView) v.findViewById(R.id.tv_completed_count)).setText(done + "/" + total);

        if (db.habits.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No habits yet\nTap + to start building your identity");
            empty.setTextColor(0xFF6B7280);
            empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32, 48, 32, 48);
            list.addView(empty);
            return;
        }

        List<Models.Habit> sorted = new ArrayList<>(db.habits);
        if (curTab == 2) sorted.sort((a, b2) -> b2.streak - a.streak);

        for (Models.Habit h : sorted) {
            boolean doneOnDate = isDoneOnDate(h, selectedDate);
            if (curTab == 1 && doneOnDate) continue;
            View item = inf.inflate(R.layout.item_habit, list, false);
            bindHabitItem(item, h, v, db, doneOnDate);
            list.addView(item);
        }
    }

    boolean isDoneOnDate(Models.Habit h, String dateStr) {
        boolean isToday = dateStr.equals(todayStr());
        if (isToday) return h.completedToday;
        if (h.logs == null) return false;
        for (Models.HabitLog log : h.logs) {
            if (dateStr.equals(log.date) && log.count >= h.dailyTarget) return true;
        }
        return false;
    }

    void bindHabitItem(View item, Models.Habit h, View fragmentView, AppData db, boolean doneOnDate) {
        // Lightning streak icon + color based on streak length
        TextView tvIcon = item.findViewById(R.id.tv_streak_icon);
        TextView tvStreak = item.findViewById(R.id.tv_badge_streak);
        int lightningColor;
        if (h.streak >= 30) lightningColor = 0xFF3B82F6;      // electric blue
        else if (h.streak >= 8) lightningColor = 0xFFEAB308;  // bright gold
        else if (h.streak >= 4) lightningColor = 0xFFF97316;  // orange
        else lightningColor = 0xFFF59E0B;                     // soft yellow
        tvIcon.setTextColor(lightningColor);
        tvStreak.setText(String.valueOf(h.streak));
        tvStreak.setTextColor(lightningColor);

        // Name
        ((TextView) item.findViewById(R.id.tv_name)).setText(h.emoji + " " + h.name);

        // Frequency
        String freq = h.dailyTarget > 1 ? h.dailyTarget + "x / day" : "Daily";
        ((TextView) item.findViewById(R.id.tv_frequency)).setText(freq);

        // Progress row: only show if dailyTarget > 1
        View progressRow = item.findViewById(R.id.progress_row);
        if (h.dailyTarget > 1) {
            progressRow.setVisibility(View.VISIBLE);
            ProgressBar pb = item.findViewById(R.id.progress_habit);
            int progress = h.dailyTarget > 0 ? Math.min(100, h.todayCount * 100 / h.dailyTarget) : 0;
            pb.setProgress(progress);
            ((TextView) item.findViewById(R.id.tv_today_count)).setText(h.todayCount + " / " + h.dailyTarget);
            item.findViewById(R.id.btn_count_add).setOnClickListener(x -> {
                h.todayCount++;
                if (h.todayCount >= h.dailyTarget && !h.completedToday) {
                    h.completedToday = true;
                    h.streak++;
                }
                logHabitToday(h);
                db.save();
                buildHabits(fragmentView, db);
                buildDateRow(fragmentView, db);
            });
        } else {
            progressRow.setVisibility(View.GONE);
        }

        // Complete button
        TextView btnComplete = item.findViewById(R.id.btn_quick_add);
        if (doneOnDate && selectedIsToday) {
            btnComplete.setText("Done ✓");
            btnComplete.setBackgroundResource(R.drawable.chip_bg_green);
            btnComplete.setTextColor(0xFFFFFFFF);
        } else {
            btnComplete.setText("Done");
            btnComplete.setBackgroundResource(R.drawable.chip_bg_active);
            btnComplete.setTextColor(0xFFFFFFFF);
        }

        btnComplete.setOnClickListener(x -> {
            // Animate scale
            ScaleAnimation scale = new ScaleAnimation(1f, 1.15f, 1f, 1.15f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                    ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
            scale.setDuration(100);
            scale.setRepeatMode(ScaleAnimation.REVERSE);
            scale.setRepeatCount(1);
            btnComplete.startAnimation(scale);

            if (selectedIsToday) {
                h.todayCount++;
                if (!h.completedToday) {
                    h.completedToday = true;
                    h.streak++;
                }
                logHabitToday(h);
                db.save();
                buildHabits(fragmentView, db);
                buildDateRow(fragmentView, db);
            }
        });

        // Overflow
        item.findViewById(R.id.btn_overflow).setOnClickListener(x -> showHabitOverflow(h, fragmentView, db));

        // Tap card -> detail
        item.setOnClickListener(x -> {
            Intent i = new Intent(getActivity(), HabitDetailActivity.class);
            i.putExtra("habitId", h.id);
            startActivity(i);
        });
    }

    void logHabitToday(Models.Habit h) {
        String today = todayStr();
        if (h.logs == null) h.logs = new ArrayList<>();
        for (Models.HabitLog l : h.logs) {
            if (today.equals(l.date)) { l.count = h.todayCount; return; }
        }
        h.logs.add(new Models.HabitLog(today, h.todayCount));
    }

    void showHabitOverflow(Models.Habit h, View fragmentView, AppData db) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.card_bg);
        layout.setPadding(0, 16, 0, 32);
        String[] opts = {"✏️  Edit Habit", "📋  Duplicate", "📊  View Detail", "🗑  Delete"};
        for (String opt : opts) {
            TextView tv = new TextView(getContext());
            tv.setText(opt);
            tv.setTextColor(opt.contains("Delete") ? 0xFFEF4444 : 0xFF1A1A2E);
            tv.setTextSize(15);
            tv.setPadding(28, 28, 28, 28);
            tv.setOnClickListener(x -> {
                sheet.dismiss();
                if (opt.contains("Edit")) {
                    Intent i = new Intent(getActivity(), EditHabitActivity.class);
                    i.putExtra("habit", h);
                    startActivity(i);
                } else if (opt.contains("Duplicate")) {
                    Models.Habit copy = duplicateHabit(h, db);
                    db.habits.add(copy);
                    db.save();
                    buildHabits(fragmentView, AppData.get(requireContext()));
                } else if (opt.contains("Detail")) {
                    Intent i = new Intent(getActivity(), HabitDetailActivity.class);
                    i.putExtra("habitId", h.id);
                    startActivity(i);
                } else if (opt.contains("Delete")) {
                    new android.app.AlertDialog.Builder(getContext())
                        .setTitle("Delete habit?")
                        .setPositiveButton("Delete", (d, w) -> {
                            db.habits.removeIf(hb -> hb.id == h.id);
                            db.save();
                            buildHabits(fragmentView, AppData.get(requireContext()));
                            buildDateRow(fragmentView, AppData.get(requireContext()));
                        })
                        .setNegativeButton("Cancel", null).show();
                }
            });
            layout.addView(tv);
        }
        sheet.setContentView(layout);
        sheet.show();
    }

    Models.Habit duplicateHabit(Models.Habit src, AppData db) {
        Models.Habit c = new Models.Habit();
        c.id = db.newId();
        c.name = src.name + " (Copy)";
        c.emoji = src.emoji;
        c.category = src.category;
        c.repeatDays = Arrays.copyOf(src.repeatDays, 7);
        c.reminderHour = src.reminderHour;
        c.reminderMinute = src.reminderMinute;
        c.reminderEnabled = src.reminderEnabled;
        c.dailyTarget = src.dailyTarget;
        return c;
    }

    // ── Progress section ──────────────────────────────────────────────────────

    void buildProgress(View v, AppData db) {
        LinearLayout list = v.findViewById(R.id.progress_list);
        list.removeAllViews();
        if (db.habits.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No habits tracked yet.\nStart building habits to see your progress here.");
            empty.setTextColor(0xFF6B7280);
            empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32, 80, 32, 80);
            list.addView(empty);
            return;
        }
        for (Models.Habit h : db.habits) {
            LinearLayout card = new LinearLayout(getContext());
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg);
            card.setPadding(20, 16, 20, 16);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 12);
            card.setLayoutParams(lp);
            LinearLayout header = new LinearLayout(getContext());
            header.setOrientation(LinearLayout.HORIZONTAL);
            header.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView name = new TextView(getContext());
            name.setText(h.emoji + " " + h.name);
            name.setTextColor(0xFF1A1A2E);
            name.setTextSize(15);
            name.setTypeface(null, Typeface.BOLD);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            header.addView(name);
            // Lightning streak
            int sc = h.streak >= 30 ? 0xFF3B82F6 : h.streak >= 8 ? 0xFFEAB308 : h.streak >= 4 ? 0xFFF97316 : 0xFFF59E0B;
            TextView streak = new TextView(getContext());
            streak.setText("\u26A1 " + h.streak);
            streak.setTextColor(sc);
            streak.setTextSize(14);
            header.addView(streak);
            card.addView(header);
            TextView label = new TextView(getContext());
            label.setText("Today: " + h.todayCount + "/" + h.dailyTarget);
            label.setTextColor(0xFF9CA3AF);
            label.setTextSize(12);
            label.setPadding(0, 8, 0, 4);
            card.addView(label);
            ProgressBar pb = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100);
            pb.setProgress(h.dailyTarget > 0 ? Math.min(100, h.todayCount * 100 / h.dailyTarget) : 0);
            pb.setProgressDrawable(requireContext().getDrawable(R.drawable.progress_habit));
            LinearLayout.LayoutParams pblp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 12);
            pb.setLayoutParams(pblp);
            card.addView(pb);
            int total = h.logs == null ? 0 : h.logs.size();
            TextView logs = new TextView(getContext());
            logs.setText(total + " days logged");
            logs.setTextColor(0xFF9CA3AF);
            logs.setTextSize(11);
            logs.setPadding(0, 6, 0, 0);
            card.addView(logs);
            list.addView(card);
        }
    }

    // ── Mindset section ───────────────────────────────────────────────────────

    void loadMindsetSection(View v, AppData db) {
        LinearLayout container = v.findViewById(R.id.mindset_section);
        if (container.getTag() != null) return;
        container.setTag("loaded");
        buildMindsetSection(container, db);
    }

    void buildMindsetSection(LinearLayout container, AppData db) {
        container.removeAllViews();
        container.setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16));

        // Advance daily lesson if needed
        String today = todayStr();
        if (!today.equals(db.dailyLessonDate)) {
            if (!db.dailyLessonDate.isEmpty()) db.dailyLessonIndex++;
            db.dailyLessonDate = today;
            db.save();
        }
        int lessonIdx = db.dailyLessonIndex % MindsetData.DAILY_LESSONS.length;
        String lessonTitle = MindsetData.DAILY_LESSONS[lessonIdx][0];
        String lessonBody = MindsetData.DAILY_LESSONS[lessonIdx][1];
        String preview = lessonBody != null && lessonBody.contains("\n")
                ? lessonBody.substring(0, lessonBody.indexOf('\n')) : lessonBody;

        // Daily lesson card
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg_primary);
        card.setPadding(dpToPx(20), dpToPx(20), dpToPx(20), dpToPx(20));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dpToPx(16));
        card.setLayoutParams(cardLp);

        // "Daily Lesson #X" badge
        LinearLayout cardHeader = new LinearLayout(getContext());
        cardHeader.setOrientation(LinearLayout.HORIZONTAL);
        cardHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);
        cardHeader.setPadding(0, 0, 0, dpToPx(8));
        TextView tvLabel = new TextView(getContext());
        tvLabel.setText("☀️ Daily Lesson");
        tvLabel.setTextColor(0xFFFFFFFF);
        tvLabel.setTextSize(12);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        cardHeader.addView(tvLabel);
        TextView tvNum = new TextView(getContext());
        tvNum.setText("#" + (db.dailyLessonIndex + 1));
        tvNum.setTextColor(0xFFE5E7EB);
        tvNum.setTextSize(12);
        tvNum.setTypeface(null, Typeface.BOLD);
        tvNum.setBackgroundResource(R.drawable.chip_bg);
        tvNum.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        cardHeader.addView(tvNum);
        card.addView(cardHeader);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(lessonTitle);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dpToPx(8));
        card.addView(tvTitle);

        TextView tvPrev = new TextView(getContext());
        tvPrev.setText(preview != null ? preview : "");
        tvPrev.setTextColor(0xFFE5E7EB);
        tvPrev.setTextSize(13);
        tvPrev.setPadding(0, 0, 0, dpToPx(12));
        tvPrev.setLineSpacing(4, 1);
        card.addView(tvPrev);

        TextView tvTap = new TextView(getContext());
        tvTap.setText("Tap to read full lesson ›");
        tvTap.setTextColor(0xFFD0D0FF);
        tvTap.setTextSize(13);
        card.addView(tvTap);

        card.setOnClickListener(x -> showMindsetReading(lessonTitle, lessonBody));
        container.addView(card);

        // Library / Favorites tabs
        LinearLayout tabRow = new LinearLayout(getContext());
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tabRowLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(44));
        tabRowLp.setMargins(0, 0, 0, dpToPx(16));
        tabRow.setLayoutParams(tabRowLp);

        final TextView tabLib = new TextView(getContext());
        tabLib.setText("Library");
        tabLib.setTextColor(0xFFFFFFFF);
        tabLib.setTextSize(14);
        tabLib.setGravity(android.view.Gravity.CENTER);
        tabLib.setBackgroundResource(R.drawable.chip_bg_active);
        LinearLayout.LayoutParams tabLibLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        tabLibLp.setMargins(0, 0, dpToPx(8), 0);
        tabLib.setLayoutParams(tabLibLp);
        tabRow.addView(tabLib);

        final TextView tabFav = new TextView(getContext());
        tabFav.setText("⭐ Favorites");
        tabFav.setTextColor(0xFF9CA3AF);
        tabFav.setTextSize(14);
        tabFav.setGravity(android.view.Gravity.CENTER);
        tabFav.setBackgroundResource(R.drawable.chip_bg);
        tabFav.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        tabRow.addView(tabFav);
        container.addView(tabRow);

        // Library content container
        LinearLayout libContent = new LinearLayout(getContext());
        libContent.setOrientation(LinearLayout.VERTICAL);
        libContent.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buildLibraryContent(libContent, db);
        container.addView(libContent);

        // Favorites content container
        LinearLayout favContent = new LinearLayout(getContext());
        favContent.setOrientation(LinearLayout.VERTICAL);
        favContent.setVisibility(View.GONE);
        favContent.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        buildFavoritesContent(favContent, db);
        container.addView(favContent);

        tabLib.setOnClickListener(x -> {
            tabLib.setBackgroundResource(R.drawable.chip_bg_active);
            tabLib.setTextColor(0xFFFFFFFF);
            tabFav.setBackgroundResource(R.drawable.chip_bg);
            tabFav.setTextColor(0xFF9CA3AF);
            libContent.setVisibility(View.VISIBLE);
            favContent.setVisibility(View.GONE);
        });
        tabFav.setOnClickListener(x -> {
            tabFav.setBackgroundResource(R.drawable.chip_bg_active);
            tabFav.setTextColor(0xFFFFFFFF);
            tabLib.setBackgroundResource(R.drawable.chip_bg);
            tabLib.setTextColor(0xFF9CA3AF);
            favContent.setVisibility(View.VISIBLE);
            libContent.setVisibility(View.GONE);
            // Rebuild favorites each time to reflect latest state
            buildFavoritesContent(favContent, db);
        });
    }

    void buildLibraryContent(LinearLayout container, AppData db) {
        container.removeAllViews();
        for (int s = 0; s < MindsetData.LIBRARY_CATEGORIES.length; s++) {
            String catIcon = MindsetData.LIBRARY_CATEGORIES[s][0];
            String catName = MindsetData.LIBRARY_CATEGORIES[s][1];

            // Category header
            LinearLayout catHeader = new LinearLayout(getContext());
            catHeader.setOrientation(LinearLayout.HORIZONTAL);
            catHeader.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams catHLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            catHLp.setMargins(0, dpToPx(8), 0, dpToPx(8));
            catHeader.setLayoutParams(catHLp);

            TextView tvCatIcon = new TextView(getContext());
            tvCatIcon.setText(catIcon);
            tvCatIcon.setTextSize(18);
            tvCatIcon.setPadding(0, 0, dpToPx(8), 0);
            catHeader.addView(tvCatIcon);

            TextView tvCatName = new TextView(getContext());
            tvCatName.setText(catName.toUpperCase(Locale.US));
            tvCatName.setTextColor(0xFF9CA3AF);
            tvCatName.setTextSize(11);
            tvCatName.setTypeface(null, Typeface.BOLD);
            catHeader.addView(tvCatName);
            container.addView(catHeader);

            if (s >= MindsetData.LIBRARY.length) continue;
            String[][] articles = MindsetData.LIBRARY[s];
            for (String[] article : articles) {
                final String artTitle = article[0];
                final String artReadTime = article.length > 1 ? article[1] : "";
                final String artBody = article.length > 2 ? article[2] : "";
                boolean isFav = db.favoriteLessons.contains(artTitle);

                LinearLayout artCard = new LinearLayout(getContext());
                artCard.setOrientation(LinearLayout.VERTICAL);
                artCard.setBackgroundResource(R.drawable.card_bg);
                artCard.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
                LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                aLp.setMargins(0, 0, 0, dpToPx(8));
                artCard.setLayoutParams(aLp);

                // Article title row + fav button
                LinearLayout titleRow = new LinearLayout(getContext());
                titleRow.setOrientation(LinearLayout.HORIZONTAL);
                titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView artTv = new TextView(getContext());
                artTv.setText(artTitle);
                artTv.setTextColor(0xFF1A1A2E);
                artTv.setTextSize(14);
                artTv.setTypeface(null, Typeface.BOLD);
                artTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                titleRow.addView(artTv);

                final TextView favBtn = new TextView(getContext());
                favBtn.setText(isFav ? "⭐" : "☆");
                favBtn.setTextSize(18);
                favBtn.setPadding(dpToPx(8), 0, 0, 0);
                favBtn.setOnClickListener(x -> {
                    if (db.favoriteLessons.contains(artTitle)) {
                        db.favoriteLessons.remove(artTitle);
                        favBtn.setText("☆");
                    } else {
                        db.favoriteLessons.add(0, artTitle);
                        favBtn.setText("⭐");
                    }
                    db.save();
                });
                titleRow.addView(favBtn);
                artCard.addView(titleRow);

                // Read time
                if (!artReadTime.isEmpty()) {
                    TextView tvTime = new TextView(getContext());
                    tvTime.setText("📖 " + artReadTime + " read");
                    tvTime.setTextColor(0xFF9CA3AF);
                    tvTime.setTextSize(11);
                    tvTime.setPadding(0, dpToPx(4), 0, 0);
                    artCard.addView(tvTime);
                }

                artCard.setOnClickListener(x -> showMindsetReading(artTitle, artBody));
                container.addView(artCard);
            }
        }
    }

    void buildFavoritesContent(LinearLayout container, AppData db) {
        container.removeAllViews();
        if (db.favoriteLessons.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("No favorites yet\nTap ☆ on any lesson to save it here");
            empty.setTextColor(0xFF6B7280);
            empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER);
            empty.setPadding(32, 48, 32, 48);
            container.addView(empty);
            return;
        }
        // Build a lookup map: title -> body
        Map<String, String> bodyMap = new HashMap<>();
        for (String[][] section : MindsetData.LIBRARY) {
            for (String[] art : section) {
                if (art.length >= 3) bodyMap.put(art[0], art[2]);
            }
        }
        for (String favTitle : db.favoriteLessons) {
            final String body = bodyMap.get(favTitle);
            LinearLayout artCard = new LinearLayout(getContext());
            artCard.setOrientation(LinearLayout.VERTICAL);
            artCard.setBackgroundResource(R.drawable.card_bg);
            artCard.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
            LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            aLp.setMargins(0, 0, 0, dpToPx(8));
            artCard.setLayoutParams(aLp);

            LinearLayout titleRow = new LinearLayout(getContext());
            titleRow.setOrientation(LinearLayout.HORIZONTAL);
            titleRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView artTv = new TextView(getContext());
            artTv.setText(favTitle);
            artTv.setTextColor(0xFF1A1A2E);
            artTv.setTextSize(14);
            artTv.setTypeface(null, Typeface.BOLD);
            artTv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            titleRow.addView(artTv);

            TextView favBtn = new TextView(getContext());
            favBtn.setText("⭐");
            favBtn.setTextSize(18);
            favBtn.setPadding(dpToPx(8), 0, 0, 0);
            final String fTitle = favTitle;
            favBtn.setOnClickListener(x -> {
                db.favoriteLessons.remove(fTitle);
                db.save();
                buildFavoritesContent(container, db);
            });
            titleRow.addView(favBtn);
            artCard.addView(titleRow);

            artCard.setOnClickListener(x -> { if (body != null) showMindsetReading(favTitle, body); });
            container.addView(artCard);
        }
    }

    void showMindsetReading(String title, String body) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFDF6);

        LinearLayout toolbar = new LinearLayout(getContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFFFFFDF6);
        toolbar.setPadding(dpToPx(16), dpToPx(48), dpToPx(16), dpToPx(12));
        toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView tvClose = new TextView(getContext());
        tvClose.setText("✕");
        tvClose.setTextSize(20);
        tvClose.setTextColor(0xFF4B5563);
        tvClose.setPadding(8, 8, 8, 8);
        tvClose.setOnClickListener(x -> dialog.dismiss());
        toolbar.addView(tvClose);
        TextView tvBar = new TextView(getContext());
        tvBar.setText("Reading");
        tvBar.setTextColor(0xFF6755C8);
        tvBar.setTextSize(14);
        tvBar.setTypeface(null, Typeface.BOLD);
        tvBar.setPadding(dpToPx(12), 0, 0, 0);
        toolbar.addView(tvBar);
        root.addView(toolbar);

        View divider = new View(getContext());
        divider.setBackgroundColor(0xFFE5DCC8);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(divider);

        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(80));

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(Typeface.SERIF, Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, dpToPx(16));
        tvTitle.setLineSpacing(6, 1);
        content.addView(tvTitle);

        View rule = new View(getContext());
        rule.setBackgroundColor(0xFFD4A853);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(3));
        rlp.setMargins(0, 0, 0, dpToPx(16));
        rule.setLayoutParams(rlp);
        content.addView(rule);

        TextView tvBody = new TextView(getContext());
        tvBody.setText(body);
        tvBody.setTextColor(0xFF3B3225);
        tvBody.setTextSize(16);
        tvBody.setTypeface(Typeface.SERIF);
        tvBody.setLineSpacing(8, 1);
        tvBody.setLetterSpacing(0.01f);
        content.addView(tvBody);

        scroll.addView(content);
        root.addView(scroll);
        dialog.setContentView(root);
        dialog.show();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    String todayStr() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
