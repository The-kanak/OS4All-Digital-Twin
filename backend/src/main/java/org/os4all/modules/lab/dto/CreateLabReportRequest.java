package org.os4all.modules.lab.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class CreateLabReportRequest {

    @NotBlank(message = "Report title is required")
    private String reportTitle;

    private String laboratoryName;

    @NotNull(message = "Collection date is required")
    private Instant collectionDate;

    private Instant reportedDate;

    @NotBlank(message = "Source is required")
    private String source;

    private String notes;

    @NotEmpty(message = "At least one lab result item is required")
    @Valid
    private List<CreateLabResultItemRequest> results = new ArrayList<>();

    public CreateLabReportRequest() {
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

    public Instant getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Instant collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Instant getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(Instant reportedDate) {
        this.reportedDate = reportedDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<CreateLabResultItemRequest> getResults() {
        return results;
    }

    public void setResults(List<CreateLabResultItemRequest> results) {
        this.results = results;
    }
}
