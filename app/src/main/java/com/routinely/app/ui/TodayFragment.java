package com.routinely.app.ui;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.view.animation.AlphaAnimation;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;
import com.routinely.app.data.AppData;
import com.routinely.app.data.MotivationalMessages;
import com.routinely.app.data.Models;
import java.util.Calendar;
import java.util.Random;

public class TodayFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v = inf.inflate(R.layout.fragment_today, c, false);
        AppData db = AppData.get(requireContext());
        // Motivational message - pick randomly once per day, stored in prefs
        showMotivationalMessage(v);
        // Greeting
        int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String g = hr<12?"Good morning ☀️":hr<17?"Good afternoon ⛅":"Good evening 🌙";
        ((TextView)v.findViewById(R.id.tv_greeting)).setText(g);
        // Date
        Calendar cal = Calendar.getInstance();
        String[] days={"Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
        String[] months={"January","February","March","April","May","June","July","August","September","October","November","December"};
        ((TextView)v.findViewById(R.id.tv_date)).setText(days[cal.get(Calendar.DAY_OF_WEEK)-1]+", "+months[cal.get(Calendar.MONTH)]+" "+cal.get(Calendar.DATE)+"th");
        // Next routine
        if (!db.routines.isEmpty()) {
            Models.Routine r = db.routines.get(0);
            ((TextView)v.findViewById(R.id.tv_routine_category)).setText((r.category!=null?r.category.toUpperCase():"MORNING")+" ROUTINE");
            ((TextView)v.findViewById(R.id.tv_routine_name)).setText(r.name);
            ((TextView)v.findViewById(R.id.tv_routine_duration)).setText(r.getTotalMinutes()+" min");
            ((TextView)v.findViewById(R.id.tv_routine_time)).setText(r.getTimeString());
            v.findViewById(R.id.btn_start_routine).setOnClickListener(x->{
                Intent i=new Intent(getActivity(),RunRoutineActivity.class); i.putExtra("routine",r); startActivity(i);
            });
        }
        // Habits strip
        buildHabitsStrip(v, db);
        // Progress
        int streak=0; for(Models.Habit h:db.habits) streak=Math.max(streak,h.streak);
        ((TextView)v.findViewById(R.id.tv_streak_count)).setText(streak+" days");
        ((TextView)v.findViewById(R.id.tv_routines_done)).setText(String.valueOf(db.activities.size()));
        // Activity
        buildActivity(v, db);
        return v;
    }

    void showMotivationalMessage(View v) {
        // Pick a new random message each day; store the last-shown date + index
        SharedPreferences prefs = requireContext().getSharedPreferences("routinely_prefs", android.content.Context.MODE_PRIVATE);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(new java.util.Date());
        String lastDate = prefs.getString("motiv_date", "");
        int msgIdx;
        if (today.equals(lastDate)) {
            msgIdx = prefs.getInt("motiv_idx", 0);
        } else {
            msgIdx = new Random().nextInt(MotivationalMessages.MESSAGES.length);
            prefs.edit().putString("motiv_date", today).putInt("motiv_idx", msgIdx).apply();
        }
        String message = MotivationalMessages.MESSAGES[msgIdx];
        TextView tvMsg = v.findViewById(R.id.tv_motivational_message);
        tvMsg.setText(message);
        // Fade-in animation
        View card = v.findViewById(R.id.card_motivational);
        card.setAlpha(0f);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        fadeIn.setStartOffset(300);
        fadeIn.setFillAfter(true);
        card.startAnimation(fadeIn);
        card.setAlpha(1f);
    }

    void buildHabitsStrip(View v, AppData db) {
        LinearLayout strip = v.findViewById(R.id.habits_strip); strip.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(getContext());
        int max = Math.min(db.habits.size(),4);
        for(int i=0;i<max;i++){
            Models.Habit h=db.habits.get(i);
            View item=inf.inflate(R.layout.item_habit_chip,strip,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(h.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(h.name);
            if(h.completedToday){item.setAlpha(0.45f);item.findViewById(R.id.check_dot).setVisibility(View.VISIBLE);}
            final Models.Habit habit=h;
            item.setOnClickListener(x->{habit.completedToday=!habit.completedToday;if(habit.completedToday)habit.streak++;AppData.get(requireContext()).save();buildHabitsStrip(v,AppData.get(requireContext()));});
            strip.addView(item);
        }
    }

    void buildActivity(View v, AppData db) {
        LinearLayout list=v.findViewById(R.id.activity_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        int max=Math.min(db.activities.size(),5);
        for(int i=0;i<max;i++){
            Models.RecentActivity a=db.activities.get(i);
            View item=inf.inflate(R.layout.item_activity,list,false);
            ((TextView)item.findViewById(R.id.tv_activity_name)).setText(a.routineName);
            ((TextView)item.findViewById(R.id.tv_activity_time)).setText(a.timestamp);
            TextView st=item.findViewById(R.id.tv_activity_status);
            st.setText(a.completed?"Complete":"Incomplete");
            st.setTextColor(a.completed?0xFF10B981:0xFF6B7280);
            list.addView(item);
        }
    }
}
