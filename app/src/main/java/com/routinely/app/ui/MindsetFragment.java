package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
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
        String preview = (lessonBody != null && lessonBody.contains("\n"))
            ? lessonBody.substring(0, lessonBody.indexOf('\n')) : lessonBody;
        ((TextView) v.findViewById(R.id.tv_lesson_number)).setText("Lesson " + (idx + 1));
        ((TextView) v.findViewById(R.id.tv_lesson_title)).setText(lessonTitle);
        ((TextView) v.findViewById(R.id.tv_lesson_preview)).setText(preview != null ? preview : "");
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
        android.app.Dialog dialog = new android.app.Dialog(requireContext(), android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        // Build ebook-style reading view
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFDF6); // warm off-white ebook bg

        // Toolbar row
        LinearLayout toolbar = new LinearLayout(getContext());
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setBackgroundColor(0xFFFFFDF6);
        toolbar.setPadding(24, 40, 24, 12);
        toolbar.setGravity(android.view.Gravity.CENTER_VERTICAL);
        TextView tvClose = new TextView(getContext()); tvClose.setText("✕"); tvClose.setTextSize(20);
        tvClose.setTextColor(0xFF4B5563); tvClose.setPadding(8,8,8,8);
        tvClose.setOnClickListener(x -> dialog.dismiss());
        toolbar.addView(tvClose);
        TextView tvBar = new TextView(getContext()); tvBar.setText("Reading"); tvBar.setTextColor(0xFF6755C8);
        tvBar.setTextSize(14); tvBar.setTypeface(null, android.graphics.Typeface.BOLD);
        tvBar.setPadding(16,0,0,0);
        toolbar.addView(tvBar);
        root.addView(toolbar);

        // Divider
        View divider = new View(getContext()); divider.setBackgroundColor(0xFFE5DCC8);
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(divider);

        // Scrollable content
        ScrollView scroll = new ScrollView(getContext());
        scroll.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(48, 32, 48, 80);

        TextView tvTitle = new TextView(getContext());
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFF1A1A2E);
        tvTitle.setTextSize(22);
        tvTitle.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
        tvTitle.setPadding(0, 0, 0, 24);
        tvTitle.setLineSpacing(6, 1);
        content.addView(tvTitle);

        // Decorative rule
        View rule = new View(getContext()); rule.setBackgroundColor(0xFFD4A853);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(80, 3); rlp.setMargins(0,0,0,24); rule.setLayoutParams(rlp);
        content.addView(rule);

        TextView tvBody = new TextView(getContext());
        tvBody.setText(body);
        tvBody.setTextColor(0xFF3B3225);
        tvBody.setTextSize(16);
        tvBody.setTypeface(android.graphics.Typeface.SERIF);
        tvBody.setLineSpacing(8, 1);
        tvBody.setLetterSpacing(0.01f);
        content.addView(tvBody);

        scroll.addView(content);
        root.addView(scroll);

        dialog.setContentView(root);
        dialog.show();
    }
}
