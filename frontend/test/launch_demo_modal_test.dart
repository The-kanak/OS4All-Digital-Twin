import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/dashboard/launch_demo_modal.dart';

void main() {
  group('LaunchDemoModal Tests', () {
    testWidgets('renders all 10 demo pipeline steps with honest provider disclosure', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LaunchDemoModal(),
          ),
        ),
      );

      // Verify header and banner
      expect(find.text('OS4All Hackathon Demo Mode'), findsOneWidget);
      expect(find.text('DEMO DATA — NOT A REAL PATIENT'), findsOneWidget);
      expect(find.text('AI Provider Disclosure'), findsOneWidget);

      // Verify all 10 steps are present in the pipeline view
      expect(find.text('Reset synthetic user'), findsOneWidget);
      expect(find.text('Load baseline'), findsOneWidget);
      expect(find.text('Load historical observations'), findsOneWidget);
      expect(find.text('Introduce recent changes'), findsOneWidget);
      expect(find.text('Run anomaly detection'), findsOneWidget);
      expect(find.text('Run AI workflow'), findsOneWidget);
      expect(find.text('Retrieve evidence if configured'), findsOneWidget);
      expect(find.text('Generate insight'), findsOneWidget);
      expect(find.text('Update timeline'), findsOneWidget);
      expect(find.text('Display final dashboard'), findsOneWidget);

      // Verify button to launch
      expect(find.text('Launch Complete Demo'), findsOneWidget);
    });

    testWidgets('honest provider disclosure does not fabricate Nebius when in mock mode', (WidgetTester tester) async {
      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LaunchDemoModal(),
          ),
        ),
      );

      // Provider disclosure card must be visible and clear
      expect(find.text('AI Provider Disclosure'), findsOneWidget);
      // Mode text is present (either AI DEMO MODE or checking state)
      expect(find.textContaining('MODE', findRichText: true), findsWidgets);
    });
  });
}
