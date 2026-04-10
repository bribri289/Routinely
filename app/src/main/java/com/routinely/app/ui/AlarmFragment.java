package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.routinely.app.R;
import com.routinely.app.data.*;
import com.routinely.app.receivers.AlarmReceiver;
import java.util.ArrayList;
import java.util.List;

public class AlarmFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v=inf.inflate(R.layout.fragment_alarm,c,false);
        v.findViewById(R.id.btn_new_alarm).setOnClickListener(x->
            startActivity(new Intent(getActivity(),EditAlarmActivity.class)));
        buildList(v, AppData.get(requireContext()));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null)buildList(v,AppData.get(requireContext()));}

    static final String[] DAY_LABELS={"S","M","T","W","T","F","S"};

    void buildList(View v, AppData db){
        LinearLayout list=v.findViewById(R.id.alarm_list); list.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(getContext());
        if(db.alarms.isEmpty()){
            buildEmptyState(list); return;
        }
        // Sort: enabled alarms first, then disabled
        List<Models.Alarm> sorted=new ArrayList<>(db.alarms);
        sorted.sort((a1,a2)->Boolean.compare(!a1.enabled,!a2.enabled));
        for(Models.Alarm a:sorted){
            View item=inf.inflate(R.layout.item_alarm,list,false);
            bindAlarmItem(item,a,v,db);
            list.addView(item);
        }
    }

    void buildEmptyState(LinearLayout list){
        TextView tv=new TextView(getContext());
        tv.setText("No alarms yet\nTap + to add one");
        tv.setTextColor(0xFF9CA3AF); tv.setTextSize(16);
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(24,80,24,80);
        list.addView(tv);
    }

    void bindAlarmItem(View item, Models.Alarm a, View fragmentView, AppData db){
        // Days dots row
        buildDayDots(item.findViewById(R.id.days_dots_row),a.repeatDays);
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
        Switch sw=item.findViewById(R.id.sw_enabled); sw.setChecked(a.enabled);
        sw.setOnCheckedChangeListener((btn,chk)->{
            a.enabled=chk; db.save();
            if(chk) AlarmReceiver.schedule(requireContext(),a);
            else AlarmReceiver.cancel(requireContext(),a.id);
        });
        // Overflow ⋮
        item.findViewById(R.id.btn_overflow).setOnClickListener(x->showAlarmOverflow(a,fragmentView,db));
        // Tap → edit
        item.setOnClickListener(x->{
            Intent i=new Intent(getActivity(),EditAlarmActivity.class);
            i.putExtra("alarmId",a.id); startActivity(i);
        });
    }

    void buildDayDots(LinearLayout row, boolean[] days){
        row.removeAllViews();
        // Use Sun=0,Mon=1,...,Sat=6 (SMTWTFS), model is Mon=0..Sun=6 → remap
        int[] modelToDisplay={6,0,1,2,3,4,5}; // display index for model index
        boolean[] display=new boolean[7];
        for(int i=0;i<7;i++) display[modelToDisplay[i]]=days[i];
        for(int d=0;d<7;d++){
            TextView tv=new TextView(getContext()); tv.setText(DAY_LABELS[d]);
            tv.setTextSize(11); tv.setGravity(android.view.Gravity.CENTER);
            tv.setTextColor(display[d]?0xFFFFFFFF:0xFF6B7280);
            tv.setBackground(getContext().getDrawable(display[d]?R.drawable.circle_primary:R.drawable.circle_bg3));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,36,1);
            lp.setMargins(3,0,3,0); tv.setLayoutParams(lp);
            row.addView(tv);
        }
    }

    void showAlarmOverflow(Models.Alarm a, View fragmentView, AppData db){
        BottomSheetDialog sheet=new BottomSheetDialog(requireContext());
        LinearLayout layout=new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.card_bg);
        layout.setPadding(0,16,0,32);
        String[] opts={"✏️  Edit Alarm","📋  Duplicate","🗑  Delete"};
        for(String opt:opts){
            TextView tv=new TextView(getContext()); tv.setText(opt);
            tv.setTextColor(opt.contains("Delete")?0xFFEF4444:0xFFFFFFFF);
            tv.setTextSize(15); tv.setPadding(28,28,28,28);
            tv.setOnClickListener(x->{
                sheet.dismiss();
                if(opt.contains("Edit")){Intent i=new Intent(getActivity(),EditAlarmActivity.class);i.putExtra("alarmId",a.id);startActivity(i);}
                else if(opt.contains("Duplicate")){
                    Models.Alarm copy=new Models.Alarm();
                    copy.id=db.newId(); copy.hour=a.hour; copy.minute=a.minute;
                    copy.label=a.label+" (Copy)"; copy.repeatDays=java.util.Arrays.copyOf(a.repeatDays,7);
                    copy.enabled=false; db.alarms.add(copy); db.save();
                    buildList(fragmentView,db);
                }
                else if(opt.contains("Delete")){
                    new android.app.AlertDialog.Builder(getContext()).setTitle("Delete alarm?")
                        .setPositiveButton("Delete",(d,w)->{
                            AlarmReceiver.cancel(requireContext(),a.id);
                            db.alarms.removeIf(al->al.id==a.id); db.save(); buildList(fragmentView,db);
                        }).setNegativeButton("Cancel",null).show();
                }
            });
            layout.addView(tv);
        }
        sheet.setContentView(layout); sheet.show();
    }
}
