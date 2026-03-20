-- name: schema_charsets
SELECT DEFAULT_CHARACTER_SET_NAME AS charset_name,
       DEFAULT_COLLATION_NAME AS collation_name,
       COUNT(*) AS schema_count
FROM information_schema.SCHEMATA
WHERE SCHEMA_NAME {{SCHEMA_PREDICATE}}
GROUP BY DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
ORDER BY schema_count DESC

-- name: table_collations
SELECT TABLE_COLLATION AS collation_name, COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND TABLE_TYPE='BASE TABLE'
GROUP BY TABLE_COLLATION
ORDER BY table_count DESC

-- name: column_charsets
SELECT CHARACTER_SET_NAME AS charset_name,
       COLLATION_NAME AS collation_name,
       COUNT(*) AS column_count
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND CHARACTER_SET_NAME IS NOT NULL
GROUP BY CHARACTER_SET_NAME, COLLATION_NAME
ORDER BY column_count DESC

-- name: storage_engines
SELECT ENGINE AS engine_name, COUNT(*) AS table_count
FROM information_schema.TABLES
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND TABLE_TYPE='BASE TABLE'
GROUP BY ENGINE
ORDER BY table_count DESC

-- name: table_charset_collation_details
SELECT t.TABLE_SCHEMA AS schema_name,
       t.TABLE_NAME AS table_name,
       t.TABLE_COLLATION AS table_collation,
       cca.CHARACTER_SET_NAME AS table_charset
FROM information_schema.TABLES t
LEFT JOIN information_schema.COLLATION_CHARACTER_SET_APPLICABILITY cca
       ON t.TABLE_COLLATION = cca.COLLATION_NAME
WHERE t.TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND t.TABLE_TYPE='BASE TABLE'
ORDER BY t.TABLE_SCHEMA, t.TABLE_NAME

-- name: column_charset_collation_details
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS table_name,
       COLUMN_NAME AS column_name,
       CHARACTER_SET_NAME AS charset_name,
       COLLATION_NAME AS collation_name,
       COLUMN_TYPE AS column_type
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND CHARACTER_SET_NAME IS NOT NULL
ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION

-- name: engine_table_details
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS table_name,
       ENGINE AS engine_name
FROM information_schema.TABLES
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND TABLE_TYPE='BASE TABLE'
ORDER BY TABLE_SCHEMA, TABLE_NAME

-- name: sql_mode
SELECT @@GLOBAL.sql_mode AS global_sql_mode, @@SESSION.sql_mode AS session_sql_mode

-- name: version
SELECT @@version

-- name: version_comment
SELECT @@version_comment

-- name: character_set_server
SELECT @@character_set_server

-- name: collation_server
SELECT @@collation_server

-- name: lower_case_table_names
SELECT @@lower_case_table_names

-- name: isolation_transaction
SELECT @@GLOBAL.transaction_isolation AS global_isolation,
       @@SESSION.transaction_isolation AS session_isolation

-- name: isolation_tx_fallback
SELECT @@GLOBAL.tx_isolation AS global_isolation,
       @@SESSION.tx_isolation AS session_isolation

-- name: temp_status
SHOW GLOBAL STATUS LIKE 'Created_tmp%'

-- name: tmp_table_size
SHOW GLOBAL VARIABLES LIKE 'tmp_table_size'

-- name: max_heap_table_size
SHOW GLOBAL VARIABLES LIKE 'max_heap_table_size'

-- name: routine_temp_tables
SELECT ROUTINE_SCHEMA AS schema_name,
       ROUTINE_NAME AS object_name,
       ROUTINE_TYPE AS object_type,
       'ROUTINE_DEFINITION' AS source
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA {{SCHEMA_PREDICATE}}
  AND ROUTINE_DEFINITION LIKE '%TEMPORARY TABLE%'
LIMIT 100

-- name: trigger_temp_tables
SELECT TRIGGER_SCHEMA AS schema_name,
       TRIGGER_NAME AS object_name,
       'TRIGGER' AS object_type,
       'ACTION_STATEMENT' AS source
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA {{SCHEMA_PREDICATE}}
  AND ACTION_STATEMENT LIKE '%TEMPORARY TABLE%'
LIMIT 100

-- name: event_temp_tables
SELECT EVENT_SCHEMA AS schema_name,
       EVENT_NAME AS object_name,
       'EVENT' AS object_type,
       'EVENT_DEFINITION' AS source
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA {{SCHEMA_PREDICATE}}
  AND EVENT_DEFINITION LIKE '%TEMPORARY TABLE%'
LIMIT 100

-- name: view_temp_tables
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS object_name,
       'VIEW' AS object_type,
       'VIEW_DEFINITION' AS source
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND VIEW_DEFINITION LIKE '%TEMPORARY TABLE%'
LIMIT 100

-- name: user_variables_by_thread
SELECT VARIABLE_NAME AS object_name,
       VARIABLE_VALUE AS detail,
       'ACTIVE_SESSION' AS source
FROM performance_schema.user_variables_by_thread
LIMIT 100

-- name: user_var_in_routine
SELECT ROUTINE_SCHEMA AS schema_name,
       ROUTINE_NAME AS object_name,
       CONCAT('type=', ROUTINE_TYPE) AS detail,
       'ROUTINE_DEFINITION' AS source
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA {{SCHEMA_PREDICATE}}
  AND ROUTINE_DEFINITION LIKE '%@%'
LIMIT 100

-- name: user_var_in_trigger
SELECT TRIGGER_SCHEMA AS schema_name,
       TRIGGER_NAME AS object_name,
       'TRIGGER' AS detail,
       'ACTION_STATEMENT' AS source
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA {{SCHEMA_PREDICATE}}
  AND ACTION_STATEMENT LIKE '%@%'
LIMIT 100

-- name: user_var_in_event
SELECT EVENT_SCHEMA AS schema_name,
       EVENT_NAME AS object_name,
       'EVENT' AS detail,
       'EVENT_DEFINITION' AS source
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA {{SCHEMA_PREDICATE}}
  AND EVENT_DEFINITION LIKE '%@%'
LIMIT 100

-- name: user_var_in_view
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS object_name,
       'VIEW' AS detail,
       'VIEW_DEFINITION' AS source
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND VIEW_DEFINITION LIKE '%@%'
LIMIT 100

-- name: stored_functions
SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS routine_name
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA {{SCHEMA_PREDICATE}}
  AND ROUTINE_TYPE='FUNCTION'
ORDER BY ROUTINE_SCHEMA, ROUTINE_NAME

-- name: stored_procedures
SELECT ROUTINE_SCHEMA AS schema_name, ROUTINE_NAME AS routine_name
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA {{SCHEMA_PREDICATE}}
  AND ROUTINE_TYPE='PROCEDURE'
ORDER BY ROUTINE_SCHEMA, ROUTINE_NAME

-- name: udf_functions
SELECT name AS function_name, dl AS shared_library, type AS function_type
FROM mysql.func

-- name: triggers
SELECT TRIGGER_SCHEMA AS schema_name, TRIGGER_NAME AS trigger_name
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA {{SCHEMA_PREDICATE}}
ORDER BY TRIGGER_SCHEMA, TRIGGER_NAME

-- name: events
SELECT EVENT_SCHEMA AS schema_name, EVENT_NAME AS event_name
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA {{SCHEMA_PREDICATE}}
ORDER BY EVENT_SCHEMA, EVENT_NAME

-- name: sequences
SELECT SEQUENCE_SCHEMA AS schema_name,
       SEQUENCE_NAME AS sequence_name,
       DATA_TYPE AS data_type,
       START_VALUE AS start_value,
       MINIMUM_VALUE AS min_value,
       MAXIMUM_VALUE AS max_value,
       `INCREMENT` AS increment_value,
       CYCLE_OPTION AS cycle_option
FROM information_schema.SEQUENCES
WHERE SEQUENCE_SCHEMA {{SCHEMA_PREDICATE}}
ORDER BY SEQUENCE_SCHEMA, SEQUENCE_NAME

-- name: sequence_tables_fallback
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS sequence_name,
       NULL AS data_type,
       NULL AS start_value,
       NULL AS min_value,
       NULL AS max_value,
       NULL AS increment_value,
       NULL AS cycle_option
FROM information_schema.TABLES
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND TABLE_TYPE='SEQUENCE'
ORDER BY TABLE_SCHEMA, TABLE_NAME

-- name: spatial_columns
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS table_name,
       COLUMN_NAME AS column_name,
       DATA_TYPE AS data_type,
       COLUMN_TYPE AS column_type
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND DATA_TYPE IN ('geometry','point','linestring','polygon','multipoint',
                    'multilinestring','multipolygon','geometrycollection')
ORDER BY TABLE_SCHEMA, TABLE_NAME, ORDINAL_POSITION

-- name: spatial_indexes
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS table_name,
       INDEX_NAME AS index_name,
       INDEX_TYPE AS index_type,
       COLUMN_NAME AS column_name,
       SEQ_IN_INDEX AS seq_in_index
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND INDEX_TYPE='SPATIAL'
ORDER BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX

-- name: fulltext_indexes
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS table_name,
       INDEX_NAME AS index_name,
       INDEX_TYPE AS index_type,
       COLUMN_NAME AS column_name,
       SEQ_IN_INDEX AS seq_in_index
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}AND INDEX_TYPE='FULLTEXT'
ORDER BY TABLE_SCHEMA, TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX

-- name: gis_function_in_routines
SELECT ROUTINE_SCHEMA AS schema_name,
       ROUTINE_NAME AS object_name,
       ROUTINE_TYPE AS object_type,
       'ROUTINE_DEFINITION' AS source,
       'GIS_FUNCTION' AS detail
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA {{SCHEMA_PREDICATE}}
  AND ROUTINE_DEFINITION IS NOT NULL
  AND UPPER(ROUTINE_DEFINITION) REGEXP {{GIS_FUNCTION_REGEX}}
LIMIT 200

-- name: gis_function_in_triggers
SELECT TRIGGER_SCHEMA AS schema_name,
       TRIGGER_NAME AS object_name,
       'TRIGGER' AS object_type,
       'ACTION_STATEMENT' AS source,
       'GIS_FUNCTION' AS detail
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA {{SCHEMA_PREDICATE}}
  AND ACTION_STATEMENT IS NOT NULL
  AND UPPER(ACTION_STATEMENT) REGEXP {{GIS_FUNCTION_REGEX}}
LIMIT 200

-- name: gis_function_in_events
SELECT EVENT_SCHEMA AS schema_name,
       EVENT_NAME AS object_name,
       'EVENT' AS object_type,
       'EVENT_DEFINITION' AS source,
       'GIS_FUNCTION' AS detail
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA {{SCHEMA_PREDICATE}}
  AND EVENT_DEFINITION IS NOT NULL
  AND UPPER(EVENT_DEFINITION) REGEXP {{GIS_FUNCTION_REGEX}}
LIMIT 200

-- name: gis_function_in_views
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS object_name,
       'VIEW' AS object_type,
       'VIEW_DEFINITION' AS source,
       'GIS_FUNCTION' AS detail
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND VIEW_DEFINITION IS NOT NULL
  AND UPPER(VIEW_DEFINITION) REGEXP {{GIS_FUNCTION_REGEX}}
LIMIT 200

-- name: xml_function_in_routines
SELECT ROUTINE_SCHEMA AS schema_name,
       ROUTINE_NAME AS object_name,
       ROUTINE_TYPE AS object_type,
       'ROUTINE_DEFINITION' AS source,
       'XML_FUNCTION' AS detail
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA {{SCHEMA_PREDICATE}}
  AND ROUTINE_DEFINITION IS NOT NULL
  AND UPPER(ROUTINE_DEFINITION) REGEXP {{XML_FUNCTION_REGEX}}
LIMIT 200

-- name: xml_function_in_triggers
SELECT TRIGGER_SCHEMA AS schema_name,
       TRIGGER_NAME AS object_name,
       'TRIGGER' AS object_type,
       'ACTION_STATEMENT' AS source,
       'XML_FUNCTION' AS detail
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA {{SCHEMA_PREDICATE}}
  AND ACTION_STATEMENT IS NOT NULL
  AND UPPER(ACTION_STATEMENT) REGEXP {{XML_FUNCTION_REGEX}}
LIMIT 200

-- name: xml_function_in_events
SELECT EVENT_SCHEMA AS schema_name,
       EVENT_NAME AS object_name,
       'EVENT' AS object_type,
       'EVENT_DEFINITION' AS source,
       'XML_FUNCTION' AS detail
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA {{SCHEMA_PREDICATE}}
  AND EVENT_DEFINITION IS NOT NULL
  AND UPPER(EVENT_DEFINITION) REGEXP {{XML_FUNCTION_REGEX}}
LIMIT 200

-- name: xml_function_in_views
SELECT TABLE_SCHEMA AS schema_name,
       TABLE_NAME AS object_name,
       'VIEW' AS object_type,
       'VIEW_DEFINITION' AS source,
       'XML_FUNCTION' AS detail
FROM information_schema.VIEWS
WHERE TABLE_SCHEMA {{SCHEMA_PREDICATE}}
  AND VIEW_DEFINITION IS NOT NULL
  AND UPPER(VIEW_DEFINITION) REGEXP {{XML_FUNCTION_REGEX}}
LIMIT 200

-- name: show_create_table
SHOW CREATE TABLE `{{SCHEMA_NAME}}`.`{{TABLE_NAME}}`
