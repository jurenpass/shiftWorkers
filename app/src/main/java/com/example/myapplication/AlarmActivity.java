package com.example.myapplication;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.example.myapplication.data.AlarmManager;
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
    private static final int FADE_DURATION = 3000;
    private static final int FADE_INTERVAL = 300;
    private static final int MAX_RETRY_COUNT = 10;
    private static final int RETRY_DELAY_MS = 1000;
    private static final long AUTO_STOP_DELAY = 10 * 60 * 1000; // 10分钟自动关闭

    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private Handler handler;
    private Runnable stopRunnable;
    private Runnable fadeRunnable;
    private Runnable retryRunnable;
    private Runnable keepAliveRunnable;
    private boolean isUserClosed = false;
    private boolean isAlarmStopped = false;
    private String alarmId;
    private static final String PREFS_ALARM_CLOSED = "alarm_closed";
    private static final String KEY_ALARM_CLOSED = "is_alarm_closed";
    private float currentVolume = MIN_VOLUME;
    private AudioManager audioManager;
    private int previousRingerMode;
    private int maxAlarmVolume;
    private int previousAlarmVolume;
    private PowerManager.WakeLock wakeLock;
    private int retryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "========== AlarmActivity onCreate ==========");

        SharedPreferences prefs = getSharedPreferences(PREFS_ALARM_CLOSED, MODE_PRIVATE);
        isUserClosed = prefs.getBoolean(KEY_ALARM_CLOSED, false);
        
        if (isUserClosed) {
            Log.d(TAG, "检测到之前已关闭闹钟，直接结束");
            prefs.edit().putBoolean(KEY_ALARM_CLOSED, false).apply();
            finish();
            return;
        }

        setupWindowFlags();
        acquireWakeLock();

        setContentView(R.layout.activity_alarm);

        cancelNotification();

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousRingerMode = audioManager.getRingerMode();

        maxAlarmVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarmVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
        Log.d(TAG, "闹钟音量已设置为最大: " + maxAlarmVolume + ", 之前音量: " + previousAlarmVolume);

        ensureActivityVisible();
        scheduleRetryIfNeeded();

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
                    closeAlarm();
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
            Log.d(TAG, "10分钟自动关闭闹钟");
            closeAlarm();
        };
        handler.postDelayed(stopRunnable, AUTO_STOP_DELAY);

        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUserClosed) {
                    Log.d(TAG, "保持闹钟Activity活跃");
                    ensureActivityVisible();
                    bringToFront();
                    handler.postDelayed(this, 5000);
                }
            }
        };
        handler.postDelayed(keepAliveRunnable, 5000);

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

    private void acquireWakeLock() {
        try {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | 
                        PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                        PowerManager.ON_AFTER_RELEASE |
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                        "AlarmActivity:WakeLock"
                );
                wakeLock.acquire(60000);
                Log.d(TAG, "唤醒锁已获取");
            }
        } catch (Exception e) {
            Log.e(TAG, "获取唤醒锁失败", e);
        }
    }

    private void setupWindowFlags() {
        Window window = getWindow();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            Log.d(TAG, "使用 setShowWhenLocked 和 setTurnScreenOn");
        }
        
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        window.addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);
        
        Log.d(TAG, "窗口标志设置完成");
    }

    private void ensureActivityVisible() {
        bringToFront();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.app.ActivityManager activityManager = (android.app.ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.moveTaskToFront(getTaskId(), android.app.ActivityManager.MOVE_TASK_WITH_HOME);
                }
            } catch (Exception e) {
                Log.e(TAG, "确保Activity可见失败", e);
            }
        }
    }

    private void scheduleRetryIfNeeded() {
        retryRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isUserClosed && retryCount < MAX_RETRY_COUNT) {
                    retryCount++;
                    Log.d(TAG, "重试确保Activity可见, 次数: " + retryCount);
                    ensureActivityVisible();
                    handler.postDelayed(this, RETRY_DELAY_MS * retryCount);
                }
            }
        };
        handler = new Handler();
        handler.postDelayed(retryRunnable, RETRY_DELAY_MS);
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

    private void closeAlarm() {
        if (isAlarmStopped) {
            Log.d(TAG, "闹钟已经停止，无需重复关闭");
            return;
        }
        
        isUserClosed = true;
        isAlarmStopped = true;
        
        Log.d(TAG, "关闭闹钟");
        
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Log.d(TAG, "MediaPlayer 已停止");
                }
                mediaPlayer.release();
                mediaPlayer = null;
                Log.d(TAG, "MediaPlayer 已释放");
            } catch (Exception e) {
                Log.e(TAG, "停止MediaPlayer失败", e);
            }
        }
        
        if (vibrator != null) {
            try {
                vibrator.cancel();
                Log.d(TAG, "振动已取消");
            } catch (Exception e) {
                Log.e(TAG, "取消振动失败", e);
            }
        }
        
        try {
            Intent intent = new Intent(com.example.myapplication.data.AlarmManager.ACTION_ALARM_CHANGED);
            sendBroadcast(intent);
            Log.d(TAG, "已发送闹钟变化广播，通知栏将更新");
        } catch (Exception e) {
            Log.e(TAG, "发送广播失败", e);
        }
        
        if (handler != null) {
            if (stopRunnable != null) {
                handler.removeCallbacks(stopRunnable);
            }
            if (fadeRunnable != null) {
                handler.removeCallbacks(fadeRunnable);
            }
            if (retryRunnable != null) {
                handler.removeCallbacks(retryRunnable);
            }
            if (keepAliveRunnable != null) {
                handler.removeCallbacks(keepAliveRunnable);
            }
            Log.d(TAG, "Handler callbacks 已移除");
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
                Log.d(TAG, "唤醒锁已释放");
            } catch (Exception e) {
                Log.e(TAG, "释放唤醒锁失败", e);
            }
        }
        
        if (audioManager != null) {
            try {
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, previousAlarmVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                Log.d(TAG, "闹钟音量已恢复为: " + previousAlarmVolume);
            } catch (Exception e) {
                Log.e(TAG, "恢复音量失败", e);
            }
        }
        
        cancelNotification();
        
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_ALARM_CLOSED, MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_ALARM_CLOSED, true).apply();
            Log.d(TAG, "闹钟关闭状态已保存");
        } catch (Exception e) {
            Log.e(TAG, "保存关闭状态失败", e);
        }
        
        rescheduleNextAlarm();
        
        SharedPreferences prefs = getSharedPreferences("alarm_prefs", MODE_PRIVATE);
        boolean appWasInForeground = prefs.getBoolean("app_was_in_foreground", false);
        
        if (appWasInForeground) {
            Log.d(TAG, "响铃前APP在前台，保持APP界面");
            finish();
        } else {
            Log.d(TAG, "响铃前APP不在前台，移到后台回到之前界面");
            moveTaskToBack(true);
            finish();
        }
    }

    private void rescheduleNextAlarm() {
        Log.d(TAG, "重新设置所有闹钟");
        try {
            AlarmManager alarmManager = new AlarmManager(this);
            
            if (alarmId != null) {
                AlarmSetting alarm = alarmManager.getAlarmById(alarmId);
                if (alarm != null && alarm.isEnabled()) {
                    alarmManager.scheduleAlarm(alarm);
                    Log.d(TAG, "当前闹钟已重新设置到下一个" + alarm.getShiftType() + "日期");
                }
            }
            
            for (AlarmSetting alarm : alarmManager.getAllAlarms()) {
                if (alarm.isEnabled()) {
                    alarmManager.scheduleAlarm(alarm);
                    Log.d(TAG, "重新调度闹钟: " + alarm.getShiftType() + " " + alarm.getReminderType());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "重新设置闹钟失败", e);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "AlarmActivity onResume");

        if (isUserClosed) {
            Log.d(TAG, "用户已关闭闹钟，直接结束");
            finish();
            return;
        }
        
        setupWindowFlags();
        ensureActivityVisible();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "AlarmActivity onPause, isFinishing: " + isFinishing());
        
        if (!isAlarmStopped && isFinishing()) {
            Log.d(TAG, "Activity结束且闹钟未停止，关闭闹钟");
            closeAlarm();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "AlarmActivity onStop");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_POWER || keyCode == KeyEvent.KEYCODE_SLEEP) && !isUserClosed) {
            Log.d(TAG, "拦截电源键");
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "返回键关闭闹钟");
        closeAlarm();
    }

    private void cancelNotification() {
        try {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFICATION_ID);
                notificationManager.cancel(NOTIFICATION_ID + 1);
                Log.d(TAG, "通知已取消");
            }
        } catch (Exception e) {
            Log.e(TAG, "取消通知失败", e);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AlarmActivity onDestroy");
        
        if (handler != null) {
            if (stopRunnable != null) {
                handler.removeCallbacks(stopRunnable);
            }
            if (fadeRunnable != null) {
                handler.removeCallbacks(fadeRunnable);
            }
            if (retryRunnable != null) {
                handler.removeCallbacks(retryRunnable);
            }
            if (keepAliveRunnable != null) {
                handler.removeCallbacks(keepAliveRunnable);
            }
        }
        
        if (wakeLock != null && wakeLock.isHeld()) {
            try {
                wakeLock.release();
            } catch (Exception e) {
                Log.e(TAG, "释放唤醒锁失败", e);
            }
        }
        
        com.example.myapplication.data.AlarmManager alarmManager = new com.example.myapplication.data.AlarmManager(this);
        if (alarmManager.isAlarmServiceEnabled()) {
            Intent serviceIntent = new Intent(this, PersistentAlarmService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            Log.d(TAG, "重启PersistentAlarmService");
        } else {
            Log.d(TAG, "闹钟服务已关闭，不重启持久服务");
        }
    }
}
