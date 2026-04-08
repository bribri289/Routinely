package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import com.routinely.app.R;
import com.routinely.app.data.AppData;
import com.routinely.app.data.Models;

public class ProfileFragment extends Fragment {
    @Override public View onCreateView(LayoutInflater inf, ViewGroup c, Bundle b) {
        View v=inf.inflate(R.layout.fragment_profile,c,false);
        AppData db=AppData.get(requireContext());
        ((TextView)v.findViewById(R.id.tv_username)).setText(db.userName.isEmpty()?"Your Name":db.userName);
        ((TextView)v.findViewById(R.id.tv_email)).setText(db.userEmail.isEmpty()?"Add email":db.userEmail);
        int streak=0; for(Models.Habit h:db.habits) streak=Math.max(streak,h.streak);
        ((TextView)v.findViewById(R.id.tv_stat_streak)).setText(String.valueOf(streak));
        ((TextView)v.findViewById(R.id.tv_stat_routines)).setText(String.valueOf(db.routines.size()));
        ((TextView)v.findViewById(R.id.tv_stat_habits)).setText(String.valueOf(db.habits.size()));
        EditText etName=v.findViewById(R.id.et_name); etName.setText(db.userName);
        EditText etEmail=v.findViewById(R.id.et_email); etEmail.setText(db.userEmail);
        v.findViewById(R.id.btn_save_profile).setOnClickListener(x->{
            db.userName=etName.getText().toString().trim(); if(db.userName.isEmpty())db.userName="You";
            db.userEmail=etEmail.getText().toString().trim(); db.save();
            ((TextView)v.findViewById(R.id.tv_username)).setText(db.userName);
            Toast.makeText(getContext(),"Profile saved",Toast.LENGTH_SHORT).show();
        });
        return v;
    }
}
