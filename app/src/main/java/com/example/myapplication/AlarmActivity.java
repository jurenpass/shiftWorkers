package com.example.myapplication;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.data.AlarmManager;
import com.example.myapplication.data.AlarmNotificationHelper;
import com.example.myapplication.data.AlarmSetting;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {
    private static final String TAG = "AlarmActivity";
    private static final int NOTIFICATION_ID = 1001;
    private static final float MIN_VOLUME = 0.2f;
    private static final float MAX_VOLUME = 1.0f;
    private static final int FADE_DURATION = 2000;
    private static final int FADE_INTERVAL = 1000;

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Handler handler;
    private Runnable stopRunnable;
    private Runnable fadeRunnable;
    private boolean isUserClosed = false;
    private String alarmId;
    private float currentVolume = MIN_VOLUME;
    private AudioManager audioManager;
    private int previousRingerMode;
    private int maxAlarmVolume;
    private int previousAlarmVolume;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "========== AlarmActivity onCreate ==========");
        setContentView(R.layout.activity_alarm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            Log.d(TAG, "使用 setShowWhenLocked 和 setTurnScreenOn");
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            );
            Log.d(TAG, "使用 WindowManager flags");
        }

        cancelNotification();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousRingerMode = audioManager.getRingerMode();

        maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        Log.d(TAG, "闹钟音量已设置为最大: " + maxAlarmVolume + ", 之前音量: " + previousAlarmVolume);

        alarmId = getIntent().getStringExtra("alarm_id");
        String shiftType = getIntent().getStringExtra("shift_type");
        String reminderType = getIntent().getStringExtra("reminder_type");
        String hour = getIntent().getStringExtra("hour");
        String minute = getIntent().getStringExtra("minute");

        Log.d(TAG, "收到参数: alarmId=" + alarmId + ", shiftType=" + shiftType + ", reminderType=" + reminderType + ", time=" + hour + ":" + minute);

        setDate();

        if (hour != null && minute != null) {
            TextView tvTime = findViewById(R.id.tv_alarm_time);
            tvTime.setText(String.format("%02d:%02d", Integer.parseInt(hour), Integer.parseInt(minute)));
            Log.d(TAG, "设置闹钟时间显示: " + hour + ":" + minute);
        }

        TextView tvTitle = findViewById(R.id.tv_alarm_title);
        if (shiftType != null && reminderType != null) {
            tvTitle.setText(shiftType + " - " + reminderType);
            Log.d(TAG, "设置闹钟标题: " + shiftType + " - " + reminderType);
        }

        SeekBar seekBar = findViewById(R.id.seekbar_close);
        seekBar.setProgress(0);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 95) {
                    Log.d(TAG, "滑动关闭闹钟，进度: " + progress);
                    isUserClosed = true;
                    stopAlarm();
                    finish();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (seekBar.getProgress() < 95) {
                    seekBar.setProgress(0);
                }
            }
        });

        playAlarm();

        handler = new Handler();
        stopRunnable = () -> {
            Log.d(TAG, "5分钟自动关闭闹钟");
            isUserClosed = true;
            stopAlarm();
            finish();
        };
        handler.postDelayed(stopRunnable, 300000);

        bringToFront();

        Log.d(TAG, "AlarmActivity 初始化完成");
    }

    private void setDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("M月d日 EEEE", Locale.CHINA);
        String dateStr = dateFormat.format(calendar.getTime());
        TextView tvDate = findViewById(R.id.tv_alarm_date);
        tvDate.setText(dateStr);
    }

    private void bringToFront() {
        Log.d(TAG, "尝试将 Activity 带到前台");
        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                activityManager.moveTaskToFront(getTaskId(), 0);
                Log.d(TAG, "Activity 已带到前台");
            }
        } catch (Exception e) {
            Log.e(TAG, "将 Activity 带到前台失败", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "AlarmActivity onResume");

        if (!isUserClosed) {
            bringToFront();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "AlarmActivity onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "AlarmActivity onStop");
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "返回键关闭闹钟");
        isUserClosed = true;
        stopAlarm();
        super.onBackPressed();
    }

    private void cancelNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(NOTIFICATION_ID);
            notificationManager.cancel(NOTIFICATION_ID + 1);
            Log.d(TAG, "通知已取消");
        }
    }

    private boolean isVibrateEnabled() {
        if (alarmId != null) {
            AlarmManager alarmManager = new AlarmManager(this);
            AlarmSetting alarm = alarmManager.getAlarmById(alarmId);
            if (alarm != null) {
                return alarm.isVibrate();
            }
        }
        return true;
    }

    private boolean isFadeEnabled() {
        if (alarmId != null) {
            AlarmManager alarmManager = new AlarmManager(this);
            AlarmSetting alarm = alarmManager.getAlarmById(alarmId);
            if (alarm != null) {
                return alarm.isFade();
            }
        }
        return true;
    }

    private void playAlarm() {
        Log.d(TAG, "========== 开始播放闹钟 ==========");

        boolean vibrate = isVibrateEnabled();
        boolean fade = isFadeEnabled();

        Log.d(TAG, "振动: " + vibrate + ", 渐近: " + fade);

        try {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            Log.d(TAG, "铃声模式已设置为正常");

            String ringtoneUri = getRingtoneUri();
            Log.d(TAG, "铃声URI: " + ringtoneUri);

            Uri alarmUri = Uri.parse(ringtoneUri);
            Log.d(TAG, "解析后的铃声URI: " + alarmUri);

            mediaPlayer = new MediaPlayer();
            Log.d(TAG, "MediaPlayer 创建成功");

            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            Log.d(TAG, "AudioAttributes 设置成功");

            mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            Log.d(TAG, "音频流类型设置为 STREAM_ALARM");

            mediaPlayer.setDataSource(this, alarmUri);
            Log.d(TAG, "数据源设置成功");

            mediaPlayer.setLooping(true);
            Log.d(TAG, "设置为循环播放");

            mediaPlayer.prepare();
            Log.d(TAG, "MediaPlayer 准备完成");

            if (fade) {
                currentVolume = MIN_VOLUME;
                mediaPlayer.setVolume(currentVolume, currentVolume);
                mediaPlayer.start();
                startVolumeFade();
                Log.d(TAG, "铃声播放开始，渐近功能开启，初始音量: " + String.format("%.0f", currentVolume * 100) + "%");
            } else {
                currentVolume = MAX_VOLUME;
                mediaPlayer.setVolume(currentVolume, currentVolume);
                mediaPlayer.start();
                Log.d(TAG, "铃声播放开始，渐近功能关闭，音量: " + String.format("%.0f", currentVolume * 100) + "%");
            }
        } catch (IOException e) {
            Log.e(TAG, "MediaPlayer 初始化失败，IOException: " + e.getMessage(), e);
            playAlarmWithRingtone();
        } catch (Exception e) {
            Log.e(TAG, "播放铃声失败，Exception: " + e.getMessage(), e);
        }

        if (vibrate) {
            try {
                vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    Log.d(TAG, "开始振动");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 1000, 500, 1000, 500, 1000}, 0));
                    } else {
                        vibrator.vibrate(new long[]{0, 1000, 500, 1000, 500, 1000}, 0);
                    }
                } else {
                    Log.w(TAG, "振动器不可用");
                }
            } catch (Exception e) {
                Log.e(TAG, "振动失败", e);
            }
        } else {
            Log.d(TAG, "振动已关闭");
        }
    }

    private void playAlarmWithRingtone() {
        Log.d(TAG, "使用 Ringtone 播放铃声");
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                Log.d(TAG, "默认闹钟铃声为空，使用铃声");
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
            if (alarmUri == null) {
                Log.d(TAG, "默认铃声也为空，使用通知铃声");
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            Log.d(TAG, "使用的铃声URI: " + alarmUri);

            Ringtone ringtone = RingtoneManager.getRingtone(this, alarmUri);
            if (ringtone != null) {
                ringtone.setLooping(true);
                ringtone.play();
                Log.d(TAG, "Ringtone 播放成功");
            } else {
                Log.e(TAG, "Ringtone 为 null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ringtone 播放失败", e);
        }
    }

    private String getRingtoneUri() {
        String ringtoneUri = null;
        if (alarmId != null) {
            AlarmManager alarmManager = new AlarmManager(this);
            AlarmSetting alarm = alarmManager.getAlarmById(alarmId);
            if (alarm != null && alarm.getRingtone() != null && !alarm.getRingtone().isEmpty() && !"default".equals(alarm.getRingtone())) {
                ringtoneUri = alarm.getRingtone();
                Log.d(TAG, "使用自定义铃声: " + ringtoneUri);
            }
        }
        if (ringtoneUri == null || ringtoneUri.isEmpty()) {
            Uri defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            ringtoneUri = defaultUri != null ? defaultUri.toString() : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString();
            Log.d(TAG, "使用默认铃声: " + ringtoneUri);
        }
        return ringtoneUri;
    }

    private void startVolumeFade() {
        int totalSteps = FADE_DURATION / FADE_INTERVAL;
        float volumeStep = (MAX_VOLUME - MIN_VOLUME) / totalSteps;

        Log.d(TAG, "开始音量渐增: 从 " + String.format("%.0f", MIN_VOLUME * 100) + "% 到 " + String.format("%.0f", MAX_VOLUME * 100) + "%, 共 " + totalSteps + " 步, 每步增加 " + String.format("%.0f", volumeStep * 100) + "%");

        fadeRunnable = new Runnable() {
            int currentStep = 0;

            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying() && currentVolume < MAX_VOLUME) {
                    currentStep++;
                    currentVolume = Math.min(MIN_VOLUME + (volumeStep * currentStep), MAX_VOLUME);
                    mediaPlayer.setVolume(currentVolume, currentVolume);
                    Log.d(TAG, "音量渐增: 步骤 " + currentStep + "/" + totalSteps + ", 当前音量: " + String.format("%.0f", currentVolume * 100) + "%");
                    handler.postDelayed(this, FADE_INTERVAL);
                } else if (currentVolume >= MAX_VOLUME) {
                    Log.d(TAG, "音量已达到最大: " + String.format("%.0f", MAX_VOLUME * 100) + "%");
                }
            }
        };
        handler.postDelayed(fadeRunnable, FADE_INTERVAL);
    }

    private void stopAlarm() {
        Log.d(TAG, "停止闹钟");
        stopAlarmInternal();
        rescheduleNextAlarm();
        updateNextAlarmNotification();
    }

    private void stopAlarmInternal() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                Log.d(TAG, "MediaPlayer 已停止");
            }
            mediaPlayer.release();
            mediaPlayer = null;
            Log.d(TAG, "MediaPlayer 已释放");
        }
        if (vibrator != null) {
            vibrator.cancel();
            Log.d(TAG, "振动已取消");
        }
        if (handler != null) {
            if (stopRunnable != null) {
                handler.removeCallbacks(stopRunnable);
            }
            if (fadeRunnable != null) {
                handler.removeCallbacks(fadeRunnable);
            }
            Log.d(TAG, "Handler callbacks 已移除");
        }
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        Log.d(TAG, "闹钟音量已恢复为: " + previousAlarmVolume);
        cancelNotification();
    }

    private void rescheduleNextAlarm() {
        Log.d(TAG, "重新设置下一个闹钟");
        try {
            AlarmManager alarmManager = new AlarmManager(this);
            if (alarmId != null) {
                AlarmSetting alarm = alarmManager.getAlarmById(alarmId);
                if (alarm != null && alarm.isEnabled()) {
                    alarmManager.scheduleAlarm(alarm);
                    Log.d(TAG, "闹钟已重新设置到下一个" + alarm.getShiftType() + "日期");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "重新设置闹钟失败", e);
        }
    }

    private void updateNextAlarmNotification() {
        Log.d(TAG, "更新下一个闹钟通知");
        try {
            AlarmManager alarmManager = new AlarmManager(this);
            AlarmSetting nextAlarm = alarmManager.getNextAlarm();
            AlarmNotificationHelper.showNextAlarmNotification(this, nextAlarm);
            Log.d(TAG, "下一个闹钟通知已更新: " + (nextAlarm != null ? nextAlarm.getShiftType() + " " + nextAlarm.getReminderType() : "无"));
        } catch (Exception e) {
            Log.e(TAG, "更新下一个闹钟通知失败", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AlarmActivity onDestroy");
        stopAlarm();
    }
}