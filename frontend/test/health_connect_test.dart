import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/healthconnect/health_connect_model.dart';
import 'package:os4all_frontend/features/healthconnect/health_connect_service.dart';

void main() {
  group('HealthConnectModel Tests', () {
    test('HealthConnectSnapshot parses full JSON accurately', () {
      final json = {
        'status': 'PERMISSION_GRANTED',
        'steps': 6540,
        'heartRateBpm': 72.5,
        'hrvMs': 48.0,
        'restingHeartRateBpm': 61.0,
        'bloodGlucoseMgDl': 105.5,
        'sleepDurationHours': 7.2,
        'sleepQuality': 0.85,
        'timestamp': '2026-10-06T08:30:00Z',
      };

      final snapshot = HealthConnectSnapshot.fromJson(json);

      expect(snapshot.status, HealthConnectStatus.PERMISSION_GRANTED);
      expect(snapshot.steps, 6540);
      expect(snapshot.heartRateBpm, 72.5);
      expect(snapshot.hrvMs, 48.0);
      expect(snapshot.sleepDurationHours, 7.2);
      expect(snapshot.sleepQuality, 0.85);
      expect(snapshot.restingHeartRateBpm, 61.0);
      expect(snapshot.bloodGlucoseMgDl, 105.5);
      expect(snapshot.hasAnyHealthData, isTrue);
      expect(snapshot.errorMessage, isNull);
    });

    test('HealthConnectSnapshot handles empty or missing data safely', () {
      final snapshot = HealthConnectSnapshot.fromJson({
        'status': 'NO_DATA',
        'errorMessage': 'No records found in Health Connect for the last 24h',
      });

      expect(snapshot.status, HealthConnectStatus.NO_DATA);
      expect(snapshot.steps, isNull);
      expect(snapshot.heartRateBpm, isNull);
      expect(snapshot.bloodGlucoseMgDl, isNull);
      expect(snapshot.hasAnyHealthData, isFalse);
      expect(snapshot.errorMessage, 'No records found in Health Connect for the last 24h');
    });

    test('toTelemetryPayload preserves real metrics and avoids fabricating glucose velocity', () {
      const snapshot = HealthConnectSnapshot(
        status: HealthConnectStatus.PERMISSION_GRANTED,
        steps: 8200,
        heartRateBpm: 75.0,
        hrvMs: 44.0,
        sleepDurationHours: 6.8,
        sleepQuality: 0.80,
        restingHeartRateBpm: 63.0,
        bloodGlucoseMgDl: 112.0,
      );

      final payload = snapshot.toTelemetryPayload('patient-test-123');

      expect(payload['patientId'], 'patient-test-123');
      expect(payload['steps'], 8200);
      expect(payload['heartRate'], 75.0);
      expect(payload['hrv'], 44.0);
      expect(payload['sleepDurationHours'], 6.8);
      expect(payload['sleepQualityScore'], 0.80);
      expect(payload['restingHeartRate'], 63.0);
      expect(payload['glucose'], 112.0);
      expect(payload['source'], 'REAL_HEALTH_CONNECT');
      expect(payload['scenario'], 'REAL_DEVICE_DATA');
      expect(payload['glucoseVelocityMgDlPerMin'], isNull, reason: 'Health Connect does not provide velocity; do not fabricate');
      expect(payload['activityLevel'], isNull, reason: 'Health Connect does not infer qualitative activity level; do not fabricate');
    });

    test('HealthConnectStatus enum values convert to and from string accurately', () {
      expect(HealthConnectStatus.values.length, 6);
      expect(HealthConnectSnapshot.fromJson({'status': 'HEALTH_CONNECT_UNAVAILABLE'}).status,
          HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE);
      expect(HealthConnectSnapshot.fromJson({'status': 'PERMISSION_NOT_GRANTED'}).status,
          HealthConnectStatus.PERMISSION_NOT_GRANTED);
      expect(HealthConnectSnapshot.fromJson({'status': 'ERROR'}).status,
          HealthConnectStatus.ERROR);
    });
  });

  group('HealthConnectService Platform Fallback Tests', () {
    setUp(() {
      debugDefaultTargetPlatformOverride = TargetPlatform.windows;
    });

    tearDown(() {
      debugDefaultTargetPlatformOverride = null;
    });

    test('Non-Android platforms gracefully report HEALTH_CONNECT_UNAVAILABLE without crashing', () async {
      final service = HealthConnectService();

      // On non-Android platform (e.g. Windows desktop, Web, macOS)
      final availability = await service.checkAvailability();
      expect(availability, HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE);

      final perms = await service.checkPermissions();
      expect(perms['hasPermissions'], isFalse);
      expect(perms['status'], 'HEALTH_CONNECT_UNAVAILABLE');

      final req = await service.requestPermissions();
      expect(req['granted'], isFalse);
      expect(req['status'], 'HEALTH_CONNECT_UNAVAILABLE');

      final snapshot = await service.readAllHealthData();
      expect(snapshot.status, HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE);
      expect(snapshot.hasAnyHealthData, isFalse);
    });
  });
}
