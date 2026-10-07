/// Realistic Synthetic Demo Data for OS4All.
/// Strictly marked as DEMO DATA — never presented as actual real patient records.
class DemoData {
  static const bool isDemoMode = true;

  static const String userFullName = "Demo User — Synthetic Data";
  static const String userEmail = "demo.patient@os4all.test";
  static const String userAge = "34";
  static const String biologicalSex = "MALE";
  static const String demoNotice = "DEMO DATA — NOT A REAL PATIENT";

  static const String currentHealthState = "STABLE";
  static const String currentHealthSummary =
      "All physiological vitals and biometric signals remain stable and aligned with your individual historical baselines.";

  static final List<Map<String, dynamic>> keyMetrics = [
    {
      "metric": "Resting Heart Rate",
      "value": "61",
      "unit": "bpm",
      "baseline": "60 bpm",
      "status": "NORMAL",
      "deviation": "+1 bpm",
      "trend": "STABLE",
      "icon": "favorite"
    },
    {
      "metric": "Heart Rate Variability",
      "value": "54",
      "unit": "ms",
      "baseline": "55 ms",
      "status": "NORMAL",
      "deviation": "-1 ms",
      "trend": "STABLE",
      "icon": "show_chart"
    },
    {
      "metric": "Blood Oxygen (SpO2)",
      "value": "98.5",
      "unit": "%",
      "baseline": "98.0%",
      "status": "NORMAL",
      "deviation": "+0.5%",
      "trend": "STABLE",
      "icon": "air"
    },
    {
      "metric": "Sleep Duration",
      "value": "7.8",
      "unit": "hrs",
      "baseline": "8.0 hrs",
      "status": "NORMAL",
      "deviation": "-0.2 hrs",
      "trend": "STABLE",
      "icon": "bedtime"
    },
    {
      "metric": "Body Temperature",
      "value": "36.6",
      "unit": "°C",
      "baseline": "36.7 °C",
      "status": "NORMAL",
      "deviation": "-0.1 °C",
      "trend": "STABLE",
      "icon": "thermostat"
    },
    {
      "metric": "Daily Steps",
      "value": "9,420",
      "unit": "steps",
      "baseline": "8,800 steps",
      "status": "NORMAL",
      "deviation": "+620",
      "trend": "ACTIVE",
      "icon": "directions_walk"
    },
  ];

  static final List<Map<String, dynamic>> personalBaselines = [
    {
      "metric": "Resting Heart Rate",
      "mean": "60.4 bpm",
      "stdDev": "± 2.8 bpm",
      "range": "55.0 - 66.0 bpm",
      "count": 42,
      "reliability": "ESTABLISHED (High Confidence)"
    },
    {
      "metric": "Heart Rate Variability (rMSSD)",
      "mean": "55.2 ms",
      "stdDev": "± 4.6 ms",
      "range": "46.0 - 64.0 ms",
      "count": 42,
      "reliability": "ESTABLISHED (High Confidence)"
    },
    {
      "metric": "Sleep Duration",
      "mean": "7.95 hrs",
      "stdDev": "± 0.6 hrs",
      "range": "6.8 - 9.1 hrs",
      "count": 30,
      "reliability": "ESTABLISHED (High Confidence)"
    },
    {
      "metric": "Blood Oxygen (SpO2)",
      "mean": "98.2%",
      "stdDev": "± 0.8%",
      "range": "96.5 - 99.5%",
      "count": 42,
      "reliability": "ESTABLISHED (High Confidence)"
    },
  ];

  static final Map<String, dynamic> latestAiInsight = {
    "summary":
        "OS4All observed: Baseline analysis confirms physiological stability with optimal autonomic recovery balance over the preceding 7 days. External evidence indicates: Maintained homeostatic stability within individual standard deviation envelopes reflects cardiovascular resilience. OS4All recommends discussing: Routine preventative checkups at scheduled clinical follow-ups.",
    "observations": [
      "Resting heart rate averaged 61 bpm, fully aligned with your 60.4 bpm baseline mean.",
      "HRV is consistent with your restorative range (54 ms vs 55.2 ms historical envelope).",
      "Sleep duration sustained at 7.8 hours, preserving natural circadian recovery."
    ],
    "possibleInterpretations": [
      "Optimal balance between parasympathetic and sympathetic autonomic nervous system tone.",
      "Effective physiological adaptation to current physical activity and stress levels."
    ],
    "recommendedActions": [
      "Maintain consistent bedtime between 10:30 PM and 11:00 PM.",
      "Continue daily hydration and moderate aerobic training."
    ],
    "evidence": [
      {
        "title": "Heart rate variability as a marker of autonomic recovery and physical stress",
        "source": "ncbi.nlm.nih.gov (Peer-Reviewed / Clinical Reference)",
        "url": "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC5900352/",
        "domain": "ncbi.nlm.nih.gov",
        "retrievedTime": "2026-10-05T09:30:00Z",
        "relevantExcerpt": "Suppression of resting nocturnal HRV paired with resting tachycardia serves as an early physiological marker of cumulative autonomic debt and delayed cardiovascular recovery.",
        "relevanceScore": 0.96,
        "relevance": "Suppression of resting nocturnal HRV paired with resting tachycardia serves as an early physiological marker of cumulative autonomic debt and delayed cardiovascular recovery."
      },
      {
        "title": "Personalized Digital Health Tracking and Biometric Baselines",
        "source": "nature.com (Peer-Reviewed / Clinical Reference)",
        "url": "https://www.nature.com/articles/s41591-020-0859-x",
        "domain": "nature.com",
        "retrievedTime": "2026-10-05T09:30:00Z",
        "relevantExcerpt": "Digital wearable devices tracking longitudinal individual baselines allow earlier and more individualized detection of physiological anomalies compared to static population averages.",
        "relevanceScore": 0.89,
        "relevance": "Digital wearable devices tracking longitudinal individual baselines allow earlier and more individualized detection of physiological anomalies compared to static population averages."
      }
    ],
    "urgency": "ROUTINE",
    "confidence": 0.94,
    "disclaimer":
        "OS4All AI insights are informational health interpretations based strictly on personal historical baselines. They DO NOT constitute a medical diagnosis."
  };

  static final List<Map<String, dynamic>> labReports = [
    {
      "id": "rep-001",
      "title": "DEMO DATA: Annual Comprehensive Metabolic Panel",
      "lab": "Quest Diagnostics Labs",
      "date": "2026-09-18",
      "status": "VERIFIED & CONFIRMED",
      "reviewRequired": false,
      "biomarkers": [
        {"name": "Fasting Glucose", "value": "92.0", "unit": "mg/dL", "ref": "70 - 99 mg/dL", "flag": "NORMAL"},
        {"name": "HbA1c", "value": "5.3", "unit": "%", "ref": "4.0 - 5.6 %", "flag": "NORMAL"},
        {"name": "Serum Creatinine", "value": "0.92", "unit": "mg/dL", "ref": "0.6 - 1.3 mg/dL", "flag": "NORMAL"},
        {"name": "ALT / SGPT", "value": "24.0", "unit": "U/L", "ref": "7 - 56 U/L", "flag": "NORMAL"},
        {"name": "Total Bilirubin", "value": "0.80", "unit": "mg/dL", "ref": "0.2 - 1.2 mg/dL", "flag": "NORMAL"},
      ]
    },
    {
      "id": "rep-002",
      "title": "DEMO DATA: Lipid Panel & CBC Screen",
      "lab": "LabCorp Clinical Pathology",
      "date": "2026-08-04",
      "status": "VERIFIED & CONFIRMED",
      "reviewRequired": false,
      "biomarkers": [
        {"name": "Total Cholesterol", "value": "178.0", "unit": "mg/dL", "ref": "125 - 200 mg/dL", "flag": "NORMAL"},
        {"name": "Triglycerides", "value": "110.0", "unit": "mg/dL", "ref": "40 - 150 mg/dL", "flag": "NORMAL"},
        {"name": "Hemoglobin", "value": "15.1", "unit": "g/dL", "ref": "12.0 - 17.5 g/dL", "flag": "NORMAL"},
        {"name": "WBC Count", "value": "6.8", "unit": "10^3/uL", "ref": "4.5 - 11.0 10^3/uL", "flag": "NORMAL"},
        {"name": "Platelets", "value": "245.0", "unit": "10^3/uL", "ref": "150 - 450 10^3/uL", "flag": "NORMAL"}
      ]
    }
  ];

  static final List<Map<String, dynamic>> upcomingTests = [
    {
      "id": "up-001",
      "title": "Comprehensive Liver Panel & ALT Follow-up",
      "laboratory": "CarePath Diagnostic Center",
      "scheduledDate": "Oct 18, 2026",
      "daysLeft": "in 13 days",
      "reason": "Baseline follow-up for ALT trend comparison (recommended repeat interval)",
      "preparation": "Fast 8-10 hours prior to sample collection. Water is allowed.",
      "category": "Hepatic Corridor",
      "status": "SCHEDULED",
      "icon": "biotech"
    },
    {
      "id": "up-002",
      "title": "Annual Cardiovascular & Lipid Profile Screen",
      "laboratory": "Quest Diagnostics Labs",
      "scheduledDate": "Nov 12, 2026",
      "daysLeft": "in 38 days",
      "reason": "Routine longitudinal cardiovascular baseline verification",
      "preparation": "12-hour fasting required. Maintain regular medication unless advised.",
      "category": "Cardiovascular Corridor",
      "status": "UPCOMING",
      "icon": "favorite"
    },
    {
      "id": "up-003",
      "title": "Renal & Electrolyte Function Test (eGFR & Creatinine)",
      "laboratory": "LabCorp Clinical Pathology",
      "scheduledDate": "Dec 05, 2026",
      "daysLeft": "in 61 days",
      "reason": "Routine kidney filtration index monitoring",
      "preparation": "Standard hydration. Avoid heavy protein shakes 24h prior.",
      "category": "Renal Corridor",
      "status": "UPCOMING",
      "icon": "water_drop"
    },
  ];

  static final List<Map<String, dynamic>> timelineItems = [
    {
      "type": "LAB_REPORT",
      "title": "Annual Comprehensive Metabolic Panel",
      "subtitle": "5 biomarkers tested • All within normal range",
      "category": "LAB",
      "timestamp": "Sep 18, 2026",
      "source": "Quest Diagnostics",
      "icon": "science"
    },
    {
      "type": "VITAL",
      "title": "Resting Heart Rate & HRV Baseline Check",
      "subtitle": "61 bpm • HRV 54 ms (Wearable Biosensor)",
      "category": "VITAL",
      "timestamp": "Sep 17, 2026",
      "source": "Smart Wearable",
      "icon": "favorite"
    },
    {
      "type": "LIFESTYLE",
      "title": "Aerobic Cardiovascular Training",
      "subtitle": "45 mins zone 2 running • Normal recovery",
      "category": "LIFESTYLE",
      "timestamp": "Sep 16, 2026",
      "source": "Activity Log",
      "icon": "fitness_center"
    },
    {
      "type": "OBSERVATION",
      "title": "Restorative Nocturnal Sleep",
      "subtitle": "7.8 hours sleep duration",
      "category": "LIFESTYLE",
      "timestamp": "Sep 15, 2026",
      "source": "Sleep Tracker",
      "icon": "bedtime"
    }
  ];

  static final List<Map<String, dynamic>> alerts = [
    {
      "title": "Baseline Stability Maintained",
      "severity": "INFO",
      "time": "Today, 08:30 AM",
      "message": "Continuous biometric signals for resting heart rate and HRV have remained within your personal baseline range for 14 consecutive days."
    },
    {
      "title": "Lab Report OCR Ingested",
      "severity": "SUCCESS",
      "time": "Sep 18, 2026",
      "message": "Metabolic panel successfully parsed with 98% confidence. All values verified and added to timeline."
    }
  ];

  static final Map<String, dynamic> signalTelemetry = {
    "days": ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"],
    "restingHeartRate": {
      "values": [59.0, 60.5, 61.0, 60.0, 61.5, 60.2, 61.0],
      "mean": 60.4,
      "low": 55.0,
      "high": 66.0,
      "unit": "bpm",
      "explanation": "Your resting heart rate has remained consistent with your 60.4 bpm personal baseline over the preceding 7 days."
    },
    "hrv": {
      "values": [56.0, 54.5, 55.0, 53.8, 55.2, 54.0, 54.0],
      "mean": 55.2,
      "low": 46.0,
      "high": 64.0,
      "unit": "ms",
      "explanation": "Nocturnal HRV rMSSD shows preserved vagal balance, well within your personal baseline variance envelope."
    },
    "sleep": {
      "values": [8.1, 7.9, 7.6, 8.0, 7.7, 8.2, 7.8],
      "mean": 7.95,
      "low": 6.8,
      "high": 9.1,
      "unit": "hrs",
      "explanation": "Sleep duration exhibits steady circadian regularity matching your personal 8-hour target."
    },
    "spo2": {
      "values": [98.2, 98.4, 98.0, 98.6, 98.5, 98.1, 98.5],
      "mean": 98.2,
      "low": 96.5,
      "high": 99.5,
      "unit": "%",
      "explanation": "Blood oxygen saturation remained steady and safely within your physiological baseline."
    }
  };
}
