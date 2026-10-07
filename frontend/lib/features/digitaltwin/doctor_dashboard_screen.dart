import 'dart:async';
import 'package:flutter/material.dart';
import '../../core/widgets/os4all_brand_logo.dart';
import './digital_twin_api_service.dart';
import '../healthconnect/health_connect_model.dart';
import '../healthconnect/health_connect_service.dart';

class DoctorDashboardScreen extends StatefulWidget {
  const DoctorDashboardScreen({super.key});

  @override
  State<DoctorDashboardScreen> createState() => _DoctorDashboardScreenState();
}

class _DoctorDashboardScreenState extends State<DoctorDashboardScreen> {
  final DigitalTwinApiService _api = DigitalTwinApiService();

  List<dynamic> _patients = [];
  Map<String, dynamic>? _selectedPatient;
  Map<String, dynamic>? _patientDetail;
  Map<String, dynamic>? _twinState;
  Map<String, dynamic>? _prediction;
  Map<String, dynamic>? _activeWearableStream;
  List<dynamic> _timelineEvents = [];

  // Live Telemetry Stream State
  Map<String, dynamic>? _latestTelemetry;
  Map<String, dynamic>? _simulationStatus;
  List<dynamic> _telemetryHistory = [];
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
  bool _isLoading = true;
  String _activeScenario = 'STABLE_PATIENT';
  String? _previousState;

  // Interaction Area state
  final TextEditingController _questionController = TextEditingController();
  final List<Map<String, String>> _interactionHistory = [];
  bool _isAskingQuestion = false;

  @override
  void initState() {
    super.initState();
    _loadInitialData();
    _checkHealthConnectStatus();
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
    setState(() => _isLoading = true);
    final patients = await _api.getPatients();
    if (mounted) {
      setState(() {
        _patients = patients;
        if (_patients.isNotEmpty) {
          _selectedPatient = _patients.first as Map<String, dynamic>;
        }
      });
      if (_selectedPatient != null) {
        await _loadPatientData(_selectedPatient!['id'].toString());
      } else {
        setState(() => _isLoading = false);
      }
    }
  }

  Future<void> _loadPatientData(String patientId) async {
    setState(() => _isLoading = true);
    try {
      final results = await Future.wait([
        _api.getPatientDetail(patientId),
        _api.getDigitalTwinState(patientId),
        _api.getPrediction(patientId),
        _api.getWearableStream(patientId, _selectedMetric, days: 7),
        _api.getTimeline(patientId, days: 30),
        _api.getLatestTelemetry(patientId),
        _api.getTelemetrySimulationStatus(patientId),
        _api.getTelemetryHistory(patientId, limit: 20),
      ]);

      final detail = results[0] as Map<String, dynamic>?;
      final twin = results[1] as Map<String, dynamic>?;
      final pred = results[2] as Map<String, dynamic>?;
      final stream = results[3] as Map<String, dynamic>?;
      final timeline = results[4] as List<dynamic>? ?? [];
      final latestTelemetry = results[5] as Map<String, dynamic>?;
      final simStatus = results[6] as Map<String, dynamic>?;
      final history = results[7] as List<dynamic>? ?? [];

      if (mounted) {
        setState(() {
          if (_twinState != null && _twinState!['state'] != null) {
            _previousState = _twinState!['state'].toString();
          }
          _patientDetail = detail;
          _twinState = twin;
          _prediction = pred;
          _activeWearableStream = stream;
          _timelineEvents = timeline;
          _latestTelemetry = latestTelemetry;
          _simulationStatus = simStatus;
          _telemetryHistory = history;
          if (twin != null && twin['activeScenario'] != null) {
            _activeScenario = twin['activeScenario'].toString();
          }
          _isLiveStreaming = simStatus != null && simStatus['running'] == true;
          _isLoading = false;
        });
        _loadAiExplanation();
      }
    } catch (e) {
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

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0F172A), // Clinical Dark Navy
      appBar: AppBar(
        backgroundColor: const Color(0xFF1E293B),
        elevation: 3,
        titleSpacing: 16,
        title: const Os4AllBrandLogo(fontSize: 18),
        actions: const [
          Padding(
            padding: EdgeInsets.only(right: 16),
            child: Icon(Icons.shield_outlined, color: Color(0xFF38BDF8), size: 20),
          ),
        ],
      ),
      body: _isLoading && _selectedPatient == null
          ? const Center(child: CircularProgressIndicator(color: Color(0xFF38BDF8)))
          : LayoutBuilder(
              builder: (context, constraints) {
                final isDesktop = constraints.maxWidth >= 900;
                final contentPadding = isDesktop ? const EdgeInsets.all(20) : const EdgeInsets.symmetric(horizontal: 14, vertical: 16);

                final dashboardPanels = [
                  // Top Clinical Twin Header Row: Workstation badge + Prototype notice
                  Wrap(
                    crossAxisAlignment: WrapCrossAlignment.center,
                    spacing: 12,
                    runSpacing: 8,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(
                          color: const Color(0xFF0284C7).withOpacity(0.2),
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: const Color(0xFF38BDF8), width: 1),
                        ),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.monitor_heart_rounded, color: Color(0xFF38BDF8), size: 14),
                            SizedBox(width: 6),
                            Flexible(
                              child: Text(
                                'CLINICAL TWIN WORKSTATION',
                                style: TextStyle(color: Color(0xFF38BDF8), fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                        decoration: BoxDecoration(
                          color: Colors.amber.withOpacity(0.15),
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: Colors.amber.withOpacity(0.5)),
                        ),
                        child: const Text(
                          'HACKATHON PROTOTYPE — NOT A MEDICAL DIAGNOSIS',
                          style: TextStyle(color: Colors.amber, fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 14),

                  // On mobile/tablet, render responsive patient selector header
                  if (!isDesktop) ...[
                    _buildMobilePatientSelector(),
                    const SizedBox(height: 16),
                  ],

                  // 1. Patient Header & Current Twin State Hero
                  _buildPatientHeroCard(),
                  const SizedBox(height: 20),

                  // 1b. Dual-Stream Data Fusion Architecture Visualization
                  _buildDataFusionCard(),
                  const SizedBox(height: 20),

                  // Health Data Sources (Synthea + Synthetic Telemetry + Android Health Connect)
                  _buildHealthDataSourcesSection(),
                  const SizedBox(height: 20),

                  // 2. Interactive Simulation Control Toolbar
                  _buildSimulationToolbar(),
                  const SizedBox(height: 20),

                  // 3. Primary Prediction: Early Glucose Spike Prediction Layer
                  _buildPrimaryPredictionCard(),
                  const SizedBox(height: 20),

                  // 3b. AI Clinical Explanation Layer (Gemini Grounded Intelligence)
                  _buildAiClinicalExplanationSection(),
                  const SizedBox(height: 20),

                  // 4. Live Wearable Telemetry & Dynamic Charts
                  _buildLiveHealthAndTrendsSection(),
                  const SizedBox(height: 20),

                  // 5. Baseline Comparison Grid (Personal Mean vs Current vs Z-score)
                  _buildBaselineComparisonSection(),
                  const SizedBox(height: 20),

                  // 6. Two-Column Layout on Desktop, Single-Column on Mobile
                  if (isDesktop)
                    Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Expanded(child: _buildHistoricalRecordsCard()),
                        const SizedBox(width: 20),
                        Expanded(child: _buildTimelineCard()),
                      ],
                    )
                  else
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        _buildHistoricalRecordsCard(),
                        const SizedBox(height: 20),
                        _buildTimelineCard(),
                      ],
                    ),
                  const SizedBox(height: 20),

                  // 7. Virtual Patient Interaction Interface (Doctor Q&A grounded in Twin)
                  _buildVirtualPatientInteractionCard(),
                  const SizedBox(height: 40),
                ];

                if (isDesktop) {
                  return Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      // LEFT SIDEBAR: Patient Cohort Selector
                      _buildPatientSelectorSidebar(),

                      // MAIN CONTENT: Doctor Multi-Panel Dashboard
                      Expanded(
                        child: SingleChildScrollView(
                          padding: contentPadding,
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
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
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: dashboardPanels,
                  ),
                );
              },
            ),
    );
  }

  // --- Mobile Patient Selector ---
  Widget _buildMobilePatientSelector() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF334155)),
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
                  Icon(Icons.groups_rounded, color: Color(0xFF38BDF8), size: 18),
                  SizedBox(width: 8),
                  Text(
                    'VIRTUAL PATIENTS',
                    style: TextStyle(color: Color(0xFF94A3B8), fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.0),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(color: const Color(0xFF334155), borderRadius: BorderRadius.circular(10)),
                child: Text('${_patients.length} loaded', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
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
                      setState(() => _selectedPatient = p as Map<String, dynamic>);
                      _loadPatientData(p['id'].toString());
                    },
                    borderRadius: BorderRadius.circular(8),
                    child: Container(
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      decoration: BoxDecoration(
                        color: isSelected ? const Color(0xFF0F172A) : const Color(0xFF1E293B),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(
                          color: isSelected ? const Color(0xFF38BDF8) : const Color(0xFF334155),
                          width: isSelected ? 1.5 : 1.0,
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          CircleAvatar(
                            radius: 12,
                            backgroundColor: isSelected ? const Color(0xFF0284C7) : const Color(0xFF334155),
                            child: Text(
                              (p['fullName']?.toString() ?? 'P').substring(0, 1),
                              style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
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
                                  color: isSelected ? Colors.white : const Color(0xFFCBD5E1),
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
        color: Color(0xFF1E293B),
        border: Border(right: BorderSide(color: Color(0xFF334155), width: 1)),
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
                  style: TextStyle(color: Color(0xFF94A3B8), fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 1.0),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                  decoration: BoxDecoration(color: const Color(0xFF334155), borderRadius: BorderRadius.circular(10)),
                  child: Text('${_patients.length}', style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold)),
                ),
              ],
            ),
          ),
          const Divider(color: Color(0xFF334155), height: 1),
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
                    setState(() => _selectedPatient = p);
                    _loadPatientData(p['id'].toString());
                  },
                  child: Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                    decoration: BoxDecoration(
                      color: isSelected ? const Color(0xFF0F172A) : Colors.transparent,
                      border: Border(
                        left: BorderSide(
                          color: isSelected ? const Color(0xFF38BDF8) : Colors.transparent,
                          width: 4,
                        ),
                        bottom: const BorderSide(color: Color(0xFF334155), width: 0.5),
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
                                  color: isSelected ? Colors.white : const Color(0xFFCBD5E1),
                                  fontWeight: FontWeight.bold,
                                  fontSize: 14,
                                ),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                              decoration: BoxDecoration(
                                color: stateColor.withOpacity(0.15),
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
                          style: const TextStyle(color: Color(0xFF64748B), fontSize: 12),
                        ),
                        const SizedBox(height: 6),
                        Row(
                          children: [
                            const Text('Risk Score: ', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11)),
                            Text(
                              '${p['glucoseSpikeProbability']} / 100',
                              style: TextStyle(
                                color: (p['glucoseSpikeProbability'] as num? ?? 0) > 50 ? Colors.redAccent : const Color(0xFF34D399),
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
  Widget _buildPatientHeroCard() {
    final detail = _patientDetail ?? {};
    final twin = _twinState ?? {};
    final state = twin['state']?.toString() ?? 'STABLE';
    final stateColor = _getStateColor(state);
    final drivers = twin['stateDrivers'] as List<dynamic>? ?? [];

    return LayoutBuilder(
      builder: (context, constraints) {
        final isNarrow = constraints.maxWidth < 650;

        final avatarWidget = CircleAvatar(
          radius: 26,
          backgroundColor: const Color(0xFF334155),
          child: Text(
            (detail['fullName']?.toString() ?? 'P').substring(0, 1),
            style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 22, fontWeight: FontWeight.bold),
          ),
        );

        final nameAndIdWidget = Wrap(
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: 10,
          runSpacing: 4,
          children: [
            Text(
              detail['fullName']?.toString() ?? 'Patient',
              style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: const Color(0xFF334155),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                'ID: ${_selectedPatient?['id']?.toString().substring(0, 8) ?? ""}',
                style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontFamily: 'monospace'),
              ),
            ),
          ],
        );

        final demographicsWidget = Text(
          '${detail['age']} years old • ${detail['biologicalSex']} • Height: ${detail['heightCm']} cm • Weight: ${detail['weightKg']} kg • BMI: ${detail['bmi']} • Blood Type: ${detail['bloodType']}',
          style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 12),
        );

        final lifestyleWidget = (detail['lifestyleNotes'] != null && detail['lifestyleNotes'].toString().isNotEmpty)
            ? Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  detail['lifestyleNotes'].toString(),
                  style: const TextStyle(color: Color(0xFF64748B), fontSize: 11, fontStyle: FontStyle.italic),
                ),
              )
            : const SizedBox.shrink();

        final statePillWidget = Container(
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
          decoration: BoxDecoration(
            color: stateColor.withOpacity(0.12),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: stateColor, width: 1.5),
          ),
          child: Column(
            crossAxisAlignment: isNarrow ? CrossAxisAlignment.start : CrossAxisAlignment.end,
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text('DIGITAL TWIN STATE', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 10, fontWeight: FontWeight.bold)),
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
                'Risk Score: ${twin['overallRiskScore'] ?? 0} / 100',
                style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
              ),
            ],
          ),
        );

        return Container(
          padding: EdgeInsets.all(isNarrow ? 16 : 22),
          decoration: BoxDecoration(
            color: const Color(0xFF1E293B),
            borderRadius: BorderRadius.circular(12),
            border: Border.all(color: const Color(0xFF334155)),
            boxShadow: [
              BoxShadow(color: Colors.black.withOpacity(0.25), blurRadius: 10, offset: const Offset(0, 4)),
            ],
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
              const Divider(color: Color(0xFF334155)),
              const SizedBox(height: 10),

              // Active State Drivers
              Wrap(
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 8,
                runSpacing: 6,
                children: [
                  const Text(
                    'ACTIVE STATE DRIVERS: ',
                    style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold),
                  ),
                  ...drivers.map((d) => Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                    decoration: BoxDecoration(
                      color: const Color(0xFF0F172A),
                      borderRadius: BorderRadius.circular(4),
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
                    color: const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: const Color(0xFF38BDF8).withOpacity(0.4)),
                  ),
                  child: Wrap(
                    crossAxisAlignment: WrapCrossAlignment.center,
                    spacing: 8,
                    runSpacing: 4,
                    children: [
                      const Icon(Icons.compare_arrows_rounded, color: Color(0xFF38BDF8), size: 18),
                      Text(
                        'STATE TRANSITION: ${_previousState!.replaceAll('_', ' ')} → ${state.replaceAll('_', ' ')}',
                        style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 12, fontWeight: FontWeight.bold),
                      ),
                      Text(
                        '• Triggered by: ${drivers.isNotEmpty ? drivers.first : "Telemetry shift"}',
                        style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 11, fontStyle: FontStyle.italic),
                      ),
                    ],
                  ),
                ),
              ],
            ],
          ),
        );
      },
    );
  }

  // --- 1b. Dual-Stream Data Fusion Architecture Card ---
  Widget _buildDataFusionCard() {
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
        : (vitals['glucose'] as num? ?? 95.0).toDouble();
    final velocityVal = _latestTelemetry?['glucoseVelocityMgDlPerMin'] != null
        ? (_latestTelemetry!['glucoseVelocityMgDlPerMin'] as num).toDouble()
        : null;
    final hrvVal = _latestTelemetry?['hrvMs'] != null
        ? (_latestTelemetry!['hrvMs'] as num).toDouble()
        : (vitals['hrv'] as num? ?? 54.0).toDouble();
    final rhrVal = _latestTelemetry?['restingHeartRateBpm'] != null
        ? (_latestTelemetry!['restingHeartRateBpm'] as num).toDouble()
        : (vitals['restingHeartRate'] as num? ?? 62.0).toDouble();
    final sleepHours = _latestTelemetry?['sleepDurationHours'] != null
        ? (_latestTelemetry!['sleepDurationHours'] as num).toDouble()
        : (vitals['sleepHours'] as num? ?? 7.5).toDouble();
    final sleepQuality = _latestTelemetry?['sleepQuality']?.toString() ?? 'GOOD';
    final stepsVal = _latestTelemetry?['steps'] != null
        ? (_latestTelemetry!['steps'] as num).toInt()
        : (vitals['steps'] as num? ?? 8500).toInt();
    final activityLvl = _latestTelemetry?['activityLevel']?.toString() ?? 'MODERATE';
    final packetConf = _latestTelemetry?['confidenceScore'] != null
        ? ((_latestTelemetry!['confidenceScore'] as num).toDouble() * 100).toInt()
        : 95;
    final packetTime = _latestTelemetry?['timestamp']?.toString().split('T').last.split('.').first ?? 'Live';

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF0284C7).withOpacity(0.4)),
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
                      color: const Color(0xFF0284C7).withOpacity(0.2),
                      borderRadius: BorderRadius.circular(6),
                    ),
                    child: const Icon(Icons.merge_type_rounded, color: Color(0xFF38BDF8), size: 18),
                  ),
                  const SizedBox(width: 8),
                  const Flexible(
                    child: Text(
                      'DUAL-STREAM DATA FUSION ARCHITECTURE',
                      style: TextStyle(color: Color(0xFF38BDF8), fontSize: 11.5, fontWeight: FontWeight.bold, letterSpacing: 0.5),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: const Color(0xFF0F172A),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: const Color(0xFF334155)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(
                        color: _isLiveStreaming ? const Color(0xFF34D399) : const Color(0xFF94A3B8),
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      _isLiveStreaming ? 'Continuous Telemetry Fusion (Active)' : 'Continuous Fusion Cycle',
                      style: TextStyle(color: _isLiveStreaming ? const Color(0xFF34D399) : const Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.w600),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          LayoutBuilder(
            builder: (context, constraints) {
              final isNarrow = constraints.maxWidth < 900;
              final stream1Items = [
                'Diagnosis: $primaryCond',
                'Baseline HbA1c: $baselineA1c%',
                'Fasting Glucose: $baselineFasting mg/dL',
                'Demographics: ${detail['age'] ?? 38}y ${detail['biologicalSex'] ?? 'M'} • BMI ${detail['bmi'] ?? 24}',
              ];
              final velocityStr = velocityVal != null ? ' (${velocityVal >= 0 ? "+" : ""}${velocityVal.toStringAsFixed(2)} mg/dL/min)' : '';
              final stream2Items = [
                'CGM Glucose: ${cgmVal.toStringAsFixed(1)} mg/dL$velocityStr',
                'Autonomic HRV: ${hrvVal.toStringAsFixed(1)} ms • Resting HR: ${rhrVal.toStringAsFixed(0)} bpm',
                'Sleep: ${sleepHours.toStringAsFixed(1)} hrs ($sleepQuality)',
                'Activity: $stepsVal steps • $activityLvl (Conf: $packetConf%)',
              ];

              return isNarrow
                  ? Column(
                      children: [
                        _buildFusionStreamBox(
                          title: 'STREAM 1: STATIC / HISTORICAL EHR',
                          subtitle: 'Synthea FHIR Baseline & Longitudinal Labs',
                          icon: Icons.history_edu_rounded,
                          accentColor: const Color(0xFF38BDF8),
                          items: stream1Items,
                        ),
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 8),
                          child: Icon(Icons.add_rounded, color: Color(0xFF38BDF8), size: 24),
                        ),
                        _buildFusionStreamBox(
                          title: 'STREAM 2: DYNAMIC WEARABLE TELEMETRY',
                          subtitle: 'Simulated Real-Time IoT & CGM (5-min stream)',
                          icon: Icons.sensors_rounded,
                          accentColor: const Color(0xFFA855F7),
                          items: stream2Items,
                        ),
                        const Padding(
                          padding: EdgeInsets.symmetric(vertical: 8),
                          child: Icon(Icons.arrow_downward_rounded, color: Color(0xFF38BDF8), size: 24),
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
                            accentColor: const Color(0xFF38BDF8),
                            items: stream1Items,
                          ),
                        ),
                        const Padding(
                          padding: EdgeInsets.symmetric(horizontal: 8),
                          child: Icon(Icons.add_rounded, color: Color(0xFF38BDF8), size: 24),
                        ),
                        Expanded(
                          child: _buildFusionStreamBox(
                            title: 'STREAM 2: DYNAMIC WEARABLES',
                            subtitle: 'Simulated Real-Time IoT & CGM (5-min)',
                            icon: Icons.sensors_rounded,
                            accentColor: const Color(0xFFA855F7),
                            items: stream2Items,
                          ),
                        ),
                        const Padding(
                          padding: EdgeInsets.symmetric(horizontal: 8),
                          child: Icon(Icons.arrow_forward_rounded, color: Color(0xFF38BDF8), size: 24),
                        ),
                        Expanded(
                          child: _buildFusionResultBox(state, stateColor, twin, packetTime),
                        ),
                      ],
                    );
            },
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
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: accentColor.withOpacity(0.3)),
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
          Text(subtitle, style: const TextStyle(color: Color(0xFF64748B), fontSize: 10)),
          const SizedBox(height: 8),
          ...items.map((it) => Padding(
            padding: const EdgeInsets.symmetric(vertical: 1.5),
            child: Row(
              children: [
                const Text('• ', style: TextStyle(color: Color(0xFF64748B), fontSize: 11)),
                Expanded(
                  child: Text(
                    it,
                    style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 11),
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
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(8),
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
                  style: TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
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
            style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.w600),
          ),
          const SizedBox(height: 2),
          Text(
            'Horizon: ${twin['predictionHorizon'] ?? "Next 2 Hours"}',
            style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 10),
          ),
          if (packetTime != null && packetTime.isNotEmpty) ...[
            const SizedBox(height: 2),
            Text(
              'Sync: $packetTime',
              style: const TextStyle(color: Color(0xFF64748B), fontSize: 9, fontFamily: 'monospace'),
            ),
          ],
        ],
      ),
    );
  }

  // --- 1c. Health Data Sources (Dual-Stream + Android Health Connect) ---
  Widget _buildHealthDataSourcesSection() {
    final bool isUnavailable = _hcStatus == HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    final bool isGranted = _hcIsConnected;
    final snapshot = _latestHcSnapshot;

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF334155)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: const Color(0xFF10B981).withOpacity(0.2),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Icon(Icons.cable_rounded, color: Color(0xFF10B981), size: 18),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'HEALTH DATA INGESTION SOURCES',
                      style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                    ),
                    Text(
                      'Static Synthea EHR + Simulated 5-min Dynamic Telemetry + Optional Android Health Connect (Read-Only)',
                      style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          LayoutBuilder(
            builder: (context, constraints) {
              final isNarrow = constraints.maxWidth < 900;
              final cards = [
                _buildSourceCard(
                  title: 'Synthea FHIR EHR',
                  subtitle: 'Historical baseline & clinical records',
                  statusBadge: 'ACTIVE / LOADED',
                  statusColor: const Color(0xFF38BDF8),
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
                  statusColor: _isLiveStreaming ? const Color(0xFF10B981) : const Color(0xFFA855F7),
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
                      ? const Color(0xFF10B981)
                      : (isUnavailable ? const Color(0xFF64748B) : const Color(0xFFF59E0B)),
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
                            backgroundColor: const Color(0xFF0284C7),
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
                            foregroundColor: const Color(0xFF38BDF8),
                            side: const BorderSide(color: Color(0xFF0284C7)),
                            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                            textStyle: const TextStyle(fontSize: 11),
                          ),
                          icon: _hcIsLoading
                              ? const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF38BDF8)))
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
                color: const Color(0xFF0F172A),
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: const Color(0xFF334155)),
              ),
              child: Row(
                children: [
                  Icon(
                    isGranted ? Icons.check_circle_outline_rounded : Icons.info_outline_rounded,
                    color: isGranted ? const Color(0xFF10B981) : const Color(0xFF94A3B8),
                    size: 16,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      _hcSyncStatusText!,
                      style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 11),
                    ),
                  ),
                  if (snapshot != null && snapshot.hasAnyHealthData) ...[
                    Text(
                      'Steps: ${snapshot.steps ?? "—"} • HR: ${snapshot.heartRateBpm?.toStringAsFixed(0) ?? "—"} • Glucose: ${snapshot.bloodGlucoseMgDl?.toStringAsFixed(0) ?? "—"}',
                      style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 11, fontFamily: 'monospace'),
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
              color: Colors.amber.withOpacity(0.08),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: Colors.amber.withOpacity(0.2)),
            ),
            child: const Row(
              children: [
                Icon(Icons.privacy_tip_outlined, color: Colors.amber, size: 14),
                SizedBox(width: 8),
                Expanded(
                  child: Text(
                    'Health Connect operates in strict read-only mode. Real-device metrics are mapped into the prototype Digital Twin for the active session. Not intended for clinical diagnostic use.',
                    style: TextStyle(color: Color(0xFFFDE68A), fontSize: 10),
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
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: statusColor.withOpacity(0.3)),
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
                  style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(color: Color(0xFF64748B), fontSize: 10)),
          const SizedBox(height: 8),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
            decoration: BoxDecoration(
              color: statusColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: statusColor.withOpacity(0.5)),
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
                const Text('• ', style: TextStyle(color: Color(0xFF64748B), fontSize: 11)),
                Expanded(
                  child: Text(d, style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 11)),
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
      padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFF0284C7).withOpacity(0.5)),
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
                  Icon(Icons.tune_rounded, color: Color(0xFF38BDF8), size: 18),
                  SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      'SIMULATION CONTROLLER',
                      style: TextStyle(color: Color(0xFF38BDF8), fontSize: 12, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              // Live Simulation Indicator
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: _isLiveStreaming ? const Color(0xFF10B981).withOpacity(0.15) : const Color(0xFF334155),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(
                    color: _isLiveStreaming ? const Color(0xFF10B981) : const Color(0xFF64748B),
                    width: 0.8,
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.circle,
                      size: 8,
                      color: _isLiveStreaming ? const Color(0xFF10B981) : const Color(0xFF94A3B8),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      _isLiveStreaming ? 'LIVE TELEMETRY (Tick #$ticks)' : 'STREAM PAUSED',
                      style: TextStyle(
                        color: _isLiveStreaming ? const Color(0xFF10B981) : const Color(0xFF94A3B8),
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              ),
              Text(
                'Active Scenario: ${_activeScenario.replaceAll('_', ' ')}',
                style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 12, fontWeight: FontWeight.w600),
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
                  backgroundColor: _isLiveStreaming ? const Color(0xFFDC2626) : const Color(0xFF059669),
                  foregroundColor: Colors.white,
                ),
                icon: Icon(_isLiveStreaming ? Icons.pause_circle_filled : Icons.play_circle_filled, size: 16),
                label: Text(_isLiveStreaming ? 'Pause Stream' : 'Start Live Telemetry (5s)'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('STABLE_PATIENT'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'STABLE_PATIENT' || _activeScenario == 'STABLE' ? const Color(0xFF0284C7) : const Color(0xFF334155),
                  foregroundColor: Colors.white,
                ),
                icon: const Icon(Icons.check_circle_outline, size: 16),
                label: const Text('1. Stable Homeostasis'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('POOR_SLEEP'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'POOR_SLEEP' ? const Color(0xFFEA580C) : const Color(0xFF334155),
                  foregroundColor: Colors.white,
                ),
                icon: const Icon(Icons.bedtime_outlined, size: 16),
                label: const Text('2. Poor Sleep → Drift'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('HIGH_ACTIVITY'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'HIGH_ACTIVITY' ? const Color(0xFF059669) : const Color(0xFF334155),
                  foregroundColor: Colors.white,
                ),
                icon: const Icon(Icons.directions_run, size: 16),
                label: const Text('3. High Activity (GLUT4)'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('GLUCOSE_SPIKE'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'GLUCOSE_SPIKE' || _activeScenario == 'GLUCOSE_RISE' ? const Color(0xFFDC2626) : const Color(0xFF334155),
                  foregroundColor: Colors.white,
                ),
                icon: const Icon(Icons.warning_amber_rounded, size: 16),
                label: const Text('4. Glucose Spike Risk'),
              ),
              ElevatedButton.icon(
                onPressed: () => _injectScenario('RECOVERY'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _activeScenario == 'RECOVERY' ? const Color(0xFF10B981) : const Color(0xFF334155),
                  foregroundColor: Colors.white,
                ),
                icon: const Icon(Icons.replay_rounded, size: 16),
                label: const Text('5. Recovery After Walk'),
              ),
              OutlinedButton.icon(
                onPressed: _nextTick,
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF38BDF8),
                  side: const BorderSide(color: Color(0xFF38BDF8)),
                ),
                icon: const Icon(Icons.fast_forward_rounded, size: 16),
                label: const Text('Next Reading Tick'),
              ),
              OutlinedButton.icon(
                onPressed: _resetToStable,
                style: OutlinedButton.styleFrom(
                  foregroundColor: const Color(0xFF94A3B8),
                  side: const BorderSide(color: Color(0xFF64748B)),
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
  Widget _buildPrimaryPredictionCard() {
    final pred = _prediction ?? {};
    final prob = (pred['probability'] as num? ?? 12.0).toDouble();
    final riskLevel = pred['riskLevel']?.toString() ?? 'LOW';
    final factors = pred['contributingFactors'] as List<dynamic>? ?? [];
    final actions = pred['recommendedClinicalActions'] as List<dynamic>? ?? [];

    Color riskColor = const Color(0xFF34D399);
    if (prob >= 75) riskColor = const Color(0xFFEF4444);
    else if (prob >= 50) riskColor = const Color(0xFFF97316);
    else if (prob >= 30) riskColor = const Color(0xFFFBBF24);

    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: riskColor.withOpacity(0.5), width: 1.5),
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
                      color: riskColor.withOpacity(0.15),
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
                          style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                        ),
                        Text(
                          pred['headline']?.toString() ?? 'Metabolic Spike Prediction',
                          style: const TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold),
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
                  color: riskColor.withOpacity(0.15),
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
                      style: const TextStyle(color: Colors.white70, fontSize: 11),
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
              color: const Color(0xFF0F172A),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: const Color(0xFF334155)),
            ),
            child: const Text(
              'Notice: This prototype score is generated from engineered physiological features and is not a clinically calibrated probability.',
              style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontStyle: FontStyle.italic),
            ),
          ),
          const SizedBox(height: 12),
          Text(
            pred['clinicalExplanation']?.toString() ?? '',
            style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 13, height: 1.4),
          ),
          _build2HourTrajectoryProjectionSection(pred),
          const SizedBox(height: 10),
          const Divider(color: Color(0xFF334155)),
          const SizedBox(height: 10),

          // Contributing Factors Breakdown
          const Text(
            'FEATURE CONTRIBUTIONS & EXPLAINABILITY:',
            style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Column(
            children: factors.map((f) {
              final m = f as Map<String, dynamic>;
              final isRisk = m['direction'] == 'RISK_INCREASING';
              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  children: [
                    Icon(
                      isRisk ? Icons.arrow_upward_rounded : Icons.check_circle_outline,
                      color: isRisk ? const Color(0xFFEF4444) : const Color(0xFF34D399),
                      size: 16,
                    ),
                    const SizedBox(width: 8),
                    Text(
                      m['factorName']?.toString() ?? '',
                      style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        m['description']?.toString() ?? '',
                        style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 12),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Text(
                      '${m['weight']} pts',
                      style: TextStyle(
                        color: isRisk ? const Color(0xFFEF4444) : const Color(0xFF34D399),
                        fontSize: 11,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),

          if (actions.isNotEmpty) ...[
            const SizedBox(height: 12),
            const Divider(color: Color(0xFF334155)),
            const SizedBox(height: 8),
            const Text(
              'RECOMMENDED CLINICAL ACTIONS:',
              style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 6),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: actions.map((a) => Padding(
                padding: const EdgeInsets.symmetric(vertical: 2),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('• ', style: TextStyle(color: Color(0xFF38BDF8), fontWeight: FontWeight.bold)),
                    Expanded(
                      child: Text(a.toString(), style: const TextStyle(color: Color(0xFFE2E8F0), fontSize: 12)),
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

  Widget _build2HourTrajectoryProjectionSection(Map<String, dynamic> pred) {
    final curGlucose = (pred['currentGlucose'] as num? ?? _latestTelemetry?['cgmGlucoseMgDl'] as num? ?? 95.0).toDouble();
    final velocity = (pred['glucoseVelocity'] as num? ?? _latestTelemetry?['glucoseVelocity'] as num? ?? 0.0).toDouble();
    final proj120 = (pred['projectedGlucose120Min'] as num? ?? (_twinState?['projectedGlucose120Min'] as num?) ?? curGlucose).toDouble();
    final delta = (pred['projectedDelta'] as num? ?? (proj120 - curGlucose)).toDouble();
    final direction = pred['trajectoryDirection']?.toString() ?? (_twinState?['trajectoryDirection']?.toString() ?? 'STABLE');
    final points = pred['trajectoryPoints'] as List<dynamic>? ?? [];

    Color dirColor = const Color(0xFF38BDF8); // STABLE blue
    IconData dirIcon = Icons.trending_flat_rounded;
    if (direction == 'RISING') {
      dirColor = const Color(0xFFF97316); // Amber/orange
      dirIcon = Icons.trending_up_rounded;
    } else if (direction == 'FALLING') {
      dirColor = const Color(0xFF34D399); // Green
      dirIcon = Icons.trending_down_rounded;
    }

    return Container(
      margin: const EdgeInsets.symmetric(vertical: 14),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: dirColor.withOpacity(0.6), width: 1.5),
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
                        color: Colors.white,
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
                  color: dirColor.withOpacity(0.15),
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
          LayoutBuilder(
            builder: (context, constraints) {
              final boxWidth = constraints.maxWidth < 450
                  ? (constraints.maxWidth - 12) / 2
                  : 160.0;
              return Wrap(
                spacing: 12,
                runSpacing: 10,
                children: [
                  _buildTrajectoryMetricBox(
                    'Current Glucose',
                    '${curGlucose.toStringAsFixed(1)} mg/dL',
                    'Live Telemetry / CGM',
                    const Color(0xFF38BDF8),
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
                    delta.abs() > 20 ? (delta > 0 ? const Color(0xFFEF4444) : const Color(0xFF34D399)) : const Color(0xFF94A3B8),
                    width: boxWidth,
                  ),
                ],
              );
            },
          ),

          // Discrete Trajectory Milestones
          if (points.isNotEmpty) ...[
            const SizedBox(height: 14),
            const Text(
              '120-MINUTE TRAJECTORY MILESTONES (30-MIN SAMPLING):',
              style: TextStyle(color: Color(0xFF94A3B8), fontSize: 10, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: points.map((p) {
                  final pt = p as Map<String, dynamic>;
                  final offset = pt['minuteOffset'] ?? 0;
                  final gVal = (pt['projectedGlucose'] as num? ?? curGlucose).toDouble();
                  final trend = pt['trendDirection']?.toString() ?? 'STABLE';
                  return Container(
                    margin: const EdgeInsets.only(right: 8),
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                    decoration: BoxDecoration(
                      color: const Color(0xFF1E293B),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: const Color(0xFF334155)),
                    ),
                    child: Column(
                      children: [
                        Text(
                          '+$offset min',
                          style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          gVal.toStringAsFixed(1),
                          style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.w900),
                        ),
                        const SizedBox(height: 1),
                        Text(
                          trend,
                          style: TextStyle(
                            color: trend == 'RISING' ? const Color(0xFFF97316) : (trend == 'FALLING' ? const Color(0xFF34D399) : const Color(0xFF38BDF8)),
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
              color: const Color(0xFF1E293B),
              borderRadius: BorderRadius.circular(4),
            ),
            child: const Row(
              children: [
                Icon(Icons.info_outline, color: Color(0xFF94A3B8), size: 13),
                SizedBox(width: 6),
                Expanded(
                  child: Text(
                    'PROTOTYPE DISCLAIMER: Projected values are algorithmic estimations based on current glucose velocity, decay kinetics, and static EHR profiles (T2D clearance factors). Not for clinical diagnostic or treatment decisions.',
                    style: TextStyle(color: Color(0xFF94A3B8), fontSize: 10, fontStyle: FontStyle.italic),
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
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFF334155)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 10, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Text(value, style: TextStyle(color: accentColor, fontSize: 15, fontWeight: FontWeight.bold)),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(color: Color(0xFF64748B), fontSize: 9)),
        ],
      ),
    );
  }

  // --- 3b. AI Clinical Explanation Layer (Google Gemini Grounded Intelligence) ---
  Widget _buildAiClinicalExplanationSection() {
    final exp = _aiExplanation;
    final source = exp?['source']?.toString() ?? 'FALLBACK';
    final model = exp?['model']?.toString() ?? 'deterministic-offline-engine';
    final explanationText = exp?['explanation']?.toString();
    final trajText = exp?['trajectoryExplanation']?.toString();
    final factors = (exp?['keyFactors'] as List<dynamic>?) ?? [];
    final isGemini = source == 'GEMINI';

    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: isGemini ? const Color(0xFFA855F7).withOpacity(0.5) : const Color(0xFF38BDF8).withOpacity(0.4),
          width: 1.5,
        ),
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
                      color: isGemini ? const Color(0xFFA855F7).withOpacity(0.2) : const Color(0xFF38BDF8).withOpacity(0.2),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      isGemini ? Icons.auto_awesome_rounded : Icons.offline_bolt_rounded,
                      color: isGemini ? const Color(0xFFA855F7) : const Color(0xFF38BDF8),
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
                          style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 0.8),
                        ),
                        Text(
                          isGemini ? 'Grounded Gemini AI Synthesis' : 'Deterministic Rule Explanation (Offline Fallback)',
                          style: const TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
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
                      color: (isGemini ? const Color(0xFFA855F7) : const Color(0xFF38BDF8)).withOpacity(0.15),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: (isGemini ? const Color(0xFFA855F7) : const Color(0xFF38BDF8)).withOpacity(0.6)),
                    ),
                    child: Text(
                      'SOURCE: $source ($model)',
                      style: TextStyle(
                        color: isGemini ? const Color(0xFFA855F7) : const Color(0xFF38BDF8),
                        fontSize: 10,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  ElevatedButton.icon(
                    onPressed: _isAiExplanationLoading ? null : () => _loadAiExplanation(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: const Color(0xFF334155),
                      foregroundColor: const Color(0xFF38BDF8),
                      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                      textStyle: const TextStyle(fontSize: 11, fontWeight: FontWeight.bold),
                    ),
                    icon: _isAiExplanationLoading
                        ? const SizedBox(width: 12, height: 12, child: CircularProgressIndicator(strokeWidth: 2, color: Color(0xFF38BDF8)))
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
              color: const Color(0xFF0F172A),
              borderRadius: BorderRadius.circular(4),
              border: Border.all(color: const Color(0xFF334155)),
            ),
            child: const Text(
              'Notice: Gemini operates strictly as an explanation layer and does not calculate numerical risk scores or trajectories. Authoritative values are computed deterministically by the Digital Twin.',
              style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontStyle: FontStyle.italic),
            ),
          ),
          const SizedBox(height: 16),
          if (_isAiExplanationLoading && exp == null)
            const Center(
              child: Padding(
                padding: EdgeInsets.symmetric(vertical: 24),
                child: CircularProgressIndicator(color: Color(0xFFA855F7)),
              ),
            )
          else ...[
            Text(
              explanationText ?? 'No explanation generated yet. Click "Refresh Explanation" to synthesize clinical rationale.',
              style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 13, height: 1.5),
            ),
            if (trajText != null && trajText.isNotEmpty) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: const Color(0xFF0F172A),
                  borderRadius: BorderRadius.circular(8),
                  border: Border.all(color: const Color(0xFF38BDF8).withOpacity(0.3)),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.show_chart_rounded, color: Color(0xFF38BDF8), size: 18),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        trajText,
                        style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 12, fontWeight: FontWeight.w600, fontFamily: 'monospace'),
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
                style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: factors.map((f) {
                  return Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
                    decoration: BoxDecoration(
                      color: const Color(0xFF0F172A),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: const Color(0xFF334155)),
                    ),
                    child: Text(
                      f.toString(),
                      style: const TextStyle(color: Color(0xFFE2E8F0), fontSize: 11),
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
  Widget _buildLiveHealthAndTrendsSection() {
    final twin = _twinState ?? {};
    final vitals = twin['vitalsSnapshot'] as Map<String, dynamic>? ?? {};

    // Live Telemetry stream readings (with vitals fallback)
    final displayGlucose = _latestTelemetry?['cgmGlucoseMgDl'] != null
        ? (_latestTelemetry!['cgmGlucoseMgDl'] as num).toDouble().toStringAsFixed(1)
        : '${vitals['glucose'] ?? 95.0}';
    final displayRhr = _latestTelemetry?['restingHeartRateBpm'] != null
        ? (_latestTelemetry!['restingHeartRateBpm'] as num).toDouble().toStringAsFixed(1)
        : '${vitals['restingHeartRate'] ?? 62.0}';
    final displayHrv = _latestTelemetry?['hrvMs'] != null
        ? (_latestTelemetry!['hrvMs'] as num).toDouble().toStringAsFixed(1)
        : '${vitals['hrv'] ?? 54.0}';
    final displaySleep = _latestTelemetry?['sleepDurationHours'] != null
        ? (_latestTelemetry!['sleepDurationHours'] as num).toDouble().toStringAsFixed(1)
        : '${vitals['sleepHours'] ?? 7.5}';
    final displaySpo2 = '${vitals['spo2'] ?? 98.4}';
    final displaySteps = _latestTelemetry?['steps'] != null
        ? '${_latestTelemetry!['steps']}'
        : '${vitals['steps'] ?? 8500}';

    return Container(
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF334155)),
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
                style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.5),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFF0F172A),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: const Color(0xFF38BDF8).withOpacity(0.4)),
                ),
                child: Text(
                  _latestTelemetry != null ? '5-MIN STREAM SYNC' : 'WEARABLE STREAM',
                  style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 9, fontWeight: FontWeight.bold),
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
          LayoutBuilder(
            builder: (context, constraints) {
              final isWide = constraints.maxWidth >= 900;
              final pills = [
                _buildVitalMetricPill('CGM Glucose', displayGlucose, 'mg/dL', const Color(0xFF38BDF8)),
                _buildVitalMetricPill('Resting HR', displayRhr, 'bpm', const Color(0xFFF43F5E)),
                _buildVitalMetricPill('HRV', displayHrv, 'ms', const Color(0xFFA855F7)),
                _buildVitalMetricPill('Sleep', displaySleep, 'hrs', const Color(0xFF6366F1)),
                _buildVitalMetricPill('SpO2', displaySpo2, '%', const Color(0xFF10B981)),
                _buildVitalMetricPill('Activity', displaySteps, 'steps', const Color(0xFFF59E0B)),
              ];

              if (isWide) {
                return Row(
                  children: pills.map((p) => Expanded(child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: p))).toList(),
                );
              }

              final int cols = constraints.maxWidth < 450 ? 2 : 3;
              const double spacing = 8.0;
              final double itemWidth = (constraints.maxWidth - (cols - 1) * spacing) / cols;

              return Wrap(
                spacing: spacing,
                runSpacing: spacing,
                children: pills.map((p) => SizedBox(width: itemWidth, child: p)).toList(),
              );
            },
          ),

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
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: isSelected ? const Color(0xFF0284C7) : const Color(0xFF0F172A),
            borderRadius: BorderRadius.circular(6),
            border: Border.all(color: isSelected ? const Color(0xFF38BDF8) : const Color(0xFF334155)),
          ),
          child: Row(
            children: [
              Icon(icon, size: 14, color: isSelected ? Colors.white : const Color(0xFF94A3B8)),
              const SizedBox(width: 6),
              Text(
                label,
                style: TextStyle(
                  color: isSelected ? Colors.white : const Color(0xFF94A3B8),
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
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: c.withOpacity(0.3)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(title, style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.w600)),
          const SizedBox(height: 4),
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.end,
            spacing: 4,
            children: [
              Text(val, style: TextStyle(color: c, fontSize: 15, fontWeight: FontWeight.bold)),
              Text(unit, style: const TextStyle(color: Color(0xFF64748B), fontSize: 10)),
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
        child: const Text('No time-series data points available for this range.', style: TextStyle(color: Color(0xFF64748B))),
      );
    }

    return Container(
      height: 180,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFF0F172A),
        borderRadius: BorderRadius.circular(8),
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
                  style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Row(
                children: [
                  Container(width: 12, height: 2, color: Colors.amber),
                  const SizedBox(width: 6),
                  Text('Baseline Mean: $baselineMean ${stream['unit']}', style: const TextStyle(color: Colors.amber, fontSize: 11)),
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

                Color barColor = const Color(0xFF38BDF8);
                if (isElevated) barColor = const Color(0xFFEF4444);
                else if (isDepressed) barColor = const Color(0xFFA855F7);

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
                              borderRadius: const BorderRadius.vertical(top: Radius.circular(2)),
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
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF334155)),
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
                style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold, letterSpacing: 0.8),
              ),
              Text(
                'Z-score = (Current - Mean) / σ',
                style: TextStyle(color: Color(0xFF64748B), fontSize: 11, fontStyle: FontStyle.italic),
              ),
            ],
          ),
          const SizedBox(height: 14),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: DataTable(
              headingRowColor: MaterialStateProperty.all(const Color(0xFF0F172A)),
              columns: const [
                DataColumn(label: Text('METRIC', style: TextStyle(color: Color(0xFF94A3B8), fontWeight: FontWeight.bold))),
                DataColumn(label: Text('BASELINE MEAN', style: TextStyle(color: Color(0xFF94A3B8), fontWeight: FontWeight.bold))),
                DataColumn(label: Text('CURRENT VALUE', style: TextStyle(color: Color(0xFF94A3B8), fontWeight: FontWeight.bold))),
                DataColumn(label: Text('DEVIATION (Δ)', style: TextStyle(color: Color(0xFF94A3B8), fontWeight: FontWeight.bold))),
                DataColumn(label: Text('Z-SCORE', style: TextStyle(color: Color(0xFF94A3B8), fontWeight: FontWeight.bold))),
                DataColumn(label: Text('TREND', style: TextStyle(color: Color(0xFF94A3B8), fontWeight: FontWeight.bold))),
              ],
              rows: devs.map((d) {
                final m = d as Map<String, dynamic>;
                final zScore = m['zScore'] != null ? (m['zScore'] as num).toDouble() : null;
                final devPct = (m['percentageDeviation'] as num? ?? 0.0).toDouble();

                Color devColor = const Color(0xFF34D399);
                if (zScore != null && zScore.abs() >= 2.0) devColor = const Color(0xFFEF4444);
                else if (zScore != null && zScore.abs() >= 1.0) devColor = const Color(0xFFF97316);

                return DataRow(
                  cells: [
                    DataCell(Text(m['metric']?.toString().toUpperCase() ?? '', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w600))),
                    DataCell(Text('${m['baselineMean']} ${m['unit']}', style: const TextStyle(color: Color(0xFFCBD5E1)))),
                    DataCell(Text('${m['currentValue']} ${m['unit']}', style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold))),
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
                      decoration: BoxDecoration(color: devColor.withOpacity(0.15), borderRadius: BorderRadius.circular(4)),
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
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF38BDF8).withOpacity(0.4)),
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
                  const Icon(Icons.history_edu_rounded, color: Color(0xFF38BDF8), size: 18),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      'SYNTHETIC EHR / HISTORICAL DATA',
                      style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.bold),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: isSynthea ? const Color(0xFF0284C7).withOpacity(0.2) : const Color(0xFF334155),
                  borderRadius: BorderRadius.circular(4),
                  border: Border.all(color: isSynthea ? const Color(0xFF38BDF8) : const Color(0xFF64748B), width: 0.8),
                ),
                child: Text(
                  isSynthea ? 'SYNTHEA FHIR EHR' : 'STATIC EHR STREAM',
                  style: TextStyle(color: isSynthea ? const Color(0xFF38BDF8) : const Color(0xFF94A3B8), fontSize: 10, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          const Text(
            'Static stream: Longitudinal EHR, confirmed diagnoses, historical lab panels, and medications.',
            style: TextStyle(color: Color(0xFF64748B), fontSize: 11),
          ),
          const SizedBox(height: 14),

          // Historical Labs summary chips
          if (labs.isNotEmpty) ...[
            const Text('HISTORICAL LABORATORY PANELS:', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: labs.take(6).map((l) {
                final m = l as Map<String, dynamic>;
                return Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(6),
                    border: Border.all(color: const Color(0xFF334155)),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text('${m['biomarker']}: ', style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11)),
                      Text('${m['value']} ${m['unit'] ?? ""}', style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 11, fontWeight: FontWeight.bold)),
                    ],
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 14),
          ],

          const Text('DOCUMENTED CONDITIONS & DIAGNOSES:', style: TextStyle(color: Color(0xFF94A3B8), fontSize: 11, fontWeight: FontWeight.bold)),
          const SizedBox(height: 8),
          if (records.isEmpty)
            const Text('No previous medical records registered.', style: TextStyle(color: Color(0xFF64748B)))
          else
            Column(
              children: records.map((r) {
                final m = r as Map<String, dynamic>;
                return Container(
                  margin: const EdgeInsets.only(bottom: 10),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: const Color(0xFF0F172A),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: const Color(0xFF334155)),
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
                              style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 13),
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(color: const Color(0xFF334155), borderRadius: BorderRadius.circular(4)),
                            child: Text(m['status']?.toString() ?? 'ACTIVE', style: const TextStyle(color: Color(0xFF38BDF8), fontSize: 10)),
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text('Diagnosed: ${m['diagnosedDate'] ?? "Historical"} • Severity: ${m['severity'] ?? "Standard"} • ICD-10: ${m['icd10Code'] ?? "Uncoded"}',
                          style: const TextStyle(color: Color(0xFF64748B), fontSize: 11)),
                      if (m['medications'] != null && m['medications'].toString().isNotEmpty) ...[
                        const SizedBox(height: 4),
                        Text('Rx: ${m['medications']}', style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11), maxLines: 2, overflow: TextOverflow.ellipsis),
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
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF334155)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Row(
            children: [
              Icon(Icons.timeline_rounded, color: Color(0xFF38BDF8), size: 18),
              SizedBox(width: 8),
              Flexible(
                child: Text(
                  'UNIFIED EVENT TIMELINE',
                  style: TextStyle(color: Colors.white, fontSize: 13, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          if (_timelineEvents.isEmpty)
            const Text('No timeline events recorded.', style: TextStyle(color: Color(0xFF64748B)))
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
                        const Icon(Icons.circle, color: Color(0xFF38BDF8), size: 8),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(e['title']?.toString() ?? '', style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600)),
                              Text(e['formattedTime']?.toString() ?? '', style: const TextStyle(color: Color(0xFF64748B), fontSize: 10)),
                              if (e['subtitle'] != null)
                                Text(e['subtitle'].toString(), style: const TextStyle(color: Color(0xFF94A3B8), fontSize: 11)),
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
      padding: const EdgeInsets.all(22),
      decoration: BoxDecoration(
        color: const Color(0xFF1E293B),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: const Color(0xFF38BDF8).withOpacity(0.4)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF38BDF8).withOpacity(0.15),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.smart_toy_outlined, color: Color(0xFF38BDF8), size: 20),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'VIRTUAL PATIENT INTERACTION ENGINE',
                      style: TextStyle(color: Color(0xFF38BDF8), fontSize: 11, fontWeight: FontWeight.bold, letterSpacing: 1.0),
                    ),
                    Text(
                      'Ask the Digital Twin — Strictly Grounded in Telemetry & Baselines',
                      style: TextStyle(color: Colors.white, fontSize: 15, fontWeight: FontWeight.bold),
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
                color: const Color(0xFF0F172A),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFF334155)),
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
                      color: isDoctor ? const Color(0xFF1E293B) : const Color(0xFF0284C7).withOpacity(0.15),
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(color: isDoctor ? const Color(0xFF334155) : const Color(0xFF38BDF8).withOpacity(0.4)),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          isDoctor ? 'Doctor Q:' : 'Virtual Twin Grounded Answer:',
                          style: TextStyle(
                            color: isDoctor ? const Color(0xFF94A3B8) : const Color(0xFF38BDF8),
                            fontSize: 10,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          item['text'] ?? '',
                          style: const TextStyle(color: Colors.white, fontSize: 12, height: 1.3),
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
                  style: const TextStyle(color: Colors.white, fontSize: 13),
                  decoration: InputDecoration(
                    hintText: 'Ask the Virtual Patient Digital Twin...',
                    hintStyle: const TextStyle(color: Color(0xFF64748B), fontSize: 13),
                    filled: true,
                    fillColor: const Color(0xFF0F172A),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF334155))),
                    enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(8), borderSide: const BorderSide(color: Color(0xFF334155))),
                  ),
                  onSubmitted: (_) => _askQuestion(),
                ),
              ),
              const SizedBox(width: 10),
              ElevatedButton.icon(
                onPressed: _isAskingQuestion ? null : () => _askQuestion(),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF0284C7),
                  foregroundColor: Colors.white,
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
      label: Text(text, style: const TextStyle(color: Color(0xFFCBD5E1), fontSize: 11)),
      backgroundColor: const Color(0xFF0F172A),
      side: const BorderSide(color: Color(0xFF334155)),
      onPressed: () => _askQuestion(text),
    );
  }

  Color _getStateColor(String state) {
    switch (state) {
      case 'STABLE':
        return const Color(0xFF34D399);
      case 'PRE_SYMPTOMATIC_DRIFT':
        return const Color(0xFFFBBF24);
      case 'ELEVATED_RISK':
        return const Color(0xFFF97316);
      case 'ACTIVE_ANOMALY':
        return const Color(0xFFEF4444);
      default:
        return const Color(0xFF38BDF8);
    }
  }
}
