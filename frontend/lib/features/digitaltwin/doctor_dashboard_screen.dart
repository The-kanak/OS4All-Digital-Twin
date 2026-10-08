import 'dart:async';
import 'package:flutter/material.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../core/widgets/os4all_brand_logo.dart';
import './digital_twin_api_service.dart';
import './digital_twin_controller.dart';
import '../healthconnect/health_connect_model.dart';
import '../healthconnect/health_connect_service.dart';

class DoctorDashboardScreen extends StatefulWidget {
  final DigitalTwinController? controller;
  final Map<String, dynamic>? initialPatient;

  const DoctorDashboardScreen({
    super.key,
    this.controller,
    this.initialPatient,
  });

  @override
  State<DoctorDashboardScreen> createState() => _DoctorDashboardScreenState();
}

class _DoctorDashboardScreenState extends State<DoctorDashboardScreen> {
  final DigitalTwinApiService _api = DigitalTwinApiService();

  static final Map<String, dynamic> _sharaDemoPatient = {
    'id': 'bab72fc3-4f22-37b1-89bc-3c968998c695',
    'fullName': 'Shara Senger',
    'name': 'Shara Senger',
    'email': 'shara.senger@synthea.health',
    'age': 54,
    'biologicalSex': 'FEMALE',
    'gender': 'Female',
    'heightCm': 168.0,
    'weightKg': 78.0,
    'bmi': 27.6,
    'bloodType': 'A+',
    'currentTwinState': 'ELEVATED_RISK',
    'overallRiskScore': 76.40,
    'glucoseSpikeProbability': 94.0,
    'primaryCondition': 'Type 2 Diabetes',
    'condition': 'Type 2 Diabetes',
    'keyMedications': ['Metformin 1000mg BID', 'Glipizide 5mg daily', 'Atorvastatin 20mg'],
    'lastUpdatedIso': '2026-10-08T06:00:00Z',
  };

  static final Map<String, dynamic> _alexRiveraDemoPatient = {
    'id': '11111111-2222-3333-4444-555555555555',
    'fullName': 'Alex Rivera (DEMO DATA)',
    'name': 'Alex Rivera',
    'email': 'alex.rivera@demo.os4all.test',
    'age': 38,
    'biologicalSex': 'MALE',
    'gender': 'Male',
    'heightCm': 178.50,
    'weightKg': 74.20,
    'bmi': 23.3,
    'bloodType': 'O+',
    'currentTwinState': 'STABLE',
    'overallRiskScore': 13.0,
    'glucoseSpikeProbability': 14.0,
    'primaryCondition': 'Impaired Fasting Glucose (Prediabetes)',
    'condition': 'Impaired Fasting Glucose (Prediabetes)',
    'keyMedications': ['Metformin 500mg daily (intermittent)', 'Omega-3 1000mg', 'Vitamin D3 2000IU'],
    'lastUpdatedIso': '2026-10-07T10:13:34.241758Z',
  };

  static double? _parseNum(dynamic val) {
    if (val == null) return null;
    if (val is num) return val.toDouble();
    if (val is String) {
      final clean = val.replaceAll(RegExp(r'[^0-9.]'), '');
      return double.tryParse(clean);
    }
    return null;
  }

  static double _computeBmi(double heightCm, double weightKg) {
    if (heightCm <= 0) return 24.0;
    final hMeters = heightCm / 100.0;
    final bmi = weightKg / (hMeters * hMeters);
    return double.parse(bmi.toStringAsFixed(1));
  }

  static Map<String, dynamic> _buildFallbackPatientDetail(Map<String, dynamic> p, DigitalTwinController ctrl) {
    final profile = ctrl.userProfile;
    final isShara = (p['fullName']?.toString().contains('Shara') == true ||
                     p['name']?.toString().contains('Shara') == true ||
                     p['fullName']?.toString().contains('Senger') == true);
    final isAlex = (p['fullName']?.toString().contains('Alex') == true ||
                    p['id']?.toString().startsWith('11111111') == true);

    final heightVal = _parseNum(p['heightCm'] ?? (isShara ? profile['height'] : null)) ??
        (isAlex ? 178.5 : 168.0);
    final weightVal = _parseNum(p['weightKg'] ?? (isShara ? profile['weight'] : null)) ??
        (isAlex ? 74.2 : 78.0);
    final bmiVal = _parseNum(p['bmi']) ?? _computeBmi(heightVal, weightVal);

    final fullNameVal = p['fullName']?.toString() ??
        p['name']?.toString() ??
        (isShara ? profile['fullName']?.toString() : null) ??
        'Shara Senger';

    final ageVal = (p['age'] as num?)?.toInt() ??
        (isShara ? (profile['age'] as num?)?.toInt() : null) ??
        (isAlex ? 38 : 54);

    final sexVal = (p['biologicalSex']?.toString() ??
            p['gender']?.toString() ??
            (isShara ? profile['gender']?.toString() : null) ??
            (isAlex ? 'MALE' : 'FEMALE'))
        .toUpperCase();

    final bloodTypeVal = p['bloodType']?.toString() ??
        (isShara ? profile['bloodType']?.toString() : null) ??
        (isAlex ? 'O+' : 'A+');

    final primaryCondVal = p['primaryCondition']?.toString() ??
        p['condition']?.toString() ??
        (isShara ? profile['primaryCondition']?.toString() : null) ??
        (isAlex ? 'Impaired Fasting Glucose (Prediabetes)' : 'Type 2 Diabetes (Synthea FHIR)');

    return {
      'id': p['id']?.toString() ?? (isAlex ? '11111111-2222-3333-4444-555555555555' : 'bab72fc3-4f22-37b1-89bc-3c968998c695'),
      'fullName': fullNameVal,
      'age': ageVal,
      'biologicalSex': sexVal,
      'gender': sexVal,
      'heightCm': heightVal,
      'weightKg': weightVal,
      'bmi': bmiVal,
      'bloodType': bloodTypeVal,
      'lifestyleNotes': p['lifestyleNotes']?.toString() ??
          (isAlex ? 'DEMO DATA: Moderate aerobic activity, desk worker, non-smoker.' : 'Desk worker, moderate aerobic activity, nocturnal recovery debt.'),
      'historicalRecords': [
        {
          'recordType': 'CONDITION',
          'conditionOrDiagnosis': primaryCondVal,
          'icd10Code': isAlex ? 'R73.01' : 'E11.9',
          'severity': isAlex ? 'MILD' : 'MODERATE',
          'status': 'ACTIVE',
          'diagnosedDate': isAlex ? '2025-04-10' : '2019-04-10',
          'medications': isAlex ? 'Metformin 500mg daily (intermittent)' : 'Metformin 1000mg BID, Glipizide 5mg daily',
          'clinicalNotes': isAlex
              ? 'Fasting blood glucose borderline elevated. Recommended lifestyle modifications.'
              : 'Synthea longitudinal EHR record linked with continuous metabolic twin.',
        },
        {
          'recordType': 'MEDICATION',
          'conditionOrDiagnosis': isAlex ? 'Omega-3 Fatty Acids 1000mg & Vitamin D3 2000IU' : 'Metformin 1000mg BID & Atorvastatin 20mg',
          'severity': 'MILD',
          'status': 'ACTIVE',
          'diagnosedDate': isAlex ? '2024-08-20' : '2021-08-15',
          'medications': isAlex ? 'Omega-3 1000mg, Vitamin D3 2000IU' : 'Metformin, Atorvastatin',
          'clinicalNotes': 'Daily metabolic and cardiovascular stabilization protocol.',
        },
      ],
      'recentBiomarkers': [
        {'biomarker': 'HbA1c', 'value': isAlex ? 5.2 : 8.2, 'unit': '%', 'referenceLow': 4.0, 'referenceHigh': 5.6},
        {'biomarker': 'Fasting Blood Glucose', 'value': isAlex ? 88.0 : 158.0, 'unit': 'mg/dL', 'referenceLow': 70.0, 'referenceHigh': 99.0},
        {'biomarker': 'Serum Creatinine', 'value': 0.95, 'unit': 'mg/dL', 'referenceLow': 0.6, 'referenceHigh': 1.3},
        {'biomarker': 'Total Cholesterol', 'value': isAlex ? 175.0 : 185.0, 'unit': 'mg/dL', 'referenceLow': 125.0, 'referenceHigh': 200.0},
        {'biomarker': 'Triglycerides', 'value': isAlex ? 110.0 : 142.0, 'unit': 'mg/dL', 'referenceLow': 40.0, 'referenceHigh': 150.0},
      ],
      'baseline': {
        'meanHba1c': isAlex ? 5.2 : 6.8,
        'meanFastingGlucose': isAlex ? 88.0 : 118.0,
        'baselineRestingHr': isAlex ? 62.0 : 68.0,
        'baselineHrv': isAlex ? 54.0 : 52.0,
      },
    };
  }

  static Map<String, dynamic> _buildFallbackTwinState(Map<String, dynamic> p, DigitalTwinController ctrl) {
    final isAlex = (p['fullName']?.toString().contains('Alex') == true ||
                    p['id']?.toString().startsWith('11111111') == true);
    final state = p['currentTwinState']?.toString() ?? (isAlex ? 'STABLE' : ctrl.twinState);
    final riskScore = (p['overallRiskScore'] as num?)?.toDouble() ?? (isAlex ? 13.0 : ctrl.riskScore);

    return {
      'state': state,
      'overallRiskScore': riskScore,
      'metabolicRiskScore': riskScore,
      'glucoseSpikeProbability': (p['glucoseSpikeProbability'] as num?)?.toDouble() ?? (isAlex ? 14.0 : 94.0),
      'activeScenario': isAlex ? 'STABLE_PATIENT' : ctrl.activeScenario,
      'vitalsSnapshot': {
        'glucose': isAlex ? 95.0 : ctrl.currentGlucose,
        'glucoseVelocity': isAlex ? 0.05 : ctrl.glucoseVelocity,
        'hrv': isAlex ? 54.0 : 42.0,
        'restingHeartRate': isAlex ? 62.0 : 78.0,
        'sleepHours': isAlex ? 7.5 : 5.8,
        'steps': isAlex ? 8500 : 2850,
      },
      'stateDrivers': isAlex
          ? ['Physiological signals within homeostatic reference range', 'Normal vagal tone']
          : [
              'Elevated postprandial glucose flux (+2.40 mg/dL/min)',
              'Sub-optimal sleep duration (5.8h vs 7.4h baseline)',
              'Autonomic vagal tone depression (HRV 42ms vs 52ms baseline)',
            ],
      'baselineDeviations': isAlex
          ? []
          : [
              {
                'metric': 'Fasting / CGM Glucose',
                'baselineMean': 118.0,
                'currentValue': ctrl.currentGlucose,
                'deviation': ctrl.currentGlucose - 118.0,
                'percentageDeviation': ((ctrl.currentGlucose - 118.0) / 118.0) * 100,
                'zScore': 2.93,
                'trend': 'ELEVATED (Spike)',
              },
              {
                'metric': 'Resting Heart Rate',
                'baselineMean': 68.0,
                'currentValue': 78.0,
                'deviation': 10.0,
                'percentageDeviation': 14.7,
                'zScore': 1.67,
                'trend': 'MILD TACHYCARDIA',
              },
              {
                'metric': 'Heart Rate Variability',
                'baselineMean': 52.0,
                'currentValue': 42.0,
                'deviation': -10.0,
                'percentageDeviation': -19.2,
                'zScore': -1.45,
                'trend': 'AUTONOMIC STRESS',
              },
              {
                'metric': 'Nocturnal Sleep',
                'baselineMean': 7.4,
                'currentValue': 5.8,
                'deviation': -1.6,
                'percentageDeviation': -21.6,
                'zScore': -1.82,
                'trend': 'SLEEP DEBT',
              },
            ],
    };
  }

  static Map<String, dynamic> _buildFallbackPrediction(Map<String, dynamic> p, DigitalTwinController ctrl) {
    final isAlex = (p['fullName']?.toString().contains('Alex') == true ||
                    p['id']?.toString().startsWith('11111111') == true);
    if (isAlex) {
      return {
        'probability': 14.0,
        'riskScore': 13.0,
        'riskLevel': 'LOW',
        'clinicalRiskCategory': 'LOW',
        'predictionHorizon': 'Next 2 Hours',
        'currentGlucose': 95.0,
        'currentGlucoseMgDl': 95.0,
        'glucoseVelocity': 0.05,
        'glucoseVelocityMgDlPerMin': 0.05,
        'trajectoryDirection': 'STABLE',
        'projectedGlucose120Min': 98.0,
        'confidenceScore': 0.95,
        'contributingFactors': [],
        'trajectoryProjection': {
          'currentGlucoseMgDl': 95.0,
          'projectedGlucose120Min': 98.0,
          'projectedDelta120Min': 3.0,
          'glucoseVelocityMgDlPerMin': 0.05,
          'trajectoryDirection': 'STABLE',
          'projectedTrajectoryPoints': [
            {'timeMinutes': 0, 'glucoseMgDl': 95.0},
            {'timeMinutes': 30, 'glucoseMgDl': 96.0},
            {'timeMinutes': 60, 'glucoseMgDl': 97.0},
            {'timeMinutes': 90, 'glucoseMgDl': 97.5},
            {'timeMinutes': 120, 'glucoseMgDl': 98.0},
          ],
        },
      };
    }

    return {
      'probability': (p['glucoseSpikeProbability'] as num?)?.toDouble() ?? 94.0,
      'riskScore': (p['overallRiskScore'] as num?)?.toDouble() ?? ctrl.riskScore,
      'riskLevel': ctrl.twinState,
      'clinicalRiskCategory': ctrl.twinState,
      'headline': 'Postprandial Glucose Flux & Spike Trajectory',
      'predictionHorizon': 'Next 2 Hours',
      'currentGlucose': ctrl.currentGlucose,
      'currentGlucoseMgDl': ctrl.currentGlucose,
      'glucoseVelocity': ctrl.glucoseVelocity,
      'glucoseVelocityMgDlPerMin': ctrl.glucoseVelocity,
      'trajectoryDirection': ctrl.trajectoryDirection,
      'projectedGlucose120Min': ctrl.trajectoryProjection['projectedGlucose120Min'] ?? 332.5,
      'projectedDelta': ctrl.trajectoryProjection['projectedDelta120Min'] ?? 170.5,
      'confidenceScore': 0.92,
      'contributingFactors': ctrl.contributingFactors,
      'trajectoryProjection': ctrl.trajectoryProjection,
      'trajectoryPoints': ctrl.trajectoryProjection['projectedTrajectoryPoints'] ?? [],
    };
  }

  static Map<String, dynamic> _buildFallbackLatestTelemetry(DigitalTwinController ctrl) {
    final t = ctrl.latestTelemetry ?? {};
    final cgm = (t['glucose'] as num?)?.toDouble() ?? ctrl.currentGlucose;
    final vel = (t['glucoseVelocity'] as num?)?.toDouble() ?? ctrl.glucoseVelocity;
    final rhr = (t['restingHeartRate'] as num?)?.toDouble() ?? 78.0;
    final hrv = (t['heartRateVariability'] as num?)?.toDouble() ?? 42.0;
    final sleep = (t['sleepHours'] as num?)?.toDouble() ?? 5.8;
    final steps = (t['dailySteps'] as num?)?.toInt() ?? 2850;

    return {
      'cgmGlucoseMgDl': cgm,
      'glucose': cgm,
      'glucoseVelocityMgDlPerMin': vel,
      'glucoseVelocity': vel,
      'restingHeartRateBpm': rhr,
      'restingHeartRate': rhr,
      'hrvMs': hrv,
      'heartRateVariability': hrv,
      'sleepDurationHours': sleep,
      'sleepHours': sleep,
      'sleepQuality': 'FAIR',
      'steps': steps,
      'dailySteps': steps,
      'activityLevel': t['activityLevel']?.toString() ?? 'Low (Sedentary)',
      'confidenceScore': 0.94,
      'timestamp': DateTime.now().toIso8601String(),
    };
  }

  static List<dynamic> _buildDefaultTimeline() {
    return [
      {'title': 'Synthea FHIR Cohort Ingested', 'formattedTime': 'Baseline Initialized', 'subtitle': 'Diagnosed Type 2 Diabetes (SNOMED 44054006)', 'category': 'SYSTEM'},
      {'title': 'Continuous CGM Stream Attached', 'formattedTime': 'T - 2 hours', 'subtitle': '5-minute streaming IoT frequency configured', 'category': 'VITALS'},
      {'title': 'Glucose Velocity Inflection Detected', 'formattedTime': 'T - 30 minutes', 'subtitle': 'Shift from +0.20 to +2.40 mg/dL/min', 'category': 'LABS'},
      {'title': 'Digital Twin State Transitioned', 'formattedTime': 'T - 5 minutes', 'subtitle': 'Transitioned to ELEVATED_RISK based on deterministic projection', 'category': 'SYSTEM'},
    ];
  }

  List<dynamic> _patients = [_sharaDemoPatient, _alexRiveraDemoPatient];
  Map<String, dynamic>? _selectedPatient;
  Map<String, dynamic>? _patientDetail;
  Map<String, dynamic>? _twinState;
  Map<String, dynamic>? _prediction;
  Map<String, dynamic>? _activeWearableStream;
  List<dynamic> _timelineEvents = [];

  // Live Telemetry Stream State
  Map<String, dynamic>? _latestTelemetry;
  Map<String, dynamic>? _simulationStatus;
  bool _isLiveStreaming = false;
  Timer? _liveStreamTimer;

  // Android Health Connect Optional Real-Device State
  HealthConnectStatus _hcStatus = HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
  bool _hcIsConnected = false;
  bool _hcIsLoading = false;
  HealthConnectSnapshot? _latestHcSnapshot;
  String? _hcSyncStatusText;

  // Gemini AI Grounded Explanation State
  Map<String, dynamic>? _aiExplanation;
  bool _isAiExplanationLoading = false;
  String? _aiExplanationError;

  String _selectedMetric = 'glucose'; // glucose, heart_rate, hrv, sleep, steps, spo2
  bool _isLoading = false;
  String _activeScenario = 'STABLE_PATIENT';
  String? _previousState;

  // Interaction Area state
  final TextEditingController _questionController = TextEditingController();
  final List<Map<String, String>> _interactionHistory = [];
  bool _isAskingQuestion = false;

  @override
  void initState() {
    super.initState();
    _initDashboardState();
    _loadInitialData();
    _checkHealthConnectStatus();
  }

  void _initDashboardState() {
    final ctrl = widget.controller ?? DigitalTwinController();
    final p = widget.initialPatient ?? ctrl.selectedPatient ?? _sharaDemoPatient;
    _selectedPatient = p;
    _patients = [
      if (_selectedPatient != null) _selectedPatient!,
      if (_selectedPatient?['id']?.toString() != _alexRiveraDemoPatient['id']?.toString()) _alexRiveraDemoPatient,
    ];
    _patientDetail = _buildFallbackPatientDetail(p, ctrl);
    _twinState = _buildFallbackTwinState(p, ctrl);
    _prediction = _buildFallbackPrediction(p, ctrl);
    _latestTelemetry = _buildFallbackLatestTelemetry(ctrl);
    _timelineEvents = ctrl.timelineEvents.isNotEmpty ? ctrl.timelineEvents : _buildDefaultTimeline();
    _aiExplanation = ctrl.geminiExplanation;
    _isLoading = false;
  }

  Future<void> _checkHealthConnectStatus() async {
    final status = await HealthConnectService().checkAvailability();
    final perm = await HealthConnectService().checkPermissions();
    if (mounted) {
      setState(() {
        _hcStatus = status;
        _hcIsConnected = perm['hasPermissions'] == true;
      });
    }
  }

  Future<void> _connectHealthConnect() async {
    setState(() => _hcIsLoading = true);
    final res = await HealthConnectService().requestPermissions();
    if (mounted) {
      final granted = res['granted'] == true;
      setState(() {
        _hcIsConnected = granted;
        _hcStatus = granted ? HealthConnectStatus.PERMISSION_GRANTED : HealthConnectStatus.PERMISSION_NOT_GRANTED;
        _hcIsLoading = false;
        _hcSyncStatusText = granted ? 'Health Connect connected successfully.' : 'Permission was not granted.';
      });
      if (granted) {
        await _refreshHealthConnectData();
      }
    }
  }

  Future<void> _refreshHealthConnectData() async {
    if (_selectedPatient == null) return;
    setState(() => _hcIsLoading = true);
    final snapshot = await HealthConnectService().readAllHealthData();
    if (mounted) {
      setState(() {
        _latestHcSnapshot = snapshot;
      });
    }

    if (snapshot.hasAnyHealthData) {
      final patientId = _selectedPatient!['id'].toString();
      await HealthConnectService().syncSnapshotToBackend(patientId, snapshot);
      await _loadPatientData(patientId);
      if (mounted) {
        setState(() {
          _hcIsLoading = false;
          _hcSyncStatusText = 'Real-device health data synchronized to Digital Twin for ${_selectedPatient!['fullName']}.';
        });
      }
    } else {
      if (mounted) {
        setState(() {
          _hcIsLoading = false;
          _hcSyncStatusText = snapshot.status == HealthConnectStatus.NO_DATA
              ? 'Health Connect connected, but no records were found on the device for the last 24h.'
              : 'Unable to read Health Connect data: ${snapshot.errorMessage ?? "No records"}';
        });
      }
    }
  }

  Future<void> _loadAiExplanation([String? focus]) async {
    if (_selectedPatient == null) return;
    final patientId = _selectedPatient!['id'].toString();
    setState(() {
      _isAiExplanationLoading = true;
      _aiExplanationError = null;
    });
    final resp = await _api.getGeminiExplanation(patientId, focus: focus);
    if (mounted) {
      setState(() {
        _isAiExplanationLoading = false;
        if (resp != null) {
          _aiExplanation = resp;
        } else {
          _aiExplanationError = 'Unable to fetch AI explanation from backend.';
        }
      });
    }
  }

  @override
  void dispose() {
    _liveStreamTimer?.cancel();
    _questionController.dispose();
    super.dispose();
  }

  Future<void> _loadInitialData() async {
    try {
      final ctrl = widget.controller ?? DigitalTwinController();
      final targetId = widget.initialPatient?['id']?.toString() ??
          ctrl.selectedPatient?['id']?.toString() ??
          _selectedPatient?['id']?.toString();
      final targetName = widget.initialPatient?['fullName']?.toString() ??
          widget.initialPatient?['name']?.toString() ??
          ctrl.selectedPatient?['name']?.toString() ??
          ctrl.selectedPatient?['fullName']?.toString() ??
          _selectedPatient?['fullName']?.toString() ??
          'Shara';

      final backendPatients = await _api.getPatients();
      if (mounted && backendPatients.isNotEmpty) {
        // Find matching patient by ID or name in backend cohort
        Map<String, dynamic>? match;
        if (targetId != null) {
          match = backendPatients.cast<Map<String, dynamic>>().firstWhere(
            (p) => p['id']?.toString() == targetId,
            orElse: () => <String, dynamic>{},
          );
        }
        if (match == null || match.isEmpty) {
          match = backendPatients.cast<Map<String, dynamic>>().firstWhere(
            (p) => (p['fullName']?.toString().contains(targetName) == true ||
                    p['name']?.toString().contains(targetName) == true ||
                    p['fullName']?.toString().contains('Shara') == true ||
                    p['fullName']?.toString().contains('Senger') == true),
            orElse: () => <String, dynamic>{},
          );
        }

        setState(() {
          _patients = backendPatients;
          if (match != null && match.isNotEmpty) {
            _selectedPatient = match;
            _patientDetail = _buildFallbackPatientDetail(match, ctrl);
          }
        });
        if (_selectedPatient != null && _selectedPatient!['id'] != null) {
          await _loadPatientData(_selectedPatient!['id'].toString());
        }
      } else if (mounted) {
        if (_selectedPatient != null && _selectedPatient!['id'] != null) {
          await _loadPatientData(_selectedPatient!['id'].toString());
        }
      }
    } catch (_) {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _loadPatientData(String patientId) async {
    try {
      final results = await Future.wait([
        _api.getPatientDetail(patientId),
        _api.getDigitalTwinState(patientId),
        _api.getPrediction(patientId),
        _api.getWearableStream(patientId, _selectedMetric, days: 7),
        _api.getTimeline(patientId, days: 30),
        _api.getLatestTelemetry(patientId),
        _api.getTelemetrySimulationStatus(patientId),
      ]);

      final detail = results[0] as Map<String, dynamic>?;
      final twin = results[1] as Map<String, dynamic>?;
      final pred = results[2] as Map<String, dynamic>?;
      final stream = results[3] as Map<String, dynamic>?;
      final timeline = results[4] as List<dynamic>? ?? [];
      final latestTelemetry = results[5] as Map<String, dynamic>?;
      final simStatus = results[6] as Map<String, dynamic>?;

      if (mounted) {
        setState(() {
          if (twin != null && twin.isNotEmpty) {
            if (_twinState != null && _twinState!['state'] != null) {
              _previousState = _twinState!['state'].toString();
            }
            _twinState = twin;
          }
          if (detail != null && detail.isNotEmpty) {
            _patientDetail = detail;
          }
          if (pred != null && pred.isNotEmpty) {
            _prediction = pred;
          }
          if (stream != null && stream.isNotEmpty) {
            _activeWearableStream = stream;
          }
          if (timeline.isNotEmpty) {
            _timelineEvents = timeline;
          }
          if (latestTelemetry != null && latestTelemetry.isNotEmpty) {
            _latestTelemetry = latestTelemetry;
          }
          if (simStatus != null) {
            _simulationStatus = simStatus;
            _isLiveStreaming = simStatus['running'] == true;
          }
          if (twin != null && twin['activeScenario'] != null) {
            _activeScenario = twin['activeScenario'].toString();
          }
          _isLoading = false;
        });
        _loadAiExplanation();
      }
    } catch (_) {
      if (mounted) {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _toggleLiveSimulation() async {
    if (_selectedPatient == null) return;
    final patientId = _selectedPatient!['id'].toString();
    if (_isLiveStreaming) {
      await _api.stopTelemetrySimulation(patientId);
      _liveStreamTimer?.cancel();
      _liveStreamTimer = null;
      setState(() => _isLiveStreaming = false);
      final status = await _api.getTelemetrySimulationStatus(patientId);
      if (mounted) {
        setState(() => _simulationStatus = status);
      }
    } else {
      await _api.startTelemetrySimulation(patientId, intervalSeconds: 5);
      setState(() => _isLiveStreaming = true);
      _liveStreamTimer?.cancel();
      _liveStreamTimer = Timer.periodic(const Duration(seconds: 4), (timer) async {
        if (!mounted || !_isLiveStreaming || _selectedPatient == null) {
          timer.cancel();
          return;
        }
        final pId = _selectedPatient!['id'].toString();
        final latest = await _api.getLatestTelemetry(pId);
        final twin = await _api.getDigitalTwinState(pId);
        final pred = await _api.getPrediction(pId);
        final status = await _api.getTelemetrySimulationStatus(pId);
        if (mounted) {
          setState(() {
            _latestTelemetry = latest;
            _twinState = twin;
            _prediction = pred;
            _simulationStatus = status;
          });
        }
      });
    }
  }

  Future<void> _changeMetricStream(String metric) async {
    if (_selectedPatient == null) return;
    setState(() => _selectedMetric = metric);
    final stream = await _api.getWearableStream(_selectedPatient!['id'].toString(), metric, days: 7);
    if (mounted) {
      setState(() => _activeWearableStream = stream);
    }
  }

  Future<void> _injectScenario(String scenarioKey) async {
    if (_selectedPatient == null) return;
    final patientId = _selectedPatient!['id'].toString();
    setState(() => _isLoading = true);
    await _api.setTelemetryScenario(patientId, scenarioKey);
    final resp = await _api.injectScenario(patientId, scenarioKey);
    if (resp != null) {
      await _loadPatientData(patientId);
    } else {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _nextTick() async {
    if (_selectedPatient == null) return;
    final resp = await _api.nextSimulationReading(_selectedPatient!['id'].toString());
    if (resp != null) {
      await _loadPatientData(_selectedPatient!['id'].toString());
    }
  }

  Future<void> _resetToStable() async {
    if (_selectedPatient == null) return;
    final patientId = _selectedPatient!['id'].toString();
    setState(() => _isLoading = true);
    await _api.setTelemetryScenario(patientId, 'STABLE');
    await _api.resetSimulation(patientId);
    await _loadPatientData(patientId);
  }

  Future<void> _askQuestion([String? preset]) async {
    final q = preset ?? _questionController.text.trim();
    if (q.isEmpty || _selectedPatient == null) return;

    setState(() {
      _isAskingQuestion = true;
      _interactionHistory.add({'sender': 'doctor', 'text': q});
    });
    _questionController.clear();

    final resp = await _api.interact(_selectedPatient!['id'].toString(), q);
    if (mounted) {
      setState(() {
        _isAskingQuestion = false;
        if (resp != null && resp['answer'] != null) {
          _interactionHistory.add({'sender': 'twin', 'text': resp['answer'].toString()});
        } else {
          _interactionHistory.add({'sender': 'twin', 'text': 'Grounded response could not be verified by backend.'});
        }
      });
    }
  }

  Widget _safePanel(String name, Widget Function() builder) {
    try {
      return builder();
    } catch (e, stack) {
      debugPrint('[DoctorDashboard] Error in panel $name: $e\n$stack');
      return Container(
        width: double.infinity,
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: MobileTheme.warningBg,
          borderRadius: BorderRadius.circular(10),
          border: Border.all(color: MobileTheme.warningBorder),
        ),
        child: Row(
          children: [
            const Icon(Icons.warning_amber_rounded, color: MobileTheme.warning, size: 20),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                'Workstation Panel "$name" is loading or encountered a non-critical error: $e',
                style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11),
              ),
            ),
          ],
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Theme(
      data: Theme.of(context).copyWith(
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            minimumSize: Size.zero,
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          ),
        ),
      ),
      child: Scaffold(
      backgroundColor: MobileTheme.background,
      appBar: AppBar(
        backgroundColor: Colors.white,
        surfaceTintColor: Colors.transparent,
        elevation: 0,
        bottom: const PreferredSize(
          preferredSize: Size.fromHeight(1),
          child: Divider(height: 1, thickness: 1, color: MobileTheme.border),
        ),
        titleSpacing: 16,
        iconTheme: const IconThemeData(color: MobileTheme.textPrimary),
        title: const Os4AllBrandLogo(fontSize: 18),
        actions: const [
          Padding(
            padding: EdgeInsets.only(right: 16),
            child: Icon(Icons.shield_outlined, color: MobileTheme.primary, size: 20),
          ),
        ],
      ),
      body: _isLoading && _selectedPatient == null
          ? const Center(child: CircularProgressIndicator(color: MobileTheme.primary))
          : LayoutBuilder(
              builder: (context, constraints) {
                final isDesktop = constraints.maxWidth >= 900;
                final contentPadding = isDesktop ? const EdgeInsets.all(20) : const EdgeInsets.symmetric(horizontal: 14, vertical: 16);
                final mainWidth = isDesktop ? (constraints.maxWidth - 280 - 40) : (constraints.maxWidth - 28);

                final dashboardPanels = [
                  // Top Clinical Twin Header Row: Workstation badge + Prototype notice
                  _safePanel(
                    'Clinical Header',
                    () => Wrap(
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 12,
                      runSpacing: 8,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          decoration: BoxDecoration(
                            color: MobileTheme.primaryBg,
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: MobileTheme.primaryBorder, width: 1),
                          ),
                          child: const Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Icon(Icons.monitor_heart_rounded, color: MobileTheme.primary, size: 14),
                              SizedBox(width: 6),
                              Flexible(
                                child: Text(
                                  'CLINICAL TWIN WORKSTATION',
                                  style: TextStyle(color: MobileTheme.primary, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                            ],
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                          decoration: BoxDecoration(
                            color: MobileTheme.warningBg,
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: MobileTheme.warningBorder),
                          ),
                          child: const Text(
                            'HACKATHON PROTOTYPE — NOT A MEDICAL DIAGNOSIS',
                            style: TextStyle(color: Color(0xFFB45309), fontSize: 10, fontWeight: FontWeight.bold),
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 14),

                  // On mobile/tablet, render responsive patient selector header
                  if (!isDesktop) ...[
                    _safePanel('Mobile Patient Selector', () => _buildMobilePatientSelector()),
                    const SizedBox(height: 16),
                  ],

                  // 1. Patient Header & Current Twin State Hero
                  _safePanel('Patient Hero', () => _buildPatientHeroCard(mainWidth)),
                  const SizedBox(height: 20),

                  // 1b. Dual-Stream Data Fusion Architecture Visualization
                  _safePanel('Data Fusion Architecture', () => _buildDataFusionCard(mainWidth)),
                  const SizedBox(height: 20),

                  // Health Data Sources (Synthea + Synthetic Telemetry + Android Health Connect)
                  _safePanel('Health Data Sources', () => _buildHealthDataSourcesSection(mainWidth)),
                  const SizedBox(height: 20),

                  // 2. Interactive Simulation Control Toolbar
                  _safePanel('Simulation Toolbar', () => _buildSimulationToolbar()),
                  const SizedBox(height: 20),

                  // 3. Primary Prediction: Early Glucose Spike Prediction Layer
                  _safePanel('Spike Prediction Layer', () => _buildPrimaryPredictionCard(mainWidth)),
                  const SizedBox(height: 20),

                  // 3b. AI Clinical Explanation Layer (Gemini Grounded Intelligence)
                  _safePanel('AI Clinical Explanation', () => _buildAiClinicalExplanationSection()),
                  const SizedBox(height: 20),

                  // 4. Live Wearable Telemetry & Dynamic Charts
                  _safePanel('Live Health & Telemetry', () => _buildLiveHealthAndTrendsSection(mainWidth)),
                  const SizedBox(height: 20),

                  // 5. Baseline Comparison Grid (Personal Mean vs Current vs Z-score)
                  _safePanel('Baseline Comparison Grid', () => _buildBaselineComparisonSection()),
                  const SizedBox(height: 20),

                  // 6. Two-Column Layout on Desktop, Single-Column on Mobile
                  if (isDesktop)
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(child: _safePanel('Historical Records', () => _buildHistoricalRecordsCard())),
                        const SizedBox(width: 20),
                        Expanded(child: _safePanel('Event Timeline', () => _buildTimelineCard())),
                      ],
                    )
                  else
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.stretch,
                      children: [
                        _safePanel('Historical Records', () => _buildHistoricalRecordsCard()),
                        const SizedBox(height: 20),
                        _safePanel('Event Timeline', () => _buildTimelineCard()),
                      ],
                    ),
                  const SizedBox(height: 20),

                  // 7. Virtual Patient Interaction Interface (Doctor Q&A grounded in Twin)
                  _safePanel('Virtual Patient Interaction', () => _buildVirtualPatientInteractionCard()),
                  const SizedBox(height: 40),
                ];

                if (isDesktop) {
                  return Row(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // LEFT SIDEBAR: Patient Cohort Selector
                      _buildPatientSelectorSidebar(),

                      // MAIN CONTENT: Doctor Multi-Panel Dashboard
                      Expanded(
                        child: SingleChildScrollView(
                          padding: contentPadding,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: dashboardPanels,
                          ),
                        ),
                      ),
                    ],
                  );
                }

                // Mobile & Narrow Tablet layout: full width single column
                return SingleChildScrollView(
                  padding: contentPadding,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: dashboardPanels,
                  ),
                );
              },
            ),
      ),
    );
  }

  // --- Mobile Patient Selector ---
  Widget _buildMobilePatientSelector() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 6,
            children: [
              const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.groups_rounded, color: MobileTheme.primary, size: 18),
                  SizedBox(width: 8),
                  Text(
                    'VIRTUAL PATIENTS',
                    style: TextStyle(color: MobileTheme.textSecondary, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.0),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(color: const Color(0xFFF1F5F9), borderRadius: BorderRadius.circular(10)),
                child: Text('${_patients.length} loaded', style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11, fontWeight: FontWeight.bold)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: _patients.map((p) {
                final isSelected = _selectedPatient != null && _selectedPatient!['id'] == p['id'];
                final state = p['currentTwinState']?.toString() ?? 'STABLE';
                final stateColor = _getStateColor(state);
                return Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: InkWell(
                    onTap: () {
                      final map = p as Map<String, dynamic>;
                      final ctrl = widget.controller ?? DigitalTwinController();
                      setState(() {
                        _selectedPatient = map;
                        _patientDetail = _buildFallbackPatientDetail(map, ctrl);
                        _twinState = _buildFallbackTwinState(map, ctrl);
                        _prediction = _buildFallbackPrediction(map, ctrl);
                      });
                      _loadPatientData(map['id'].toString());
                    },
                    borderRadius: BorderRadius.circular(10),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: isSelected ? MobileTheme.primaryBg : Colors.white,
                        borderRadius: BorderRadius.circular(10),
                        border: Border.all(
                          color: isSelected ? MobileTheme.primary : MobileTheme.border,
                          width: isSelected ? 1.5 : 1.0,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          CircleAvatar(
                            radius: 12,
                            backgroundColor: isSelected ? MobileTheme.primary : const Color(0xFFE2E8F0),
                            child: Text(
                              (p['fullName']?.toString() ?? 'P').isNotEmpty ? (p['fullName']?.toString() ?? 'P').substring(0, 1) : 'P',
                              style: TextStyle(color: isSelected ? Colors.white : MobileTheme.textPrimary, fontSize: 11, fontWeight: FontWeight.bold),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Text(
                                p['fullName']?.toString() ?? 'Patient',
                                style: TextStyle(
                                  color: isSelected ? MobileTheme.primaryDark : MobileTheme.textPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 12,
                                ),
                              ),
                              Row(
                                children: [
                                  Container(
                                    width: 6,
                                    height: 6,
                                    decoration: BoxDecoration(color: stateColor, shape: BoxShape.circle),
                                  ),
                                  const SizedBox(width: 4),
                                  Text(
                                    state.replaceAll('_', ' '),
                                    style: TextStyle(color: stateColor, fontSize: 9, fontWeight: FontWeight.bold),
                                  ),
                                ],
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  // --- Sidebar: Synthetic Patient Cohort ---
  Widget _buildPatientSelectorSidebar() {
    return Container(
      width: 280,
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(right: BorderSide(color: MobileTheme.border, width: 1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'VIRTUAL PATIENTS',
                  style: TextStyle(color: MobileTheme.textSecondary, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.0),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(color: const Color(0xFFF1F5F9), borderRadius: BorderRadius.circular(10)),
                  child: Text('${_patients.length}', style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11, fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          ),
          const Divider(color: MobileTheme.border, height: 1),
          Expanded(
            child: ListView.builder(
              itemCount: _patients.length,
              itemBuilder: (context, idx) {
                final p = _patients[idx] as Map<String, dynamic>;
                final isSelected = _selectedPatient != null && _selectedPatient!['id'] == p['id'];
                final state = p['currentTwinState']?.toString() ?? 'STABLE';
                final stateColor = _getStateColor(state);

                return InkWell(
                  onTap: () {
                    final ctrl = widget.controller ?? DigitalTwinController();
                    setState(() {
                      _selectedPatient = p;
                      _patientDetail = _buildFallbackPatientDetail(p, ctrl);
                      _twinState = _buildFallbackTwinState(p, ctrl);
                      _prediction = _buildFallbackPrediction(p, ctrl);
                    });
                    _loadPatientData(p['id'].toString());
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    decoration: BoxDecoration(
                      color: isSelected ? MobileTheme.primaryBg : Colors.transparent,
                      border: Border(
                        left: BorderSide(
                          color: isSelected ? MobileTheme.primary : Colors.transparent,
                          width: 4,
                        ),
                        bottom: const BorderSide(color: MobileTheme.border, width: 0.5),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                p['fullName']?.toString() ?? 'Patient',
                                style: TextStyle(
                                  color: isSelected ? MobileTheme.primaryDark : MobileTheme.textPrimary,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: stateColor.withOpacity(0.12),
                                borderRadius: BorderRadius.circular(4),
                                border: Border.all(color: stateColor.withOpacity(0.4), width: 0.8),
                              ),
                              child: Text(
                                state.replaceAll('_', ' '),
                                style: TextStyle(color: stateColor, fontSize: 9, fontWeight: FontWeight.w800),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${p['age']}y • ${p['biologicalSex']} • BMI ${p['bmi']}',
                          style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 12),
                        ),
                        const SizedBox(height: 6),
                        Wrap(
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            const Text('Risk Score: ', style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11)),
                            Text(
                              '${p['glucoseSpikeProbability']} / 100',
                              style: TextStyle(
                                color: (p['glucoseSpikeProbability'] as num? ?? 0) > 50 ? MobileTheme.critical : MobileTheme.stable,
                                fontWeight: FontWeight.bold,
                                fontSize: 11,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  // --- 1. Hero Card: Digital Twin Status ---
  Widget _buildPatientHeroCard([double availableWidth = 800]) {
    final detail = _patientDetail ?? {};
    final twin = _twinState ?? {};
    final selected = _selectedPatient ?? {};
    final state = twin['state']?.toString() ?? selected['currentTwinState']?.toString() ?? 'STABLE';
    final stateColor = _getStateColor(state);
    final drivers = twin['stateDrivers'] as List<dynamic>? ?? [];
    final isNarrow = availableWidth < 650;

    final patientName = (detail['fullName']?.toString().isNotEmpty == true)
        ? detail['fullName'].toString()
        : (selected['fullName']?.toString().isNotEmpty == true)
            ? selected['fullName'].toString()
            : (selected['name']?.toString().isNotEmpty == true)
                ? selected['name'].toString()
                : 'Shara Senger';

    final avatarWidget = CircleAvatar(
      radius: 26,
      backgroundColor: MobileTheme.primaryBg,
      child: Text(
        patientName.isNotEmpty ? patientName.substring(0, 1) : 'P',
        style: const TextStyle(color: MobileTheme.primary, fontSize: 22, fontWeight: FontWeight.bold),
      ),
    );

    final idRaw = selected['id']?.toString() ?? detail['id']?.toString() ?? 'bab72fc3';
    final idDisplay = idRaw.contains('-')
        ? idRaw.split('-').first
        : (idRaw.length > 8 ? idRaw.substring(0, 8) : idRaw);

    final nameAndIdWidget = Wrap(
      crossAxisAlignment: WrapCrossAlignment.center,
      spacing: 10,
      runSpacing: 4,
      children: [
        Text(
          patientName,
          style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 18, fontWeight: FontWeight.bold),
        ),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
          decoration: BoxDecoration(
            color: const Color(0xFFF1F5F9),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: MobileTheme.border),
          ),
          child: Text(
            'ID: $idDisplay',
            style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontFamily: 'monospace'),
          ),
        ),
      ],
    );

    final ageVal = detail['age'] ?? selected['age'] ?? 54;
    final genderVal = detail['biologicalSex'] ?? detail['gender'] ?? selected['biologicalSex'] ?? selected['gender'] ?? 'FEMALE';
    final heightVal = detail['heightCm'] ?? selected['heightCm'] ?? 168.0;
    final weightVal = detail['weightKg'] ?? selected['weightKg'] ?? 78.0;
    final bmiVal = detail['bmi'] ?? selected['bmi'] ?? 27.6;
    final bloodTypeVal = detail['bloodType'] ?? selected['bloodType'] ?? 'A+';

    final demographicsWidget = Text(
      '$ageVal years old • $genderVal • Height: $heightVal cm • Weight: $weightVal kg • BMI: $bmiVal • Blood Type: $bloodTypeVal',
      style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 12),
    );

    final lifestyleNotes = detail['lifestyleNotes']?.toString() ?? selected['lifestyleNotes']?.toString() ?? '';
    final lifestyleWidget = lifestyleNotes.isNotEmpty
        ? Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              lifestyleNotes,
              style: const TextStyle(color: MobileTheme.textSubtle, fontSize: 11, fontStyle: FontStyle.italic),
            ),
          )
        : const SizedBox.shrink();

    final riskNum = twin['overallRiskScore'] ?? selected['overallRiskScore'] ?? 76.4;
    final riskScoreDisplay = (riskNum is num) ? riskNum.toStringAsFixed(1) : riskNum.toString();

    final statePillWidget = Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: stateColor.withOpacity(0.08),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: stateColor, width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: isNarrow ? CrossAxisAlignment.start : CrossAxisAlignment.end,
        mainAxisSize: MainAxisSize.min,
        children: [
          const Text('DIGITAL TWIN STATE', style: TextStyle(color: MobileTheme.textSecondary, fontSize: 10, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(Icons.hub_rounded, color: stateColor, size: 18),
              const SizedBox(width: 6),
              Text(
                state.replaceAll('_', ' '),
                style: TextStyle(color: stateColor, fontSize: 16, fontWeight: FontWeight.w900),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Risk Score: $riskScoreDisplay / 100',
            style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12, fontWeight: FontWeight.w600),
          ),
        ],
      ),
    );

    return Container(
      width: double.infinity,
      padding: EdgeInsets.all(isNarrow ? 16 : 22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (isNarrow)
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    avatarWidget,
                    const SizedBox(width: 12),
                    Expanded(child: nameAndIdWidget),
                  ],
                ),
                const SizedBox(height: 10),
                demographicsWidget,
                lifestyleWidget,
                const SizedBox(height: 12),
                statePillWidget,
              ],
            )
          else
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                avatarWidget,
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      nameAndIdWidget,
                      const SizedBox(height: 6),
                      demographicsWidget,
                      lifestyleWidget,
                    ],
                  ),
                ),
                const SizedBox(width: 16),
                statePillWidget,
              ],
            ),

          const SizedBox(height: 16),
          const Divider(color: MobileTheme.border),
          const SizedBox(height: 10),

          // Active State Drivers
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 6,
            children: [
              const Text(
                'ACTIVE STATE DRIVERS: ',
                style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold),
              ),
              ...drivers.map((d) => Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: const Color(0xFFF8FAFC),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: stateColor.withOpacity(0.4)),
                ),
                child: Text(
                  d.toString(),
                  style: TextStyle(color: stateColor, fontSize: 11, fontWeight: FontWeight.w600),
                ),
              )),
            ],
          ),

          // State Transition Tracking Banner
          if (_previousState != null && _previousState != state) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              decoration: BoxDecoration(
                color: MobileTheme.primaryBg,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: MobileTheme.primaryBorder),
              ),
              child: Wrap(
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 8,
                runSpacing: 4,
                children: [
                  const Icon(Icons.compare_arrows_rounded, color: MobileTheme.primary, size: 18),
                  Text(
                    'STATE TRANSITION: ${_previousState!.replaceAll('_', ' ')} → ${state.replaceAll('_', ' ')}',
                    style: const TextStyle(color: MobileTheme.primary, fontSize: 12, fontWeight: FontWeight.bold),
                  ),
                  Text(
                    '• Triggered by: ${drivers.isNotEmpty ? drivers.first : "Telemetry shift"}',
                    style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontStyle: FontStyle.italic),
                  ),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }

  // --- 1b. Dual-Stream Data Fusion Architecture Card ---
  Widget _buildDataFusionCard([double availableWidth = 800]) {
    final detail = _patientDetail ?? {};
    final twin = _twinState ?? {};
    final vitals = twin['vitalsSnapshot'] as Map<String, dynamic>? ?? {};
    final state = twin['state']?.toString() ?? 'STABLE';
    final stateColor = _getStateColor(state);
    final records = detail['historicalRecords'] as List<dynamic>? ?? [];
    final primaryCond = records.isNotEmpty ? (records.first['conditionOrDiagnosis'] ?? 'Healthy') : 'Baseline Normal';
    final baseline = detail['baseline'] as Map<String, dynamic>? ?? {};
    final baselineA1c = baseline['meanHba1c'] ?? 5.4;
    final baselineFasting = baseline['meanFastingGlucose'] ?? 88;

    // Live Telemetry Stream resolution
    final cgmVal = _latestTelemetry?['cgmGlucoseMgDl'] != null 
        ? (_latestTelemetry!['cgmGlucoseMgDl'] as num).toDouble() 
        : _latestTelemetry?['glucose'] != null
            ? (_latestTelemetry!['glucose'] as num).toDouble()
            : (vitals['glucose'] as num? ?? 162.0).toDouble();
    final velocityVal = _latestTelemetry?['glucoseVelocityMgDlPerMin'] != null
        ? (_latestTelemetry!['glucoseVelocityMgDlPerMin'] as num).toDouble()
        : _latestTelemetry?['glucoseVelocity'] != null
            ? (_latestTelemetry!['glucoseVelocity'] as num).toDouble()
            : 2.40;
    final hrvVal = _latestTelemetry?['hrvMs'] != null
        ? (_latestTelemetry!['hrvMs'] as num).toDouble()
        : _latestTelemetry?['heartRateVariability'] != null
            ? (_latestTelemetry!['heartRateVariability'] as num).toDouble()
            : (vitals['hrv'] as num? ?? 42.0).toDouble();
    final rhrVal = _latestTelemetry?['restingHeartRateBpm'] != null
        ? (_latestTelemetry!['restingHeartRateBpm'] as num).toDouble()
        : _latestTelemetry?['restingHeartRate'] != null
            ? (_latestTelemetry!['restingHeartRate'] as num).toDouble()
            : (vitals['restingHeartRate'] as num? ?? 78.0).toDouble();
    final sleepHours = _latestTelemetry?['sleepDurationHours'] != null
        ? (_latestTelemetry!['sleepDurationHours'] as num).toDouble()
        : _latestTelemetry?['sleepHours'] != null
            ? (_latestTelemetry!['sleepHours'] as num).toDouble()
            : (vitals['sleepHours'] as num? ?? 5.8).toDouble();
    final sleepQuality = _latestTelemetry?['sleepQuality']?.toString() ?? 'FAIR';
    final stepsVal = _latestTelemetry?['steps'] != null
        ? (_latestTelemetry!['steps'] as num).toInt()
        : _latestTelemetry?['dailySteps'] != null
            ? (_latestTelemetry!['dailySteps'] as num).toInt()
            : (vitals['steps'] as num? ?? 2850).toInt();
    final activityLvl = _latestTelemetry?['activityLevel']?.toString() ?? 'MODERATE';
    final packetConf = _latestTelemetry?['confidenceScore'] != null
        ? ((_latestTelemetry!['confidenceScore'] as num).toDouble() * 100).toInt()
        : 95;
    final packetTime = _latestTelemetry?['timestamp']?.toString().split('T').last.split('.').first ?? 'Live';

    final isNarrow = availableWidth < 800;
    final stream1Items = [
      'Diagnosis: $primaryCond',
      'Baseline HbA1c: $baselineA1c%',
      'Fasting Glucose: $baselineFasting mg/dL',
      'Demographics: ${detail['age'] ?? _selectedPatient?['age'] ?? 54}y ${detail['biologicalSex'] ?? detail['gender'] ?? _selectedPatient?['biologicalSex'] ?? 'FEMALE'} • BMI ${detail['bmi'] ?? _selectedPatient?['bmi'] ?? 27.6}',
    ];
    final velocityStr = ' (${velocityVal >= 0 ? "+" : ""}${velocityVal.toStringAsFixed(2)} mg/dL/min)';
    final stream2Items = [
      'CGM Glucose: ${cgmVal.toStringAsFixed(1)} mg/dL$velocityStr',
      'Autonomic HRV: ${hrvVal.toStringAsFixed(1)} ms • Resting HR: ${rhrVal.toStringAsFixed(0)} bpm',
      'Sleep: ${sleepHours.toStringAsFixed(1)} hrs ($sleepQuality)',
      'Activity: $stepsVal steps • $activityLvl (Conf: $packetConf%)',
    ];

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 10,
            runSpacing: 8,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    padding: const EdgeInsets.all(6),
                    decoration: BoxDecoration(
                      color: MobileTheme.primaryBg,
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Icon(Icons.merge_type_rounded, color: MobileTheme.primary, size: 18),
                  ),
                  const SizedBox(width: 8),
                  const Flexible(
                    child: Text(
                      'DUAL-STREAM DATA FUSION ARCHITECTURE',
                      style: TextStyle(color: MobileTheme.primary, fontSize: 11.5, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: MobileTheme.border),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: _isLiveStreaming ? MobileTheme.stable : MobileTheme.textSubtle,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      _isLiveStreaming ? 'Continuous Telemetry Fusion (Active)' : 'Continuous Fusion Cycle',
                      style: TextStyle(color: _isLiveStreaming ? MobileTheme.stable : MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.w600),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          isNarrow
              ? Column(
                  children: [
                    _buildFusionStreamBox(
                      title: 'STREAM 1: STATIC / HISTORICAL EHR',
                      subtitle: 'Synthea FHIR Baseline & Longitudinal Labs',
                      icon: Icons.history_edu_rounded,
                      accentColor: MobileTheme.primary,
                      items: stream1Items,
                    ),
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 8),
                      child: Icon(Icons.add_rounded, color: MobileTheme.primary, size: 24),
                    ),
                    _buildFusionStreamBox(
                      title: 'STREAM 2: DYNAMIC WEARABLE TELEMETRY',
                      subtitle: 'Simulated Real-Time IoT & CGM (5-min stream)',
                      icon: Icons.sensors_rounded,
                      accentColor: MobileTheme.geminiPurple,
                      items: stream2Items,
                    ),
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 8),
                      child: Icon(Icons.arrow_downward_rounded, color: MobileTheme.primary, size: 24),
                    ),
                    _buildFusionResultBox(state, stateColor, twin, packetTime),
                  ],
                )
              : Row(
                  crossAxisAlignment: CrossAxisAlignment.center,
                  children: [
                    Expanded(
                      child: _buildFusionStreamBox(
                        title: 'STREAM 1: STATIC / EHR',
                        subtitle: 'Synthea FHIR Baseline & Records',
                        icon: Icons.history_edu_rounded,
                        accentColor: MobileTheme.primary,
                        items: stream1Items,
                      ),
                    ),
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 8),
                      child: Icon(Icons.add_rounded, color: MobileTheme.primary, size: 24),
                    ),
                    Expanded(
                      child: _buildFusionStreamBox(
                        title: 'STREAM 2: DYNAMIC WEARABLES',
                        subtitle: 'Simulated Real-Time IoT & CGM (5-min)',
                        icon: Icons.sensors_rounded,
                        accentColor: MobileTheme.geminiPurple,
                        items: stream2Items,
                      ),
                    ),
                    const Padding(
                      padding: EdgeInsets.symmetric(horizontal: 8),
                      child: Icon(Icons.arrow_forward_rounded, color: MobileTheme.primary, size: 24),
                    ),
                    Expanded(
                      child: _buildFusionResultBox(state, stateColor, twin, packetTime),
                    ),
                  ],
                ),
        ],
      ),
    );
  }

  Widget _buildFusionStreamBox({
    required String title,
    required String subtitle,
    required IconData icon,
    required Color accentColor,
    required List<String> items,
  }) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: accentColor.withOpacity(0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: accentColor, size: 16),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  title,
                  style: TextStyle(color: accentColor, fontSize: 11, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 10)),
          const SizedBox(height: 8),
          ...items.map((it) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 1.5),
            child: Row(
              children: [
                const Text('• ', style: TextStyle(color: MobileTheme.textSubtle, fontSize: 11)),
                Expanded(
                  child: Text(
                    it,
                    style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          )),
        ],
      ),
    );
  }

  Widget _buildFusionResultBox(String state, Color stateColor, Map<String, dynamic> twin, [String? packetTime]) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: stateColor, width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(Icons.hub_rounded, color: stateColor, size: 16),
              const SizedBox(width: 6),
              const Expanded(
                child: Text(
                  'FUSED DIGITAL TWIN STATE',
                  style: TextStyle(color: MobileTheme.textPrimary, fontSize: 11, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            state.replaceAll('_', ' '),
            style: TextStyle(color: stateColor, fontSize: 14, fontWeight: FontWeight.w900),
          ),
          const SizedBox(height: 6),
          Text(
            'Risk Score: ${twin['overallRiskScore'] ?? 0} / 100',
            style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 2),
          Text(
            'Horizon: ${twin['predictionHorizon'] ?? "Next 2 Hours"}',
            style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 10),
          ),
          if (packetTime != null && packetTime.isNotEmpty) ...[
            const SizedBox(height: 2),
            Text(
              'Sync: $packetTime',
              style: const TextStyle(color: MobileTheme.textSubtle, fontSize: 9, fontFamily: 'monospace'),
            ),
          ],
        ],
      ),
    );
  }

  // --- 1c. Health Data Sources (Dual-Stream + Android Health Connect) ---
  Widget _buildHealthDataSourcesSection([double availableWidth = 800]) {
    final bool isUnavailable = _hcStatus == HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    final bool isGranted = _hcIsConnected;
    final snapshot = _latestHcSnapshot;
    final isNarrow = availableWidth < 800;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: MobileTheme.successBg,
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Icon(Icons.cable_rounded, color: MobileTheme.stable, size: 18),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'HEALTH DATA INGESTION SOURCES',
                      style: TextStyle(color: MobileTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                    ),
                    Text(
                      'Static Synthea EHR + Simulated 5-min Dynamic Telemetry + Optional Android Health Connect (Read-Only)',
                      style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          Builder(
            builder: (context) {
              final cards = [
                _buildSourceCard(
                  title: 'Synthea FHIR EHR',
                  subtitle: 'Historical baseline & clinical records',
                  statusBadge: 'ACTIVE / LOADED',
                  statusColor: MobileTheme.primary,
                  icon: Icons.folder_shared_rounded,
                  details: [
                    'Patient: ${_selectedPatient?["fullName"] ?? "Loaded"}',
                    'Demographics, BMI, Conditions',
                    'Longitudinal lab biomarkers',
                  ],
                ),
                _buildSourceCard(
                  title: 'Simulated Dynamic Stream',
                  subtitle: '5-minute IoT/CGM physiological stream',
                  statusBadge: _isLiveStreaming ? 'STREAMING ACTIVE' : 'AVAILABLE (STANDBY)',
                  statusColor: _isLiveStreaming ? MobileTheme.stable : MobileTheme.geminiPurple,
                  icon: Icons.sensors_rounded,
                  details: [
                    'Continuous CGM glucose & velocities',
                    'Heart rate, HRV, Resting HR',
                    '5 simulated clinical perturbation scenarios',
                  ],
                ),
                _buildSourceCard(
                  title: 'Android Health Connect',
                  subtitle: 'Optional on-device real sensor integration',
                  statusBadge: isGranted
                      ? 'CONNECTED (READ-ONLY)'
                      : (isUnavailable ? 'NOT AVAILABLE' : 'PERMISSION NEEDED'),
                  statusColor: isGranted
                      ? MobileTheme.stable
                      : (isUnavailable ? MobileTheme.textSubtle : MobileTheme.drift),
                  icon: Icons.phone_android_rounded,
                  details: [
                    'Read-only: Steps, HR, HRV, Sleep, RHR, Glucose',
                    'SDK: androidx.health.connect:connect-client',
                    'Fallback: Synthetic stream remains primary',
                  ],
                  trailingAction: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      if (!isGranted && !isUnavailable)
                        ElevatedButton.icon(
                          onPressed: _hcIsLoading ? null : _connectHealthConnect,
                          style: ElevatedButton.styleFrom(
                            backgroundColor: MobileTheme.primary,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                            textStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                          ),
                          icon: _hcIsLoading
                              ? const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                              : const Icon(Icons.link_rounded, size: 14),
                          label: const Text('Connect'),
                        ),
                      if (isGranted) ...[
                        OutlinedButton.icon(
                          onPressed: _hcIsLoading ? null : _refreshHealthConnectData,
                          style: OutlinedButton.styleFrom(
                            foregroundColor: MobileTheme.primary,
                            side: const BorderSide(color: MobileTheme.primary),
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                            textStyle: const TextStyle(fontSize: 11),
                          ),
                          icon: _hcIsLoading
                              ? const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: MobileTheme.primary))
                              : const Icon(Icons.sync_rounded, size: 14),
                          label: const Text('Sync Telemetry'),
                        ),
                      ],
                    ],
                  ),
                ),
              ];

              return isNarrow
                  ? Column(children: cards.map((c) => Padding(padding: const EdgeInsets.only(bottom: 12), child: c)).toList())
                  : Row(crossAxisAlignment: CrossAxisAlignment.start, children: cards.map((c) => Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 6), child: c))).toList());
            },
          ),
          if (_hcSyncStatusText != null && _hcSyncStatusText!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: const Color(0xFFF8FAFC),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: MobileTheme.border),
              ),
              child: Row(
                children: [
                  Icon(
                    isGranted ? Icons.check_circle_outline_rounded : Icons.info_outline_rounded,
                    color: isGranted ? MobileTheme.stable : MobileTheme.textSubtle,
                    size: 16,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _hcSyncStatusText!,
                      style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11),
                    ),
                  ),
                  if (snapshot != null && snapshot.hasAnyHealthData) ...[
                    Text(
                      'Steps: ${snapshot.steps ?? "—"} • HR: ${snapshot.heartRateBpm?.toStringAsFixed(0) ?? "—"} • Glucose: ${snapshot.bloodGlucoseMgDl?.toStringAsFixed(0) ?? "—"}',
                      style: const TextStyle(color: MobileTheme.primary, fontSize: 11, fontFamily: 'monospace', fontWeight: FontWeight.bold),
                    ),
                  ],
                ],
              ),
            ),
          ],
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(
              color: MobileTheme.warningBg,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: MobileTheme.warningBorder),
            ),
            child: const Row(
              children: [
                Icon(Icons.privacy_tip_outlined, color: Color(0xFFB45309), size: 14),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Health Connect operates in strict read-only mode. Real-device metrics are mapped into the prototype Digital Twin for the active session. Not intended for clinical diagnostic use.',
                    style: TextStyle(color: Color(0xFF92400E), fontSize: 10),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSourceCard({
    required String title,
    required String subtitle,
    required String statusBadge,
    required Color statusColor,
    required IconData icon,
    required List<String> details,
    Widget? trailingAction,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: statusColor.withOpacity(0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: statusColor, size: 18),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 10)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: statusColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: statusColor.withOpacity(0.4)),
            ),
            child: Text(
              statusBadge,
              style: TextStyle(color: statusColor, fontSize: 10, fontWeight: FontWeight.bold),
            ),
          ),
          const SizedBox(height: 10),
          ...details.map((d) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 2),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('• ', style: TextStyle(color: MobileTheme.textSubtle, fontSize: 11)),
                Expanded(
                  child: Text(d, style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11)),
                ),
              ],
            ),
          )),
          if (trailingAction != null) ...[
            const SizedBox(height: 10),
            trailingAction,
          ],
        ],
      ),
    );
  }

  // --- 2. Simulation Toolbar ---
  Widget _buildSimulationToolbar() {
    final ticks = _simulationStatus?['ticksGenerated'] ?? 0;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 12,
            runSpacing: 8,
            children: [
              const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.tune_rounded, color: MobileTheme.primary, size: 18),
                  SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      'SIMULATION CONTROLLER',
                      style: TextStyle(color: MobileTheme.primary, fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              // Live Simulation Indicator
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: _isLiveStreaming ? MobileTheme.successBg : const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(
                    color: _isLiveStreaming ? MobileTheme.successBorder : MobileTheme.border,
                    width: 0.8,
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.circle,
                      size: 8,
                      color: _isLiveStreaming ? MobileTheme.stable : MobileTheme.textSubtle,
                    ),
                    const SizedBox(width: 6),
                    Text(
                      _isLiveStreaming ? 'LIVE TELEMETRY (Tick #$ticks)' : 'STREAM PAUSED',
                      style: TextStyle(
                        color: _isLiveStreaming ? MobileTheme.stable : MobileTheme.textSecondary,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                'Active Scenario: ${_activeScenario.replaceAll('_', ' ')}',
                style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 12, fontWeight: FontWeight.w600),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 10,
            runSpacing: 10,
            children: [
              ElevatedButton.icon(
                onPressed: _toggleLiveSimulation,
                style: ElevatedButton.styleFrom(
                  backgroundColor: _isLiveStreaming ? MobileTheme.critical : MobileTheme.stable,
                  foregroundColor: Colors.white,
                ),
                icon: Icon(_isLiveStreaming ? Icons.pause_circle_filled : Icons.play_circle_filled, size: 16),
                label: Text(_isLiveStreaming ? 'Pause Stream' : 'Start Live Telemetry (5s)'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('STABLE_PATIENT'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'STABLE_PATIENT' || _activeScenario == 'STABLE' ? MobileTheme.primary : const Color(0xFFF1F5F9),
                  foregroundColor: _activeScenario == 'STABLE_PATIENT' || _activeScenario == 'STABLE' ? Colors.white : MobileTheme.textPrimary,
                  side: BorderSide(color: _activeScenario == 'STABLE_PATIENT' || _activeScenario == 'STABLE' ? MobileTheme.primary : MobileTheme.border),
                  elevation: 0,
                ),
                icon: const Icon(Icons.check_circle_outline, size: 16),
                label: const Text('1. Stable Homeostasis'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('POOR_SLEEP'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'POOR_SLEEP' ? MobileTheme.drift : const Color(0xFFF1F5F9),
                  foregroundColor: _activeScenario == 'POOR_SLEEP' ? Colors.white : MobileTheme.textPrimary,
                  side: BorderSide(color: _activeScenario == 'POOR_SLEEP' ? MobileTheme.drift : MobileTheme.border),
                  elevation: 0,
                ),
                icon: const Icon(Icons.bedtime_outlined, size: 16),
                label: const Text('2. Poor Sleep → Drift'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('HIGH_ACTIVITY'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'HIGH_ACTIVITY' ? MobileTheme.stable : const Color(0xFFF1F5F9),
                  foregroundColor: _activeScenario == 'HIGH_ACTIVITY' ? Colors.white : MobileTheme.textPrimary,
                  side: BorderSide(color: _activeScenario == 'HIGH_ACTIVITY' ? MobileTheme.stable : MobileTheme.border),
                  elevation: 0,
                ),
                icon: const Icon(Icons.directions_run, size: 16),
                label: const Text('3. High Activity (GLUT4)'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('GLUCOSE_SPIKE'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'GLUCOSE_SPIKE' || _activeScenario == 'GLUCOSE_RISE' ? MobileTheme.critical : const Color(0xFFF1F5F9),
                  foregroundColor: _activeScenario == 'GLUCOSE_SPIKE' || _activeScenario == 'GLUCOSE_RISE' ? Colors.white : MobileTheme.textPrimary,
                  side: BorderSide(color: _activeScenario == 'GLUCOSE_SPIKE' || _activeScenario == 'GLUCOSE_RISE' ? MobileTheme.critical : MobileTheme.border),
                  elevation: 0,
                ),
                icon: const Icon(Icons.warning_amber_rounded, size: 16),
                label: const Text('4. Glucose Spike Risk'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('RECOVERY'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'RECOVERY' ? MobileTheme.stable : const Color(0xFFF1F5F9),
                  foregroundColor: _activeScenario == 'RECOVERY' ? Colors.white : MobileTheme.textPrimary,
                  side: BorderSide(color: _activeScenario == 'RECOVERY' ? MobileTheme.stable : MobileTheme.border),
                  elevation: 0,
                ),
                icon: const Icon(Icons.replay_rounded, size: 16),
                label: const Text('5. Recovery After Walk'),
              ),
              OutlinedButton.icon(
                onPressed: _nextTick,
                style: OutlinedButton.styleFrom(
                  foregroundColor: MobileTheme.primary,
                  side: const BorderSide(color: MobileTheme.primary),
                ),
                icon: const Icon(Icons.fast_forward_rounded, size: 16),
                label: const Text('Next Reading Tick'),
              ),
              OutlinedButton.icon(
                onPressed: _resetToStable,
                style: OutlinedButton.styleFrom(
                  foregroundColor: MobileTheme.textSecondary,
                  side: const BorderSide(color: MobileTheme.border),
                ),
                icon: const Icon(Icons.restart_alt_rounded, size: 16),
                label: const Text('Reset'),
              ),
            ],
          ),
        ],
      ),
    );
  }


  // --- 3. Primary Prediction: Early Glucose Spike Prediction Layer ---
  Widget _buildPrimaryPredictionCard([double availableWidth = 800]) {
    final pred = _prediction ?? {};
    final prob = (pred['probability'] as num? ??
        pred['riskScore'] as num? ??
        _twinState?['overallRiskScore'] as num? ??
        _selectedPatient?['overallRiskScore'] as num? ??
        76.4).toDouble();
    final riskLevel = pred['riskLevel']?.toString() ??
        pred['clinicalRiskCategory']?.toString() ??
        _twinState?['state']?.toString() ??
        'ELEVATED_RISK';
    final factors = (pred['contributingFactors'] as List<dynamic>?) ?? [];
    final actions = (pred['recommendedClinicalActions'] as List<dynamic>?) ?? [];

    Color riskColor = MobileTheme.stable;
    if (prob >= 75) riskColor = MobileTheme.critical;
    else if (prob >= 50) riskColor = MobileTheme.drift;
    else if (prob >= 30) riskColor = const Color(0xFFEAB308);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: riskColor.withOpacity(0.5), width: 1.5),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 12,
            runSpacing: 10,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: riskColor.withOpacity(0.12),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(Icons.batch_prediction_rounded, color: riskColor, size: 22),
                  ),
                  const SizedBox(width: 12),
                  Flexible(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'PRIMARY CLINICAL USE CASE',
                          style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                        ),
                        Text(
                          pred['headline']?.toString() ?? 'Metabolic Spike Prediction',
                          style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.bold),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                decoration: BoxDecoration(
                  color: riskColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: riskColor),
                ),
                child: Wrap(
                  crossAxisAlignment: WrapCrossAlignment.center,
                  spacing: 6,
                  runSpacing: 4,
                  children: [
                    Text(
                      'PROTOTYPE RISK SCORE: ${prob.toStringAsFixed(1)} / 100 [$riskLevel]',
                      style: TextStyle(color: riskColor, fontSize: 12, fontWeight: FontWeight.w900),
                    ),
                    Text(
                      '(${pred['horizonWindow'] ?? "Next 2 Hours"})',
                      style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: MobileTheme.border),
            ),
            child: const Text(
              'Notice: This prototype score is generated from engineered physiological features and is not a clinically calibrated probability.',
              style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontStyle: FontStyle.italic),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            pred['clinicalExplanation']?.toString() ?? '',
            style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 13, height: 1.4),
          ),
          _build2HourTrajectoryProjectionSection(pred, availableWidth),
          const SizedBox(height: 10),
          const Divider(color: MobileTheme.border),
          const SizedBox(height: 10),

          // Contributing Factors Breakdown
          const Text(
            'FEATURE CONTRIBUTIONS & EXPLAINABILITY:',
            style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Column(
            children: factors.map((f) {
              final m = f as Map<String, dynamic>;
              final isRisk = m['direction'] == 'RISK_INCREASING';
              final factorName = m['factorName']?.toString() ?? m['factor']?.toString() ?? '';
              final desc = m['description']?.toString() ?? m['detail']?.toString() ?? '';
              final pts = m['weight'] != null ? '${m['weight']} pts' : (m['contributionScore'] != null ? '${((m['contributionScore'] as num) * 100).toInt()}%' : 'High');
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 5),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Padding(
                      padding: const EdgeInsets.only(top: 2),
                      child: Icon(
                        isRisk ? Icons.arrow_upward_rounded : Icons.check_circle_outline,
                        color: isRisk ? MobileTheme.critical : MobileTheme.stable,
                        size: 16,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  factorName,
                                  style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12, fontWeight: FontWeight.bold),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(
                                pts,
                                style: TextStyle(
                                  color: isRisk ? MobileTheme.critical : MobileTheme.stable,
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                          if (desc.isNotEmpty) ...[
                            const SizedBox(height: 2),
                            Text(
                              desc,
                              style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),

          if (actions.isNotEmpty) ...[
            const SizedBox(height: 12),
            const Divider(color: MobileTheme.border),
            const SizedBox(height: 8),
            const Text(
              'RECOMMENDED CLINICAL ACTIONS:',
              style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 6),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: actions.map((a) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('• ', style: TextStyle(color: MobileTheme.primary, fontWeight: FontWeight.bold)),
                    Expanded(
                      child: Text(a.toString(), style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12)),
                    ),
                  ],
                ),
              )).toList(),
            ),
          ],
        ],
      ),
    );
  }

  Widget _build2HourTrajectoryProjectionSection(Map<String, dynamic> pred, [double availableWidth = 800]) {
    final curGlucose = (pred['currentGlucose'] as num? ??
        pred['currentGlucoseMgDl'] as num? ??
        pred['trajectoryProjection']?['currentGlucoseMgDl'] as num? ??
        _latestTelemetry?['cgmGlucoseMgDl'] as num? ??
        _latestTelemetry?['glucose'] as num? ??
        162.0).toDouble();

    final velocity = (pred['glucoseVelocity'] as num? ??
        pred['glucoseVelocityMgDlPerMin'] as num? ??
        pred['trajectoryProjection']?['glucoseVelocityMgDlPerMin'] as num? ??
        _latestTelemetry?['glucoseVelocity'] as num? ??
        _latestTelemetry?['glucoseVelocityMgDlPerMin'] as num? ??
        2.40).toDouble();

    final proj120 = (pred['projectedGlucose120Min'] as num? ??
        pred['trajectoryProjection']?['projectedGlucose120Min'] as num? ??
        (_twinState?['projectedGlucose120Min'] as num?) ??
        332.5).toDouble();

    final delta = (pred['projectedDelta'] as num? ??
        pred['projectedDelta120Min'] as num? ??
        pred['trajectoryProjection']?['projectedDelta120Min'] as num? ??
        (proj120 - curGlucose)).toDouble();

    final direction = pred['trajectoryDirection']?.toString() ??
        pred['trajectoryProjection']?['trajectoryDirection']?.toString() ??
        (_twinState?['trajectoryDirection']?.toString() ?? 'RISING');

    final points = pred['trajectoryPoints'] as List<dynamic>? ??
        pred['projectedTrajectoryPoints'] as List<dynamic>? ??
        pred['trajectoryProjection']?['projectedTrajectoryPoints'] as List<dynamic>? ??
        [];

    Color dirColor = MobileTheme.primary; // STABLE blue
    IconData dirIcon = Icons.trending_flat_rounded;
    if (direction == 'RISING') {
      dirColor = const Color(0xFFEA580C); // Amber/orange
      dirIcon = Icons.trending_up_rounded;
    } else if (direction == 'FALLING') {
      dirColor = MobileTheme.stable; // Green
      dirIcon = Icons.trending_down_rounded;
    }

    return Container(
      width: double.infinity,
      margin: const EdgeInsets.symmetric(vertical: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: dirColor.withOpacity(0.4), width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 10,
            runSpacing: 8,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.auto_graph_rounded, color: dirColor, size: 18),
                  const SizedBox(width: 8),
                  const Flexible(
                    child: Text(
                      'PROTOTYPE 2-HOUR GLUCOSE TRAJECTORY PROJECTION',
                      style: TextStyle(
                        color: MobileTheme.textPrimary,
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                        letterSpacing: 0.5,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: dirColor.withOpacity(0.12),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: dirColor),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(dirIcon, color: dirColor, size: 15),
                    const SizedBox(width: 4),
                    Flexible(
                      child: Text(
                        '$direction (${velocity >= 0 ? '+' : ''}${velocity.toStringAsFixed(2)} mg/dL/min)',
                        style: TextStyle(color: dirColor, fontSize: 10, fontWeight: FontWeight.bold),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // 4 Metric Indicators
          () {
            final boxWidth = availableWidth < 500
                ? (availableWidth - 50) / 2
                : 160.0;
            return Wrap(
              spacing: 12,
              runSpacing: 10,
              children: [
                _buildTrajectoryMetricBox(
                  'Current Glucose',
                  '${curGlucose.toStringAsFixed(1)} mg/dL',
                  'Live Telemetry / CGM',
                  MobileTheme.primary,
                  width: boxWidth,
                ),
                _buildTrajectoryMetricBox(
                  'Rate of Change (v₀)',
                  '${velocity >= 0 ? '+' : ''}${velocity.toStringAsFixed(2)} mg/dL/min',
                  'Dynamic 5m Slope',
                  dirColor,
                  width: boxWidth,
                ),
                _buildTrajectoryMetricBox(
                  'Projected @ +120 Min',
                  '${proj120.toStringAsFixed(1)} mg/dL',
                  'Damped Velocity Model',
                  dirColor,
                  width: boxWidth,
                ),
                _buildTrajectoryMetricBox(
                  'Projected Delta',
                  '${delta >= 0 ? '+' : ''}${delta.toStringAsFixed(1)} mg/dL',
                  'Net Expected Shift',
                  delta.abs() > 20 ? (delta > 0 ? MobileTheme.critical : MobileTheme.stable) : MobileTheme.textSecondary,
                  width: boxWidth,
                ),
              ],
            );
          }(),

          // Discrete Trajectory Milestones
          if (points.isNotEmpty) ...[
            const SizedBox(height: 14),
            const Text(
              '120-MINUTE TRAJECTORY MILESTONES (30-MIN SAMPLING):',
              style: TextStyle(color: MobileTheme.textSecondary, fontSize: 10, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: points.map((p) {
                  final pt = p as Map<String, dynamic>;
                  final offset = pt['minuteOffset'] ?? pt['timeMinutes'] ?? 0;
                  final gVal = (pt['projectedGlucose'] as num? ?? pt['glucoseMgDl'] as num? ?? curGlucose).toDouble();
                  final trend = pt['trendDirection']?.toString() ?? direction;
                  return Container(
                    margin: const EdgeInsets.only(right: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: MobileTheme.border),
                    ),
                    child: Column(
                      children: [
                        Text(
                          '+$offset min',
                          style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          gVal.toStringAsFixed(1),
                          style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.w900),
                        ),
                        const SizedBox(height: 1),
                        Text(
                          trend,
                          style: TextStyle(
                            color: trend == 'RISING' ? const Color(0xFFEA580C) : (trend == 'FALLING' ? MobileTheme.stable : MobileTheme.primary),
                            fontSize: 9,
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ),
            ),
          ],

          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFFF1F5F9),
              borderRadius: BorderRadius.circular(6),
            ),
            child: const Row(
              children: [
                Icon(Icons.info_outline, color: MobileTheme.textSubtle, size: 13),
                SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'PROTOTYPE DISCLAIMER: Projected values are algorithmic estimations based on current glucose velocity, decay kinetics, and static EHR profiles (T2D clearance factors). Not for clinical diagnostic or treatment decisions.',
                    style: TextStyle(color: MobileTheme.textSecondary, fontSize: 10, fontStyle: FontStyle.italic),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTrajectoryMetricBox(String label, String value, String subtitle, Color accentColor, {double? width}) {
    return Container(
      width: width ?? 160,
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: MobileTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 10, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(value, style: TextStyle(color: accentColor, fontSize: 15, fontWeight: FontWeight.bold)),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(color: MobileTheme.textSubtle, fontSize: 9)),
        ],
      ),
    );
  }

  // --- 3b. AI Clinical Explanation Layer (Google Gemini Grounded Intelligence) ---
  Widget _buildAiClinicalExplanationSection() {
    final exp = _aiExplanation;
    if (_aiExplanationError != null && exp == null) {
      return Container(
        width: double.infinity,
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: Colors.red.shade50,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: Colors.red.shade200),
        ),
        child: Text(
          _aiExplanationError!,
          style: TextStyle(color: Colors.red.shade800, fontSize: 13),
        ),
      );
    }
    final source = exp?['source']?.toString() ?? 'FALLBACK';
    final model = exp?['model']?.toString() ?? 'deterministic-offline-engine';
    final explanationText = exp?['explanation']?.toString();
    final trajText = exp?['trajectoryExplanation']?.toString();
    final factors = (exp?['keyFactors'] as List<dynamic>?) ?? [];
    final isGemini = source == 'GEMINI';

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: isGemini ? MobileTheme.aiBorder : MobileTheme.primaryBorder,
          width: 1.5,
        ),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 12,
            runSpacing: 10,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: isGemini ? MobileTheme.aiBg : MobileTheme.primaryBg,
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      isGemini ? Icons.auto_awesome_rounded : Icons.offline_bolt_rounded,
                      color: isGemini ? MobileTheme.geminiPurple : MobileTheme.primary,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Flexible(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'AI CLINICAL EXPLANATION LAYER',
                          style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                        ),
                        Text(
                          isGemini ? 'Grounded Gemini AI Synthesis' : 'Deterministic Rule Explanation (Offline Fallback)',
                          style: TextStyle(
                            color: isGemini ? MobileTheme.geminiPurple : MobileTheme.textPrimary,
                            fontSize: 13,
                            fontWeight: FontWeight.bold,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    decoration: BoxDecoration(
                      color: isGemini ? MobileTheme.aiBg : MobileTheme.primaryBg,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: isGemini ? MobileTheme.aiBorder : MobileTheme.primaryBorder),
                    ),
                    child: Text(
                      'SOURCE: $source ($model)',
                      style: TextStyle(
                        color: isGemini ? MobileTheme.geminiPurple : MobileTheme.primary,
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  ElevatedButton.icon(
                    onPressed: _isAiExplanationLoading ? null : () => _loadAiExplanation(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFFF1F5F9),
                      foregroundColor: isGemini ? MobileTheme.geminiPurple : MobileTheme.primary,
                      elevation: 0,
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      textStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                    ),
                    icon: _isAiExplanationLoading
                        ? SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: isGemini ? MobileTheme.geminiPurple : MobileTheme.primary))
                        : const Icon(Icons.refresh_rounded, size: 14),
                    label: const Text('Refresh Explanation'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
            decoration: BoxDecoration(
              color: const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: MobileTheme.border),
            ),
            child: const Text(
              'Notice: Gemini operates strictly as an explanation layer and does not calculate numerical risk scores or trajectories. Authoritative values are computed deterministically by the Digital Twin.',
              style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontStyle: FontStyle.italic),
            ),
          ),
          const SizedBox(height: 16),
          if (_isAiExplanationLoading && exp == null)
            Center(
              child: Padding(
                padding: const EdgeInsets.symmetric(vertical: 24),
                child: CircularProgressIndicator(color: isGemini ? MobileTheme.geminiPurple : MobileTheme.primary),
              ),
            )
          else ...[
            Text(
              explanationText ?? 'No explanation generated yet. Click "Refresh Explanation" to synthesize clinical rationale.',
              style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 13, height: 1.5),
            ),
            if (trajText != null && trajText.isNotEmpty) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: MobileTheme.primaryBg,
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: MobileTheme.primaryBorder),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.show_chart_rounded, color: MobileTheme.primary, size: 18),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        trajText,
                        style: const TextStyle(color: MobileTheme.primaryDark, fontSize: 12, fontWeight: FontWeight.w600, fontFamily: 'monospace'),
                      ),
                    ),
                  ],
                ),
              ),
            ],
            if (factors.isNotEmpty) ...[
              const SizedBox(height: 12),
              const Text(
                'EXPLAINED CLINICAL DRIVERS & PHYSIOLOGICAL MECHANISMS:',
                style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: factors.map((f) {
                  return Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: const Color(0xFFF8FAFC),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: MobileTheme.border),
                    ),
                    child: Text(
                      f.toString(),
                      style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11),
                    ),
                  );
                }).toList(),
              ),
            ],
          ],
        ],
      ),
    );
  }

  // --- 4. Live Health Telemetry & Time-Series ---
  Widget _buildLiveHealthAndTrendsSection([double availableWidth = 800]) {
    final twin = _twinState ?? {};
    final vitals = twin['vitalsSnapshot'] as Map<String, dynamic>? ?? {};

    // Live Telemetry stream readings (with vitals fallback)
    final displayGlucose = _latestTelemetry?['cgmGlucoseMgDl'] != null
        ? (_latestTelemetry!['cgmGlucoseMgDl'] as num).toDouble().toStringAsFixed(1)
        : _latestTelemetry?['glucose'] != null
            ? (_latestTelemetry!['glucose'] as num).toDouble().toStringAsFixed(1)
            : '${vitals['glucose'] ?? 162.0}';
    final displayRhr = _latestTelemetry?['restingHeartRateBpm'] != null
        ? (_latestTelemetry!['restingHeartRateBpm'] as num).toDouble().toStringAsFixed(1)
        : _latestTelemetry?['restingHeartRate'] != null
            ? (_latestTelemetry!['restingHeartRate'] as num).toDouble().toStringAsFixed(1)
            : '${vitals['restingHeartRate'] ?? 78.0}';
    final displayHrv = _latestTelemetry?['hrvMs'] != null
        ? (_latestTelemetry!['hrvMs'] as num).toDouble().toStringAsFixed(1)
        : _latestTelemetry?['heartRateVariability'] != null
            ? (_latestTelemetry!['heartRateVariability'] as num).toDouble().toStringAsFixed(1)
            : '${vitals['hrv'] ?? 42.0}';
    final displaySleep = _latestTelemetry?['sleepDurationHours'] != null
        ? (_latestTelemetry!['sleepDurationHours'] as num).toDouble().toStringAsFixed(1)
        : _latestTelemetry?['sleepHours'] != null
            ? (_latestTelemetry!['sleepHours'] as num).toDouble().toStringAsFixed(1)
            : '${vitals['sleepHours'] ?? 5.8}';
    final displaySpo2 = '${_latestTelemetry?['spo2Percent'] ?? _latestTelemetry?['spo2'] ?? vitals['spo2'] ?? 98.4}';
    final displaySteps = _latestTelemetry?['steps'] != null
        ? '${_latestTelemetry!['steps']}'
        : _latestTelemetry?['dailySteps'] != null
            ? '${_latestTelemetry!['dailySteps']}'
            : '${vitals['steps'] ?? 2850}';

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 12,
            runSpacing: 10,
            children: [
              const Text(
                'LIVE WEARABLE & CGM TELEMETRY',
                style: TextStyle(color: MobileTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.5),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: MobileTheme.primaryBg,
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: MobileTheme.primaryBorder),
                ),
                child: Text(
                  _latestTelemetry != null ? '5-MIN STREAM SYNC' : 'WEARABLE STREAM',
                  style: const TextStyle(color: MobileTheme.primary, fontSize: 9, fontWeight: FontWeight.bold),
                ),
              ),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    _buildMetricTabButton('glucose', 'Glucose (CGM)', Icons.water_drop_outlined),
                    _buildMetricTabButton('resting_heart_rate', 'Resting HR', Icons.favorite_border),
                    _buildMetricTabButton('hrv', 'HRV', Icons.graphic_eq),
                    _buildMetricTabButton('sleep', 'Sleep', Icons.bedtime_outlined),
                    _buildMetricTabButton('steps', 'Steps', Icons.directions_walk),
                    _buildMetricTabButton('spo2', 'SpO2', Icons.air),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Vitals Metrics Cards (Responsive grid / row)
          () {
            final isWide = availableWidth >= 900;
            final pills = [
              _buildVitalMetricPill('CGM Glucose', displayGlucose, 'mg/dL', MobileTheme.primary),
              _buildVitalMetricPill('Resting HR', displayRhr, 'bpm', const Color(0xFFF43F5E)),
              _buildVitalMetricPill('HRV', displayHrv, 'ms', MobileTheme.geminiPurple),
              _buildVitalMetricPill('Sleep', displaySleep, 'hrs', const Color(0xFF6366F1)),
              _buildVitalMetricPill('SpO2', displaySpo2, '%', MobileTheme.stable),
              _buildVitalMetricPill('Activity', displaySteps, 'steps', MobileTheme.drift),
            ];

            if (isWide) {
              return Row(
                children: pills.map((p) => Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: p))).toList(),
              );
            }

            final int cols = availableWidth < 450 ? 2 : 3;
            const double spacing = 8.0;
            final double itemWidth = (availableWidth - (cols - 1) * spacing - 44) / cols;

            return Wrap(
              spacing: spacing,
              runSpacing: spacing,
              children: pills.map((p) => SizedBox(width: itemWidth, child: p)).toList(),
            );
          }(),

          const SizedBox(height: 20),

          // Stream Chart Representation
          _buildWearableStreamChart(),
        ],
      ),
    );
  }

  Widget _buildMetricTabButton(String metricKey, String label, IconData icon) {
    final isSelected = _selectedMetric == metricKey;
    return Padding(
      padding: const EdgeInsets.only(left: 6),
      child: InkWell(
        onTap: () => _changeMetricStream(metricKey),
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: isSelected ? MobileTheme.primary : const Color(0xFFF1F5F9),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: isSelected ? MobileTheme.primary : MobileTheme.border),
          ),
          child: Row(
            children: [
              Icon(icon, size: 14, color: isSelected ? Colors.white : MobileTheme.textSecondary),
              const SizedBox(width: 6),
              Text(
                label,
                style: TextStyle(
                  color: isSelected ? Colors.white : MobileTheme.textSecondary,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildVitalMetricPill(String title, String val, String unit, Color c) {
    return Container(
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: MobileTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(title, style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.end,
            spacing: 4,
            children: [
              Text(val, style: TextStyle(color: c, fontSize: 15, fontWeight: FontWeight.bold)),
              Text(unit, style: const TextStyle(color: MobileTheme.textSubtle, fontSize: 10)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildWearableStreamChart() {
    final stream = _activeWearableStream ?? {};
    final points = stream['points'] as List<dynamic>? ?? [];
    final baselineMean = (stream['baselineMean'] as num? ?? 90.0).toDouble();

    // Rate of change indicator for glucose
    final velocityVal = _latestTelemetry?['glucoseVelocityMgDlPerMin'] as num?;
    final velocityStr = (_selectedMetric == 'glucose' && velocityVal != null)
        ? ' • Rate of Change: ${velocityVal >= 0 ? "+" : ""}${velocityVal.toDouble().toStringAsFixed(2)} mg/dL/min'
        : '';

    if (points.isEmpty) {
      return Container(
        height: 180,
        alignment: Alignment.center,
        child: const Text('No time-series data points available for this range.', style: TextStyle(color: MobileTheme.textSecondary)),
      );
    }

    return Container(
      height: 180,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: MobileTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  '7-DAY STREAM FOR ${_selectedMetric.toUpperCase()} (POINTS: ${points.length})$velocityStr',
                  style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Row(
                children: [
                  Container(width: 12, height: 2, color: const Color(0xFFD97706)),
                  const SizedBox(width: 6),
                  Text('Baseline Mean: $baselineMean ${stream['unit']}', style: const TextStyle(color: Color(0xFFD97706), fontSize: 11, fontWeight: FontWeight.w600)),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          Expanded(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: points.map((p) {
                final m = p as Map<String, dynamic>;
                final val = (m['value'] as num? ?? 0.0).toDouble();
                // Normalized height
                double hRatio = (val / (baselineMean * 1.8)).clamp(0.1, 1.0);

                final isElevated = val > baselineMean * 1.15;
                final isDepressed = val < baselineMean * 0.85;

                Color barColor = MobileTheme.primary;
                if (isElevated) barColor = MobileTheme.critical;
                else if (isDepressed) barColor = MobileTheme.geminiPurple;

                return Expanded(
                  child: Tooltip(
                    message: '${m['timestampIso']}: $val ${stream['unit']}',
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 1.5),
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          Container(
                            height: 120 * hRatio,
                            decoration: BoxDecoration(
                              color: barColor,
                              borderRadius: const BorderRadius.vertical(top: Radius.circular(3)),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  // --- 5. Baseline Comparison Grid ---
  Widget _buildBaselineComparisonSection() {
    final twin = _twinState ?? {};
    final devs = twin['baselineDeviations'] as List<dynamic>? ?? [];

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 12,
            runSpacing: 4,
            children: [
              Text(
                'PERSONALIZED BASELINE COMPARISON ENGINE',
                style: TextStyle(color: MobileTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.8),
              ),
              Text(
                'Z-score = (Current - Mean) / σ',
                style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontStyle: FontStyle.italic),
              ),
            ],
          ),
          const SizedBox(height: 14),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: DataTable(
              headingRowColor: MaterialStateProperty.all(const Color(0xFFF8FAFC)),
              columns: const [
                DataColumn(label: Text('METRIC', style: TextStyle(color: MobileTheme.textSecondary, fontWeight: FontWeight.bold))),
                DataColumn(label: Text('BASELINE MEAN', style: TextStyle(color: MobileTheme.textSecondary, fontWeight: FontWeight.bold))),
                DataColumn(label: Text('CURRENT VALUE', style: TextStyle(color: MobileTheme.textSecondary, fontWeight: FontWeight.bold))),
                DataColumn(label: Text('DEVIATION (Δ)', style: TextStyle(color: MobileTheme.textSecondary, fontWeight: FontWeight.bold))),
                DataColumn(label: Text('Z-SCORE', style: TextStyle(color: MobileTheme.textSecondary, fontWeight: FontWeight.bold))),
                DataColumn(label: Text('TREND', style: TextStyle(color: MobileTheme.textSecondary, fontWeight: FontWeight.bold))),
              ],
              rows: devs.map((d) {
                final m = d as Map<String, dynamic>;
                final zScore = m['zScore'] != null ? (m['zScore'] as num).toDouble() : null;
                final devPct = (m['percentageDeviation'] as num? ?? 0.0).toDouble();

                Color devColor = MobileTheme.stable;
                if (zScore != null && zScore.abs() >= 2.0) devColor = MobileTheme.critical;
                else if (zScore != null && zScore.abs() >= 1.0) devColor = MobileTheme.drift;

                return DataRow(
                  cells: [
                    DataCell(Text(m['metric']?.toString().toUpperCase() ?? '', style: const TextStyle(color: MobileTheme.textPrimary, fontWeight: FontWeight.w600))),
                    DataCell(Text('${m['baselineMean']} ${m['unit']}', style: const TextStyle(color: MobileTheme.textSecondary))),
                    DataCell(Text('${m['currentValue']} ${m['unit']}', style: const TextStyle(color: MobileTheme.textPrimary, fontWeight: FontWeight.bold))),
                    DataCell(Text(
                      '${devPct >= 0 ? "+" : ""}${devPct.toStringAsFixed(1)}%',
                      style: TextStyle(color: devColor, fontWeight: FontWeight.bold),
                    )),
                    DataCell(Text(
                      zScore != null ? '${zScore >= 0 ? "+" : ""}${zScore.toStringAsFixed(2)}σ' : 'N/A',
                      style: TextStyle(color: devColor, fontWeight: FontWeight.bold),
                    )),
                    DataCell(Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(color: devColor.withOpacity(0.12), borderRadius: BorderRadius.circular(4)),
                      child: Text(m['trendDirection']?.toString() ?? 'STABLE', style: TextStyle(color: devColor, fontSize: 11, fontWeight: FontWeight.bold)),
                    )),
                  ],
                );
              }).toList(),
            ),
          ),
        ],
      ),
    );
  }

  // --- 6. Historical Records Card ---
  Widget _buildHistoricalRecordsCard() {
    final detail = _patientDetail ?? {};
    final records = detail['historicalRecords'] as List<dynamic>? ?? [];
    final labs = detail['recentBiomarkers'] as List<dynamic>? ?? [];
    final isSynthea = (detail['fullName']?.toString() ?? '').contains('Synthea');

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 6,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.history_edu_rounded, color: MobileTheme.primary, size: 18),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      'SYNTHETIC EHR / HISTORICAL DATA',
                      style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12, fontWeight: FontWeight.bold),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: isSynthea ? MobileTheme.primaryBg : const Color(0xFFF1F5F9),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: isSynthea ? MobileTheme.primaryBorder : MobileTheme.border, width: 0.8),
                ),
                child: Text(
                  isSynthea ? 'SYNTHEA FHIR EHR' : 'STATIC EHR STREAM',
                  style: TextStyle(color: isSynthea ? MobileTheme.primary : MobileTheme.textSecondary, fontSize: 10, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          const Text(
            'Static stream: Longitudinal EHR, confirmed diagnoses, historical lab panels, and medications.',
            style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11),
          ),
          const SizedBox(height: 14),

          // Historical Labs summary chips
          if (labs.isNotEmpty) ...[
            const Text('HISTORICAL LABORATORY PANELS:', style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: labs.take(6).map((l) {
                final m = l as Map<String, dynamic>;
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8FAFC),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: MobileTheme.border),
                  ),
                  child: Text.rich(
                    TextSpan(
                      style: const TextStyle(fontSize: 11),
                      children: [
                        TextSpan(text: '${m['biomarker']}: ', style: const TextStyle(color: MobileTheme.textSecondary)),
                        TextSpan(text: '${m['value']} ${m['unit'] ?? ""}', style: const TextStyle(color: MobileTheme.primary, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 14),
          ],

          const Text('DOCUMENTED CONDITIONS & DIAGNOSES:', style: TextStyle(color: MobileTheme.textSecondary, fontSize: 11, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (records.isEmpty)
            const Text('No previous medical records registered.', style: TextStyle(color: MobileTheme.textSecondary))
          else
            Column(
              children: records.map((r) {
                final m = r as Map<String, dynamic>;
                return Container(
                  margin: const EdgeInsets.only(bottom: 10),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8FAFC),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: MobileTheme.border),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Expanded(
                            child: Text(
                              m['conditionOrDiagnosis']?.toString() ?? '',
                              style: const TextStyle(color: MobileTheme.textPrimary, fontWeight: FontWeight.bold, fontSize: 13),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(color: MobileTheme.primaryBg, borderRadius: BorderRadius.circular(4)),
                            child: Text(m['status']?.toString() ?? 'ACTIVE', style: const TextStyle(color: MobileTheme.primary, fontSize: 10, fontWeight: FontWeight.bold)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text('Diagnosed: ${m['diagnosedDate'] ?? "Historical"} • Severity: ${m['severity'] ?? "Standard"} • ICD-10: ${m['icd10Code'] ?? "Uncoded"}',
                          style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 11)),
                      if (m['medications'] != null && m['medications'].toString().isNotEmpty) ...[
                        const SizedBox(height: 4),
                        Text('Rx: ${m['medications']}', style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11), maxLines: 2, overflow: TextOverflow.ellipsis),
                      ],
                    ],
                  ),
                );
              }).toList(),
            ),
        ],
      ),
    );
  }

  // --- 6. Event Timeline Card ---
  Widget _buildTimelineCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.timeline_rounded, color: MobileTheme.primary, size: 18),
              SizedBox(width: 8),
              Flexible(
                child: Text(
                  'UNIFIED EVENT TIMELINE',
                  style: TextStyle(color: MobileTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          if (_timelineEvents.isEmpty)
            const Text('No timeline events recorded.', style: TextStyle(color: MobileTheme.textSecondary))
          else
            SizedBox(
              height: 280,
              child: ListView.builder(
                itemCount: _timelineEvents.length,
                itemBuilder: (context, idx) {
                  final e = _timelineEvents[idx] as Map<String, dynamic>;
                  return Padding(
                    padding: const EdgeInsets.only(bottom: 10),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(Icons.circle, color: MobileTheme.primary, size: 8),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(e['title']?.toString() ?? '', style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12, fontWeight: FontWeight.w600)),
                              Text(e['formattedTime']?.toString() ?? '', style: const TextStyle(color: MobileTheme.textSecondary, fontSize: 10)),
                              if (e['subtitle'] != null)
                                Text(e['subtitle'].toString(), style: const TextStyle(color: MobileTheme.textSubtle, fontSize: 11)),
                            ],
                          ),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    );
  }

  // --- 7. Virtual Patient Interaction Interface ---
  Widget _buildVirtualPatientInteractionCard() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: MobileTheme.border),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: MobileTheme.primaryBg,
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.smart_toy_outlined, color: MobileTheme.primary, size: 20),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'VIRTUAL PATIENT INTERACTION ENGINE',
                      style: TextStyle(color: MobileTheme.primary, fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 1.0),
                    ),
                    Text(
                      'Ask the Digital Twin — Strictly Grounded in Telemetry & Baselines',
                      style: TextStyle(color: MobileTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // Preset Questions
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _buildPresetChip('Why is this patient currently elevated risk?'),
              _buildPresetChip('What changed from baseline?'),
              _buildPresetChip('What is driving the predicted glucose spike?'),
              _buildPresetChip('What happened during the last 7 days?'),
              _buildPresetChip('Show me the patient\'s current Digital Twin state.'),
            ],
          ),
          const SizedBox(height: 14),

          // Conversation Scroll Area
          if (_interactionHistory.isNotEmpty)
            Container(
              height: 220,
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFF8FAFC),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: MobileTheme.border),
              ),
              child: ListView.builder(
                itemCount: _interactionHistory.length,
                itemBuilder: (context, idx) {
                  final item = _interactionHistory[idx];
                  final isDoctor = item['sender'] == 'doctor';
                  return Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: isDoctor ? Colors.white : MobileTheme.primaryBg,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: isDoctor ? MobileTheme.border : MobileTheme.primaryBorder),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          isDoctor ? 'Doctor Q:' : 'Virtual Twin Grounded Answer:',
                          style: TextStyle(
                            color: isDoctor ? MobileTheme.textSecondary : MobileTheme.primary,
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          item['text'] ?? '',
                          style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 12, height: 1.3),
                        ),
                      ],
                    ),
                  );
                },
              ),
            ),

          const SizedBox(height: 12),

          // Input Box
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _questionController,
                  style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'Ask the Virtual Patient Digital Twin...',
                    hintStyle: const TextStyle(color: MobileTheme.textSubtle, fontSize: 13),
                    filled: true,
                    fillColor: Colors.white,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: MobileTheme.border)),
                    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: MobileTheme.border)),
                    focusedBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: MobileTheme.primary)),
                  ),
                  onSubmitted: (_) => _askQuestion(),
                ),
              ),
              const SizedBox(width: 10),
              ElevatedButton.icon(
                onPressed: _isAskingQuestion ? null : () => _askQuestion(),
                style: ElevatedButton.styleFrom(
                  backgroundColor: MobileTheme.primary,
                  foregroundColor: Colors.white,
                  minimumSize: const Size(80, 44),
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                ),
                icon: _isAskingQuestion
                    ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                    : const Icon(Icons.send_rounded, size: 16),
                label: const Text('Ask Twin'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPresetChip(String text) {
    return ActionChip(
      label: Text(text, style: const TextStyle(color: MobileTheme.textPrimary, fontSize: 11)),
      backgroundColor: const Color(0xFFF1F5F9),
      side: const BorderSide(color: MobileTheme.border),
      onPressed: () => _askQuestion(text),
    );
  }

  Color _getStateColor(String state) {
    switch (state.toUpperCase()) {
      case 'STABLE':
        return MobileTheme.stable;
      case 'PRE_SYMPTOMATIC_DRIFT':
        return MobileTheme.drift;
      case 'ELEVATED_RISK':
        return const Color(0xFFEA580C);
      case 'ACTIVE_ANOMALY':
      case 'CRITICAL':
        return MobileTheme.critical;
      default:
        return MobileTheme.primary;
    }
  }
}
