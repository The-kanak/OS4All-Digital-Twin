package org.os4all.modules.digitaltwin.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.os4all.modules.digitaltwin.fhir.dto.FhirParsedPatient;
import org.os4all.modules.digitaltwin.fhir.mapper.FhirPatientMapper;
import org.os4all.modules.digitaltwin.fhir.service.FhirImportService;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Synthea FHIR Ingestion & Mapping Integration Tests")
class FhirImportIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FhirPatientMapper patientMapper = new FhirPatientMapper();

    @Test
    @DisplayName("Parse synthetic FHIR bundle and extract demographics, conditions, HbA1c, and BP")
    void testParseFhirBundleSample() throws Exception {
        // Sample FHIR bundle JSON modeled after Synthea output
        String sampleBundleJson = """
        {
          "resourceType": "Bundle",
          "entry": [
            {
              "resource": {
                "resourceType": "Patient",
                "id": "synthetic-sample-001",
                "gender": "female",
                "birthDate": "1965-04-12",
                "name": [
                  {
                    "family": "Morales",
                    "given": ["Elena", "Rosa"]
                  }
                ]
              }
            },
            {
              "resource": {
                "resourceType": "Condition",
                "clinicalStatus": { "coding": [{ "code": "active" }] },
                "code": {
                  "coding": [{ "code": "44054006", "system": "http://snomed.info/sct" }],
                  "text": "Diabetes mellitus type 2 (disorder)"
                },
                "onsetDateTime": "2018-06-20T10:00:00Z"
              }
            },
            {
              "resource": {
                "resourceType": "Condition",
                "clinicalStatus": { "coding": [{ "code": "active" }] },
                "code": {
                  "coding": [{ "code": "38341003", "system": "http://snomed.info/sct" }],
                  "text": "Essential hypertension (disorder)"
                },
                "onsetDateTime": "2015-02-14T08:30:00Z"
              }
            },
            {
              "resource": {
                "resourceType": "Observation",
                "code": {
                  "coding": [{ "code": "4548-4" }],
                  "text": "Hemoglobin A1c/Hemoglobin.total in Blood"
                },
                "valueQuantity": {
                  "value": 7.42,
                  "unit": "%"
                },
                "effectiveDateTime": "2024-05-15T09:00:00Z"
              }
            },
            {
              "resource": {
                "resourceType": "Observation",
                "code": {
                  "coding": [{ "code": "2339-0" }],
                  "text": "Glucose [Mass/volume] in Blood"
                },
                "valueQuantity": {
                  "value": 142.0,
                  "unit": "mg/dL"
                },
                "effectiveDateTime": "2024-05-15T09:00:00Z"
              }
            },
            {
              "resource": {
                "resourceType": "Observation",
                "code": {
                  "coding": [{ "code": "85354-9" }],
                  "text": "Blood pressure panel with all children optional"
                },
                "component": [
                  {
                    "code": { "text": "Systolic Blood Pressure" },
                    "valueQuantity": { "value": 134.0, "unit": "mm[Hg]" }
                  },
                  {
                    "code": { "text": "Diastolic Blood Pressure" },
                    "valueQuantity": { "value": 84.0, "unit": "mm[Hg]" }
                  }
                ],
                "effectiveDateTime": "2024-05-15T09:00:00Z"
              }
            },
            {
              "resource": {
                "resourceType": "MedicationRequest",
                "medicationCodeableConcept": {
                  "text": "Metformin hydrochloride 500 MG Oral Tablet"
                }
              }
            }
          ]
        }
        """;

        JsonNode root = objectMapper.readTree(sampleBundleJson);
        FhirParsedPatient patient = patientMapper.mapPatientBundle(root, "sample-bundle.json");

        assertNotNull(patient);
        assertEquals("Elena Rosa Morales", patient.fullName());
        assertEquals("FEMALE", patient.gender());
        assertEquals("1965-04-12", patient.birthDate().toString());
        assertTrue(patient.hasType2Diabetes(), "Should detect Type 2 Diabetes condition");
        assertTrue(patient.hasHypertension(), "Should detect Hypertension");

        // Verify blood pressure
        assertNotNull(patient.bloodPressureSystolic());
        assertEquals(134.0, patient.bloodPressureSystolic().doubleValue());
        assertEquals(84.0, patient.bloodPressureDiastolic().doubleValue());

        // Verify labs
        assertEquals(2, patient.labs().size());
        assertTrue(patient.labs().stream().anyMatch(l -> l.name().contains("Hemoglobin A1c") && l.value().doubleValue() == 7.42));
        assertTrue(patient.labs().stream().anyMatch(l -> l.name().contains("Glucose") && l.value().doubleValue() == 142.0));

        // Verify medication
        assertEquals(1, patient.medications().size());
        assertEquals("Metformin hydrochloride 500 MG Oral Tablet", patient.medications().get(0));
    }

    @Test
    @DisplayName("Verify candidate scanning against external Synthea directory if present")
    void testScanSyntheaCandidateDirectory() {
        File dir = new File("D:/Projects/synthea-master/output/fhir");
        if (dir.exists() && dir.isDirectory()) {
            FhirImportService service = new FhirImportService(
                    "D:/Projects/synthea-master/output/fhir",
                    objectMapper, patientMapper, null, null, null, null, null, null
            );
            List<FhirParsedPatient> candidates = service.scanCandidatePatients("D:/Projects/synthea-master/output/fhir");
            assertNotNull(candidates);
            assertTrue(candidates.size() >= 10, "Should discover candidate synthetic patients from Synthea");
            assertTrue(candidates.stream().anyMatch(FhirParsedPatient::hasType2Diabetes), "Should discover at least one Type 2 Diabetes patient");
        }
    }

    @Test
    @DisplayName("Verify Shara Senger details from Synthea bundle")
    void testFindSharaSenger() {
        File dir = new File("D:/Projects/synthea-master/output/fhir");
        if (dir.exists() && dir.isDirectory()) {
            FhirImportService service = new FhirImportService(
                    "D:/Projects/synthea-master/output/fhir",
                    objectMapper, patientMapper, null, null, null, null, null, null
            );
            List<FhirParsedPatient> candidates = service.scanCandidatePatients("D:/Projects/synthea-master/output/fhir");
            for (FhirParsedPatient p : candidates) {
                System.out.println("CANDIDATE: " + p.fullName() + " ID=" + p.id() + " T2D=" + p.hasType2Diabetes() + " Labs=" + p.labs().size());
            }
            FhirParsedPatient shara = candidates.stream()
                    .filter(p -> p.fullName().contains("Shara") || p.fullName().contains("Senger"))
                    .findFirst()
                    .orElse(null);
            assertNotNull(shara, "Shara Senger should be found in Synthea candidates");
            System.out.println("FOUND SHARA SENGER: " + shara.fullName() + " ID: " + shara.id() + " T2D: " + shara.hasType2Diabetes());
        }
    }
}
