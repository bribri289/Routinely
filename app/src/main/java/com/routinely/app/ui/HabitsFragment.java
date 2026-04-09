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
        // Support opening directly to a specific section (e.g. Mindset = 2)
        if(getArguments()!=null) sectionIdx=getArguments().getInt("section",0);
        setupSectionTabs(v,db);
        // Apply initial section visibility
        v.findViewById(R.id.habits_section).setVisibility(sectionIdx==0?View.VISIBLE:View.GONE);
        v.findViewById(R.id.progress_section).setVisibility(sectionIdx==1?View.VISIBLE:View.GONE);
        v.findViewById(R.id.mindset_section).setVisibility(sectionIdx==2?View.VISIBLE:View.GONE);
        int[] tabIds={R.id.section_tab_habits,R.id.section_tab_progress,R.id.section_tab_mindset};
        for(int i=0;i<tabIds.length;i++){
            v.findViewById(tabIds[i]).setBackgroundResource(i==sectionIdx?R.drawable.chip_bg_active:R.drawable.chip_bg);
            ((TextView)v.findViewById(tabIds[i])).setTextColor(i==sectionIdx?0xFFFFFFFF:0xFF9CA3AF);
        }
        // Remove compact toggle — hidden per design update
        v.findViewById(R.id.tv_compact_label).setVisibility(View.GONE);
        v.findViewById(R.id.sw_compact).setVisibility(View.GONE);
        v.findViewById(R.id.btn_new_habit).setVisibility(sectionIdx==0?View.VISIBLE:View.GONE);
        if(sectionIdx==2) loadMindsetFragment(v);
        setupTabs(v,db);
        buildScorecard(v,db);
        buildHabits(v,db);
        v.findViewById(R.id.btn_new_habit).setOnClickListener(x->
            startActivity(new Intent(getActivity(),EditHabitActivity.class)));
        return v;
    }
    @Override public void onResume(){super.onResume();View v=getView();if(v!=null){AppData db=AppData.get(requireContext());buildHabits(v,db);buildScorecard(v,db);if(sectionIdx==1)buildProgress(v,db);}}

    void setupSectionTabs(View v, AppData db){
        int[] tabIds={R.id.section_tab_habits, R.id.section_tab_progress, R.id.section_tab_mindset};
        for(int i=0;i<tabIds.length;i++){final int idx=i;
            v.findViewById(tabIds[i]).setOnClickListener(x->{
                sectionIdx=idx;
                for(int j=0;j<tabIds.length;j++){
                    v.findViewById(tabIds[j]).setBackgroundResource(j==idx?R.drawable.chip_bg_active:R.drawable.chip_bg);
                    ((android.widget.TextView)v.findViewById(tabIds[j])).setTextColor(j==idx?0xFFFFFFFF:0xFF9CA3AF);
                }
                v.findViewById(R.id.habits_section).setVisibility(idx==0?View.VISIBLE:View.GONE);
                v.findViewById(R.id.progress_section).setVisibility(idx==1?View.VISIBLE:View.GONE);
                v.findViewById(R.id.mindset_section).setVisibility(idx==2?View.VISIBLE:View.GONE);
                // Compact toggle is hidden — always GONE
                v.findViewById(R.id.tv_compact_label).setVisibility(View.GONE);
                v.findViewById(R.id.sw_compact).setVisibility(View.GONE);
                v.findViewById(R.id.btn_new_habit).setVisibility(idx==0?View.VISIBLE:View.GONE);
                if(idx==1) buildProgress(v,AppData.get(requireContext()));
                if(idx==2) loadMindsetFragment(v);
            });
        }
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
        ((TextView)v.findViewById(R.id.tv_completed_count)).setText(done+"/"+total);
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

    void loadMindsetFragment(View v){
        LinearLayout container=(LinearLayout)v.findViewById(R.id.mindset_section);
        if(container.getTag()!=null) return; // already built
        container.setTag("loaded");
        buildMindsetSection(container);
    }

    void buildMindsetSection(LinearLayout container){
        container.setPadding(16,16,16,16);
        int idx=(java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)-1)
                 % MindsetData.DAILY_LESSONS.length;
        String lessonTitle=MindsetData.DAILY_LESSONS[idx][0];
        String lessonBody=MindsetData.DAILY_LESSONS[idx][1];
        String preview=(lessonBody!=null&&lessonBody.contains("\n"))
            ?lessonBody.substring(0,lessonBody.indexOf('\n')):lessonBody;

        // Daily lesson card
        LinearLayout card=new LinearLayout(getContext()); card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_bg_primary); card.setPadding(40,32,40,32);
        LinearLayout.LayoutParams cardLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); cardLp.setMargins(0,0,0,20); card.setLayoutParams(cardLp);
        TextView tvNum=new TextView(getContext()); tvNum.setText("Lesson "+(idx+1));
        tvNum.setTextColor(0xFFE5E7EB); tvNum.setTextSize(11); tvNum.setBackgroundResource(R.drawable.chip_bg); tvNum.setPadding(12,6,12,6); card.addView(tvNum);
        TextView tvTitle=new TextView(getContext()); tvTitle.setText(lessonTitle);
        tvTitle.setTextColor(0xFFFFFFFF); tvTitle.setTextSize(18); tvTitle.setTypeface(null,android.graphics.Typeface.BOLD); tvTitle.setPadding(0,12,0,8); card.addView(tvTitle);
        TextView tvPrev=new TextView(getContext()); tvPrev.setText(preview!=null?preview:"");
        tvPrev.setTextColor(0xFFE5E7EB); tvPrev.setTextSize(13); tvPrev.setPadding(0,0,0,16); card.addView(tvPrev);
        TextView tvTap=new TextView(getContext()); tvTap.setText("Tap to read full lesson ›");
        tvTap.setTextColor(0xFFE5E7EB); tvTap.setTextSize(13); card.addView(tvTap);
        card.setOnClickListener(x->showMindsetReading(lessonTitle,lessonBody));
        container.addView(card);

        // Library section header
        TextView tvLib=new TextView(getContext()); tvLib.setText("LIBRARY");
        tvLib.setTextColor(0xFF9CA3AF); tvLib.setTextSize(11); tvLib.setPadding(0,8,0,12); container.addView(tvLib);

        // Library cards
        for(int s=0;s<MindsetData.LIBRARY.length;s++){
            String[][] section=MindsetData.LIBRARY[s];
            for(String[] article:section){
                final String artTitle=article[0]; final String artBody=article[2];
                LinearLayout artCard=new LinearLayout(getContext()); artCard.setOrientation(LinearLayout.VERTICAL);
                artCard.setBackgroundResource(R.drawable.card_bg); artCard.setPadding(20,16,20,16);
                LinearLayout.LayoutParams aLp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); aLp.setMargins(0,0,0,10); artCard.setLayoutParams(aLp);
                TextView artTv=new TextView(getContext()); artTv.setText(artTitle);
                artTv.setTextColor(0xFF1A1A2E); artTv.setTextSize(15); artTv.setTypeface(null,android.graphics.Typeface.BOLD); artCard.addView(artTv);
                if(article.length>1){TextView artSub=new TextView(getContext());artSub.setText(article[1]);artSub.setTextColor(0xFF9CA3AF);artSub.setTextSize(12);artSub.setPadding(0,4,0,0);artCard.addView(artSub);}
                artCard.setOnClickListener(x->showMindsetReading(artTitle,artBody));
                container.addView(artCard);
            }
        }
    }

    void showMindsetReading(String title, String body){
        android.app.Dialog dialog=new android.app.Dialog(requireContext(),android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        LinearLayout root=new LinearLayout(getContext()); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFDF6);
        // Toolbar
        LinearLayout toolbar=new LinearLayout(getContext()); toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFFFFFDF6); toolbar.setPadding(24,48,24,12); toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView tvClose=new TextView(getContext()); tvClose.setText("✕"); tvClose.setTextSize(20); tvClose.setTextColor(0xFF4B5563); tvClose.setPadding(8,8,8,8);
        tvClose.setOnClickListener(x->dialog.dismiss()); toolbar.addView(tvClose);
        TextView tvBar=new TextView(getContext()); tvBar.setText("Reading"); tvBar.setTextColor(0xFF6755C8); tvBar.setTextSize(14); tvBar.setTypeface(null,android.graphics.Typeface.BOLD); tvBar.setPadding(16,0,0,0);
        toolbar.addView(tvBar); root.addView(toolbar);
        // Divider
        View div=new View(getContext()); div.setBackgroundColor(0xFFE5DCC8); div.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,1)); root.addView(div);
        // Scrollable content
        ScrollView scroll=new ScrollView(getContext()); scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,0,1));
        LinearLayout content=new LinearLayout(getContext()); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(48,32,48,80);
        TextView tvTitle=new TextView(getContext()); tvTitle.setText(title);
        tvTitle.setTextColor(0xFF1A1A2E); tvTitle.setTextSize(22); tvTitle.setTypeface(android.graphics.Typeface.SERIF,android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0,0,0,24); tvTitle.setLineSpacing(6,1); content.addView(tvTitle);
        View rule=new View(getContext()); rule.setBackgroundColor(0xFFD4A853);
        LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(80,3); rlp.setMargins(0,0,0,24); rule.setLayoutParams(rlp); content.addView(rule);
        TextView tvBody=new TextView(getContext()); tvBody.setText(body);
        tvBody.setTextColor(0xFF3B3225); tvBody.setTextSize(16); tvBody.setTypeface(android.graphics.Typeface.SERIF); tvBody.setLineSpacing(8,1); content.addView(tvBody);
        scroll.addView(content); root.addView(scroll);
        dialog.setContentView(root); dialog.show();
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
