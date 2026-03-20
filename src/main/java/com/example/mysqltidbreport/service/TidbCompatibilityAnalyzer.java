package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.CompatibilityItem;
import com.example.mysqltidbreport.model.IncompatibleObject;
import com.example.mysqltidbreport.model.InspectionResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TidbCompatibilityAnalyzer {

    private static final Set<String> TIDB_SUPPORTED_CHARSETS = Set.of(
            "ascii", "latin1", "binary", "utf8", "utf8mb4", "gbk"
    );

    public List<CompatibilityItem> analyze(InspectionResult result) {
        List<CompatibilityItem> items = new ArrayList<>();
        items.add(analyzeCharset(result));
        items.add(analyzeCollation(result));
        items.add(analyzeStorageEngine(result));
        items.add(analyzeSpatial(result));
        items.add(analyzeFulltext(result));
        items.add(analyzeXmlFunctions(result));
        items.add(analyzeSqlMode(result));
        items.add(analyzeTempTable(result));
        items.add(analyzeUserVariables(result));
        items.add(analyzeUdf(result));
        items.add(analyzeStoredFunctions(result));
        items.add(analyzeStoredProcedures(result));
        items.add(analyzeTriggers(result));
        items.add(analyzeEvents(result));
        items.add(analyzeSequences(result));
        items.add(analyzeIsolation(result));
        return items;
    }

    private CompatibilityItem analyzeCharset(InspectionResult result) {
        Set<String> charsets = new LinkedHashSet<>();
        charsets.addAll(extractDistinct(result.getSchemaCharsets(), "charset_name"));
        charsets.addAll(extractDistinct(result.getColumnCharsets(), "charset_name"));
        charsets.addAll(extractDistinct(result.getTableCharsetCollationDetails(), "table_charset"));
        charsets.addAll(extractDistinct(result.getColumnCharsetCollationDetails(), "charset_name"));

        if (charsets.isEmpty()) {
            return item("字符集", "未检测到业务库字符集", "UNKNOWN",
                    "未读取到字符集元数据，可能是库为空或权限不足",
                    "补充权限后重新扫描，或指定 schema 参数缩小范围", List.of());
        }

        List<String> unsupported = charsets.stream()
                .map(v -> v.toLowerCase(Locale.ROOT))
                .filter(v -> !TIDB_SUPPORTED_CHARSETS.contains(v))
                .toList();

        if (!unsupported.isEmpty()) {
            Map<String, TableIssue> issueMap = new LinkedHashMap<>();

            for (Map<String, Object> row : result.getTableCharsetCollationDetails()) {
                String charset = lower(valueOf(row, "table_charset"));
                if (unsupported.contains(charset)) {
                    addTableIssue(issueMap,
                            valueOf(row, "schema_name"),
                            valueOf(row, "table_name"),
                            "表默认字符集=" + valueOf(row, "table_charset"));
                }
            }

            for (Map<String, Object> row : result.getColumnCharsetCollationDetails()) {
                String charset = lower(valueOf(row, "charset_name"));
                if (unsupported.contains(charset)) {
                    addTableIssue(issueMap,
                            valueOf(row, "schema_name"),
                            valueOf(row, "table_name"),
                            "列 " + valueOf(row, "column_name") + " 字符集=" + valueOf(row, "charset_name"));
                }
            }

            List<IncompatibleObject> objects = buildTableObjects("字符集", issueMap, result);
            return item("字符集", String.join(", ", charsets), "INCOMPATIBLE",
                    "检测到 TiDB 常用兼容列表外字符集: " + String.join(", ", unsupported),
                    "迁移前统一改为 utf8mb4，验证应用端编码和排序行为", objects);
        }

        if (charsets.stream().anyMatch(v -> "latin1".equalsIgnoreCase(v))) {
            Map<String, TableIssue> issueMap = new LinkedHashMap<>();
            for (Map<String, Object> row : result.getTableCharsetCollationDetails()) {
                if ("latin1".equalsIgnoreCase(valueOf(row, "table_charset"))) {
                    addTableIssue(issueMap,
                            valueOf(row, "schema_name"),
                            valueOf(row, "table_name"),
                            "表默认字符集=latin1");
                }
            }
            for (Map<String, Object> row : result.getColumnCharsetCollationDetails()) {
                if ("latin1".equalsIgnoreCase(valueOf(row, "charset_name"))) {
                    addTableIssue(issueMap,
                            valueOf(row, "schema_name"),
                            valueOf(row, "table_name"),
                            "列 " + valueOf(row, "column_name") + " 字符集=latin1");
                }
            }
            return item("字符集", String.join(", ", charsets), "PARTIAL",
                    "TiDB 支持 latin1，但官方建议优先 utf8mb4 以规避语义差异风险",
                    "在迁移窗口将 latin1 字段评估并转换为 utf8mb4",
                    buildTableObjects("字符集", issueMap, result));
        }

        return item("字符集", String.join(", ", charsets), "COMPATIBLE",
                "字符集处于 TiDB 常见兼容范围内", "继续校验排序规则与索引行为", List.of());
    }

    private CompatibilityItem analyzeCollation(InspectionResult result) {
        Set<String> collations = new LinkedHashSet<>();
        collations.addAll(extractDistinct(result.getTableCollations(), "collation_name"));
        collations.addAll(extractDistinct(result.getColumnCharsets(), "collation_name"));
        collations.addAll(extractDistinct(result.getTableCharsetCollationDetails(), "table_collation"));
        collations.addAll(extractDistinct(result.getColumnCharsetCollationDetails(), "collation_name"));

        if (collations.isEmpty()) {
            return item("排序规则", "未检测到排序规则", "UNKNOWN",
                    "没有读取到表/列排序规则", "检查权限后重试", List.of());
        }

        Map<String, TableIssue> issueMap = new LinkedHashMap<>();
        for (Map<String, Object> row : result.getTableCharsetCollationDetails()) {
            String collation = lower(valueOf(row, "table_collation"));
            if (isObviousUnsupportedCollation(collation)) {
                addTableIssue(issueMap,
                        valueOf(row, "schema_name"),
                        valueOf(row, "table_name"),
                        "表默认排序规则=" + valueOf(row, "table_collation"));
            }
        }
        for (Map<String, Object> row : result.getColumnCharsetCollationDetails()) {
            String collation = lower(valueOf(row, "collation_name"));
            if (isObviousUnsupportedCollation(collation)) {
                addTableIssue(issueMap,
                        valueOf(row, "schema_name"),
                        valueOf(row, "table_name"),
                        "列 " + valueOf(row, "column_name") + " 排序规则=" + valueOf(row, "collation_name"));
            }
        }

        if (!issueMap.isEmpty()) {
            return item("排序规则", shortList(collations), "INCOMPATIBLE",
                    "检测到依赖 utf16/utf32/ucs2 相关排序规则，TiDB 通常不作为兼容主路径",
                    "迁移前改为 utf8mb4 系排序规则并做结果集回归测试",
                    buildTableObjects("排序规则", issueMap, result));
        }

        return item("排序规则", shortList(collations), "PARTIAL",
                "TiDB 支持大量 MySQL 排序规则，但并非 100% 等价，需按目标版本逐项校验",
                "在 TiDB 目标版本执行 SHOW COLLATION 并对关键 SQL 做排序一致性回归",
                List.of());
    }

    private CompatibilityItem analyzeStorageEngine(InspectionResult result) {
        Set<String> engines = extractDistinct(result.getStorageEngines(), "engine_name");
        if (engines.isEmpty()) {
            return item("存储引擎", "未检测到业务表引擎", "UNKNOWN",
                    "没有读取到 TABLES 元数据", "确认 schema 或权限后重试", List.of());
        }

        List<String> nonInno = engines.stream()
                .filter(v -> !"innodb".equalsIgnoreCase(v))
                .toList();

        if (!nonInno.isEmpty()) {
            List<IncompatibleObject> objects = new ArrayList<>();
            for (Map<String, Object> row : result.getEngineTableDetails()) {
                String engine = valueOf(row, "engine_name");
                if (!"innodb".equalsIgnoreCase(engine)) {
                    String schemaName = valueOf(row, "schema_name");
                    String tableName = valueOf(row, "table_name");
                    objects.add(new IncompatibleObject(
                            "存储引擎",
                            "TABLE",
                            schemaName,
                            tableName,
                            "存储引擎=" + engine,
                            result.getTableDefinitions().get(tableKey(schemaName, tableName))
                    ));
                }
            }

            return item("存储引擎", String.join(", ", engines), "INCOMPATIBLE",
                    "TiDB 不提供 MyISAM/MEMORY 等 MySQL 存储引擎语义",
                    "迁移前将非 InnoDB 表改造并验证行为一致性", objects);
        }

        return item("存储引擎", String.join(", ", engines), "COMPATIBLE",
                "业务表均为 InnoDB，属于 TiDB 迁移常见兼容路径", "继续评估 SQL 与对象兼容项", List.of());
    }

    private CompatibilityItem analyzeSpatial(InspectionResult result) {
        Map<String, TableIssue> tableIssues = new LinkedHashMap<>();

        for (Map<String, Object> row : result.getSpatialColumns()) {
            String reason = "列 " + valueOf(row, "column_name") + " 使用空间类型=" + valueOf(row, "data_type");
            addTableIssue(tableIssues, valueOf(row, "schema_name"), valueOf(row, "table_name"), reason);
        }

        for (Map<String, Object> row : result.getSpatialIndexes()) {
            String reason = "存在 SPATIAL 索引 " + valueOf(row, "index_name")
                    + "（列=" + valueOf(row, "column_name") + "）";
            addTableIssue(tableIssues, valueOf(row, "schema_name"), valueOf(row, "table_name"), reason);
        }

        List<IncompatibleObject> objects = new ArrayList<>(buildTableObjects("空间特性(GIS/GEOMETRY)", tableIssues, result));
        objects.addAll(buildDefinitionObjects(
                "空间特性(GIS/GEOMETRY)",
                result.getGisFunctionEvidence(),
                "对象定义中使用 GIS 函数"
        ));

        if (objects.isEmpty()) {
            return item("空间特性(GIS/GEOMETRY)", "未检测到空间类型/空间索引/GIS 函数", "COMPATIBLE",
                    "未发现相关依赖", "无额外动作", List.of());
        }

        String usage = "空间列=" + result.getSpatialColumns().size()
                + ", SPATIAL索引=" + result.getSpatialIndexes().size()
                + ", GIS函数证据=" + result.getGisFunctionEvidence().size();
        return item("空间特性(GIS/GEOMETRY)", usage, "INCOMPATIBLE",
                "检测到空间类型或 GIS 函数依赖；TiDB 当前不支持 MySQL GIS 空间数据类型与函数",
                "迁移前将空间计算迁移到应用层/专用 GIS 引擎，或改造数据模型",
                objects);
    }

    private CompatibilityItem analyzeFulltext(InspectionResult result) {
        if (result.getFulltextIndexes().isEmpty()) {
            return item("FULLTEXT 索引", "未检测到 FULLTEXT 索引", "COMPATIBLE",
                    "未发现该类对象", "无额外动作", List.of());
        }

        Map<String, TableIssue> issueMap = new LinkedHashMap<>();
        for (Map<String, Object> row : result.getFulltextIndexes()) {
            String reason = "存在 FULLTEXT 索引 " + valueOf(row, "index_name")
                    + "（列=" + valueOf(row, "column_name") + "）";
            addTableIssue(issueMap, valueOf(row, "schema_name"), valueOf(row, "table_name"), reason);
        }

        return item("FULLTEXT 索引",
                "FULLTEXT 索引数量=" + result.getFulltextIndexes().size(),
                "PARTIAL",
                "TiDB 仅在部分 TiDB Cloud Starter/Essential 场景支持 FULLTEXT，通用部署通常不支持",
                "评估是否迁移到外部检索系统（如 ES/OpenSearch）或确认目标 TiDB 规格可用性",
                buildTableObjects("FULLTEXT 索引", issueMap, result));
    }

    private CompatibilityItem analyzeXmlFunctions(InspectionResult result) {
        if (result.getXmlFunctionEvidence().isEmpty()) {
            return item("XML Functions", "未检测到 ExtractValue/UpdateXML 使用", "COMPATIBLE",
                    "未发现该类函数依赖", "无额外动作", List.of());
        }

        List<IncompatibleObject> objects = buildDefinitionObjects(
                "XML Functions",
                result.getXmlFunctionEvidence(),
                "对象定义中使用 XML 函数（ExtractValue/UpdateXML）"
        );

        return item("XML Functions",
                "检测到 XML 函数依赖，记录数=" + result.getXmlFunctionEvidence().size(),
                "INCOMPATIBLE",
                "TiDB 不支持 MySQL XML 函数（ExtractValue、UpdateXML）",
                "将 XML 解析逻辑迁移到应用层，或改造为 JSON / 结构化列",
                objects);
    }

    private CompatibilityItem analyzeSqlMode(InspectionResult result) {
        Set<String> modes = new LinkedHashSet<>();
        for (Map<String, Object> row : result.getSqlModeRows()) {
            modes.addAll(splitModes(valueOf(row, "global_sql_mode")));
            modes.addAll(splitModes(valueOf(row, "session_sql_mode")));
        }

        if (modes.isEmpty()) {
            return item("SQL 模式", "未检测到 SQL_MODE", "UNKNOWN",
                    "未读取到 global/session sql_mode", "补充权限并重试", List.of());
        }

        Set<String> upperModes = modes.stream().map(v -> v.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());

        if (upperModes.contains("ORACLE") || upperModes.contains("POSTGRESQL")) {
            return item("SQL 模式", String.join(", ", modes), "INCOMPATIBLE",
                    "TiDB 对 ORACLE/POSTGRESQL 兼容模式不提供等价语义",
                    "移除该模式并改写依赖特性", List.of());
        }

        if (upperModes.contains("ONLY_FULL_GROUP_BY")
                || upperModes.contains("NO_DIR_IN_CREATE")
                || upperModes.contains("NO_ENGINE_SUBSTITUTION")) {
            return item("SQL 模式", String.join(", ", modes), "PARTIAL",
                    "TiDB 对部分 SQL_MODE 存在差异或不完全生效",
                    "对 GROUP BY 与 DDL 行为做专项回归", List.of());
        }

        return item("SQL 模式", String.join(", ", modes), "COMPATIBLE",
                "已用 SQL_MODE 未命中明显不兼容项", "建议仍对关键语句做端到端回归", List.of());
    }

    private CompatibilityItem analyzeTempTable(InspectionResult result) {
        boolean hasExplicitTemp = !result.getTempTableEvidence().isEmpty();
        long createdTmp = result.getTempTableStatus().stream()
                .filter(row -> "Created_tmp_tables".equalsIgnoreCase(valueOf(row, "Variable_name")))
                .mapToLong(row -> longValue(row.get("Value")))
                .sum();

        if (hasExplicitTemp) {
            List<IncompatibleObject> objects = new ArrayList<>();
            for (Map<String, Object> row : result.getTempTableEvidence()) {
                objects.add(new IncompatibleObject(
                        "临时表",
                        valueOf(row, "object_type"),
                        valueOf(row, "schema_name"),
                        valueOf(row, "object_name"),
                        "检测到 TEMPORARY TABLE 相关定义（来源=" + valueOf(row, "source") + "）",
                        null
                ));
            }
            return item("临时表", "检测到显式临时表相关 SQL", "PARTIAL",
                    "TiDB 支持临时表但实现与 MySQL 存在差异（生命周期、限制、优化器行为）",
                    "在 TiDB 上逐条回归 CREATE TEMPORARY TABLE 相关流程", objects);
        }

        if (createdTmp > 0) {
            return item("临时表", "Created_tmp_tables=" + createdTmp, "PARTIAL",
                    "MySQL 内部临时表统计不代表对象迁移，但说明查询可能依赖临时中间结果",
                    "在 TiDB 上做慢 SQL 与执行计划复核，避免性能回退", List.of());
        }

        return item("临时表", "未检测到显式临时表证据", "COMPATIBLE",
                "未发现明确依赖临时表对象的证据", "保留上线前 SQL 回归测试", List.of());
    }

    private CompatibilityItem analyzeUserVariables(InspectionResult result) {
        if (result.getUserVariableEvidence().isEmpty()) {
            return item("自定义变量", "未检测到 @var 使用证据", "COMPATIBLE",
                    "未发现用户变量依赖", "可继续评估其他对象", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getUserVariableEvidence()) {
            objects.add(new IncompatibleObject(
                    "自定义变量",
                    valueOf(row, "source"),
                    valueOf(row, "schema_name"),
                    valueOf(row, "object_name"),
                    "检测到用户变量使用（" + valueOf(row, "detail") + "）",
                    null
            ));
        }

        return item("自定义变量", "检测到用户变量使用，记录数=" + result.getUserVariableEvidence().size(), "PARTIAL",
                "TiDB 支持用户变量但官方文档提示相关能力在部分版本/场景下需谨慎使用",
                "改造核心逻辑避免重度依赖会话变量，并做并发场景回归", objects);
    }

    private CompatibilityItem analyzeUdf(InspectionResult result) {
        if (result.getUdfFunctions().isEmpty()) {
            return item("自定义函数(UDF)", "未检测到 mysql.func 记录", "COMPATIBLE",
                    "未发现 MySQL UDF 插件依赖", "继续检查存储过程和存储函数", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getUdfFunctions()) {
            objects.add(new IncompatibleObject(
                    "自定义函数(UDF)",
                    "UDF",
                    "mysql",
                    valueOf(row, "function_name"),
                    "UDF 插件库=" + valueOf(row, "shared_library"),
                    null
            ));
        }

        return item("自定义函数(UDF)", "UDF 数量=" + result.getUdfFunctions().size(), "INCOMPATIBLE",
                "TiDB 不支持 MySQL C/UDF 插件体系", "将 UDF 逻辑迁移为应用层代码或 SQL 改写", objects);
    }

    private CompatibilityItem analyzeStoredFunctions(InspectionResult result) {
        if (result.getStoredFunctions().isEmpty()) {
            return item("存储函数", "未检测到存储函数", "COMPATIBLE",
                    "不存在该类对象", "无额外动作", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getStoredFunctions()) {
            objects.add(new IncompatibleObject(
                    "存储函数", "FUNCTION", valueOf(row, "schema_name"), valueOf(row, "routine_name"),
                    "TiDB 不提供等价存储函数能力", null
            ));
        }

        return item("存储函数", "存储函数数量=" + result.getStoredFunctions().size(), "INCOMPATIBLE",
                "TiDB 与 MySQL 在存储函数能力上不等价，通常不作为兼容路径",
                "将存储函数逻辑迁移到应用层或改写为普通 SQL", objects);
    }

    private CompatibilityItem analyzeStoredProcedures(InspectionResult result) {
        if (result.getStoredProcedures().isEmpty()) {
            return item("存储过程", "未检测到存储过程", "COMPATIBLE",
                    "不存在该类对象", "无额外动作", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getStoredProcedures()) {
            objects.add(new IncompatibleObject(
                    "存储过程", "PROCEDURE", valueOf(row, "schema_name"), valueOf(row, "routine_name"),
                    "TiDB 不支持 MySQL 存储过程", null
            ));
        }

        return item("存储过程", "存储过程数量=" + result.getStoredProcedures().size(), "INCOMPATIBLE",
                "TiDB 不支持 MySQL 存储过程", "将过程逻辑迁移到应用服务层", objects);
    }

    private CompatibilityItem analyzeTriggers(InspectionResult result) {
        if (result.getTriggers().isEmpty()) {
            return item("触发器", "未检测到触发器", "COMPATIBLE",
                    "不存在该类对象", "无额外动作", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getTriggers()) {
            objects.add(new IncompatibleObject(
                    "触发器", "TRIGGER", valueOf(row, "schema_name"), valueOf(row, "trigger_name"),
                    "TiDB 不支持 MySQL 触发器", null
            ));
        }

        return item("触发器", "触发器数量=" + result.getTriggers().size(), "INCOMPATIBLE",
                "TiDB 不支持 MySQL 触发器", "将触发逻辑迁移为应用事务逻辑或异步任务", objects);
    }

    private CompatibilityItem analyzeEvents(InspectionResult result) {
        if (result.getEvents().isEmpty()) {
            return item("事件(Event)", "未检测到事件", "COMPATIBLE",
                    "不存在该类对象", "无额外动作", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getEvents()) {
            objects.add(new IncompatibleObject(
                    "事件(Event)", "EVENT", valueOf(row, "schema_name"), valueOf(row, "event_name"),
                    "TiDB 不支持 MySQL Event Scheduler", null
            ));
        }

        return item("事件(Event)", "事件数量=" + result.getEvents().size(), "INCOMPATIBLE",
                "TiDB 不支持 MySQL Event Scheduler", "改为外部调度器（如 Quartz、Cron、Airflow）", objects);
    }

    private CompatibilityItem analyzeSequences(InspectionResult result) {
        if (result.getSequences().isEmpty()) {
            return item("Sequence", "未检测到 Sequence 对象", "COMPATIBLE",
                    "未发现独立 Sequence 对象依赖", "无额外动作", List.of());
        }

        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : result.getSequences()) {
            String schemaName = valueOf(row, "schema_name");
            String sequenceName = valueOf(row, "sequence_name");
            List<String> attrs = new ArrayList<>();
            addAttr(attrs, "data_type", valueOf(row, "data_type"));
            addAttr(attrs, "start_value", valueOf(row, "start_value"));
            addAttr(attrs, "increment", valueOf(row, "increment_value"));
            addAttr(attrs, "cycle_option", valueOf(row, "cycle_option"));

            String reason = attrs.isEmpty()
                    ? "检测到 Sequence 对象，需验证语法与 nextval/lastval 行为"
                    : "检测到 Sequence 对象（" + String.join(", ", attrs) + "）";

            objects.add(new IncompatibleObject(
                    "Sequence",
                    "SEQUENCE",
                    schemaName,
                    sequenceName,
                    reason,
                    null
            ));
        }

        return item("Sequence", "Sequence 数量=" + result.getSequences().size(), "PARTIAL",
                "TiDB 支持 Sequence，但与 MySQL/MariaDB 在语法和行为上可能存在差异",
                "逐个对照 CREATE SEQUENCE 参数，并回归 nextval/setval/cycle 相关逻辑",
                objects);
    }

    private CompatibilityItem analyzeIsolation(InspectionResult result) {
        String global = safe(result.getIsolationLevels().get("global")).toUpperCase(Locale.ROOT);
        String session = safe(result.getIsolationLevels().get("session")).toUpperCase(Locale.ROOT);
        String usage = "global=" + safe(result.getIsolationLevels().get("global"))
                + ", session=" + safe(result.getIsolationLevels().get("session"));

        if (global.contains("READ-UNCOMMITTED") || global.contains("SERIALIZABLE")
                || session.contains("READ-UNCOMMITTED") || session.contains("SERIALIZABLE")) {
            return item("事务隔离级别", usage, "INCOMPATIBLE",
                    "TiDB 主要兼容 READ COMMITTED/REPEATABLE READ，其他级别需改造",
                    "统一到 RC 或 RR 并验证并发一致性", List.of());
        }

        if (global.contains("READ-COMMITTED") || global.contains("REPEATABLE-READ")
                || session.contains("READ-COMMITTED") || session.contains("REPEATABLE-READ")) {
            return item("事务隔离级别", usage, "COMPATIBLE",
                    "隔离级别位于 TiDB 常见兼容范围", "关注 RC/RR 下锁冲突与重试策略", List.of());
        }

        return item("事务隔离级别", usage, "UNKNOWN",
                "无法识别当前隔离级别", "手工执行 SELECT @@GLOBAL.transaction_isolation 校验", List.of());
    }

    private CompatibilityItem item(String category, String mysqlUsage, String compatibility,
                                   String evidence, String recommendation, List<IncompatibleObject> objects) {
        return new CompatibilityItem(category, mysqlUsage, compatibility, evidence, recommendation, objects);
    }

    private Set<String> extractDistinct(List<Map<String, Object>> rows, String key) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> row : rows) {
            String value = valueOf(row, key);
            if (!value.isBlank() && !"NULL".equalsIgnoreCase(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private String valueOf(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return safe(value == null ? null : String.valueOf(value));
    }

    private Set<String> splitModes(String raw) {
        if (raw == null || raw.isBlank() || "NULL".equalsIgnoreCase(raw)) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String shortList(Set<String> values) {
        List<String> list = new ArrayList<>(values);
        if (list.size() <= 8) {
            return String.join(", ", list);
        }
        return String.join(", ", list.subList(0, 8)) + " ... (total=" + list.size() + ")";
    }

    private String safe(String value) {
        return value == null ? "N/A" : value;
    }

    private String lower(String value) {
        return safe(value).toLowerCase(Locale.ROOT);
    }

    private long longValue(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void addAttr(List<String> attrs, String key, String value) {
        if (value == null || value.isBlank() || "N/A".equalsIgnoreCase(value) || "NULL".equalsIgnoreCase(value)) {
            return;
        }
        attrs.add(key + "=" + value);
    }

    private List<IncompatibleObject> buildDefinitionObjects(String category,
                                                            List<Map<String, Object>> evidenceRows,
                                                            String reasonPrefix) {
        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map<String, Object> row : evidenceRows) {
            String objectType = valueOf(row, "object_type");
            if ("N/A".equalsIgnoreCase(objectType) || "NULL".equalsIgnoreCase(objectType)) {
                objectType = valueOf(row, "source");
            }
            objects.add(new IncompatibleObject(
                    category,
                    objectType,
                    valueOf(row, "schema_name"),
                    valueOf(row, "object_name"),
                    reasonPrefix + "（来源=" + valueOf(row, "source")
                            + ", 标记=" + valueOf(row, "detail") + "）",
                    null
            ));
        }
        return objects;
    }

    private boolean isObviousUnsupportedCollation(String collationLower) {
        return collationLower.startsWith("utf16")
                || collationLower.startsWith("utf32")
                || collationLower.startsWith("ucs2");
    }

    private void addTableIssue(Map<String, TableIssue> issueMap, String schemaName, String tableName, String reason) {
        if ("N/A".equals(schemaName) || "N/A".equals(tableName)
                || "NULL".equalsIgnoreCase(schemaName) || "NULL".equalsIgnoreCase(tableName)) {
            return;
        }
        String key = tableKey(schemaName, tableName);
        TableIssue issue = issueMap.computeIfAbsent(key, k -> new TableIssue(schemaName, tableName));
        issue.reasons.add(reason);
    }

    private List<IncompatibleObject> buildTableObjects(String category,
                                                       Map<String, TableIssue> issueMap,
                                                       InspectionResult result) {
        List<IncompatibleObject> objects = new ArrayList<>();
        for (Map.Entry<String, TableIssue> entry : issueMap.entrySet()) {
            TableIssue issue = entry.getValue();
            objects.add(new IncompatibleObject(
                    category,
                    "TABLE",
                    issue.schemaName,
                    issue.tableName,
                    String.join("；", issue.reasons),
                    result.getTableDefinitions().get(tableKey(issue.schemaName, issue.tableName))
            ));
        }
        return objects;
    }

    private String tableKey(String schemaName, String tableName) {
        return schemaName + "." + tableName;
    }

    private static class TableIssue {
        private final String schemaName;
        private final String tableName;
        private final Set<String> reasons = new LinkedHashSet<>();

        private TableIssue(String schemaName, String tableName) {
            this.schemaName = schemaName;
            this.tableName = tableName;
        }
    }
}
