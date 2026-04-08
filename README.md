# Routinely — Native Android App

## Features
- **Today dashboard** — greeting, next routine, habits strip, progress, recent activity
- **Routines** — create/edit with steps, duration, linked habits, linked alarm
- **Habits** — daily tracking with streaks, reminders, routine linking
- **Alarms** — full alarm system with:
  - Exact alarm scheduling (Android AlarmManager)
  - 8 wake-up mission types (math, memory, typing, shake, squats, steps, barcode, photo)
  - Per-mission difficulty/count customisation
  - Snooze controls (prevent, limit, mission-to-snooze)
  - Wake-up check quiz after dismissal
  - Safety features (vibrate, ultra-loud, prevent power-off)
  - Linked routine auto-start after missions
- **Profile** — name, stats, streaks

## Build (via GitHub — FREE)
1. Create a GitHub account → New repository → "routinely"
2. Upload all these files (drag-and-drop the zip contents)
3. Go to Actions tab → watch the build run
4. Download the APK from Artifacts
5. Install on your phone (allow "Install unknown apps" in Settings)

## All Permissions Included
- POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM
- RECEIVE_BOOT_COMPLETED (reschedule after reboot)
- WAKE_LOCK, FOREGROUND_SERVICE (keeps alarm alive)
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
- SYSTEM_ALERT_WINDOW (appear on top)
- CAMERA, ACTIVITY_RECOGNITION, VIBRATE
