import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';

/// SCREEN 1 — HOME
/// Executive, calm, mobile-first overview of the OS4All Digital Twin.
/// Prioritizes: Twin State -> Risk -> Current Glucose -> 2-Hour Projection -> Drivers -> Live Signals -> Quick Actions.
class HomeScreen extends StatelessWidget {
  final Function(int tabIndex)? onNavigateToTab;

  const HomeScreen({super.key, this.onNavigateToTab});

  Color _getStateColor(String state) {
    switch (state.toUpperCase()) {
      case 'STABLE':
        return MobileTheme.stable;
      case 'PRE_SYMPTOMATIC_DRIFT':
        return MobileTheme.drift;
      case 'ELEVATED_RISK':
      case 'CRITICAL':
      case 'ACTIVE_ANOMALY':
        return MobileTheme.critical;
      default:
        return MobileTheme.drift;
    }
  }

  void _showPatientSwitcher(BuildContext context, DigitalTwinController controller) {
    showModalBottomSheet(
      context: context,
      backgroundColor: MobileTheme.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) {
        final patients = controller.patients;
        return SafeArea(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Select Cohort Patient',
                      style: GoogleFonts.inter(
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: MobileTheme.textPrimary,
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close_rounded, color: MobileTheme.textSecondary),
                      onPressed: () => Navigator.pop(ctx),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                if (patients.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    child: Text(
                      'Active Patient: Shara Senger (Synthea FHIR)',
                      style: GoogleFonts.inter(color: MobileTheme.textSecondary),
                    ),
                  )
                else
                  Flexible(
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: patients.length,
                      separatorBuilder: (_, __) => const Divider(color: MobileTheme.border, height: 1),
                      itemBuilder: (context, idx) {
                        final p = patients[idx] as Map<String, dynamic>;
                        final isSelected = p['name'] == controller.selectedPatient?['name'];
                        return ListTile(
                          contentPadding: EdgeInsets.zero,
                          title: Text(
                            p['name']?.toString() ?? 'Patient',
                            style: GoogleFonts.inter(
                              color: isSelected ? MobileTheme.primary : MobileTheme.textPrimary,
                              fontWeight: isSelected ? FontWeight.w700 : FontWeight.w500,
                            ),
                          ),
                          subtitle: Text(
                            '${p['age'] ?? 54} • ${p['gender'] ?? 'Female'} • ${p['condition'] ?? 'Type 2 Diabetes'}',
                            style: GoogleFonts.inter(color: MobileTheme.textSecondary, fontSize: 12),
                          ),
                          trailing: isSelected
                              ? const Icon(Icons.check_circle_rounded, color: MobileTheme.primary)
                              : null,
                          onTap: () {
                            controller.switchPatient(p);
                            Navigator.pop(ctx);
                          },
                        );
                      },
                    ),
                  ),
              ],
            ),
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    final controller = DigitalTwinController();

    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final stateColor = _getStateColor(controller.twinState);
        final patient = controller.selectedPatient ?? {};
        final trajectory = controller.trajectoryProjection;
        final points = trajectory['projectedTrajectoryPoints'] as List<dynamic>? ?? [];
        final factors = controller.contributingFactors.take(3).toList();
        final telem = controller.latestTelemetry ?? {};

        final patientName = patient['name']?.toString() ?? 'Shara Senger';
        final patientAge = (patient['age'] as num?)?.toInt() ?? 54;
        final patientGender = patient['gender']?.toString() ?? 'Female';
        final patientCondition = patient['condition']?.toString() ?? 'Type 2 Diabetes';

        return Scaffold(
          backgroundColor: MobileTheme.background,
          body: SafeArea(
            child: RefreshIndicator(
              onRefresh: () => controller.refreshAll(),
              color: MobileTheme.primary,
              backgroundColor: MobileTheme.surface,
              child: SingleChildScrollView(
                physics: const AlwaysScrollableScrollPhysics(),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // 1. Top Header Bar
                    OS4Header(
                      trailing: IconButton(
                        icon: const Icon(Icons.refresh_rounded, color: MobileTheme.textSecondary),
                        tooltip: 'Refresh Twin',
                        constraints: const BoxConstraints(minWidth: 48, minHeight: 48),
                        onPressed: () => controller.refreshAll(),
                      ),
                    ),
                    const SizedBox(height: 12),

                    // 2. Patient Identity Card
                    PatientIdentityCard(
                      name: patientName,
                      age: patientAge,
                      gender: patientGender,
                      condition: patientCondition,
                      onSwitchPatient: () => _showPatientSwitcher(context, controller),
                    ),
                    const SizedBox(height: 14),

                    // 3. Digital Twin Status Card
                    TwinStatusCard(
                      state: controller.twinState,
                      riskScore: controller.riskScore,
                      currentGlucose: controller.currentGlucose,
                      glucoseVelocity: controller.glucoseVelocity,
                      trajectoryDirection: controller.trajectoryDirection,
                      stateColor: stateColor,
                    ),
                    const SizedBox(height: 14),

                    // 4. 2-Hour Outlook
                    ProjectionCard(
                      trajectory: trajectory,
                      points: points,
                      stateColor: stateColor,
                      onExplore: () => onNavigateToTab?.call(1),
                    ),
                    const SizedBox(height: 16),

                    // 5. What is Driving the Change? (Top 3 Factors)
                    _buildDriversSection(factors, stateColor),
                    const SizedBox(height: 16),

                    // 6. Live Signals (2-Column Adaptive Grid)
                    _buildLiveSignalsSection(telem, controller),
                    const SizedBox(height: 18),

                    // 7. Quick Actions (Exactly 3)
                    _buildQuickActions(context),
                    const SizedBox(height: 16),

                    // 8. Bottom Disclaimer Banner
                    const PrototypeDisclaimerBanner(),
                    const SizedBox(height: 20),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildDriversSection(List<dynamic> factors, Color stateColor) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'WHAT IS DRIVING THE CHANGE?',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 8),
        Container(
          width: double.infinity,
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: MobileTheme.surface,
            borderRadius: MobileTheme.cardRadius,
            border: Border.all(color: MobileTheme.border, width: 1),
          ),
          child: Column(
            children: factors.map<Widget>((f) {
              final factorMap = f as Map<String, dynamic>;
              final title = factorMap['factor']?.toString() ?? 'Physiological Factor';
              final detail = factorMap['detail']?.toString() ?? '';
              final isIncreasing = factorMap['direction'] == 'RISK_INCREASING';

              return Padding(
                padding: const EdgeInsets.symmetric(vertical: 6),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      width: 22,
                      height: 22,
                      margin: const EdgeInsets.only(top: 2),
                      decoration: BoxDecoration(
                        color: isIncreasing ? MobileTheme.critical.withOpacity(0.15) : MobileTheme.stable.withOpacity(0.15),
                        shape: BoxShape.circle,
                      ),
                      child: Icon(
                        isIncreasing ? Icons.arrow_upward_rounded : Icons.check_rounded,
                        color: isIncreasing ? MobileTheme.critical : MobileTheme.stable,
                        size: 13,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            title,
                            style: GoogleFonts.inter(
                              fontSize: 13,
                              fontWeight: FontWeight.w700,
                              color: MobileTheme.textPrimary,
                            ),
                          ),
                          if (detail.isNotEmpty) ...[
                            const SizedBox(height: 2),
                            Text(
                              detail,
                              style: GoogleFonts.inter(
                                fontSize: 11,
                                color: MobileTheme.textSecondary,
                                height: 1.35,
                              ),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ],
                ),
              );
            }).toList(),
          ),
        ),
      ],
    );
  }

  Widget _buildLiveSignalsSection(Map<String, dynamic> telem, DigitalTwinController controller) {
    final glucose = (telem['glucose'] as num?)?.toDouble() ?? controller.currentGlucose;
    final hr = (telem['restingHeartRate'] as num?)?.toInt() ?? 78;
    final hrv = (telem['heartRateVariability'] as num?)?.toDouble() ?? 42.0;
    final sleep = (telem['sleepHours'] as num?)?.toDouble() ?? 5.8;
    final steps = (telem['dailySteps'] as num?)?.toInt() ?? 2850;
    final activity = telem['activityLevel']?.toString() ?? 'Sedentary';

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'LIVE SIGNALS',
              style: GoogleFonts.inter(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                color: MobileTheme.textPrimary,
                letterSpacing: 0.5,
              ),
            ),
            Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  width: 6,
                  height: 6,
                  decoration: const BoxDecoration(
                    color: MobileTheme.stable,
                    shape: BoxShape.circle,
                  ),
                ),
                const SizedBox(width: 5),
                Text(
                  controller.isLiveStreaming ? 'Streaming' : 'Paused',
                  style: GoogleFonts.inter(
                    fontSize: 10.5,
                    fontWeight: FontWeight.w600,
                    color: MobileTheme.stable,
                  ),
                ),
              ],
            ),
          ],
        ),
        const SizedBox(height: 8),
        LayoutBuilder(
          builder: (context, constraints) {
            final cardWidth = (constraints.maxWidth - 10) / 2;
            return Wrap(
              spacing: 10,
              runSpacing: 10,
              children: [
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Glucose',
                    value: glucose.toStringAsFixed(0),
                    unit: 'mg/dL',
                    icon: Icons.water_drop_rounded,
                    color: MobileTheme.primary,
                    subtitle: 'CGM stream',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Heart Rate',
                    value: '$hr',
                    unit: 'BPM',
                    icon: Icons.favorite_rounded,
                    color: MobileTheme.critical,
                    subtitle: 'Resting pulse',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'HRV (RMSSD)',
                    value: hrv.toStringAsFixed(0),
                    unit: 'ms',
                    icon: Icons.monitor_heart_rounded,
                    color: MobileTheme.secondaryAccent,
                    subtitle: 'Autonomic tone',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Sleep',
                    value: sleep.toStringAsFixed(1),
                    unit: 'hrs',
                    icon: Icons.bedtime_rounded,
                    color: MobileTheme.primary,
                    subtitle: '7.4h target',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Steps',
                    value: '$steps',
                    unit: 'steps',
                    icon: Icons.directions_walk_rounded,
                    color: MobileTheme.stable,
                    subtitle: 'Daily activity',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Metabolic State',
                    value: activity.split(' ').first,
                    unit: '',
                    icon: Icons.bolt_rounded,
                    color: MobileTheme.drift,
                    subtitle: 'Current posture',
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Widget _buildQuickActions(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'EXPLORE ACTIONS',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
        ),
        const SizedBox(height: 8),
        QuickActionCard(
          title: 'Explore Digital Twin',
          subtitle: 'Simulate counterfactual scenarios and trajectory shifts',
          icon: Icons.hub_rounded,
          color: MobileTheme.primary,
          onTap: () => onNavigateToTab?.call(1),
        ),
        const SizedBox(height: 8),
        QuickActionCard(
          title: 'Live Data & Health Connect',
          subtitle: 'Sync physical device signals and monitor continuous telemetry',
          icon: Icons.sensors_rounded,
          color: MobileTheme.stable,
          onTap: () => onNavigateToTab?.call(2),
        ),
        const SizedBox(height: 8),
        QuickActionCard(
          title: 'Ask Digital Twin AI',
          subtitle: 'Gemini clinical explanations grounded in deterministic math',
          icon: Icons.auto_awesome_rounded,
          color: MobileTheme.geminiPurple,
          onTap: () => onNavigateToTab?.call(3),
        ),
      ],
    );
  }
}
