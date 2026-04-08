package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
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
        if(db.routines.isEmpty()){
            TextView empty=new TextView(getContext());
            empty.setText("No routines yet\nTap + to build your first routine");
            empty.setTextColor(0xFF6B7280); empty.setTextSize(14);
            empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32,80,32,80);
            list.addView(empty); return;
        }
        for(Models.Routine r:db.routines){
            View item=inf.inflate(R.layout.item_routine,list,false);
            ((TextView)item.findViewById(R.id.tv_emoji)).setText(r.emoji);
            ((TextView)item.findViewById(R.id.tv_name)).setText(r.name);
            ((TextView)item.findViewById(R.id.tv_category)).setText(r.category);
            ((TextView)item.findViewById(R.id.tv_days)).setText(r.getDaysString());
            ((TextView)item.findViewById(R.id.tv_time)).setText(r.getTimeString());
            // Duration & steps
            int totalMins=r.getTotalMinutes();
            String dur=totalMins>=60?(totalMins/60)+"h "+(totalMins%60)+"m":totalMins+"m";
            ((TextView)item.findViewById(R.id.tv_duration)).setText(dur+" · "+r.steps.size()+" steps");
            // Linked alarm
            TextView tvAlarm=item.findViewById(R.id.tv_linked_alarm);
            if(r.linkedAlarmId!=0){
                Models.Alarm a=db.findAlarm(r.linkedAlarmId);
                if(a!=null){tvAlarm.setText("⏰ "+a.getTimeString());tvAlarm.setVisibility(View.VISIBLE);}
                else tvAlarm.setVisibility(View.GONE);
            } else tvAlarm.setVisibility(View.GONE);
            // Linked habits count
            int habitCount=0;
            for(Models.RoutineStep s:r.steps) if(s.linkedHabitId!=0) habitCount++;
            TextView tvHabits=item.findViewById(R.id.tv_habit_count);
            if(habitCount>0){tvHabits.setText("🌱 "+habitCount+" habits");tvHabits.setVisibility(View.VISIBLE);}
            else tvHabits.setVisibility(View.GONE);
            // Start button
            item.findViewById(R.id.btn_start).setOnClickListener(x->{
                Intent i=new Intent(getActivity(),RunRoutineActivity.class);
                i.putExtra("routine",r); startActivity(i);
            });
            // Edit on tap
            item.setOnClickListener(x->{
                Intent i=new Intent(getActivity(),EditRoutineActivity.class);
                i.putExtra("routine",r); startActivity(i);
            });
            list.addView(item);
        }
    }
}
