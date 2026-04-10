package com.routinely.app.ui;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.util.*;

public class MissionActivity extends AppCompatActivity {
    List<Models.Mission> missions;
    String barcodeTarget="";
    int curIdx=0, alarmId=-1;
    boolean preview=false;
    int shakeCount=0;
    SensorManager sensorMgr;
    Sensor accel;
    // Shake tracking
    SensorEventListener shakeListener;
    float lastX=0,lastY=0,lastZ=0;
    boolean shakeInitialized=false;
    static final float SHAKE_THRESHOLD=15f;
    // Barcode
    static final int REQ_BARCODE_DISMISS=301;

    @SuppressWarnings("unchecked")
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        setContentView(R.layout.activity_mission);
        missions=(List<Models.Mission>)getIntent().getSerializableExtra("missions");
        alarmId=getIntent().getIntExtra("alarmId",-1);
        preview=getIntent().getBooleanExtra("preview",false);
        if(missions==null||missions.isEmpty()){finish();return;}
        sensorMgr=(SensorManager)getSystemService(SENSOR_SERVICE);
        if(sensorMgr!=null) accel=sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        showMission(0);
    }

    @Override protected void onPause(){
        super.onPause();
        if(shakeListener!=null&&sensorMgr!=null) sensorMgr.unregisterListener(shakeListener);
    }

    @Override protected void onResume(){
        super.onResume();
        // Re-register shake listener if we're on a shake mission
        if(shakeListener!=null&&sensorMgr!=null&&accel!=null)
            sensorMgr.registerListener(shakeListener,accel,SensorManager.SENSOR_DELAY_GAME);
    }

    void showMission(int idx){
        // Unregister any previous shake listener when moving between missions
        if(shakeListener!=null&&sensorMgr!=null){sensorMgr.unregisterListener(shakeListener);shakeListener=null;}
        curIdx=idx;
        if(idx>=missions.size()){onAllComplete();return;}
        Models.Mission m=missions.get(idx);
        ((TextView)findViewById(R.id.tv_mission_progress)).setText("Mission "+(idx+1)+" of "+missions.size());
        ((ProgressBar)findViewById(R.id.mission_progress_bar)).setMax(missions.size());
        ((ProgressBar)findViewById(R.id.mission_progress_bar)).setProgress(idx+1);
        ((TextView)findViewById(R.id.tv_mission_emoji)).setText(m.getEmoji());
        ((TextView)findViewById(R.id.tv_mission_name)).setText(m.getDisplayName());
        LinearLayout config=findViewById(R.id.mission_content);
        config.removeAllViews();
        switch(m.type){
            case Models.Mission.MATH:    buildMath(config,m);   break;
            case Models.Mission.MEMORY:  buildMemory(config,m); break;
            case Models.Mission.TYPING:  buildTyping(config,m); break;
            case Models.Mission.SHAKE:   buildShake(config,m);  break;
            case Models.Mission.SQUATS:
            case Models.Mission.STEPS:   buildCount(config,m);  break;
            case Models.Mission.BARCODE: buildBarcode(config,m);break;
            case Models.Mission.PHOTO:   buildPhoto(config,m);  break;
        }
        Button btnSkip=findViewById(R.id.btn_skip_mission);
        btnSkip.setVisibility(!m.required?View.VISIBLE:View.GONE);
        btnSkip.setOnClickListener(v->showMission(curIdx+1));
    }

    void addLabel(ViewGroup p, String t){
        TextView tv=new TextView(this);
        tv.setText(t);
        tv.setTextColor(0xFF9CA3AF);
        tv.setTextSize(13);
        tv.setPadding(0,8,0,8);
        tv.setGravity(android.view.Gravity.CENTER);
        p.addView(tv);
    }

    void addBigText(ViewGroup p, String t, int color){
        TextView tv=new TextView(this);
        tv.setText(t);
        tv.setTextColor(color);
        tv.setTextSize(32);
        tv.setTypeface(null,android.graphics.Typeface.BOLD);
        tv.setPadding(0,16,0,16);
        tv.setGravity(android.view.Gravity.CENTER);
        p.addView(tv);
    }

    void buildMath(LinearLayout l, Models.Mission m){
        nextMathQ(l,m,new int[]{0});
    }

    void nextMathQ(LinearLayout l, Models.Mission m, int[] correct){
        l.removeAllViews();
        int a, bb;
        switch(m.difficulty){
            case "easy":    a=(int)(Math.random()*10)+1; bb=(int)(Math.random()*5)+1;   break;
            case "hard":    a=(int)(Math.random()*50)+10; bb=(int)(Math.random()*20)+5; break;
            case "extreme": a=(int)(Math.random()*99)+10; bb=(int)(Math.random()*50)+10;break;
            default:        a=(int)(Math.random()*20)+5;  bb=(int)(Math.random()*10)+2; break;
        }
        List<String> ops=new ArrayList<>();
        if(m.opAdd) ops.add("+");
        if(m.opSub) ops.add("-");
        if(m.opMul) ops.add("x");
        if(ops.isEmpty()) ops.add("+");
        String op=ops.get((int)(Math.random()*ops.size()));
        final int ans=op.equals("+")?(a+bb):op.equals("-")?(a-bb):(a*bb);
        addLabel(l,"Question "+(correct[0]+1)+" of "+m.questionCount+" ("+m.difficulty+")");
        addBigText(l,a+" "+op+" "+bb+" = ?",0xFFF97316);
        EditText et=new EditText(this);
        et.setHint("Your answer");
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF6B7280);
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
        et.setBackground(getDrawable(R.drawable.input_bg));
        et.setPadding(20,14,20,14);
        et.setTextSize(20);
        et.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,16,0,8); et.setLayoutParams(lp); l.addView(et);
        TextView tvErr=new TextView(this); tvErr.setTextColor(0xFFEF4444); tvErr.setTextSize(13); tvErr.setGravity(android.view.Gravity.CENTER); l.addView(tvErr);
        Button btn=new Button(this); btn.setText("Submit"); btn.setBackground(getDrawable(R.drawable.btn_primary_bg)); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(15);
        lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,120); lp.setMargins(0,16,0,0); btn.setLayoutParams(lp);
        btn.setOnClickListener(v->{
            try{
                int val=Integer.parseInt(et.getText().toString());
                if(val==ans){correct[0]++;if(correct[0]>=m.questionCount)showMission(curIdx+1);else nextMathQ(l,m,correct);}
                else{tvErr.setText("Incorrect! Try again.");et.setText("");}
            }catch(Exception e){tvErr.setText("Enter a number");}
        });
        l.addView(btn);
    }

    void buildMemory(LinearLayout l, Models.Mission m){
        addLabel(l,"Match all pairs to continue");
        String[] parts=m.gridSize.split("x");
        int cols=Integer.parseInt(parts[0]);
        int rows=Integer.parseInt(parts[1]);
        int pairs=(cols*rows)/2;
        String[] emojis={"[star]","[fire]","[water]","[moon]","[target]","[bulb]","[music]","[trophy]","[rainbow]","[snow]","[butterfly]","[bird]"};
        String[] emojiVals={"⭐","🔥","💧","🌙","🎯","💡","🎵","🏆","🌈","❄️","🦋","🐦"};
        List<String> deck=new ArrayList<>();
        for(int i=0;i<pairs;i++){deck.add(emojiVals[i%emojiVals.length]);deck.add(emojiVals[i%emojiVals.length]);}
        Collections.shuffle(deck);
        GridLayout grid=new GridLayout(this); grid.setColumnCount(cols); grid.setRowCount(rows);
        List<TextView> cards=new ArrayList<>();
        int[] matched={0};
        int[] flipped={-1};
        for(int i=0;i<deck.size();i++){
            final int idx=i;
            TextView card=new TextView(this);
            card.setText("?");
            card.setTextSize(24);
            card.setGravity(android.view.Gravity.CENTER);
            card.setPadding(8,8,8,8);
            card.setBackground(getDrawable(R.drawable.chip_bg));
            card.setMinWidth(72); card.setMinHeight(72);
            GridLayout.LayoutParams glp=new GridLayout.LayoutParams(); glp.setMargins(6,6,6,6); card.setLayoutParams(glp);
            final String val=deck.get(i);
            card.setOnClickListener(v->{
                if(card.getText().toString().equals(val)) return;
                card.setText(val);
                if(flipped[0]==-1){flipped[0]=idx;}
                else if(flipped[0]!=idx){
                    int fi=flipped[0]; flipped[0]=-1;
                    TextView prev=cards.get(fi); String pv=deck.get(fi);
                    if(val.equals(pv)){matched[0]++;prev.setBackground(getDrawable(R.drawable.circle_green));card.setBackground(getDrawable(R.drawable.circle_green));if(matched[0]>=pairs)showMission(curIdx+1);}
                    else{new Handler().postDelayed(()->{card.setText("?");prev.setText("?");},800);}
                }else flipped[0]=-1;
            });
            cards.add(card); grid.addView(card);
        }
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity=android.view.Gravity.CENTER_HORIZONTAL; lp.setMargins(0,16,0,0); grid.setLayoutParams(lp); l.addView(grid);
    }

    void buildTyping(LinearLayout l, Models.Mission m){
        String[] shortQ={"Rise and shine","Wake up now","Good morning"};
        String[] medQ={"The early bird catches the worm","Consistency is the key to success"};
        String[] longQ={"Success is not final, failure is not fatal. Courage to continue counts."};
        String[] pool=m.textLength.equals("short")?shortQ:m.textLength.equals("medium")?medQ:longQ;
        String target=pool[(int)(Math.random()*pool.length)];
        if(!m.requireCaps) target=target.toLowerCase();
        final String finalTarget=target;
        addLabel(l,"Type this exactly:");
        TextView tvT=new TextView(this); tvT.setText(target); tvT.setTextColor(0xFFF97316); tvT.setTextSize(16); tvT.setPadding(16,16,16,16); tvT.setBackground(getDrawable(R.drawable.card_bg2));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,8,0,8); tvT.setLayoutParams(lp); l.addView(tvT);
        EditText et=new EditText(this); et.setHint("Type here..."); et.setTextColor(0xFFFFFFFF); et.setHintTextColor(0xFF6B7280); et.setBackground(getDrawable(R.drawable.input_bg)); et.setPadding(20,14,20,14); et.setTextSize(15);
        lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0,8,0,8); et.setLayoutParams(lp); l.addView(et);
        TextView tvErr=new TextView(this); tvErr.setTextColor(0xFFEF4444); tvErr.setTextSize(12); l.addView(tvErr);
        Button btn=new Button(this); btn.setText("Confirm"); btn.setBackground(getDrawable(R.drawable.btn_primary_bg)); btn.setTextColor(0xFFFFFFFF); btn.setTextSize(15);
        lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,120); lp.setMargins(0,8,0,0); btn.setLayoutParams(lp);
        btn.setOnClickListener(v->{
            String typed=et.getText().toString();
            boolean ok=m.requireCaps?typed.equals(finalTarget):typed.equalsIgnoreCase(finalTarget);
            if(ok)showMission(curIdx+1);
            else{tvErr.setText("Not quite right. Try again.");et.setText("");}
        });
        l.addView(btn);
    }

    void buildShake(LinearLayout l, Models.Mission m){
        shakeCount=0; shakeInitialized=false;
        final int target=m.targetCount;
        TextView tvCount=new TextView(this);
        tvCount.setText("0 / "+target);
        tvCount.setTextSize(40); tvCount.setTypeface(null,android.graphics.Typeface.BOLD);
        tvCount.setTextColor(0xFFF97316); tvCount.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0,24,0,8); tvCount.setLayoutParams(lp); l.addView(tvCount);
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); pb.setMax(target); pb.setProgress(0);
        lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,24); lp.setMargins(0,8,0,24); pb.setLayoutParams(lp); l.addView(pb);
        addLabel(l,"Shake your phone vigorously!");
        if(accel!=null){
            // Real accelerometer shake detection
            if(shakeListener!=null) sensorMgr.unregisterListener(shakeListener);
            shakeListener=new SensorEventListener(){
                public void onSensorChanged(SensorEvent e){
                    if(!shakeInitialized){lastX=e.values[0];lastY=e.values[1];lastZ=e.values[2];shakeInitialized=true;return;}
                    float dx=Math.abs(e.values[0]-lastX);
                    float dy=Math.abs(e.values[1]-lastY);
                    float dz=Math.abs(e.values[2]-lastZ);
                    if(dx+dy+dz>SHAKE_THRESHOLD){
                        shakeCount=Math.min(shakeCount+1,target);
                        runOnUiThread(()->{
                            tvCount.setText(shakeCount+" / "+target);
                            pb.setProgress(shakeCount);
                            if(shakeCount>=target){sensorMgr.unregisterListener(shakeListener);showMission(curIdx+1);}
                        });
                    }
                    lastX=e.values[0]; lastY=e.values[1]; lastZ=e.values[2];
                }
                public void onAccuracyChanged(Sensor s,int a){}
            };
            sensorMgr.registerListener(shakeListener,accel,SensorManager.SENSOR_DELAY_GAME);
        } else {
            // Fallback simulation button if no accelerometer
            addLabel(l,"(No accelerometer detected — tap button)");
            Button sim=new Button(this); sim.setText("Shake! (tap to simulate)"); sim.setBackground(getDrawable(R.drawable.btn_primary_bg)); sim.setTextColor(0xFFFFFFFF);
            lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,120); lp.setMargins(0,8,0,0); sim.setLayoutParams(lp);
            sim.setOnClickListener(v->{
                shakeCount=Math.min(shakeCount+3,target);
                tvCount.setText(shakeCount+" / "+target);
                pb.setProgress(shakeCount);
                if(shakeCount>=target) showMission(curIdx+1);
            });
            l.addView(sim);
        }
    }

    void buildCount(LinearLayout l, Models.Mission m){
        final int target=m.targetCount;
        final int[] count={0};
        boolean isSquat=m.type.equals(Models.Mission.SQUATS);
        addLabel(l,isSquat?"Do "+target+" squats":"Walk "+target+" steps");
        TextView tvCount=new TextView(this); tvCount.setText("0 / "+target); tvCount.setTextSize(40); tvCount.setTypeface(null,android.graphics.Typeface.BOLD); tvCount.setTextColor(0xFFF97316); tvCount.setGravity(android.view.Gravity.CENTER); l.addView(tvCount);
        ProgressBar pb=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); pb.setMax(target); pb.setProgress(0); l.addView(pb);
        Button btn=new Button(this); btn.setText(isSquat?"Squat done!":"Step taken!"); btn.setBackground(getDrawable(R.drawable.btn_primary_bg)); btn.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,120); lp.setMargins(0,16,0,0); btn.setLayoutParams(lp);
        btn.setOnClickListener(v->{count[0]++;tvCount.setText(count[0]+" / "+target);pb.setProgress(count[0]);if(count[0]>=target)showMission(curIdx+1);});
        l.addView(btn);
    }

    void buildBarcode(LinearLayout l, Models.Mission m){
        String walkTo=m.barcodeLabel.isEmpty()?"the registered item":m.barcodeLabel;
        addLabel(l,"Walk to: "+walkTo);
        String regText=m.registeredBarcode.isEmpty()
            ?"No specific barcode registered — any scan will be accepted"
            :"Target barcode: "+m.barcodeLabel;
        addLabel(l,regText);
        // Status
        TextView tvStatus=new TextView(this);
        tvStatus.setText("Tap the button to open the scanner");
        tvStatus.setTextColor(0xFF9CA3AF);
        tvStatus.setTextSize(14);
        tvStatus.setGravity(android.view.Gravity.CENTER);
        tvStatus.setPadding(0,8,0,8);
        l.addView(tvStatus);
        Button btn=new Button(this);
        btn.setText("📷 Open Barcode Scanner");
        btn.setBackground(getDrawable(R.drawable.btn_primary_bg));
        btn.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,130);
        lp.setMargins(0,16,0,0); btn.setLayoutParams(lp);
        btn.setOnClickListener(v->{
            Intent scanIntent=new Intent(this,BarcodeScanActivity.class);
            scanIntent.putExtra(BarcodeScanActivity.EXTRA_PROMPT,
                m.barcodeLabel.isEmpty()?"Scan the registered item":"Scan: "+m.barcodeLabel);
            if(!m.registeredBarcode.isEmpty()){
                scanIntent.putExtra(BarcodeScanActivity.EXTRA_TARGET_VALUE,m.registeredBarcode);
            }
            startActivityForResult(scanIntent,REQ_BARCODE_DISMISS);
        });
        l.addView(btn);
        barcodeTarget=m.registeredBarcode;
    }

    void buildPhoto(LinearLayout l, Models.Mission m){
        String goTo=m.photoLabel.isEmpty()?"the registered location":m.photoLabel;
        addLabel(l,"Go to: "+goTo);
        addLabel(l,m.hasReferencePhoto?"Take a photo matching your reference":"Take a photo of the location");
        Button btn=new Button(this); btn.setText("Take photo"); btn.setBackground(getDrawable(R.drawable.btn_primary_bg)); btn.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,120); lp.setMargins(0,24,0,0); btn.setLayoutParams(lp);
        btn.setOnClickListener(v->{
            android.content.Intent i=new android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
            if(i.resolveActivity(getPackageManager())!=null) startActivityForResult(i,201);
            else showMission(curIdx+1);
        });
        l.addView(btn);
    }

    @Override protected void onActivityResult(int req,int res,android.content.Intent data){
        super.onActivityResult(req,res,data);
        if(req==201&&res==RESULT_OK){
            // Photo taken
            showMission(curIdx+1);
        } else if(req==REQ_BARCODE_DISMISS&&res==RESULT_OK&&data!=null){
            // CameraX + ML Kit barcode result
            String scanned=data.getStringExtra(BarcodeScanActivity.EXTRA_BARCODE_VALUE);
            if(scanned!=null){
                // If a target was set, BarcodeScanActivity already validated it before returning OK
                Toast.makeText(this,"Barcode matched! ✓",Toast.LENGTH_SHORT).show();
                showMission(curIdx+1);
            }
        }
    }

    void onAllComplete(){
        AppData db=AppData.get(this);
        Models.Alarm alarm=db.findAlarm(alarmId);
        if(alarm!=null&&alarm.linkedRoutineId!=0){
            Models.Routine r=db.findRoutine(alarm.linkedRoutineId);
            if(r!=null){
                android.content.Intent i=new android.content.Intent(this,RunRoutineActivity.class);
                i.putExtra("routine",r); startActivity(i);
            }
        }
        if(!preview&&alarm!=null){
            if(alarm.wakeCheckEnabled){
                new Handler().postDelayed(()->{
                    android.content.Intent i=new android.content.Intent(this,WakeCheckActivity.class);
                    i.putExtra("alarmId",alarmId); startActivity(i);
                },alarm.wakeCheckDelay*60*1000L);
            }
            alarm.snoozeCount=0; db.save();
        }
        Toast.makeText(this,"All missions complete!",Toast.LENGTH_LONG).show();
        finish();
    }
}
