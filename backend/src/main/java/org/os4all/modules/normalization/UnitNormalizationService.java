package org.os4all.modules.normalization;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes biomarker names, observation types, and measurement units to standardized units.
 */
@Component
public class UnitNormalizationService {

    public record NormalizedBiomarker(
            String canonicalName,
            BigDecimal normalizedValue,
            String normalizedUnit,
            BigDecimal referenceLow,
            BigDecimal referenceHigh
    ) {}

    public record NormalizedObservation(
            BigDecimal normalizedValue,
            String normalizedUnit
    ) {}

    // Map common aliases to canonical names
    private static final Map<String, String> BIOMARKER_CANONICAL_MAP;
    // Map canonical biomarker to standard target unit
    private static final Map<String, String> BIOMARKER_TARGET_UNIT;
    // Default reference ranges in standard units (clinical baseline references)
    private static final Map<String, BigDecimal[]> DEFAULT_REFERENCE_RANGES;

    static {
        Map<String, String> aliases = new HashMap<>();
        // ALT / SGPT
        aliases.put("alt", "ALT/SGPT");
        aliases.put("sgpt", "ALT/SGPT");
        aliases.put("alt/sgpt", "ALT/SGPT");
        aliases.put("alanine aminotransferase", "ALT/SGPT");

        // AST
        aliases.put("ast", "AST");
        aliases.put("sgot", "AST");
        aliases.put("ast/sgot", "AST");
        aliases.put("aspartate aminotransferase", "AST");

        // Bilirubin
        aliases.put("bilirubin", "bilirubin");
        aliases.put("total bilirubin", "bilirubin");
        aliases.put("t. bilirubin", "bilirubin");
        aliases.put("t-bili", "bilirubin");

        // Creatinine
        aliases.put("creatinine", "creatinine");
        aliases.put("serum creatinine", "creatinine");

        // Hemoglobin
        aliases.put("hemoglobin", "hemoglobin");
        aliases.put("haemoglobin", "hemoglobin");
        aliases.put("hb", "hemoglobin");
        aliases.put("hgb", "hemoglobin");

        // Glucose
        aliases.put("glucose", "glucose");
        aliases.put("fasting glucose", "glucose");
        aliases.put("fasting blood sugar", "glucose");
        aliases.put("fbs", "glucose");
        aliases.put("blood glucose", "glucose");

        // HbA1c
        aliases.put("hba1c", "HbA1c");
        aliases.put("glycated hemoglobin", "HbA1c");
        aliases.put("a1c", "HbA1c");

        // WBC
        aliases.put("wbc", "WBC");
        aliases.put("white blood cell", "WBC");
        aliases.put("white blood cells", "WBC");
        aliases.put("white blood cell count", "WBC");
        aliases.put("leukocytes", "WBC");

        // Platelets
        aliases.put("platelets", "platelets");
        aliases.put("platelet count", "platelets");
        aliases.put("plt", "platelets");
        aliases.put("thrombocytes", "platelets");

        // Cholesterol
        aliases.put("cholesterol", "cholesterol");
        aliases.put("total cholesterol", "cholesterol");
        aliases.put("serum cholesterol", "cholesterol");

        // Triglycerides
        aliases.put("triglycerides", "triglycerides");
        aliases.put("tg", "triglycerides");
        aliases.put("trigs", "triglycerides");

        BIOMARKER_CANONICAL_MAP = Collections.unmodifiableMap(aliases);

        Map<String, String> targetUnits = new HashMap<>();
        targetUnits.put("ALT/SGPT", "U/L");
        targetUnits.put("AST", "U/L");
        targetUnits.put("bilirubin", "mg/dL");
        targetUnits.put("creatinine", "mg/dL");
        targetUnits.put("hemoglobin", "g/dL");
        targetUnits.put("glucose", "mg/dL");
        targetUnits.put("HbA1c", "%");
        targetUnits.put("WBC", "10^3/uL");
        targetUnits.put("platelets", "10^3/uL");
        targetUnits.put("cholesterol", "mg/dL");
        targetUnits.put("triglycerides", "mg/dL");
        BIOMARKER_TARGET_UNIT = Collections.unmodifiableMap(targetUnits);

        Map<String, BigDecimal[]> ranges = new HashMap<>();
        ranges.put("ALT/SGPT", new BigDecimal[]{new BigDecimal("7.0"), new BigDecimal("56.0")});
        ranges.put("AST", new BigDecimal[]{new BigDecimal("10.0"), new BigDecimal("40.0")});
        ranges.put("bilirubin", new BigDecimal[]{new BigDecimal("0.2"), new BigDecimal("1.2")});
        ranges.put("creatinine", new BigDecimal[]{new BigDecimal("0.6"), new BigDecimal("1.3")});
        ranges.put("hemoglobin", new BigDecimal[]{new BigDecimal("12.0"), new BigDecimal("17.5")});
        ranges.put("glucose", new BigDecimal[]{new BigDecimal("70.0"), new BigDecimal("99.0")});
        ranges.put("HbA1c", new BigDecimal[]{new BigDecimal("4.0"), new BigDecimal("5.6")});
        ranges.put("WBC", new BigDecimal[]{new BigDecimal("4.5"), new BigDecimal("11.0")});
        ranges.put("platelets", new BigDecimal[]{new BigDecimal("150.0"), new BigDecimal("450.0")});
        ranges.put("cholesterol", new BigDecimal[]{new BigDecimal("125.0"), new BigDecimal("200.0")});
        ranges.put("triglycerides", new BigDecimal[]{new BigDecimal("40.0"), new BigDecimal("150.0")});
        DEFAULT_REFERENCE_RANGES = Collections.unmodifiableMap(ranges);
    }

    /**
     * Resolves raw biomarker name into canonical representation.
     */
    public String canonicalizeBiomarker(String rawBiomarker) {
        if (rawBiomarker == null || rawBiomarker.trim().isEmpty()) {
            return "UNKNOWN";
        }
        String clean = rawBiomarker.trim().toLowerCase(Locale.ROOT);
        return BIOMARKER_CANONICAL_MAP.getOrDefault(clean, rawBiomarker.trim());
    }

    /**
     * Normalizes a biomarker value, unit, and reference ranges to standard units.
     */
    public NormalizedBiomarker normalizeBiomarker(
            String biomarker,
            BigDecimal value,
            String unit,
            BigDecimal referenceLow,
            BigDecimal referenceHigh
    ) {
        String canonical = canonicalizeBiomarker(biomarker);
        String targetUnit = BIOMARKER_TARGET_UNIT.get(canonical);

        if (targetUnit == null || unit == null || value == null) {
            // Unrecognized biomarker or unit; keep values intact
            BigDecimal refLow = referenceLow != null ? referenceLow : getDefaultRefLow(canonical);
            BigDecimal refHigh = referenceHigh != null ? referenceHigh : getDefaultRefHigh(canonical);
            return new NormalizedBiomarker(canonical, value, unit, refLow, refHigh);
        }

        String rawUnitClean = cleanUnit(unit);
        BigDecimal convertedValue = value;
        BigDecimal convertedLow = referenceLow;
        BigDecimal convertedHigh = referenceHigh;

        // Glucose & Cholesterol & Triglycerides (mmol/L to mg/dL)
        if ("glucose".equalsIgnoreCase(canonical)) {
            if ("mmol/L".equalsIgnoreCase(rawUnitClean)) {
                // 1 mmol/L glucose = 18.0182 mg/dL
                BigDecimal factor = new BigDecimal("18.0182");
                convertedValue = value.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
        } else if ("cholesterol".equalsIgnoreCase(canonical)) {
            if ("mmol/L".equalsIgnoreCase(rawUnitClean)) {
                // 1 mmol/L cholesterol = 38.67 mg/dL
                BigDecimal factor = new BigDecimal("38.67");
                convertedValue = value.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
        } else if ("triglycerides".equalsIgnoreCase(canonical)) {
            if ("mmol/L".equalsIgnoreCase(rawUnitClean)) {
                // 1 mmol/L triglycerides = 88.57 mg/dL
                BigDecimal factor = new BigDecimal("88.57");
                convertedValue = value.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
        } else if ("creatinine".equalsIgnoreCase(canonical)) {
            if ("umol/L".equalsIgnoreCase(rawUnitClean) || "µmol/L".equalsIgnoreCase(rawUnitClean)) {
                // 1 umol/L creatinine = 0.011312 mg/dL (or / 88.4)
                BigDecimal divisor = new BigDecimal("88.4");
                convertedValue = value.divide(divisor, 2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.divide(divisor, 2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.divide(divisor, 2, RoundingMode.HALF_UP);
            }
        } else if ("bilirubin".equalsIgnoreCase(canonical)) {
            if ("umol/L".equalsIgnoreCase(rawUnitClean) || "µmol/L".equalsIgnoreCase(rawUnitClean)) {
                // 1 umol/L bilirubin = 0.05847 mg/dL (or / 17.1)
                BigDecimal divisor = new BigDecimal("17.1");
                convertedValue = value.divide(divisor, 2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.divide(divisor, 2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.divide(divisor, 2, RoundingMode.HALF_UP);
            }
        } else if ("hemoglobin".equalsIgnoreCase(canonical)) {
            if ("g/L".equalsIgnoreCase(rawUnitClean)) {
                // 1 g/L = 0.1 g/dL
                BigDecimal factor = new BigDecimal("0.1");
                convertedValue = value.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.multiply(factor).setScale(2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.multiply(factor).setScale(2, RoundingMode.HALF_UP);
            }
        } else if ("WBC".equalsIgnoreCase(canonical) || "platelets".equalsIgnoreCase(canonical)) {
            if ("/uL".equalsIgnoreCase(rawUnitClean) || "/mcL".equalsIgnoreCase(rawUnitClean) || "/mm3".equalsIgnoreCase(rawUnitClean) || "cells/uL".equalsIgnoreCase(rawUnitClean)) {
                // raw count e.g. 7500 /uL -> 7.5 10^3/uL
                convertedValue = value.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                if (convertedLow != null) convertedLow = convertedLow.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
                if (convertedHigh != null) convertedHigh = convertedHigh.divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP);
            }
        }

        if (convertedLow == null) {
            convertedLow = getDefaultRefLow(canonical);
        }
        if (convertedHigh == null) {
            convertedHigh = getDefaultRefHigh(canonical);
        }

        return new NormalizedBiomarker(canonical, convertedValue, targetUnit, convertedLow, convertedHigh);
    }

    /**
     * Normalizes health observation measurements (temperature, height, weight, etc.)
     */
    public NormalizedObservation normalizeObservation(String observationType, BigDecimal value, String unit) {
        if (value == null || unit == null) {
            return new NormalizedObservation(value, unit);
        }
        String u = cleanUnit(unit);
        String type = observationType != null ? observationType.trim().toUpperCase(Locale.ROOT) : "";

        // Temperature: Fahrenheit to Celsius
        if (type.contains("TEMP") || "degF".equalsIgnoreCase(u) || "°F".equalsIgnoreCase(u) || "F".equalsIgnoreCase(u)) {
            if ("degF".equalsIgnoreCase(u) || "°F".equalsIgnoreCase(u) || "F".equalsIgnoreCase(u) || "FAHRENHEIT".equalsIgnoreCase(u)) {
                // (F - 32) * 5 / 9
                BigDecimal celsius = value.subtract(new BigDecimal("32"))
                        .multiply(new BigDecimal("5"))
                        .divide(new BigDecimal("9"), 2, RoundingMode.HALF_UP);
                return new NormalizedObservation(celsius, "°C");
            }
            return new NormalizedObservation(value.setScale(2, RoundingMode.HALF_UP), "°C");
        }

        // Weight: lbs to kg
        if (type.contains("WEIGHT") || "lbs".equalsIgnoreCase(u) || "lb".equalsIgnoreCase(u)) {
            if ("lbs".equalsIgnoreCase(u) || "lb".equalsIgnoreCase(u)) {
                BigDecimal kg = value.multiply(new BigDecimal("0.45359237")).setScale(2, RoundingMode.HALF_UP);
                return new NormalizedObservation(kg, "kg");
            }
            return new NormalizedObservation(value.setScale(2, RoundingMode.HALF_UP), "kg");
        }

        // Height: inches or ft to cm
        if (type.contains("HEIGHT") || "in".equalsIgnoreCase(u) || "inches".equalsIgnoreCase(u)) {
            if ("in".equalsIgnoreCase(u) || "inch".equalsIgnoreCase(u) || "inches".equalsIgnoreCase(u)) {
                BigDecimal cm = value.multiply(new BigDecimal("2.54")).setScale(2, RoundingMode.HALF_UP);
                return new NormalizedObservation(cm, "cm");
            }
            return new NormalizedObservation(value.setScale(2, RoundingMode.HALF_UP), "cm");
        }

        // Blood Glucose observation: mmol/L to mg/dL
        if (type.contains("GLUCOSE") && "mmol/L".equalsIgnoreCase(u)) {
            BigDecimal mgdl = value.multiply(new BigDecimal("18.0182")).setScale(2, RoundingMode.HALF_UP);
            return new NormalizedObservation(mgdl, "mg/dL");
        }

        return new NormalizedObservation(value, unit);
    }

    private String cleanUnit(String unit) {
        if (unit == null) return "";
        return unit.trim().replaceAll("\\s+", "");
    }

    public BigDecimal getDefaultRefLow(String canonicalBiomarker) {
        BigDecimal[] range = DEFAULT_REFERENCE_RANGES.get(canonicalBiomarker);
        return range != null ? range[0] : null;
    }

    public BigDecimal getDefaultRefHigh(String canonicalBiomarker) {
        BigDecimal[] range = DEFAULT_REFERENCE_RANGES.get(canonicalBiomarker);
        return range != null ? range[1] : null;
    }
}
