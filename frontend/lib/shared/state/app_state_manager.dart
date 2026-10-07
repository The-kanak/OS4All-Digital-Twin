import 'package:flutter/material.dart';
import '../../core/utils/demo_data.dart';

/// Global application state management using ChangeNotifier.
/// Tracks authentication status, demo mode toggle, active health state, and cached signals.
class AppStateManager extends ChangeNotifier {
  bool _isAuthenticated = true; // Set to true by default for instantaneous demo exploring
  bool _useDemoData = true;
  String _userToken = 'mock-jwt-token';
  String _userEmail = DemoData.userEmail;
  String _userName = DemoData.userFullName;

  bool _consentDataStorage = true;
  bool _consentAiInference = true;
  bool _consentEvidenceSearch = true;

  bool get isAuthenticated => _isAuthenticated;
  bool get useDemoData => _useDemoData;
  String get userToken => _userToken;
  String get userEmail => _userEmail;
  String get userName => _userName;

  bool get consentDataStorage => _consentDataStorage;
  bool get consentAiInference => _consentAiInference;
  bool get consentEvidenceSearch => _consentEvidenceSearch;

  void login(String email, String token, String name) {
    _isAuthenticated = true;
    _userEmail = email;
    _userToken = token;
    _userName = name;
    notifyListeners();
  }

  void logout() {
    _isAuthenticated = false;
    _userToken = '';
    notifyListeners();
  }

  void toggleDemoMode(bool enabled) {
    _useDemoData = enabled;
    notifyListeners();
  }

  void updateConsent({bool? dataStorage, bool? aiInference, bool? evidenceSearch}) {
    if (dataStorage != null) _consentDataStorage = dataStorage;
    if (aiInference != null) _consentAiInference = aiInference;
    if (evidenceSearch != null) _consentEvidenceSearch = evidenceSearch;
    notifyListeners();
  }
}
