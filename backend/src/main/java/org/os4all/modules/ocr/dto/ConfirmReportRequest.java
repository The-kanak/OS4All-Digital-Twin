package org.os4all.modules.ocr.dto;

import java.util.List;

public class ConfirmReportRequest {

    private String reportTitle;
    private String laboratoryName;
    private List<ConfirmReportItemRequest> items;

    public ConfirmReportRequest() {
    }

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getLaboratoryName() {
        return laboratoryName;
    }

    public void setLaboratoryName(String laboratoryName) {
        this.laboratoryName = laboratoryName;
    }

    public List<ConfirmReportItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ConfirmReportItemRequest> items) {
        this.items = items;
    }
}
