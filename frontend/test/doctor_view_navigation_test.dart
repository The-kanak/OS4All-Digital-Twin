import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/shell/app_shell.dart';
import 'package:os4all_frontend/features/digitaltwin/doctor_dashboard_screen.dart';

void main() {
  testWidgets('New UI: TWIN tab -> Doctor View opens DoctorDashboardScreen directly', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(392, 840);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      const MaterialApp(
        home: AppShell(initialTab: 1), // TWIN tab is index 1
      ),
    );
    await tester.pumpAndSettle();

    // Verify TWIN tab is displayed
    expect(find.text('Your Digital Twin'), findsOneWidget);

    // Verify 'Doctor View' action button is present in the header
    final doctorViewFinder = find.text('Doctor View');
    expect(doctorViewFinder, findsOneWidget);

    // Verify tooltip
    expect(find.byTooltip('Clinical workstation'), findsOneWidget);

    // Tap 'Doctor View'
    await tester.tap(doctorViewFinder);
    await tester.pumpAndSettle();

    // Verify DoctorDashboardScreen is opened directly
    expect(find.byType(DoctorDashboardScreen), findsOneWidget);

    // Verify Clinical Workstation Header is rendered in the body
    expect(find.text('CLINICAL TWIN WORKSTATION'), findsOneWidget);
    expect(find.text('HACKATHON PROTOTYPE — NOT A MEDICAL DIAGNOSIS'), findsOneWidget);

    // Verify key doctor panels are rendered (not blank)
    expect(find.text('VIRTUAL PATIENTS'), findsOneWidget);
    expect(find.text('DUAL-STREAM DATA FUSION ARCHITECTURE'), findsOneWidget);
    expect(find.text('SIMULATION CONTROLLER'), findsOneWidget);
    expect(find.text('PRIMARY CLINICAL USE CASE'), findsOneWidget);

    // Verify patient name is populated with Shara Senger from TWIN screen
    expect(find.textContaining('Shara Senger'), findsAtLeastNWidgets(1));

    // Verify demographics are fully populated (no nulls)
    expect(find.textContaining('54 years old'), findsOneWidget);
    expect(find.textContaining('FEMALE'), findsAtLeastNWidgets(1));
    expect(find.textContaining('Height: 168'), findsOneWidget);
    expect(find.textContaining('Weight: 78'), findsOneWidget);
    expect(find.textContaining('BMI: 27.6'), findsOneWidget);
    expect(find.textContaining('Blood Type: A+'), findsOneWidget);

    // Verify Risk Score matches populated synthetic twin state
    expect(find.textContaining('76.4 / 100'), findsAtLeastNWidgets(1));

    // Verify no literal 'null' string is rendered anywhere in demographics or titles
    expect(find.textContaining('null years old'), findsNothing);
    expect(find.textContaining('Height: null'), findsNothing);
    expect(find.textContaining('Weight: null'), findsNothing);
    expect(find.textContaining('BMI: null'), findsNothing);
    expect(find.textContaining('Blood Type: null'), findsNothing);

    // Tap back button
    final backButton = find.byType(BackButton);
    expect(backButton, findsOneWidget);
    await tester.tap(backButton);
    await tester.pumpAndSettle();

    // Verify returned cleanly to TWIN tab
    expect(find.text('Your Digital Twin'), findsOneWidget);
  });
}
