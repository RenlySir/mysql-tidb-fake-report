package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.InspectionResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MySqlInspectionService {

    private static final String NON_SYSTEM_SCHEMA_FILTER =
            " NOT IN ('information_schema','mysql','performance_schema','sys') ";
    private static final String GIS_FUNCTION_REGEX =
            "'ST_[A-Z0-9_]*\\\\(|GEOMFROMTEXT\\\\(|GEOMETRYFROMTEXT\\\\(|POINTFROMTEXT\\\\('";
    private static final String XML_FUNCTION_REGEX =
            "'EXTRACTVALUE\\\\(|UPDATEXML\\\\('";

    private final JdbcTemplate jdbcTemplate;

    public MySqlInspectionService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InspectionResult inspect(String schema) {
        InspectionResult result = new InspectionResult();
        result.setGeneratedAt(LocalDateTime.now());
        result.setScannedSchema(StringUtils.hasText(schema) ? schema : "ALL_NON_SYSTEM_SCHEMAS");

        loadServerVariables(result);
        loadIsolationLevels(result);

        result.setSchemaCharsets(queryList(
                result,
                "Schema charsets",
                "SELECT DEFAULT_CHARACTER_SET_NAME AS charset_name, " +
                        "DEFAULT_COLLATION_NAME AS collation_name, COUNT(*) AS schema_count " +
                        "FROM information_schema.SCHEMATA " +
                        "WHERE SCHEMA_NAME " + schemaPredicate(schema) +
                        "GROUP BY DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME " +
                        "ORDER BY schema_count DESC",
                schemaArg(schema)
        ));

        result.setTableCollations(queryList(
                result,
                "Table collations",
                "SELECT TABLE_COLLATION AS collation_name, COUNT(*) AS table_count " +
                        "FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) + "AND TABLE_TYPE='BASE TABLE' " +
                        "GROUP BY TABLE_COLLATION ORDER BY table_count DESC",
                schemaArg(schema)
        ));

        result.setColumnCharsets(queryList(
                result,
                "Column charsets",
                "SELECT CHARACTER_SET_NAME AS charset_name, COLLATION_NAME AS collation_name, " +
                        "COUNT(*) AS column_count " +
                        "FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND CHARACTER_SET_NAME IS NOT NULL " +
                        "GROUP BY CHARACTER_SET_NAME, COLLATION_NAME " +
                        "ORDER BY column_count DESC",
                schemaArg(schema)
        ));

        result.setStorageEngines(queryList(
                result,
                "Storage engines",
                "SELECT ENGINE AS engine_name, COUNT(*) AS table_count " +
                        "FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) + "AND TABLE_TYPE='BASE TABLE' " +
                        "GROUP BY ENGINE ORDER BY table_count DESC",
                schemaArg(schema)
        ));

        result.setTableCharsetCollationDetails(queryList(
                result,
                "Table charset collation details",
                "SELECT t.TABLE_SCHEMA AS schema_name, t.TABLE_NAME AS table_name, " +
                        "t.TABLE_COLLATION AS table_collation, cca.CHARACTER_SET_NAME AS table_charset " +
                        "FROM information_schema.TABLES t " +
                        "LEFT JOIN information_schema.COLLATION_CHARACTER_SET_APPLICABILITY cca " +
                        "ON t.TABLE_COLLATION = cca.COLLATION_NAME " +
                        "WHERE t.TABLE_SCHEMA " + schemaPredicate(schema) + "AND t.TABLE_TYPE='BASE TABLE' " +
                        "ORDER BY t.TABLE_SCHEMA, t.TABLE_NAME",
                schemaArg(schema)
        ));

        result.setColumnCharsetCollationDetails(queryList(
                result,
                "Column charset collation details",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS table_name, COLUMN_NAME AS column_name, " +
                        "CHARACTER_SET_NAME AS charset_name, COLLATION_NAME AS collation_name, " +
                        "COLUMN_TYPE AS column_type " +
                        "FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND CHARACTER_SET_NAME IS NOT NULL " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION",
                schemaArg(schema)
        ));

        result.setEngineTableDetails(queryList(
                result,
                "Engine table details",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS table_name, ENGINE AS engine_name " +
                        "FROM information_schema.TABLES " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) + "AND TABLE_TYPE='BASE TABLE' " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME",
                schemaArg(schema)
        ));

        loadTableDefinitions(result);

        result.setSqlModeRows(queryList(
                result,
                "SQL mode",
                "SELECT @@GLOBAL.sql_mode AS global_sql_mode, @@SESSION.sql_mode AS session_sql_mode"
        ));

        loadTempTableDetails(result, schema);
        loadVariableUsageEvidence(result, schema);
        loadRoutinesAndProgrammability(result, schema);
        loadSequences(result, schema);
        loadAdvancedFeatureEvidence(result, schema);

        return result;
    }

    private void loadServerVariables(InspectionResult result) {
        Map<String, String> serverVariables = new LinkedHashMap<>();
        serverVariables.put("version", queryScalar(result, "version", "SELECT @@version"));
        serverVariables.put("version_comment", queryScalar(result, "version_comment", "SELECT @@version_comment"));
        serverVariables.put("character_set_server", queryScalar(result, "character_set_server",
                "SELECT @@character_set_server"));
        serverVariables.put("collation_server", queryScalar(result, "collation_server",
                "SELECT @@collation_server"));
        serverVariables.put("lower_case_table_names", queryScalar(result, "lower_case_table_names",
                "SELECT @@lower_case_table_names"));
        result.setServerVariables(serverVariables);
    }

    private void loadIsolationLevels(InspectionResult result) {
        Map<String, String> isolation = new LinkedHashMap<>();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    "SELECT @@GLOBAL.transaction_isolation AS global_isolation, " +
                            "@@SESSION.transaction_isolation AS session_isolation"
            );
            isolation.put("global", stringValue(row.get("global_isolation")));
            isolation.put("session", stringValue(row.get("session_isolation")));
        } catch (Exception ignore) {
            try {
                Map<String, Object> row = jdbcTemplate.queryForMap(
                        "SELECT @@GLOBAL.tx_isolation AS global_isolation, @@SESSION.tx_isolation AS session_isolation"
                );
                isolation.put("global", stringValue(row.get("global_isolation")));
                isolation.put("session", stringValue(row.get("session_isolation")));
            } catch (Exception e) {
                result.getQueryWarnings().add("Isolation level query failed: " + e.getMessage());
                isolation.put("global", "N/A");
                isolation.put("session", "N/A");
            }
        }
        result.setIsolationLevels(isolation);
    }

    private void loadTempTableDetails(InspectionResult result, String schema) {
        List<Map<String, Object>> statusRows = new ArrayList<>();
        statusRows.addAll(queryList(result, "Temp status", "SHOW GLOBAL STATUS LIKE 'Created_tmp%'"));
        result.setTempTableStatus(statusRows);

        List<Map<String, Object>> tempVars = new ArrayList<>();
        tempVars.addAll(queryList(result, "tmp_table_size",
                "SHOW GLOBAL VARIABLES LIKE 'tmp_table_size'"));
        tempVars.addAll(queryList(result, "max_heap_table_size",
                "SHOW GLOBAL VARIABLES LIKE 'max_heap_table_size'"));
        result.setTempTableVariables(tempVars);

        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.addAll(queryList(
                result,
                "Routine temporary tables",
                "SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS object_name, " +
                        "ROUTINE_TYPE AS object_type, 'ROUTINE_DEFINITION' AS source " +
                        "FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA " + schemaPredicate(schema) +
                        "AND ROUTINE_DEFINITION LIKE '%TEMPORARY TABLE%' LIMIT 100",
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "Trigger temporary tables",
                "SELECT TRIGGER_SCHEMA AS schema_name, TRIGGER_NAME AS object_name, " +
                        "'TRIGGER' AS object_type, 'ACTION_STATEMENT' AS source " +
                        "FROM information_schema.TRIGGERS " +
                        "WHERE TRIGGER_SCHEMA " + schemaPredicate(schema) +
                        "AND ACTION_STATEMENT LIKE '%TEMPORARY TABLE%' LIMIT 100",
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "Event temporary tables",
                "SELECT EVENT_SCHEMA AS schema_name, EVENT_NAME AS object_name, " +
                        "'EVENT' AS object_type, 'EVENT_DEFINITION' AS source " +
                        "FROM information_schema.EVENTS " +
                        "WHERE EVENT_SCHEMA " + schemaPredicate(schema) +
                        "AND EVENT_DEFINITION LIKE '%TEMPORARY TABLE%' LIMIT 100",
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "View temporary tables",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS object_name, " +
                        "'VIEW' AS object_type, 'VIEW_DEFINITION' AS source " +
                        "FROM information_schema.VIEWS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND VIEW_DEFINITION LIKE '%TEMPORARY TABLE%' LIMIT 100",
                schemaArg(schema)
        ));
        result.setTempTableEvidence(evidence);
    }

    private void loadVariableUsageEvidence(InspectionResult result, String schema) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.addAll(queryList(
                result,
                "User variables by thread",
                "SELECT VARIABLE_NAME AS object_name, VARIABLE_VALUE AS detail, 'ACTIVE_SESSION' AS source " +
                        "FROM performance_schema.user_variables_by_thread LIMIT 100"
        ));
        evidence.addAll(queryList(
                result,
                "User variable in routine",
                "SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS object_name, " +
                        "CONCAT('type=', ROUTINE_TYPE) AS detail, 'ROUTINE_DEFINITION' AS source " +
                        "FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA " + schemaPredicate(schema) +
                        "AND ROUTINE_DEFINITION LIKE '%@%' LIMIT 100",
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "User variable in trigger",
                "SELECT TRIGGER_SCHEMA AS schema_name, TRIGGER_NAME AS object_name, " +
                        "'TRIGGER' AS detail, 'ACTION_STATEMENT' AS source " +
                        "FROM information_schema.TRIGGERS " +
                        "WHERE TRIGGER_SCHEMA " + schemaPredicate(schema) +
                        "AND ACTION_STATEMENT LIKE '%@%' LIMIT 100",
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "User variable in event",
                "SELECT EVENT_SCHEMA AS schema_name, EVENT_NAME AS object_name, " +
                        "'EVENT' AS detail, 'EVENT_DEFINITION' AS source " +
                        "FROM information_schema.EVENTS " +
                        "WHERE EVENT_SCHEMA " + schemaPredicate(schema) +
                        "AND EVENT_DEFINITION LIKE '%@%' LIMIT 100",
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "User variable in view",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS object_name, " +
                        "'VIEW' AS detail, 'VIEW_DEFINITION' AS source " +
                        "FROM information_schema.VIEWS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND VIEW_DEFINITION LIKE '%@%' LIMIT 100",
                schemaArg(schema)
        ));
        result.setUserVariableEvidence(evidence);
    }

    private void loadRoutinesAndProgrammability(InspectionResult result, String schema) {
        result.setStoredFunctions(queryList(
                result,
                "Stored functions",
                "SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS routine_name " +
                        "FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA " + schemaPredicate(schema) +
                        "AND ROUTINE_TYPE='FUNCTION' ORDER BY ROUTINE_SCHEMA, ROUTINE_NAME",
                schemaArg(schema)
        ));

        result.setStoredProcedures(queryList(
                result,
                "Stored procedures",
                "SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS routine_name " +
                        "FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA " + schemaPredicate(schema) +
                        "AND ROUTINE_TYPE='PROCEDURE' ORDER BY ROUTINE_SCHEMA, ROUTINE_NAME",
                schemaArg(schema)
        ));

        result.setUdfFunctions(queryList(
                result,
                "UDF functions",
                "SELECT name AS function_name, dl AS shared_library, type AS function_type FROM mysql.func"
        ));

        result.setTriggers(queryList(
                result,
                "Triggers",
                "SELECT TRIGGER_SCHEMA AS schema_name, TRIGGER_NAME AS trigger_name " +
                        "FROM information_schema.TRIGGERS " +
                        "WHERE TRIGGER_SCHEMA " + schemaPredicate(schema) +
                        "ORDER BY TRIGGER_SCHEMA, TRIGGER_NAME",
                schemaArg(schema)
        ));

        result.setEvents(queryList(
                result,
                "Events",
                "SELECT EVENT_SCHEMA AS schema_name, EVENT_NAME AS event_name " +
                        "FROM information_schema.EVENTS " +
                        "WHERE EVENT_SCHEMA " + schemaPredicate(schema) +
                        "ORDER BY EVENT_SCHEMA, EVENT_NAME",
                schemaArg(schema)
        ));
    }

    private void loadSequences(InspectionResult result, String schema) {
        String sequenceSql = "SELECT SEQUENCE_SCHEMA AS schema_name, " +
                "SEQUENCE_NAME AS sequence_name, DATA_TYPE AS data_type, " +
                "START_VALUE AS start_value, MINIMUM_VALUE AS min_value, MAXIMUM_VALUE AS max_value, " +
                "`INCREMENT` AS increment_value, CYCLE_OPTION AS cycle_option " +
                "FROM information_schema.SEQUENCES " +
                "WHERE SEQUENCE_SCHEMA " + schemaPredicate(schema) +
                "ORDER BY SEQUENCE_SCHEMA, SEQUENCE_NAME";

        List<Map<String, Object>> rows = safeQueryForList(result, "Sequences", sequenceSql, schemaArg(schema));
        if (!rows.isEmpty()) {
            result.setSequences(rows);
            return;
        }

        String fallbackSql = "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS sequence_name, " +
                "NULL AS data_type, NULL AS start_value, NULL AS min_value, NULL AS max_value, " +
                "NULL AS increment_value, NULL AS cycle_option " +
                "FROM information_schema.TABLES " +
                "WHERE TABLE_SCHEMA " + schemaPredicate(schema) + "AND TABLE_TYPE='SEQUENCE' " +
                "ORDER BY TABLE_SCHEMA, TABLE_NAME";

        result.setSequences(safeQueryForList(result, "Sequence tables", fallbackSql, schemaArg(schema)));
    }

    private void loadAdvancedFeatureEvidence(InspectionResult result, String schema) {
        result.setSpatialColumns(queryList(
                result,
                "Spatial columns",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS table_name, COLUMN_NAME AS column_name, " +
                        "DATA_TYPE AS data_type, COLUMN_TYPE AS column_type " +
                        "FROM information_schema.COLUMNS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND DATA_TYPE IN ('geometry','point','linestring','polygon','multipoint'," +
                        "'multilinestring','multipolygon','geometrycollection') " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION",
                schemaArg(schema)
        ));

        result.setSpatialIndexes(queryList(
                result,
                "Spatial indexes",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS table_name, INDEX_NAME AS index_name, " +
                        "INDEX_TYPE AS index_type, COLUMN_NAME AS column_name, SEQ_IN_INDEX AS seq_in_index " +
                        "FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) + "AND INDEX_TYPE='SPATIAL' " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX",
                schemaArg(schema)
        ));

        result.setFulltextIndexes(queryList(
                result,
                "Fulltext indexes",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS table_name, INDEX_NAME AS index_name, " +
                        "INDEX_TYPE AS index_type, COLUMN_NAME AS column_name, SEQ_IN_INDEX AS seq_in_index " +
                        "FROM information_schema.STATISTICS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) + "AND INDEX_TYPE='FULLTEXT' " +
                        "ORDER BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX",
                schemaArg(schema)
        ));

        List<Map<String, Object>> gisEvidence = new ArrayList<>();
        gisEvidence.addAll(queryList(
                result,
                "GIS function in routines",
                "SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS object_name, " +
                        "ROUTINE_TYPE AS object_type, 'ROUTINE_DEFINITION' AS source, 'GIS_FUNCTION' AS detail " +
                        "FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA " + schemaPredicate(schema) +
                        "AND ROUTINE_DEFINITION IS NOT NULL " +
                        "AND UPPER(ROUTINE_DEFINITION) REGEXP " + GIS_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        gisEvidence.addAll(queryList(
                result,
                "GIS function in triggers",
                "SELECT TRIGGER_SCHEMA AS schema_name, TRIGGER_NAME AS object_name, " +
                        "'TRIGGER' AS object_type, 'ACTION_STATEMENT' AS source, 'GIS_FUNCTION' AS detail " +
                        "FROM information_schema.TRIGGERS " +
                        "WHERE TRIGGER_SCHEMA " + schemaPredicate(schema) +
                        "AND ACTION_STATEMENT IS NOT NULL " +
                        "AND UPPER(ACTION_STATEMENT) REGEXP " + GIS_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        gisEvidence.addAll(queryList(
                result,
                "GIS function in events",
                "SELECT EVENT_SCHEMA AS schema_name, EVENT_NAME AS object_name, " +
                        "'EVENT' AS object_type, 'EVENT_DEFINITION' AS source, 'GIS_FUNCTION' AS detail " +
                        "FROM information_schema.EVENTS " +
                        "WHERE EVENT_SCHEMA " + schemaPredicate(schema) +
                        "AND EVENT_DEFINITION IS NOT NULL " +
                        "AND UPPER(EVENT_DEFINITION) REGEXP " + GIS_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        gisEvidence.addAll(queryList(
                result,
                "GIS function in views",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS object_name, " +
                        "'VIEW' AS object_type, 'VIEW_DEFINITION' AS source, 'GIS_FUNCTION' AS detail " +
                        "FROM information_schema.VIEWS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND VIEW_DEFINITION IS NOT NULL " +
                        "AND UPPER(VIEW_DEFINITION) REGEXP " + GIS_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        result.setGisFunctionEvidence(gisEvidence);

        List<Map<String, Object>> xmlEvidence = new ArrayList<>();
        xmlEvidence.addAll(queryList(
                result,
                "XML function in routines",
                "SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS object_name, " +
                        "ROUTINE_TYPE AS object_type, 'ROUTINE_DEFINITION' AS source, 'XML_FUNCTION' AS detail " +
                        "FROM information_schema.ROUTINES " +
                        "WHERE ROUTINE_SCHEMA " + schemaPredicate(schema) +
                        "AND ROUTINE_DEFINITION IS NOT NULL " +
                        "AND UPPER(ROUTINE_DEFINITION) REGEXP " + XML_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        xmlEvidence.addAll(queryList(
                result,
                "XML function in triggers",
                "SELECT TRIGGER_SCHEMA AS schema_name, TRIGGER_NAME AS object_name, " +
                        "'TRIGGER' AS object_type, 'ACTION_STATEMENT' AS source, 'XML_FUNCTION' AS detail " +
                        "FROM information_schema.TRIGGERS " +
                        "WHERE TRIGGER_SCHEMA " + schemaPredicate(schema) +
                        "AND ACTION_STATEMENT IS NOT NULL " +
                        "AND UPPER(ACTION_STATEMENT) REGEXP " + XML_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        xmlEvidence.addAll(queryList(
                result,
                "XML function in events",
                "SELECT EVENT_SCHEMA AS schema_name, EVENT_NAME AS object_name, " +
                        "'EVENT' AS object_type, 'EVENT_DEFINITION' AS source, 'XML_FUNCTION' AS detail " +
                        "FROM information_schema.EVENTS " +
                        "WHERE EVENT_SCHEMA " + schemaPredicate(schema) +
                        "AND EVENT_DEFINITION IS NOT NULL " +
                        "AND UPPER(EVENT_DEFINITION) REGEXP " + XML_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        xmlEvidence.addAll(queryList(
                result,
                "XML function in views",
                "SELECT TABLE_SCHEMA AS schema_name, TABLE_NAME AS object_name, " +
                        "'VIEW' AS object_type, 'VIEW_DEFINITION' AS source, 'XML_FUNCTION' AS detail " +
                        "FROM information_schema.VIEWS " +
                        "WHERE TABLE_SCHEMA " + schemaPredicate(schema) +
                        "AND VIEW_DEFINITION IS NOT NULL " +
                        "AND UPPER(VIEW_DEFINITION) REGEXP " + XML_FUNCTION_REGEX + " LIMIT 200",
                schemaArg(schema)
        ));
        result.setXmlFunctionEvidence(xmlEvidence);
    }

    private void loadTableDefinitions(InspectionResult result) {
        Map<String, String> definitions = new LinkedHashMap<>();
        for (Map<String, Object> row : result.getTableCharsetCollationDetails()) {
            String schemaName = stringValue(row.get("schema_name"));
            String tableName = stringValue(row.get("table_name"));
            String tableKey = tableKey(schemaName, tableName);

            if ("NULL".equalsIgnoreCase(schemaName) || "NULL".equalsIgnoreCase(tableName)) {
                continue;
            }
            if (definitions.containsKey(tableKey)) {
                continue;
            }

            try {
                String sql = "SHOW CREATE TABLE `" + escapeIdentifier(schemaName) + "`.`" +
                        escapeIdentifier(tableName) + "`";
                Map<String, Object> ddlRow = jdbcTemplate.queryForMap(sql);
                String ddl = extractCreateTableSql(ddlRow);
                if (ddl != null) {
                    definitions.put(tableKey, ddl);
                }
            } catch (Exception e) {
                result.getQueryWarnings().add(
                        "SHOW CREATE TABLE failed for " + tableKey + ": " + e.getMessage());
            }
        }
        result.setTableDefinitions(definitions);
    }

    private List<Map<String, Object>> queryList(InspectionResult result, String label, String sql, Object... args) {
        try {
            if (args == null || args.length == 0) {
                return jdbcTemplate.queryForList(sql);
            }
            return jdbcTemplate.queryForList(sql, args);
        } catch (Exception e) {
            result.getQueryWarnings().add(label + " query failed: " + e.getMessage());
            return List.of();
        }
    }

    private List<Map<String, Object>> safeQueryForList(InspectionResult result, String label, String sql,
                                                       Object... args) {
        try {
            if (args == null || args.length == 0) {
                return jdbcTemplate.queryForList(sql);
            }
            return jdbcTemplate.queryForList(sql, args);
        } catch (Exception e) {
            if (!isMissingMetadataError(e)) {
                result.getQueryWarnings().add(label + " query failed: " + e.getMessage());
            }
            return List.of();
        }
    }

    private String queryScalar(InspectionResult result, String label, String sql, Object... args) {
        try {
            Object value;
            if (args == null || args.length == 0) {
                value = jdbcTemplate.queryForObject(sql, Object.class);
            } else {
                value = jdbcTemplate.queryForObject(sql, Object.class, args);
            }
            return stringValue(value);
        } catch (Exception e) {
            result.getQueryWarnings().add(label + " query failed: " + e.getMessage());
            return "N/A";
        }
    }

    private String schemaPredicate(String schema) {
        if (StringUtils.hasText(schema)) {
            return " = ? ";
        }
        return NON_SYSTEM_SCHEMA_FILTER;
    }

    private Object[] schemaArg(String schema) {
        if (StringUtils.hasText(schema)) {
            return new Object[]{schema};
        }
        return new Object[]{};
    }

    private String stringValue(Object value) {
        return value == null ? "NULL" : String.valueOf(value);
    }

    private String extractCreateTableSql(Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        Object direct = row.get("Create Table");
        if (direct != null) {
            return String.valueOf(direct);
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.toLowerCase().contains("create table")) {
                return entry.getValue() == null ? null : String.valueOf(entry.getValue());
            }
        }
        return null;
    }

    private String tableKey(String schemaName, String tableName) {
        return schemaName + "." + tableName;
    }

    private String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }

    private boolean isMissingMetadataError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("information_schema.sequences")
                || lower.contains("unknown table")
                || lower.contains("unknown column")
                || lower.contains("doesn't exist")
                || lower.contains("not exist");
    }
}
