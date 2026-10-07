package org.os4all.modules.timeline.service;

import org.os4all.core.exception.ResourceNotFoundException;
import org.os4all.modules.ingestion.entity.*;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.lab.entity.LabReport;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.timeline.dto.TimelineItemResponse;
import org.os4all.modules.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class TimelineService {

    private final HealthObservationRepository observationRepository;
    private final LabReportRepository labReportRepository;
    private final UserRepository userRepository;

    public TimelineService(
            HealthObservationRepository observationRepository,
            LabReportRepository labReportRepository,
            UserRepository userRepository
    ) {
        this.observationRepository = observationRepository;
        this.labReportRepository = labReportRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<TimelineItemResponse> getUnifiedTimeline(
            UUID userId,
            Instant startDate,
            Instant endDate,
            String filterType, // "ALL", "OBSERVATIONS", "LABS", "VITALS", "LIFESTYLE", "SYMPTOMS"
            int limit
    ) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        List<TimelineItemResponse> items = new ArrayList<>();
        String filter = filterType != null ? filterType.trim().toUpperCase(Locale.ROOT) : "ALL";

        // Collect Observations
        if (!"LABS".equals(filter)) {
            List<HealthObservation> obsList = observationRepository.findTimelineObservations(userId, startDate, endDate);
            for (HealthObservation obs : obsList) {
                if ("VITALS".equals(filter) && !(obs instanceof VitalMeasurement)) continue;
                if ("LIFESTYLE".equals(filter) && !(obs instanceof LifestyleObservation)) continue;
                if ("SYMPTOMS".equals(filter) && !(obs instanceof SymptomObservation)) continue;

                Map<String, Object> details = new HashMap<>();
                details.put("observationType", obs.getObservationType().name());
                details.put("valueNumeric", obs.getValueNumeric());
                details.put("valueText", obs.getValueText());
                details.put("unit", obs.getUnit());
                details.put("standardValueNumeric", obs.getStandardValueNumeric());
                details.put("standardUnit", obs.getStandardUnit());
                details.put("confidence", obs.getConfidence());

                String title;
                String subtitle;
                String category;

                if (obs instanceof VitalMeasurement vital) {
                    category = "VITAL";
                    title = vital.getVitalName() != null ? vital.getVitalName() : vital.getObservationType().name();
                    subtitle = (vital.getValueNumeric() != null ? vital.getValueNumeric() + " " + (vital.getUnit() != null ? vital.getUnit() : "") : "")
                            + (vital.getDeviceModel() != null ? " (" + vital.getDeviceModel() + ")" : "");
                    details.put("deviceModel", vital.getDeviceModel());
                } else if (obs instanceof LifestyleObservation lifestyle) {
                    category = "LIFESTYLE";
                    title = lifestyle.getLifestyleCategory() != null ? lifestyle.getLifestyleCategory() : lifestyle.getObservationType().name();
                    subtitle = (lifestyle.getDurationMinutes() != null ? lifestyle.getDurationMinutes() + " mins" : "")
                            + (lifestyle.getValueText() != null ? " - " + lifestyle.getValueText() : "");
                    details.put("durationMinutes", lifestyle.getDurationMinutes());
                } else if (obs instanceof SymptomObservation symptom) {
                    category = "SYMPTOM";
                    title = symptom.getSymptomName() != null ? symptom.getSymptomName() : "Symptom Log";
                    subtitle = (symptom.getSeverity() != null ? "Severity: " + symptom.getSeverity() : "")
                            + (symptom.getBodySite() != null ? " at " + symptom.getBodySite() : "");
                    details.put("severity", symptom.getSeverity());
                    details.put("bodySite", symptom.getBodySite());
                } else {
                    category = "OBSERVATION";
                    title = obs.getObservationType().name();
                    subtitle = obs.getValueNumeric() != null ? obs.getValueNumeric() + " " + obs.getUnit() : obs.getValueText();
                }

                items.add(new TimelineItemResponse(
                        obs.getId(),
                        "OBSERVATION",
                        title,
                        subtitle,
                        category,
                        obs.getTimestamp(),
                        obs.getSource(),
                        details
                ));
            }
        }

        // Collect Lab Reports
        if (!"VITALS".equals(filter) && !"LIFESTYLE".equals(filter) && !"SYMPTOMS".equals(filter) && !"OBSERVATIONS".equals(filter)) {
            List<LabReport> labList = labReportRepository.findTimelineReports(userId, startDate, endDate);
            for (LabReport report : labList) {
                Map<String, Object> details = new HashMap<>();
                details.put("laboratoryName", report.getLaboratoryName());
                details.put("reportTitle", report.getReportTitle());
                details.put("notes", report.getNotes());

                List<Map<String, Object>> biomarkerItems = new ArrayList<>();
                if (report.getResults() != null) {
                    for (LabResult res : report.getResults()) {
                        // Never silently convert unconfirmed OCR results into trusted medical timeline
                        if (Boolean.FALSE.equals(res.getIsConfirmed())) {
                            continue;
                        }
                        Map<String, Object> bm = new HashMap<>();
                        bm.put("biomarker", res.getBiomarker());
                        bm.put("standardizedBiomarker", res.getStandardizedBiomarker());
                        bm.put("value", res.getValue());
                        bm.put("unit", res.getUnit());
                        bm.put("referenceLow", res.getReferenceLow());
                        bm.put("referenceHigh", res.getReferenceHigh());
                        biomarkerItems.add(bm);
                    }
                }
                if (biomarkerItems.isEmpty() && report.getResults() != null && !report.getResults().isEmpty()) {
                    // All biomarkers are unconfirmed / pending review
                    continue;
                }
                details.put("biomarkers", biomarkerItems);

                String subtitle = report.getLaboratoryName() != null ?
                        report.getLaboratoryName() + " — " + biomarkerItems.size() + " biomarkers" :
                        biomarkerItems.size() + " biomarkers tested";

                items.add(new TimelineItemResponse(
                        report.getId(),
                        "LAB_REPORT",
                        report.getReportTitle(),
                        subtitle,
                        "LAB",
                        report.getCollectionDate(),
                        report.getSource(),
                        details
                ));
            }
        }

        // Sort unified timeline strictly descending by event timestamp
        items.sort(Comparator.comparing(TimelineItemResponse::getTimestamp).reversed());

        int max = limit > 0 ? Math.min(limit, items.size()) : items.size();
        return items.subList(0, max);
    }
}
