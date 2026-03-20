package com.example.mysqltidbreport.model;

public class ReportPayload {

    private InspectionResult inspectionResult;
    private ReportSummary reportSummary;

    public ReportPayload(InspectionResult inspectionResult, ReportSummary reportSummary) {
        this.inspectionResult = inspectionResult;
        this.reportSummary = reportSummary;
    }

    public InspectionResult getInspectionResult() {
        return inspectionResult;
    }

    public void setInspectionResult(InspectionResult inspectionResult) {
        this.inspectionResult = inspectionResult;
    }

    public ReportSummary getReportSummary() {
        return reportSummary;
    }

    public void setReportSummary(ReportSummary reportSummary) {
        this.reportSummary = reportSummary;
    }
}
