package org.os4all.modules.digitaltwin.fhir.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.os4all.modules.digitaltwin.fhir.dto.FhirParsedPatient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FhirPatientMapper {

    public FhirParsedPatient mapPatientBundle(JsonNode bundle, String sourceFileName) {
        if (bundle == null || !bundle.has("entry") || !bundle.get("entry").isArray()) {
            return null;
        }

        JsonNode patientResource = null;
        List<JsonNode> conditionNodes = new ArrayList<>();
        List<JsonNode> observationNodes = new ArrayList<>();
        List<JsonNode> medicationNodes = new ArrayList<>();
        List<JsonNode> encounterNodes = new ArrayList<>();

        for (JsonNode entry : bundle.get("entry")) {
            JsonNode res = entry.path("resource");
            String resType = res.path("resourceType").asText("");
            switch (resType) {
                case "Patient" -> patientResource = res;
                case "Condition" -> conditionNodes.add(res);
                case "Observation" -> observationNodes.add(res);
                case "MedicationRequest" -> medicationNodes.add(res);
                case "Encounter" -> encounterNodes.add(res);
            }
        }

        if (patientResource == null) {
            return null;
        }

        String fhirId = patientResource.path("id").asText(UUID.randomUUID().toString());
        UUID id = UUID.nameUUIDFromBytes(("synthea-patient-" + fhirId).getBytes());

        // Name
        String given = "";
        String family = "";
        JsonNode nameNode = patientResource.path("name");
        if (nameNode.isArray() && !nameNode.isEmpty()) {
            JsonNode first = nameNode.get(0);
            family = first.path("family").asText("");
            JsonNode givenNode = first.path("given");
            if (givenNode.isArray() && !givenNode.isEmpty()) {
                List<String> givens = new ArrayList<>();
                givenNode.forEach(g -> givens.add(g.asText()));
                given = String.join(" ", givens);
            }
        }
        String fullName = (given + " " + family).trim();
        if (fullName.isEmpty()) fullName = "Synthetic Patient";

        String gender = patientResource.path("gender").asText("unknown").toUpperCase();
        String birthDateStr = patientResource.path("birthDate").asText("");
        LocalDate birthDate = !birthDateStr.isEmpty() ? LocalDate.parse(birthDateStr) : LocalDate.of(1975, 1, 1);
        int age = (int) ChronoUnit.YEARS.between(birthDate, LocalDate.now());

        String email = fhirId.substring(0, Math.min(8, fhirId.length())).toLowerCase() + "@synthea.os4all.test";

        // Map Conditions
        boolean hasT2D = false;
        boolean hasPrediabetes = false;
        boolean hasHypertension = false;
        List<FhirParsedPatient.FhirParsedCondition> parsedConditions = new ArrayList<>();

        for (JsonNode cNode : conditionNodes) {
            String text = cNode.path("code").path("text").asText("");
            String code = cNode.path("code").path("coding").path(0).path("code").asText("");
            String status = cNode.path("clinicalStatus").path("coding").path(0).path("code").asText("active");
            String onsetStr = cNode.path("onsetDateTime").asText("");
            LocalDate onset = null;
            if (onsetStr.length() >= 10) {
                try {
                    onset = LocalDate.parse(onsetStr.substring(0, 10));
                } catch (Exception ignored) {}
            }

            String lower = text.toLowerCase();
            if (lower.contains("type 2") || lower.contains("diabetes mellitus") || code.equals("44054006")) {
                hasT2D = true;
            } else if (lower.contains("prediabetes") || code.equals("15777000")) {
                hasPrediabetes = true;
            }
            if (lower.contains("hypertension") || code.equals("38341003")) {
                hasHypertension = true;
            }

            parsedConditions.add(new FhirParsedPatient.FhirParsedCondition(code, text, status, onset));
        }

        // Map Observations
        BigDecimal heightCm = null;
        BigDecimal weightKg = null;
        BigDecimal bmi = null;
        BigDecimal bpSystolic = null;
        BigDecimal bpDiastolic = null;
        List<FhirParsedPatient.FhirParsedLab> parsedLabs = new ArrayList<>();

        for (JsonNode oNode : observationNodes) {
            String code = oNode.path("code").path("coding").path(0).path("code").asText("");
            String text = oNode.path("code").path("text").asText("");
            String dateIso = oNode.path("effectiveDateTime").asText("");

            // Blood pressure component check
            if (code.equals("85354-9") || text.toLowerCase().contains("blood pressure")) {
                JsonNode components = oNode.path("component");
                if (components.isArray()) {
                    for (JsonNode comp : components) {
                        String compText = comp.path("code").path("text").asText("").toLowerCase();
                        double val = comp.path("valueQuantity").path("value").asDouble(0.0);
                        if (val > 0) {
                            if (compText.contains("systolic")) bpSystolic = BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP);
                            else if (compText.contains("diastolic")) bpDiastolic = BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP);
                        }
                    }
                }
                continue;
            }

            // Normal quantity observations
            JsonNode vq = oNode.path("valueQuantity");
            if (!vq.has("value") || vq.path("value").isNull()) continue;

            BigDecimal val = BigDecimal.valueOf(vq.path("value").asDouble()).setScale(2, RoundingMode.HALF_UP);
            String unit = vq.path("unit").asText("");

            if (code.equals("8302-2") || text.toLowerCase().contains("body height")) {
                heightCm = val;
            } else if (code.equals("29463-7") || text.toLowerCase().contains("body weight")) {
                weightKg = val;
            } else if (code.equals("39156-5") || text.toLowerCase().contains("body mass index")) {
                bmi = val;
            } else {
                // Collect relevant metabolic & lab biomarkers
                parsedLabs.add(new FhirParsedPatient.FhirParsedLab(code, text, val, unit, dateIso, null, null));
            }
        }

        if (bmi == null && heightCm != null && weightKg != null && heightCm.doubleValue() > 0) {
            double hMeters = heightCm.doubleValue() / 100.0;
            bmi = BigDecimal.valueOf(weightKg.doubleValue() / (hMeters * hMeters)).setScale(1, RoundingMode.HALF_UP);
        }

        // Map Medications
        List<String> medications = new ArrayList<>();
        for (JsonNode mNode : medicationNodes) {
            String medName = mNode.path("medicationCodeableConcept").path("text").asText("");
            if (!medName.isBlank() && !medications.contains(medName)) {
                medications.add(medName);
            }
        }

        // Map Encounters
        List<FhirParsedPatient.FhirParsedEncounter> parsedEncounters = new ArrayList<>();
        for (JsonNode eNode : encounterNodes) {
            String type = eNode.path("type").path(0).path("text").asText("Encounter");
            String start = eNode.path("period").path("start").asText("");
            parsedEncounters.add(new FhirParsedPatient.FhirParsedEncounter(type, start));
        }

        return new FhirParsedPatient(
                id,
                fhirId,
                sourceFileName,
                fullName,
                email,
                gender,
                birthDate,
                age,
                heightCm,
                weightKg,
                bmi,
                bpSystolic,
                bpDiastolic,
                hasT2D,
                hasPrediabetes,
                hasHypertension,
                parsedConditions,
                parsedLabs,
                medications,
                parsedEncounters
        );
    }
}
