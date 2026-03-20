package com.example.mysqltidbreport.controller;

import com.example.mysqltidbreport.model.ReportPayload;
import com.example.mysqltidbreport.service.ReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

@Controller
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping({"/", "/report"})
    public String report(@RequestParam(required = false) String schema, Model model) {
        ReportPayload payload = reportService.generateReport(schema);
        model.addAttribute("result", payload.getInspectionResult());
        model.addAttribute("summary", payload.getReportSummary());
        return "report";
    }

    @GetMapping("/report/export")
    public ResponseEntity<String> export(@RequestParam(required = false) String schema) {
        ReportPayload payload = reportService.generateReport(schema);
        try {
            Path file = reportService.exportHtml(payload);
            String body = "HTML 报告已生成: " + file.toAbsolutePath();
            return ResponseEntity.ok(body);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "导出 HTML 失败: " + e.getMessage(), e);
        }
    }
}
