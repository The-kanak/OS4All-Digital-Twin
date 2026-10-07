package org.os4all.core.config;

import org.os4all.modules.ingestion.entity.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Configuration
public class HealthDataDemoSeeder {

    private static final Logger log = LoggerFactory.getLogger(HealthDataDemoSeeder.class);

    public static final String DEMO_METADATA_HEADER = "{\"disclaimer\":\"DEMO DATA - NOT REAL PATIENT INFORMATION. FOR DEVELOPMENT AND TEST USE ONLY.\"}";
    public static final String DEMO_USER_EMAIL = "alex.rivera@demo.os4all.test";

    @Bean
    @Profile("!test")
    public CommandLineRunner seedDemoHealthData(
            UserRepository userRepository,
            HealthObservationRepository observationRepository,
            LabReportRepository labReportRepository,
            UnitNormalizationService normalizationService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("Checking if Health Data Engine demo data is present...");

            User demoUser = userRepository.findByEmail(DEMO_USER_EMAIL).orElseGet(() -> {
                User u = new User();
                u.setId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
                u.setEmail(DEMO_USER_EMAIL);
                u.setFullName("Alex Rivera (DEMO DATA)");
                u.setPasswordHash(passwordEncoder.encode("DemoPassword123!"));
                u.setRole(Role.ROLE_USER);
                u.setActive(true);

                UserProfile profile = new UserProfile();
                profile.setUser(u);
                profile.setDateOfBirth(LocalDate.of(1988, 6, 15));
                profile.setBiologicalSex("MALE");
                profile.setHeightCm(new BigDecimal("178.5"));
                profile.setWeightKg(new BigDecimal("74.2"));
                profile.setBloodType("O+");
                profile.setLifestyleNotes("DEMO DATA: Moderate aerobic activity, desk worker, non-smoker.");
                u.setProfile(profile);

                return userRepository.save(u);
            });

            if (observationRepository.countByUserId(demoUser.getId()) > 0) {
                log.info("Demo health observations already initialized for user {}", demoUser.getEmail());
                return;
            }

            log.info("Seeding realistic synthetic DEMO DATA (NOT REAL PATIENT INFORMATION) for development...");

            Instant now = Instant.now();

            // 1. Seed Vital Measurements (Resting Heart Rate, HRV, SpO2, Temperature, Heart Rate)
            // Resting Heart Rate (6 observations)
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Resting Heart Rate",
                    new BigDecimal("68"), "bpm", now.minus(8, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("1.0"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Resting Heart Rate",
                    new BigDecimal("69"), "bpm", now.minus(7, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("1.0"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Resting Heart Rate",
                    new BigDecimal("70"), "bpm", now.minus(5, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("1.0"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Resting Heart Rate",
                    new BigDecimal("72"), "bpm", now.minus(3, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("1.0"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Resting Heart Rate",
                    new BigDecimal("67"), "bpm", now.minus(2, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("1.0"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Resting Heart Rate",
                    new BigDecimal("65"), "bpm", now.minus(1, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("1.0"));

            // Heart Rate / Active
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Active Heart Rate",
                    new BigDecimal("115"), "bpm", now.minus(6, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("0.98"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Active Heart Rate",
                    new BigDecimal("120"), "bpm", now.minus(4, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("0.98"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Active Heart Rate",
                    new BigDecimal("112"), "bpm", now.minus(3, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("0.98"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Active Heart Rate",
                    new BigDecimal("118"), "bpm", now.minus(2, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("0.98"));
            seedVital(observationRepository, demoUser, ObservationType.HEART_RATE, "Active Heart Rate",
                    new BigDecimal("110"), "bpm", now.minus(1, ChronoUnit.DAYS), "Apple Watch Series 9", new BigDecimal("0.98"));

            // HRV (5 observations)
            seedVital(observationRepository, demoUser, ObservationType.HRV, "Heart Rate Variability",
                    new BigDecimal("52"), "ms", now.minus(6, ChronoUnit.DAYS), "Oura Ring Gen 3", new BigDecimal("0.97"));
            seedVital(observationRepository, demoUser, ObservationType.HRV, "Heart Rate Variability",
                    new BigDecimal("55"), "ms", now.minus(5, ChronoUnit.DAYS), "Oura Ring Gen 3", new BigDecimal("0.97"));
            seedVital(observationRepository, demoUser, ObservationType.HRV, "Heart Rate Variability",
                    new BigDecimal("54"), "ms", now.minus(4, ChronoUnit.DAYS), "Oura Ring Gen 3", new BigDecimal("0.96"));
            seedVital(observationRepository, demoUser, ObservationType.HRV, "Heart Rate Variability",
                    new BigDecimal("58"), "ms", now.minus(2, ChronoUnit.DAYS), "Oura Ring Gen 3", new BigDecimal("0.96"));
            seedVital(observationRepository, demoUser, ObservationType.HRV, "Heart Rate Variability",
                    new BigDecimal("56"), "ms", now.minus(1, ChronoUnit.DAYS), "Oura Ring Gen 3", new BigDecimal("0.98"));

            // SpO2 (5 observations)
            seedVital(observationRepository, demoUser, ObservationType.OXYGEN_SATURATION, "Blood Oxygen (SpO2)",
                    new BigDecimal("98"), "%", now.minus(6, ChronoUnit.DAYS), "Pulse Oximeter Pro", new BigDecimal("0.99"));
            seedVital(observationRepository, demoUser, ObservationType.OXYGEN_SATURATION, "Blood Oxygen (SpO2)",
                    new BigDecimal("97"), "%", now.minus(5, ChronoUnit.DAYS), "Pulse Oximeter Pro", new BigDecimal("0.99"));
            seedVital(observationRepository, demoUser, ObservationType.OXYGEN_SATURATION, "Blood Oxygen (SpO2)",
                    new BigDecimal("99"), "%", now.minus(4, ChronoUnit.DAYS), "Pulse Oximeter Pro", new BigDecimal("0.99"));
            seedVital(observationRepository, demoUser, ObservationType.OXYGEN_SATURATION, "Blood Oxygen (SpO2)",
                    new BigDecimal("98"), "%", now.minus(2, ChronoUnit.DAYS), "Pulse Oximeter Pro", new BigDecimal("0.99"));
            seedVital(observationRepository, demoUser, ObservationType.OXYGEN_SATURATION, "Blood Oxygen (SpO2)",
                    new BigDecimal("98"), "%", now.minus(1, ChronoUnit.DAYS), "Pulse Oximeter Pro", new BigDecimal("0.99"));

            // Body Temperature (5 observations)
            seedVital(observationRepository, demoUser, ObservationType.BODY_TEMPERATURE, "Core Temperature",
                    new BigDecimal("98.4"), "degF", now.minus(6, ChronoUnit.DAYS), "Withings Thermo", new BigDecimal("0.96"));
            seedVital(observationRepository, demoUser, ObservationType.BODY_TEMPERATURE, "Core Temperature",
                    new BigDecimal("98.6"), "degF", now.minus(5, ChronoUnit.DAYS), "Withings Thermo", new BigDecimal("0.96"));
            seedVital(observationRepository, demoUser, ObservationType.BODY_TEMPERATURE, "Core Temperature",
                    new BigDecimal("98.5"), "degF", now.minus(3, ChronoUnit.DAYS), "Withings Thermo", new BigDecimal("0.95"));
            seedVital(observationRepository, demoUser, ObservationType.BODY_TEMPERATURE, "Core Temperature",
                    new BigDecimal("98.7"), "degF", now.minus(2, ChronoUnit.DAYS), "Withings Thermo", new BigDecimal("0.95"));
            seedVital(observationRepository, demoUser, ObservationType.BODY_TEMPERATURE, "Core Temperature",
                    new BigDecimal("98.6"), "degF", now.minus(1, ChronoUnit.DAYS), "Withings Thermo", new BigDecimal("0.95"));

            // Steps (5 observations)
            seedLifestyle(observationRepository, demoUser, ObservationType.STEPS, "Step Count",
                    new BigDecimal("8200"), "steps", now.minus(5, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.STEPS, "Step Count",
                    new BigDecimal("9100"), "steps", now.minus(4, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.STEPS, "Step Count",
                    new BigDecimal("7800"), "steps", now.minus(3, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.STEPS, "Step Count",
                    new BigDecimal("10400"), "steps", now.minus(2, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.STEPS, "Step Count",
                    new BigDecimal("8600"), "steps", now.minus(1, ChronoUnit.DAYS));

            // Sleep Duration (5 observations)
            seedLifestyle(observationRepository, demoUser, ObservationType.SLEEP, "Sleep Hygiene",
                    "7.2 hours restful sleep", 432, now.minus(5, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.SLEEP, "Sleep Hygiene",
                    "7.5 hours deep sleep", 450, now.minus(4, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.SLEEP, "Sleep Hygiene",
                    "7.0 hours uninterrupted sleep", 420, now.minus(3, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.SLEEP, "Sleep Hygiene",
                    "8.0 hours restorative sleep", 480, now.minus(2, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.SLEEP, "Sleep Hygiene",
                    "7.4 hours restful sleep", 444, now.minus(1, ChronoUnit.DAYS));

            seedVital(observationRepository, demoUser, ObservationType.BLOOD_PRESSURE_SYSTOLIC, "Systolic BP",
                    new BigDecimal("120"), "mmHg", now.minus(4, ChronoUnit.DAYS), "Omron Evolv BP7000", new BigDecimal("0.98"));
            seedVital(observationRepository, demoUser, ObservationType.BLOOD_PRESSURE_DIASTOLIC, "Diastolic BP",
                    new BigDecimal("78"), "mmHg", now.minus(4, ChronoUnit.DAYS), "Omron Evolv BP7000", new BigDecimal("0.98"));

            // 2. Seed Other Lifestyle Observations (Exercise, Hydration)
            seedLifestyle(observationRepository, demoUser, ObservationType.EXERCISE, "Running",
                    "Zone 2 cardio training in park", 45, now.minus(3, ChronoUnit.DAYS));
            seedLifestyle(observationRepository, demoUser, ObservationType.HYDRATION, "Water Intake",
                    new BigDecimal("2.5"), "L", now.minus(1, ChronoUnit.DAYS));

            // 3. Seed Symptom Observations
            seedSymptom(observationRepository, demoUser, "Mild lower back tightness", "MILD", "Lower Lumbar",
                    now.minus(4, ChronoUnit.DAYS));

            // 4. Seed Comprehensive Lab Report (Common Biomarkers)
            LabReport comprehensivePanel = new LabReport();
            comprehensivePanel.setUser(demoUser);
            comprehensivePanel.setReportTitle("Comprehensive Metabolic & Lipid Panel (DEMO DATA)");
            comprehensivePanel.setLaboratoryName("Quest Diagnostics (DEMO LAB)");
            comprehensivePanel.setCollectionDate(now.minus(7, ChronoUnit.DAYS));
            comprehensivePanel.setReportedDate(now.minus(6, ChronoUnit.DAYS));
            comprehensivePanel.setSource("DEMO DATA: Quest Diagnostic Portal Upload");
            comprehensivePanel.setNotes("DEMO DATA: Annual preventive metabolic screening. NOT REAL PATIENT INFORMATION.");

            // Common biomarkers required by specification
            addLabResult(comprehensivePanel, demoUser, normalizationService, "ALT/SGPT", new BigDecimal("24.0"), "U/L", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "AST", new BigDecimal("22.0"), "U/L", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "bilirubin", new BigDecimal("0.8"), "mg/dL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "creatinine", new BigDecimal("0.95"), "mg/dL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "hemoglobin", new BigDecimal("15.2"), "g/dL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "glucose", new BigDecimal("88.0"), "mg/dL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "HbA1c", new BigDecimal("5.2"), "%", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "WBC", new BigDecimal("6.4"), "10^3/uL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "platelets", new BigDecimal("240.0"), "10^3/uL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "cholesterol", new BigDecimal("175.0"), "mg/dL", "Comprehensive Panel");
            addLabResult(comprehensivePanel, demoUser, normalizationService, "triglycerides", new BigDecimal("110.0"), "mg/dL", "Comprehensive Panel");

            labReportRepository.save(comprehensivePanel);

            log.info("Successfully seeded comprehensive DEMO DATA for Health Data Engine. All entries tagged with DEMO metadata.");
        };
    }

    private void seedVital(
            HealthObservationRepository repo,
            User user,
            ObservationType type,
            String vitalName,
            BigDecimal value,
            String unit,
            Instant timestamp,
            String deviceModel,
            BigDecimal confidence
    ) {
        VitalMeasurement vital = new VitalMeasurement();
        vital.setUser(user);
        vital.setObservationType(type);
        vital.setVitalName(vitalName);
        vital.setValueNumeric(value);
        vital.setUnit(unit);
        vital.setStandardValueNumeric(value);
        vital.setStandardUnit(unit);
        vital.setTimestamp(timestamp);
        vital.setSource("DEMO DATA: " + deviceModel);
        vital.setDeviceModel(deviceModel);
        vital.setConfidence(confidence);
        vital.setMetadata(DEMO_METADATA_HEADER);
        repo.save(vital);
    }

    private void seedLifestyle(
            HealthObservationRepository repo,
            User user,
            ObservationType type,
            String category,
            String note,
            Integer durationMinutes,
            Instant timestamp
    ) {
        LifestyleObservation obs = new LifestyleObservation();
        obs.setUser(user);
        obs.setObservationType(type);
        obs.setLifestyleCategory(category);
        obs.setValueText(note);
        obs.setDurationMinutes(durationMinutes);
        obs.setTimestamp(timestamp);
        obs.setSource("DEMO DATA: Mobile Manual Log");
        obs.setConfidence(BigDecimal.valueOf(1.0));
        obs.setMetadata(DEMO_METADATA_HEADER);
        repo.save(obs);
    }

    private void seedLifestyle(
            HealthObservationRepository repo,
            User user,
            ObservationType type,
            String category,
            BigDecimal value,
            String unit,
            Instant timestamp
    ) {
        LifestyleObservation obs = new LifestyleObservation();
        obs.setUser(user);
        obs.setObservationType(type);
        obs.setLifestyleCategory(category);
        obs.setValueNumeric(value);
        obs.setUnit(unit);
        obs.setStandardValueNumeric(value);
        obs.setStandardUnit(unit);
        obs.setTimestamp(timestamp);
        obs.setSource("DEMO DATA: Smart Hydration Bottle");
        obs.setConfidence(BigDecimal.valueOf(1.0));
        obs.setMetadata(DEMO_METADATA_HEADER);
        repo.save(obs);
    }

    private void seedSymptom(
            HealthObservationRepository repo,
            User user,
            String symptomName,
            String severity,
            String bodySite,
            Instant timestamp
    ) {
        SymptomObservation obs = new SymptomObservation();
        obs.setUser(user);
        obs.setObservationType(ObservationType.SYMPTOM);
        obs.setSymptomName(symptomName);
        obs.setValueText(symptomName);
        obs.setSeverity(severity);
        obs.setBodySite(bodySite);
        obs.setTimestamp(timestamp);
        obs.setSource("DEMO DATA: Symptom Checker Log");
        obs.setConfidence(BigDecimal.valueOf(0.95));
        obs.setMetadata(DEMO_METADATA_HEADER);
        repo.save(obs);
    }

    private void addLabResult(
            LabReport report,
            User user,
            UnitNormalizationService normService,
            String biomarker,
            BigDecimal value,
            String unit,
            String reportTitle
    ) {
        UnitNormalizationService.NormalizedBiomarker norm = normService.normalizeBiomarker(
                biomarker, value, unit, null, null
        );

        LabResult r = new LabResult();
        r.setUser(user);
        r.setBiomarker(biomarker);
        r.setStandardizedBiomarker(norm.canonicalName());
        r.setValue(value);
        r.setUnit(unit);
        r.setStandardValue(norm.normalizedValue());
        r.setStandardUnit(norm.normalizedUnit());
        r.setReferenceLow(norm.referenceLow());
        r.setReferenceHigh(norm.referenceHigh());
        r.setCollectionDate(report.getCollectionDate());
        r.setSourceReport("DEMO DATA: " + reportTitle);
        r.setNotes("DEMO DATA: Calibrated synthetic reference point.");
        report.addResult(r);
    }
}
