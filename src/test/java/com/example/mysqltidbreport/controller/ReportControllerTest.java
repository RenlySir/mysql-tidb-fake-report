package com.example.mysqltidbreport.controller;

import com.example.mysqltidbreport.model.InspectionResult;
import com.example.mysqltidbreport.model.ReportPayload;
import com.example.mysqltidbreport.model.ReportSummary;
import com.example.mysqltidbreport.service.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Test
    void shouldRenderReportViewAndAttachModel() {
        InspectionResult result = new InspectionResult();
        ReportSummary summary = new ReportSummary();
        ReportPayload payload = new ReportPayload(result, summary);

        when(reportService.generateReport("demo")).thenReturn(payload);

        ExtendedModelMap model = new ExtendedModelMap();
        String view = reportController.report("demo", model);

        assertEquals("report", view);
        assertEquals(result, model.getAttribute("result"));
        assertEquals(summary, model.getAttribute("summary"));
    }

    @Test
    void shouldExportReportSuccessfully() throws Exception {
        ReportPayload payload = new ReportPayload(new InspectionResult(), new ReportSummary());
        when(reportService.generateReport("demo")).thenReturn(payload);
        when(reportService.exportHtml(payload)).thenReturn(Path.of("/tmp/report.html"));

        String body = reportController.export("demo").getBody();

        assertEquals("HTML 报告已生成: /tmp/report.html", body);
    }

    @Test
    void shouldWrapIOExceptionAsResponseStatusException() throws Exception {
        ReportPayload payload = new ReportPayload(new InspectionResult(), new ReportSummary());
        when(reportService.generateReport("demo")).thenReturn(payload);
        when(reportService.exportHtml(payload)).thenThrow(new IOException("disk full"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> reportController.export("demo"));

        assertEquals(500, ex.getStatusCode().value());
    }
}
