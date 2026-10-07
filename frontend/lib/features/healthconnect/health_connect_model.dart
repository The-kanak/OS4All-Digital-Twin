// ignore_for_file: constant_identifier_names

/// Health Connect integration states.
enum HealthConnectStatus {
  HEALTH_CONNECT_AVAILABLE,
  HEALTH_CONNECT_UNAVAILABLE,
  PERMISSION_NOT_GRANTED,
  PERMISSION_GRANTED,
  NO_DATA,
  ERROR;

  static HealthConnectStatus fromString(String? val) {
    if (val == null) return HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    switch (val.trim().toUpperCase()) {
      case 'HEALTH_CONNECT_AVAILABLE':
        return HealthConnectStatus.HEALTH_CONNECT_AVAILABLE;
      case 'PERMISSION_GRANTED':
        return HealthConnectStatus.PERMISSION_GRANTED;
      case 'PERMISSION_NOT_GRANTED':
        return HealthConnectStatus.PERMISSION_NOT_GRANTED;
      case 'NO_DATA':
        return HealthConnectStatus.NO_DATA;
      case 'ERROR':
        return HealthConnectStatus.ERROR;
      case 'HEALTH_CONNECT_UNAVAILABLE':
      default:
        return HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE;
    }
  }
}

/// Strongly-typed snapshot of data read from Android Health Connect.
/// Fields are nullable where data does not exist or has not been recorded on the device.
/// Never fabricates missing values.
class HealthConnectSnapshot {
  final int? steps;
  final double? heartRateBpm;
  final double? hrvMs;
  final double? sleepDurationHours;
  final double? sleepQuality;
  final double? restingHeartRateBpm;
  final double? bloodGlucoseMgDl;
  final DateTime? recordedAt;
  final String source;
  final bool available;
  final HealthConnectStatus status;
  final String? errorMessage;

  const HealthConnectSnapshot({
    this.steps,
    this.heartRateBpm,
    this.hrvMs,
    this.sleepDurationHours,
    this.sleepQuality,
    this.restingHeartRateBpm,
    this.bloodGlucoseMgDl,
    this.recordedAt,
    this.source = 'REAL_HEALTH_CONNECT',
    this.available = false,
    this.status = HealthConnectStatus.HEALTH_CONNECT_UNAVAILABLE,
    this.errorMessage,
  });

  /// Factory constructor parsing structured map returned from the Android Health Connect bridge.
  factory HealthConnectSnapshot.fromJson(Map<String, dynamic> json) {
    DateTime? parsedDate;
    final rawDate = json['recordedAt'];
    if (rawDate != null) {
      if (rawDate is String) {
        parsedDate = DateTime.tryParse(rawDate);
      } else if (rawDate is int) {
        parsedDate = DateTime.fromMillisecondsSinceEpoch(rawDate);
      }
    }

    final rawStatus = json['status']?.toString();
    final status = HealthConnectStatus.fromString(rawStatus);

    return HealthConnectSnapshot(
      steps: (json['steps'] as num?)?.toInt(),
      heartRateBpm: (json['heartRateBpm'] as num?)?.toDouble(),
      hrvMs: (json['hrvMs'] as num?)?.toDouble(),
      sleepDurationHours: (json['sleepDurationHours'] as num?)?.toDouble(),
      sleepQuality: (json['sleepQuality'] as num?)?.toDouble(),
      restingHeartRateBpm: (json['restingHeartRateBpm'] as num?)?.toDouble(),
      bloodGlucoseMgDl: (json['bloodGlucoseMgDl'] as num?)?.toDouble(),
      recordedAt: parsedDate,
      source: json['source']?.toString() ?? 'REAL_HEALTH_CONNECT',
      available: json['available'] == true || status == HealthConnectStatus.PERMISSION_GRANTED || status == HealthConnectStatus.HEALTH_CONNECT_AVAILABLE,
      status: status,
      errorMessage: json['errorMessage']?.toString(),
    );
  }

  /// Converts snapshot to JSON map.
  Map<String, dynamic> toJson() {
    return {
      if (steps != null) 'steps': steps,
      if (heartRateBpm != null) 'heartRateBpm': heartRateBpm,
      if (hrvMs != null) 'hrvMs': hrvMs,
      if (sleepDurationHours != null) 'sleepDurationHours': sleepDurationHours,
      if (sleepQuality != null) 'sleepQuality': sleepQuality,
      if (restingHeartRateBpm != null) 'restingHeartRateBpm': restingHeartRateBpm,
      if (bloodGlucoseMgDl != null) 'bloodGlucoseMgDl': bloodGlucoseMgDl,
      if (recordedAt != null) 'recordedAt': recordedAt!.toIso8601String(),
      'source': source,
      'available': available,
      'status': status.name,
      if (errorMessage != null) 'errorMessage': errorMessage,
    };
  }

  /// Maps Health Connect snapshot into OS4All Telemetry Ingestion payload.
  /// Does NOT fabricate continuous CGM velocity or activity classifications.
  Map<String, dynamic> toTelemetryPayload(String patientId) {
    return {
      'patientId': patientId,
      'timestamp': (recordedAt ?? DateTime.now()).toUtc().toIso8601String(),
      if (bloodGlucoseMgDl != null) 'glucose': bloodGlucoseMgDl,
      if (heartRateBpm != null) 'heartRate': heartRateBpm,
      if (hrvMs != null) 'hrv': hrvMs,
      if (restingHeartRateBpm != null) 'restingHeartRate': restingHeartRateBpm,
      if (sleepDurationHours != null) 'sleepDurationHours': sleepDurationHours,
      if (sleepQuality != null) 'sleepQualityScore': sleepQuality,
      if (steps != null) 'steps': steps,
      'source': 'REAL_HEALTH_CONNECT',
      'scenario': 'REAL_DEVICE_DATA',
    };
  }

  /// Returns true if at least one physiological health metric was captured.
  bool get hasAnyHealthData =>
      steps != null ||
      heartRateBpm != null ||
      hrvMs != null ||
      restingHeartRateBpm != null ||
      sleepDurationHours != null ||
      bloodGlucoseMgDl != null;
}
