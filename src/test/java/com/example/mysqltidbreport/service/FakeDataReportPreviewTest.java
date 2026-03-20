package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.CompatibilityItem;
import com.example.mysqltidbreport.model.IncompatibleObject;
import com.example.mysqltidbreport.model.InspectionResult;
import com.example.mysqltidbreport.model.ReportSummary;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeDataReportPreviewTest {

    @Test
    void shouldGeneratePreviewHtmlFromFakeData() throws Exception {
        InspectionResult result = buildFakeInspectionResult();
        ReportSummary summary = new ReportSummary();
        summary.setCompatibleCount(1);
        summary.setPartialCount(5);
        summary.setIncompatibleCount(10);
        summary.setUnknownCount(0);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        templateEngine.setTemplateResolver(resolver);

        Context context = new Context();
        context.setVariable("result", result);
        context.setVariable("summary", summary);

        String html = templateEngine.process("report", context);

        Path out = Path.of("target/report-preview/mysql-tidb-fake-report.html").toAbsolutePath();
        Files.createDirectories(out.getParent());
        Files.writeString(out, html, StandardCharsets.UTF_8);

        System.out.println("PREVIEW_REPORT=" + out);
        assertTrue(Files.exists(out));
        assertTrue(Files.size(out) > 1024);
        assertTrue(html.contains("MySQL 使用对象与 TiDB 兼容性报告"));
        assertTrue(html.contains("demo_order_db"));
    }

    private InspectionResult buildFakeInspectionResult() {
        InspectionResult result = new InspectionResult();
        result.setGeneratedAt(LocalDateTime.of(2026, 3, 20, 20, 0, 0));
        result.setScannedSchema("demo_order_db");

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("version", "8.0.36");
        vars.put("version_comment", "MySQL Community Server - GPL");
        vars.put("character_set_server", "utf8mb4");
        vars.put("collation_server", "utf8mb4_0900_ai_ci");
        vars.put("lower_case_table_names", "1");
        result.setServerVariables(vars);

        Map<String, String> isolation = new LinkedHashMap<>();
        isolation.put("global", "REPEATABLE-READ");
        isolation.put("session", "REPEATABLE-READ");
        result.setIsolationLevels(isolation);

        result.setSchemaCharsets(List.of(
                row("charset_name", "utf8mb4", "collation_name", "utf8mb4_0900_ai_ci", "schema_count", 1)
        ));
        result.setTableCollations(List.of(
                row("collation_name", "utf8mb4_0900_ai_ci", "table_count", 34),
                row("collation_name", "utf8mb4_general_ci", "table_count", 5)
        ));
        result.setColumnCharsets(List.of(
                row("charset_name", "utf8mb4", "collation_name", "utf8mb4_0900_ai_ci", "column_count", 265),
                row("charset_name", "latin1", "collation_name", "latin1_swedish_ci", "column_count", 12)
        ));
        result.setStorageEngines(List.of(
                row("engine_name", "InnoDB", "table_count", 37),
                row("engine_name", "MyISAM", "table_count", 2)
        ));

        result.setSqlModeRows(List.of(
                row("global_sql_mode", "STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY,NO_ENGINE_SUBSTITUTION", "session_sql_mode", "STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY")
        ));

        result.setTempTableStatus(List.of(
                row("Variable_name", "Created_tmp_tables", "Value", 3842),
                row("Variable_name", "Created_tmp_disk_tables", "Value", 524)
        ));
        result.setTempTableVariables(List.of(
                row("Variable_name", "tmp_table_size", "Value", 16777216),
                row("Variable_name", "max_heap_table_size", "Value", 16777216)
        ));
        result.setTempTableEvidence(List.of(
                row("schema_name", "demo_order_db", "object_name", "sp_calc_daily", "object_type", "PROCEDURE", "source", "ROUTINE_DEFINITION")
        ));

        result.setUserVariableEvidence(List.of(
                row("schema_name", "demo_order_db", "object_name", "sp_calc_daily", "detail", "type=PROCEDURE", "source", "ROUTINE_DEFINITION")
        ));

        result.setUdfFunctions(List.of(
                row("function_name", "geo_hash_udf", "shared_library", "libgeo_udf.so", "function_type", "function")
        ));
        result.setStoredFunctions(List.of(
                row("schema_name", "demo_order_db", "routine_name", "f_order_score")
        ));
        result.setStoredProcedures(List.of(
                row("schema_name", "demo_order_db", "routine_name", "sp_sync_order_snapshot")
        ));
        result.setTriggers(List.of(
                row("schema_name", "demo_order_db", "trigger_name", "trg_order_audit")
        ));
        result.setEvents(List.of(
                row("schema_name", "demo_order_db", "event_name", "ev_cleanup_old_log")
        ));
        result.setSequences(List.of(
                row("schema_name", "demo_order_db", "sequence_name", "seq_order_id",
                        "data_type", "bigint", "start_value", 100000, "increment_value", 1, "cycle_option", "NO")
        ));
        result.setSpatialColumns(List.of(
                row("schema_name", "demo_order_db", "table_name", "geo_store", "column_name", "shape",
                        "data_type", "geometry", "column_type", "geometry")
        ));
        result.setSpatialIndexes(List.of(
                row("schema_name", "demo_order_db", "table_name", "geo_store", "index_name", "idx_geo_shape",
                        "column_name", "shape", "index_type", "SPATIAL")
        ));
        result.setFulltextIndexes(List.of(
                row("schema_name", "demo_order_db", "table_name", "article", "index_name", "idx_fulltext_content",
                        "column_name", "content", "index_type", "FULLTEXT")
        ));
        result.setGisFunctionEvidence(List.of(
                row("schema_name", "demo_order_db", "object_name", "sp_geo_stats", "object_type", "PROCEDURE",
                        "source", "ROUTINE_DEFINITION", "detail", "GIS_FUNCTION")
        ));
        result.setXmlFunctionEvidence(List.of(
                row("schema_name", "demo_order_db", "object_name", "sp_xml_parse", "object_type", "PROCEDURE",
                        "source", "ROUTINE_DEFINITION", "detail", "XML_FUNCTION")
        ));

        String orderArchiveDdl = "CREATE TABLE `order_archive` (\\n"
                + "  `id` bigint NOT NULL,\\n"
                + "  `order_no` varchar(64) CHARACTER SET latin1 COLLATE latin1_swedish_ci DEFAULT NULL,\\n"
                + "  PRIMARY KEY (`id`)\\n"
                + ") ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_swedish_ci;";
        String orderLegacyDdl = "CREATE TABLE `order_legacy` (\\n"
                + "  `id` bigint NOT NULL,\\n"
                + "  `customer_name` varchar(120) CHARACTER SET utf16 COLLATE utf16_general_ci DEFAULT NULL,\\n"
                + "  PRIMARY KEY (`id`)\\n"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf16 COLLATE=utf16_general_ci;";
        String geoStoreDdl = "CREATE TABLE `geo_store` (\\n"
                + "  `id` bigint NOT NULL,\\n"
                + "  `name` varchar(128) DEFAULT NULL,\\n"
                + "  `shape` geometry NOT NULL,\\n"
                + "  PRIMARY KEY (`id`),\\n"
                + "  SPATIAL KEY `idx_geo_shape` (`shape`)\\n"
                + ") ENGINE=InnoDB;";
        String articleDdl = "CREATE TABLE `article` (\\n"
                + "  `id` bigint NOT NULL,\\n"
                + "  `content` text,\\n"
                + "  PRIMARY KEY (`id`),\\n"
                + "  FULLTEXT KEY `idx_fulltext_content` (`content`)\\n"
                + ") ENGINE=InnoDB;";

        result.setCompatibilityItems(List.of(
                new CompatibilityItem("字符集", "utf8mb4, latin1, utf16", "INCOMPATIBLE", "存在 utf16 字符集表", "迁移前统一到 utf8mb4",
                        List.of(
                                new IncompatibleObject("字符集", "TABLE", "demo_order_db", "order_legacy",
                                        "表默认字符集=utf16；列 customer_name 字符集=utf16", orderLegacyDdl),
                                new IncompatibleObject("字符集", "TABLE", "demo_order_db", "order_archive",
                                        "表默认字符集=latin1（建议改造）", orderArchiveDdl)
                        )),
                new CompatibilityItem("排序规则", "utf8mb4_0900_ai_ci, utf8mb4_general_ci, utf16_general_ci", "INCOMPATIBLE",
                        "存在 utf16_general_ci 排序规则", "改为 utf8mb4 系排序规则",
                        List.of(
                                new IncompatibleObject("排序规则", "TABLE", "demo_order_db", "order_legacy",
                                        "表默认排序规则=utf16_general_ci", orderLegacyDdl)
                        )),
                new CompatibilityItem("存储引擎", "InnoDB, MyISAM", "INCOMPATIBLE", "包含 MyISAM", "迁移前改造为 InnoDB",
                        List.of(
                                new IncompatibleObject("存储引擎", "TABLE", "demo_order_db", "order_archive",
                                        "存储引擎=MyISAM", orderArchiveDdl)
                        )),
                new CompatibilityItem("空间特性(GIS/GEOMETRY)", "空间列=1, SPATIAL索引=1, GIS函数证据=1",
                        "INCOMPATIBLE",
                        "检测到空间类型与 GIS 函数依赖",
                        "迁移前改造空间数据模型并迁移 GIS 逻辑",
                        List.of(
                                new IncompatibleObject("空间特性(GIS/GEOMETRY)", "TABLE", "demo_order_db", "geo_store",
                                        "列 shape 使用空间类型=geometry；存在 SPATIAL 索引 idx_geo_shape（列=shape）", geoStoreDdl),
                                new IncompatibleObject("空间特性(GIS/GEOMETRY)", "PROCEDURE", "demo_order_db", "sp_geo_stats",
                                        "对象定义中使用 GIS 函数（来源=ROUTINE_DEFINITION, 标记=GIS_FUNCTION）", null)
                        )),
                new CompatibilityItem("FULLTEXT 索引", "FULLTEXT 索引数量=1", "PARTIAL",
                        "仅部分 TiDB Cloud 场景支持 FULLTEXT",
                        "评估是否使用外部检索系统",
                        List.of(
                                new IncompatibleObject("FULLTEXT 索引", "TABLE", "demo_order_db", "article",
                                        "存在 FULLTEXT 索引 idx_fulltext_content（列=content）", articleDdl)
                        )),
                new CompatibilityItem("XML Functions", "检测到 XML 函数依赖，记录数=1", "INCOMPATIBLE",
                        "存在 ExtractValue/UpdateXML 依赖", "迁移到应用层解析或改造为 JSON",
                        List.of(
                                new IncompatibleObject("XML Functions", "PROCEDURE", "demo_order_db", "sp_xml_parse",
                                        "对象定义中使用 XML 函数（来源=ROUTINE_DEFINITION, 标记=XML_FUNCTION）", null)
                        )),
                new CompatibilityItem("SQL 模式", "STRICT_TRANS_TABLES,ONLY_FULL_GROUP_BY", "PARTIAL", "ONLY_FULL_GROUP_BY 行为需对比", "做 SQL 回归"),
                new CompatibilityItem("临时表", "存在显式 TEMPORARY TABLE", "PARTIAL", "临时表行为存在差异", "流程逐条回归"),
                new CompatibilityItem("自定义变量", "检测到 @var 使用", "PARTIAL", "会话变量重度使用有风险", "改造核心链路"),
                new CompatibilityItem("自定义函数(UDF)", "geo_hash_udf", "INCOMPATIBLE", "TiDB 不支持 MySQL UDF", "迁移到应用层"),
                new CompatibilityItem("存储函数", "f_order_score", "INCOMPATIBLE", "存储函数不等价", "迁移到应用层"),
                new CompatibilityItem("存储过程", "sp_sync_order_snapshot", "INCOMPATIBLE", "不支持存储过程", "改为服务编排"),
                new CompatibilityItem("触发器", "trg_order_audit", "INCOMPATIBLE", "不支持触发器", "改为应用事务逻辑"),
                new CompatibilityItem("事件(Event)", "ev_cleanup_old_log", "INCOMPATIBLE", "不支持 Event Scheduler", "改外部调度器"),
                new CompatibilityItem("Sequence", "Sequence 数量=1", "PARTIAL",
                        "TiDB 支持 Sequence，但语法与行为需要校验",
                        "验证 nextval/setval 语义并做并发回归",
                        List.of(
                                new IncompatibleObject("Sequence", "SEQUENCE", "demo_order_db", "seq_order_id",
                                        "start_value=100000, increment=1, cycle_option=NO", null)
                        )),
                new CompatibilityItem("事务隔离级别", "global=REPEATABLE-READ, session=REPEATABLE-READ", "COMPATIBLE", "在常见兼容范围", "关注冲突重试")
        ));

        result.setQueryWarnings(List.of(
                "User variables by thread query failed: access denied",
                "UDF functions query may need mysql.func privilege"
        ));

        return result;
    }

    private Map<String, Object> row(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
