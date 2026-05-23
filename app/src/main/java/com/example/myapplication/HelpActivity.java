package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HelpActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_STORAGE = 100;
    private File savedImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ImageView ivQrcode = findViewById(R.id.iv_qrcode);
        ivQrcode.setOnLongClickListener(v -> {
            showSaveDialog();
            return true;
        });
    }

    private void showSaveDialog() {
        new AlertDialog.Builder(this)
                .setTitle("二维码操作")
                .setItems(new String[]{"保存到相册", "保存并打开微信"}, (dialog, which) -> {
                    if (checkPermission()) {
                        if (which == 0) {
                            saveQrCode(false);
                        } else {
                            saveQrCode(true);
                        }
                    } else {
                        requestPermission();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return true; // Android 13+ 不需要存储权限
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveQrCode(false);
            } else {
                Toast.makeText(this, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveQrCode(boolean openWeChat) {
        try {
            ImageView ivQrcode = findViewById(R.id.iv_qrcode);
            Bitmap bitmap = ((BitmapDrawable) ivQrcode.getDrawable()).getBitmap();

            String fileName = "wechat_qrcode_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".png";
            
            File saveDir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                saveDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            } else {
                saveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            }
            
            if (saveDir != null && !saveDir.exists()) {
                saveDir.mkdirs();
            }

            File imageFile = new File(saveDir, fileName);
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();

            savedImageFile = imageFile;
            
            if (openWeChat) {
                Toast.makeText(this, "二维码已保存，正在打开微信...", Toast.LENGTH_SHORT).show();
                openWeChatApp();
            } else {
                Toast.makeText(this, "二维码已保存到相册", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openWeChatApp() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                Toast.makeText(this, "请在微信中使用扫一扫功能", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "未检测到微信应用", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "打开微信失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}