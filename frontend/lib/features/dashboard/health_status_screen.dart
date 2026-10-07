import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class HealthStatusScreen extends StatelessWidget {
  const HealthStatusScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Health State Analysis')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
            HealthcareCard(
              borderColor: AppTheme.success.withValues(alpha: 0.5),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Transparent Anomaly Engine State', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, fontWeight: FontWeight.w600)),
                      HealthcareBadge(label: DemoData.currentHealthState, color: AppTheme.success),
                    ],
                  ),
                  SizedBox(height: 12),
                  Text('Individual Homeostasis: STABLE', style: TextStyle(color: AppTheme.textPrimary, fontSize: 20, fontWeight: FontWeight.w700)),
                  SizedBox(height: 8),
                  Text(DemoData.currentHealthSummary, style: TextStyle(color: AppTheme.textSecondary, fontSize: 14, height: 1.4)),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const Text('Rule-Based Engine State Matrix', style: TextStyle(color: AppTheme.textPrimary, fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 12),
            _buildStateCard(
              'STABLE',
              'Continuous signals are within personal normal baseline envelopes (within 1.5 standard deviations).',
              AppTheme.success,
              true,
            ),
            const SizedBox(height: 10),
            _buildStateCard(
              'DRIFT',
              'Subtle directional shift persisting for 3-5 days. Not an acute anomaly, but signals early departure from baseline.',
              AppTheme.warning,
              false,
            ),
            const SizedBox(height: 10),
            _buildStateCard(
              'ANOMALY',
              'Sustained statistical deviation (>2.0 standard deviations) or compound multi-signal autonomic strain.',
              AppTheme.danger,
              false,
            ),
            const SizedBox(height: 10),
            _buildStateCard(
              'FOLLOW_UP',
              'Transient deviation resolving back towards personal historical baseline.',
              AppTheme.info,
              false,
            ),
            const SizedBox(height: 24),
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.surfaceLight,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.cardBorder),
              ),
              child: const Text(
                'Non-Diagnostic Disclaimer: Statistical departure from an individual baseline describes a physiological variance. It does not equal a medical diagnosis. Consult a physician for diagnostic evaluation.',
                style: TextStyle(color: AppTheme.textMuted, fontSize: 12, height: 1.4),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStateCard(String title, String desc, Color color, bool isActive) {
    return HealthcareCard(
      borderColor: isActive ? color.withValues(alpha: 0.5) : AppTheme.cardBorder,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(8),
            decoration: BoxDecoration(color: color.withValues(alpha: 0.15), shape: BoxShape.circle),
            child: Icon(isActive ? Icons.check_circle : Icons.circle_outlined, color: color, size: 20),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(title, style: TextStyle(color: isActive ? color : AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                    if (isActive) HealthcareBadge(label: 'CURRENT STATE', color: color),
                  ],
                ),
                const SizedBox(height: 4),
                Text(desc, style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.35)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
