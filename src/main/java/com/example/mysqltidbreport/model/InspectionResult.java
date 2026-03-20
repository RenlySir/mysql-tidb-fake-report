package com.example.mysqltidbreport.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InspectionResult {

    private LocalDateTime generatedAt;
    private String scannedSchema;
    private Map<String, String> serverVariables = new LinkedHashMap<>();
    private Map<String, String> isolationLevels = new LinkedHashMap<>();

    private List<Map<String, Object>> schemaCharsets = new ArrayList<>();
    private List<Map<String, Object>> tableCollations = new ArrayList<>();
    private List<Map<String, Object>> columnCharsets = new ArrayList<>();
    private List<Map<String, Object>> storageEngines = new ArrayList<>();
    private List<Map<String, Object>> tableCharsetCollationDetails = new ArrayList<>();
    private List<Map<String, Object>> columnCharsetCollationDetails = new ArrayList<>();
    private List<Map<String, Object>> engineTableDetails = new ArrayList<>();
    private Map<String, String> tableDefinitions = new LinkedHashMap<>();

    private List<Map<String, Object>> sqlModeRows = new ArrayList<>();
    private List<Map<String, Object>> tempTableStatus = new ArrayList<>();
    private List<Map<String, Object>> tempTableVariables = new ArrayList<>();
    private List<Map<String, Object>> tempTableEvidence = new ArrayList<>();

    private List<Map<String, Object>> userVariableEvidence = new ArrayList<>();
    private List<Map<String, Object>> udfFunctions = new ArrayList<>();
    private List<Map<String, Object>> storedFunctions = new ArrayList<>();
    private List<Map<String, Object>> storedProcedures = new ArrayList<>();
    private List<Map<String, Object>> triggers = new ArrayList<>();
    private List<Map<String, Object>> events = new ArrayList<>();
    private List<Map<String, Object>> sequences = new ArrayList<>();
    private List<Map<String, Object>> spatialColumns = new ArrayList<>();
    private List<Map<String, Object>> spatialIndexes = new ArrayList<>();
    private List<Map<String, Object>> fulltextIndexes = new ArrayList<>();
    private List<Map<String, Object>> gisFunctionEvidence = new ArrayList<>();
    private List<Map<String, Object>> xmlFunctionEvidence = new ArrayList<>();

    private List<CompatibilityItem> compatibilityItems = new ArrayList<>();
    private List<String> queryWarnings = new ArrayList<>();

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getScannedSchema() {
        return scannedSchema;
    }

    public void setScannedSchema(String scannedSchema) {
        this.scannedSchema = scannedSchema;
    }

    public Map<String, String> getServerVariables() {
        return serverVariables;
    }

    public void setServerVariables(Map<String, String> serverVariables) {
        this.serverVariables = serverVariables;
    }

    public Map<String, String> getIsolationLevels() {
        return isolationLevels;
    }

    public void setIsolationLevels(Map<String, String> isolationLevels) {
        this.isolationLevels = isolationLevels;
    }

    public List<Map<String, Object>> getSchemaCharsets() {
        return schemaCharsets;
    }

    public void setSchemaCharsets(List<Map<String, Object>> schemaCharsets) {
        this.schemaCharsets = schemaCharsets;
    }

    public List<Map<String, Object>> getTableCollations() {
        return tableCollations;
    }

    public void setTableCollations(List<Map<String, Object>> tableCollations) {
        this.tableCollations = tableCollations;
    }

    public List<Map<String, Object>> getColumnCharsets() {
        return columnCharsets;
    }

    public void setColumnCharsets(List<Map<String, Object>> columnCharsets) {
        this.columnCharsets = columnCharsets;
    }

    public List<Map<String, Object>> getStorageEngines() {
        return storageEngines;
    }

    public void setStorageEngines(List<Map<String, Object>> storageEngines) {
        this.storageEngines = storageEngines;
    }

    public List<Map<String, Object>> getTableCharsetCollationDetails() {
        return tableCharsetCollationDetails;
    }

    public void setTableCharsetCollationDetails(List<Map<String, Object>> tableCharsetCollationDetails) {
        this.tableCharsetCollationDetails = tableCharsetCollationDetails;
    }

    public List<Map<String, Object>> getColumnCharsetCollationDetails() {
        return columnCharsetCollationDetails;
    }

    public void setColumnCharsetCollationDetails(List<Map<String, Object>> columnCharsetCollationDetails) {
        this.columnCharsetCollationDetails = columnCharsetCollationDetails;
    }

    public List<Map<String, Object>> getEngineTableDetails() {
        return engineTableDetails;
    }

    public void setEngineTableDetails(List<Map<String, Object>> engineTableDetails) {
        this.engineTableDetails = engineTableDetails;
    }

    public Map<String, String> getTableDefinitions() {
        return tableDefinitions;
    }

    public void setTableDefinitions(Map<String, String> tableDefinitions) {
        this.tableDefinitions = tableDefinitions;
    }

    public List<Map<String, Object>> getSqlModeRows() {
        return sqlModeRows;
    }

    public void setSqlModeRows(List<Map<String, Object>> sqlModeRows) {
        this.sqlModeRows = sqlModeRows;
    }

    public List<Map<String, Object>> getTempTableStatus() {
        return tempTableStatus;
    }

    public void setTempTableStatus(List<Map<String, Object>> tempTableStatus) {
        this.tempTableStatus = tempTableStatus;
    }

    public List<Map<String, Object>> getTempTableVariables() {
        return tempTableVariables;
    }

    public void setTempTableVariables(List<Map<String, Object>> tempTableVariables) {
        this.tempTableVariables = tempTableVariables;
    }

    public List<Map<String, Object>> getTempTableEvidence() {
        return tempTableEvidence;
    }

    public void setTempTableEvidence(List<Map<String, Object>> tempTableEvidence) {
        this.tempTableEvidence = tempTableEvidence;
    }

    public List<Map<String, Object>> getUserVariableEvidence() {
        return userVariableEvidence;
    }

    public void setUserVariableEvidence(List<Map<String, Object>> userVariableEvidence) {
        this.userVariableEvidence = userVariableEvidence;
    }

    public List<Map<String, Object>> getUdfFunctions() {
        return udfFunctions;
    }

    public void setUdfFunctions(List<Map<String, Object>> udfFunctions) {
        this.udfFunctions = udfFunctions;
    }

    public List<Map<String, Object>> getStoredFunctions() {
        return storedFunctions;
    }

    public void setStoredFunctions(List<Map<String, Object>> storedFunctions) {
        this.storedFunctions = storedFunctions;
    }

    public List<Map<String, Object>> getStoredProcedures() {
        return storedProcedures;
    }

    public void setStoredProcedures(List<Map<String, Object>> storedProcedures) {
        this.storedProcedures = storedProcedures;
    }

    public List<Map<String, Object>> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Map<String, Object>> triggers) {
        this.triggers = triggers;
    }

    public List<Map<String, Object>> getEvents() {
        return events;
    }

    public void setEvents(List<Map<String, Object>> events) {
        this.events = events;
    }

    public List<Map<String, Object>> getSequences() {
        return sequences;
    }

    public void setSequences(List<Map<String, Object>> sequences) {
        this.sequences = sequences;
    }

    public List<Map<String, Object>> getSpatialColumns() {
        return spatialColumns;
    }

    public void setSpatialColumns(List<Map<String, Object>> spatialColumns) {
        this.spatialColumns = spatialColumns;
    }

    public List<Map<String, Object>> getSpatialIndexes() {
        return spatialIndexes;
    }

    public void setSpatialIndexes(List<Map<String, Object>> spatialIndexes) {
        this.spatialIndexes = spatialIndexes;
    }

    public List<Map<String, Object>> getFulltextIndexes() {
        return fulltextIndexes;
    }

    public void setFulltextIndexes(List<Map<String, Object>> fulltextIndexes) {
        this.fulltextIndexes = fulltextIndexes;
    }

    public List<Map<String, Object>> getGisFunctionEvidence() {
        return gisFunctionEvidence;
    }

    public void setGisFunctionEvidence(List<Map<String, Object>> gisFunctionEvidence) {
        this.gisFunctionEvidence = gisFunctionEvidence;
    }

    public List<Map<String, Object>> getXmlFunctionEvidence() {
        return xmlFunctionEvidence;
    }

    public void setXmlFunctionEvidence(List<Map<String, Object>> xmlFunctionEvidence) {
        this.xmlFunctionEvidence = xmlFunctionEvidence;
    }

    public List<CompatibilityItem> getCompatibilityItems() {
        return compatibilityItems;
    }

    public void setCompatibilityItems(List<CompatibilityItem> compatibilityItems) {
        this.compatibilityItems = compatibilityItems;
    }

    public List<String> getQueryWarnings() {
        return queryWarnings;
    }

    public void setQueryWarnings(List<String> queryWarnings) {
        this.queryWarnings = queryWarnings;
    }
}
