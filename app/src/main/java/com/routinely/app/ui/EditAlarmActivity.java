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
    // No longer a container — we use tile chips row
    Models.Mission pendingBarcodeMission=null;
    TextView pendingBarcodeTv=null;
    String[] DAYS={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
    String[] SOUNDS={"Default ringtone","Gentle chime","Digital buzz","Ocean wave","Birds","Fanfare","Drumroll","Soft pulse"};
    // NumberPicker selection storage
    int pickerHour=7, pickerMinute=0, pickerAmPm=0; // 0=AM,1=PM

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

        // ── Drum-roll time picker ──
        NumberPicker npHour=findViewById(R.id.np_hour);
        NumberPicker npMin=findViewById(R.id.np_minute);
        NumberPicker npAmPm=findViewById(R.id.np_ampm);
        // Hours 1-12
        String[] hours=new String[12];
        for(int i=0;i<12;i++) hours[i]=String.valueOf(i+1);
        npHour.setMinValue(0); npHour.setMaxValue(11); npHour.setDisplayedValues(hours); npHour.setWrapSelectorWheel(true);
        // Minutes 00-59
        String[] mins=new String[60];
        for(int i=0;i<60;i++) mins[i]=String.format("%02d",i);
        npMin.setMinValue(0); npMin.setMaxValue(59); npMin.setDisplayedValues(mins); npMin.setWrapSelectorWheel(true);
        // AM/PM
        npAmPm.setMinValue(0); npAmPm.setMaxValue(1); npAmPm.setDisplayedValues(new String[]{"AM","PM"}); npAmPm.setWrapSelectorWheel(false);
        // Init from alarm
        pickerAmPm=alarm.hour>=12?1:0;
        pickerHour=alarm.hour%12; if(pickerHour==0)pickerHour=12;
        pickerMinute=alarm.minute;
        npHour.setValue(pickerHour-1); npMin.setValue(pickerMinute); npAmPm.setValue(pickerAmPm);
        npHour.setOnValueChangedListener((p,o,n)->pickerHour=n+1);
        npMin.setOnValueChangedListener((p,o,n)->pickerMinute=n);
        npAmPm.setOnValueChangedListener((p,o,n)->pickerAmPm=n);
        // Style NumberPickers
        styleNumberPicker(npHour); styleNumberPicker(npMin); styleNumberPicker(npAmPm);

        // ── Repeat Days ──
        LinearLayout daysRow=findViewById(R.id.days_row); daysRow.removeAllViews();
        for(int i=0;i<7;i++){final int idx=i;
            TextView chip=new TextView(this); chip.setText(DAYS[i]); chip.setPadding(26,12,26,12); chip.setTextColor(0xFFFFFFFF); chip.setTextSize(12);
            chip.setBackground(getDrawable(alarm.repeatDays[i]?R.drawable.chip_bg_active:R.drawable.chip_bg));
            chip.setOnClickListener(v->{alarm.repeatDays[idx]=!alarm.repeatDays[idx];chip.setBackground(getDrawable(alarm.repeatDays[idx]?R.drawable.chip_bg_active:R.drawable.chip_bg));});
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(4,0,4,0); chip.setLayoutParams(lp); daysRow.addView(chip);}

        // ── Mission tile row ──
        ((TextView)findViewById(R.id.tv_mission_count)).setText(alarm.missions.size()+"/5");
        rebuildMissionTiles();

        // Wake check
        ((Switch)findViewById(R.id.sw_wake_check)).setChecked(alarm.wakeCheckEnabled);
        int wcMin=Math.max(1,alarm.wakeCheckDelay);
        ((TextView)findViewById(R.id.tv_wc_delay_label)).setText("After "+wcMin+" min ›");

        // ── Sound ──
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
            i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("audio/*"); startActivityForResult(i,401);
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

        // Volume
        SeekBar volBar=findViewById(R.id.seekbar_volume); volBar.setProgress(alarm.volume);
        TextView tvVol=findViewById(R.id.tv_volume); tvVol.setText(alarm.volume+"%");
        volBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){alarm.volume=p;tvVol.setText(p+"%");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});

        ((Switch)findViewById(R.id.sw_ultra_loud)).setChecked(alarm.ultraLoud);
        ((Switch)findViewById(R.id.sw_gradual)).setChecked(alarm.gradualVolume);
        ((Switch)findViewById(R.id.sw_vibrate)).setChecked(alarm.vibrate);

        // Wallpaper
        TextView tvWallpaper=findViewById(R.id.tv_wallpaper);
        if(alarm.wallpaperUri!=null&&!alarm.wallpaperUri.isEmpty()){
            tvWallpaper.setText(alarm.wallpaperIsVideo?"Video set":"Image set"); tvWallpaper.setTextColor(0xFF10B981);
        }
        findViewById(R.id.btn_pick_wallpaper_image).setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("image/*"); startActivityForResult(i,601);
        });
        findViewById(R.id.btn_pick_wallpaper_video).setOnClickListener(v->{
            Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.addCategory(Intent.CATEGORY_OPENABLE); i.setType("video/*"); startActivityForResult(i,602);
        });
        findViewById(R.id.btn_clear_wallpaper).setOnClickListener(v->{
            alarm.wallpaperUri=""; alarm.wallpaperIsVideo=false;
            tvWallpaper.setText("No wallpaper"); tvWallpaper.setTextColor(0xFF9CA3AF);
        });

        // Snooze
        ((Switch)findViewById(R.id.sw_prevent_snooze)).setChecked(alarm.preventSnooze);
        ((Switch)findViewById(R.id.sw_mission_to_snooze)).setChecked(alarm.missionToSnooze);
        SeekBar snoozeBar=findViewById(R.id.seekbar_snooze); snoozeBar.setProgress(alarm.maxSnoozes);
        TextView tvSnooze=findViewById(R.id.tv_snooze_count); tvSnooze.setText(alarm.maxSnoozes+"x");
        snoozeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){alarm.maxSnoozes=Math.max(1,p);tvSnooze.setText(alarm.maxSnoozes+"x");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});
        SeekBar snoozeMinBar=findViewById(R.id.seekbar_snooze_mins); snoozeMinBar.setProgress(alarm.snoozeMinutes>0?alarm.snoozeMinutes:5);
        TextView tvSnoozeMins=findViewById(R.id.tv_snooze_mins); tvSnoozeMins.setText((alarm.snoozeMinutes>0?alarm.snoozeMinutes:5)+" min");
        snoozeMinBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean u){alarm.snoozeMinutes=Math.max(1,p);tvSnoozeMins.setText(alarm.snoozeMinutes+" min");}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}});

        // Routine trigger
        Spinner rSpin=findViewById(R.id.spinner_routine);
        List<String> rLabels=new ArrayList<>(); rLabels.add("No routine trigger");
        List<Integer> rIds=new ArrayList<>(); rIds.add(0);
        for(Models.Routine r:db.routines){rLabels.add(r.emoji+" "+r.name);rIds.add(r.id);}
        ArrayAdapter<String> ra=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,rLabels);
        ra.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); rSpin.setAdapter(ra);
        for(int i=0;i<rIds.size();i++) if(rIds.get(i)==alarm.linkedRoutineId){rSpin.setSelection(i);break;}
        final List<Integer> routineIds=rIds;

        // Save
        findViewById(R.id.btn_save).setOnClickListener(v->{
            alarm.label=etLabel.getText().toString().trim(); if(alarm.label.isEmpty())alarm.label="Alarm";
            // Reconstruct 24h from drum-roll
            int h24=pickerHour%12; if(pickerAmPm==1) h24+=12;
            alarm.hour=h24; alarm.minute=pickerMinute;
            alarm.soundIndex=soundSpin.getSelectedItemPosition();
            alarm.gradualVolume=((Switch)findViewById(R.id.sw_gradual)).isChecked();
            alarm.vibrate=((Switch)findViewById(R.id.sw_vibrate)).isChecked();
            alarm.ultraLoud=((Switch)findViewById(R.id.sw_ultra_loud)).isChecked();
            alarm.preventSnooze=((Switch)findViewById(R.id.sw_prevent_snooze)).isChecked();
            alarm.missionToSnooze=((Switch)findViewById(R.id.sw_mission_to_snooze)).isChecked();
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

    void styleNumberPicker(NumberPicker np){
        np.setTextColor(getColor(R.color.text_primary));
        np.setSelectionDividerHeight(2);
        try{
            java.lang.reflect.Field f=NumberPicker.class.getDeclaredField("mSelectionDivider");
            f.setAccessible(true);
            f.set(np,new android.graphics.drawable.ColorDrawable(getColor(R.color.orange)));
        }catch(Exception ignored){}
    }

    void rebuildMissionTiles(){
        LinearLayout row=findViewById(R.id.mission_tiles_row); row.removeAllViews();
        for(int i=0;i<alarm.missions.size();i++){
            final int idx=i;
            Models.Mission m=alarm.missions.get(i);
            LinearLayout tile=new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL);
            tile.setBackground(getDrawable(R.drawable.card_bg2)); tile.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(80,80); lp.setMargins(0,0,8,0); tile.setLayoutParams(lp);
            TextView icon=new TextView(this); icon.setText(m.getEmoji()); icon.setTextSize(20); icon.setGravity(android.view.Gravity.CENTER); tile.addView(icon);
            TextView name=new TextView(this); name.setText(m.getDisplayName()); name.setTextColor(0xFF9CA3AF); name.setTextSize(9); name.setGravity(android.view.Gravity.CENTER); name.setMaxLines(1); tile.addView(name);
            // × badge
            tile.setOnLongClickListener(v->{
                alarm.missions.remove(idx); rebuildMissionTiles();
                ((TextView)findViewById(R.id.tv_mission_count)).setText(alarm.missions.size()+"/5"); return true;
            });
            row.addView(tile);
        }
        // + add chip
        if(alarm.missions.size()<5){
            TextView addChip=new TextView(this); addChip.setText("+"); addChip.setTextSize(28);
            addChip.setTextColor(0xFFFFFFFF); addChip.setGravity(android.view.Gravity.CENTER);
            addChip.setBackground(getDrawable(R.drawable.card_bg2));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(80,80); lp.setMargins(0,0,8,0); addChip.setLayoutParams(lp);
            addChip.setOnClickListener(v->openMissionPicker());
            row.addView(addChip);
        }
    }

    void openMissionPicker(){
        MissionPickerBottomSheet sheet=new MissionPickerBottomSheet();
        sheet.setListener(mission->{
            if(alarm.missions.size()<5){
                alarm.missions.add(mission); rebuildMissionTiles();
                ((TextView)findViewById(R.id.tv_mission_count)).setText(alarm.missions.size()+"/5");
            }
        });
        sheet.show(getSupportFragmentManager(),"missions");
    }

    @Override protected void onActivityResult(int req, int res, Intent data){
        super.onActivityResult(req,res,data);
        TextView tvCustomSound=findViewById(R.id.tv_custom_sound);
        if(req==101&&res==RESULT_OK){
            for(Models.Mission m:alarm.missions) if(m.type.equals(Models.Mission.PHOTO)){m.hasReferencePhoto=true;}
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
        com.google.zxing.integration.android.IntentResult scanResult=
            com.google.zxing.integration.android.IntentIntegrator.parseActivityResult(req,res,data);
        if(scanResult!=null&&scanResult.getContents()!=null&&pendingBarcodeMission!=null){
            pendingBarcodeMission.registeredBarcode=scanResult.getContents();
            if(pendingBarcodeTv!=null){pendingBarcodeTv.setText("Registered: "+scanResult.getContents());pendingBarcodeTv.setTextColor(0xFF10B981);}
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

    // Keep mission config builders for future config sheet invocation
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
    }

    void buildBarcodeConfig(LinearLayout l, Models.Mission m){
        addLabel(l,"Item name (e.g. Toothpaste in bathroom)");
        EditText et=new EditText(this); et.setHint("Label"); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(0xFF6B7280); et.setBackground(getDrawable(R.drawable.input_bg)); et.setPadding(20,14,20,14); et.setText(m.barcodeLabel);
        et.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){}public void onTextChanged(CharSequence s,int a,int b,int c){m.barcodeLabel=s.toString();}public void afterTextChanged(android.text.Editable s){}});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,4,0,8); et.setLayoutParams(lp); l.addView(et);
        TextView tvReg=new TextView(this); tvReg.setText(m.registeredBarcode.isEmpty()?"Not registered":"Registered"); tvReg.setTextColor(m.registeredBarcode.isEmpty()?0xFFF59E0B:0xFF10B981); tvReg.setTextSize(12); l.addView(tvReg);
        Button btn=new Button(this); btn.setText("Scan to register"); btn.setBackground(getDrawable(R.drawable.chip_bg_active)); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(12);
        btn.setOnClickListener(v->{
            try{
                pendingBarcodeMission=m; pendingBarcodeTv=tvReg;
                Intent scanIntent=new Intent(this,com.journeyapps.barcodescanner.CaptureActivity.class);
                scanIntent.putExtra("PROMPT_MESSAGE","Scan item barcode to register");
                startActivityForResult(scanIntent,501);
            }catch(Exception e){
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
        Button btn=new Button(this); btn.setText("Take reference photo"); btn.setBackground(getDrawable(R.drawable.chip_bg)); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(12);
        btn.setOnClickListener(v->{Intent i=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);if(i.resolveActivity(getPackageManager())!=null)startActivityForResult(i,101);});
        l.addView(btn);
    }
}
