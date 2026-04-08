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
            // Play button
            item.findViewById(R.id.btn_play).setOnClickListener(x->{
                if(r.steps.isEmpty()){Toast.makeText(getContext(),"Add steps first",Toast.LENGTH_SHORT).show();return;}
                Intent i=new Intent(getActivity(),RunRoutineActivity.class);
                i.putExtra("routine",r); startActivity(i);
            });
            // Overflow ⋮
            item.findViewById(R.id.btn_overflow).setOnClickListener(x->showOverflow(r,v,db));
            // Tap card → edit
            item.setOnClickListener(x->{
                Intent i=new Intent(getActivity(),EditRoutineActivity.class);
                i.putExtra("routine",r); startActivity(i);
            });
            list.addView(item);
        }
    }

    String fmtTime(int h,int m){
        String ap=h<12?"am":"pm"; int hh=h%12; if(hh==0)hh=12;
        return String.format("%d:%02d%s",hh,m,ap);
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
