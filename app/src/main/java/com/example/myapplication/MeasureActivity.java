package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Camera;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class MeasureActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private TextView tvLength;
    private View startPoint;
    private View endPoint;
    private ImageView measureLine;
    private View measureContainer;
    private LinearLayout guideLayout;
    private Button btnCm;
    private Button btnInch;
    private SurfaceView cameraPreview;
    private Camera camera;

    private float startX = -1, startY = -1;
    private float endX = -1, endY = -1;
    private boolean isFirstPoint = true;
    private boolean isMetric = true;
    private float pixelsPerCm = 50f;

    private static final int REQUEST_CAMERA_PERMISSION = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);

        tvLength = findViewById(R.id.tv_length);
        startPoint = findViewById(R.id.start_point);
        endPoint = findViewById(R.id.end_point);
        measureLine = findViewById(R.id.measure_line);
        measureContainer = findViewById(R.id.measure_container);
        guideLayout = findViewById(R.id.guide_layout);
        btnCm = findViewById(R.id.btn_cm);
        btnInch = findViewById(R.id.btn_inch);
        cameraPreview = findViewById(R.id.camera_preview);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetMeasurement());
        findViewById(R.id.btn_calibrate).setOnClickListener(v -> showCalibrateDialog());

        btnCm.setOnClickListener(v -> switchToMetric(true));
        btnInch.setOnClickListener(v -> switchToMetric(false));

        measureContainer.setOnTouchListener(this::onTouchEvent);
        cameraPreview.getHolder().addCallback(this);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "需要相机权限才能使用测量功能", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (camera != null) {
            Camera.Parameters params = camera.getParameters();
            Camera.Size size = getOptimalPreviewSize(width, height, params);
            params.setPreviewSize(size.width, size.height);
            camera.setParameters(params);
            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopCamera();
    }

    private void startCamera() {
        try {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            camera.setPreviewDisplay(cameraPreview.getHolder());
            Camera.Parameters params = camera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            camera.setParameters(params);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    private Camera.Size getOptimalPreviewSize(int w, int h, Camera.Parameters params) {
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        for (Camera.Size size : params.getSupportedPreviewSizes()) {
            double diff = Math.abs(size.height - targetHeight);
            if (diff < minDiff) {
                optimalSize = size;
                minDiff = diff;
            }
        }

        if (optimalSize == null) {
            optimalSize = params.getSupportedPreviewSizes().get(0);
        }

        return optimalSize;
    }

    private boolean onTouchEvent(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            guideLayout.setVisibility(View.INVISIBLE);

            if (isFirstPoint) {
                startX = x;
                startY = y;
                showStartPoint(x, y);
                isFirstPoint = false;
            } else {
                endX = x;
                endY = y;
                showEndPoint(x, y);
                calculateAndDisplayLength();
                drawLine();
                isFirstPoint = true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && !isFirstPoint) {
            endX = x;
            endY = y;
            showEndPoint(x, y);
            calculateAndDisplayLength();
            drawLine();
        }

        return true;
    }

    private void showStartPoint(float x, float y) {
        startPoint.setX(x - 12);
        startPoint.setY(y - 12);
        startPoint.setVisibility(View.VISIBLE);
        endPoint.setVisibility(View.INVISIBLE);
        measureLine.setVisibility(View.INVISIBLE);
    }

    private void showEndPoint(float x, float y) {
        endPoint.setX(x - 12);
        endPoint.setY(y - 12);
        endPoint.setVisibility(View.VISIBLE);
    }

    private void calculateAndDisplayLength() {
        float distance = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        float lengthCm = distance / pixelsPerCm;
        float lengthInch = lengthCm / 2.54f;

        if (isMetric) {
            tvLength.setText(String.format("%.2f", lengthCm));
        } else {
            tvLength.setText(String.format("%.2f", lengthInch));
        }
    }

    private void drawLine() {
        int width = measureContainer.getWidth();
        int height = measureContainer.getHeight();

        if (width == 0 || height == 0) return;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(3);
        paint.setAlpha(200);

        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        canvas.drawPath(path, paint);

        paint.setStrokeWidth(1);
        paint.setAlpha(100);
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        if (length > 0) {
            float arrowLength = 15;
            float arrowAngle = (float) Math.toRadians(30);

            float endArrowX1 = endX - arrowLength * (float) Math.cos(Math.atan2(dy, dx) - arrowAngle);
            float endArrowY1 = endY - arrowLength * (float) Math.sin(Math.atan2(dy, dx) - arrowAngle);
            float endArrowX2 = endX - arrowLength * (float) Math.cos(Math.atan2(dy, dx) + arrowAngle);
            float endArrowY2 = endY - arrowLength * (float) Math.sin(Math.atan2(dy, dx) + arrowAngle);

            canvas.drawLine(endX, endY, endArrowX1, endArrowY1, paint);
            canvas.drawLine(endX, endY, endArrowX2, endArrowY2, paint);
        }

        measureLine.setImageBitmap(bitmap);
        measureLine.setVisibility(View.VISIBLE);
    }

    private void switchToMetric(boolean metric) {
        isMetric = metric;
        if (metric) {
            btnCm.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
            btnInch.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        } else {
            btnCm.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
            btnInch.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_light));
        }
        if (startX >= 0 && endX >= 0) {
            calculateAndDisplayLength();
        }
    }

    private void resetMeasurement() {
        startX = -1;
        startY = -1;
        endX = -1;
        endY = -1;
        isFirstPoint = true;
        startPoint.setVisibility(View.INVISIBLE);
        endPoint.setVisibility(View.INVISIBLE);
        measureLine.setVisibility(View.INVISIBLE);
        guideLayout.setVisibility(View.VISIBLE);
        tvLength.setText("0.00");
    }

    private void showCalibrateDialog() {
        Toast.makeText(this, "使用标准物品进行校准\n\n1. 点击开始点\n2. 沿物品边缘拖动到结束点\n3. 输入实际长度", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
    }
}