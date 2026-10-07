import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../core/theme/mobile_design_system.dart';
import '../../shared/widgets/os4_components.dart';
import '../digitaltwin/digital_twin_controller.dart';

/// DEMO MODE SCREEN
/// Guided 6-Step Competition Walkthrough for Hackathon Judges.
/// Step 1: Meet the Virtual Patient
/// Step 2: View the Baseline
/// Step 3: Inject a Scenario
/// Step 4: Watch the Trajectory Change
/// Step 5: Ask Gemini Why
/// Step 6: Show Live/Synthetic Signals
class DemoModeScreen extends StatefulWidget {
  const DemoModeScreen({super.key});

  @override
  State<DemoModeScreen> createState() => _DemoModeScreenState();
}

class _DemoModeScreenState extends State<DemoModeScreen> {
  int _currentStep = 0;

  final List<Map<String, dynamic>> _steps = [
    {
      'title': 'Meet the Virtual Patient',
      'tag': 'Synthea FHIR',
      'icon': Icons.person_rounded,
      'color': MobileTheme.primary,
      'headline': 'Real Clinical Baseline: Shara Senger',
      'body':
          'Synthea FHIR patient record ingested with confirmed Type 2 Diabetes (SNOMED 44054006). Baseline HbA1c is 8.2% with a resting metabolic rate of 118 mg/dL.',
      'metric': 'Shara Senger • 54yo • T2D',
      'submetric': 'Synthea EHR ID: P-9821',
    },
    {
      'title': 'View the Baseline',
      'tag': 'Homeostasis',
      'icon': Icons.home_rounded,
      'color': MobileTheme.stable,
      'headline': 'Personal Homeostatic Equilibrium Established',
      'body':
          'Deterministic baseline parameters model hepatic glucose production vs basal insulin clearance. Patient operates within a personal euglycemic target band of 70–140 mg/dL.',
      'metric': '118 mg/dL Resting Baseline',
      'submetric': 'Risk Score: 18.5 / 100 (STABLE)',
    },
    {
      'title': 'Inject a Scenario',
      'tag': 'Counterfactual',
      'icon': Icons.restaurant_rounded,
      'color': MobileTheme.critical,
      'headline': 'High Carbohydrate Meal Ingestion',
      'body':
          'Injecting an acute postprandial dietary influx. Systemic glucose rate of change inflects to +2.40 mg/dL/min, triggering a state transition to ELEVATED RISK.',
      'metric': '+2.40 mg/dL/min Flux Velocity',
      'submetric': 'Current Sensor: 162 mg/dL',
    },
    {
      'title': 'Watch the Trajectory Change',
      'tag': '2-Hour RK4',
      'icon': Icons.auto_graph_rounded,
      'color': MobileTheme.drift,
      'headline': 'Deterministic 2-Hour Outlook Divergence',
      'body':
          'The Runge-Kutta 4 trajectory engine projects systemic glucose will rise to 332.5 mg/dL (+170.5 mg/dL) at T+120 minutes without corrective physical activity or insulin.',
      'metric': '332.5 mg/dL at 120 Minutes',
      'submetric': 'Risk Score: 76.4 / 100 (ELEVATED RISK)',
    },
    {
      'title': 'Ask Gemini Why',
      'tag': 'Grounded AI',
      'icon': Icons.auto_awesome_rounded,
      'color': MobileTheme.geminiPurple,
      'headline': 'Gemini Synthesizes Deterministic Math',
      'body':
          'Google Gemini consumes the authoritative twin outputs and generates a clinical explanation: "Rapid carbohydrate influx coupled with sedentary posture causes early spike trajectory."',
      'metric': 'Grounded in Deterministic Model',
      'submetric': 'Clinical Headline & Risk Factors Generated',
    },
    {
      'title': 'Show Live & Synthetic Signals',
      'tag': 'IoT Bridge',
      'icon': Icons.sensors_rounded,
      'color': MobileTheme.primary,
      'headline': 'Android Health Connect & Synthetic Streaming',
      'body':
          'Dual ingestion architecture: Physical device telemetry via Android Health Platform bridge alongside continuous 5-second synthetic telemetry for automated simulation.',
      'metric': 'Health Connect + IoT Stream',
      'submetric': 'Glucose • Heart Rate • HRV • Steps • Sleep',
    },
  ];

  void _nextStep(DigitalTwinController controller) {
    if (_currentStep == 2) {
      // Step 3 triggers High Carb Meal scenario in controller
      controller.injectScenario('HIGH_CARB_MEAL');
    }

    if (_currentStep < _steps.length - 1) {
      setState(() => _currentStep++);
    } else {
      // Completed walkthrough
      Navigator.of(context).pop();
    }
  }

  void _previousStep() {
    if (_currentStep > 0) {
      setState(() => _currentStep--);
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = DigitalTwinController();
    final step = _steps[_currentStep];
    final color = step['color'] as Color;
    final isLast = _currentStep == _steps.length - 1;

    return Scaffold(
      backgroundColor: MobileTheme.background,
      appBar: AppBar(
        backgroundColor: MobileTheme.surface,
        elevation: 0,
        leading: IconButton(
          icon: const Icon(Icons.close_rounded, color: MobileTheme.textPrimary),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'Live Demo Mode',
          style: GoogleFonts.inter(
            color: MobileTheme.textPrimary,
            fontSize: 17,
            fontWeight: FontWeight.w700,
          ),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Center(
              child: Text(
                'Step ${_currentStep + 1} of ${_steps.length}',
                style: GoogleFonts.inter(
                  color: MobileTheme.primary,
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
        ],
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Progress Bar
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: (_currentStep + 1) / _steps.length,
                  backgroundColor: MobileTheme.surfaceElevated,
                  valueColor: AlwaysStoppedAnimation<Color>(color),
                  minHeight: 6,
                ),
              ),
              const SizedBox(height: 20),

              // Step Tag & Icon Header
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  StatusPill(label: step['tag'] as String, color: color),
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: color.withOpacity(0.15),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(step['icon'] as IconData, color: color, size: 22),
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Title & Headline
              Text(
                step['title'] as String,
                style: GoogleFonts.inter(
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                  color: MobileTheme.textSecondary,
                  letterSpacing: 0.5,
                ),
              ),
              const SizedBox(height: 4),
              Text(
                step['headline'] as String,
                style: GoogleFonts.inter(
                  fontSize: 20,
                  fontWeight: FontWeight.w800,
                  color: MobileTheme.textPrimary,
                  letterSpacing: -0.3,
                  height: 1.25,
                ),
              ),
              const SizedBox(height: 16),

              // Narrative Body Card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: MobileTheme.surface,
                  borderRadius: MobileTheme.cardRadius,
                  border: Border.all(color: MobileTheme.border),
                ),
                child: Text(
                  step['body'] as String,
                  style: GoogleFonts.inter(
                    fontSize: 14,
                    fontWeight: FontWeight.w400,
                    color: MobileTheme.textPrimary,
                    height: 1.5,
                  ),
                ),
              ),
              const SizedBox(height: 16),

              // Key Output Metric Card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: MobileTheme.surfaceElevated,
                  borderRadius: MobileTheme.cardRadius,
                  border: Border.all(color: color.withOpacity(0.4)),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      step['metric'] as String,
                      style: GoogleFonts.inter(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                        color: color,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      step['submetric'] as String,
                      style: GoogleFonts.inter(
                        fontSize: 12,
                        color: MobileTheme.textSecondary,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),

              const Spacer(),

              // Single Obvious CTA: "Continue" / "Complete Demo"
              PrimaryButton(
                label: isLast ? 'Complete Demo Walkthrough' : 'Continue',
                icon: isLast ? Icons.check_circle_rounded : Icons.arrow_forward_rounded,
                onPressed: () => _nextStep(controller),
              ),
              if (_currentStep > 0) ...[
                const SizedBox(height: 10),
                Center(
                  child: TextButton(
                    onPressed: _previousStep,
                    child: Text(
                      'Back to Previous Step',
                      style: GoogleFonts.inter(
                        fontSize: 13,
                        color: MobileTheme.textSecondary,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
