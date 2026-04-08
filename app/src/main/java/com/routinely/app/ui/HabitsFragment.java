package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Arrays;

public class HabitsFragment extends Fragment {
    int curTab=0;
    int sectionIdx=0;
    boolean compactMode=false;

    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b){
        View v=inf.inflate(R.layout.fragment_habits,c,false);
        AppData db=AppData.get(requireContext());
        setupSectionTabs(v,db);
        setupTabs(v,db);
        buildScorecard(v,db);
        buildHabits(v,db);
        v.findViewById(R.id.btn_new_habit).setOnClickListener(x->
            startActivity(new Intent(getActivity(),EditHabitActivity.class)));
        Switch swCompact=v.findViewById(R.id.sw_compact);
        swCompact.setChecked(compactMode);
        swCompact.setOnCheckedChangeListener((btn,chk)->{
            compactMode=chk;
            buildHabits(v,AppData.get(requireContext()));
        });
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null){AppData db=AppData.get(requireContext());buildHabits(v,db);buildScorecard(v,db);if(sectionIdx==1)buildProgress(v,db);}}

    void setupSectionTabs(View v, AppData db){
        v.findViewById(R.id.section_tab_habits).setOnClickListener(x->{
            sectionIdx=0;
            v.findViewById(R.id.section_tab_habits).setBackgroundResource(R.drawable.chip_bg_active);
            ((android.widget.TextView)v.findViewById(R.id.section_tab_habits)).setTextColor(0xFFFFFFFF);
            v.findViewById(R.id.section_tab_progress).setBackgroundResource(R.drawable.chip_bg);
            ((android.widget.TextView)v.findViewById(R.id.section_tab_progress)).setTextColor(0xFF9CA3AF);
            v.findViewById(R.id.habits_section).setVisibility(View.VISIBLE);
            v.findViewById(R.id.progress_section).setVisibility(View.GONE);
            v.findViewById(R.id.tv_compact_label).setVisibility(View.VISIBLE);
            v.findViewById(R.id.sw_compact).setVisibility(View.VISIBLE);
            v.findViewById(R.id.btn_new_habit).setVisibility(View.VISIBLE);
        });
        v.findViewById(R.id.section_tab_progress).setOnClickListener(x->{
            sectionIdx=1;
            v.findViewById(R.id.section_tab_progress).setBackgroundResource(R.drawable.chip_bg_active);
            ((android.widget.TextView)v.findViewById(R.id.section_tab_progress)).setTextColor(0xFFFFFFFF);
            v.findViewById(R.id.section_tab_habits).setBackgroundResource(R.drawable.chip_bg);
            ((android.widget.TextView)v.findViewById(R.id.section_tab_habits)).setTextColor(0xFF9CA3AF);
            v.findViewById(R.id.habits_section).setVisibility(View.GONE);
            v.findViewById(R.id.progress_section).setVisibility(View.VISIBLE);
            v.findViewById(R.id.tv_compact_label).setVisibility(View.GONE);
            v.findViewById(R.id.sw_compact).setVisibility(View.GONE);
            v.findViewById(R.id.btn_new_habit).setVisibility(View.GONE);
            buildProgress(v,AppData.get(requireContext()));
        });
    }

    void setupTabs(View v, AppData db){
        int[] tabIds={R.id.tab_all,R.id.tab_today,R.id.tab_streaks};
        for(int i=0;i<tabIds.length;i++){final int idx=i;
            v.findViewById(tabIds[i]).setOnClickListener(x->{
                curTab=idx;
                for(int j=0;j<tabIds.length;j++)
                    v.findViewById(tabIds[j]).setBackgroundResource(j==idx?R.drawable.chip_bg_active:R.drawable.chip_bg);
                buildHabits(v,AppData.get(requireContext()));
            });
        }
        v.findViewById(tabIds[0]).setBackgroundResource(R.drawable.chip_bg_active);
    }

    void buildScorecard(View v, AppData db){
        LinearLayout grid=v.findViewById(R.id.habit_grid);
        grid.removeAllViews();
        String[] dayLabels={"M","T","W","T","F","S","S"};
        // Header row
        LinearLayout headerRow=new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        TextView spacer=new TextView(getContext()); spacer.setLayoutParams(new LinearLayout.LayoutParams(0,40,3));
        headerRow.addView(spacer);
        for(String dl:dayLabels){
            TextView dt=new TextView(getContext()); dt.setText(dl); dt.setTextColor(0xFF6B7280);
            dt.setTextSize(11); dt.setGravity(android.view.Gravity.CENTER);
            dt.setLayoutParams(new LinearLayout.LayoutParams(0,40,1)); headerRow.addView(dt);}
        grid.addView(headerRow);
        int maxHabits=db.habits.isEmpty()?0:Math.min(db.habits.size(),6);
        for(int h=0;h<maxHabits;h++){
            Models.Habit habit=db.habits.get(h);
            LinearLayout row=new LinearLayout(getContext()); row.setOrientation(LinearLayout.HORIZONTAL);
            TextView name=new TextView(getContext());
            name.setText(habit.emoji+" "+habit.name); name.setTextColor(0xFFE5E7EB); name.setTextSize(12);
            name.setMaxLines(1); name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,44,3)); row.addView(name);
            for(int d=0;d<7;d++){
                final int habitIdx=h; final int dayIdx=d;
                View dot=new View(getContext());
                boolean done=(d<6)||habit.completedToday;
                dot.setBackgroundResource(done?R.drawable.circle_primary:R.drawable.circle_bg3);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,44,1);
                lp.setMargins(4,4,4,4); dot.setLayoutParams(lp);
                dot.setOnClickListener(x->{
                    if(dayIdx==6){habit.completedToday=!habit.completedToday;if(habit.completedToday)habit.streak++;else if(habit.streak>0)habit.streak--;AppData.get(requireContext()).save();buildScorecard(v,AppData.get(requireContext()));buildHabits(v,AppData.get(requireContext()));}
                });
                row.addView(dot);
            }
            grid.addView(row);
        }
    }

    void buildHabits(View v, AppData db){
        LinearLayout list=v.findViewById(R.id.habits_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        // Stats header
        int streak=0; int done=0; int total=db.habits.size();
        for(Models.Habit h:db.habits){streak=Math.max(streak,h.streak);if(h.completedToday)done++;}
        ((TextView)v.findViewById(R.id.tv_score)).setText(total>0?(done*100/total)+"%":"0%");
        ((TextView)v.findViewById(R.id.tv_streak)).setText(streak+" day streak");
        if(db.habits.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No habits yet\nTap + and start building your identity");
            empty.setTextColor(0xFF6B7280); empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32,48,32,48);
            list.addView(empty); return;
        }
        for(Models.Habit h:db.habits){
            if(curTab==1&&h.completedToday) continue;
            View item=inf.inflate(R.layout.item_habit,list,false);
            bindHabitItem(item,h,v,db);
            list.addView(item);
        }
    }

    void bindHabitItem(View item, Models.Habit h, View fragmentView, AppData db){
        // Badge streak number
        ((TextView)item.findViewById(R.id.tv_badge_streak)).setText(String.valueOf(h.streak));
        // Name
        ((TextView)item.findViewById(R.id.tv_name)).setText(h.emoji+" "+h.name);
        // Frequency
        String freq=h.dailyTarget>1?h.dailyTarget+" times/day":"Daily";
        ((TextView)item.findViewById(R.id.tv_frequency)).setText(freq);
        // Progress
        ProgressBar pb=item.findViewById(R.id.progress_habit);
        int progress=h.dailyTarget>0?Math.min(100,h.todayCount*100/h.dailyTarget):0;
        pb.setProgress(progress);
        // Today count
        ((TextView)item.findViewById(R.id.tv_today_count)).setText(h.todayCount+" times");
        // Quick add (big + button)
        item.findViewById(R.id.btn_quick_add).setOnClickListener(x->{
            h.todayCount++;
            if(h.todayCount>=h.dailyTarget&&!h.completedToday){h.completedToday=true;h.streak++;}
            logHabitToday(h);
            db.save();
            buildHabits(fragmentView,db);
            buildScorecard(fragmentView,db);
        });
        // Count add (small +)
        item.findViewById(R.id.btn_count_add).setOnClickListener(x->{
            h.todayCount++;
            if(h.todayCount>=h.dailyTarget&&!h.completedToday){h.completedToday=true;h.streak++;}
            logHabitToday(h);
            db.save();
            buildHabits(fragmentView,db);
            buildScorecard(fragmentView,db);
        });
        // Overflow ⋮
        item.findViewById(R.id.btn_overflow).setOnClickListener(x->showHabitOverflow(h,fragmentView,db));
        // Tap card → detail
        item.setOnClickListener(x->{
            Intent i=new Intent(getActivity(),HabitDetailActivity.class);
            i.putExtra("habitId",h.id); startActivity(i);
        });
    }

    void logHabitToday(Models.Habit h){
        String today=new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());
        if(h.logs==null) h.logs=new java.util.ArrayList<>();
        for(Models.HabitLog l:h.logs){ if(today.equals(l.date)){l.count=h.todayCount;return;} }
        h.logs.add(new Models.HabitLog(today,h.todayCount));
    }

    void showHabitOverflow(Models.Habit h, View fragmentView, AppData db){
        BottomSheetDialog sheet=new BottomSheetDialog(requireContext());
        LinearLayout layout=new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.card_bg);
        layout.setPadding(0,16,0,32);
        String[] opts={"✏️  Edit Habit","📋  Duplicate","📊  View Detail","🗑  Delete"};
        for(String opt:opts){
            TextView tv=new TextView(getContext());
            tv.setText(opt); tv.setTextColor(opt.contains("Delete")?0xFFEF4444:0xFFFFFFFF);
            tv.setTextSize(15); tv.setPadding(28,28,28,28);
            tv.setOnClickListener(x->{
                sheet.dismiss();
                if(opt.contains("Edit")){Intent i=new Intent(getActivity(),EditHabitActivity.class);i.putExtra("habit",h);startActivity(i);}
                else if(opt.contains("Duplicate")){
                    Models.Habit copy=duplicateHabit(h,db);
                    db.habits.add(copy); db.save();
                    buildHabits(fragmentView,AppData.get(requireContext()));
                }
                else if(opt.contains("Detail")){Intent i=new Intent(getActivity(),HabitDetailActivity.class);i.putExtra("habitId",h.id);startActivity(i);}
                else if(opt.contains("Delete")){
                    new android.app.AlertDialog.Builder(getContext()).setTitle("Delete habit?")
                        .setPositiveButton("Delete",(d,w)->{db.habits.removeIf(hb->hb.id==h.id);db.save();buildHabits(fragmentView,AppData.get(requireContext()));buildScorecard(fragmentView,AppData.get(requireContext()));})
                        .setNegativeButton("Cancel",null).show();
                }
            });
            layout.addView(tv);
        }
        sheet.setContentView(layout); sheet.show();
    }

    Models.Habit duplicateHabit(Models.Habit src, AppData db){
        Models.Habit c=new Models.Habit();
        c.id=db.newId(); c.name=src.name+" (Copy)"; c.emoji=src.emoji;
        c.category=src.category; c.repeatDays=Arrays.copyOf(src.repeatDays,7);
        c.reminderHour=src.reminderHour; c.reminderMinute=src.reminderMinute;
        c.reminderEnabled=src.reminderEnabled; c.dailyTarget=src.dailyTarget;
        return c;
    }

    void buildProgress(View v, AppData db){
        LinearLayout list=v.findViewById(R.id.progress_list); list.removeAllViews();
        if(db.habits.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No habits tracked yet.\nStart building habits to see your progress here.");
            empty.setTextColor(0xFF6B7280); empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32,80,32,80);
            list.addView(empty); return;
        }
        for(Models.Habit h:db.habits){
            LinearLayout card=new LinearLayout(getContext()); card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_bg); card.setPadding(20,16,20,16);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,0,12); card.setLayoutParams(lp);
            LinearLayout header=new LinearLayout(getContext()); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView name=new TextView(getContext()); name.setText(h.emoji+" "+h.name); name.setTextColor(0xFF1A1A2E); name.setTextSize(15); name.setTypeface(null,android.graphics.Typeface.BOLD);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); header.addView(name);
            TextView streak=new TextView(getContext()); streak.setText("🔥 "+h.streak); streak.setTextColor(0xFFF97316); streak.setTextSize(14); header.addView(streak);
            card.addView(header);
            TextView label=new TextView(getContext()); label.setText("Today: "+h.todayCount+"/"+h.dailyTarget); label.setTextColor(0xFF9CA3AF); label.setTextSize(12); label.setPadding(0,8,0,4); card.addView(label);
            ProgressBar pb=new ProgressBar(getContext(),null,android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100); pb.setProgress(h.dailyTarget>0?Math.min(100,h.todayCount*100/h.dailyTarget):0);
            pb.setProgressDrawable(requireContext().getDrawable(R.drawable.progress_habit));
            LinearLayout.LayoutParams pblp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,12); pb.setLayoutParams(pblp);
            card.addView(pb);
            int total=h.logs==null?0:h.logs.size();
            TextView logs=new TextView(getContext()); logs.setText(total+" days logged"); logs.setTextColor(0xFF9CA3AF); logs.setTextSize(11); logs.setPadding(0,6,0,0); card.addView(logs);
            list.addView(card);
        }
    }
}
