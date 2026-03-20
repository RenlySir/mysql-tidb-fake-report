package com.example.mysqltidbreport.model;

public class IncompatibleObject {

    private String category;
    private String objectType;
    private String schemaName;
    private String objectName;
    private String reason;
    private String definition;

    public IncompatibleObject() {
    }

    public IncompatibleObject(String category, String objectType, String schemaName, String objectName,
                              String reason, String definition) {
        this.category = category;
        this.objectType = objectType;
        this.schemaName = schemaName;
        this.objectName = objectName;
        this.reason = reason;
        this.definition = definition;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDefinition() {
        return definition;
    }

    public void setDefinition(String definition) {
        this.definition = definition;
    }
}
