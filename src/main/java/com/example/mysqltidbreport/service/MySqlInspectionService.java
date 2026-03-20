package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.InspectionResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class MySqlInspectionService {

    private static final String SQL_RESOURCE = "/sql/mysql-inspection.sql";
    private static final String SQL_MARKER = "-- name:";
    private static final Map<String, String> SQL_TEMPLATES = loadSqlTemplates();

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
                sql("schema_charsets", schema),
                schemaArg(schema)
        ));

        result.setTableCollations(queryList(
                result,
                "Table collations",
                sql("table_collations", schema),
                schemaArg(schema)
        ));

        result.setColumnCharsets(queryList(
                result,
                "Column charsets",
                sql("column_charsets", schema),
                schemaArg(schema)
        ));

        result.setStorageEngines(queryList(
                result,
                "Storage engines",
                sql("storage_engines", schema),
                schemaArg(schema)
        ));

        result.setTableCharsetCollationDetails(queryList(
                result,
                "Table charset collation details",
                sql("table_charset_collation_details", schema),
                schemaArg(schema)
        ));

        result.setColumnCharsetCollationDetails(queryList(
                result,
                "Column charset collation details",
                sql("column_charset_collation_details", schema),
                schemaArg(schema)
        ));

        result.setEngineTableDetails(queryList(
                result,
                "Engine table details",
                sql("engine_table_details", schema),
                schemaArg(schema)
        ));

        loadTableDefinitions(result);

        result.setSqlModeRows(queryList(
                result,
                "SQL mode",
                sql("sql_mode")
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
        serverVariables.put("version", queryScalar(result, "version", sql("version")));
        serverVariables.put("version_comment", queryScalar(result, "version_comment", sql("version_comment")));
        serverVariables.put("character_set_server", queryScalar(result, "character_set_server",
                sql("character_set_server")));
        serverVariables.put("collation_server", queryScalar(result, "collation_server",
                sql("collation_server")));
        serverVariables.put("lower_case_table_names", queryScalar(result, "lower_case_table_names",
                sql("lower_case_table_names")));
        result.setServerVariables(serverVariables);
    }

    private void loadIsolationLevels(InspectionResult result) {
        Map<String, String> isolation = new LinkedHashMap<>();
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql("isolation_transaction"));
            isolation.put("global", stringValue(row.get("global_isolation")));
            isolation.put("session", stringValue(row.get("session_isolation")));
        } catch (Exception ignore) {
            try {
                Map<String, Object> row = jdbcTemplate.queryForMap(sql("isolation_tx_fallback"));
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
        statusRows.addAll(queryList(result, "Temp status", sql("temp_status")));
        result.setTempTableStatus(statusRows);

        List<Map<String, Object>> tempVars = new ArrayList<>();
        tempVars.addAll(queryList(result, "tmp_table_size", sql("tmp_table_size")));
        tempVars.addAll(queryList(result, "max_heap_table_size", sql("max_heap_table_size")));
        result.setTempTableVariables(tempVars);

        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.addAll(queryList(
                result,
                "Routine temporary tables",
                sql("routine_temp_tables", schema),
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "Trigger temporary tables",
                sql("trigger_temp_tables", schema),
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "Event temporary tables",
                sql("event_temp_tables", schema),
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "View temporary tables",
                sql("view_temp_tables", schema),
                schemaArg(schema)
        ));
        result.setTempTableEvidence(evidence);
    }

    private void loadVariableUsageEvidence(InspectionResult result, String schema) {
        List<Map<String, Object>> evidence = new ArrayList<>();
        evidence.addAll(queryList(
                result,
                "User variables by thread",
                sql("user_variables_by_thread")
        ));
        evidence.addAll(queryList(
                result,
                "User variable in routine",
                sql("user_var_in_routine", schema),
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "User variable in trigger",
                sql("user_var_in_trigger", schema),
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "User variable in event",
                sql("user_var_in_event", schema),
                schemaArg(schema)
        ));
        evidence.addAll(queryList(
                result,
                "User variable in view",
                sql("user_var_in_view", schema),
                schemaArg(schema)
        ));
        result.setUserVariableEvidence(evidence);
    }

    private void loadRoutinesAndProgrammability(InspectionResult result, String schema) {
        result.setStoredFunctions(queryList(
                result,
                "Stored functions",
                sql("stored_functions", schema),
                schemaArg(schema)
        ));

        result.setStoredProcedures(queryList(
                result,
                "Stored procedures",
                sql("stored_procedures", schema),
                schemaArg(schema)
        ));

        result.setUdfFunctions(queryList(
                result,
                "UDF functions",
                sql("udf_functions")
        ));

        result.setTriggers(queryList(
                result,
                "Triggers",
                sql("triggers", schema),
                schemaArg(schema)
        ));

        result.setEvents(queryList(
                result,
                "Events",
                sql("events", schema),
                schemaArg(schema)
        ));
    }

    private void loadSequences(InspectionResult result, String schema) {
        List<Map<String, Object>> rows = safeQueryForList(
                result,
                "Sequences",
                sql("sequences", schema),
                schemaArg(schema)
        );
        if (!rows.isEmpty()) {
            result.setSequences(rows);
            return;
        }

        result.setSequences(safeQueryForList(
                result,
                "Sequence tables",
                sql("sequence_tables_fallback", schema),
                schemaArg(schema)
        ));
    }

    private void loadAdvancedFeatureEvidence(InspectionResult result, String schema) {
        result.setSpatialColumns(queryList(
                result,
                "Spatial columns",
                sql("spatial_columns", schema),
                schemaArg(schema)
        ));

        result.setSpatialIndexes(queryList(
                result,
                "Spatial indexes",
                sql("spatial_indexes", schema),
                schemaArg(schema)
        ));

        result.setFulltextIndexes(queryList(
                result,
                "Fulltext indexes",
                sql("fulltext_indexes", schema),
                schemaArg(schema)
        ));

        List<Map<String, Object>> gisEvidence = new ArrayList<>();
        gisEvidence.addAll(queryList(
                result,
                "GIS function in routines",
                sql("gis_function_in_routines", schema, Map.of("{{GIS_FUNCTION_REGEX}}", GIS_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        gisEvidence.addAll(queryList(
                result,
                "GIS function in triggers",
                sql("gis_function_in_triggers", schema, Map.of("{{GIS_FUNCTION_REGEX}}", GIS_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        gisEvidence.addAll(queryList(
                result,
                "GIS function in events",
                sql("gis_function_in_events", schema, Map.of("{{GIS_FUNCTION_REGEX}}", GIS_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        gisEvidence.addAll(queryList(
                result,
                "GIS function in views",
                sql("gis_function_in_views", schema, Map.of("{{GIS_FUNCTION_REGEX}}", GIS_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        result.setGisFunctionEvidence(gisEvidence);

        List<Map<String, Object>> xmlEvidence = new ArrayList<>();
        xmlEvidence.addAll(queryList(
                result,
                "XML function in routines",
                sql("xml_function_in_routines", schema, Map.of("{{XML_FUNCTION_REGEX}}", XML_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        xmlEvidence.addAll(queryList(
                result,
                "XML function in triggers",
                sql("xml_function_in_triggers", schema, Map.of("{{XML_FUNCTION_REGEX}}", XML_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        xmlEvidence.addAll(queryList(
                result,
                "XML function in events",
                sql("xml_function_in_events", schema, Map.of("{{XML_FUNCTION_REGEX}}", XML_FUNCTION_REGEX)),
                schemaArg(schema)
        ));
        xmlEvidence.addAll(queryList(
                result,
                "XML function in views",
                sql("xml_function_in_views", schema, Map.of("{{XML_FUNCTION_REGEX}}", XML_FUNCTION_REGEX)),
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
                String sql = sql("show_create_table", null, Map.of(
                        "{{SCHEMA_NAME}}", escapeIdentifier(schemaName),
                        "{{TABLE_NAME}}", escapeIdentifier(tableName)
                ));
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

    private String sql(String key) {
        return sql(key, null, Map.of());
    }

    private String sql(String key, String schema) {
        return sql(key, schema, Map.of());
    }

    private String sql(String key, String schema, Map<String, String> replacements) {
        String template = SQL_TEMPLATES.get(key);
        if (template == null) {
            throw new IllegalArgumentException("SQL template not found: " + key);
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("{{SCHEMA_PREDICATE}}", schemaPredicate(schema));
        if (replacements != null && !replacements.isEmpty()) {
            vars.putAll(replacements);
        }

        String rendered = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String value = entry.getValue();
            if (value == null) {
                continue;
            }
            rendered = rendered.replace(entry.getKey(), value);
        }
        return rendered;
    }

    private static Map<String, String> loadSqlTemplates() {
        try (InputStream in = MySqlInspectionService.class.getResourceAsStream(SQL_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("SQL resource not found: " + SQL_RESOURCE);
            }

            Map<String, String> templates = new LinkedHashMap<>();
            Set<String> duplicated = new LinkedHashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String currentName = null;
                StringBuilder currentSql = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith(SQL_MARKER)) {
                        if (currentName != null) {
                            putSqlTemplate(templates, duplicated, currentName, currentSql.toString());
                        }
                        currentName = line.substring(SQL_MARKER.length()).trim();
                        currentSql.setLength(0);
                        continue;
                    }
                    if (currentName == null) {
                        continue;
                    }
                    currentSql.append(line).append('\n');
                }
                if (currentName != null) {
                    putSqlTemplate(templates, duplicated, currentName, currentSql.toString());
                }
            }

            if (!duplicated.isEmpty()) {
                throw new IllegalStateException("Duplicated SQL template name(s): " + String.join(", ", duplicated));
            }
            if (templates.isEmpty()) {
                throw new IllegalStateException("No SQL templates loaded from " + SQL_RESOURCE);
            }
            return templates;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load SQL templates from " + SQL_RESOURCE, e);
        }
    }

    private static void putSqlTemplate(Map<String, String> templates, Set<String> duplicated,
                                       String name, String sql) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            return;
        }

        String normalizedSql = sql == null ? "" : sql.trim();
        if (normalizedSql.isEmpty()) {
            return;
        }

        if (templates.containsKey(normalizedName)) {
            duplicated.add(normalizedName);
            return;
        }
        templates.put(normalizedName, normalizedSql);
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
