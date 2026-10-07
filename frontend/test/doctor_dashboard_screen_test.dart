import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/digitaltwin/doctor_dashboard_screen.dart';

void main() {
  testWidgets('DoctorDashboardScreen renders all competition digital twin workstation elements', (WidgetTester tester) async {
    // Set widescreen desktop/tablet dimensions typical for doctor dashboard
    tester.view.physicalSize = const Size(1400, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );

    // Initial pump
    await tester.pump();

    // Verify Clinical Workstation Header & Hackathon Disclaimer
    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.text('HACKATHON PROTOTYPE — NOT A MEDICAL DIAGNOSIS'), findsOneWidget);

    // Verify Key Panels
    expect(find.text('VIRTUAL PATIENTS'), findsOneWidget);
    expect(find.text('DUAL-STREAM DATA FUSION ARCHITECTURE'), findsOneWidget);
    expect(find.text('SIMULATION CONTROLLER'), findsOneWidget);
    expect(find.text('PRIMARY CLINICAL USE CASE'), findsOneWidget);
    expect(find.text('LIVE WEARABLE & CGM TELEMETRY'), findsOneWidget);
    expect(find.text('PERSONALIZED BASELINE COMPARISON ENGINE'), findsOneWidget);
    expect(find.text('SYNTHETIC EHR / HISTORICAL DATA'), findsOneWidget);
    expect(find.text('UNIFIED EVENT TIMELINE'), findsOneWidget);
    expect(find.text('VIRTUAL PATIENT INTERACTION ENGINE'), findsOneWidget);
    expect(find.text('AI CLINICAL EXPLANATION LAYER'), findsOneWidget);

    // Verify all 5 competition simulation scenario buttons
    expect(find.text('1. Stable Homeostasis'), findsOneWidget);
    expect(find.text('2. Poor Sleep → Drift'), findsOneWidget);
    expect(find.text('3. High Activity (GLUT4)'), findsOneWidget);
    expect(find.text('4. Glucose Spike Risk'), findsOneWidget);
    expect(find.text('5. Recovery After Walk'), findsOneWidget);
  });

  testWidgets('DoctorDashboardScreen renders cleanly on Android mobile phone dimensions (392x840) without overflows', (WidgetTester tester) async {
    // Set standard mobile phone dimensions (e.g. Xiaomi / Pixel: 392 x 840 logical dp)
    tester.view.physicalSize = const Size(392, 840);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );

    await tester.pump();

    // Verify Clinical Workstation Header & Hackathon Disclaimer are rendered
    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.text('HACKATHON PROTOTYPE — NOT A MEDICAL DIAGNOSIS'), findsOneWidget);

    // Verify Key Panels on mobile
    expect(find.text('VIRTUAL PATIENTS'), findsOneWidget);
    expect(find.text('DUAL-STREAM DATA FUSION ARCHITECTURE'), findsOneWidget);
    expect(find.text('SIMULATION CONTROLLER'), findsOneWidget);
    expect(find.text('PRIMARY CLINICAL USE CASE'), findsOneWidget);
    expect(find.text('LIVE WEARABLE & CGM TELEMETRY'), findsOneWidget);
    expect(find.text('PERSONALIZED BASELINE COMPARISON ENGINE'), findsOneWidget);
    expect(find.text('SYNTHETIC EHR / HISTORICAL DATA'), findsOneWidget);
    expect(find.text('UNIFIED EVENT TIMELINE'), findsOneWidget);
    expect(find.text('VIRTUAL PATIENT INTERACTION ENGINE'), findsOneWidget);
    expect(find.text('AI CLINICAL EXPLANATION LAYER'), findsOneWidget);

    // Verify all 5 competition simulation scenario buttons are present and accessible
    expect(find.text('1. Stable Homeostasis'), findsOneWidget);
    expect(find.text('2. Poor Sleep → Drift'), findsOneWidget);
    expect(find.text('3. High Activity (GLUT4)'), findsOneWidget);
    expect(find.text('4. Glucose Spike Risk'), findsOneWidget);
    expect(find.text('5. Recovery After Walk'), findsOneWidget);

    // Ensure tester catches no layout overflow exceptions
    expect(tester.takeException(), isNull);
  });

  testWidgets('DoctorDashboardScreen renders on Xiaomi M2101K7BI physical resolution (1080x2400 @ 2.755 DPR) and scrolls cleanly', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(1080, 2400);
    tester.view.devicePixelRatio = 2.755;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );
    await tester.pump();

    // Verify key headers
    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.text('HACKATHON PROTOTYPE — NOT A MEDICAL DIAGNOSIS'), findsOneWidget);

    // Scroll down multiple times to exercise layout for all off-screen widgets
    final scrollableFinder = find.byType(Scrollable).first;
    await tester.drag(scrollableFinder, const Offset(0, -600));
    await tester.pump();
    expect(tester.takeException(), isNull);

    await tester.drag(scrollableFinder, const Offset(0, -600));
    await tester.pump();
    expect(tester.takeException(), isNull);

    await tester.drag(scrollableFinder, const Offset(0, -600));
    await tester.pump();
    expect(tester.takeException(), isNull);

    // Verify Virtual Patient Interaction Card rendered
    expect(find.text('VIRTUAL PATIENT INTERACTION ENGINE'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('DoctorDashboardScreen renders on compact Android viewport (360x640) without overflow', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(360, 640);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );
    await tester.pump();

    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.text('SIMULATION CONTROLLER'), findsOneWidget);
    expect(find.text('PRIMARY CLINICAL USE CASE'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}

