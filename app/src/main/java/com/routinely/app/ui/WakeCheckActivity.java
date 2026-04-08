package com.routinely.app.ui;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.routinely.app.R;
import com.routinely.app.data.*;
import java.util.*;

public class WakeCheckActivity extends AppCompatActivity {
    int alarmId; int checksLeft; AppData db; Models.Alarm alarm;
    static String[][] QUESTIONS={
        {"What day of the week is it?","Monday","Tuesday","Wednesday","Sunday","0+dayOfWeek"},
        {"What is 8 × 7?","54","56","58","52","1"},
        {"Which month comes after March?","May","February","April","June","2"},
        {"What is 15 − 9?","7","5","6","8","2"},
        {"How many days in a week?","5","6","7","8","2"},
        {"What is 12 ÷ 4?","2","3","4","6","1"},
        {"What comes after Tuesday?","Monday","Thursday","Wednesday","Friday","2"},
        {"What is 3 × 8?","24","28","18","32","0"}
    };

    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_wake_check);
        db=AppData.get(this);
        alarmId=getIntent().getIntExtra("alarmId",-1);
        alarm=db.findAlarm(alarmId);
        checksLeft=alarm!=null?alarm.wakeCheckCount:1;
        nextQuestion();
    }

    void nextQuestion(){
        if(checksLeft<=0){
            Toast.makeText(this,"Wake-up confirmed! Stay awake! 🌟",Toast.LENGTH_LONG).show();
            finish(); return;
        }
        String[] q=QUESTIONS[(int)(Math.random()*QUESTIONS.length)];
        ((TextView)findViewById(R.id.tv_wc_question)).setText(q[0]);
        ((TextView)findViewById(R.id.tv_wc_count)).setText("Check "+(alarm!=null?(alarm.wakeCheckCount-checksLeft+1):1)+" of "+(alarm!=null?alarm.wakeCheckCount:1));
        List<String> opts=Arrays.asList(q[1],q[2],q[3],q[4]);
        final int ansIdx=Integer.parseInt(q[5]);
        final String answer=opts.get(ansIdx);
        int[] btnIds={R.id.btn_opt1,R.id.btn_opt2,R.id.btn_opt3,R.id.btn_opt4};
        for(int i=0;i<4;i++){
            Button btn=findViewById(btnIds[i]); btn.setText(opts.get(i));
            final String optVal=opts.get(i);
            btn.setOnClickListener(v->{
                if(optVal.equals(answer)){
                    checksLeft--;
                    if(checksLeft>0){Toast.makeText(this,"Correct! One more...",Toast.LENGTH_SHORT).show();nextQuestion();}
                    else nextQuestion();
                } else {
                    ((TextView)findViewById(R.id.tv_wc_feedback)).setText("Wrong! Try again.");
                    ((TextView)findViewById(R.id.tv_wc_feedback)).setTextColor(0xFFEF4444);
                }
            });
        }
        ((TextView)findViewById(R.id.tv_wc_feedback)).setText("");
    }
}
