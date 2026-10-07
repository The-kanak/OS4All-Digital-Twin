import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class BaselineScreen extends StatelessWidget {
  const BaselineScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Personal Baseline Engine')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
            HealthcareCard(
              borderColor: AppTheme.cyan.withValues(alpha: 0.4),
              child: const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('Core Mathematical Assumption', style: TextStyle(color: AppTheme.cyan, fontSize: 13, fontWeight: FontWeight.w700)),
                  SizedBox(height: 6),
                  Text(
                    '"Normal for the population is not necessarily normal for the individual."',
                    style: TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w700, fontStyle: FontStyle.italic),
                  ),
                  SizedBox(height: 8),
                  Text(
                    'Baselines require >= 14 observation days before statistical reliability is declared. We calculate mean, standard deviation, and personalized 95% confidence envelopes.',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.4),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 20),
            const Text('Established Continuous Baselines', style: TextStyle(color: AppTheme.textPrimary, fontSize: 18, fontWeight: FontWeight.w700)),
            const SizedBox(height: 12),
            ...DemoData.personalBaselines.map((b) => Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: HealthcareCard(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(b['metric'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 16, fontWeight: FontWeight.w700)),
                            const HealthcareBadge(label: 'ESTABLISHED', color: AppTheme.success),
                          ],
                        ),
                        const SizedBox(height: 14),
                        Row(
                          children: [
                            _buildStatItem('Personal Mean', b['mean']),
                            _buildStatItem('Std Deviation', b['stdDev']),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            _buildStatItem('Normal Envelope', b['range']),
                            _buildStatItem('Observations', '${b['count']} days'),
                          ],
                        ),
                      ],
                    ),
                  ),
                )),
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem(String label, String value) {
    return Expanded(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: const TextStyle(color: AppTheme.textMuted, fontSize: 12)),
          const SizedBox(height: 2),
          Text(value, style: const TextStyle(color: AppTheme.textPrimary, fontSize: 14, fontWeight: FontWeight.w700)),
        ],
      ),
    );
  }
}
