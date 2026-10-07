package org.os4all.modules.timeline.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class TimelineItemResponse {

    private UUID id;
    private String itemType; // "OBSERVATION" or "LAB_REPORT"
    private String title;
    private String subtitle;
    private String category;
    private Instant timestamp;
    private String source;
    private Map<String, Object> details;

    public TimelineItemResponse() {
    }

    public TimelineItemResponse(UUID id, String itemType, String title, String subtitle, String category, Instant timestamp, String source, Map<String, Object> details) {
        this.id = id;
        this.itemType = itemType;
        this.title = title;
        this.subtitle = subtitle;
        this.category = category;
        this.timestamp = timestamp;
        this.source = source;
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }
}
