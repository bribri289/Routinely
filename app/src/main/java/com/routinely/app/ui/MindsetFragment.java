package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;

public class MindsetFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b){
        return inf.inflate(R.layout.fragment_mindset,c,false);
    }

    @Override public void onViewCreated(View v, Bundle b){
        super.onViewCreated(v,b);
        // Library / Saved tab switcher
        v.findViewById(R.id.tab_library).setOnClickListener(x->{
            v.findViewById(R.id.tab_library).setBackgroundResource(R.drawable.chip_bg_active);
            v.findViewById(R.id.tab_saved).setBackgroundResource(R.drawable.chip_bg);
            v.findViewById(R.id.saved_empty).setVisibility(View.GONE);
            v.findViewById(R.id.library_content).setVisibility(View.VISIBLE);
        });
        v.findViewById(R.id.tab_saved).setOnClickListener(x->{
            v.findViewById(R.id.tab_saved).setBackgroundResource(R.drawable.chip_bg_active);
            v.findViewById(R.id.tab_library).setBackgroundResource(R.drawable.chip_bg);
            v.findViewById(R.id.library_content).setVisibility(View.GONE);
            v.findViewById(R.id.saved_empty).setVisibility(View.VISIBLE);
        });
    }
}
