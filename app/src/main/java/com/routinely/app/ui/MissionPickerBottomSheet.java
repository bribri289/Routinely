package com.routinely.app.ui;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.routinely.app.R;
import com.routinely.app.data.Models;

public class MissionPickerBottomSheet extends BottomSheetDialogFragment {
    public interface OnMissionSelected { void onSelected(Models.Mission mission); }
    private OnMissionSelected listener;
    public void setListener(OnMissionSelected l){ this.listener=l; }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup c, @Nullable Bundle b){
        View v=inf.inflate(R.layout.fragment_mission_picker,c,false);
        v.findViewById(R.id.btn_close_mission).setOnClickListener(x->dismiss());

        bind(v,R.id.brain_row_color, Models.Mission.MEMORY,false);
        bind(v,R.id.brain_row_typing,Models.Mission.TYPING,false);
        bind(v,R.id.brain_row_math,  Models.Mission.MATH,  false);
        bind(v,R.id.body_row_step,   Models.Mission.STEPS, false);
        bind(v,R.id.body_row_qr,     Models.Mission.BARCODE,false);
        bind(v,R.id.body_row_shake,  Models.Mission.SHAKE, false);
        bind(v,R.id.body_row_photo,  Models.Mission.PHOTO, false);
        bind(v,R.id.body_row_squat,  Models.Mission.SQUATS,false);
        return v;
    }

    void bind(View root, int rowId, String type, boolean isPro){
        root.findViewById(rowId).setOnClickListener(v->{
            if(isPro){
                Toast.makeText(getContext(),"This mission requires PRO",Toast.LENGTH_SHORT).show();
                return;
            }
            if(listener!=null) listener.onSelected(new Models.Mission(type));
            dismiss();
        });
    }
}
