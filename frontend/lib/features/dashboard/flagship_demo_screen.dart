import 'package:flutter/material.dart';
import 'demo_mode_screen.dart';

/// Legacy FlagshipDemoScreen mapped to modern DemoModeScreen
class FlagshipDemoScreen extends StatelessWidget {
  const FlagshipDemoScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const DemoModeScreen();
  }
}
