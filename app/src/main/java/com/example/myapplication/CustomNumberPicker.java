package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.NumberPicker;

import java.lang.reflect.Field;

public class CustomNumberPicker extends NumberPicker {

    private GestureDetector mGestureDetector;
    private EditText mEditText;
    private int mMinValue;
    private int mMaxValue;

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
        setDividerColor();
    }

    private void activateEditMode() {
        if (mEditText != null) {
            mEditText.setFocusable(true);
            mEditText.setFocusableInTouchMode(true);
            mEditText.setRawInputType(android.text.InputType.TYPE_CLASS_PHONE);
            mEditText.requestFocus();
            mEditText.selectAll();

            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(mEditText, InputMethodManager.SHOW_FORCED);
            }
        }
    }

    @Override
    public void setMinValue(int minValue) {
        super.setMinValue(minValue);
        mMinValue = minValue;
    }

    @Override
    public void setMaxValue(int maxValue) {
        super.setMaxValue(maxValue);
        mMaxValue = maxValue;
    }

    private void setDividerColor() {
        try {
            Field field = NumberPicker.class.getDeclaredField("mSelectionDivider");
            field.setAccessible(true);
            field.set(this, new ColorDrawable(Color.parseColor("#1E90FF")));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            Field field = NumberPicker.class.getDeclaredField("mSelectionDividersDistance");
            field.setAccessible(true);
            field.set(this, 36);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addView(android.view.View child, int index, android.view.ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof EditText) {
            mEditText = (EditText) child;
            mEditText.setTextSize(28);
            mEditText.setTextColor(Color.parseColor("#333333"));
            mEditText.setTypeface(null, android.graphics.Typeface.BOLD);
            mEditText.setSelectAllOnFocus(true);
            mEditText.setRawInputType(android.text.InputType.TYPE_CLASS_PHONE);

            mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    activateEditMode();
                    return true;
                }
            });

            mEditText.setOnTouchListener((v, event) -> {
                mGestureDetector.onTouchEvent(event);
                return false;
            });

            mEditText.setOnEditorActionListener((v, actionId, event) -> {
                validateAndSetValue();
                return true;
            });
        }
    }

    private void validateAndSetValue() {
        if (mEditText == null) return;

        try {
            int inputValue = Integer.parseInt(mEditText.getText().toString());
            int correctedValue = Math.max(mMinValue, Math.min(mMaxValue, inputValue));
            setValue(correctedValue);

            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mEditText.getWindowToken(), 0);
            }
        } catch (NumberFormatException e) {
        }
    }
}