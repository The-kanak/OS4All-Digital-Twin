import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/dashboard/dashboard_screen.dart';
import 'package:os4all_frontend/shared/widgets/baseline_signal_chart.dart';

void main() {
  testWidgets('DashboardScreen renders health-intelligence hierarchy and sections', (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: DashboardScreen(),
      ),
    );

    // Verify primary sections exist
    expect(find.text('OS4ALL HEALTH STATUS'), findsOneWidget);
    expect(find.text('STABLE'), findsWidgets);
    expect(find.text('DRIFT'), findsWidgets);
    expect(find.text('ANOMALY'), findsWidgets);
    expect(find.text('FOLLOW-UP'), findsWidgets);

    // Verify explanatory language (not frightening alarms)
    expect(find.text('What Changed? (Recent Changes)'), findsOneWidget);
    expect(find.text('Personal Baseline Engine'), findsOneWidget);
    expect(find.text('Signal Trends (With Baselines)'), findsOneWidget);
    expect(find.text('AI Health Intelligence Synthesis'), findsOneWidget);
    expect(find.text('Supporting Evidence'), findsOneWidget);
    expect(find.text('What Should I Do Next?'), findsOneWidget);
    expect(find.text('Health Timeline'), findsOneWidget);

    // Verify baseline chart renders
    expect(find.byType(BaselineSignalChart), findsOneWidget);

    // Switch to DRIFT state and verify explanatory text updates smoothly
    final driftSelector = find.byKey(const ValueKey('state_selector_drift'));
    await tester.ensureVisible(driftSelector);
    await tester.tap(driftSelector);
    await tester.pumpAndSettle();

    expect(find.text('Early Signal Drift Detected'), findsOneWidget);
  });

  testWidgets('DashboardScreen layouts on narrow mobile screen (360x780) with zero RenderFlex overflows', (WidgetTester tester) async {
    // Simulate typical Android phone viewport (Redmi Note 10 Pro / M2101K7BI width ~ 392 or narrow 360)
    tester.view.physicalSize = const Size(360, 780);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(() => tester.view.resetPhysicalSize());

    await tester.pumpWidget(
      const MaterialApp(
        home: DashboardScreen(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('HACKATHON DEMO MODE'), findsOneWidget);
    expect(find.text('OS4ALL HEALTH STATUS'), findsOneWidget);
    expect(find.text('What Changed? (Recent Changes)'), findsOneWidget);
    expect(find.text('Personal Baseline Engine'), findsOneWidget);
    expect(find.text('AI Health Intelligence Synthesis'), findsOneWidget);

    // No exception thrown during pump
    expect(tester.takeException(), isNull);
  });
}
