import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/features/insights/intelligence_trace_screen.dart';

void main() {
  testWidgets('IntelligenceTraceScreen renders all 8 pipeline steps with status, latency, and tags',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      const MaterialApp(
        home: IntelligenceTraceScreen(),
      ),
    );

    // Verify header and judge summary card
    expect(find.text('OS4All Intelligence Trace'), findsOneWidget);
    expect(find.text('Deterministic Health-Intelligence Trace'), findsOneWidget);
    expect(find.text('DEMO TRACE'), findsOneWidget);

    // Verify all 8 steps in the pipeline hierarchy are present
    expect(find.text('DATA RECEIVED'), findsOneWidget);
    expect(find.text('BASELINE COMPARED'), findsOneWidget);
    expect(find.text('TREND DETECTED'), findsOneWidget);
    expect(find.text('PATTERN ANALYZED'), findsOneWidget);
    expect(find.text('NEMOTRON REASONING'), findsOneWidget);
    expect(find.text('EVIDENCE RETRIEVED'), findsOneWidget);
    expect(find.text('INSIGHT GENERATED'), findsOneWidget);
    expect(find.text('ACTION RECOMMENDED'), findsOneWidget);

    // Verify required provider tags
    expect(find.text('NVIDIA Nemotron via Nebius'), findsOneWidget);
    expect(find.text('Tavily Evidence Agent'), findsOneWidget);

    // Verify privacy & security verification card is present
    expect(find.text('Privacy & Security Verification'), findsOneWidget);

    // Test "View details" technical interaction
    final viewDetailsFinder = find.text('View details (Technical Audit)').first;
    expect(viewDetailsFinder, findsOneWidget);

    await tester.tap(viewDetailsFinder);
    await tester.pumpAndSettle();

    expect(find.text('Hide technical details'), findsOneWidget);
    expect(find.text('Signals Ingested: '), findsOneWidget);
  });
}
