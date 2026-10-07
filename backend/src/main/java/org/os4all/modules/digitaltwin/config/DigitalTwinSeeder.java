package org.os4all.modules.digitaltwin.config;

import org.os4all.modules.digitaltwin.entity.HistoricalMedicalRecord;
import org.os4all.modules.digitaltwin.repository.HistoricalMedicalRecordRepository;
import org.os4all.modules.digitaltwin.service.DigitalTwinService;
import org.os4all.modules.digitaltwin.service.DigitalTwinSimulationEngine;
import org.os4all.modules.ingestion.repository.HealthObservationRepository;
import org.os4all.modules.lab.entity.LabReport;
import org.os4all.modules.lab.entity.LabResult;
import org.os4all.modules.lab.repository.LabReportRepository;
import org.os4all.modules.normalization.UnitNormalizationService;
import org.os4all.modules.user.entity.Role;
import org.os4all.modules.user.entity.User;
import org.os4all.modules.user.entity.UserProfile;
import org.os4all.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Configuration
public class DigitalTwinSeeder {

    private static final Logger log = LoggerFactory.getLogger(DigitalTwinSeeder.class);

    public static final UUID PATIENT_1_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    public static final UUID PATIENT_2_ID = UUID.fromString("22222222-3333-4444-5555-666666666666");
    public static final UUID PATIENT_3_ID = UUID.fromString("33333333-4444-5555-6666-777777777777");

    @Bean
    @Order(10)
    @Profile("!test")
    public CommandLineRunner seedDigitalTwinPatients(
            UserRepository userRepository,
            HistoricalMedicalRecordRepository medicalRecordRepository,
            LabReportRepository labReportRepository,
            UnitNormalizationService normService,
            PasswordEncoder passwordEncoder,
            DigitalTwinSimulationEngine simulationEngine,
            DigitalTwinService digitalTwinService,
            org.os4all.modules.digitaltwin.fhir.service.FhirImportService fhirImportService
    ) {
        return args -> {
            log.info("Initializing OS4All Digital Twin synthetic patient cohorts...");

            // 1. Patient 1: Alex Rivera (38M, Prediabetes Risk Profile)
            User p1 = getOrCreatePatient(
                    userRepository, passwordEncoder, PATIENT_1_ID,
                    "alex.rivera@demo.os4all.test", "Alex Rivera (Virtual Twin)",
                    LocalDate.of(1988, 6, 15), "MALE",
                    new BigDecimal("178.5"), new BigDecimal("78.5"), "O+",
                    "Desk worker, high cognitive stress, variable sleep routine (5.5–7 hrs), occasional running."
            );
            seedRecordsForAlex(medicalRecordRepository, p1);
            seedLabsForAlex(labReportRepository, normService, p1);

            // 2. Patient 2: Sarah Chen (44F, Metabolic Syndrome & Autonomic Strain)
            User p2 = getOrCreatePatient(
                    userRepository, passwordEncoder, PATIENT_2_ID,
                    "sarah.chen@demo.os4all.test", "Sarah Chen (Virtual Twin)",
                    LocalDate.of(1982, 3, 22), "FEMALE",
                    new BigDecimal("165.0"), new BigDecimal("68.0"), "A+",
                    "Corporate manager, chronic nocturnal sleep fragmentation, low physical activity."
            );
            seedRecordsForSarah(medicalRecordRepository, p2);
            seedLabsForSarah(labReportRepository, normService, p2);

            // 3. Patient 3: Marcus Johnson (52M, Insulin Resistance & Early Dysglycemia)
            User p3 = getOrCreatePatient(
                    userRepository, passwordEncoder, PATIENT_3_ID,
                    "marcus.johnson@demo.os4all.test", "Marcus Johnson (Virtual Twin)",
                    LocalDate.of(1974, 11, 8), "MALE",
                    new BigDecimal("182.0"), new BigDecimal("91.0"), "B+",
                    "High stress executive, family history of T2D, sedentary schedule, frequent late dinners."
            );
            seedRecordsForMarcus(medicalRecordRepository, p3);
            seedLabsForMarcus(labReportRepository, normService, p3);

            // Seed initial longitudinal baseline & calculate Digital Twin states
            simulationEngine.injectScenario(p1.getId(), "STABLE_PATIENT");
            simulationEngine.injectScenario(p2.getId(), "POOR_SLEEP");
            simulationEngine.injectScenario(p3.getId(), "GLUCOSE_SPIKE");

            // 4. Ingest curated Synthea FHIR cohorts (including Shara Senger)
            try {
                fhirImportService.importSyntheaCohort(null, true);
                log.info("Curated Synthea FHIR cohorts successfully imported on startup.");
            } catch (Exception e) {
                log.warn("Synthea FHIR cohort import on startup: {}", e.getMessage());
            }

            log.info("OS4All Digital Twin cohorts successfully calibrated with longitudinal telemetry.");
        };
    }

    private User getOrCreatePatient(
            UserRepository repo, PasswordEncoder encoder, UUID id,
            String email, String fullName, LocalDate dob, String sex,
            BigDecimal heightCm, BigDecimal weightKg, String bloodType, String notes
    ) {
        return repo.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setId(id);
            u.setEmail(email);
            u.setFullName(fullName);
            u.setPasswordHash(encoder.encode("DemoPassword123!"));
            u.setRole(Role.ROLE_USER);
            u.setActive(true);

            UserProfile profile = new UserProfile();
            profile.setUser(u);
            profile.setDateOfBirth(dob);
            profile.setBiologicalSex(sex);
            profile.setHeightCm(heightCm);
            profile.setWeightKg(weightKg);
            profile.setBloodType(bloodType);
            profile.setLifestyleNotes(notes);
            u.setProfile(profile);

            return repo.save(u);
        });
    }

    private void seedRecordsForAlex(HistoricalMedicalRecordRepository repo, User user) {
        if (repo.countByUserId(user.getId()) > 0) return;

        createRecord(repo, user, "CONDITION", "Impaired Fasting Glucose (Prediabetes)", "R73.01", "MILD", "ACTIVE",
                LocalDate.of(2025, 4, 10), "Metformin 500mg daily (intermittent)", "Mother: Type 2 Diabetes diagnosed age 58",
                "Fasting blood glucose borderline elevated. Recommended lifestyle modifications and continuous tracking.");

        createRecord(repo, user, "FAMILY_HISTORY", "Maternal Diabetes Mellitus & Cardiovascular Disease", "Z83.3", "MODERATE", "CHRONIC",
                LocalDate.of(2023, 1, 15), "None", "Maternal grandmother: CAD; Mother: T2D",
                "Strong genetic predisposition to metabolic drift under chronic sleep deprivation.");

        createRecord(repo, user, "MEDICATION", "Omega-3 Fatty Acids 1000mg & Vitamin D3 2000IU", null, "MILD", "ACTIVE",
                LocalDate.of(2024, 8, 20), "Omega-3 1000mg, Vitamin D3 2000IU", "None",
                "Daily nutritional supplementation for cardiovascular and autonomic support.");
    }

    private void seedRecordsForSarah(HistoricalMedicalRecordRepository repo, User user) {
        if (repo.countByUserId(user.getId()) > 0) return;

        createRecord(repo, user, "CONDITION", "Essential Hypertension & Autonomic Strain", "I10", "MODERATE", "ACTIVE",
                LocalDate.of(2024, 2, 14), "Lisinopril 10mg daily", "Father: Hypertension at age 50",
                "Blood pressure fluctuates with work stress and accumulated sleep deficit.");

        createRecord(repo, user, "CONDITION", "Nocturnal Sleep Fragmentation & Insomnia", "G47.00", "MODERATE", "ACTIVE",
                LocalDate.of(2024, 9, 5), "Melatonin 3mg PRN", "Sister: Sleep disturbances",
                "Reported persistent difficulty maintaining sleep beyond 5.5 hours under corporate deadlines.");
    }

    private void seedRecordsForMarcus(HistoricalMedicalRecordRepository repo, User user) {
        if (repo.countByUserId(user.getId()) > 0) return;

        createRecord(repo, user, "CONDITION", "Early Type 2 Dysglycemia & Hyperinsulinemia", "E11.9", "MODERATE", "ACTIVE",
                LocalDate.of(2023, 10, 18), "Metformin 850mg twice daily", "Both parents: Type 2 Diabetes",
                "Elevated post-meal glycemic excursions. High sensitivity to carbohydrate loads.");

        createRecord(repo, user, "CONDITION", "Hyperlipidemia (Elevated Triglycerides)", "E78.1", "MODERATE", "ACTIVE",
                LocalDate.of(2024, 1, 12), "Atorvastatin 20mg daily", "Father: Myocardial infarction age 62",
                "Lipid profile shows elevated triglycerides and borderline HDL.");
    }

    private void createRecord(
            HistoricalMedicalRecordRepository repo, User user, String type, String condition,
            String icd10, String severity, String status, LocalDate diagnosed,
            String meds, String family, String notes
    ) {
        HistoricalMedicalRecord r = new HistoricalMedicalRecord();
        r.setUser(user);
        r.setRecordType(type);
        r.setConditionOrDiagnosis(condition);
        r.setIcd10Code(icd10);
        r.setSeverity(severity);
        r.setStatus(status);
        r.setDiagnosedDate(diagnosed);
        r.setMedications(meds);
        r.setFamilyHistoryNotes(family);
        r.setClinicalNotes(notes);
        repo.save(r);
    }

    private void seedLabsForAlex(LabReportRepository repo, UnitNormalizationService norm, User user) {
        if (repo.countByUserId(user.getId()) > 0) return;

        LabReport report = new LabReport();
        report.setUser(user);
        report.setReportTitle("Comprehensive Metabolic & Glycemic Panel");
        report.setLaboratoryName("BioPath Clinical Diagnostics (Synthetic)");
        report.setCollectionDate(Instant.now().minus(14, ChronoUnit.DAYS));
        report.setReportedDate(Instant.now().minus(13, ChronoUnit.DAYS));
        report.setSource("SYNTHETIC_LAB_EHR");
        report.setNotes("Baseline metabolic screening. Fasting duration 10 hours.");

        addLab(report, user, norm, "HbA1c", new BigDecimal("5.6"), "%");
        addLab(report, user, norm, "glucose", new BigDecimal("98.0"), "mg/dL");
        addLab(report, user, norm, "fasting_glucose", new BigDecimal("96.0"), "mg/dL");
        addLab(report, user, norm, "post_meal_glucose", new BigDecimal("132.0"), "mg/dL");
        addLab(report, user, norm, "creatinine", new BigDecimal("0.95"), "mg/dL");
        addLab(report, user, norm, "ALT/SGPT", new BigDecimal("26.0"), "U/L");
        addLab(report, user, norm, "AST", new BigDecimal("22.0"), "U/L");
        addLab(report, user, norm, "bilirubin", new BigDecimal("0.7"), "mg/dL");
        addLab(report, user, norm, "cholesterol", new BigDecimal("188.0"), "mg/dL");
        addLab(report, user, norm, "triglycerides", new BigDecimal("128.0"), "mg/dL");

        repo.save(report);
    }

    private void seedLabsForSarah(LabReportRepository repo, UnitNormalizationService norm, User user) {
        if (repo.countByUserId(user.getId()) > 0) return;

        LabReport report = new LabReport();
        report.setUser(user);
        report.setReportTitle("Metabolic & Endocrine Panel");
        report.setLaboratoryName("BioPath Clinical Diagnostics (Synthetic)");
        report.setCollectionDate(Instant.now().minus(20, ChronoUnit.DAYS));
        report.setReportedDate(Instant.now().minus(19, ChronoUnit.DAYS));
        report.setSource("SYNTHETIC_LAB_EHR");
        report.setNotes("Evaluation of cardiovascular and autonomic recovery indices.");

        addLab(report, user, norm, "HbA1c", new BigDecimal("5.8"), "%");
        addLab(report, user, norm, "glucose", new BigDecimal("104.0"), "mg/dL");
        addLab(report, user, norm, "creatinine", new BigDecimal("0.88"), "mg/dL");
        addLab(report, user, norm, "ALT/SGPT", new BigDecimal("28.0"), "U/L");
        addLab(report, user, norm, "cholesterol", new BigDecimal("210.0"), "mg/dL");
        addLab(report, user, norm, "triglycerides", new BigDecimal("155.0"), "mg/dL");

        repo.save(report);
    }

    private void seedLabsForMarcus(LabReportRepository repo, UnitNormalizationService norm, User user) {
        if (repo.countByUserId(user.getId()) > 0) return;

        LabReport report = new LabReport();
        report.setUser(user);
        report.setReportTitle("Lipid & Glycemic Profiling");
        report.setLaboratoryName("BioPath Clinical Diagnostics (Synthetic)");
        report.setCollectionDate(Instant.now().minus(25, ChronoUnit.DAYS));
        report.setReportedDate(Instant.now().minus(24, ChronoUnit.DAYS));
        report.setSource("SYNTHETIC_LAB_EHR");
        report.setNotes("Quarterly dysglycemia follow-up.");

        addLab(report, user, norm, "HbA1c", new BigDecimal("6.2"), "%");
        addLab(report, user, norm, "glucose", new BigDecimal("118.0"), "mg/dL");
        addLab(report, user, norm, "post_meal_glucose", new BigDecimal("158.0"), "mg/dL");
        addLab(report, user, norm, "creatinine", new BigDecimal("1.05"), "mg/dL");
        addLab(report, user, norm, "ALT/SGPT", new BigDecimal("38.0"), "U/L");
        addLab(report, user, norm, "cholesterol", new BigDecimal("230.0"), "mg/dL");
        addLab(report, user, norm, "triglycerides", new BigDecimal("195.0"), "mg/dL");

        repo.save(report);
    }

    private void addLab(LabReport report, User user, UnitNormalizationService normService, String biomarker, BigDecimal val, String unit) {
        var n = normService.normalizeBiomarker(biomarker, val, unit, null, null);
        LabResult res = new LabResult();
        res.setUser(user);
        res.setBiomarker(biomarker);
        res.setStandardizedBiomarker(n.canonicalName());
        res.setValue(val);
        res.setUnit(unit);
        res.setStandardValue(n.normalizedValue());
        res.setStandardUnit(n.normalizedUnit());
        res.setReferenceLow(n.referenceLow());
        res.setReferenceHigh(n.referenceHigh());
        res.setCollectionDate(report.getCollectionDate());
        res.setSourceReport("SYNTHETIC_LAB_EHR");
        report.addResult(res);
    }
}
