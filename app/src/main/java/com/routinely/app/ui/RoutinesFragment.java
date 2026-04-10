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
            // Hide category box per new design requirement
            item.findViewById(R.id.tv_category).setVisibility(View.GONE);
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
            // Play button → start routine immediately (no preview screen)
            item.findViewById(R.id.btn_play).setOnClickListener(x->{
                if(r.steps.isEmpty()){Toast.makeText(getContext(),"Add steps first",Toast.LENGTH_SHORT).show();return;}
                Intent i=new Intent(getActivity(),RunRoutineActivity.class);
                i.putExtra("routine",r);
                i.putExtra("autoStart",true);
                startActivity(i);
            });
            // Overflow ⋮
            item.findViewById(R.id.btn_overflow).setOnClickListener(x->showOverflow(r,v,db));
            // Tap card → show step summary dialog; from there user can edit
            item.setOnClickListener(x->showStepSummary(r));
            list.addView(item);
        }
    }

    String fmtTime(int h,int m){
        String ap=h<12?"am":"pm"; int hh=h%12; if(hh==0)hh=12;
        return String.format("%d:%02d%s",hh,m,ap);
    }

    /** Show a step summary card list. Tapping a step opens the edit screen. */
    void showStepSummary(Models.Routine r) {
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar_FullScreen);
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF111827);

        // Header row
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(20, 48, 20, 12);
        header.setBackgroundColor(0xFF111827);
        TextView tvClose = new TextView(getContext()); tvClose.setText("✕");
        tvClose.setTextColor(0xFF9CA3AF); tvClose.setTextSize(20); tvClose.setPadding(8, 8, 8, 8);
        tvClose.setOnClickListener(x -> dialog.dismiss());
        header.addView(tvClose);
        LinearLayout titleCol = new LinearLayout(getContext()); titleCol.setOrientation(LinearLayout.VERTICAL);
        titleCol.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        titleCol.setPadding(12, 0, 0, 0);
        TextView tvName = new TextView(getContext()); tvName.setText(r.emoji + "  " + r.name);
        tvName.setTextColor(0xFFFFFFFF); tvName.setTextSize(17); tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        titleCol.addView(tvName);
        TextView tvSub = new TextView(getContext()); tvSub.setText(r.steps.size() + " steps · " + r.getTotalMinutes() + " min");
        tvSub.setTextColor(0xFF9CA3AF); tvSub.setTextSize(12); tvSub.setPadding(0, 2, 0, 0);
        titleCol.addView(tvSub);
        header.addView(titleCol);
        // Edit button
        Button btnEdit = new Button(getContext()); btnEdit.setText("✏️ Edit");
        btnEdit.setTextColor(0xFFFFFFFF); btnEdit.setTextSize(13);
        btnEdit.setBackgroundResource(R.drawable.chip_bg);
        btnEdit.setOnClickListener(x -> { dialog.dismiss(); Intent i = new Intent(getActivity(), EditRoutineActivity.class); i.putExtra("routine", r); startActivity(i); });
        header.addView(btnEdit);
        root.addView(header);

        // Divider
        View div = new View(getContext()); div.setBackgroundColor(0xFF1F2937);
        div.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        root.addView(div);

        // Steps list
        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout stepsList = new LinearLayout(getContext());
        stepsList.setOrientation(LinearLayout.VERTICAL);
        stepsList.setPadding(16, 8, 16, 80);

        if (r.steps.isEmpty()) {
            TextView empty = new TextView(getContext()); empty.setText("No steps yet. Tap Edit to add steps.");
            empty.setTextColor(0xFF6B7280); empty.setTextSize(14); empty.setGravity(android.view.Gravity.CENTER); empty.setPadding(32, 64, 32, 64);
            stepsList.addView(empty);
        } else {
            for (int i = 0; i < r.steps.size(); i++) {
                Models.RoutineStep step = r.steps.get(i);
                LinearLayout card = new LinearLayout(getContext()); card.setOrientation(LinearLayout.HORIZONTAL);
                card.setBackgroundResource(R.drawable.card_bg); card.setGravity(android.view.Gravity.CENTER_VERTICAL);
                card.setPadding(16, 14, 16, 14);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0, 0, 0, 8); card.setLayoutParams(lp);

                // Icon
                TextView tvEmoji = new TextView(getContext()); tvEmoji.setText(step.emoji);
                tvEmoji.setTextSize(22); tvEmoji.setPadding(0, 0, 14, 0); card.addView(tvEmoji);
                // Name + duration
                LinearLayout col = new LinearLayout(getContext()); col.setOrientation(LinearLayout.VERTICAL);
                col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                TextView tvStepName = new TextView(getContext()); tvStepName.setText(step.name);
                tvStepName.setTextColor(0xFFFFFFFF); tvStepName.setTextSize(15); col.addView(tvStepName);
                int sm = step.durationSeconds / 60;
                String durStr = sm > 0 ? sm + " min" : step.durationSeconds + "s";
                TextView tvDur = new TextView(getContext()); tvDur.setText(durStr);
                tvDur.setTextColor(0xFF9CA3AF); tvDur.setTextSize(12); col.addView(tvDur);
                card.addView(col);
                // Step number badge
                TextView tvNum = new TextView(getContext()); tvNum.setText(String.valueOf(i + 1));
                tvNum.setTextColor(0xFF6B7280); tvNum.setTextSize(12); card.addView(tvNum);

                stepsList.addView(card);
            }
        }
        scroll.addView(stepsList);
        root.addView(scroll);

        // Bottom: Start Routine button
        Button btnStart = new Button(getContext()); btnStart.setText("▶  Start Routine");
        btnStart.setTextColor(0xFFFFFFFF); btnStart.setTextSize(15);
        btnStart.setBackgroundResource(R.drawable.btn_primary_bg);
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 56 * (int)getResources().getDisplayMetrics().density);
        btnLp.setMargins(16, 8, 16, 24); btnStart.setLayoutParams(btnLp);
        btnStart.setOnClickListener(x -> {
            dialog.dismiss();
            if (r.steps.isEmpty()) { Toast.makeText(getContext(), "Add steps first", Toast.LENGTH_SHORT).show(); return; }
            Intent i = new Intent(getActivity(), RunRoutineActivity.class);
            i.putExtra("routine", r); i.putExtra("autoStart", true); startActivity(i);
        });
        root.addView(btnStart);

        dialog.setContentView(root);
        dialog.show();
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
