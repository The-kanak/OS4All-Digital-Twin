import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../core/widgets/os4all_brand_logo.dart';
import '../../shared/widgets/healthcare_widgets.dart';
import '../../shared/widgets/baseline_signal_chart.dart';

import '../vitals/vitals_screen.dart';
import '../baseline/baseline_screen.dart';
import '../timeline/timeline_screen.dart';
import '../insights/insights_screen.dart';
import '../counselor/counselor_screen.dart';
import '../alerts/alerts_screen.dart';
import '../labs/labs_screen.dart';
import '../profile/profile_screen.dart';
import '../../core/network/app_config.dart';
import '../evidence/evidence_screen.dart';
import '../insights/intelligence_trace_screen.dart';
import './flagship_demo_screen.dart';
import './launch_demo_modal.dart';
import '../digitaltwin/doctor_dashboard_screen.dart';

/// Health Intelligence Workflow States
enum HealthStatusState {
  stable,
  drift,
  anomaly,
  followUp;

  String get label {
    switch (this) {
      case HealthStatusState.stable:
        return 'STABLE';
      case HealthStatusState.drift:
        return 'DRIFT';
      case HealthStatusState.anomaly:
        return 'ANOMALY';
      case HealthStatusState.followUp:
        return 'FOLLOW-UP';
    }
  }

  Color get color {
    switch (this) {
      case HealthStatusState.stable:
        return AppTheme.stable;
      case HealthStatusState.drift:
        return AppTheme.warning;
      case HealthStatusState.anomaly:
        return const Color(0xFFEF4444);
      case HealthStatusState.followUp:
        return AppTheme.primary;
    }
  }

  IconData get icon {
    switch (this) {
      case HealthStatusState.stable:
        return Icons.check_circle_rounded;
      case HealthStatusState.drift:
        return Icons.trending_up_rounded;
      case HealthStatusState.anomaly:
        return Icons.warning_amber_rounded;
      case HealthStatusState.followUp:
        return Icons.calendar_month_rounded;
    }
  }
}

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({super.key});

  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  int _currentIndex = 0;
  HealthStatusState _activeState = HealthStatusState.stable;
  String _selectedMetricTab = 'restingHR'; // restingHR, hrv, sleep, spo2
  bool _isLoadingTelemetry = false;
  bool _hasTelemetryError = false;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        title: const Os4AllBrandLogo(fontSize: 18),
        titleSpacing: 8,
        actions: [
          if (AppConfig.isDemoModeEnabled)
            Padding(
              padding: const EdgeInsets.only(right: 4),
              child: ElevatedButton.icon(
                key: const ValueKey('launch_demo_appbar_btn'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  foregroundColor: Colors.white,
                  elevation: 1,
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  minimumSize: Size.zero,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                ),
                icon: const Icon(Icons.rocket_launch_rounded, size: 13),
                label: const Text('DEMO', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
                onPressed: () => LaunchDemoModal.show(
                  context,
                  onDemoCompleted: () {
                    setState(() {
                      _activeState = HealthStatusState.anomaly;
                    });
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Deterministic OS4All Demo pipeline completed successfully!'),
                        backgroundColor: AppTheme.primary,
                        duration: Duration(seconds: 3),
                      ),
                    );
                  },
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.only(right: 2),
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFF0284C7),
                foregroundColor: Colors.white,
                elevation: 1,
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              ),
              icon: const Icon(Icons.monitor_heart_rounded, size: 13),
              label: const Text('TWIN', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const DoctorDashboardScreen())),
            ),
          ),
          PopupMenuButton<String>(
            tooltip: 'Navigation & Workstations',
            icon: const Icon(Icons.more_vert_rounded, color: AppTheme.textPrimary, size: 20),
            padding: EdgeInsets.zero,
            onSelected: (val) {
              if (val == 'trace') {
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const IntelligenceTraceScreen()));
              } else if (val == 'demo_chain') {
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const FlagshipDemoScreen()));
              } else if (val == 'alerts') {
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AlertsScreen()));
              } else if (val == 'profile') {
                Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen()));
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(
                value: 'trace',
                child: Row(
                  children: [
                    Icon(Icons.account_tree_rounded, size: 18, color: AppTheme.secondary),
                    SizedBox(width: 8),
                    Text('Pipeline Trace'),
                  ],
                ),
              ),
              const PopupMenuItem(
                value: 'demo_chain',
                child: Row(
                  children: [
                    Icon(Icons.play_circle_filled_rounded, size: 18, color: AppTheme.primary),
                    SizedBox(width: 8),
                    Text('Demo Chain'),
                  ],
                ),
              ),
              const PopupMenuItem(
                value: 'alerts',
                child: Row(
                  children: [
                    Icon(Icons.notifications_none_rounded, size: 18, color: AppTheme.textPrimary),
                    SizedBox(width: 8),
                    Text('Notifications'),
                  ],
                ),
              ),
              const PopupMenuItem(
                value: 'profile',
                child: Row(
                  children: [
                    Icon(Icons.person_outline_rounded, size: 18, color: AppTheme.textPrimary),
                    SizedBox(width: 8),
                    Text('Profile Settings'),
                  ],
                ),
              ),
            ],
          ),
          if (MediaQuery.sizeOf(context).width >= 460) ...[
            IconButton(
              tooltip: 'Alerts',
              icon: Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceElevated,
                  shape: BoxShape.circle,
                  border: Border.all(color: AppTheme.border),
                ),
                child: const Icon(Icons.notifications_none_rounded, color: AppTheme.textPrimary, size: 18),
              ),
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const AlertsScreen())),
            ),
            IconButton(
              tooltip: 'Profile',
              icon: Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppTheme.surfaceElevated,
                  shape: BoxShape.circle,
                  border: Border.all(color: AppTheme.border),
                ),
                child: const Icon(Icons.person_outline_rounded, color: AppTheme.textPrimary, size: 18),
              ),
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const ProfileScreen())),
            ),
          ],
          const SizedBox(width: 4),
        ],
      ),
      body: _buildDashboardBody(),
      bottomNavigationBar: Container(
        decoration: const BoxDecoration(
          color: AppTheme.surface,
          border: Border(top: BorderSide(color: AppTheme.border, width: 1)),
        ),
        child: NavigationBar(
          backgroundColor: AppTheme.surface,
          indicatorColor: AppTheme.primary.withValues(alpha: 0.14),
          selectedIndex: _currentIndex,
          onDestinationSelected: (idx) {
            setState(() => _currentIndex = idx);
            if (idx == 1) Navigator.of(context).push(MaterialPageRoute(builder: (_) => const LabsScreen()));
            if (idx == 2) Navigator.of(context).push(MaterialPageRoute(builder: (_) => const TimelineScreen()));
            if (idx == 3) Navigator.of(context).push(MaterialPageRoute(builder: (_) => const InsightsScreen()));
            if (idx == 4) Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CounselorScreen()));
          },
          destinations: const [
            NavigationDestination(
              icon: Icon(Icons.favorite_outline_rounded),
              selectedIcon: Icon(Icons.favorite_rounded, color: AppTheme.primary),
              label: 'Overview',
            ),
            NavigationDestination(
              icon: Icon(Icons.science_outlined),
              selectedIcon: Icon(Icons.science_rounded, color: AppTheme.primary),
              label: 'Lab Vault',
            ),
            NavigationDestination(
              icon: Icon(Icons.timeline_rounded),
              selectedIcon: Icon(Icons.timeline_rounded, color: AppTheme.primary),
              label: 'Timeline',
            ),
            NavigationDestination(
              icon: Icon(Icons.auto_awesome_outlined),
              selectedIcon: Icon(Icons.auto_awesome_rounded, color: AppTheme.primary),
              label: 'Insights',
            ),
            NavigationDestination(
              icon: Icon(Icons.support_agent_outlined),
              selectedIcon: Icon(Icons.support_agent_rounded, color: AppTheme.primary),
              label: 'Counselor',
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildDashboardBody() {
    return SingleChildScrollView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 36),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Mandatory Demo Label
          const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
          const SizedBox(height: 8),

          // Launch Demo Interactive Hero Banner (Visible when Demo Mode enabled)
          if (AppConfig.isDemoModeEnabled) ...[
            _buildLaunchDemoBanner(),
            const SizedBox(height: 12),
          ],

          // User Header & Live Baseline Badge
          _buildUserHeader(),
          const SizedBox(height: 16),

          // ==========================================
          // 1. OS4All Health Status (Visual Indicator: STABLE, DRIFT, ANOMALY, FOLLOW-UP)
          // ==========================================
          _buildHealthStatusSection(),
          const SizedBox(height: 16),

          // State Selector Pill Bar (Allows interacting with STABLE / DRIFT / ANOMALY / FOLLOW-UP states)
          _buildVisualStateSelector(),
          const SizedBox(height: 18),

          // ==========================================
          // 2. Personal Baseline Section
          // ==========================================
          _buildPersonalBaselineSection(),
          const SizedBox(height: 18),

          // ==========================================
          // 3. What Changed? (Recent Changes)
          // ==========================================
          _buildRecentChangesSection(),
          const SizedBox(height: 18),

          // ==========================================
          // 4. Signal Trends (Interactive Charts with Personal Baseline Ranges)
          // ==========================================
          _buildSignalTrendsSection(),
          const SizedBox(height: 18),

          // ==========================================
          // 5. Why did it change? & What does it mean? (AI Insight)
          // ==========================================
          _buildAiInsightSection(),
          const SizedBox(height: 18),

          // ==========================================
          // 6. Supporting Evidence (Tavily Grounded Citations)
          // ==========================================
          _buildEvidenceSection(),
          const SizedBox(height: 18),

          // ==========================================
          // 7. What should I do next? (Recommended Next Step)
          // ==========================================
          _buildRecommendedNextStepSection(),
          const SizedBox(height: 18),

          // ==========================================
          // 8. Historical Timeline
          // ==========================================
          _buildHealthTimelineSection(),
          const SizedBox(height: 18),

          // AI Counselor Conversation Prompt Card
          _buildCounselorCard(),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // Launch Demo Interactive Banner (Hackathon Demo Mode)
  // -------------------------------------------------------------
  Widget _buildLaunchDemoBanner() {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            AppTheme.primary.withValues(alpha: 0.16),
            AppTheme.secondary.withValues(alpha: 0.08),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppTheme.primary.withValues(alpha: 0.35), width: 1.2),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isNarrow = constraints.maxWidth < 500;
          if (isNarrow) {
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(9),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withValues(alpha: 0.2),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.rocket_launch_rounded, color: AppTheme.primary, size: 22),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Wrap(
                            crossAxisAlignment: WrapCrossAlignment.center,
                            spacing: 8,
                            runSpacing: 4,
                            children: [
                              const Text(
                                'HACKATHON DEMO MODE',
                                style: TextStyle(
                                  fontSize: 13,
                                  fontWeight: FontWeight.w900,
                                  letterSpacing: 0.8,
                                  color: AppTheme.primary,
                                ),
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: AppTheme.surfaceElevated,
                                  borderRadius: BorderRadius.circular(6),
                                  border: Border.all(color: AppTheme.border),
                                ),
                                child: const Text(
                                  '10 STEPS',
                                  style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          const Text(
                            'Run the complete end-to-end OS4All health-intelligence pipeline with live telemetry.',
                            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary, height: 1.3),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                SizedBox(
                  width: double.infinity,
                  child: ElevatedButton(
                    key: const ValueKey('launch_demo_banner_btn'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.primary,
                      foregroundColor: Colors.white,
                      elevation: 0,
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                    ),
                    onPressed: () => LaunchDemoModal.show(
                      context,
                      onDemoCompleted: () {
                        setState(() {
                          _activeState = HealthStatusState.anomaly;
                        });
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(
                            content: Text('Deterministic OS4All Demo pipeline completed successfully!'),
                            backgroundColor: AppTheme.primary,
                            duration: Duration(seconds: 3),
                          ),
                        );
                      },
                    ),
                    child: const Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.play_arrow_rounded, size: 18),
                        SizedBox(width: 4),
                        Text('Launch Demo', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                      ],
                    ),
                  ),
                ),
              ],
            );
          }

          return Row(
            children: [
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: 0.2),
                  shape: BoxShape.circle,
                ),
                child: const Icon(Icons.rocket_launch_rounded, color: AppTheme.primary, size: 24),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 8,
                      runSpacing: 4,
                      children: [
                        const Text(
                          'HACKATHON DEMO MODE',
                          style: TextStyle(
                            fontSize: 13,
                            fontWeight: FontWeight.w900,
                            letterSpacing: 0.8,
                            color: AppTheme.primary,
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: AppTheme.surfaceElevated,
                            borderRadius: BorderRadius.circular(6),
                            border: Border.all(color: AppTheme.border),
                          ),
                          child: const Text(
                            '10 STEPS',
                            style: TextStyle(fontSize: 9, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 3),
                    const Text(
                      'Run the complete end-to-end OS4All health-intelligence pipeline with live telemetry.',
                      style: TextStyle(fontSize: 12, color: AppTheme.textSecondary, height: 1.3),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              ElevatedButton(
                key: const ValueKey('launch_demo_banner_btn'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primary,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                ),
                onPressed: () => LaunchDemoModal.show(
                  context,
                  onDemoCompleted: () {
                    setState(() {
                      _activeState = HealthStatusState.anomaly;
                    });
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('Deterministic OS4All Demo pipeline completed successfully!'),
                        backgroundColor: AppTheme.primary,
                        duration: Duration(seconds: 3),
                      ),
                    );
                  },
                ),
                child: const Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.play_arrow_rounded, size: 18),
                    SizedBox(width: 4),
                    Text('Launch', style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold)),
                  ],
                ),
              ),
            ],
          );
        },
      ),
    );
  }

  // -------------------------------------------------------------
  // Header
  // -------------------------------------------------------------
  Widget _buildUserHeader() {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                DemoData.userFullName,
                style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                      fontWeight: FontWeight.w800,
                      color: AppTheme.textPrimary,
                      fontSize: 20,
                    ),
              ),
              const SizedBox(height: 2),
              const Text(
                'Deterministic Health Intelligence',
                style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, fontWeight: FontWeight.w500),
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
        const SizedBox(width: 8),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 5),
          decoration: BoxDecoration(
            color: AppTheme.surface,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppTheme.border),
            boxShadow: AppTheme.cardShadow,
          ),
          child: Row(
            children: [
              CircleAvatar(radius: 4, backgroundColor: _activeState.color),
              const SizedBox(width: 6),
              Text(
                _activeState.label,
                style: TextStyle(color: _activeState.color, fontSize: 11, fontWeight: FontWeight.w800),
              ),
            ],
          ),
        ),
      ],
    );
  }

  // -------------------------------------------------------------
  // 1. OS4All Health Status Card
  // -------------------------------------------------------------
  Widget _buildHealthStatusCardContent() {
    String stateTitle;
    String explanatoryMessage;

    switch (_activeState) {
      case HealthStatusState.stable:
        stateTitle = 'Individual Homeostasis Stable';
        explanatoryMessage =
            'All tracked continuous biometric parameters (resting heart rate, HRV, SpO2, sleep duration, and activity) remain consistent with your individual historical baseline intervals.';
        break;
      case HealthStatusState.drift:
        stateTitle = 'Early Signal Drift Detected';
        explanatoryMessage =
            'Your resting heart rate has remained above your personal baseline for 3 consecutive days, alongside a gradual 4-day reduction in nocturnal HRV.';
        break;
      case HealthStatusState.anomaly:
        stateTitle = 'Multi-Signal Perturbation Pattern';
        explanatoryMessage =
            'A compound physiological pattern has emerged across 4 days: resting heart rate is elevated (+14 bpm) while nocturnal HRV is suppressed (-19 ms) with restricted sleep.';
        break;
      case HealthStatusState.followUp:
        stateTitle = 'Scheduled Clinical Follow-up';
        explanatoryMessage =
            'Your annual cardiovascular and metabolic follow-up tests are approaching in 13 days to re-verify long-term baseline stability.';
        break;
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: _activeState.color.withValues(alpha: 0.4), width: 1.5),
        boxShadow: AppTheme.cardShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'OS4ALL HEALTH STATUS',
                  style: TextStyle(
                    color: AppTheme.textMuted,
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    letterSpacing: 1.1,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              const SizedBox(width: 8),
              HealthcareBadge(
                label: _activeState.label,
                color: _activeState.color,
              ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: _activeState.color.withValues(alpha: 0.12),
                  shape: BoxShape.circle,
                ),
                child: Icon(_activeState.icon, color: _activeState.color, size: 20),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  stateTitle,
                  style: const TextStyle(
                    color: AppTheme.textPrimary,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                    letterSpacing: -0.3,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            explanatoryMessage,
            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13.5, height: 1.45),
          ),
        ],
      ),
    );
  }

  Widget _buildHealthStatusSection() {
    return _buildHealthStatusCardContent();
  }

  // -------------------------------------------------------------
  // Visual Status Indicator Pill Switcher (STABLE, DRIFT, ANOMALY, FOLLOW-UP)
  // -------------------------------------------------------------
  Widget _buildVisualStateSelector() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 6),
      decoration: BoxDecoration(
        color: AppTheme.surfaceElevated,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.border),
      ),
      child: Row(
        children: HealthStatusState.values.map((state) {
          final isSelected = _activeState == state;
          return Expanded(
            child: GestureDetector(
              key: ValueKey('state_selector_${state.name}'),
              behavior: HitTestBehavior.opaque,
              onTap: () => setState(() => _activeState = state),
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 200),
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  color: isSelected ? AppTheme.surface : Colors.transparent,
                  borderRadius: BorderRadius.circular(8),
                  border: isSelected
                      ? Border.all(color: state.color.withValues(alpha: 0.5), width: 1.2)
                      : null,
                  boxShadow: isSelected ? AppTheme.cardShadow : null,
                ),
                child: Column(
                  children: [
                    Icon(
                      state.icon,
                      size: 15,
                      color: isSelected ? state.color : AppTheme.textMuted,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      state.label,
                      style: TextStyle(
                        fontSize: 10.5,
                        fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
                        color: isSelected ? AppTheme.textPrimary : AppTheme.textMuted,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  // -------------------------------------------------------------
  // 2. Personal Baseline Section
  // -------------------------------------------------------------
  Widget _buildPersonalBaselineSection() {
    return HealthcareCard(
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const BaselineScreen())),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(6),
                      decoration: BoxDecoration(
                        color: AppTheme.primary.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(Icons.tune_rounded, color: AppTheme.primary, size: 18),
                    ),
                    const SizedBox(width: 10),
                    const Expanded(
                      child: Text('Personal Baseline Engine',
                          style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700),
                          overflow: TextOverflow.ellipsis),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.arrow_forward_ios_rounded, color: AppTheme.textMuted, size: 14),
            ],
          ),
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
            decoration: BoxDecoration(
              color: AppTheme.surfaceElevated,
              borderRadius: BorderRadius.circular(8),
            ),
            child: const Text(
              '"Normal for the population is not necessarily normal for the individual."',
              style: TextStyle(
                  color: AppTheme.primary, fontSize: 12.5, fontWeight: FontWeight.w600, fontStyle: FontStyle.italic),
            ),
          ),
          const SizedBox(height: 12),
          const Text(
            'Calibrated across 42 days of continuous telemetry with 95% statistical envelopes.',
            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
          ),
          const SizedBox(height: 12),
          // Baseline Range Summary Badges
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: DemoData.personalBaselines.map((b) {
              return Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: AppTheme.background,
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: AppTheme.border),
                ),
                child: Text(
                  '${b['metric'].toString().split(' ')[0]}: ${b['range']}',
                  style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: AppTheme.textSecondary),
                ),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // 3. What Changed? (Recent Changes)
  // -------------------------------------------------------------
  Widget _buildRecentChangesSection() {
    List<Map<String, dynamic>> changes;
    if (_activeState == HealthStatusState.stable) {
      changes = [
        {
          'metric': 'Resting Heart Rate',
          'description': 'Averaged 61 bpm over the past 7 days (baseline mean: 60.4 bpm).',
          'trend': '0.6 bpm departure (Within normal variance)',
          'statusColor': AppTheme.stable,
        },
        {
          'metric': 'Nocturnal HRV (rMSSD)',
          'description': 'Sustained at 54 ms (baseline envelope: 46.0 – 64.0 ms).',
          'trend': 'Preserved parasympathetic tone',
          'statusColor': AppTheme.stable,
        },
        {
          'metric': 'Sleep Duration',
          'description': 'Sustained at 7.8 hours across consecutive evenings.',
          'trend': 'Normal circadian consistency',
          'statusColor': AppTheme.stable,
        },
      ];
    } else {
      changes = [
        {
          'metric': 'Resting Heart Rate',
          'description': 'Your resting heart rate has remained above your personal baseline for 5 days.',
          'trend': '+14 bpm departure (+2.4σ from personal mean)',
          'statusColor': AppTheme.warning,
        },
        {
          'metric': 'Nocturnal HRV (rMSSD)',
          'description': 'Decreased to 36 ms over the last 4 nights (baseline mean: 55.2 ms).',
          'trend': '-19 ms departure (-2.1σ below personal mean)',
          'statusColor': AppTheme.warning,
        },
        {
          'metric': 'Sleep Duration',
          'description': 'Reduced to 5.2 hours average over 4 consecutive evenings.',
          'trend': '2.8 hours below 8.0 hr personal baseline',
          'statusColor': AppTheme.danger,
        },
      ];
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(
              child: Text(
                'What Changed? (Recent Changes)',
                style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w800),
              ),
            ),
            const SizedBox(width: 8),
            HealthcareBadge(
              label: '${changes.length} SIGNALS',
              color: AppTheme.primary,
            ),
          ],
        ),
        const SizedBox(height: 10),
        ...changes.map((ch) {
          final color = ch['statusColor'] as Color;
          return Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.border),
                boxShadow: AppTheme.cardShadow,
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    margin: const EdgeInsets.only(top: 2),
                    width: 8,
                    height: 8,
                    decoration: BoxDecoration(color: color, shape: BoxShape.circle),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          ch['metric'] as String,
                          style: const TextStyle(
                              color: AppTheme.textPrimary, fontSize: 13.5, fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          ch['description'] as String,
                          style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12.5, height: 1.35),
                        ),
                        const SizedBox(height: 4),
                        Text(
                          ch['trend'] as String,
                          style: TextStyle(color: color, fontSize: 11.5, fontWeight: FontWeight.w600),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          );
        }),
      ],
    );
  }

  // -------------------------------------------------------------
  // 4. Signal Trends (Interactive Charts with Personal Baseline Ranges)
  // -------------------------------------------------------------
  Widget _buildSignalTrendsSection() {
    final telemetry = DemoData.signalTelemetry;
    final days = (telemetry['days'] as List<dynamic>).cast<String>();

    Map<String, dynamic> chartData;
    String metricTitle;
    Color lineColor;

    switch (_selectedMetricTab) {
      case 'restingHR':
        chartData = telemetry['restingHeartRate'] as Map<String, dynamic>;
        metricTitle = 'Resting Heart Rate';
        lineColor = AppTheme.primary;
        break;
      case 'hrv':
        chartData = telemetry['hrv'] as Map<String, dynamic>;
        metricTitle = 'Heart Rate Variability (rMSSD)';
        lineColor = AppTheme.secondary;
        break;
      case 'sleep':
        chartData = telemetry['sleep'] as Map<String, dynamic>;
        metricTitle = 'Sleep Duration';
        lineColor = const Color(0xFF6366F1);
        break;
      case 'spo2':
      default:
        chartData = telemetry['spo2'] as Map<String, dynamic>;
        metricTitle = 'Blood Oxygen (SpO2)';
        lineColor = AppTheme.stable;
        break;
    }

    // Adapt telemetry values if ANOMALY state is selected
    List<double> values = (chartData['values'] as List<dynamic>).map((e) => (e as num).toDouble()).toList();
    if (_activeState == HealthStatusState.anomaly || _activeState == HealthStatusState.drift) {
      if (_selectedMetricTab == 'restingHR') {
        values = [60.0, 62.0, 65.0, 69.0, 72.0, 74.0, 74.5];
      } else if (_selectedMetricTab == 'hrv') {
        values = [55.0, 52.0, 48.0, 43.0, 39.0, 37.0, 36.0];
      } else if (_selectedMetricTab == 'sleep') {
        values = [8.0, 7.8, 6.5, 5.8, 5.4, 5.2, 5.0];
      }
    }

    final mean = (chartData['mean'] as num).toDouble();
    final low = (chartData['low'] as num).toDouble();
    final high = (chartData['high'] as num).toDouble();
    final unit = chartData['unit'] as String;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            const Expanded(
              child: Text(
                'Signal Trends (With Baselines)',
                style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w800),
              ),
            ),
            IconButton(
              icon: const Icon(Icons.refresh_rounded, size: 18, color: AppTheme.textSecondary),
              tooltip: 'Simulate Reload',
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
              onPressed: () {
                setState(() => _isLoadingTelemetry = true);
                Future.delayed(const Duration(milliseconds: 600), () {
                  if (mounted) setState(() => _isLoadingTelemetry = false);
                });
              },
            ),
            const SizedBox(width: 8),
            TextButton(
              style: TextButton.styleFrom(
                padding: const EdgeInsets.symmetric(horizontal: 8),
                minimumSize: Size.zero,
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const VitalsScreen())),
              child: const Text('All Signals', style: TextStyle(color: AppTheme.primary, fontWeight: FontWeight.w700)),
            ),
          ],
        ),
        const SizedBox(height: 8),

        // Metric Tab Bar
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(
            children: [
              _buildMetricTabChip('restingHR', 'Resting HR', Icons.favorite_rounded),
              _buildMetricTabChip('hrv', 'HRV (rMSSD)', Icons.show_chart_rounded),
              _buildMetricTabChip('sleep', 'Sleep', Icons.bedtime_rounded),
              _buildMetricTabChip('spo2', 'SpO2', Icons.air_rounded),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // Beautiful Baseline Signal Chart
        BaselineSignalChart(
          metricName: metricTitle,
          unit: unit,
          values: values,
          labels: days,
          baselineMean: mean,
          baselineLow: low,
          baselineHigh: high,
          lineColor: lineColor,
          state: _activeState.label,
          isLoading: _isLoadingTelemetry,
          hasError: _hasTelemetryError,
          errorMessage: 'Sensor connection interrupted. Tap retry to reload telemetry.',
          onRetry: () => setState(() {
            _hasTelemetryError = false;
            _isLoadingTelemetry = false;
          }),
        ),
      ],
    );
  }

  Widget _buildMetricTabChip(String key, String label, IconData icon) {
    final isSelected = _selectedMetricTab == key;
    return Padding(
      padding: const EdgeInsets.only(right: 8),
      child: ChoiceChip(
        label: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 14, color: isSelected ? Colors.white : AppTheme.textSecondary),
            const SizedBox(width: 6),
            Text(label),
          ],
        ),
        selected: isSelected,
        onSelected: (_) => setState(() => _selectedMetricTab = key),
        selectedColor: AppTheme.primary,
        backgroundColor: AppTheme.surfaceLight,
        labelStyle: TextStyle(
          color: isSelected ? Colors.white : AppTheme.textSecondary,
          fontSize: 12,
          fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
        ),
        side: BorderSide(color: isSelected ? AppTheme.primary : AppTheme.border),
      ),
    );
  }

  // -------------------------------------------------------------
  // 5. Why did it change? & What does it mean? (AI Insight)
  // -------------------------------------------------------------
  Widget _buildAiInsightSection() {
    final insight = DemoData.latestAiInsight;

    String whyDidChange;
    String whatDoesItMean;

    if (_activeState == HealthStatusState.stable) {
      whyDidChange =
          'All tracked continuous biometric parameters remain firmly aligned with your individual historical baseline intervals. No active anomalies or directional drifts are present.';
      whatDoesItMean =
          'Your autonomic nervous system reflects optimal parasympathetic-sympathetic balance with effective physiological adaptation to current physical and daily activity.';
    } else {
      whyDidChange =
          'A multi-signal autonomic departure emerged over 4 days: cumulative nocturnal sleep loss coincided with elevated sympathetic cardiovascular tone, leading to higher resting pulse and suppressed HRV.';
      whatDoesItMean =
          'This pattern typically reflects cumulative physiological recovery debt or acute physical/environmental stress rather than an underlying medical condition. Continued monitoring will verify if metrics normalize after restorative rest.';
    }

    return HealthcareCard(
      borderColor: AppTheme.primary.withValues(alpha: 0.35),
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const InsightsScreen())),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.auto_awesome_rounded, color: AppTheme.primary, size: 18),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text('AI Health Intelligence Synthesis',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700),
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: 8),
              HealthcareBadge(
                label: _activeState == HealthStatusState.stable ? 'ROUTINE' : 'MONITOR',
                color: _activeState == HealthStatusState.stable ? AppTheme.stable : AppTheme.warning,
              ),
            ],
          ),
          const SizedBox(height: 14),

          // Why did it change?
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppTheme.surfaceLight,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.help_outline_rounded, size: 14, color: AppTheme.primary),
                    SizedBox(width: 6),
                    Text(
                      'Why did it change?',
                      style: TextStyle(color: AppTheme.textPrimary, fontSize: 12.5, fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  whyDidChange,
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12.5, height: 1.4),
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),

          // What does it mean?
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppTheme.surfaceLight,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.border),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.lightbulb_outline_rounded, size: 14, color: AppTheme.secondary),
                    SizedBox(width: 6),
                    Text(
                      'What does it mean?',
                      style: TextStyle(color: AppTheme.textPrimary, fontSize: 12.5, fontWeight: FontWeight.w700),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  whatDoesItMean,
                  style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12.5, height: 1.4),
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),

          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 6,
            children: [
              Text(
                'Confidence: ${(insight['confidence'] * 100).toInt()}% • Grounded on 42-day baseline',
                style: const TextStyle(color: AppTheme.textMuted, fontSize: 11.5, fontWeight: FontWeight.w500),
              ),
              const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text('Detailed report',
                      style: TextStyle(color: AppTheme.primary, fontSize: 12, fontWeight: FontWeight.w700)),
                  SizedBox(width: 2),
                  Icon(Icons.arrow_forward_rounded, color: AppTheme.primary, size: 13),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // 6. Supporting Evidence (Tavily Grounded Citations)
  // -------------------------------------------------------------
  Widget _buildEvidenceSection() {
    final evidenceList = (DemoData.latestAiInsight['evidence'] as List<dynamic>? ?? []);

    return HealthcareCard(
      borderColor: AppTheme.secondary.withValues(alpha: 0.35),
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const EvidenceScreen())),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppTheme.secondary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.menu_book_rounded, color: AppTheme.secondary, size: 18),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text('Supporting Evidence',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700),
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: 8),
              const HealthcareBadge(label: 'TAVILY GROUNDED', color: AppTheme.secondary),
            ],
          ),
          const SizedBox(height: 6),
          const Text(
            'Scientific studies cited to ground interpretation of detected physiological patterns:',
            style: TextStyle(color: AppTheme.textSecondary, fontSize: 12.5),
          ),
          const SizedBox(height: 10),
          ...evidenceList.take(2).map((item) {
            final e = item as Map<String, dynamic>;
            return Container(
              margin: const EdgeInsets.only(bottom: 8),
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: AppTheme.surfaceLight,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.border),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Icon(Icons.article_outlined, size: 16, color: AppTheme.primary),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          e['title'] as String,
                          style: const TextStyle(
                              color: AppTheme.textPrimary, fontSize: 12.5, fontWeight: FontWeight.w700),
                        ),
                        const SizedBox(height: 2),
                        Text(
                          e['source'] as String,
                          style: const TextStyle(color: AppTheme.primary, fontSize: 11, fontWeight: FontWeight.w600),
                        ),
                      ],
                    ),
                  ),
                  const Icon(Icons.open_in_new_rounded, size: 14, color: AppTheme.textMuted),
                ],
              ),
            );
          }),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // 7. What should I do next? (Recommended Next Step)
  // -------------------------------------------------------------
  Widget _buildRecommendedNextStepSection() {
    List<String> nextSteps;
    if (_activeState == HealthStatusState.stable) {
      nextSteps = [
        'Maintain current bedtime between 10:30 PM and 11:00 PM to support restorative sleep.',
        'Continue daily hydration and moderate aerobic training at your current volume.',
        'Keep wearing your biosensor continuously to sustain high statistical baseline confidence.',
      ];
    } else {
      nextSteps = [
        'Prioritize 8 hours of restorative sleep over the next 2-3 consecutive evenings.',
        'Temporarily substitute high-intensity training with active recovery until nocturnal HRV rebounds.',
        'Log lifestyle context (such as late meals, travel, or caffeine intake) to corroborate trend recovery.',
        'If resting pulse remains elevated for more than 5 days, review the trend report with your physician.',
      ];
    }

    return HealthcareCard(
      borderColor: AppTheme.stable.withValues(alpha: 0.4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppTheme.stable.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.directions_run_rounded, color: AppTheme.stable, size: 18),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text('What Should I Do Next?',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700),
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: 8),
              const HealthcareBadge(label: 'ACTION PLAN', color: AppTheme.stable),
            ],
          ),
          const SizedBox(height: 12),
          ...nextSteps.map((step) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('✓ ',
                        style: TextStyle(color: AppTheme.stable, fontSize: 14, fontWeight: FontWeight.bold)),
                    Expanded(
                      child: Text(
                        step,
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.35),
                      ),
                    ),
                  ],
                ),
              )),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // 8. Historical Timeline
  // -------------------------------------------------------------
  Widget _buildHealthTimelineSection() {
    return HealthcareCard(
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const TimelineScreen())),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppTheme.primary.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Icon(Icons.timeline_rounded, color: AppTheme.primary, size: 18),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text('Health Timeline',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700),
                    overflow: TextOverflow.ellipsis),
              ),
              const SizedBox(width: 8),
              Text('${DemoData.timelineItems.length} records',
                  style: const TextStyle(color: AppTheme.textMuted, fontSize: 12, fontWeight: FontWeight.w600)),
            ],
          ),
          const SizedBox(height: 12),
          ...DemoData.timelineItems.take(3).map((item) => Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(6),
                      decoration:
                          BoxDecoration(color: AppTheme.surfaceElevated, borderRadius: BorderRadius.circular(8)),
                      child: const Icon(Icons.circle, size: 7, color: AppTheme.primary),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(item['title'] as String,
                              style: const TextStyle(
                                  color: AppTheme.textPrimary, fontSize: 13, fontWeight: FontWeight.w600)),
                          Text('${item['timestamp']} • ${item['subtitle']}',
                              style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12)),
                        ],
                      ),
                    ),
                  ],
                ),
              )),
          const SizedBox(height: 6),
          const Align(
            alignment: Alignment.centerRight,
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Flexible(
                  child: Text(
                    'View full timeline history',
                    style: TextStyle(color: AppTheme.primary, fontSize: 12, fontWeight: FontWeight.w700),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                SizedBox(width: 4),
                Icon(Icons.arrow_forward_rounded, color: AppTheme.primary, size: 12),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // -------------------------------------------------------------
  // AI Counselor Prompt
  // -------------------------------------------------------------
  Widget _buildCounselorCard() {
    return HealthcareCard(
      onTap: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const CounselorScreen())),
      child: const Row(
        children: [
          CircleAvatar(
            backgroundColor: AppTheme.surfaceElevated,
            radius: 22,
            child: Icon(Icons.support_agent_rounded, color: AppTheme.primary),
          ),
          SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Ask AI Health Counselor',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                SizedBox(height: 2),
                Text('Get objective answers about your baseline changes and lab results.',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 12.5)),
              ],
            ),
          ),
          Icon(Icons.chevron_right_rounded, color: AppTheme.textMuted),
        ],
      ),
    );
  }
}
