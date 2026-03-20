package com.example.mysqltidbreport.model;

public class ReportSummary {

    private int compatibleCount;
    private int partialCount;
    private int incompatibleCount;
    private int unknownCount;

    public int getCompatibleCount() {
        return compatibleCount;
    }

    public void setCompatibleCount(int compatibleCount) {
        this.compatibleCount = compatibleCount;
    }

    public int getPartialCount() {
        return partialCount;
    }

    public void setPartialCount(int partialCount) {
        this.partialCount = partialCount;
    }

    public int getIncompatibleCount() {
        return incompatibleCount;
    }

    public void setIncompatibleCount(int incompatibleCount) {
        this.incompatibleCount = incompatibleCount;
    }

    public int getUnknownCount() {
        return unknownCount;
    }

    public void setUnknownCount(int unknownCount) {
        this.unknownCount = unknownCount;
    }
}
