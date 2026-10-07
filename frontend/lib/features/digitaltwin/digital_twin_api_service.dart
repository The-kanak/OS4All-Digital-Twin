import 'dart:convert';
import 'package:http/http.dart' as http;
import '../../core/network/app_config.dart';

class DigitalTwinApiService {
  static final DigitalTwinApiService _instance = DigitalTwinApiService._internal();
  factory DigitalTwinApiService() => _instance;
  DigitalTwinApiService._internal();

  String get baseUrl => AppConfig.apiBaseUrl;

  Future<List<dynamic>> getPatients() async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as List<dynamic>? ?? [];
      }
    } catch (_) {}
    return [];
  }

  Future<Map<String, dynamic>?> getPatientDetail(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getDigitalTwinState(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId/digital-twin'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getPrediction(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId/prediction'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getWearableStream(String patientId, String metric, {int days = 7}) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId/wearables?metric=$metric&days=$days'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getBaseline(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId/baseline'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<List<dynamic>> getTimeline(String patientId, {int days = 30}) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId/timeline?days=$days'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as List<dynamic>? ?? [];
      }
    } catch (_) {}
    return [];
  }

  Future<Map<String, dynamic>?> injectScenario(String patientId, String scenario) async {
    try {
      final res = await http.post(Uri.parse('$baseUrl/simulation/scenario/$scenario?patientId=$patientId'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> nextSimulationReading(String patientId) async {
    try {
      final res = await http.post(Uri.parse('$baseUrl/simulation/next-reading?patientId=$patientId'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> resetSimulation(String patientId) async {
    try {
      final res = await http.post(Uri.parse('$baseUrl/simulation/reset?patientId=$patientId'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> interact(String patientId, String question) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/patients/$patientId/interact'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'question': question}),
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  String get telemetryUrl => baseUrl.replaceAll(RegExp(r'/v1$'), '') + '/telemetry';

  Future<Map<String, dynamic>?> getLatestTelemetry(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$telemetryUrl/$patientId/latest'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<List<dynamic>> getTelemetryHistory(String patientId, {int limit = 50}) async {
    try {
      final res = await http.get(Uri.parse('$telemetryUrl/$patientId/history?limit=$limit'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as List<dynamic>? ?? [];
      }
    } catch (_) {}
    return [];
  }

  Future<Map<String, dynamic>?> getTelemetryStream(String patientId, {String metric = 'glucose', int days = 7}) async {
    try {
      final res = await http.get(Uri.parse('$telemetryUrl/$patientId/stream?metric=$metric&days=$days'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> startTelemetrySimulation(String patientId, {String? scenario, int intervalSeconds = 5}) async {
    try {
      final uri = Uri.parse('$telemetryUrl/simulation/start?patientId=$patientId&intervalSeconds=$intervalSeconds${scenario != null ? '&scenario=$scenario' : ''}');
      final res = await http.post(uri);
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> stopTelemetrySimulation(String patientId) async {
    try {
      final res = await http.post(Uri.parse('$telemetryUrl/simulation/stop?patientId=$patientId'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> setTelemetryScenario(String patientId, String scenario) async {
    try {
      final res = await http.post(Uri.parse('$telemetryUrl/simulation/scenario?patientId=$patientId&scenario=$scenario'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getTelemetrySimulationStatus(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$telemetryUrl/simulation/status?patientId=$patientId'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> importSynthea({String? path, bool metabolicOnly = true}) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/../ehr/import/synthea'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          if (path != null) 'path': path,
          'metabolicOnly': metabolicOnly.toString(),
        }),
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getGeminiExplanation(String patientId, {String? focus}) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/patients/$patientId/gemini/explanation'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'context': 'digital-twin',
          if (focus != null) 'focus': focus,
        }),
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getGeminiChat(String patientId, String question) async {
    try {
      final res = await http.post(
        Uri.parse('$baseUrl/patients/$patientId/gemini/chat'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'question': question}),
      );
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }

  Future<Map<String, dynamic>?> getGeminiStatus(String patientId) async {
    try {
      final res = await http.get(Uri.parse('$baseUrl/patients/$patientId/gemini/status'));
      if (res.statusCode == 200) {
        final data = jsonDecode(res.body);
        return data['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }
}
