package com.example.myapplication.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class ShiftRuleManager {
    private static final String PREFS_NAME = "shift_rules_prefs";
    private static final String KEY_RULES = "rules";
    private static final String KEY_CURRENT_RULE_ID = "current_rule_id";
    
    private static ShiftRuleManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    
    private ShiftRuleManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        initDefaultRules();
    }
    
    public static synchronized ShiftRuleManager getInstance(Context context) {
        if (instance == null) {
            instance = new ShiftRuleManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private void initDefaultRules() {
        List<ShiftRule> rules = getRules();
        if (rules.isEmpty()) {
            ShiftRule hsm2Rule = ShiftCalendarUtil.createDefaultRule();
            hsm2Rule.setName("武钢二热轧");
            hsm2Rule.setCompanyName("HSM2");
            hsm2Rule.setTag("");
            hsm2Rule.setCycleDays(4);
            hsm2Rule.setGroupCount(4);
            hsm2Rule.setDefault(true);
            hsm2Rule.setId("hsm2_main");
            
            List<ShiftRule.ShiftDetail> details = new ArrayList<>();
            details.add(new ShiftRule.ShiftDetail(1, ShiftCalendarUtil.SHIFT_WHITE, "08:00到20:00"));
            details.add(new ShiftRule.ShiftDetail(2, ShiftCalendarUtil.SHIFT_NIGHT, "20:00到23:59"));
            details.add(new ShiftRule.ShiftDetail(3, ShiftCalendarUtil.SHIFT_EVENING, "00:00到08:00"));
            details.add(new ShiftRule.ShiftDetail(4, ShiftCalendarUtil.SHIFT_REST, "00:00到23:59"));
            hsm2Rule.setShiftDetails(details);
            
            hsm2Rule.setDefaultGroupName("丁班");
            hsm2Rule.setTodayShift(ShiftCalendarUtil.SHIFT_WHITE);
            
            List<ShiftRule.OtherGroup> otherGroups = new ArrayList<>();
            otherGroups.add(new ShiftRule.OtherGroup("丙班", ShiftCalendarUtil.SHIFT_NIGHT));
            otherGroups.add(new ShiftRule.OtherGroup("乙班", ShiftCalendarUtil.SHIFT_EVENING));
            otherGroups.add(new ShiftRule.OtherGroup("甲班", ShiftCalendarUtil.SHIFT_REST));
            hsm2Rule.setOtherGroups(otherGroups);
            
            rules.add(hsm2Rule);
            
            saveRules(rules);
            if (getCurrentRuleId() == null) {
                setCurrentRuleId("hsm2_main");
            }
        }
    }
    
    public List<ShiftRule> getRules() {
        String json = prefs.getString(KEY_RULES, "");
        if (json.isEmpty()) {
            return new ArrayList<>();
        }
        return gson.fromJson(json, new TypeToken<List<ShiftRule>>() {}.getType());
    }
    
    public void saveRules(List<ShiftRule> rules) {
        String json = gson.toJson(rules);
        prefs.edit().putString(KEY_RULES, json).apply();
    }
    
    public ShiftRule getRuleById(String id) {
        List<ShiftRule> rules = getRules();
        for (ShiftRule rule : rules) {
            if (id.equals(rule.getId())) {
                return rule;
            }
        }
        return null;
    }
    
    public List<ShiftRule> getRulesByCompany(String companyName) {
        List<ShiftRule> result = new ArrayList<>();
        List<ShiftRule> rules = getRules();
        for (ShiftRule rule : rules) {
            if (companyName.equals(rule.getCompanyName())) {
                result.add(rule);
            }
        }
        return result;
    }
    
    public List<String> getCompanies() {
        List<String> companies = new ArrayList<>();
        List<ShiftRule> rules = getRules();
        for (ShiftRule rule : rules) {
            String company = rule.getCompanyName();
            if (!companies.contains(company)) {
                companies.add(company);
            }
        }
        return companies;
    }
    
    public void addRule(ShiftRule rule) {
        List<ShiftRule> rules = getRules();
        if (rule.getId() == null || rule.getId().isEmpty()) {
            rule.setId(String.valueOf(System.currentTimeMillis()));
        }
        rules.add(rule);
        saveRules(rules);
    }
    
    public void updateRule(ShiftRule rule) {
        List<ShiftRule> rules = getRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId() != null && rules.get(i).getId().equals(rule.getId())) {
                rules.set(i, rule);
                break;
            }
        }
        saveRules(rules);
    }
    
    public void deleteRule(String id) {
        List<ShiftRule> rules = getRules();
        for (int i = rules.size() - 1; i >= 0; i--) {
            if (rules.get(i).getId() != null && rules.get(i).getId().equals(id)) {
                rules.remove(i);
                break;
            }
        }
        saveRules(rules);
    }
    
    public String getCurrentRuleId() {
        return prefs.getString(KEY_CURRENT_RULE_ID, null);
    }
    
    public void setCurrentRuleId(String id) {
        prefs.edit().putString(KEY_CURRENT_RULE_ID, id).apply();
    }
    
    public ShiftRule getCurrentRule() {
        String id = getCurrentRuleId();
        if (id != null) {
            return getRuleById(id);
        }
        return null;
    }
}
