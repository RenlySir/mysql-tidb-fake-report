package com.example.mysqltidbreport.service;

import com.example.mysqltidbreport.model.CompatibilityItem;
import com.example.mysqltidbreport.model.InspectionResult;
import com.example.mysqltidbreport.model.ReportPayload;
import com.example.mysqltidbreport.model.ReportSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final MySqlInspectionService inspectionService;
    private final TidbCompatibilityAnalyzer compatibilityAnalyzer;
    private final SpringTemplateEngine templateEngine;

    @Value("${report.output-dir:./report-output}")
    private String outputDir;

    public ReportService(MySqlInspectionService inspectionService,
                         TidbCompatibilityAnalyzer compatibilityAnalyzer,
                         SpringTemplateEngine templateEngine) {
        this.inspectionService = inspectionService;
        this.compatibilityAnalyzer = compatibilityAnalyzer;
        this.templateEngine = templateEngine;
    }

    public ReportPayload generateReport(String schema) {
        InspectionResult result = inspectionService.inspect(schema);
        List<CompatibilityItem> compatibilityItems = compatibilityAnalyzer.analyze(result);
        result.setCompatibilityItems(compatibilityItems);

        ReportSummary summary = new ReportSummary();
        for (CompatibilityItem item : compatibilityItems) {
            String status = item.getTidbCompatibility();
            if ("COMPATIBLE".equalsIgnoreCase(status)) {
                summary.setCompatibleCount(summary.getCompatibleCount() + 1);
            } else if ("PARTIAL".equalsIgnoreCase(status)) {
                summary.setPartialCount(summary.getPartialCount() + 1);
            } else if ("INCOMPATIBLE".equalsIgnoreCase(status)) {
                summary.setIncompatibleCount(summary.getIncompatibleCount() + 1);
            } else {
                summary.setUnknownCount(summary.getUnknownCount() + 1);
            }
        }

        return new ReportPayload(result, summary);
    }

    public String renderHtml(ReportPayload payload) {
        Context context = new Context();
        context.setVariable("result", payload.getInspectionResult());
        context.setVariable("summary", payload.getReportSummary());
        return templateEngine.process("report", context);
    }

    public Path exportHtml(ReportPayload payload) throws IOException {
        String timestamp = payload.getInspectionResult().getGeneratedAt().format(FILE_TS);
        Path outputDirectory = Paths.get(outputDir).toAbsolutePath().normalize();
        Files.createDirectories(outputDirectory);

        Path outputFile = outputDirectory.resolve("mysql-tidb-compat-report-" + timestamp + ".html");
        Files.writeString(outputFile, renderHtml(payload), StandardCharsets.UTF_8);
        return outputFile;
    }
}
