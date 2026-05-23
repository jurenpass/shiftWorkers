package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "收到启动完成广播");

            com.example.myapplication.data.AlarmManager alarmManager = new com.example.myapplication.data.AlarmManager(context);
            
            if (alarmManager.isAlarmServiceEnabled()) {
                Log.d(TAG, "闹钟服务已开启，启动持久服务并调度闹钟");
                
                // 启动持久前台服务保持进程活跃
                Intent serviceIntent = new Intent(context, PersistentAlarmService.class);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                // 重新调度所有闹钟
                alarmManager.scheduleAllAlarms();
            } else {
                Log.d(TAG, "闹钟服务已关闭，不启动持久服务");
            }
        }
    }
}
