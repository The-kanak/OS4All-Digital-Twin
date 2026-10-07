import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class VitalsScreen extends StatelessWidget {
  const VitalsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Continuous Vitals & Signals')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
          ...DemoData.keyMetrics.map((m) {
            return Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: HealthcareCard(
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.surfaceLight,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(Icons.monitor_heart, color: AppTheme.cyan, size: 24),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(m['metric'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                        const SizedBox(height: 2),
                        Text('Historical Baseline: ${m['baseline']}', style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                        const SizedBox(height: 2),
                        Text('Deviation: ${m['deviation']} (${m['trend']})', style: const TextStyle(color: AppTheme.cyanMuted, fontSize: 12, fontWeight: FontWeight.w600)),
                      ],
                    ),
                  ),
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Row(
                        crossAxisAlignment: CrossAxisAlignment.baseline,
                        textBaseline: TextBaseline.alphabetic,
                        children: [
                          Text(m['value'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 20, fontWeight: FontWeight.w800)),
                          const SizedBox(width: 3),
                          Text(m['unit'], style: const TextStyle(color: AppTheme.textMuted, fontSize: 11)),
                        ],
                      ),
                      const SizedBox(height: 4),
                      HealthcareBadge(label: m['status'], color: AppTheme.success),
                    ],
                  ),
                ],
              ),
            ),
          );
        }),
      ],
    ),
  );
}
}
