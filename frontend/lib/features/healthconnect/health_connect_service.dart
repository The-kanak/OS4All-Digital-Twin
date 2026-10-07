import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import '../../core/network/app_config.dart';
import 'health_connect_model.dart';

/// Service managing optional Android Health Connect integration.
/// Communicates with native Android via MethodChannel 'os4all/health_connect'.
/// Safely falls back on web, iOS, desktop, or devices without Health Connect installed.
class HealthConnectService {
  static final HealthConnectService _instance = HealthConnectService._internal();
  factory HealthConnectService() => _instance;
  HealthConnectService._internal();

  static const MethodChannel _channel = MethodChannel('os4all/health_connect');

  bool get isAndroidSupported => !kIsWeb && defaultTargetPlatform == TargetPlatform.android;

  String get telemetryUrl => '${AppConfig.apiBaseUrl.replaceAll(RegExp(r'/v1$'), '')}/telemetry';

  /// Checks whether Android Health Connect is installed and available on this device.
  Future<HealthConnectStatus> checkAvailability() async {
    if (!isAndroidSupported) {
      return HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    }
    try {
      final res = await _channel.invokeMethod<String>('checkAvailability');
      return HealthConnectStatus.fromString(res);
    } catch (_) {
      return HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    }
  }

  /// Checks if required read permissions are already granted.
  Future<Map<String, dynamic>> checkPermissions() async {
    if (!isAndroidSupported) {
      return {
        'hasPermissions': false,
        'allGranted': false,
        'status': HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE.name,
      };
    }
    try {
      final res = await _channel.invokeMapMethod<String, dynamic>('hasPermissions');
      return res ?? {'hasPermissions': false, 'allGranted': false, 'status': HealthConnectStatus.ERROR.name};
    } catch (e) {
      return {'hasPermissions': false, 'allGranted': false, 'status': HealthConnectStatus.ERROR.name, 'errorMessage': e.toString()};
    }
  }

  /// Prompts user to grant Health Connect read permissions via system dialog.
  Future<Map<String, dynamic>> requestPermissions() async {
    if (!isAndroidSupported) {
      return {
        'granted': false,
        'allGranted': false,
        'status': HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE.name,
        'errorMessage': 'Health Connect is only supported on physical or emulated Android devices (API 26+).',
      };
    }
    try {
      final res = await _channel.invokeMapMethod<String, dynamic>('requestPermissions');
      return res ?? {'granted': false, 'allGranted': false, 'status': HealthConnectStatus.PERMISSION_NOT_GRANTED.name};
    } catch (e) {
      return {'granted': false, 'allGranted': false, 'status': HealthConnectStatus.ERROR.name, 'errorMessage': e.toString()};
    }
  }

  /// Reads all supported health metrics from Health Connect across the last 24 hours.
  /// Supported metrics: Steps, Heart Rate, HRV, Resting Heart Rate, Sleep, Blood Glucose.
  Future<HealthConnectSnapshot> readAllHealthData() async {
    if (!isAndroidSupported) {
      return const HealthConnectSnapshot(
        available: false,
        status: HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE,
        errorMessage: 'Health Connect is not supported on this platform.',
      );
    }
    try {
      final res = await _channel.invokeMapMethod<String, dynamic>('readAllHealthData');
      if (res != null) {
        return HealthConnectSnapshot.fromJson(res);
      }
      return const HealthConnectSnapshot(
        status: HealthConnectStatus.NO_DATA,
        available: true,
      );
    } catch (e) {
      return HealthConnectSnapshot(
        status: HealthConnectStatus.ERROR,
        available: false,
        errorMessage: e.toString(),
      );
    }
  }

  /// Ingests a real-device Health Connect snapshot into the backend for the selected patient.
  /// Reuses existing telemetry validation, velocity calculation, and Digital Twin fusion.
  Future<Map<String, dynamic>?> syncSnapshotToBackend(String patientId, HealthConnectSnapshot snapshot) async {
    try {
      final payload = snapshot.toTelemetryPayload(patientId);
      final url = Uri.parse('$telemetryUrl/health-connect');
      final res = await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(payload),
      );

      if (res.statusCode == 200) {
        final body = jsonDecode(res.body);
        return body['data'] as Map<String, dynamic>?;
      }
    } catch (_) {}
    return null;
  }
}
