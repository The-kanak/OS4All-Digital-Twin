import 'package:flutter/material.dart';
import '../../core/theme/mobile_design_system.dart';
import '../digitaltwin/digital_twin_controller.dart';
import '../home/home_screen.dart';
import '../digitaltwin/digital_twin_screen.dart';
import '../livedata/live_data_screen.dart';
import '../aiinsights/ai_insights_screen.dart';
import '../profile/profile_screen.dart';

/// AppShell
/// Unified Mobile Shell hosting the 5 primary tabs:
/// 1. HOME
/// 2. TWIN
/// 3. LIVE
/// 4. AI
/// 5. PROFILE
class AppShell extends StatefulWidget {
  final int initialTab;

  const AppShell({super.key, this.initialTab = 0});

  @override
  State<AppShell> createState() => _AppShellState();
}

class _AppShellState extends State<AppShell> {
  late int _currentIndex;

  @override
  void initState() {
    super.initState();
    _currentIndex = widget.initialTab;
    DigitalTwinController().initialize();
  }

  void _onTabSelected(int index) {
    setState(() {
      _currentIndex = index;
    });
  }

  @override
  Widget build(BuildContext context) {
    final screens = [
      HomeScreen(onNavigateToTab: _onTabSelected),
      const DigitalTwinScreen(),
      const LiveDataScreen(),
      const AiInsightsScreen(),
      const ProfileScreen(),
    ];

    return Scaffold(
      backgroundColor: MobileTheme.background,
      body: IndexedStack(
        index: _currentIndex,
        children: screens,
      ),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: MobileTheme.surface,
          border: Border(top: BorderSide(color: MobileTheme.border, width: 1)),
        ),
        child: NavigationBar(
          selectedIndex: _currentIndex,
          onDestinationSelected: _onTabSelected,
          backgroundColor: MobileTheme.surface,
          indicatorColor: MobileTheme.lightBlueAccent,
          elevation: 0,
          labelBehavior: NavigationDestinationLabelBehavior.alwaysShow,
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.home_outlined, color: MobileTheme.textSecondary),
              selectedIcon: Icon(Icons.home_rounded, color: MobileTheme.primary),
              label: 'HOME',
            ),
            NavigationDestination(
              icon: Icon(Icons.hub_outlined, color: MobileTheme.textSecondary),
              selectedIcon: Icon(Icons.hub_rounded, color: MobileTheme.primary),
              label: 'TWIN',
            ),
            NavigationDestination(
              icon: Icon(Icons.sensors_outlined, color: MobileTheme.textSecondary),
              selectedIcon: Icon(Icons.sensors_rounded, color: MobileTheme.primary),
              label: 'LIVE',
            ),
            NavigationDestination(
              icon: Icon(Icons.auto_awesome_outlined, color: MobileTheme.textSecondary),
              selectedIcon: Icon(Icons.auto_awesome_rounded, color: MobileTheme.geminiPurple),
              label: 'AI',
            ),
            NavigationDestination(
              icon: Icon(Icons.person_outline_rounded, color: MobileTheme.textSecondary),
              selectedIcon: Icon(Icons.person_rounded, color: MobileTheme.primary),
              label: 'PROFILE',
            ),
          ],
        ),
      ),
    );
  }
}
