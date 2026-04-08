package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.util.Calendar;

public class HabitsFragment extends Fragment {
    int curTab=0;

    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b){
        View v=inf.inflate(R.layout.fragment_habits,c,false);
        AppData db=AppData.get(requireContext());
        setupTabs(v,db);
        buildScorecard(v,db);
        buildHabits(v,db);
        v.findViewById(R.id.btn_new_habit).setOnClickListener(x->
            startActivity(new Intent(getActivity(),EditHabitActivity.class)));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null){AppData db=AppData.get(requireContext());buildHabits(v,db);buildScorecard(v,db);}}

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
        // Weekly completion grid (Atomic Habits style)
        LinearLayout grid=v.findViewById(R.id.habit_grid);
        grid.removeAllViews();
        Calendar cal=Calendar.getInstance();
        // Show last 7 days columns for each habit
        String[] dayLabels={"M","T","W","T","F","S","S"};
        int today=cal.get(Calendar.DAY_OF_WEEK);

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

        // One row per habit
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
                dot.setBackgroundResource(done?R.drawable.circle_orange:R.drawable.circle_bg3);
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
            if(curTab==1&&h.completedToday) continue; // Today tab: show incomplete only
            if(curTab==2) { /* Streaks: show all sorted by streak */ }
            View item=inf.inflate(R.layout.item_habit,list,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(h.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(h.name);
            // Streak badge
            TextView tvStreak=item.findViewById(R.id.tv_streak);
            tvStreak.setText(h.streak>0?"🔥 "+h.streak+" days":"Start today");
            tvStreak.setTextColor(h.streak>0?0xFFF97316:0xFF6B7280);
            // Linked routine
            TextView tvRoutine=item.findViewById(R.id.tv_linked_routine);
            if(h.linkedRoutineId!=0){
                Models.Routine r=db.findRoutine(h.linkedRoutineId);
                if(r!=null){tvRoutine.setText("In: "+r.emoji+" "+r.name);tvRoutine.setVisibility(View.VISIBLE);}
                else tvRoutine.setVisibility(View.GONE);
            } else tvRoutine.setVisibility(View.GONE);
            // Check button
            View checkBtn=item.findViewById(R.id.check_icon);
            checkBtn.setBackgroundResource(h.completedToday?R.drawable.circle_green:R.drawable.circle_bg3);
            item.setOnClickListener(x->{
                h.completedToday=!h.completedToday;
                if(h.completedToday)h.streak++; else if(h.streak>0)h.streak--;
                AppData.get(requireContext()).save();
                buildHabits(v,AppData.get(requireContext()));
                buildScorecard(v,AppData.get(requireContext()));
            });
            item.setOnLongClickListener(x->{
                Intent i=new Intent(getActivity(),EditHabitActivity.class);
                i.putExtra("habit",h); startActivity(i); return true;
            });
            list.addView(item);
        }
    }
}
