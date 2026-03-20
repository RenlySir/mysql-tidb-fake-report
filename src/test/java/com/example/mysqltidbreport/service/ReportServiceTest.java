package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.CompatibilityItem;
import com.example.mysqltidbreport.model.InspectionResult;
import com.example.mysqltidbreport.model.ReportPayload;
import com.example.mysqltidbreport.model.ReportSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private MySqlInspectionService inspectionService;

    @Mock
    private TidbCompatibilityAnalyzer compatibilityAnalyzer;

    @TempDir
    Path tempDir;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        templateEngine.setTemplateResolver(resolver);

        reportService = new ReportService(inspectionService, compatibilityAnalyzer, templateEngine);
    }

    @Test
    void shouldGenerateSummaryCountsCorrectly() {
        InspectionResult inspectionResult = new InspectionResult();
        when(inspectionService.inspect("demo")).thenReturn(inspectionResult);
        when(compatibilityAnalyzer.analyze(inspectionResult)).thenReturn(List.of(
                new CompatibilityItem("字符集", "utf8mb4", "COMPATIBLE", "ok", "none"),
                new CompatibilityItem("排序规则", "utf8mb4_general_ci", "PARTIAL", "partial", "check"),
                new CompatibilityItem("UDF", "f1", "INCOMPATIBLE", "bad", "rewrite"),
                new CompatibilityItem("隔离级别", "RR", "UNKNOWN", "n/a", "verify")
        ));

        ReportPayload payload = reportService.generateReport("demo");

        assertEquals(1, payload.getReportSummary().getCompatibleCount());
        assertEquals(1, payload.getReportSummary().getPartialCount());
        assertEquals(1, payload.getReportSummary().getIncompatibleCount());
        assertEquals(1, payload.getReportSummary().getUnknownCount());
        assertEquals(4, payload.getInspectionResult().getCompatibilityItems().size());
    }

    @Test
    void shouldRenderHtmlWithTemplateVariables() {
        InspectionResult result = new InspectionResult();
        result.setGeneratedAt(LocalDateTime.of(2026, 3, 20, 19, 15));
        result.setScannedSchema("demo");
        ReportPayload payload = new ReportPayload(result, new ReportSummary());

        String html = reportService.renderHtml(payload);

        assertTrue(html.contains("MySQL 使用对象与 TiDB 兼容性报告"));
        assertTrue(html.contains("demo"));
    }

    @Test
    void shouldExportHtmlToConfiguredDirectory() throws Exception {
        InspectionResult result = new InspectionResult();
        result.setGeneratedAt(LocalDateTime.of(2026, 3, 20, 19, 10, 5));
        result.setScannedSchema("demo");
        ReportPayload payload = new ReportPayload(result, new ReportSummary());

        ReflectionTestUtils.setField(reportService, "outputDir", tempDir.toString());

        Path output = reportService.exportHtml(payload);

        assertTrue(Files.exists(output));
        assertTrue(output.getFileName().toString().startsWith("mysql-tidb-compat-report-20260320-191005"));
        assertTrue(Files.readString(output).contains("MySQL 使用对象与 TiDB 兼容性报告"));
    }
}
