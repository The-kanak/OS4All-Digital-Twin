/// Environment configuration for OS4All Flutter App.
/// Never store sensitive keys or AI provider secrets on the client side!
class AppConfig {
  static const String appName = 'OS4All Digital Twin';
  static const String tagline = 'From Patient Data to a Living Health Model';
  static const String disclaimer = 'Research / Hackathon Prototype — Not a Medical Diagnosis.';
  
  // Can be overridden at build-time using --dart-define=API_BASE_URL=https://api.yourdomain.com
  static const String defaultApiBaseUrl = 'http://10.0.2.2:8082/api/v1'; // Android emulator localhost default
  static const String webLocalBaseUrl = 'http://localhost:8082/api/v1';

  static String get apiBaseUrl {
    const fromEnv = String.fromEnvironment('API_BASE_URL');
    if (fromEnv.isNotEmpty) return fromEnv;
    return webLocalBaseUrl;
  }

  /// Whether demo/development mode is enabled (visible for hackathon judging & dev environments)
  static const bool isDemoModeEnabled = bool.fromEnvironment('DEMO_MODE', defaultValue: true);
}
