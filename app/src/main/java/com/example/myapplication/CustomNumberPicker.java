package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class CustomNumberPicker extends View {
    private List<Integer> values = new ArrayList<>();
    private int selectedIndex = 0;
    private float scrollY = 0;
    private float lastTouchY = 0;
    
    private Paint textPaint;
    private Paint dividerPaint;
    private Paint selectedTextPaint;
    
    private int itemHeight = 200;
    private int visibleCount = 3;
    private float textSizeNormal;
    private float textSizeSelected;
    private boolean wrapEnabled = false;
    
    public CustomNumberPicker(Context context) {
        super(context);
        init(context);
    }
    
    public CustomNumberPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public CustomNumberPicker(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        textSizeNormal = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28, 
                context.getResources().getDisplayMetrics());
        textSizeSelected = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 36, 
                context.getResources().getDisplayMetrics());
        
        textPaint = new Paint();
        textPaint.setTextSize(textSizeNormal);
        textPaint.setColor(Color.parseColor("#999999"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        selectedTextPaint = new Paint();
        selectedTextPaint.setTextSize(textSizeSelected);
        selectedTextPaint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        selectedTextPaint.setColor(Color.parseColor("#333333"));
        selectedTextPaint.setTextAlign(Paint.Align.CENTER);
        
        dividerPaint = new Paint();
        dividerPaint.setColor(Color.parseColor("#1E90FF"));
        dividerPaint.setStrokeWidth(6f);
        
        setFocusable(true);
        setFocusableInTouchMode(true);
    }
    
    public void setWrapEnabled(boolean enabled) {
        this.wrapEnabled = enabled;
    }
    
    public void setMinValue(int min) {
        values.clear();
        int max = values.isEmpty() ? min + 20 : values.get(values.size() - 1);
        for (int i = min; i <= max; i++) {
            values.add(i);
        }
        selectedIndex = values.size() / 2;
        invalidate();
    }
    
    public void setMaxValue(int max) {
        int min = values.isEmpty() ? max - 20 : values.get(0);
        values.clear();
        for (int i = min; i <= max; i++) {
            values.add(i);
        }
        selectedIndex = values.size() / 2;
        invalidate();
    }
    
    public void setValue(int value) {
        for (int i = 0; i < values.size(); i++) {
            if (values.get(i) == value) {
                selectedIndex = i;
                scrollY = 0;
                invalidate();
                return;
            }
        }
        if (!values.isEmpty()) {
            selectedIndex = 0;
            invalidate();
        }
    }
    
    public int getValue() {
        if (values.isEmpty()) return 1;
        return values.get(selectedIndex);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = itemHeight * visibleCount;
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), height);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int width = getWidth();
        int height = getHeight();
        int centerY = height / 2;
        
        canvas.drawLine(0, centerY - itemHeight / 2, width, centerY - itemHeight / 2, dividerPaint);
        canvas.drawLine(0, centerY + itemHeight / 2, width, centerY + itemHeight / 2, dividerPaint);
        
        int firstVisibleIndex = selectedIndex - 1;
        int lastVisibleIndex = selectedIndex + 1;
        
        for (int i = firstVisibleIndex; i <= lastVisibleIndex; i++) {
            int actualIndex = i;
            if (wrapEnabled && !values.isEmpty()) {
                while (actualIndex < 0) {
                    actualIndex += values.size();
                }
                actualIndex = actualIndex % values.size();
            }
            
            if (actualIndex < 0 || actualIndex >= values.size()) continue;
            
            int y = centerY + (i - selectedIndex) * itemHeight;
            float alpha = 1.0f - Math.abs(i - selectedIndex) * 0.3f;
            
            Paint paint = (i == selectedIndex) ? selectedTextPaint : textPaint;
            paint.setAlpha((int) (255 * alpha));
            
            canvas.drawText(String.valueOf(values.get(actualIndex)), width / 2, y + paint.getTextSize() / 2, paint);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - lastTouchY;
                scrollY += deltaY;
                
                if (Math.abs(scrollY) > itemHeight / 3) {
                    if (scrollY > 0) {
                        if (selectedIndex > 0) {
                            selectedIndex--;
                        } else if (wrapEnabled && !values.isEmpty()) {
                            selectedIndex = values.size() - 1;
                        }
                    } else {
                        if (selectedIndex < values.size() - 1) {
                            selectedIndex++;
                        } else if (wrapEnabled && !values.isEmpty()) {
                            selectedIndex = 0;
                        }
                    }
                    scrollY = 0;
                    invalidate();
                }
                
                lastTouchY = event.getY();
                return true;
            case MotionEvent.ACTION_UP:
                scrollY = 0;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }
}
