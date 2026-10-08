import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/digitaltwin/doctor_dashboard_screen.dart';

class RealHttpOverrides extends HttpOverrides {
  @override
  HttpClient createHttpClient(SecurityContext? context) {
    return super.createHttpClient(context);
  }
}

void main() {
  HttpOverrides.global = RealHttpOverrides();

  testWidgets('Test DoctorDashboardScreen with real backend responses', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    // Let's create an ErrorHandler to catch any FlutterError
    FlutterErrorDetails? caughtError;
    final originalOnError = FlutterError.onError;
    FlutterError.onError = (FlutterErrorDetails details) {
      caughtError = details;
      originalOnError?.call(details);
    };

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );

    // Wait for async timers / futures
    await tester.pump(const Duration(milliseconds: 500));
    await tester.pump(const Duration(seconds: 1));

    if (caughtError != null) {
      fail('Caught Flutter error: ${caughtError!.exceptionAsString()}\n${caughtError!.stack}');
    }
  });
}
