package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;
import com.routinely.app.data.*;

public class ProgressFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b){
        View v=inf.inflate(R.layout.fragment_progress,c,false);
        AppData db=AppData.get(requireContext());
        buildCharts(v,db);
        return v;
    }

    void buildCharts(View v, AppData db){
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
            // Header
            LinearLayout header=new LinearLayout(getContext()); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextView name=new TextView(getContext()); name.setText(h.emoji+" "+h.name); name.setTextColor(0xFFE5E7EB); name.setTextSize(15); name.setTypeface(null,android.graphics.Typeface.BOLD);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1)); header.addView(name);
            TextView streak=new TextView(getContext()); streak.setText("🔥 "+h.streak); streak.setTextColor(0xFFF97316); streak.setTextSize(14); header.addView(streak);
            card.addView(header);
            // Progress bar
            TextView label=new TextView(getContext()); label.setText("Today: "+h.todayCount+"/"+h.dailyTarget); label.setTextColor(0xFF9CA3AF); label.setTextSize(12); label.setPadding(0,8,0,4); card.addView(label);
            ProgressBar pb=new ProgressBar(getContext(),null,android.R.attr.progressBarStyleHorizontal);
            pb.setMax(100); pb.setProgress(h.dailyTarget>0?Math.min(100,h.todayCount*100/h.dailyTarget):0);
            pb.setProgressDrawable(getContext().getDrawable(R.drawable.progress_habit));
            LinearLayout.LayoutParams pblp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,12); pb.setLayoutParams(pblp);
            card.addView(pb);
            // Total logs
            int total=h.logs==null?0:h.logs.size();
            TextView logs=new TextView(getContext()); logs.setText(total+" days logged"); logs.setTextColor(0xFF9CA3AF); logs.setTextSize(11); logs.setPadding(0,6,0,0); card.addView(logs);
            list.addView(card);
        }
    }
}
