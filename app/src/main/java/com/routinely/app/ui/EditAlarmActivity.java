package com.routinely.app.ui;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import com.routinely.app.receivers.AlarmReceiver;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import java.util.*;

public class EditAlarmActivity extends AppCompatActivity {
    AppData db; Models.Alarm alarm; boolean isNew=false;
    LinearLayout missionsContainer;
    Models.Mission pendingBarcodeMission=null;
    TextView pendingBarcodeTv=null;
    String[] DAYS={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
    String[] SOUNDS={"Default ringtone","Gentle chime","Digital buzz","Ocean wave","Birds","Fanfare","Drumroll","Soft pulse"};

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_edit_alarm);
        db=AppData.get(this);
        int almId=getIntent().getIntExtra("alarmId",-1);
        if(almId==-1){isNew=true;alarm=new Models.Alarm();alarm.id=db.newId();}
        else{alarm=db.findAlarm(almId);if(alarm==null){finish();return;}}
        setup();
    }

    void setup(){
        findViewById(R.id.btn_back).setOnClickListener(v->finish());
        ((TextView)findViewById(R.id.tv_title)).setText(isNew?"New Alarm":"Edit Alarm");
        EditText etLabel=findViewById(R.id.et_label); etLabel.setText(alarm.label);
        TimePicker tp=findViewById(R.id.time_picker); tp.setIs24HourView(false); tp.setHour(alarm.hour); tp.setMinute(alarm.minute);

        LinearLayout daysRow=findViewById(R.id.days_row); daysRow.removeAllViews();
        for(int i=0;i<7;i++){final int idx=i;
            TextView chip=new TextView(this); chip.setText(DAYS[i]); chip.setPadding(26,12,26,12); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(alarm.repeatDays[i]?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{alarm.repeatDays[idx]=!alarm.repeatDays[idx];chip.setBackground(getDrawable(alarm.repeatDays[idx]?R.drawable.chip_bg_active:R.drawable.chip_bg));});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); daysRow.addView(chip);}

        Spinner soundSpin=findViewById(R.id.spinner_sound);
        ArrayAdapter<String> sa=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,SOUNDS);
        sa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); soundSpin.setAdapter(sa);
        soundSpin.setSelection(Math.min(alarm.soundIndex,SOUNDS.length-1));

        TextView tvCustomSound=findViewById(R.id.tv_custom_sound);
        if(alarm.customSoundUri!=null&&!alarm.customSoundUri.isEmpty()){
            tvCustomSound.setText("Custom: "+getFileNameFromUri(alarm.customSoundUri));
            tvCustomSound.setTextColor(0xFF10B981);
        }
        findViewById(R.id.btn_pick_sound).setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("audio/*");
            startActivityForResult(i,401);
        });
        findViewById(R.id.btn_system_alarm).setOnClickListener(v->{
            Intent i=new Intent(android.media.RingtoneManager.ACTION_RINGTONE_PICKER);
            i.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TYPE,android.media.RingtoneManager.TYPE_ALARM);
            i.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_TITLE,"Choose alarm sound");
            if(alarm.customSoundUri!=null&&!alarm.customSoundUri.isEmpty())
                i.putExtra(android.media.RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,android.net.Uri.parse(alarm.customSoundUri));
            startActivityForResult(i,402);
        });
        findViewById(R.id.btn_clear_sound).setOnClickListener(v->{
            alarm.customSoundUri="";
            tvCustomSound.setText("Using: "+SOUNDS[soundSpin.getSelectedItemPosition()]);
            tvCustomSound.setTextColor(0xFF9CA3AF);
        });

        // Wallpaper
        TextView tvWallpaper=findViewById(R.id.tv_wallpaper);
        if(alarm.wallpaperUri!=null&&!alarm.wallpaperUri.isEmpty()){
            tvWallpaper.setText(alarm.wallpaperIsVideo?"Video set":"Image set");
            tvWallpaper.setTextColor(0xFF10B981);
        }
        findViewById(R.id.btn_pick_wallpaper_image).setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*");
            startActivityForResult(i,601);
        });
        findViewById(R.id.btn_pick_wallpaper_video).setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*");
            startActivityForResult(i,602);
        });
        findViewById(R.id.btn_clear_wallpaper).setOnClickListener(v->{
            alarm.wallpaperUri=""; alarm.wallpaperIsVideo=false;
            tvWallpaper.setText("No wallpaper"); tvWallpaper.setTextColor(0xFF9CA3AF);
        });

        SeekBar volBar=findViewById(R.id.seekbar_volume); volBar.setProgress(alarm.volume);
        TextView tvVol=findViewById(R.id.tv_volume); tvVol.setText(alarm.volume+"%");
        volBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){alarm.volume=p;tvVol.setText(p+"%");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});

        ((Switch)findViewById(R.id.sw_gradual)).setChecked(alarm.gradualVolume);
        ((Switch)findViewById(R.id.sw_vibrate)).setChecked(alarm.vibrate);
        ((Switch)findViewById(R.id.sw_ultra_loud)).setChecked(alarm.ultraLoud);
        ((Switch)findViewById(R.id.sw_prevent_snooze)).setChecked(alarm.preventSnooze);
        ((Switch)findViewById(R.id.sw_mission_to_snooze)).setChecked(alarm.missionToSnooze);
        ((Switch)findViewById(R.id.sw_prevent_poweroff)).setChecked(alarm.preventPowerOff);
        ((Switch)findViewById(R.id.sw_play_screen_off)).setChecked(alarm.playWhenScreenOff);
        ((Switch)findViewById(R.id.sw_wake_check)).setChecked(alarm.wakeCheckEnabled);

        ((SeekBar)findViewById(R.id.seekbar_snooze)).setProgress(alarm.maxSnoozes);
        ((TextView)findViewById(R.id.tv_snooze_count)).setText(alarm.maxSnoozes+"x");
        ((SeekBar)findViewById(R.id.seekbar_snooze)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){alarm.maxSnoozes=Math.max(1,p);((TextView)findViewById(R.id.tv_snooze_count)).setText(alarm.maxSnoozes+"x");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});

        // Snooze duration
        SeekBar snoozeMinsBar=findViewById(R.id.seekbar_snooze_mins);
        TextView tvSnoozeMins=findViewById(R.id.tv_snooze_mins);
        if(snoozeMinsBar!=null){
            snoozeMinsBar.setProgress(alarm.snoozeMinutes>0?alarm.snoozeMinutes:5);
            if(tvSnoozeMins!=null) tvSnoozeMins.setText((alarm.snoozeMinutes>0?alarm.snoozeMinutes:5)+" min");
            snoozeMinsBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
                public void onProgressChanged(SeekBar s,int p,boolean u){alarm.snoozeMinutes=Math.max(1,p);if(tvSnoozeMins!=null)tvSnoozeMins.setText(alarm.snoozeMinutes+" min");}
                public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});
        }

        SeekBar wcBar=findViewById(R.id.seekbar_wc_delay); wcBar.setProgress(alarm.wakeCheckDelay);
        TextView tvWC=findViewById(R.id.tv_wc_delay); tvWC.setText(alarm.wakeCheckDelay+" min");
        wcBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){alarm.wakeCheckDelay=Math.max(1,p);tvWC.setText(alarm.wakeCheckDelay+" min");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});

        Spinner rSpin=findViewById(R.id.spinner_routine);
        List<String> rLabels=new ArrayList<>(); rLabels.add("No routine trigger");
        List<Integer> rIds=new ArrayList<>(); rIds.add(0);
        for(Models.Routine r:db.routines){rLabels.add(r.emoji+" "+r.name);rIds.add(r.id);}
        ArrayAdapter<String> ra=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,rLabels);
        ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); rSpin.setAdapter(ra);
        for(int i=0;i<rIds.size();i++) if(rIds.get(i)==alarm.linkedRoutineId){rSpin.setSelection(i);break;}
        final List<Integer> routineIds=rIds;

        missionsContainer=findViewById(R.id.missions_container);
        rebuildMissions();
        addMissionBtn(Models.Mission.MATH,"Math"); addMissionBtn(Models.Mission.MEMORY,"Memory");
        addMissionBtn(Models.Mission.TYPING,"Typing"); addMissionBtn(Models.Mission.SHAKE,"Shake");
        addMissionBtn(Models.Mission.SQUATS,"Squats"); addMissionBtn(Models.Mission.STEPS,"Steps");
        addMissionBtn(Models.Mission.BARCODE,"Barcode"); addMissionBtn(Models.Mission.PHOTO,"Photo");

        findViewById(R.id.btn_preview_missions).setOnClickListener(v->{
            if(alarm.missions.isEmpty()){Toast.makeText(this,"Add a mission first",Toast.LENGTH_SHORT).show();return;}
            Intent i=new Intent(this,MissionActivity.class);
            i.putExtra("missions",(java.io.Serializable)alarm.missions);
            i.putExtra("preview",true); startActivity(i);
        });

        findViewById(R.id.btn_save).setOnClickListener(v->{
            alarm.label=etLabel.getText().toString().trim(); if(alarm.label.isEmpty())alarm.label="Alarm";
            alarm.hour=tp.getHour(); alarm.minute=tp.getMinute();
            alarm.soundIndex=soundSpin.getSelectedItemPosition();
            alarm.gradualVolume=((Switch)findViewById(R.id.sw_gradual)).isChecked();
            alarm.vibrate=((Switch)findViewById(R.id.sw_vibrate)).isChecked();
            alarm.ultraLoud=((Switch)findViewById(R.id.sw_ultra_loud)).isChecked();
            alarm.preventSnooze=((Switch)findViewById(R.id.sw_prevent_snooze)).isChecked();
            alarm.missionToSnooze=((Switch)findViewById(R.id.sw_mission_to_snooze)).isChecked();
            alarm.preventPowerOff=((Switch)findViewById(R.id.sw_prevent_poweroff)).isChecked();
            alarm.playWhenScreenOff=((Switch)findViewById(R.id.sw_play_screen_off)).isChecked();
            alarm.wakeCheckEnabled=((Switch)findViewById(R.id.sw_wake_check)).isChecked();
            alarm.linkedRoutineId=routineIds.get(rSpin.getSelectedItemPosition());
            if(isNew)db.alarms.add(alarm);
            alarm.enabled=true; db.save();
            AlarmReceiver.schedule(this,alarm);
            Toast.makeText(this,"Alarm saved for "+alarm.getTimeString(),Toast.LENGTH_SHORT).show();
            finish();
        });

        View btnDel=findViewById(R.id.btn_delete); btnDel.setVisibility(isNew?View.GONE:View.VISIBLE);
        btnDel.setOnClickListener(v->{AlarmReceiver.cancel(this,alarm.id);db.alarms.remove(alarm);db.save();finish();});
    }

    @Override protected void onActivityResult(int req, int res, Intent data){
        super.onActivityResult(req,res,data);
        TextView tvCustomSound=findViewById(R.id.tv_custom_sound);
        if(req==101&&res==RESULT_OK){
            for(Models.Mission m:alarm.missions) if(m.type.equals(Models.Mission.PHOTO)){m.hasReferencePhoto=true;}
            rebuildMissions();
        } else if(req==401&&res==RESULT_OK&&data!=null){
            android.net.Uri uri=data.getData();
            if(uri!=null){
                getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);
                alarm.customSoundUri=uri.toString();
                if(tvCustomSound!=null){tvCustomSound.setText("Custom: "+getFileNameFromUri(uri.toString()));tvCustomSound.setTextColor(0xFF10B981);}
            }
        } else if(req==402&&res==RESULT_OK&&data!=null){
            android.net.Uri uri=(android.net.Uri)data.getParcelableExtra(android.media.RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
            if(uri!=null){
                alarm.customSoundUri=uri.toString();
                android.media.Ringtone r=android.media.RingtoneManager.getRingtone(this,uri);
                String name=r!=null?r.getTitle(this):"Selected ringtone";
                if(tvCustomSound!=null){tvCustomSound.setText("Sound: "+name);tvCustomSound.setTextColor(0xFF10B981);}
            }
        } else if(req==601&&res==RESULT_OK&&data!=null){
            android.net.Uri uri=data.getData();
            if(uri!=null){
                try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception e){}
                alarm.wallpaperUri=uri.toString(); alarm.wallpaperIsVideo=false;
                TextView tvW=findViewById(R.id.tv_wallpaper); if(tvW!=null){tvW.setText("Image set");tvW.setTextColor(0xFF10B981);}
            }
        } else if(req==602&&res==RESULT_OK&&data!=null){
            android.net.Uri uri=data.getData();
            if(uri!=null){
                try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception e){}
                alarm.wallpaperUri=uri.toString(); alarm.wallpaperIsVideo=true;
                TextView tvW=findViewById(R.id.tv_wallpaper); if(tvW!=null){tvW.setText("Video set");tvW.setTextColor(0xFF10B981);}
            }
        }
        // Handle ZXing barcode scan result
        com.google.zxing.integration.android.IntentResult scanResult=
            com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(req,res,data);
        if(scanResult!=null&&scanResult.getContents()!=null&&pendingBarcodeMission!=null){
            pendingBarcodeMission.registeredBarcode=scanResult.getContents();
            if(pendingBarcodeTv!=null){
                pendingBarcodeTv.setText("Registered: "+scanResult.getContents());
                pendingBarcodeTv.setTextColor(0xFF10B981);
            }
            Toast.makeText(this,"Barcode registered!",Toast.LENGTH_SHORT).show();
        }
    }

    String getFileNameFromUri(String uriStr){
        try{
            android.net.Uri uri=android.net.Uri.parse(uriStr);
            android.database.Cursor c=getContentResolver().query(uri,null,null,null,null);
            if(c!=null&&c.moveToFirst()){int idx=c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);if(idx>=0){String name=c.getString(idx);c.close();return name;}c.close();}
        }catch(Exception e){}
        try{return uriStr.substring(uriStr.lastIndexOf('/')+1);}catch(Exception e){return "custom sound";}
    }

    void addMissionBtn(String type, String label){
        LinearLayout row=findViewById(R.id.mission_buttons_row);
        Button btn=new Button(this); btn.setText(label); btn.setTextSize(11); btn.setTextColor(0xFFFFFFFF);
        btn.setBackground(getDrawable(R.drawable.chip_bg)); btn.setPadding(16,8,16,8);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,4,4,4); btn.setLayoutParams(lp);
        btn.setOnClickListener(v->{
            if(alarm.missions.size()>=5){Toast.makeText(this,"Max 5 missions",Toast.LENGTH_SHORT).show();return;}
            alarm.missions.add(new Models.Mission(type)); rebuildMissions();
        });
        row.addView(btn);
    }

    void rebuildMissions(){
        missionsContainer.removeAllViews();
        LayoutInflater inf=LayoutInflater.from(this);
        for(int i=0;i<alarm.missions.size();i++){
            Models.Mission m=alarm.missions.get(i);
            View item=inf.inflate(R.layout.item_mission_edit,missionsContainer,false);
            ((TextView)item.findViewById(R.id.tv_mission_icon)).setText(m.getEmoji());
            ((TextView)item.findViewById(R.id.tv_mission_name)).setText(m.getDisplayName());
            buildMissionConfig(item.findViewById(R.id.mission_config),m);
            final int idx=i;
            item.findViewById(R.id.btn_remove_mission).setOnClickListener(v->{alarm.missions.remove(idx);rebuildMissions();});
            Switch swReq=item.findViewById(R.id.sw_required); swReq.setChecked(m.required);
            swReq.setOnCheckedChangeListener((btn,chk)->m.required=chk);
            missionsContainer.addView(item);
        }
    }

    void buildMissionConfig(LinearLayout layout, Models.Mission m){
        layout.removeAllViews();
        switch(m.type){
            case Models.Mission.MATH: buildMathConfig(layout,m); break;
            case Models.Mission.MEMORY: buildMemoryConfig(layout,m); break;
            case Models.Mission.TYPING: buildTypingConfig(layout,m); break;
            case Models.Mission.SHAKE: buildCountConfig(layout,m,"Shakes",5,200); break;
            case Models.Mission.SQUATS: buildCountConfig(layout,m,"Squats",5,50); break;
            case Models.Mission.STEPS: buildCountConfig(layout,m,"Steps",10,200); break;
            case Models.Mission.BARCODE: buildBarcodeConfig(layout,m); break;
            case Models.Mission.PHOTO: buildPhotoConfig(layout,m); break;
        }
    }

    void addLabel(ViewGroup p,String t){TextView tv=new TextView(this);tv.setText(t);tv.setTextColor(0xFF9CA3AF);tv.setTextSize(12);tv.setPadding(0,8,0,4);p.addView(tv);}

    void buildMathConfig(LinearLayout l, Models.Mission m){
        addLabel(l,"Difficulty");
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(String d:new String[]{"easy","medium","hard","extreme"}){
            TextView chip=new TextView(this); chip.setText(d); chip.setPadding(20,10,20,10); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(11);
            chip.setBackground(getDrawable(m.difficulty.equals(d)?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{m.difficulty=d;buildMathConfig(l,m);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); row.addView(chip);}
        l.addView(row);
        addLabel(l,"Questions: "+m.questionCount);
        SeekBar sb=new SeekBar(this); sb.setMax(10); sb.setProgress(m.questionCount);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){m.questionCount=Math.max(1,p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        l.addView(sb);
        addLabel(l,"Operations");
        LinearLayout ops=new LinearLayout(this); ops.setOrientation(LinearLayout.HORIZONTAL);
        addOpCheck(ops,m,"+ Add",0); addOpCheck(ops,m,"- Sub",1); addOpCheck(ops,m,"x Mul",2); addOpCheck(ops,m,"/ Div",3);
        l.addView(ops);
    }

    void addOpCheck(LinearLayout row,Models.Mission m,String label,int type){
        CheckBox cb=new CheckBox(this); cb.setText(label); cb.setTextColor(0xFFFFFFFF); cb.setTextSize(12);
        boolean chk=(type==0&&m.opAdd)||(type==1&&m.opSub)||(type==2&&m.opMul)||(type==3&&m.opDiv);
        cb.setChecked(chk);
        cb.setOnCheckedChangeListener((btn,c)->{if(type==0)m.opAdd=c;else if(type==1)m.opSub=c;else if(type==2)m.opMul=c;else m.opDiv=c;});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,0,12,0); cb.setLayoutParams(lp);
        row.addView(cb);
    }

    void buildMemoryConfig(LinearLayout l, Models.Mission m){
        addLabel(l,"Grid size");
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(String g:new String[]{"2x2","2x3","3x4","4x4"}){
            TextView chip=new TextView(this); chip.setText(g); chip.setPadding(20,10,20,10); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(m.gridSize.equals(g)?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{m.gridSize=g;buildMemoryConfig(l,m);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); row.addView(chip);}
        l.addView(row);
    }

    void buildTypingConfig(LinearLayout l, Models.Mission m){
        addLabel(l,"Text length");
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(String tl:new String[]{"short","medium","long"}){
            TextView chip=new TextView(this); chip.setText(tl); chip.setPadding(20,10,20,10); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(m.textLength.equals(tl)?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{m.textLength=tl;buildTypingConfig(l,m);});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); row.addView(chip);}
        l.addView(row);
        CheckBox caps=new CheckBox(this); caps.setText("Require correct capitalisation"); caps.setTextColor(0xFFFFFFFF); caps.setTextSize(12); caps.setChecked(m.requireCaps);
        caps.setOnCheckedChangeListener((btn,c)->m.requireCaps=c); l.addView(caps);
        CheckBox punct=new CheckBox(this); punct.setText("Require correct punctuation"); punct.setTextColor(0xFFFFFFFF); punct.setTextSize(12); punct.setChecked(m.requirePunct);
        punct.setOnCheckedChangeListener((btn,c)->m.requirePunct=c); l.addView(punct);
    }

    void buildCountConfig(LinearLayout l,Models.Mission m,String unit,int min,int max){
        addLabel(l,unit+": "+m.targetCount);
        SeekBar sb=new SeekBar(this); sb.setMax(max); sb.setProgress(m.targetCount);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean u){m.targetCount=Math.max(min,p);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
        l.addView(sb);
        addLabel(l,"Sensitivity");
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for(String s:new String[]{"low","medium","high"}){
            TextView chip=new TextView(this); chip.setText(s); chip.setPadding(20,10,20,10); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(m.sensitivity.equals(s)?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{m.sensitivity=s;});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); row.addView(chip);}
        l.addView(row);
    }

    void buildBarcodeConfig(LinearLayout l, Models.Mission m){
        addLabel(l,"Item name (e.g. Toothpaste in bathroom)");
        EditText et=new EditText(this); et.setHint("Label"); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(0xFF6B7280); et.setBackground(getDrawable(R.drawable.input_bg)); et.setPadding(20,14,20,14); et.setText(m.barcodeLabel);
        et.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){m.barcodeLabel=s.toString();}public void afterTextChanged(android.text.Editable s){}});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,4,0,8); et.setLayoutParams(lp); l.addView(et);
        TextView tvReg=new TextView(this); tvReg.setText(m.registeredBarcode.isEmpty()?"Not registered":"Registered"); tvReg.setTextColor(m.registeredBarcode.isEmpty()?0xFFF59E0B:0xFF10B981); tvReg.setTextSize(12); l.addView(tvReg);
        Button btn=new Button(this); btn.setText("Scan to register"); btn.setBackground(getDrawable(R.drawable.chip_bg_active)); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(12);
        btn.setOnClickListener(v->{
            // ZXing embedded camera scanner
            try {
                pendingBarcodeMission=m; pendingBarcodeTv=tvReg;
                Intent scanIntent=new Intent(this,com.journeyapps.barcodescanner.CaptureActivity.class);
                scanIntent.putExtra("PROMPT_MESSAGE","Scan item barcode to register");
                startActivityForResult(scanIntent,501);
            } catch(Exception e){
                android.app.AlertDialog.Builder d=new android.app.AlertDialog.Builder(this);
                d.setTitle("Register barcode"); EditText input=new EditText(this); input.setHint("Enter barcode value");
                d.setView(input); d.setPositiveButton("Register",(dlg,w)->{m.registeredBarcode=input.getText().toString().trim();tvReg.setText("Registered");tvReg.setTextColor(0xFF10B981);});
                d.setNegativeButton("Cancel",null); d.show();
            }
        });
        l.addView(btn);
    }

    void buildPhotoConfig(LinearLayout l, Models.Mission m){
        addLabel(l,"Target location or object");
        EditText et=new EditText(this); et.setHint("e.g. Bathroom sink"); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(0xFF6B7280); et.setBackground(getDrawable(R.drawable.input_bg)); et.setPadding(20,14,20,14); et.setText(m.photoLabel);
        et.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){m.photoLabel=s.toString();}public void afterTextChanged(android.text.Editable s){}});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,4,0,8); et.setLayoutParams(lp); l.addView(et);
        TextView tv=new TextView(this); tv.setText(m.hasReferencePhoto?"Reference photo saved":"No reference photo"); tv.setTextColor(m.hasReferencePhoto?0xFF10B981:0xFFF59E0B); tv.setTextSize(12); l.addView(tv);
        Button btn=new Button(this); btn.setText("Take reference photo"); btn.setBackground(getDrawable(R.drawable.chip_bg)); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(12);
        btn.setOnClickListener(v->{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,101);});
        l.addView(btn);
    }
}
