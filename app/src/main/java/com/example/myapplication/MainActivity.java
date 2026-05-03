package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.Toast;

import com.example.myapplication.data.HolidayManager;
import com.example.myapplication.data.HolidayUpdateService;
import com.example.myapplication.data.ShiftRule;
import com.example.myapplication.data.ShiftCalendarUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private ShiftRule currentRule;
    private float startX, startY;
    private static final int SWIPE_THRESHOLD = 50;
    private Calendar currentCalendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentRule = ShiftCalendarUtil.createDefaultRule();
        
        initHolidayData();

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        fragmentManager = getSupportFragmentManager();
        
        checkHolidayUpdate();

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                Fragment fragment = null;

                if (id == R.id.nav_home) {
                    fragment = HomeFragment.newInstance(currentRule);
                } else if (id == R.id.nav_calendar) {
                    fragment = CalendarFragment.newInstance(currentRule);
                } else if (id == R.id.nav_alarm) {
                    fragment = AlarmFragment.newInstance();
                } else if (id == R.id.nav_profile) {
                    fragment = ProfileFragment.newInstance();
                }

                if (fragment != null) {
                    FragmentTransaction transaction = fragmentManager.beginTransaction();
                    transaction.replace(R.id.fragment_container, fragment);
                    transaction.commit();
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
        }
        
        }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = ev.getX();
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_UP:
                float diffX = ev.getX() - startX;
                float diffY = ev.getY() - startY;
                
                if (Math.abs(diffX) > SWIPE_THRESHOLD || Math.abs(diffY) > SWIPE_THRESHOLD) {
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (diffX > SWIPE_THRESHOLD) {
                            currentCalendar.add(Calendar.MONTH, -1);
                        } else {
                            currentCalendar.add(Calendar.MONTH, 1);
                        }
                    } else {
                        if (diffY > SWIPE_THRESHOLD) {
                            currentCalendar.add(Calendar.YEAR, -1);
                        } else {
                            currentCalendar.add(Calendar.YEAR, 1);
                        }
                    }
                    Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof CalendarFragment) {
                        ((CalendarFragment) currentFragment).updateCalendarByDate(currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH) + 1);
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }
    
    private void initHolidayData() {
        HolidayManager.getInstance().loadFromPreferences(this);
    }
    
    private void checkHolidayUpdate() {
        HolidayUpdateService.checkForYearUpdate(this, new HolidayUpdateService.UpdateCallback() {
            @Override
            public void onSuccess() {
            }
            
            @Override
            public void onFailed(String error) {
            }
        });
    }

    public ShiftRule getCurrentRule() {
        return currentRule;
    }

    public void setCurrentRule(ShiftRule rule) {
        this.currentRule = rule;
    }

    public void refreshCalendar() {
        bottomNavigationView.setSelectedItemId(R.id.nav_calendar);
    }
}
