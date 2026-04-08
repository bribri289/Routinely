package com.routinely.app.data;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class ModelsTest {

    // -------------------------------------------------------------------------
    // Routine.getTimeString()
    // -------------------------------------------------------------------------

    @Test
    public void routine_getTimeString_morningHour() {
        Models.Routine r = new Models.Routine();
        r.startHour = 7;
        r.startMinute = 0;
        assertEquals("7:00 AM", r.getTimeString());
    }

    @Test
    public void routine_getTimeString_afternoonHour() {
        Models.Routine r = new Models.Routine();
        r.startHour = 13;
        r.startMinute = 30;
        assertEquals("1:30 PM", r.getTimeString());
    }

    @Test
    public void routine_getTimeString_noon() {
        Models.Routine r = new Models.Routine();
        r.startHour = 12;
        r.startMinute = 0;
        assertEquals("12:00 PM", r.getTimeString());
    }

    @Test
    public void routine_getTimeString_midnight() {
        Models.Routine r = new Models.Routine();
        r.startHour = 0;
        r.startMinute = 0;
        assertEquals("12:00 AM", r.getTimeString());
    }

    @Test
    public void routine_getTimeString_leadingZeroMinute() {
        Models.Routine r = new Models.Routine();
        r.startHour = 8;
        r.startMinute = 5;
        assertEquals("8:05 AM", r.getTimeString());
    }

    @Test
    public void routine_getTimeString_lastMinuteOfDay() {
        Models.Routine r = new Models.Routine();
        r.startHour = 23;
        r.startMinute = 59;
        assertEquals("11:59 PM", r.getTimeString());
    }

    // -------------------------------------------------------------------------
    // Routine.getDaysString()
    // -------------------------------------------------------------------------

    @Test
    public void routine_getDaysString_everyDay() {
        Models.Routine r = new Models.Routine();
        r.repeatDays = new boolean[]{true, true, true, true, true, true, true};
        assertEquals("Every day", r.getDaysString());
    }

    @Test
    public void routine_getDaysString_weekdays() {
        Models.Routine r = new Models.Routine();
        r.repeatDays = new boolean[]{true, true, true, true, true, false, false};
        assertEquals("Weekdays", r.getDaysString());
    }

    @Test
    public void routine_getDaysString_weekends() {
        Models.Routine r = new Models.Routine();
        r.repeatDays = new boolean[]{false, false, false, false, false, true, true};
        assertEquals("Weekends", r.getDaysString());
    }

    @Test
    public void routine_getDaysString_singleDay() {
        Models.Routine r = new Models.Routine();
        r.repeatDays = new boolean[]{true, false, false, false, false, false, false};
        assertEquals("Mon", r.getDaysString());
    }

    @Test
    public void routine_getDaysString_multipleDays() {
        Models.Routine r = new Models.Routine();
        r.repeatDays = new boolean[]{true, false, true, false, true, false, false};
        assertEquals("Mon • Wed • Fri", r.getDaysString());
    }

    @Test
    public void routine_getDaysString_noDays() {
        Models.Routine r = new Models.Routine();
        r.repeatDays = new boolean[]{false, false, false, false, false, false, false};
        assertEquals("", r.getDaysString());
    }

    // -------------------------------------------------------------------------
    // Routine.getTotalMinutes()
    // -------------------------------------------------------------------------

    @Test
    public void routine_getTotalMinutes_emptySteps_returnsMinimumOne() {
        Models.Routine r = new Models.Routine();
        r.steps = new ArrayList<>();
        assertEquals(1, r.getTotalMinutes());
    }

    @Test
    public void routine_getTotalMinutes_singleStep() {
        Models.Routine r = new Models.Routine();
        Models.RoutineStep step = new Models.RoutineStep();
        step.durationSeconds = 300; // 5 minutes
        r.steps = new ArrayList<>(Arrays.asList(step));
        assertEquals(5, r.getTotalMinutes());
    }

    @Test
    public void routine_getTotalMinutes_multipleSteps() {
        Models.Routine r = new Models.Routine();
        Models.RoutineStep s1 = new Models.RoutineStep();
        s1.durationSeconds = 600; // 10 min
        Models.RoutineStep s2 = new Models.RoutineStep();
        s2.durationSeconds = 300; // 5 min
        r.steps = new ArrayList<>(Arrays.asList(s1, s2));
        assertEquals(15, r.getTotalMinutes());
    }

    @Test
    public void routine_getTotalMinutes_truncatesSecondsToMinutes() {
        Models.Routine r = new Models.Routine();
        Models.RoutineStep step = new Models.RoutineStep();
        step.durationSeconds = 90; // 1 minute 30 seconds → 1 minute after integer division
        r.steps = new ArrayList<>(Arrays.asList(step));
        assertEquals(1, r.getTotalMinutes());
    }

    @Test
    public void routine_getTotalMinutes_subMinuteStep_returnsOne() {
        Models.Routine r = new Models.Routine();
        Models.RoutineStep step = new Models.RoutineStep();
        step.durationSeconds = 30; // less than 1 minute → 0 → max(1, 0) = 1
        r.steps = new ArrayList<>(Arrays.asList(step));
        assertEquals(1, r.getTotalMinutes());
    }

    // -------------------------------------------------------------------------
    // RoutineStep.getDurationString()
    // -------------------------------------------------------------------------

    @Test
    public void routineStep_getDurationString_secondsOnly() {
        Models.RoutineStep s = new Models.RoutineStep();
        s.durationSeconds = 45;
        assertEquals("45s", s.getDurationString());
    }

    @Test
    public void routineStep_getDurationString_minutesAndSeconds() {
        Models.RoutineStep s = new Models.RoutineStep();
        s.durationSeconds = 90;
        assertEquals("1m 30s", s.getDurationString());
    }

    @Test
    public void routineStep_getDurationString_exactMinutes() {
        Models.RoutineStep s = new Models.RoutineStep();
        s.durationSeconds = 300;
        assertEquals("5m 00s", s.getDurationString());
    }

    @Test
    public void routineStep_getDurationString_hoursMinutesSeconds() {
        Models.RoutineStep s = new Models.RoutineStep();
        s.durationSeconds = 3661; // 1h 1m 1s
        assertEquals("1h 01m 01s", s.getDurationString());
    }

    @Test
    public void routineStep_getDurationString_exactHour() {
        Models.RoutineStep s = new Models.RoutineStep();
        s.durationSeconds = 3600;
        assertEquals("1h 00m 00s", s.getDurationString());
    }

    @Test
    public void routineStep_getDurationString_multipleHours() {
        Models.RoutineStep s = new Models.RoutineStep();
        s.durationSeconds = 7322; // 2h 2m 2s
        assertEquals("2h 02m 02s", s.getDurationString());
    }

    // -------------------------------------------------------------------------
    // Alarm.getTimeString()
    // -------------------------------------------------------------------------

    @Test
    public void alarm_getTimeString_morningHour() {
        Models.Alarm a = new Models.Alarm();
        a.hour = 7;
        a.minute = 0;
        assertEquals("7:00 AM", a.getTimeString());
    }

    @Test
    public void alarm_getTimeString_noon() {
        Models.Alarm a = new Models.Alarm();
        a.hour = 12;
        a.minute = 0;
        assertEquals("12:00 PM", a.getTimeString());
    }

    @Test
    public void alarm_getTimeString_midnight() {
        Models.Alarm a = new Models.Alarm();
        a.hour = 0;
        a.minute = 0;
        assertEquals("12:00 AM", a.getTimeString());
    }

    @Test
    public void alarm_getTimeString_afternoonHour() {
        Models.Alarm a = new Models.Alarm();
        a.hour = 15;
        a.minute = 45;
        assertEquals("3:45 PM", a.getTimeString());
    }

    @Test
    public void alarm_getTimeString_leadingZeroMinute() {
        Models.Alarm a = new Models.Alarm();
        a.hour = 9;
        a.minute = 5;
        assertEquals("9:05 AM", a.getTimeString());
    }

    // -------------------------------------------------------------------------
    // Alarm.getDaysString()
    // -------------------------------------------------------------------------

    @Test
    public void alarm_getDaysString_everyDay() {
        Models.Alarm a = new Models.Alarm();
        a.repeatDays = new boolean[]{true, true, true, true, true, true, true};
        assertEquals("Every day", a.getDaysString());
    }

    @Test
    public void alarm_getDaysString_weekdays() {
        Models.Alarm a = new Models.Alarm();
        a.repeatDays = new boolean[]{true, true, true, true, true, false, false};
        assertEquals("Weekdays", a.getDaysString());
    }

    @Test
    public void alarm_getDaysString_weekends() {
        Models.Alarm a = new Models.Alarm();
        a.repeatDays = new boolean[]{false, false, false, false, false, true, true};
        assertEquals("Weekends", a.getDaysString());
    }

    @Test
    public void alarm_getDaysString_singleDay() {
        Models.Alarm a = new Models.Alarm();
        a.repeatDays = new boolean[]{false, false, false, false, false, false, true};
        assertEquals("Sun", a.getDaysString());
    }

    @Test
    public void alarm_getDaysString_multipleDays() {
        Models.Alarm a = new Models.Alarm();
        a.repeatDays = new boolean[]{false, true, false, true, false, false, true};
        assertEquals("Tue • Thu • Sun", a.getDaysString());
    }

    @Test
    public void alarm_getDaysString_noDays() {
        Models.Alarm a = new Models.Alarm();
        a.repeatDays = new boolean[]{false, false, false, false, false, false, false};
        assertEquals("", a.getDaysString());
    }

    // -------------------------------------------------------------------------
    // Mission.getDisplayName()
    // -------------------------------------------------------------------------

    @Test
    public void mission_getDisplayName_math() {
        assertEquals("Math Problem", new Models.Mission(Models.Mission.MATH).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_memory() {
        assertEquals("Memory Game", new Models.Mission(Models.Mission.MEMORY).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_typing() {
        assertEquals("Typing Challenge", new Models.Mission(Models.Mission.TYPING).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_shake() {
        assertEquals("Shake Phone", new Models.Mission(Models.Mission.SHAKE).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_squats() {
        assertEquals("Squats", new Models.Mission(Models.Mission.SQUATS).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_steps() {
        assertEquals("Walk Steps", new Models.Mission(Models.Mission.STEPS).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_barcode() {
        assertEquals("Barcode Scan", new Models.Mission(Models.Mission.BARCODE).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_photo() {
        assertEquals("Photo Mission", new Models.Mission(Models.Mission.PHOTO).getDisplayName());
    }

    @Test
    public void mission_getDisplayName_unknownType() {
        assertEquals("custom", new Models.Mission("custom").getDisplayName());
    }

    // -------------------------------------------------------------------------
    // Mission.getEmoji()
    // -------------------------------------------------------------------------

    @Test
    public void mission_getEmoji_math() {
        assertEquals("🔢", new Models.Mission(Models.Mission.MATH).getEmoji());
    }

    @Test
    public void mission_getEmoji_memory() {
        assertEquals("🧠", new Models.Mission(Models.Mission.MEMORY).getEmoji());
    }

    @Test
    public void mission_getEmoji_typing() {
        assertEquals("⌨️", new Models.Mission(Models.Mission.TYPING).getEmoji());
    }

    @Test
    public void mission_getEmoji_shake() {
        assertEquals("📳", new Models.Mission(Models.Mission.SHAKE).getEmoji());
    }

    @Test
    public void mission_getEmoji_squats() {
        assertEquals("🦵", new Models.Mission(Models.Mission.SQUATS).getEmoji());
    }

    @Test
    public void mission_getEmoji_steps() {
        assertEquals("👣", new Models.Mission(Models.Mission.STEPS).getEmoji());
    }

    @Test
    public void mission_getEmoji_barcode() {
        assertEquals("🔲", new Models.Mission(Models.Mission.BARCODE).getEmoji());
    }

    @Test
    public void mission_getEmoji_photo() {
        assertEquals("📷", new Models.Mission(Models.Mission.PHOTO).getEmoji());
    }

    @Test
    public void mission_getEmoji_unknownType() {
        assertEquals("❓", new Models.Mission("unknown").getEmoji());
    }

    // -------------------------------------------------------------------------
    // Habit.getEffectiveReminderTimes()
    // -------------------------------------------------------------------------

    @Test
    public void habit_getEffectiveReminderTimes_emptyList_fallsBackToLegacyFields() {
        Models.Habit h = new Models.Habit();
        h.reminderTimes = new ArrayList<>();
        h.reminderHour = 8;
        h.reminderMinute = 30;

        List<int[]> times = h.getEffectiveReminderTimes();
        assertEquals(1, times.size());
        assertArrayEquals(new int[]{8, 30}, times.get(0));
    }

    @Test
    public void habit_getEffectiveReminderTimes_nullList_fallsBackToLegacyFields() {
        Models.Habit h = new Models.Habit();
        h.reminderTimes = null;
        h.reminderHour = 7;
        h.reminderMinute = 15;

        List<int[]> times = h.getEffectiveReminderTimes();
        assertEquals(1, times.size());
        assertArrayEquals(new int[]{7, 15}, times.get(0));
    }

    @Test
    public void habit_getEffectiveReminderTimes_populatedList_returnsIt() {
        Models.Habit h = new Models.Habit();
        h.reminderTimes = new ArrayList<>();
        h.reminderTimes.add(new int[]{6, 0});
        h.reminderTimes.add(new int[]{18, 30});
        h.reminderHour = 8;
        h.reminderMinute = 0;

        List<int[]> times = h.getEffectiveReminderTimes();
        assertSame(h.reminderTimes, times);
        assertEquals(2, times.size());
        assertArrayEquals(new int[]{6, 0}, times.get(0));
        assertArrayEquals(new int[]{18, 30}, times.get(1));
    }

    // -------------------------------------------------------------------------
    // HabitLog constructor
    // -------------------------------------------------------------------------

    @Test
    public void habitLog_constructor_storesFields() {
        Models.HabitLog log = new Models.HabitLog("2024-01-15", 3);
        assertEquals("2024-01-15", log.date);
        assertEquals(3, log.count);
    }

    // -------------------------------------------------------------------------
    // RecentActivity constructor
    // -------------------------------------------------------------------------

    @Test
    public void recentActivity_constructor_storesFields() {
        Models.RecentActivity a = new Models.RecentActivity("Morning Routine", "Jan 1, 7:00 AM", true);
        assertEquals("Morning Routine", a.routineName);
        assertEquals("Jan 1, 7:00 AM", a.timestamp);
        assertTrue(a.completed);
    }

    @Test
    public void recentActivity_constructor_notCompleted() {
        Models.RecentActivity a = new Models.RecentActivity("Evening Routine", "Dec 31, 9:00 PM", false);
        assertFalse(a.completed);
    }

    // -------------------------------------------------------------------------
    // Mission default field values
    // -------------------------------------------------------------------------

    @Test
    public void mission_defaults() {
        Models.Mission m = new Models.Mission(Models.Mission.MATH);
        assertTrue(m.required);
        assertEquals("medium", m.difficulty);
        assertEquals(3, m.questionCount);
        assertTrue(m.opAdd);
        assertTrue(m.opSub);
        assertTrue(m.opMul);
        assertFalse(m.opDiv);
    }

    // -------------------------------------------------------------------------
    // Routine default field values
    // -------------------------------------------------------------------------

    @Test
    public void routine_defaults() {
        Models.Routine r = new Models.Routine();
        assertEquals("🌅", r.emoji);
        assertEquals("Morning", r.category);
        assertEquals(7, r.startHour);
        assertEquals(0, r.startMinute);
        assertTrue(r.active);
        assertFalse(r.archived);
        assertNotNull(r.steps);
    }

    // -------------------------------------------------------------------------
    // Alarm default field values
    // -------------------------------------------------------------------------

    @Test
    public void alarm_defaults() {
        Models.Alarm a = new Models.Alarm();
        assertEquals(7, a.hour);
        assertEquals(0, a.minute);
        assertEquals("Alarm", a.label);
        assertTrue(a.enabled);
        assertTrue(a.gradualVolume);
        assertTrue(a.vibrate);
        assertFalse(a.ultraLoud);
        assertEquals(80, a.volume);
        assertEquals(3, a.maxSnoozes);
        assertEquals(5, a.snoozeMinutes);
    }
}
