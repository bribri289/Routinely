package com.routinely.app.data;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class AppDataTest {

    private Context ctx;

    @Before
    public void setUp() throws Exception {
        ctx = ApplicationProvider.getApplicationContext();
        resetSingleton();
    }

    @After
    public void tearDown() throws Exception {
        resetSingleton();
    }

    /** Reset AppData singleton between tests so each test gets a fresh instance. */
    private static void resetSingleton() throws Exception {
        Field f = AppData.class.getDeclaredField("inst");
        f.setAccessible(true);
        f.set(null, null);
    }

    private AppData get() {
        return AppData.get(ctx);
    }

    // -------------------------------------------------------------------------
    // newId()
    // -------------------------------------------------------------------------

    @Test
    public void newId_startsAtOne() {
        AppData db = get();
        assertEquals(1, db.newId());
    }

    @Test
    public void newId_incrementsOnEachCall() {
        AppData db = get();
        int first = db.newId();
        int second = db.newId();
        int third = db.newId();
        assertEquals(first + 1, second);
        assertEquals(second + 1, third);
    }

    // -------------------------------------------------------------------------
    // findRoutine()
    // -------------------------------------------------------------------------

    @Test
    public void findRoutine_returnsNullForEmptyList() {
        assertNull(get().findRoutine(1));
    }

    @Test
    public void findRoutine_returnsNullWhenNotFound() {
        AppData db = get();
        Models.Routine r = new Models.Routine();
        r.id = 5;
        db.routines.add(r);
        assertNull(db.findRoutine(99));
    }

    @Test
    public void findRoutine_findsRoutineById() {
        AppData db = get();
        Models.Routine r1 = new Models.Routine();
        r1.id = 1;
        r1.name = "Morning";
        Models.Routine r2 = new Models.Routine();
        r2.id = 2;
        r2.name = "Evening";
        db.routines.add(r1);
        db.routines.add(r2);

        Models.Routine found = db.findRoutine(2);
        assertNotNull(found);
        assertEquals("Evening", found.name);
    }

    @Test
    public void findRoutine_findsFirstMatch() {
        AppData db = get();
        Models.Routine r = new Models.Routine();
        r.id = 42;
        r.name = "Workout";
        db.routines.add(r);
        assertSame(r, db.findRoutine(42));
    }

    // -------------------------------------------------------------------------
    // findHabit()
    // -------------------------------------------------------------------------

    @Test
    public void findHabit_returnsNullForEmptyList() {
        assertNull(get().findHabit(1));
    }

    @Test
    public void findHabit_returnsNullWhenNotFound() {
        AppData db = get();
        Models.Habit h = new Models.Habit();
        h.id = 3;
        db.habits.add(h);
        assertNull(db.findHabit(100));
    }

    @Test
    public void findHabit_findsHabitById() {
        AppData db = get();
        Models.Habit h1 = new Models.Habit();
        h1.id = 10;
        h1.name = "Drink Water";
        Models.Habit h2 = new Models.Habit();
        h2.id = 11;
        h2.name = "Read";
        db.habits.add(h1);
        db.habits.add(h2);

        Models.Habit found = db.findHabit(10);
        assertNotNull(found);
        assertEquals("Drink Water", found.name);
    }

    // -------------------------------------------------------------------------
    // findAlarm()
    // -------------------------------------------------------------------------

    @Test
    public void findAlarm_returnsNullForEmptyList() {
        assertNull(get().findAlarm(1));
    }

    @Test
    public void findAlarm_returnsNullWhenNotFound() {
        AppData db = get();
        Models.Alarm a = new Models.Alarm();
        a.id = 7;
        db.alarms.add(a);
        assertNull(db.findAlarm(99));
    }

    @Test
    public void findAlarm_findsAlarmById() {
        AppData db = get();
        Models.Alarm a1 = new Models.Alarm();
        a1.id = 20;
        a1.label = "Wake Up";
        Models.Alarm a2 = new Models.Alarm();
        a2.id = 21;
        a2.label = "Nap";
        db.alarms.add(a1);
        db.alarms.add(a2);

        Models.Alarm found = db.findAlarm(21);
        assertNotNull(found);
        assertEquals("Nap", found.label);
    }

    // -------------------------------------------------------------------------
    // logActivity()
    // -------------------------------------------------------------------------

    @Test
    public void logActivity_addsEntryAtFront() {
        AppData db = get();
        db.logActivity("Morning Run", true);
        assertEquals(1, db.activities.size());
        assertEquals("Morning Run", db.activities.get(0).routineName);
        assertTrue(db.activities.get(0).completed);
    }

    @Test
    public void logActivity_prependsNewEntries() {
        AppData db = get();
        db.logActivity("First", true);
        db.logActivity("Second", false);

        assertEquals("Second", db.activities.get(0).routineName);
        assertEquals("First", db.activities.get(1).routineName);
        assertFalse(db.activities.get(0).completed);
    }

    @Test
    public void logActivity_capsListAtThirty() {
        AppData db = get();
        for (int i = 0; i < 35; i++) {
            db.logActivity("Activity " + i, true);
        }
        assertEquals(30, db.activities.size());
        // The most recent should be "Activity 34"
        assertEquals("Activity 34", db.activities.get(0).routineName);
    }

    @Test
    public void logActivity_setsTimestamp() {
        AppData db = get();
        db.logActivity("Test", true);
        assertNotNull(db.activities.get(0).timestamp);
        assertFalse(db.activities.get(0).timestamp.isEmpty());
    }

    // -------------------------------------------------------------------------
    // Singleton behavior
    // -------------------------------------------------------------------------

    @Test
    public void get_returnsSameInstance() {
        AppData first = AppData.get(ctx);
        AppData second = AppData.get(ctx);
        assertSame(first, second);
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    public void initialState_listsAreEmpty() {
        AppData db = get();
        assertTrue(db.routines.isEmpty());
        assertTrue(db.habits.isEmpty());
        assertTrue(db.alarms.isEmpty());
        assertTrue(db.activities.isEmpty());
    }

    @Test
    public void initialState_userFieldsAreEmpty() {
        AppData db = get();
        assertEquals("", db.userName);
        assertEquals("", db.userEmail);
    }

    @Test
    public void initialState_nextIdIsOne() {
        AppData db = get();
        assertEquals(1, db.nextId);
    }
}
