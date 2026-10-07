import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:os4all_frontend/features/shell/app_shell.dart';
import 'package:os4all_frontend/features/home/home_screen.dart';
import 'package:os4all_frontend/features/digitaltwin/digital_twin_screen.dart';
import 'package:os4all_frontend/features/livedata/live_data_screen.dart';
import 'package:os4all_frontend/features/aiinsights/ai_insights_screen.dart';
import 'package:os4all_frontend/features/profile/profile_screen.dart';
import 'package:os4all_frontend/features/profile/edit_profile_screen.dart';
import 'package:os4all_frontend/features/digitaltwin/digital_twin_controller.dart';
import 'package:os4all_frontend/shared/widgets/os4_components.dart';

void main() {
  setUp(() {
    SharedPreferences.setMockInitialValues({});
  });

  group('OS4All Mobile-First UI/UX Redesign Test Suite', () {
    testWidgets('AppShell renders on physical Xiaomi M2101K7BI dimensions with zero overflows', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(1080, 2400);
      tester.view.devicePixelRatio = 2.755;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const MaterialApp(
          home: AppShell(),
        ),
      );
      await tester.pump();

      // Verify 5 Navigation Tabs are rendered
      expect(find.text('HOME'), findsOneWidget);
      expect(find.text('TWIN'), findsOneWidget);
      expect(find.text('LIVE'), findsOneWidget);
      expect(find.text('AI'), findsOneWidget);
      expect(find.text('PROFILE'), findsOneWidget);

      // Verify Home Screen Content rendered by default
      expect(find.text('DIGITAL TWIN STATE'), findsOneWidget);
      expect(find.text('2-HOUR OUTLOOK'), findsOneWidget);
      expect(find.text('WHAT IS DRIVING THE CHANGE?'), findsOneWidget);
      expect(find.text('LIVE SIGNALS'), findsOneWidget);
      expect(find.text('EXPLORE ACTIONS'), findsOneWidget);
      expect(find.text('Explore Digital Twin'), findsOneWidget);
      expect(find.text('Ask Digital Twin AI'), findsOneWidget);

      expect(tester.takeException(), isNull);
    });

    testWidgets('Bottom navigation switches smoothly through all 5 primary mobile screens', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(392, 840);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const MaterialApp(
          home: AppShell(),
        ),
      );
      await tester.pump();

      // 1. HOME SCREEN VERIFICATION
      expect(find.byType(HomeScreen), findsOneWidget);
      expect(find.text('Prototype • Not a medical diagnosis'), findsOneWidget);

      // 2. SWITCH TO DIGITAL TWIN TAB (index 1)
      await tester.tap(find.text('TWIN'));
      await tester.pump();

      expect(find.byType(DigitalTwinScreen), findsOneWidget);
      expect(find.text('Your Digital Twin'), findsOneWidget);
      expect(find.text('CURRENT PHYSIOLOGICAL STATE'), findsOneWidget);
      expect(find.text('CURRENT PHYSIOLOGY'), findsOneWidget);
      expect(find.text('2-HOUR PROJECTED TRAJECTORY'), findsOneWidget);
      expect(find.text('WHY IS THE TWIN CHANGING?'), findsOneWidget);
      expect(find.text('EXPLORE A SCENARIO'), findsOneWidget);

      // Verify all 5 competition simulation buttons
      expect(find.text('Baseline'), findsOneWidget);
      expect(find.text('Missed Insulin'), findsOneWidget);
      expect(find.text('High Carb Meal'), findsOneWidget);
      expect(find.text('Exercise'), findsOneWidget);
      expect(find.text('Stress Event'), findsOneWidget);

      // Test scenario interaction
      final scenarioBaseline = find.text('Baseline');
      await tester.ensureVisible(scenarioBaseline);
      await tester.tap(scenarioBaseline);
      await tester.pump();
      expect(tester.takeException(), isNull);

      // 3. SWITCH TO LIVE DATA TAB (index 2)
      await tester.tap(find.text('LIVE'));
      await tester.pump();
      expect(tester.takeException(), isNull);

      expect(find.byType(LiveDataScreen), findsOneWidget);
      expect(find.text('Live Signals'), findsOneWidget);
      expect(find.text('Android Health Connect'), findsOneWidget);
      expect(find.text('REAL-TIME SIGNALS'), findsOneWidget);
      expect(find.text('WEARABLES & DEVICES'), findsOneWidget);
      expect(find.text('DEMO DATA SIMULATION'), findsOneWidget);

      // 4. SWITCH TO AI INSIGHTS TAB (index 3)
      await tester.tap(find.text('AI'));
      await tester.pump();
      expect(tester.takeException(), isNull);

      expect(find.byType(AiInsightsScreen), findsOneWidget);
      expect(find.text('AI INSIGHTS'), findsOneWidget);
      expect(find.text('CURRENT SITUATION'), findsOneWidget);
      expect(find.text('WHY THIS IS HAPPENING'), findsOneWidget);
      expect(find.text('WHAT THE TWIN PROJECTS'), findsOneWidget);
      expect(find.text('ASK THE DIGITAL TWIN'), findsOneWidget);
      expect(find.text('What is changing?'), findsOneWidget);
      expect(find.text('Why is risk elevated?'), findsOneWidget);

      // Test preset question interaction
      final presetQuestion = find.text('What is changing?');
      await tester.ensureVisible(presetQuestion);
      await tester.tap(presetQuestion);
      await tester.pump();
      expect(tester.takeException(), isNull);

      // 5. SWITCH TO PROFILE TAB (index 4)
      await tester.tap(find.text('PROFILE'));
      await tester.pump();

      expect(find.byType(ProfileScreen), findsOneWidget);
      expect(find.text('Profile & Settings'), findsOneWidget);
      expect(find.text('PHYSIOLOGICAL BASELINE'), findsOneWidget);
      expect(find.text('HEALTH PROFILE & NAVIGATION'), findsOneWidget);
      expect(find.text('Edit Profile & Baselines'), findsOneWidget);

      // 6. OPEN EDIT PROFILE SCREEN
      final editProfileBtn = find.widgetWithText(PrimaryButton, 'Edit Profile & Baselines');
      await tester.ensureVisible(editProfileBtn);
      await tester.tap(editProfileBtn);
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(find.byType(EditProfileScreen), findsOneWidget);
      expect(find.text('Full Name'), findsOneWidget);
      expect(find.text('Age'), findsOneWidget);
      expect(find.text('Gender'), findsOneWidget);
      expect(find.text('Save'), findsOneWidget);

      // Tap Save in AppBar to return
      await tester.tap(find.text('Save'));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 500));

      expect(find.byType(ProfileScreen), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('Home screen quick action buttons navigate to respective tabs cleanly', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(392, 840);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const MaterialApp(
          home: AppShell(),
        ),
      );
      await tester.pump();

      // Tap "Explore Digital Twin" on Home Screen
      final viewTwinBtn = find.text('Explore Digital Twin');
      await tester.ensureVisible(viewTwinBtn);
      await tester.tap(viewTwinBtn);
      await tester.pump();

      expect(find.byType(DigitalTwinScreen), findsOneWidget);
      expect(tester.takeException(), isNull);

      // Switch back to Home
      await tester.tap(find.text('HOME'));
      await tester.pump();

      // Tap "Ask Digital Twin AI" on Home Screen
      final askAiBtn = find.text('Ask Digital Twin AI');
      await tester.ensureVisible(askAiBtn);
      await tester.tap(askAiBtn);
      await tester.pump();

      expect(find.byType(AiInsightsScreen), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('Health Connect sync button shows high-contrast floating SnackBar feedback', (WidgetTester tester) async {
      tester.view.physicalSize = const Size(392, 840);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);

      await tester.pumpWidget(
        const MaterialApp(
          home: Scaffold(
            body: LiveDataScreen(),
          ),
        ),
      );
      await tester.pump();

      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
        const MethodChannel('os4all/health_connect'),
        (MethodCall methodCall) async {
          if (methodCall.method == 'requestPermissions') {
            return {'granted': true, 'allGranted': true, 'status': 'PERMISSION_GRANTED'};
          }
          if (methodCall.method == 'readAllHealthData') {
            return {
              'bloodGlucose': 105.0,
              'restingHeartRate': 72.0,
              'heartRateVariability': 48.0,
              'dailySteps': 8420,
              'sleepDurationHours': 7.2,
              'hasData': true,
            };
          }
          return null;
        },
      );
      addTearDown(() {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
          const MethodChannel('os4all/health_connect'),
          null,
        );
      });

      // Find Health Connect sync button (only PrimaryButton on LiveDataScreen)
      final syncBtn = find.byType(PrimaryButton);
      expect(syncBtn, findsOneWidget);

      final controller = DigitalTwinController();
      controller.setLoading(false);
      await tester.pump();

      await tester.ensureVisible(syncBtn);
      await tester.tap(syncBtn);
      // Pump until async connectHealthConnect completes and SnackBar animation finishes
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 100));
      await tester.pumpAndSettle();

      // Verify SnackBar is displayed with floating behavior and dark navy background
      final snackBarFinder = find.byType(SnackBar);
      expect(snackBarFinder, findsOneWidget);

      final snackBar = tester.widget<SnackBar>(snackBarFinder);
      expect(snackBar.behavior, SnackBarBehavior.floating);
      expect(snackBar.backgroundColor, const Color(0xFF0F172A));

      // Verify clear feedback text is shown
      expect(
        find.textContaining('Health Connect'),
        findsWidgets,
      );

      expect(tester.takeException(), isNull);
    });
  });
}
