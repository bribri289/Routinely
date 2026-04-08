package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;
import com.routinely.app.data.*;
import com.routinely.app.receivers.AlarmReceiver;

public class AlarmFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v=inf.inflate(R.layout.fragment_alarm,c,false);
        v.findViewById(R.id.btn_new_alarm).setOnClickListener(x->
            startActivity(new Intent(getActivity(),EditAlarmActivity.class)));
        buildList(v, AppData.get(requireContext()));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildList(v,AppData.get(requireContext()));}

    void buildList(View v, AppData db){
        LinearLayout list=v.findViewById(R.id.alarm_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        if(db.alarms.isEmpty()){
            View empty=new TextView(getContext());
            ((TextView)empty).setText("No alarms yet\nTap + to create your first alarm");
            ((TextView)empty).setTextColor(0xFF6B7280);
            ((TextView)empty).setTextSize(15);
            ((TextView)empty).setGravity(android.view.Gravity.CENTER);
            ((TextView)empty).setPadding(32,80,32,80);
            list.addView(empty); return;
        }
        for(Models.Alarm a:db.alarms){
            View item=inf.inflate(R.layout.item_alarm,list,false);
            // Big time
            ((TextView)item.findViewById(R.id.tv_time)).setText(a.getTimeString());
            ((TextView)item.findViewById(R.id.tv_label)).setText(a.label);
            ((TextView)item.findViewById(R.id.tv_days)).setText(a.getDaysString());
            // Mission icons
            LinearLayout missionRow=item.findViewById(R.id.mission_icons_row);
            missionRow.removeAllViews();
            for(Models.Mission m:a.missions){
                TextView icon=new TextView(getContext());
                icon.setText(m.getEmoji()+"\n"+m.getDisplayName());
                icon.setTextColor(0xFF9CA3AF); icon.setTextSize(10);
                icon.setGravity(android.view.Gravity.CENTER);
                icon.setPadding(12,8,12,8);
                icon.setBackground(getContext().getDrawable(R.drawable.chip_bg));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0,0,8,0); icon.setLayoutParams(lp);
                missionRow.addView(icon);
            }
            missionRow.setVisibility(a.missions.isEmpty()?View.GONE:View.VISIBLE);
            // Linked routine
            TextView tvRoutine=item.findViewById(R.id.tv_linked_routine);
            if(a.linkedRoutineId!=0){
                Models.Routine r=db.findRoutine(a.linkedRoutineId);
                if(r!=null){tvRoutine.setText("→ Starts: "+r.emoji+" "+r.name);tvRoutine.setVisibility(View.VISIBLE);}
                else tvRoutine.setVisibility(View.GONE);
            } else tvRoutine.setVisibility(View.GONE);
            // Toggle
            Switch sw=item.findViewById(R.id.sw_enabled);
            sw.setChecked(a.enabled);
            sw.setOnCheckedChangeListener((btn,chk)->{
                a.enabled=chk; db.save();
                if(chk)AlarmReceiver.schedule(requireContext(),a);
                else AlarmReceiver.cancel(requireContext(),a.id);
            });
            item.setOnClickListener(x->{
                Intent i=new Intent(getActivity(),EditAlarmActivity.class);
                i.putExtra("alarmId",a.id); startActivity(i);
            });
            list.addView(item);
        }
    }
}
