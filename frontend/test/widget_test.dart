import 'package:flutter_test/flutter_test.dart';
import 'package:os4all_frontend/main.dart';
import 'package:os4all_frontend/core/network/app_config.dart';

void main() {
  testWidgets('OS4AllApp renders splash screen correctly', (WidgetTester tester) async {
    // Build our OS4All application
    await tester.pumpWidget(const OS4AllApp());

    // Verify brand name and tagline are rendered
    expect(find.text(AppConfig.appName), findsOneWidget);
    expect(find.text(AppConfig.tagline), findsOneWidget);

    // Drain navigation timer to complete test cleanly
    await tester.pumpAndSettle(const Duration(seconds: 3));
  });
}
