import 'package:flutter/material.dart';
import 'core/theme/app_theme.dart';
import 'core/network/app_config.dart';
import 'features/auth/splash_screen.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const OS4AllApp());
}

class OS4AllApp extends StatelessWidget {
  const OS4AllApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: AppConfig.appName,
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      home: const SplashScreen(isAuthenticated: true),
    );
  }
}
