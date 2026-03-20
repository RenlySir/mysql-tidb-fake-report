package com.example.mysqltidbreport.model;

import java.util.ArrayList;
import java.util.List;

public class CompatibilityItem {

    private String category;
    private String mysqlUsage;
    private String tidbCompatibility;
    private String evidence;
    private String recommendation;
    private List<IncompatibleObject> incompatibleObjects = new ArrayList<>();

    public CompatibilityItem() {
    }

    public CompatibilityItem(String category, String mysqlUsage, String tidbCompatibility, String evidence,
                             String recommendation) {
        this.category = category;
        this.mysqlUsage = mysqlUsage;
        this.tidbCompatibility = tidbCompatibility;
        this.evidence = evidence;
        this.recommendation = recommendation;
    }

    public CompatibilityItem(String category, String mysqlUsage, String tidbCompatibility, String evidence,
                             String recommendation, List<IncompatibleObject> incompatibleObjects) {
        this.category = category;
        this.mysqlUsage = mysqlUsage;
        this.tidbCompatibility = tidbCompatibility;
        this.evidence = evidence;
        this.recommendation = recommendation;
        if (incompatibleObjects != null) {
            this.incompatibleObjects = incompatibleObjects;
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMysqlUsage() {
        return mysqlUsage;
    }

    public void setMysqlUsage(String mysqlUsage) {
        this.mysqlUsage = mysqlUsage;
    }

    public String getTidbCompatibility() {
        return tidbCompatibility;
    }

    public void setTidbCompatibility(String tidbCompatibility) {
        this.tidbCompatibility = tidbCompatibility;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public List<IncompatibleObject> getIncompatibleObjects() {
        return incompatibleObjects;
    }

    public void setIncompatibleObjects(List<IncompatibleObject> incompatibleObjects) {
        this.incompatibleObjects = incompatibleObjects;
    }
}
