package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.routinely.app.R;
import com.routinely.app.data.MindsetData;
import java.util.Calendar;

public class MindsetFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b){
        return inf.inflate(R.layout.fragment_mindset,c,false);
    }

    @Override public void onViewCreated(View v, Bundle b){
        super.onViewCreated(v,b);

        // Populate today's daily lesson
        int idx = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - 1) % MindsetData.DAILY_LESSONS.length;
        String lessonTitle = MindsetData.DAILY_LESSONS[idx][0];
        String lessonBody  = MindsetData.DAILY_LESSONS[idx][1];
        ((TextView) v.findViewById(R.id.tv_lesson_number)).setText("Lesson " + (idx + 1));
        ((TextView) v.findViewById(R.id.tv_lesson_title)).setText(lessonTitle);
        ((TextView) v.findViewById(R.id.tv_lesson_preview)).setText(lessonBody.split("\n")[0]);
        // Full-card tap → read full lesson
        v.findViewById(R.id.daily_lesson_card).setOnClickListener(x ->
            showReading(lessonTitle, lessonBody));

        // Library / Saved tab switcher
        v.findViewById(R.id.tab_library).setOnClickListener(x->{
            v.findViewById(R.id.tab_library).setBackgroundResource(R.drawable.chip_bg_active);
            ((TextView)v.findViewById(R.id.tab_library)).setTextColor(0xFFFFFFFF);
            v.findViewById(R.id.tab_saved).setBackgroundResource(R.drawable.chip_bg);
            ((TextView)v.findViewById(R.id.tab_saved)).setTextColor(0xFF9CA3AF);
            v.findViewById(R.id.saved_empty).setVisibility(View.GONE);
            v.findViewById(R.id.library_content).setVisibility(View.VISIBLE);
        });
        v.findViewById(R.id.tab_saved).setOnClickListener(x->{
            v.findViewById(R.id.tab_saved).setBackgroundResource(R.drawable.chip_bg_active);
            ((TextView)v.findViewById(R.id.tab_saved)).setTextColor(0xFFFFFFFF);
            v.findViewById(R.id.tab_library).setBackgroundResource(R.drawable.chip_bg);
            ((TextView)v.findViewById(R.id.tab_library)).setTextColor(0xFF9CA3AF);
            v.findViewById(R.id.library_content).setVisibility(View.GONE);
            v.findViewById(R.id.saved_empty).setVisibility(View.VISIBLE);
        });

        // Attach click handlers to each library article card
        attachLibraryClicks(v);
    }

    void attachLibraryClicks(View v) {
        LinearLayout libContent = v.findViewById(R.id.library_content);
        // library_content children alternate: header row (even idx) / HorizontalScrollView (odd idx)
        // Each HorizontalScrollView contains one inner LinearLayout with 2 card LinearLayouts
        int sectionCount = MindsetData.LIBRARY.length;
        for (int s = 0; s < sectionCount; s++) {
            int hsvIdx = s * 2 + 1;
            if (hsvIdx >= libContent.getChildCount()) break;
            View hsvView = libContent.getChildAt(hsvIdx);
            if (!(hsvView instanceof HorizontalScrollView)) continue;
            LinearLayout cardRow = (LinearLayout) ((HorizontalScrollView) hsvView).getChildAt(0);
            for (int c = 0; c < cardRow.getChildCount() && c < MindsetData.LIBRARY[s].length; c++) {
                final String[] article = MindsetData.LIBRARY[s][c];
                cardRow.getChildAt(c).setOnClickListener(x -> showReading(article[0], article[2]));
            }
        }
    }

    void showReading(String title, String body) {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        ScrollView scroll = new ScrollView(getContext());
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundResource(R.drawable.card_bg);
        layout.setPadding(28, 24, 28, 48);
        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTextSize(18);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 16);
        layout.addView(tvTitle);
        TextView tvBody = new TextView(getContext());
        tvBody.setText(body);
        tvBody.setTextColor(0xFF4B5563);
        tvBody.setTextSize(14);
        tvBody.setLineSpacing(4, 1);
        layout.addView(tvBody);
        scroll.addView(layout);
        sheet.setContentView(scroll);
        sheet.show();
    }
}
