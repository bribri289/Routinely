package com.routinely.app.data;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Models {

    public static class Routine implements Serializable {
        public int id;
        public String name="", emoji="🌅", category="Morning";
        public int startHour=7, startMinute=0;
        public boolean[] repeatDays={true,true,true,true,true,false,false};
        public List<RoutineStep> steps=new ArrayList<>();
        public int linkedAlarmId=0;
        public boolean active=true;
        public boolean archived=false;
        public boolean showSuggestions=false;

        public String getTimeString(){
            String ap=startHour<12?"AM":"PM";
            int h=startHour%12; if(h==0)h=12;
            return String.format("%d:%02d %s",h,startMinute,ap);
        }
        public String getDaysString(){
            if(allDays())return "Every day";
            if(weekdays())return "Weekdays";
            if(weekends())return "Weekends";
            String[] n={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<7;i++) if(repeatDays[i]){if(sb.length()>0)sb.append(" • ");sb.append(n[i]);}
            return sb.toString();
        }
        public int getTotalMinutes(){
            int t=0;
            for(RoutineStep s:steps) t+=s.durationSeconds/60;
            return Math.max(1,t);
        }
        private boolean allDays(){for(boolean d:repeatDays)if(!d)return false;return true;}
        private boolean weekdays(){return repeatDays[0]&&repeatDays[1]&&repeatDays[2]&&repeatDays[3]&&repeatDays[4]&&!repeatDays[5]&&!repeatDays[6];}
        private boolean weekends(){return !repeatDays[0]&&!repeatDays[1]&&!repeatDays[2]&&!repeatDays[3]&&!repeatDays[4]&&repeatDays[5]&&repeatDays[6];}
    }

    public static class RoutineStep implements Serializable {
        public int id;
        public String name="Step", description="", emoji="✅";
        public int durationSeconds=300;
        public String recurrenceType="daily";
        public boolean[] repeatDays={true,true,true,true,true,true,true};
        public int customInterval=1;
        public int linkedHabitId=0;
        public int durationMinutes=0; // legacy compat

        public String getDurationString(){
            int h=durationSeconds/3600;
            int m=(durationSeconds%3600)/60;
            int s=durationSeconds%60;
            if(h>0)return String.format("%dh %02dm %02ds",h,m,s);
            if(m>0)return String.format("%dm %02ds",m,s);
            return String.format("%ds",s);
        }
    }

    public static class Habit implements Serializable {
        public int id;
        public String name="", emoji="💧", category="";
        public int streak=0;
        public boolean completedToday=false;
        public boolean[] repeatDays={true,true,true,true,true,true,true};
        public int reminderHour=8, reminderMinute=0;
        public boolean reminderEnabled=true;
        public int linkedRoutineId=0;
        // Extended fields
        public int dailyTarget=1;
        public int todayCount=0;
        public List<int[]> reminderTimes=new ArrayList<>(); // each int[2] = {hour, minute}
        public List<HabitLog> logs=new ArrayList<>();
        public String createdDate="";

        public List<int[]> getEffectiveReminderTimes(){
            if(reminderTimes!=null&&!reminderTimes.isEmpty()) return reminderTimes;
            List<int[]> t=new ArrayList<>(); t.add(new int[]{reminderHour,reminderMinute}); return t;
        }
    }

    public static class HabitLog implements Serializable {
        public String date; // "yyyy-MM-dd"
        public int count;
        public HabitLog(String d,int c){date=d;count=c;}
    }

    public static class Alarm implements Serializable {
        public int id;
        public int hour=7, minute=0;
        public String label="Alarm";
        public boolean[] repeatDays={true,true,true,true,true,false,false};
        public boolean enabled=true;
        public int linkedRoutineId=0;
        // Sound
        public int soundIndex=0;
        public int volume=80;
        public boolean gradualVolume=true;
        public boolean vibrate=true;
        public boolean ultraLoud=false;
        public String customSoundUri="";
        // Wallpaper
        public String wallpaperUri="";
        public boolean wallpaperIsVideo=false;
        // Safety
        public boolean preventPowerOff=false;
        public boolean playWhenScreenOff=true;
        // Missions
        public List<Mission> missions=new ArrayList<>();
        // Snooze
        public boolean preventSnooze=false;
        public int maxSnoozes=3;
        public boolean missionToSnooze=false;
        public int snoozeCount=0;
        public int snoozeMinutes=5;
        // Wake check
        public boolean wakeCheckEnabled=false;
        public int wakeCheckDelay=2;
        public int wakeCheckCount=1;

        public String getTimeString(){
            String ap=hour<12?"AM":"PM";
            int h=hour%12; if(h==0)h=12;
            return String.format("%d:%02d %s",h,minute,ap);
        }
        public String getDaysString(){
            if(allDays())return "Every day";
            if(weekdays())return "Weekdays";
            if(weekends())return "Weekends";
            String[] n={"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
            StringBuilder sb=new StringBuilder();
            for(int i=0;i<7;i++) if(repeatDays[i]){if(sb.length()>0)sb.append(" • ");sb.append(n[i]);}
            return sb.toString();
        }
        private boolean allDays(){for(boolean d:repeatDays)if(!d)return false;return true;}
        private boolean weekdays(){return repeatDays[0]&&repeatDays[1]&&repeatDays[2]&&repeatDays[3]&&repeatDays[4]&&!repeatDays[5]&&!repeatDays[6];}
        private boolean weekends(){return !repeatDays[0]&&!repeatDays[1]&&!repeatDays[2]&&!repeatDays[3]&&!repeatDays[4]&&repeatDays[5]&&repeatDays[6];}
    }

    public static class Mission implements Serializable {
        public static final String MATH="math",MEMORY="memory",TYPING="typing",
            SHAKE="shake",SQUATS="squats",STEPS="steps",BARCODE="barcode",PHOTO="photo";
        public String type;
        public boolean required=true;
        public String difficulty="medium";
        public int questionCount=3;
        public boolean opAdd=true,opSub=true,opMul=true,opDiv=false;
        public String gridSize="2x3";
        public String textLength="short";
        public boolean requireCaps=true,requirePunct=false;
        public int targetCount=20;
        public String sensitivity="medium";
        public String registeredBarcode="";
        public String barcodeLabel="";
        public String photoLabel="";
        public boolean hasReferencePhoto=false;
        public String referencePhotoPath="";

        public Mission(String t){this.type=t;}

        public String getDisplayName(){
            switch(type){
                case MATH:return "Math Problem";
                case MEMORY:return "Memory Game";
                case TYPING:return "Typing Challenge";
                case SHAKE:return "Shake Phone";
                case SQUATS:return "Squats";
                case STEPS:return "Walk Steps";
                case BARCODE:return "Barcode Scan";
                case PHOTO:return "Photo Mission";
                default:return type;
            }
        }

        public String getEmoji(){
            switch(type){
                case MATH:return "🔢";
                case MEMORY:return "🧠";
                case TYPING:return "⌨️";
                case SHAKE:return "📳";
                case SQUATS:return "🦵";
                case STEPS:return "👣";
                case BARCODE:return "🔲";
                case PHOTO:return "📷";
                default:return "❓";
            }
        }
    }

    public static class RecentActivity implements Serializable {
        public String routineName, timestamp;
        public boolean completed;
        public RecentActivity(String n,String t,boolean c){
            routineName=n; timestamp=t; completed=c;
        }
    }
}
