package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.CompatibilityItem;
import com.example.mysqltidbreport.model.InspectionResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TidbCompatibilityAnalyzerTest {

    private final TidbCompatibilityAnalyzer analyzer = new TidbCompatibilityAnalyzer();

    @Test
    void shouldMarkUnsupportedCharsetAsIncompatible() {
        InspectionResult result = new InspectionResult();
        result.setSchemaCharsets(List.of(row("charset_name", "utf16", "collation_name", "utf16_general_ci")));
        result.setColumnCharsets(List.of());

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);

        assertEquals("INCOMPATIBLE", byCategory.get("字符集").getTidbCompatibility());
    }

    @Test
    void shouldIncludeTableAndDefinitionForIncompatibleCharsetAndCollation() {
        InspectionResult result = new InspectionResult();
        result.setSchemaCharsets(List.of(row("charset_name", "utf16", "collation_name", "utf16_general_ci")));
        result.setColumnCharsets(List.of());
        result.setTableCharsetCollationDetails(List.of(
                row("schema_name", "demo_db", "table_name", "orders_legacy",
                        "table_charset", "utf16", "table_collation", "utf16_general_ci")
        ));
        result.setColumnCharsetCollationDetails(List.of(
                row("schema_name", "demo_db", "table_name", "orders_legacy",
                        "column_name", "customer_name",
                        "charset_name", "utf16", "collation_name", "utf16_general_ci")
        ));
        result.setTableDefinitions(Map.of(
                "demo_db.orders_legacy",
                "CREATE TABLE `orders_legacy` (`id` bigint PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf16"
        ));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);
        CompatibilityItem charsetItem = byCategory.get("字符集");
        CompatibilityItem collationItem = byCategory.get("排序规则");

        assertEquals("INCOMPATIBLE", charsetItem.getTidbCompatibility());
        assertFalse(charsetItem.getIncompatibleObjects().isEmpty());
        assertEquals("orders_legacy", charsetItem.getIncompatibleObjects().get(0).getObjectName());
        assertTrue(charsetItem.getIncompatibleObjects().get(0).getDefinition().contains("CREATE TABLE"));

        assertEquals("INCOMPATIBLE", collationItem.getTidbCompatibility());
        assertFalse(collationItem.getIncompatibleObjects().isEmpty());
        assertEquals("orders_legacy", collationItem.getIncompatibleObjects().get(0).getObjectName());
        assertTrue(collationItem.getIncompatibleObjects().get(0).getDefinition().contains("CREATE TABLE"));
    }

    @Test
    void shouldMarkProgrammabilityObjectsAsIncompatibleWhenUsed() {
        InspectionResult result = new InspectionResult();
        result.setUdfFunctions(List.of(row("function_name", "f1")));
        result.setStoredFunctions(List.of(row("routine_name", "sf1")));
        result.setStoredProcedures(List.of(row("routine_name", "sp1")));
        result.setTriggers(List.of(row("trigger_name", "trg1")));
        result.setEvents(List.of(row("event_name", "ev1")));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);

        assertEquals("INCOMPATIBLE", byCategory.get("自定义函数(UDF)").getTidbCompatibility());
        assertEquals("INCOMPATIBLE", byCategory.get("存储函数").getTidbCompatibility());
        assertEquals("INCOMPATIBLE", byCategory.get("存储过程").getTidbCompatibility());
        assertEquals("INCOMPATIBLE", byCategory.get("触发器").getTidbCompatibility());
        assertEquals("INCOMPATIBLE", byCategory.get("事件(Event)").getTidbCompatibility());
    }

    @Test
    void shouldMarkSqlModeAndIsolationAsIncompatibleForKnownRiskModes() {
        InspectionResult result = new InspectionResult();
        result.setSqlModeRows(List.of(row(
                "global_sql_mode", "ORACLE,ONLY_FULL_GROUP_BY",
                "session_sql_mode", "ORACLE"
        )));
        result.setIsolationLevels(Map.of("global", "SERIALIZABLE", "session", "REPEATABLE-READ"));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);

        assertEquals("INCOMPATIBLE", byCategory.get("SQL 模式").getTidbCompatibility());
        assertEquals("INCOMPATIBLE", byCategory.get("事务隔离级别").getTidbCompatibility());
    }

    @Test
    void shouldMarkTempAndUserVariablesAsPartialWhenEvidenceExists() {
        InspectionResult result = new InspectionResult();
        result.setTempTableEvidence(List.of(row("schema_name", "app", "object_name", "sp_tmp")));
        result.setUserVariableEvidence(List.of(row("schema_name", "app", "object_name", "sp_v")));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);

        assertEquals("PARTIAL", byCategory.get("临时表").getTidbCompatibility());
        assertEquals("PARTIAL", byCategory.get("自定义变量").getTidbCompatibility());
    }

    @Test
    void shouldMarkInnoDbEngineAsCompatible() {
        InspectionResult result = new InspectionResult();
        result.setStorageEngines(List.of(row("engine_name", "InnoDB")));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);

        assertEquals("COMPATIBLE", byCategory.get("存储引擎").getTidbCompatibility());
    }

    @Test
    void shouldMarkSequenceAsPartialWhenUsed() {
        InspectionResult result = new InspectionResult();
        result.setSequences(List.of(
                row("schema_name", "app", "sequence_name", "seq_order_id",
                        "data_type", "bigint", "start_value", 1, "increment_value", 1, "cycle_option", "NO")
        ));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);
        CompatibilityItem item = byCategory.get("Sequence");

        assertEquals("PARTIAL", item.getTidbCompatibility());
        assertFalse(item.getIncompatibleObjects().isEmpty());
        assertEquals("seq_order_id", item.getIncompatibleObjects().get(0).getObjectName());
    }

    @Test
    void shouldMarkSpatialFeatureAsIncompatibleAndIncludeObjectNames() {
        InspectionResult result = new InspectionResult();
        result.setSpatialColumns(List.of(
                row("schema_name", "demo", "table_name", "geo_store", "column_name", "shape", "data_type", "geometry")
        ));
        result.setSpatialIndexes(List.of(
                row("schema_name", "demo", "table_name", "geo_store", "index_name", "idx_geo_shape", "column_name", "shape")
        ));
        result.setGisFunctionEvidence(List.of(
                row("schema_name", "demo", "object_name", "sp_geo_stats", "object_type", "PROCEDURE",
                        "source", "ROUTINE_DEFINITION", "detail", "GIS_FUNCTION")
        ));
        result.setTableDefinitions(Map.of(
                "demo.geo_store",
                "CREATE TABLE `geo_store` (`id` bigint primary key, `shape` geometry) ENGINE=InnoDB"
        ));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);
        CompatibilityItem item = byCategory.get("空间特性(GIS/GEOMETRY)");

        assertEquals("INCOMPATIBLE", item.getTidbCompatibility());
        assertTrue(item.getIncompatibleObjects().stream().anyMatch(o -> "geo_store".equals(o.getObjectName())));
        assertTrue(item.getIncompatibleObjects().stream().anyMatch(o -> "sp_geo_stats".equals(o.getObjectName())));
    }

    @Test
    void shouldMarkFulltextAsPartialWhenIndexesExist() {
        InspectionResult result = new InspectionResult();
        result.setFulltextIndexes(List.of(
                row("schema_name", "demo", "table_name", "article", "index_name", "idx_fulltext_content",
                        "column_name", "content", "index_type", "FULLTEXT")
        ));
        result.setTableDefinitions(Map.of(
                "demo.article",
                "CREATE TABLE `article` (`id` bigint primary key, `content` text, FULLTEXT KEY `idx_fulltext_content` (`content`)) ENGINE=InnoDB"
        ));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);
        CompatibilityItem item = byCategory.get("FULLTEXT 索引");

        assertEquals("PARTIAL", item.getTidbCompatibility());
        assertFalse(item.getIncompatibleObjects().isEmpty());
        assertEquals("article", item.getIncompatibleObjects().get(0).getObjectName());
    }

    @Test
    void shouldMarkXmlFunctionsAsIncompatibleWhenEvidenceExists() {
        InspectionResult result = new InspectionResult();
        result.setXmlFunctionEvidence(List.of(
                row("schema_name", "demo", "object_name", "sp_xml_parse", "object_type", "PROCEDURE",
                        "source", "ROUTINE_DEFINITION", "detail", "XML_FUNCTION")
        ));

        Map<String, CompatibilityItem> byCategory = analyzeByCategory(result);
        CompatibilityItem item = byCategory.get("XML Functions");

        assertEquals("INCOMPATIBLE", item.getTidbCompatibility());
        assertFalse(item.getIncompatibleObjects().isEmpty());
        assertEquals("sp_xml_parse", item.getIncompatibleObjects().get(0).getObjectName());
    }

    private Map<String, CompatibilityItem> analyzeByCategory(InspectionResult result) {
        List<CompatibilityItem> items = analyzer.analyze(result);
        return items.stream().collect(Collectors.toMap(CompatibilityItem::getCategory, Function.identity()));
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
