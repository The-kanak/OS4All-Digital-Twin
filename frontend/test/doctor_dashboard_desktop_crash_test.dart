import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/digitaltwin/doctor_dashboard_screen.dart';

void main() {
  testWidgets('DoctorDashboardScreen desktop layout test', (WidgetTester tester) async {
    tester.view.physicalSize = const Size(1200, 900);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      const MaterialApp(
        home: DoctorDashboardScreen(),
      ),
    );
    await tester.pumpAndSettle();

    // Verify if any exception happened
    expect(tester.takeException(), isNull);
  });
}
