package com.example.myapplication;

import android.content.Intent;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RingtoneSelectActivity extends AppCompatActivity {
    private static final String TAG = "RingtoneSelectActivity";
    private RingtoneAdapter adapter;
    private Ringtone currentRingtone;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringtone_select);

        String selectedRingtone = getIntent().getStringExtra("selected_ringtone");

        List<RingtoneItem> ringtoneList = new ArrayList<>();
        ringtoneList.add(new RingtoneItem("default", "默认铃声", null));

        File miuiAlarmDir = new File("/system/media/audio/alarms");
        if (miuiAlarmDir.exists() && miuiAlarmDir.isDirectory()) {
            File[] files = miuiAlarmDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".ogg") || file.getName().endsWith(".mp3") || file.getName().endsWith(".wav"))) {
                        String name = file.getName().replaceAll("\\.(ogg|mp3|wav)$", "");
                        Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                        ringtoneList.add(new RingtoneItem(uri.toString(), "MIUI闹钟 - " + name, uri));
                    }
                }
            }
        }

        File miuiRingtoneDir = new File("/system/media/audio/ringtones");
        if (miuiRingtoneDir.exists() && miuiRingtoneDir.isDirectory()) {
            File[] files = miuiRingtoneDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && (file.getName().endsWith(".ogg") || file.getName().endsWith(".mp3") || file.getName().endsWith(".wav"))) {
                        String name = file.getName().replaceAll("\\.(ogg|mp3|wav)$", "");
                        Uri uri = Uri.parse("file://" + file.getAbsolutePath());
                        ringtoneList.add(new RingtoneItem(uri.toString(), "MIUI铃声 - " + name, uri));
                    }
                }
            }
        }

        RingtoneManager ringtoneManager = new RingtoneManager(this);
        ringtoneManager.setType(RingtoneManager.TYPE_ALARM);
        Cursor cursor = ringtoneManager.getCursor();

        while (cursor.moveToNext()) {
            String title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String uri = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
            String id = cursor.getString(RingtoneManager.ID_COLUMN_INDEX);
            Uri ringtoneUri = Uri.parse(uri + "/" + id);
            ringtoneList.add(new RingtoneItem(ringtoneUri.toString(), title, ringtoneUri));
        }
        cursor.close();

        adapter = new RingtoneAdapter(this, ringtoneList, selectedRingtone);

        ListView listView = findViewById(R.id.list_ringtones);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            RingtoneItem item = ringtoneList.get(position);
            playRingtone(item.getUriString());
            adapter.setSelected(item.getUriString());
            adapter.notifyDataSetChanged();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("ringtone", item.getUriString());
            setResult(RESULT_OK, resultIntent);
        });

        findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            stopRingtone();
            finish();
        });

        findViewById(R.id.btn_ok).setOnClickListener(v -> {
            stopRingtone();
            finish();
        });
    }

    private void playRingtone(String uriString) {
        stopRingtone();
        if (uriString != null && !uriString.equals("default")) {
            try {
                Uri uri = Uri.parse(uriString);
                if (uriString.startsWith("file://")) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
                    mediaPlayer.setDataSource(this, uri);
                    mediaPlayer.setLooping(false);
                    mediaPlayer.setVolume(1.0f, 1.0f);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } else {
                    currentRingtone = RingtoneManager.getRingtone(this, uri);
                    if (currentRingtone != null) {
                        currentRingtone.setLooping(false);
                        currentRingtone.play();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "播放铃声失败", e);
            }
        }
    }

    private void stopRingtone() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception e) {
                Log.e(TAG, "停止MediaPlayer失败", e);
            }
        }
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
            currentRingtone = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }
}