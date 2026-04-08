package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.text.style.UnderlineSpan;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class HabitDetailActivity extends AppCompatActivity {
    AppData db; Models.Habit habit;

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_habit_detail);
        db=AppData.get(this);
        int habitId=getIntent().getIntExtra("habitId",-1);
        habit=db.findHabit(habitId);
        if(habit==null){finish();return;}
        setup();
    }

    @Override protected void onResume(){
        super.onResume();
        // Refresh habit reference in case it was edited
        habit=db.findHabit(habit.id);
        if(habit==null){finish();return;}
        setup();
    }

    void setup(){
        findViewById(R.id.btn_back).setOnClickListener(v->finish());
        findViewById(R.id.btn_edit).setOnClickListener(v->{
            Intent i=new Intent(this,EditHabitActivity.class);
            i.putExtra("habit",habit); startActivity(i);
        });

        // Identity statement with underline on key phrases
        String identity;
        if(habit.name.isEmpty()){
            identity=habit.emoji+" "+habit.category;
        } else {
            identity="I will "+habit.name.toLowerCase()+
                (habit.category.isEmpty()?"":", "+habit.category.toLowerCase())+
                " so that I can become a more productive person";
        }
        SpannableString ss=new SpannableString(identity);
        // Underline the habit name portion (skip "I will " prefix = 7 chars)
        if(!habit.name.isEmpty()){
            int start=7; // "I will " is 7 chars
            int end=Math.min(identity.length(),start+habit.name.length());
            if(start<end) ss.setSpan(new UnderlineSpan(),start,end,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        ((TextView)findViewById(R.id.tv_identity)).setText(ss);

        // Schedule
        List<int[]> times=habit.getEffectiveReminderTimes();
        StringBuilder sch=new StringBuilder("Daily at ");
        for(int i=0;i<times.size();i++){
            int[] t=times.get(i);
            int h=t[0]%12; if(h==0)h=12;
            String ap=t[0]<12?"AM":"PM";
            if(i>0)sch.append(", ");
            sch.append(String.format("%d:%02d %s",h,t[1],ap));
        }
        ((TextView)findViewById(R.id.tv_schedule)).setText(sch.toString());

        // Total reps from logs
        int totalReps=0;
        if(habit.logs!=null) for(Models.HabitLog l:habit.logs) totalReps+=l.count;
        ((TextView)findViewById(R.id.tv_total_reps)).setText(String.valueOf(totalReps));

        // Since date
        String since=habit.createdDate.isEmpty()?"–":habit.createdDate;
        ((TextView)findViewById(R.id.tv_since_date)).setText(since);

        // History button
        findViewById(R.id.btn_history).setOnClickListener(v->
            Toast.makeText(this,"History coming soon",Toast.LENGTH_SHORT).show());

        // Calendar grid
        buildCalendarGrid();

        // Current schedule
        ((TextView)findViewById(R.id.tv_current_schedule)).setText(
            "Since "+(habit.createdDate.isEmpty()?"–":habit.createdDate));

        // Records
        int done=habit.logs==null?0:(int)habit.logs.stream().filter(l->l.count>=habit.dailyTarget).count();
        int total=habit.logs==null?0:habit.logs.size();
        int rate=total>0?done*100/total:0;
        ((TextView)findViewById(R.id.tv_completion_rate)).setText(rate+"%");
        ((TextView)findViewById(R.id.tv_current_streak)).setText(String.valueOf(habit.streak));
        ((TextView)findViewById(R.id.tv_perfect_days)).setText(String.valueOf(done));
        ((TextView)findViewById(R.id.tv_perfect_streak)).setText(String.valueOf(habit.streak));

        // Streaks sub-list
        buildStreaksList();
    }

    void buildCalendarGrid(){
        LinearLayout grid=findViewById(R.id.calendar_grid);
        grid.removeAllViews();
        // Month headers row
        String[] months={"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"};
        Calendar cal=Calendar.getInstance();
        int currentMonth=cal.get(Calendar.MONTH);
        // Show last 3 months label row
        LinearLayout monthRow=new LinearLayout(this); monthRow.setOrientation(LinearLayout.HORIZONTAL);
        for(int i=2;i>=0;i--){
            int m=(currentMonth-i+12)%12;
            TextView tv=new TextView(this); tv.setText(months[m]);
            tv.setTextColor(0xFF6B7280); tv.setTextSize(11);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,30,1)); tv.setGravity(Gravity.CENTER);
            monthRow.addView(tv);
        }
        grid.addView(monthRow);
        // Day-of-week labels
        String[] dows={"S","M","T","W","T","F","S"};
        LinearLayout dowRow=new LinearLayout(this); dowRow.setOrientation(LinearLayout.HORIZONTAL);
        for(String d:dows){
            TextView tv=new TextView(this); tv.setText(d); tv.setTextColor(0xFF6B7280); tv.setTextSize(10);
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,28,1)); tv.setGravity(Gravity.CENTER);
            dowRow.addView(tv);
        }
        grid.addView(dowRow);
        // Build log lookup
        Set<String> loggedDates=new HashSet<>();
        if(habit.logs!=null) for(Models.HabitLog l:habit.logs) if(l.count>0) loggedDates.add(l.date);
        // Show 3 weeks × 7 days
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        cal=Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR,-20);
        for(int row=0;row<3;row++){
            LinearLayout weekRow=new LinearLayout(this); weekRow.setOrientation(LinearLayout.HORIZONTAL);
            for(int d=0;d<7;d++){
                String dateStr=sdf.format(cal.getTime());
                View dot=new View(this);
                dot.setBackgroundResource(loggedDates.contains(dateStr)?R.drawable.circle_orange:R.drawable.circle_bg3);
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,28,1);
                lp.setMargins(3,3,3,3); dot.setLayoutParams(lp);
                weekRow.addView(dot);
                cal.add(Calendar.DAY_OF_YEAR,1);
            }
            grid.addView(weekRow);
        }
    }

    void buildStreaksList(){
        LinearLayout list=findViewById(R.id.streaks_list); list.removeAllViews();
        // Show one row for each "habit instance" — here represented as the habit itself
        // plus all logged habit names (same habit, conceptually one per repetition goal)
        List<String> goals=new ArrayList<>();
        goals.add(habit.name);
        if(!habit.category.isEmpty()) goals.add("Spend time on: "+habit.category);
        for(String goal:goals){
            LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
            row.setBackgroundResource(R.drawable.card_bg); row.setPadding(20,16,20,16);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0,0,0,10); row.setLayoutParams(lp);
            LinearLayout topRow=new LinearLayout(this); topRow.setOrientation(LinearLayout.HORIZONTAL); topRow.setGravity(Gravity.CENTER_VERTICAL);
            TextView emoji=new TextView(this); emoji.setText(habit.emoji); emoji.setTextSize(24); emoji.setPadding(0,0,12,0); topRow.addView(emoji);
            LinearLayout info=new LinearLayout(this); info.setOrientation(LinearLayout.VERTICAL); info.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
            TextView streakLabel=new TextView(this); streakLabel.setText(habit.streak+" days in a row!"); streakLabel.setTextColor(0xFFF97316); streakLabel.setTextSize(14); streakLabel.setTypeface(null,android.graphics.Typeface.BOLD);
            TextView desc=new TextView(this); desc.setText(goal); desc.setTextColor(0xFF9CA3AF); desc.setTextSize(12); desc.setMaxLines(2); desc.setEllipsize(TextUtils.TruncateAt.END);
            info.addView(streakLabel); info.addView(desc); topRow.addView(info);
            row.addView(topRow); list.addView(row);
        }
    }
}
