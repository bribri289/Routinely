package com.routinely.app.data;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AppData {
    private static AppData inst;
    private static final String PREFS = "routinely_prefs";
    private final SharedPreferences prefs;
    private final Gson gson = new Gson();

    public List<Models.Routine> routines = new ArrayList<>();
    public List<Models.Habit> habits = new ArrayList<>();
    public List<Models.Alarm> alarms = new ArrayList<>();
    public List<Models.RecentActivity> activities = new ArrayList<>();
    public String userName = "";
    public String userEmail = "";
    public int nextId = 1;
    public int dailyLessonIndex = 0;
    public String dailyLessonDate = "";
    public List<String> favoriteLessons = new ArrayList<>();

    private AppData(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public static synchronized AppData get(Context ctx) {
        if (inst == null) inst = new AppData(ctx);
        return inst;
    }

    public int newId() { return nextId++; }

    public void save() {
        prefs.edit()
            .putString("routines", gson.toJson(routines))
            .putString("habits", gson.toJson(habits))
            .putString("alarms", gson.toJson(alarms))
            .putString("activities", gson.toJson(activities))
            .putString("userName", userName)
            .putString("userEmail", userEmail)
            .putInt("nextId", nextId)
            .putInt("dailyLessonIndex", dailyLessonIndex)
            .putString("dailyLessonDate", dailyLessonDate)
            .putString("favoriteLessons", gson.toJson(favoriteLessons))
            .apply();
    }

    private void load() {
        String rj=prefs.getString("routines",null);
        String hj=prefs.getString("habits",null);
        String aj=prefs.getString("alarms",null);
        String acj=prefs.getString("activities",null);
        String flj=prefs.getString("favoriteLessons",null);
        Type rl=new TypeToken<List<Models.Routine>>(){}.getType();
        Type hl=new TypeToken<List<Models.Habit>>(){}.getType();
        Type al=new TypeToken<List<Models.Alarm>>(){}.getType();
        Type acl=new TypeToken<List<Models.RecentActivity>>(){}.getType();
        Type fll=new TypeToken<List<String>>(){}.getType();
        if(rj!=null) routines=gson.fromJson(rj,rl);
        if(hj!=null) habits=gson.fromJson(hj,hl);
        if(aj!=null) alarms=gson.fromJson(aj,al);
        if(acj!=null) activities=gson.fromJson(acj,acl);
        if(flj!=null) favoriteLessons=gson.fromJson(flj,fll);
        if(favoriteLessons==null) favoriteLessons=new ArrayList<>();
        userName=prefs.getString("userName","");
        userEmail=prefs.getString("userEmail","");
        nextId=prefs.getInt("nextId",1);
        dailyLessonIndex=prefs.getInt("dailyLessonIndex",0);
        dailyLessonDate=prefs.getString("dailyLessonDate","");
    }

    public Models.Routine findRoutine(int id){for(Models.Routine r:routines)if(r.id==id)return r;return null;}
    public Models.Habit findHabit(int id){for(Models.Habit h:habits)if(h.id==id)return h;return null;}
    public Models.Alarm findAlarm(int id){for(Models.Alarm a:alarms)if(a.id==id)return a;return null;}
    public void logActivity(String name, boolean done){
        String ts=android.text.format.DateFormat.format("MMM d, h:mm a",new java.util.Date()).toString();
        activities.add(0,new Models.RecentActivity(name,ts,done));
        if(activities.size()>30)activities.remove(activities.size()-1);
        save();
    }
}
