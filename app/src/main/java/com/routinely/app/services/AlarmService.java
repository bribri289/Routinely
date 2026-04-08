package com.routinely.app.services;
import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.content.pm.ServiceInfo;
import androidx.core.app.NotificationCompat;
import com.routinely.app.RoutinelyApp;
import com.routinely.app.data.*;
import com.routinely.app.ui.AlarmRingActivity;

public class AlarmService extends Service {
    MediaPlayer player;
    Vibrator vibrator;

    // Built-in alarm sounds (raw resource names or system URIs)
    static final String[] SOUND_URIS = {
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) + "", // placeholder
        "gentle",   // maps to system notification
        "digital",
        "ocean",
        "birds",
        "fanfare",
        "drumroll",
        "pulse",
    };

    @Override public int onStartCommand(Intent intent, int f, int id){
        int alarmId = intent != null ? intent.getIntExtra("alarmId", -1) : -1;
        AppData db = AppData.get(this);
        Models.Alarm alarm = db.findAlarm(alarmId);
        String label = alarm != null ? alarm.label : "Alarm";

        // Launch ring screen immediately
        Intent ring = new Intent(this, AlarmRingActivity.class);
        ring.putExtra("alarmId", alarmId);
        ring.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(ring);

        // Foreground notification
        Intent open = new Intent(this, AlarmRingActivity.class);
        open.putExtra("alarmId", alarmId);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT |
            (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
        PendingIntent pi = PendingIntent.getActivity(this, alarmId, open, piFlags);

        Notification notif = new NotificationCompat.Builder(this, RoutinelyApp.CH_ALARM)
            .setContentTitle("⏰ " + label)
            .setContentText("Wake up! Tap to dismiss.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setFullScreenIntent(pi, true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            startForeground(1001, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        else
            startForeground(1001, notif);

        // --- SOUND ---
        playSound(alarm);

        // --- VIBRATION ---
        if (alarm == null || alarm.vibrate) {
            vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                long[] pattern = {0, 600, 400, 600, 400, 1200};
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
                else
                    vibrator.vibrate(pattern, 0);
            }
        }

        return START_STICKY;
    }

    void playSound(Models.Alarm alarm) {
        try {
            Uri soundUri = null;

            // 1. Custom uploaded sound
            if (alarm != null && alarm.customSoundUri != null && !alarm.customSoundUri.isEmpty()) {
                soundUri = Uri.parse(alarm.customSoundUri);
            }
            // 2. Selected sound index
            else {
                soundUri = getAlarmUri(alarm != null ? alarm.soundIndex : 0);
            }

            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            if (soundUri == null) {
                soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            // Set volume to max if ultra-loud
            AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (am != null) {
                if (alarm != null && alarm.ultraLoud) {
                    am.setStreamVolume(AudioManager.STREAM_ALARM,
                        am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
                } else if (alarm != null) {
                    int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    int targetVol = (int)(maxVol * alarm.volume / 100f);
                    am.setStreamVolume(AudioManager.STREAM_ALARM, targetVol, 0);
                }
            }

            player = new MediaPlayer();
            player.setAudioStreamType(AudioManager.STREAM_ALARM);
            player.setDataSource(this, soundUri);
            player.setLooping(true);

            // Gradual volume
            if (alarm != null && alarm.gradualVolume) {
                player.setVolume(0.1f, 0.1f);
                // Gradually increase over 30 seconds
                Handler h = new Handler(getMainLooper());
                final float[] vol = {0.1f};
                Runnable ramp = new Runnable() {
                    public void run() {
                        if (player != null && vol[0] < 1.0f) {
                            vol[0] = Math.min(1.0f, vol[0] + 0.05f);
                            player.setVolume(vol[0], vol[0]);
                            h.postDelayed(this, 1500);
                        }
                    }
                };
                h.postDelayed(ramp, 1500);
            } else {
                player.setVolume(1.0f, 1.0f);
            }

            player.prepare();
            player.start();

        } catch (Exception e) {
            // Fallback: use Ringtone API which is simpler
            try {
                Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                Ringtone r = RingtoneManager.getRingtone(this, fallback);
                if (r != null) r.play();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    Uri getAlarmUri(int index) {
        switch (index) {
            case 1: return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            case 2: return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            default: return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        }
    }

    @Override public void onDestroy() {
        if (player != null) {
            try { player.stop(); player.release(); } catch (Exception e) {}
            player = null;
        }
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent i) { return null; }
}
