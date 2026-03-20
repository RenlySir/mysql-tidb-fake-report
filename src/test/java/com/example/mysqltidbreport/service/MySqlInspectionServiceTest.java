package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.InspectionResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MySqlInspectionServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private MySqlInspectionService inspectionService;

    @Test
    void shouldFallbackToTxIsolationWhenTransactionIsolationVariableUnavailable() {
        mockCommonQueryBehavior();

        when(jdbcTemplate.queryForMap(contains("transaction_isolation")))
                .thenThrow(new RuntimeException("no transaction_isolation"));
        when(jdbcTemplate.queryForMap(contains("tx_isolation")))
                .thenReturn(Map.of("global_isolation", "READ-COMMITTED", "session_isolation", "REPEATABLE-READ"));

        InspectionResult result = inspectionService.inspect("demo");

        assertEquals("demo", result.getScannedSchema());
        assertEquals("READ-COMMITTED", result.getIsolationLevels().get("global"));
        assertEquals("REPEATABLE-READ", result.getIsolationLevels().get("session"));
    }

    @Test
    void shouldSetIsolationToNAAndRecordWarningWhenBothIsolationQueriesFail() {
        mockCommonQueryBehavior();

        when(jdbcTemplate.queryForMap(contains("transaction_isolation")))
                .thenThrow(new RuntimeException("first failed"));
        when(jdbcTemplate.queryForMap(contains("tx_isolation")))
                .thenThrow(new RuntimeException("second failed"));

        InspectionResult result = inspectionService.inspect(null);

        assertEquals("ALL_NON_SYSTEM_SCHEMAS", result.getScannedSchema());
        assertEquals("N/A", result.getIsolationLevels().get("global"));
        assertEquals("N/A", result.getIsolationLevels().get("session"));
        assertTrue(result.getQueryWarnings().stream().anyMatch(w -> w.contains("Isolation level query failed")));
    }

    @Test
    void shouldLoadSequencesFromInformationSchemaWhenAvailable() {
        mockCommonQueryBehavior();
        when(jdbcTemplate.queryForMap(contains("transaction_isolation")))
                .thenReturn(Map.of("global_isolation", "REPEATABLE-READ", "session_isolation", "REPEATABLE-READ"));
        when(jdbcTemplate.queryForList(contains("FROM information_schema.SEQUENCES"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "sequence_name", "seq_order_id",
                        "data_type", "bigint",
                        "start_value", 1,
                        "increment_value", 1,
                        "cycle_option", "NO"
                )));

        InspectionResult result = inspectionService.inspect("demo");

        assertEquals(1, result.getSequences().size());
        assertEquals("seq_order_id", result.getSequences().get(0).get("sequence_name"));
    }

    @Test
    void shouldFallbackToSequenceTableTypeWhenSequencesMetadataUnavailable() {
        mockCommonQueryBehavior();
        when(jdbcTemplate.queryForMap(contains("transaction_isolation")))
                .thenReturn(Map.of("global_isolation", "REPEATABLE-READ", "session_isolation", "REPEATABLE-READ"));
        when(jdbcTemplate.queryForList(contains("FROM information_schema.SEQUENCES"), any(Object[].class)))
                .thenThrow(new RuntimeException("Table 'information_schema.SEQUENCES' doesn't exist"));
        when(jdbcTemplate.queryForList(contains("TABLE_TYPE='SEQUENCE'"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "sequence_name", "seq_invoice_id"
                )));

        InspectionResult result = inspectionService.inspect("demo");

        assertEquals(1, result.getSequences().size());
        assertEquals("seq_invoice_id", result.getSequences().get(0).get("sequence_name"));
        assertFalse(result.getQueryWarnings().stream().anyMatch(w -> w.contains("Sequences query failed")));
    }

    @Test
    void shouldCollectAdvancedFeatureEvidenceWhenPresent() {
        mockCommonQueryBehavior();
        when(jdbcTemplate.queryForMap(contains("transaction_isolation")))
                .thenReturn(Map.of("global_isolation", "REPEATABLE-READ", "session_isolation", "REPEATABLE-READ"));
        when(jdbcTemplate.queryForList(contains("DATA_TYPE IN ('geometry'"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "table_name", "geo_store",
                        "column_name", "shape",
                        "data_type", "geometry"
                )));
        when(jdbcTemplate.queryForList(contains("INDEX_TYPE='SPATIAL'"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "table_name", "geo_store",
                        "index_name", "idx_geo_shape"
                )));
        when(jdbcTemplate.queryForList(contains("INDEX_TYPE='FULLTEXT'"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "table_name", "article",
                        "index_name", "idx_fulltext_content"
                )));
        when(jdbcTemplate.queryForList(contains("GIS_FUNCTION"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "object_name", "sp_geo_stats",
                        "object_type", "PROCEDURE"
                )));
        when(jdbcTemplate.queryForList(contains("XML_FUNCTION"), any(Object[].class)))
                .thenReturn(List.of(Map.of(
                        "schema_name", "demo",
                        "object_name", "sp_xml_parse",
                        "object_type", "PROCEDURE"
                )));

        InspectionResult result = inspectionService.inspect("demo");

        assertEquals(1, result.getSpatialColumns().size());
        assertEquals(1, result.getSpatialIndexes().size());
        assertEquals(1, result.getFulltextIndexes().size());
        assertFalse(result.getGisFunctionEvidence().isEmpty());
        assertFalse(result.getXmlFunctionEvidence().isEmpty());
    }

    private void mockCommonQueryBehavior() {
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Object.class))).thenReturn("mock-value");
        lenient().when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());
        lenient().when(jdbcTemplate.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());
    }
}
