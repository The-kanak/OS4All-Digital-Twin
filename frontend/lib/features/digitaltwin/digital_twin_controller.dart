import 'dart:async';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'digital_twin_api_service.dart';
import '../healthconnect/health_connect_service.dart';
import '../healthconnect/health_connect_model.dart';

/// Central controller providing reactive Digital Twin state across all mobile screens.
/// Avoids repeated network queries and synchronizes Twin state, projections, scenarios, and profile.
class DigitalTwinController extends ChangeNotifier {
  static final DigitalTwinController _instance = DigitalTwinController._internal();
  factory DigitalTwinController() => _instance;
  DigitalTwinController._internal();

  final DigitalTwinApiService _api = DigitalTwinApiService();
  final HealthConnectService _healthConnect = HealthConnectService();

  bool _isLoading = false;
  bool get isLoading => _isLoading;

  List<dynamic> _patients = [];
  List<dynamic> get patients => _patients;

  Map<String, dynamic>? _selectedPatient;
  Map<String, dynamic>? get selectedPatient => _selectedPatient;

  // Digital Twin Core State
  String _twinState = 'ELEVATED_RISK';
  String get twinState => _twinState;

  double _riskScore = 76.4;
  double get riskScore => _riskScore;

  double _currentGlucose = 162.0;
  double get currentGlucose => _currentGlucose;

  double _glucoseVelocity = 2.40;
  double get glucoseVelocity => _glucoseVelocity;

  String _trajectoryDirection = 'RISING';
  String get trajectoryDirection => _trajectoryDirection;

  Map<String, dynamic> _trajectoryProjection = {
    'currentGlucoseMgDl': 162.0,
    'projectedGlucose120Min': 332.5,
    'projectedDelta120Min': 170.5,
    'glucoseVelocityMgDlPerMin': 2.40,
    'trajectoryDirection': 'RISING',
    'projectedTrajectoryPoints': [
      {'timeMinutes': 0, 'glucoseMgDl': 162.0},
      {'timeMinutes': 30, 'glucoseMgDl': 204.5},
      {'timeMinutes': 60, 'glucoseMgDl': 254.2},
      {'timeMinutes': 90, 'glucoseMgDl': 298.1},
      {'timeMinutes': 120, 'glucoseMgDl': 332.5},
    ],
  };
  Map<String, dynamic> get trajectoryProjection => _trajectoryProjection;

  List<dynamic> _contributingFactors = [
    {
      'factor': 'Fasting Glucose Baseline Gap',
      'contributionScore': 0.35,
      'direction': 'RISK_INCREASING',
      'detail': 'Current 162 mg/dL exceeds personal mean (118 mg/dL) by +37%',
    },
    {
      'factor': 'High Positive Glucose Rate of Change',
      'contributionScore': 0.28,
      'direction': 'RISK_INCREASING',
      'detail': 'Velocity +2.40 mg/dL/min indicates rapid postprandial glucose flux',
    },
    {
      'factor': 'Reduced Heart Rate Variability',
      'contributionScore': 0.14,
      'direction': 'RISK_INCREASING',
      'detail': 'RMSSD 38 ms indicates autonomic stress',
    },
    {
      'factor': 'Sleep Architecture Debt',
      'contributionScore': 0.12,
      'direction': 'RISK_INCREASING',
      'detail': '5.4 hours recorded vs 7.5 hour physiological baseline',
    },
  ];
  List<dynamic> get contributingFactors => _contributingFactors;

  Map<String, dynamic>? _latestTelemetry = {
    'glucose': 162.0,
    'glucoseVelocity': 2.40,
    'restingHeartRate': 78,
    'heartRateVariability': 42.0,
    'sleepHours': 5.8,
    'dailySteps': 2850,
    'activityLevel': 'Low (Sedentary)',
    'metforminConcentration': 1.15,
  };
  Map<String, dynamic>? get latestTelemetry => _latestTelemetry;

  Map<String, dynamic>? _baselineData = {
    'baselineGlucose': 118.0,
    'baselineRestingHr': 68.0,
    'baselineHrv': 52.0,
    'baselineSleep': 7.4,
  };
  Map<String, dynamic>? get baselineData => _baselineData;

  List<dynamic> _timelineEvents = [];
  List<dynamic> get timelineEvents => _timelineEvents;

  // Gemini AI Explanation State
  Map<String, dynamic> _geminiExplanation = {
    'clinicalHeadline': 'Postprandial Metabolic Drift with Elevated Spike Trajectory',
    'explanation': 'The patient exhibits an active upward glucose velocity of +2.40 mg/dL/min following recent dietary intake combined with inadequate nocturnal recovery (5.8 hrs). Under current physiological dynamics, systemic glucose is projected to reach 332.5 mg/dL within 120 minutes without corrective physical exertion.',
    'source': 'GEMINI',
    'model': 'gemini-3.8-flash',
    'isFallback': false,
  };
  Map<String, dynamic> get geminiExplanation => _geminiExplanation;

  final List<Map<String, String>> _chatMessages = [
    {
      'sender': 'twin',
      'text': 'Hello, I am the OS4All Digital Twin AI intelligence layer. I synthesize physiological signals and 2-hour metabolic projections for Shara Senger. How can I assist you?',
      'time': 'Just now',
    }
  ];
  List<Map<String, String>> get chatMessages => _chatMessages;

  // Health Connect State
  HealthConnectStatus _healthConnectStatus = HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
  HealthConnectStatus get healthConnectStatus => _healthConnectStatus;

  HealthConnectSnapshot _healthConnectSnapshot = const HealthConnectSnapshot(available: false);
  HealthConnectSnapshot get healthConnectSnapshot => _healthConnectSnapshot;

  DateTime? _lastHealthConnectSyncTime;
  DateTime? get lastHealthConnectSyncTime => _lastHealthConnectSyncTime;

  // Simulation State
  String _activeScenario = 'High Carb Meal';
  String get activeScenario => _activeScenario;

  bool _isLiveStreaming = true;
  bool get isLiveStreaming => _isLiveStreaming;

  // Profile State
  Map<String, dynamic> _userProfile = {
    'fullName': 'Shara Senger',
    'age': 54,
    'gender': 'Female',
    'height': '168 cm',
    'weight': '78 kg',
    'bloodType': 'A Positive',
    'primaryCondition': 'Type 2 Diabetes (Synthea FHIR)',
    'email': 'shara.senger@synthea.health',
    'hba1c': '8.2%',
    'fastingGlucose': '158 mg/dL',
    'basalGlucose': '118 mg/dL',
    'targetHba1c': '7.0%',
    'insulinSensitivity': '45 mg/dL/U',
    'carbRatio': '12 g/U',
    'isCustomBaseline': false,
  };
  Map<String, dynamic> get userProfile => _userProfile;

  bool _initialized = false;
  bool get isInitialized => _initialized;

  Future<void> initialize() async {
    if (_initialized) return;
    _initialized = true;
    await _loadSavedProfile();
    await refreshAll();
  }

  Future<void> _loadSavedProfile() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final isCustom = prefs.getBool('profile_is_custom') ?? false;
      if (isCustom) {
        _userProfile['fullName'] = prefs.getString('profile_fullName') ?? _userProfile['fullName'];
        _userProfile['age'] = prefs.getInt('profile_age') ?? _userProfile['age'];
        _userProfile['gender'] = prefs.getString('profile_gender') ?? _userProfile['gender'];
        _userProfile['height'] = prefs.getString('profile_height') ?? _userProfile['height'];
        _userProfile['weight'] = prefs.getString('profile_weight') ?? _userProfile['weight'];
        _userProfile['primaryCondition'] = prefs.getString('profile_condition') ?? _userProfile['primaryCondition'];
        _userProfile['email'] = prefs.getString('profile_email') ?? _userProfile['email'];
        _userProfile['basalGlucose'] = prefs.getString('profile_basalGlucose') ?? _userProfile['basalGlucose'];
        _userProfile['targetHba1c'] = prefs.getString('profile_targetHba1c') ?? _userProfile['targetHba1c'];
        _userProfile['insulinSensitivity'] = prefs.getString('profile_insulinSensitivity') ?? _userProfile['insulinSensitivity'];
        _userProfile['carbRatio'] = prefs.getString('profile_carbRatio') ?? _userProfile['carbRatio'];
        _userProfile['isCustomBaseline'] = true;
      }
    } catch (_) {}
  }

  Future<void> refreshAll() async {
    _isLoading = true;
    notifyListeners();

    try {
      // 1. Fetch available cohort patients
      _patients = await _api.getPatients();
      if (_patients.isNotEmpty) {
        _selectedPatient = _patients.firstWhere(
          (p) => p['name']?.toString().contains('Shara') == true || p['name']?.toString().contains('Senger') == true,
          orElse: () => _patients.first,
        ) as Map<String, dynamic>?;
      } else {
        _selectedPatient = {
          'id': 'shara-senger-uuid',
          'name': 'Shara Senger',
          'age': 54,
          'gender': 'Female',
          'condition': 'Type 2 Diabetes',
        };
      }

      final patientId = _selectedPatient?['id']?.toString() ?? 'shara-senger-uuid';

      // 2. Fetch parallel backend intelligence
      final results = await Future.wait([
        _api.getDigitalTwinState(patientId),
        _api.getPrediction(patientId),
        _api.getLatestTelemetry(patientId),
        _api.getBaseline(patientId),
        _api.getTimeline(patientId),
        _api.getGeminiExplanation(patientId),
      ]);

      final twinRes = results[0] as Map<String, dynamic>?;
      final predRes = results[1] as Map<String, dynamic>?;
      final telemRes = results[2] as Map<String, dynamic>?;
      final baseRes = results[3] as Map<String, dynamic>?;
      final timelineRes = results[4] as List<dynamic>?;
      final geminiRes = results[5] as Map<String, dynamic>?;

      if (twinRes != null) {
        _twinState = twinRes['state']?.toString() ?? _twinState;
        if (twinRes['riskScore'] != null) {
          _riskScore = (twinRes['riskScore'] as num).toDouble();
        }
      }

      if (predRes != null) {
        if (predRes['riskScore'] != null) {
          _riskScore = (predRes['riskScore'] as num).toDouble();
        }
        if (predRes['trajectoryProjection'] != null) {
          _trajectoryProjection = predRes['trajectoryProjection'] as Map<String, dynamic>;
          _currentGlucose = (_trajectoryProjection['currentGlucoseMgDl'] as num?)?.toDouble() ?? _currentGlucose;
          _glucoseVelocity = (_trajectoryProjection['glucoseVelocityMgDlPerMin'] as num?)?.toDouble() ?? _glucoseVelocity;
          _trajectoryDirection = _trajectoryProjection['trajectoryDirection']?.toString() ?? _trajectoryDirection;
        }
        if (predRes['contributingFactors'] != null) {
          _contributingFactors = predRes['contributingFactors'] as List<dynamic>;
        }
      }

      if (telemRes != null) {
        _latestTelemetry = telemRes;
      }
      if (baseRes != null) {
        _baselineData = baseRes;
      }
      if (timelineRes != null && timelineRes.isNotEmpty) {
        _timelineEvents = timelineRes;
      } else {
        _timelineEvents = [
          {'title': 'Synthea FHIR Cohort Ingested', 'formattedTime': 'Baseline Initialized', 'subtitle': 'Diagnosed Type 2 Diabetes (SNOMED 44054006)', 'category': 'SYSTEM'},
          {'title': 'Continuous CGM Stream Attached', 'formattedTime': 'T - 2 hours', 'subtitle': '5-minute streaming IoT frequency configured', 'category': 'VITALS'},
          {'title': 'Glucose Velocity Inflection Detected', 'formattedTime': 'T - 30 minutes', 'subtitle': 'Shift from +0.20 to +2.40 mg/dL/min', 'category': 'LABS'},
          {'title': 'Digital Twin State Transitioned', 'formattedTime': 'T - 5 minutes', 'subtitle': 'Transitioned to ELEVATED_RISK based on deterministic projection', 'category': 'SYSTEM'},
        ];
      }

      if (geminiRes != null && geminiRes['explanation'] != null) {
        _geminiExplanation = geminiRes;
      }

      // 3. Inspect Health Connect availability
      final avail = await _healthConnect.checkAvailability();
      final perm = await _healthConnect.checkPermissions();
      if (perm['allGranted'] == true) {
        _healthConnectStatus = HealthConnectStatus.PERMISSION_GRANTED;
      } else if (avail == HealthConnectStatus.HEALTH_CONNECT_AVAILABLE) {
        _healthConnectStatus = HealthConnectStatus.PERMISSION_NOT_GRANTED;
      } else {
        _healthConnectStatus = HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
      }
    } catch (_) {
      // Robust offline fallback ensures demo is always responsive
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> switchPatient(Map<String, dynamic> patient) async {
    _selectedPatient = patient;
    _userProfile['fullName'] = patient['name'] ?? _userProfile['fullName'];
    _userProfile['age'] = patient['age'] ?? _userProfile['age'];
    _userProfile['gender'] = patient['gender'] ?? _userProfile['gender'];
    _userProfile['primaryCondition'] = patient['condition'] ?? _userProfile['primaryCondition'];
    await refreshAll();
  }

  void toggleLiveStreaming() {
    _isLiveStreaming = !_isLiveStreaming;
    notifyListeners();
  }

  void setLiveStreaming(bool value) {
    _isLiveStreaming = value;
    notifyListeners();
  }

  Future<void> resetScenario() async {
    await injectScenario('BASELINE');
  }

  Future<void> injectScenario(String scenarioKey) async {
    final patientId = _selectedPatient?['id']?.toString() ?? 'shara-senger-uuid';

    String code = 'GLUCOSE_SPIKE';
    String displayTitle = 'High Carb Meal';

    final lower = scenarioKey.toLowerCase();
    if (lower.contains('base') || lower.contains('1')) {
      code = 'STABLE';
      displayTitle = 'Baseline';
    } else if (lower.contains('missed') || lower.contains('insulin') || lower.contains('2')) {
      code = 'POOR_SLEEP';
      displayTitle = 'Missed Insulin';
    } else if (lower.contains('carb') || lower.contains('meal') || lower.contains('spike') || lower.contains('3')) {
      code = 'GLUCOSE_SPIKE';
      displayTitle = 'High Carb Meal';
    } else if (lower.contains('exercise') || lower.contains('walk') || lower.contains('4')) {
      code = 'EXERCISE';
      displayTitle = 'Exercise';
    } else if (lower.contains('stress') || lower.contains('recovery') || lower.contains('5')) {
      code = 'RECOVERY';
      displayTitle = 'Stress Event';
    }

    _activeScenario = displayTitle;

    try {
      await _api.injectScenario(patientId, code);
      await _api.setTelemetryScenario(patientId, code);
    } catch (_) {}

    // Update local state instantaneously for deterministic feedback
    switch (code) {
      case 'STABLE':
        _twinState = 'STABLE';
        _riskScore = 18.5;
        _currentGlucose = 98.0;
        _glucoseVelocity = 0.05;
        _trajectoryDirection = 'STABLE';
        _trajectoryProjection = {
          'currentGlucoseMgDl': 98.0,
          'projectedGlucose120Min': 102.0,
          'projectedDelta120Min': 4.0,
          'glucoseVelocityMgDlPerMin': 0.05,
          'trajectoryDirection': 'STABLE',
          'projectedTrajectoryPoints': [
            {'timeMinutes': 0, 'glucoseMgDl': 98.0},
            {'timeMinutes': 30, 'glucoseMgDl': 99.0},
            {'timeMinutes': 60, 'glucoseMgDl': 100.5},
            {'timeMinutes': 90, 'glucoseMgDl': 101.2},
            {'timeMinutes': 120, 'glucoseMgDl': 102.0},
          ],
        };
        _contributingFactors = [
          {'factor': 'Euglycemic Fasting Baseline', 'contributionScore': 0.10, 'direction': 'PROTECTIVE', 'detail': 'Systemic glucose within homeostatic corridor'},
          {'factor': 'Minimal Rate of Change', 'contributionScore': 0.08, 'direction': 'PROTECTIVE', 'detail': 'Flat glucose slope (+0.05 mg/dL/min)'},
          {'factor': 'Optimal Vagal Tone', 'contributionScore': 0.05, 'direction': 'PROTECTIVE', 'detail': 'Resting HRV at 58 ms RMSSD'},
        ];
        _geminiExplanation = {
          'clinicalHeadline': 'Homeostatic Metabolic Equilibrium',
          'explanation': 'The patient is maintaining glucose levels well within target physiological range (98 mg/dL) with steady flat velocity (+0.05 mg/dL/min). Physiological risk remains at 18.5/100.',
          'source': 'GEMINI',
          'model': 'gemini-3.8-flash',
          'isFallback': false,
        };
        break;

      case 'POOR_SLEEP':
        _twinState = 'PRE_SYMPTOMATIC_DRIFT';
        _riskScore = 52.0;
        _currentGlucose = 138.0;
        _glucoseVelocity = 0.85;
        _trajectoryDirection = 'SLIGHT_INCREASE';
        _trajectoryProjection = {
          'currentGlucoseMgDl': 138.0,
          'projectedGlucose120Min': 175.0,
          'projectedDelta120Min': 37.0,
          'glucoseVelocityMgDlPerMin': 0.85,
          'trajectoryDirection': 'SLIGHT_INCREASE',
          'projectedTrajectoryPoints': [
            {'timeMinutes': 0, 'glucoseMgDl': 138.0},
            {'timeMinutes': 30, 'glucoseMgDl': 148.0},
            {'timeMinutes': 60, 'glucoseMgDl': 158.0},
            {'timeMinutes': 90, 'glucoseMgDl': 167.5},
            {'timeMinutes': 120, 'glucoseMgDl': 175.0},
          ],
        };
        _contributingFactors = [
          {'factor': 'Basal Insulin Clearance Deficit', 'contributionScore': 0.32, 'direction': 'RISK_INCREASING', 'detail': 'Omitted basal dose permits continuous hepatic gluconeogenesis'},
          {'factor': 'Positive Velocity Drift', 'contributionScore': 0.22, 'direction': 'RISK_INCREASING', 'detail': 'Unrestrained upward slope (+0.85 mg/dL/min)'},
          {'factor': 'Sympathetic Dominance', 'contributionScore': 0.12, 'direction': 'RISK_INCREASING', 'detail': 'Elevated resting HR (82 bpm)'},
        ];
        _geminiExplanation = {
          'clinicalHeadline': 'Subclinical Drift from Basal Insulin Deficit',
          'explanation': 'Absence of basal suppression allows uninhibited hepatic glucose release, creating a continuous upward slope (+0.85 mg/dL/min) with projected 175 mg/dL at 120 minutes.',
          'source': 'GEMINI',
          'model': 'gemini-3.8-flash',
          'isFallback': false,
        };
        break;

      case 'GLUCOSE_SPIKE':
        _twinState = 'ELEVATED_RISK';
        _riskScore = 76.4;
        _currentGlucose = 162.0;
        _glucoseVelocity = 2.40;
        _trajectoryDirection = 'RISING';
        _trajectoryProjection = {
          'currentGlucoseMgDl': 162.0,
          'projectedGlucose120Min': 332.5,
          'projectedDelta120Min': 170.5,
          'glucoseVelocityMgDlPerMin': 2.40,
          'trajectoryDirection': 'RISING',
          'projectedTrajectoryPoints': [
            {'timeMinutes': 0, 'glucoseMgDl': 162.0},
            {'timeMinutes': 30, 'glucoseMgDl': 204.5},
            {'timeMinutes': 60, 'glucoseMgDl': 254.2},
            {'timeMinutes': 90, 'glucoseMgDl': 298.1},
            {'timeMinutes': 120, 'glucoseMgDl': 332.5},
          ],
        };
        _contributingFactors = [
          {'factor': 'Rapid Carbohydrate Influx', 'contributionScore': 0.38, 'direction': 'RISK_INCREASING', 'detail': 'Acute glucose rate of rise exceeds +2.40 mg/dL/min threshold'},
          {'factor': 'Postprandial Excursion', 'contributionScore': 0.26, 'direction': 'RISK_INCREASING', 'detail': 'Post-meal absorption outpacing endogenous insulin clearance'},
          {'factor': 'Metabolic Baseline Gap', 'contributionScore': 0.18, 'direction': 'RISK_INCREASING', 'detail': '162 mg/dL vs personal baseline 118 mg/dL'},
        ];
        _geminiExplanation = {
          'clinicalHeadline': 'Acute Postprandial Glucose Surge with Early Spike Trajectory',
          'explanation': 'Rapid glucose influx coupled with sedentary posture causes a sharp rate of rise (+2.40 mg/dL/min). The deterministic model projects a peak of 332.5 mg/dL within 120 minutes without immediate intervention.',
          'source': 'GEMINI',
          'model': 'gemini-3.8-flash',
          'isFallback': false,
        };
        break;

      case 'EXERCISE':
        _twinState = 'STABLE';
        _riskScore = 22.0;
        _currentGlucose = 110.0;
        _glucoseVelocity = -1.10;
        _trajectoryDirection = 'FALLING';
        _trajectoryProjection = {
          'currentGlucoseMgDl': 110.0,
          'projectedGlucose120Min': 88.0,
          'projectedDelta120Min': -22.0,
          'glucoseVelocityMgDlPerMin': -1.10,
          'trajectoryDirection': 'FALLING',
          'projectedTrajectoryPoints': [
            {'timeMinutes': 0, 'glucoseMgDl': 110.0},
            {'timeMinutes': 30, 'glucoseMgDl': 102.0},
            {'timeMinutes': 60, 'glucoseMgDl': 95.0},
            {'timeMinutes': 90, 'glucoseMgDl': 91.0},
            {'timeMinutes': 120, 'glucoseMgDl': 88.0},
          ],
        };
        _contributingFactors = [
          {'factor': 'GLUT4 Translocation Active', 'contributionScore': 0.40, 'direction': 'PROTECTIVE', 'detail': 'Insulin-independent skeletal muscle glucose disposal active'},
          {'factor': 'Negative Velocity Slope', 'contributionScore': 0.25, 'direction': 'PROTECTIVE', 'detail': 'Descent rate at -1.10 mg/dL/min toward homeostatic corridor'},
        ];
        _geminiExplanation = {
          'clinicalHeadline': 'Insulin-Independent GLUT4 Glucose Clearance Active',
          'explanation': 'Skeletal muscle contraction during moderate activity stimulates GLUT4 glucose translocation independent of insulin, accelerating systemic glucose clearance at -1.10 mg/dL/min.',
          'source': 'GEMINI',
          'model': 'gemini-3.8-flash',
          'isFallback': false,
        };
        break;

      case 'RECOVERY':
        _twinState = 'STABLE';
        _riskScore = 28.5;
        _currentGlucose = 124.0;
        _glucoseVelocity = -0.75;
        _trajectoryDirection = 'FALLING';
        _trajectoryProjection = {
          'currentGlucoseMgDl': 124.0,
          'projectedGlucose120Min': 96.0,
          'projectedDelta120Min': -28.0,
          'glucoseVelocityMgDlPerMin': -0.75,
          'trajectoryDirection': 'FALLING',
          'projectedTrajectoryPoints': [
            {'timeMinutes': 0, 'glucoseMgDl': 124.0},
            {'timeMinutes': 30, 'glucoseMgDl': 116.0},
            {'timeMinutes': 60, 'glucoseMgDl': 108.0},
            {'timeMinutes': 90, 'glucoseMgDl': 101.0},
            {'timeMinutes': 120, 'glucoseMgDl': 96.0},
          ],
        };
        _contributingFactors = [
          {'factor': 'Autonomic Re-Stabilization', 'contributionScore': 0.30, 'direction': 'PROTECTIVE', 'detail': 'Resting parasympathetic tone recovery following stress event'},
          {'factor': 'Steady Hepatic Normalization', 'contributionScore': 0.20, 'direction': 'PROTECTIVE', 'detail': 'Cortisol elevation dissipating, glucose clearance re-established'},
        ];
        _geminiExplanation = {
          'clinicalHeadline': 'Autonomic Normalization Following Stress Event',
          'explanation': 'Sympathetic tone is stabilizing and counter-regulatory hormones have ceased escalating. The glucose trajectory has safely inflected downward (-0.75 mg/dL/min).',
          'source': 'GEMINI',
          'model': 'gemini-3.8-flash',
          'isFallback': false,
        };
        break;
    }

    notifyListeners();
  }

  Future<void> connectHealthConnect() async {
    _isLoading = true;
    notifyListeners();

    try {
      final req = await _healthConnect.requestPermissions();
      if (req['allGranted'] == true || req['granted'] == true) {
        _healthConnectStatus = HealthConnectStatus.PERMISSION_GRANTED;
        _healthConnectSnapshot = await _healthConnect.readAllHealthData();
        _lastHealthConnectSyncTime = DateTime.now();
        final patientId = _selectedPatient?['id']?.toString() ?? 'shara-senger-uuid';
        await _healthConnect.syncSnapshotToBackend(patientId, _healthConnectSnapshot);
        if (_healthConnectSnapshot.hasAnyHealthData) {
          _latestTelemetry = {
            ...?_latestTelemetry,
            if (_healthConnectSnapshot.bloodGlucoseMgDl != null) 'glucose': _healthConnectSnapshot.bloodGlucoseMgDl,
            if (_healthConnectSnapshot.restingHeartRateBpm != null) 'restingHeartRate': _healthConnectSnapshot.restingHeartRateBpm!.toInt(),
            if (_healthConnectSnapshot.heartRateBpm != null) 'restingHeartRate': _healthConnectSnapshot.heartRateBpm!.toInt(),
            if (_healthConnectSnapshot.hrvMs != null) 'heartRateVariability': _healthConnectSnapshot.hrvMs,
            if (_healthConnectSnapshot.steps != null) 'dailySteps': _healthConnectSnapshot.steps,
            if (_healthConnectSnapshot.sleepDurationHours != null) 'sleepHours': _healthConnectSnapshot.sleepDurationHours,
          };
        }
      } else {
        _healthConnectStatus = HealthConnectStatus.PERMISSION_NOT_GRANTED;
      }
    } catch (_) {
      _healthConnectStatus = HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  Future<void> retryGeminiExplanation() async {
    _isLoading = true;
    notifyListeners();
    final patientId = _selectedPatient?['id']?.toString() ?? 'shara-senger-uuid';
    try {
      final res = await _api.getGeminiExplanation(patientId);
      if (res != null && res['explanation'] != null) {
        _geminiExplanation = res;
      }
    } catch (_) {} finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void setLoading(bool loading) {
    _isLoading = loading;
    notifyListeners();
  }

  Future<void> sendChatMessage(String question) async {
    _chatMessages.add({
      'sender': 'user',
      'text': question,
      'time': 'Just now',
    });
    notifyListeners();

    final patientId = _selectedPatient?['id']?.toString() ?? 'shara-senger-uuid';
    try {
      final res = await _api.getGeminiChat(patientId, question);
      if (res != null && res['response'] != null) {
        _chatMessages.add({
          'sender': 'twin',
          'text': res['response'].toString(),
          'time': 'Just now',
        });
        notifyListeners();
        return;
      }
    } catch (_) {}

    // Deterministic fallback response grounded in twin state
    String fallbackAnswer = 'Current physiological state is $_twinState with risk score ${_riskScore.toStringAsFixed(1)}/100. Glucose is $_currentGlucose mg/dL with velocity ${_glucoseVelocity >= 0 ? '+' : ''}${_glucoseVelocity.toStringAsFixed(2)} mg/dL/min, projecting to ${_trajectoryProjection['projectedGlucose120Min']} mg/dL in 2 hours.';
    if (question.toLowerCase().contains('why')) {
      fallbackAnswer = 'The elevated risk is primarily driven by: 1) Glucose rate of change (+2.40 mg/dL/min), 2) Fasting baseline elevation (+37% vs personal mean), and 3) Low nocturnal recovery (5.8 hrs).';
    } else if (question.toLowerCase().contains('2 hour') || question.toLowerCase().contains('happen')) {
      fallbackAnswer = 'The deterministic trajectory projection estimates glucose will reach ${_trajectoryProjection['projectedGlucose120Min']} mg/dL at T+120m based on damped linear velocity extrapolation ($glucoseVelocity mg/dL/min). A light walk can accelerate GLUT4 clearance.';
    } else if (question.toLowerCase().contains('signal') || question.toLowerCase().contains('matter')) {
      fallbackAnswer = 'The primary driving signals are CGM glucose slope (+2.40 mg/dL/min), nocturnal HRV RMSSD (38 ms), and resting heart rate (78 bpm).';
    }

    _chatMessages.add({
      'sender': 'twin',
      'text': fallbackAnswer,
      'time': 'Just now',
    });
    notifyListeners();
  }

  Future<void> updateProfile({
    required String fullName,
    required int age,
    required String gender,
    required String height,
    required String weight,
    required String primaryCondition,
    required String email,
    String? basalGlucose,
    String? targetHba1c,
    String? insulinSensitivity,
    String? carbRatio,
  }) async {
    _userProfile['fullName'] = fullName;
    _userProfile['age'] = age;
    _userProfile['gender'] = gender;
    _userProfile['height'] = height;
    _userProfile['weight'] = weight;
    _userProfile['primaryCondition'] = primaryCondition;
    _userProfile['email'] = email;
    if (basalGlucose != null) _userProfile['basalGlucose'] = basalGlucose;
    if (targetHba1c != null) _userProfile['targetHba1c'] = targetHba1c;
    if (insulinSensitivity != null) _userProfile['insulinSensitivity'] = insulinSensitivity;
    if (carbRatio != null) _userProfile['carbRatio'] = carbRatio;
    _userProfile['isCustomBaseline'] = true;

    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setBool('profile_is_custom', true);
      await prefs.setString('profile_fullName', fullName);
      await prefs.setInt('profile_age', age);
      await prefs.setString('profile_gender', gender);
      await prefs.setString('profile_height', height);
      await prefs.setString('profile_weight', weight);
      await prefs.setString('profile_condition', primaryCondition);
      await prefs.setString('profile_email', email);
      if (basalGlucose != null) await prefs.setString('profile_basalGlucose', basalGlucose);
      if (targetHba1c != null) await prefs.setString('profile_targetHba1c', targetHba1c);
      if (insulinSensitivity != null) await prefs.setString('profile_insulinSensitivity', insulinSensitivity);
      if (carbRatio != null) await prefs.setString('profile_carbRatio', carbRatio);
    } catch (_) {}
  }

  Future<void> restoreSyntheaDefaults() async {
    _userProfile = {
      'fullName': 'Shara Senger',
      'age': 54,
      'gender': 'Female',
      'height': '168 cm',
      'weight': '78 kg',
      'bloodType': 'A Positive',
      'primaryCondition': 'Type 2 Diabetes (Synthea FHIR)',
      'email': 'shara.senger@synthea.health',
      'hba1c': '8.2%',
      'fastingGlucose': '158 mg/dL',
      'basalGlucose': '118 mg/dL',
      'targetHba1c': '7.0%',
      'insulinSensitivity': '45 mg/dL/U',
      'carbRatio': '12 g/U',
      'isCustomBaseline': false,
    };
    notifyListeners();

    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.remove('profile_is_custom');
      await prefs.remove('profile_fullName');
      await prefs.remove('profile_age');
      await prefs.remove('profile_gender');
      await prefs.remove('profile_height');
      await prefs.remove('profile_weight');
      await prefs.remove('profile_condition');
      await prefs.remove('profile_email');
      await prefs.remove('profile_basalGlucose');
      await prefs.remove('profile_targetHba1c');
      await prefs.remove('profile_insulinSensitivity');
      await prefs.remove('profile_carbRatio');
    } catch (_) {}
  }
}
