package com.example.myapplication.data;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class HolidayUpdateService {
    private static final String TAG = "HolidayUpdateService";
    private static final String API_URL = "https://api.example.com/holidays";
    
    public interface UpdateCallback {
        void onSuccess();
        void onFailed(String error);
    }
    
    public static void checkAndUpdateHolidays(final Context context, final int year, final UpdateCallback callback) {
        new AsyncTask<Void, Void, Boolean>() {
            private String errorMessage;
            
            @Override
            protected Boolean doInBackground(Void... voids) {
                try {
                    URL url = new URL(API_URL + "?year=" + year);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    
                    int responseCode = connection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        
                        return parseAndSave(response.toString(), context);
                    } else {
                        errorMessage = "Server error: " + responseCode;
                        return false;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error fetching holidays", e);
                    errorMessage = e.getMessage();
                    return false;
                }
            }
            
            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    callback.onSuccess();
                } else {
                    callback.onFailed(errorMessage);
                }
            }
        }.execute();
    }
    
    private static boolean parseAndSave(String jsonResponse, Context context) {
        try {
            JSONObject json = new JSONObject(jsonResponse);
            
            Map<String, String> holidays = new HashMap<>();
            JSONArray holidayArray = json.getJSONArray("holidays");
            for (int i = 0; i < holidayArray.length(); i++) {
                String date = holidayArray.getString(i);
                holidays.put(date, "休");
            }
            
            Map<String, String> workDays = new HashMap<>();
            JSONArray workArray = json.getJSONArray("workDays");
            for (int i = 0; i < workArray.length(); i++) {
                String date = workArray.getString(i);
                workDays.put(date, "班");
            }
            
            HolidayManager.getInstance().updateHolidays(holidays, workDays);
            HolidayManager.getInstance().saveToPreferences(context);
            
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing holiday data", e);
            return false;
        }
    }
    
    public static void checkForYearUpdate(final Context context, final UpdateCallback callback) {
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        HolidayManager manager = HolidayManager.getInstance();
        
        if (!manager.shouldCheckUpdate(currentYear)) {
            callback.onSuccess();
            return;
        }
        
        manager.setLastCheckTime(System.currentTimeMillis());
        
        if (manager.needsUpdate(currentYear)) {
            checkAndUpdateHolidays(context, currentYear, new UpdateCallback() {
                @Override
                public void onSuccess() {
                    manager.setLastUpdateYear(currentYear);
                    manager.saveToPreferences(context);
                    callback.onSuccess();
                }
                
                @Override
                public void onFailed(String error) {
                    manager.saveToPreferences(context);
                    callback.onFailed(error);
                }
            });
        } else if (java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) >= 10) {
            int nextYear = currentYear + 1;
            if (manager.needsUpdate(nextYear)) {
                checkAndUpdateHolidays(context, nextYear, new UpdateCallback() {
                    @Override
                    public void onSuccess() {
                        manager.setLastUpdateYear(nextYear);
                        manager.saveToPreferences(context);
                        callback.onSuccess();
                    }
                    
                    @Override
                    public void onFailed(String error) {
                        manager.saveToPreferences(context);
                        callback.onFailed(error);
                    }
                });
            } else {
                manager.saveToPreferences(context);
                callback.onSuccess();
            }
        } else {
            manager.saveToPreferences(context);
            callback.onSuccess();
        }
    }
}
