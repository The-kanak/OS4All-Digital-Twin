import 'dart:convert';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/digitaltwin/doctor_dashboard_screen.dart';

void main() {
  testWidgets('Test DoctorDashboardScreen rendering states', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    // Fetch real data directly via curl from localhost:8082
    final pRes = Process.runSync('curl', ['-s', 'http://localhost:8082/api/v1/patients']);
    final patientsJson = jsonDecode(pRes.stdout as String)['data'] as List<dynamic>;

    final id = '11111111-2222-3333-4444-555555555555';
    final dRes = Process.runSync('curl', ['-s', 'http://localhost:8082/api/v1/patients/$id']);
    final detailJson = jsonDecode(dRes.stdout as String)['data'] as Map<String, dynamic>;

    final twRes = Process.runSync('curl', ['-s', 'http://localhost:8082/api/v1/patients/$id/digital-twin']);
    final twinJson = jsonDecode(twRes.stdout as String)['data'] as Map<String, dynamic>;

    final prRes = Process.runSync('curl', ['-s', 'http://localhost:8082/api/v1/patients/$id/prediction']);
    final predJson = jsonDecode(prRes.stdout as String)['data'] as Map<String, dynamic>;

    final wRes = Process.runSync('curl', ['-s', 'http://localhost:8082/api/v1/patients/$id/wearables?metric=glucose&days=7']);
    final streamJson = jsonDecode(wRes.stdout as String)['data'] as Map<String, dynamic>;

    final tlRes = Process.runSync('curl', ['-s', 'http://localhost:8082/api/v1/patients/$id/timeline']);
    final timelineJson = jsonDecode(tlRes.stdout as String)['data'] as List<dynamic>;

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );

    // Let's pump once so initState fires
    await tester.pump();

    // Now let's see what is on screen
    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.textContaining('Alex Rivera'), findsAtLeastNWidgets(1));
    expect(find.text('SIMULATION CONTROLLER'), findsOneWidget);
    expect(find.text('PRIMARY CLINICAL USE CASE'), findsOneWidget);
    expect(find.text('AI CLINICAL EXPLANATION LAYER'), findsOneWidget);
    expect(find.text('LIVE WEARABLE & CGM TELEMETRY'), findsOneWidget);
    expect(find.text('PERSONALIZED BASELINE COMPARISON ENGINE'), findsOneWidget);
    expect(find.text('VIRTUAL PATIENT INTERACTION ENGINE'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('Test DoctorDashboardScreen rendering on mobile portrait (Xiaomi 393x851)', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(393, 851);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );
    await tester.pump();

    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.text('VIRTUAL PATIENTS'), findsOneWidget);
    expect(find.textContaining('Alex Rivera'), findsAtLeastNWidgets(1));
    expect(tester.takeException(), isNull);
  });
}
