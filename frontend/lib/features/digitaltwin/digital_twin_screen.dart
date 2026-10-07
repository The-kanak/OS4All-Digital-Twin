import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import 'digital_twin_controller.dart';

/// SCREEN 2 — TWIN (HERO SCREEN)
/// "Your Digital Twin"
/// Virtual representation of the patient's current physiological state.
/// Prioritizes: Virtual Patient -> Current State -> Current Physiology -> 2-Hour Trajectory -> Drivers -> Scenarios -> Advanced.
class DigitalTwinScreen extends StatefulWidget {
  const DigitalTwinScreen({super.key});

  @override
  State<DigitalTwinScreen> createState() => _DigitalTwinScreenState();
}

class _DigitalTwinScreenState extends State<DigitalTwinScreen> {
  bool _showAdvancedParams = false;

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
        final factors = controller.contributingFactors;
        final telem = controller.latestTelemetry ?? {};

        final patientName = patient['name']?.toString() ?? 'Shara Senger';
        final patientAge = (patient['age'] as num?)?.toInt() ?? 54;
        final patientGender = patient['gender']?.toString() ?? 'Female';
        final patientCondition = patient['condition']?.toString() ?? 'Type 2 Diabetes';

        return Scaffold(
          backgroundColor: MobileTheme.background,
          body: SafeArea(
            child: SingleChildScrollView(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // 1. Header
                  _buildHeader(),
                  const SizedBox(height: 12),

                  // 2. Virtual Patient Identity Header
                  PatientIdentityCard(
                    name: patientName,
                    age: patientAge,
                    gender: patientGender,
                    condition: patientCondition,
                  ),
                  const SizedBox(height: 14),

                  // 3. Large State Visualization Card
                  _buildStateVisualizationCard(controller, stateColor),
                  const SizedBox(height: 16),

                  // 4. Current Physiology
                  _buildCurrentPhysiologySection(controller, telem),
                  const SizedBox(height: 16),

                  // 5. 2-Hour Trajectory Graph
                  TrajectoryChartCard(
                    points: points,
                    currentGlucose: controller.currentGlucose,
                    trajectoryDirection: controller.trajectoryDirection,
                    stateColor: stateColor,
                  ),
                  const SizedBox(height: 10),

                  // Direction and Summary
                  _buildTrajectorySummaryBanner(controller, stateColor),
                  const SizedBox(height: 16),

                  // 6. Why? Contributing Signals
                  _buildWhySection(factors),
                  const SizedBox(height: 18),

                  // 7. Counterfactual Simulation
                  _buildCounterfactualSection(controller),
                  const SizedBox(height: 16),

                  // 8. Collapsible Advanced Simulation
                  _buildAdvancedSection(controller),
                  const SizedBox(height: 16),

                  // Bottom Disclaimer
                  const PrototypeDisclaimerBanner(),
                  const SizedBox(height: 20),
                ],
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: 8,
          runSpacing: 4,
          children: [
            Text(
              'Your Digital Twin',
              style: GoogleFonts.inter(
                fontSize: 22,
                fontWeight: FontWeight.w800,
                color: MobileTheme.textPrimary,
                letterSpacing: -0.4,
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: MobileTheme.primary.withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: MobileTheme.primary.withOpacity(0.4)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.bolt_rounded, size: 13, color: MobileTheme.primary),
                  const SizedBox(width: 4),
                  Text(
                    'Active Model',
                    style: GoogleFonts.inter(
                      fontSize: 10.5,
                      fontWeight: FontWeight.w700,
                      color: MobileTheme.primary,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
        const SizedBox(height: 2),
        Text(
          'Virtual representation of the patient\'s current physiological state.',
          style: GoogleFonts.inter(
            fontSize: 12,
            color: MobileTheme.textSecondary,
          ),
        ),
      ],
    );
  }

  Widget _buildStateVisualizationCard(DigitalTwinController controller, Color stateColor) {
    final stateTitle = controller.twinState.replaceAll('_', ' ');
    String stateDescription = 'Metabolic homeostasis maintained within standard targets.';
    if (controller.twinState == 'PRE_SYMPTOMATIC_DRIFT') {
      stateDescription = 'Directional departure from personal resting baseline detected.';
    } else if (controller.twinState == 'ELEVATED_RISK') {
      stateDescription = 'Postprandial influx accelerating glucose toward upper threshold.';
    } else if (controller.twinState == 'CRITICAL') {
      stateDescription = 'Acute metabolic anomaly projected within the next 2 hours.';
    }

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: stateColor.withOpacity(0.6), width: 1.5),
        boxShadow: MobileTheme.subtleShadow,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Text(
                'CURRENT PHYSIOLOGICAL STATE',
                style: GoogleFonts.inter(
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textSecondary,
                  letterSpacing: 0.6,
                ),
              ),
              StatusPill(label: stateTitle, color: stateColor),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            stateTitle,
            style: GoogleFonts.inter(
              fontSize: 22,
              fontWeight: FontWeight.w900,
              color: stateColor,
              letterSpacing: -0.4,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            stateDescription,
            style: GoogleFonts.inter(
              fontSize: 13,
              fontWeight: FontWeight.w400,
              color: MobileTheme.textPrimary,
              height: 1.35,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCurrentPhysiologySection(DigitalTwinController controller, Map<String, dynamic> telem) {
    final glucose = (telem['glucose'] as num?)?.toDouble() ?? controller.currentGlucose;
    final velocity = (telem['glucoseVelocity'] as num?)?.toDouble() ?? controller.glucoseVelocity;
    final hr = (telem['restingHeartRate'] as num?)?.toInt() ?? 78;
    final hrv = (telem['heartRateVariability'] as num?)?.toDouble() ?? 42.0;
    final sleep = (telem['sleepHours'] as num?)?.toDouble() ?? 5.8;
    final steps = (telem['dailySteps'] as num?)?.toInt() ?? 2850;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'CURRENT PHYSIOLOGY',
          style: GoogleFonts.inter(
            fontSize: 12,
            fontWeight: FontWeight.w800,
            color: MobileTheme.textPrimary,
            letterSpacing: 0.5,
          ),
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
                    label: 'Current Glucose',
                    value: glucose.toStringAsFixed(0),
                    unit: 'mg/dL',
                    icon: Icons.water_drop_rounded,
                    color: MobileTheme.primary,
                    subtitle: 'Sensor reading',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Glucose Velocity',
                    value: '${velocity >= 0 ? '+' : ''}${velocity.toStringAsFixed(2)}',
                    unit: 'mg/dL/min',
                    icon: Icons.speed_rounded,
                    color: velocity.abs() > 1.5 ? MobileTheme.critical : MobileTheme.drift,
                    subtitle: 'Flux rate of change',
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
                    subtitle: 'Autonomic recovery',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Sleep Recovery',
                    value: sleep.toStringAsFixed(1),
                    unit: 'hrs',
                    icon: Icons.bedtime_rounded,
                    color: MobileTheme.primary,
                    subtitle: 'Nocturnal duration',
                  ),
                ),
                SizedBox(
                  width: cardWidth,
                  child: SignalCard(
                    label: 'Daily Activity',
                    value: '$steps',
                    unit: 'steps',
                    icon: Icons.directions_walk_rounded,
                    color: MobileTheme.stable,
                    subtitle: 'Accumulated movement',
                  ),
                ),
              ],
            );
          },
        ),
      ],
    );
  }

  Widget _buildTrajectorySummaryBanner(DigitalTwinController controller, Color stateColor) {
    final trajectory = controller.trajectoryProjection;
    final projected = trajectory['projectedGlucose120Min']?.toString() ?? '332.5';
    final delta = trajectory['projectedDelta120Min'] != null
        ? ((trajectory['projectedDelta120Min'] as num) >= 0 ? '+' : '') +
            (trajectory['projectedDelta120Min'] as num).toStringAsFixed(1)
        : '+170.5';
    final dir = controller.trajectoryDirection;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: MobileTheme.border),
      ),
      child: Wrap(
        alignment: WrapAlignment.spaceBetween,
        crossAxisAlignment: WrapCrossAlignment.center,
        spacing: 8,
        runSpacing: 4,
        children: [
          Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Direction: ',
                style: GoogleFonts.inter(
                  fontSize: 12,
                  color: MobileTheme.textSecondary,
                  fontWeight: FontWeight.w500,
                ),
              ),
              Text(
                dir,
                style: GoogleFonts.inter(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w800,
                  color: stateColor,
                ),
              ),
            ],
          ),
          Text(
            'Target: $projected mg/dL ($delta)',
            style: GoogleFonts.inter(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: MobileTheme.textPrimary,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildWhySection(List<dynamic> factors) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          'WHY IS THE TWIN CHANGING?',
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
                    Icon(
                      isIncreasing ? Icons.trending_up_rounded : Icons.check_circle_outline_rounded,
                      color: isIncreasing ? MobileTheme.critical : MobileTheme.stable,
                      size: 18,
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

  Widget _buildCounterfactualSection(DigitalTwinController controller) {
    final active = controller.activeScenario;

    final scenarios = [
      {
        'key': 'BASELINE',
        'title': 'Baseline',
        'subtitle': 'Normal circadian clearance',
        'icon': Icons.home_rounded,
      },
      {
        'key': 'MISSED_INSULIN',
        'title': 'Missed Insulin',
        'subtitle': 'Omitted basal dose drift',
        'icon': Icons.medication_liquid_rounded,
      },
      {
        'key': 'HIGH_CARB_MEAL',
        'title': 'High Carb Meal',
        'subtitle': 'Acute postprandial glucose surge',
        'icon': Icons.restaurant_rounded,
      },
      {
        'key': 'EXERCISE',
        'title': 'Exercise',
        'subtitle': 'Insulin-independent GLUT4 clearance',
        'icon': Icons.fitness_center_rounded,
      },
      {
        'key': 'STRESS',
        'title': 'Stress Event',
        'subtitle': 'Autonomic counter-regulatory surge',
        'icon': Icons.warning_amber_rounded,
      },
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          spacing: 8,
          runSpacing: 4,
          children: [
            Text(
              'EXPLORE A SCENARIO',
              style: GoogleFonts.inter(
                fontSize: 12,
                fontWeight: FontWeight.w800,
                color: MobileTheme.textPrimary,
                letterSpacing: 0.5,
              ),
            ),
            TextButton.icon(
              onPressed: () => controller.resetScenario(),
              icon: const Icon(Icons.restart_alt_rounded, size: 16, color: MobileTheme.primary),
              label: Text(
                'Reset Scenario',
                style: GoogleFonts.inter(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: MobileTheme.primary,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 4),
        Text(
          'Inject a simulated physiological counterfactual to see how the trajectory inflects.',
          style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textSecondary),
        ),
        const SizedBox(height: 12),

        // Vertical stack of 5 scenario cards with >=48dp touch targets
        Column(
          children: scenarios.map((sc) {
            final title = sc['title'] as String;
            final subtitle = sc['subtitle'] as String;
            final key = sc['key'] as String;
            final icon = sc['icon'] as IconData;
            final isSelected = active.toLowerCase().contains(title.toLowerCase());
            return Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: ScenarioCard(
                title: title,
                subtitle: subtitle,
                icon: icon,
                isSelected: isSelected,
                onSelect: () => controller.injectScenario(key),
              ),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _buildAdvancedSection(DigitalTwinController controller) {
    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        color: MobileTheme.surface,
        borderRadius: MobileTheme.cardRadius,
        border: Border.all(color: MobileTheme.border),
      ),
      child: Material(
        color: Colors.transparent,
        child: Column(
          children: [
            ListTile(
              title: Text(
                'Advanced Simulation Parameters',
                style: GoogleFonts.inter(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: MobileTheme.textPrimary,
                ),
              ),
              subtitle: Text(
                'Underlying physiological coefficients (RK4)',
                style: GoogleFonts.inter(fontSize: 11, color: MobileTheme.textSecondary),
              ),
              trailing: Icon(
                _showAdvancedParams ? Icons.expand_less_rounded : Icons.expand_more_rounded,
                color: MobileTheme.textSecondary,
              ),
              onTap: () {
                setState(() {
                  _showAdvancedParams = !_showAdvancedParams;
                });
              },
            ),
          if (_showAdvancedParams) ...[
            const Divider(color: MobileTheme.border, height: 1),
            Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                children: [
                  _buildParamRow('Insulin Sensitivity Factor (S_I)', '45.0 mg/dL/U'),
                  _buildParamRow('Glucose Clearance Decay (k_decay)', '0.014 min⁻¹'),
                  _buildParamRow('Basal Endogenous Production (EGP)', '2.2 mg/kg/min'),
                  _buildParamRow('Carbohydrate Ratio (C_R)', '12.0 g/U'),
                  _buildParamRow('Active Scenario Coefficient', controller.activeScenario),
                ],
              ),
            ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildParamRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Flexible(
            child: Text(
              label,
              style: GoogleFonts.inter(fontSize: 11.5, color: MobileTheme.textSecondary),
              overflow: TextOverflow.ellipsis,
            ),
          ),
          Text(
            value,
            style: GoogleFonts.inter(
              fontSize: 12,
              fontWeight: FontWeight.w700,
              color: MobileTheme.primary,
            ),
          ),
        ],
      ),
    );
  }
}
