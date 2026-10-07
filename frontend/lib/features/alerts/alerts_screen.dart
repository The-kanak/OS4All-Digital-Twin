import 'package:flutter/material.dart';
import '../../core/theme/app_theme.dart';
import '../../core/utils/demo_data.dart';
import '../../shared/widgets/healthcare_widgets.dart';

class AlertsScreen extends StatelessWidget {
  const AlertsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(title: const Text('Notifications & Health Alerts')),
      body: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          const DemoDataNoticeBanner(label: 'DEMO DATA — NOT A REAL PATIENT'),
          ...DemoData.alerts.map((alert) {
            final isSuccess = alert['severity'] == 'SUCCESS';
            return Padding(
              padding: const EdgeInsets.only(bottom: 14),
              child: HealthcareCard(
              borderColor: isSuccess ? AppTheme.success.withValues(alpha: 0.3) : AppTheme.info.withValues(alpha: 0.3),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: isSuccess ? AppTheme.success.withValues(alpha: 0.15) : AppTheme.info.withValues(alpha: 0.15),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      isSuccess ? Icons.check_circle_outline : Icons.notifications_active_outlined,
                      color: isSuccess ? AppTheme.success : AppTheme.info,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 14),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(alert['title'], style: const TextStyle(color: AppTheme.textPrimary, fontSize: 15, fontWeight: FontWeight.w700)),
                            Text(alert['time'], style: const TextStyle(color: AppTheme.textMuted, fontSize: 11)),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(alert['message'], style: const TextStyle(color: AppTheme.textSecondary, fontSize: 13, height: 1.35)),
                      ],
                    ),
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
