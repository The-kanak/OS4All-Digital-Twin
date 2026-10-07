package org.os4all.modules.normalization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class UnitNormalizationServiceTest {

    private UnitNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new UnitNormalizationService();
    }

    @Test
    @DisplayName("Should canonicalize biomarker aliases properly")
    void testCanonicalization() {
        assertEquals("ALT/SGPT", service.canonicalizeBiomarker("alt"));
        assertEquals("ALT/SGPT", service.canonicalizeBiomarker("SGPT"));
        assertEquals("ALT/SGPT", service.canonicalizeBiomarker("alanine aminotransferase"));
        assertEquals("AST", service.canonicalizeBiomarker("ast"));
        assertEquals("AST", service.canonicalizeBiomarker("sgot"));
        assertEquals("bilirubin", service.canonicalizeBiomarker("total bilirubin"));
        assertEquals("creatinine", service.canonicalizeBiomarker("serum creatinine"));
        assertEquals("hemoglobin", service.canonicalizeBiomarker("Hb"));
        assertEquals("glucose", service.canonicalizeBiomarker("Fasting Blood Sugar"));
        assertEquals("HbA1c", service.canonicalizeBiomarker("A1C"));
        assertEquals("WBC", service.canonicalizeBiomarker("white blood cells"));
        assertEquals("platelets", service.canonicalizeBiomarker("plt"));
        assertEquals("cholesterol", service.canonicalizeBiomarker("total cholesterol"));
        assertEquals("triglycerides", service.canonicalizeBiomarker("trigs"));
    }

    @Test
    @DisplayName("Should convert glucose from mmol/L to mg/dL")
    void testGlucoseConversion() {
        // 5.5 mmol/L * 18.0182 = 99.10 mg/dL
        var norm = service.normalizeBiomarker("glucose", new BigDecimal("5.5"), "mmol/L", new BigDecimal("3.9"), new BigDecimal("5.6"));
        assertEquals("glucose", norm.canonicalName());
        assertEquals("mg/dL", norm.normalizedUnit());
        assertEquals(new BigDecimal("99.10"), norm.normalizedValue());
        assertEquals(new BigDecimal("70.27"), norm.referenceLow());
        assertEquals(new BigDecimal("100.90"), norm.referenceHigh());
    }

    @Test
    @DisplayName("Should convert creatinine from umol/L to mg/dL")
    void testCreatinineConversion() {
        // 88.4 umol/L -> 1.00 mg/dL
        var norm = service.normalizeBiomarker("creatinine", new BigDecimal("88.4"), "umol/L", new BigDecimal("53.0"), new BigDecimal("106.0"));
        assertEquals("creatinine", norm.canonicalName());
        assertEquals("mg/dL", norm.normalizedUnit());
        assertEquals(new BigDecimal("1.00"), norm.normalizedValue());
        assertEquals(new BigDecimal("0.60"), norm.referenceLow());
        assertEquals(new BigDecimal("1.20"), norm.referenceHigh());
    }

    @Test
    @DisplayName("Should convert hemoglobin from g/L to g/dL")
    void testHemoglobinConversion() {
        // 150 g/L -> 15.0 g/dL
        var norm = service.normalizeBiomarker("hemoglobin", new BigDecimal("150.0"), "g/L", null, null);
        assertEquals("hemoglobin", norm.canonicalName());
        assertEquals("g/dL", norm.normalizedUnit());
        assertEquals(new BigDecimal("15.00"), norm.normalizedValue());
        assertEquals(new BigDecimal("12.0"), norm.referenceLow());
        assertEquals(new BigDecimal("17.5"), norm.referenceHigh());
    }

    @Test
    @DisplayName("Should convert WBC raw count /uL to 10^3/uL")
    void testWbcConversion() {
        // 7500 /uL -> 7.50 10^3/uL
        var norm = service.normalizeBiomarker("WBC", new BigDecimal("7500"), "/uL", null, null);
        assertEquals("WBC", norm.canonicalName());
        assertEquals("10^3/uL", norm.normalizedUnit());
        assertEquals(new BigDecimal("7.50"), norm.normalizedValue());
        assertEquals(new BigDecimal("4.5"), norm.referenceLow());
        assertEquals(new BigDecimal("11.0"), norm.referenceHigh());
    }

    @Test
    @DisplayName("Should convert temperature Fahrenheit to Celsius")
    void testTemperatureConversion() {
        // 98.6 degF -> 37.00 °C
        var norm = service.normalizeObservation("BODY_TEMPERATURE", new BigDecimal("98.6"), "degF");
        assertEquals(new BigDecimal("37.00"), norm.normalizedValue());
        assertEquals("°C", norm.normalizedUnit());
    }

    @Test
    @DisplayName("Should convert weight lbs to kg")
    void testWeightConversion() {
        // 160 lbs -> 72.57 kg
        var norm = service.normalizeObservation("WEIGHT", new BigDecimal("160"), "lbs");
        assertEquals(new BigDecimal("72.57"), norm.normalizedValue());
        assertEquals("kg", norm.normalizedUnit());
    }
}
